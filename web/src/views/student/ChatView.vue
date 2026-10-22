<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { chatApi } from '@/api/social'
import { useAuthStore } from '@/stores/auth'
import type { ChatMessage, Conversation } from '@/types'
import { formatMoney, relativeTime } from '@/utils/format'
import UserAvatar from '@/components/UserAvatar.vue'
import EmptyState from '@/components/EmptyState.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const conversations = ref<Conversation[]>([])
const activeId = ref<number | null>(null)
const messages = ref<ChatMessage[]>([])
const draft = ref('')
const loadingList = ref(false)
const loadingMessages = ref(false)
const sending = ref(false)
const bodyRef = ref<HTMLElement | null>(null)

const offerVisible = ref(false)
const offerAmount = ref<number | null>(null)
const offerNote = ref('')

let timer: number | undefined

const active = computed(() => conversations.value.find((item) => item.id === activeId.value) ?? null)
const totalUnread = computed(() => conversations.value.reduce((sum, item) => sum + item.unreadCount, 0))

async function loadConversations() {
  loadingList.value = true
  try {
    conversations.value = await chatApi.list()
  } catch {
    conversations.value = []
  } finally {
    loadingList.value = false
  }
}

async function scrollToBottom() {
  await nextTick()
  const el = bodyRef.value
  if (el) {
    el.scrollTop = el.scrollHeight
  }
}

async function selectConversation(id: number, markRead = true) {
  if (activeId.value === id && messages.value.length) {
    return
  }
  activeId.value = id
  loadingMessages.value = true
  try {
    messages.value = await chatApi.messages(id)
    if (markRead) {
      await chatApi.markRead(id).catch(() => undefined)
      const target = conversations.value.find((item) => item.id === id)
      if (target) {
        target.unreadCount = 0
      }
    }
    await scrollToBottom()
  } catch {
    messages.value = []
  } finally {
    loadingMessages.value = false
  }
}

async function pollMessages() {
  if (!activeId.value) {
    return
  }
  const last = messages.value[messages.value.length - 1]
  try {
    const fresh = await chatApi.messages(activeId.value, last?.id)
    if (fresh.length) {
      messages.value = [...messages.value, ...fresh]
      await scrollToBottom()
      await chatApi.markRead(activeId.value).catch(() => undefined)
    }
  } catch {
    // 轮询失败静默处理，下一次继续
  }
}

async function send() {
  const content = draft.value.trim()
  if (!content || !activeId.value) {
    return
  }
  sending.value = true
  try {
    const message = await chatApi.send(activeId.value, { content, type: 'TEXT' })
    messages.value = [...messages.value, message]
    draft.value = ''
    const target = conversations.value.find((item) => item.id === message.conversationId)
    if (target) {
      target.lastMessage = content
      target.lastMessageAt = message.createdAt
    }
    await scrollToBottom()
  } finally {
    sending.value = false
  }
}

async function sendOffer() {
  if (!activeId.value || !offerAmount.value || offerAmount.value <= 0) {
    ElMessage.warning('请输入有效的报价金额')
    return
  }
  const note = offerNote.value.trim()
  const content = note ? `报价 ${formatMoney(offerAmount.value)}：${note}` : `报价 ${formatMoney(offerAmount.value)}`
  sending.value = true
  try {
    const message = await chatApi.send(activeId.value, {
      content,
      type: 'OFFER',
      offerAmount: offerAmount.value,
    })
    messages.value = [...messages.value, message]
    offerVisible.value = false
    offerAmount.value = null
    offerNote.value = ''
    ElMessage.success('报价已发送，等待卖家回复')
    await scrollToBottom()
  } finally {
    sending.value = false
  }
}

function openItem() {
  if (active.value) {
    router.push({ name: 'item-detail', params: { id: active.value.itemId } })
  }
}

watch(
  () => route.query.conv,
  async (value) => {
    const id = Number(value)
    if (Number.isFinite(id) && id > 0) {
      await selectConversation(id)
    }
  },
)

onMounted(async () => {
  await loadConversations()
  const fromQuery = Number(route.query.conv)
  if (Number.isFinite(fromQuery) && fromQuery > 0) {
    await selectConversation(fromQuery)
  } else if (conversations.value.length) {
    await selectConversation(conversations.value[0].id)
  }
  timer = window.setInterval(() => {
    pollMessages()
    loadConversations()
  }, 6000)
})

onUnmounted(() => {
  if (timer) {
    window.clearInterval(timer)
  }
})
</script>

<template>
  <div class="page-shell page">
    <div class="row between wrap" style="gap: 14px; margin-bottom: 18px">
      <div>
        <h1 class="display" style="font-size: clamp(26px, 3.2vw, 34px)">消息 · 议价</h1>
        <p class="muted small" style="margin-top: 6px">和卖家约时间、谈价格，所有沟通留痕便于维权</p>
      </div>
      <span v-if="totalUnread" class="pill pop">{{ totalUnread }} 条未读</span>
    </div>

    <div class="chat-layout">
      <aside v-loading="loadingList" class="chat-list">
        <div v-if="conversations.length === 0 && !loadingList" class="empty-box" style="padding: 32px 18px">
          <p class="small muted">还没有会话</p>
          <p class="tiny muted" style="margin-top: 6px">在商品详情页点「聊一聊」开始</p>
        </div>

        <button
          v-for="conv in conversations"
          :key="conv.id"
          type="button"
          class="conv"
          :class="{ active: conv.id === activeId }"
          @click="selectConversation(conv.id)"
        >
          <UserAvatar :name="conv.counterpartNickname" :size="38" />
          <div style="flex: 1; min-width: 0; text-align: left">
            <div class="row between" style="gap: 6px">
              <strong class="ellipsis small">{{ conv.counterpartNickname }}</strong>
              <span class="tiny muted nowrap">{{ relativeTime(conv.lastMessageAt) }}</span>
            </div>
            <div class="tiny muted ellipsis" style="margin-top: 2px">{{ conv.itemTitle }}</div>
            <div class="row between" style="gap: 6px; margin-top: 2px">
              <span class="tiny ellipsis" style="color: var(--ink-soft)">{{ conv.lastMessage }}</span>
              <el-badge v-if="conv.unreadCount" :value="conv.unreadCount" :max="99" />
            </div>
          </div>
        </button>
      </aside>

      <section class="chat-main">
        <template v-if="active">
          <header class="chat-head">
            <div style="min-width: 0">
              <strong class="ellipsis">{{ active.counterpartNickname }}</strong>
              <div class="tiny muted ellipsis">
                关于《{{ active.itemTitle }}》 ·
                <router-link :to="{ name: 'item-detail', params: { id: active.itemId } }" class="brand-text">查看商品</router-link>
              </div>
            </div>
            <el-button size="small" round @click="openItem">商品详情</el-button>
          </header>

          <div ref="bodyRef" v-loading="loadingMessages" class="chat-body">
            <div v-for="message in messages" :key="message.id" class="msg" :class="{ mine: message.senderId === auth.user?.id }">
              <UserAvatar :name="message.senderNickname" :size="34" />
              <div>
                <div class="bubble" :class="{ offer: message.type === 'OFFER' }">
                  <div v-if="message.type === 'OFFER'" class="tiny" style="opacity: 0.8; margin-bottom: 2px">议价报价</div>
                  {{ message.content }}
                </div>
                <div class="time">{{ relativeTime(message.createdAt) }}</div>
              </div>
            </div>
            <div v-if="messages.length === 0 && !loadingMessages" class="empty-box">
              <p class="small muted">打个招呼吧，先问问成色和交易时间</p>
            </div>
          </div>

          <div class="chat-compose">
            <el-button round :disabled="sending" @click="offerVisible = true">💸 报价</el-button>
            <el-input
              v-model="draft"
              placeholder="输入消息，Enter 发送"
              maxlength="1000"
              @keyup.enter="send"
            />
            <el-button type="primary" round :loading="sending" @click="send">发送</el-button>
          </div>
        </template>

        <EmptyState v-else icon="💬" title="选择左侧的会话" hint="或者去商品详情页点「聊一聊」发起新对话" />
      </section>
    </div>

    <el-dialog v-model="offerVisible" title="发起议价" width="min(420px, 92vw)">
      <el-form label-position="top">
        <el-form-item label="报价金额（元）">
          <el-input-number v-model="offerAmount" :min="0.01" :precision="2" :step="5" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="附言（可选）">
          <el-input v-model="offerNote" maxlength="100" placeholder="例如：今天能面交的话就这个价" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="offerVisible = false">取消</el-button>
        <el-button type="primary" :loading="sending" @click="sendOffer">发送报价</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.chat-list {
  scrollbar-width: thin;
}

.conv {
  width: 100%;
  border: 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
  background: transparent;
  font-family: inherit;
}

.conv:hover {
  background: #faf7f0;
}

.bubble.offer {
  border-color: #f0c8a8;
  background: var(--pop-soft);
}

.msg.mine .bubble.offer {
  background: var(--pop);
  border-color: var(--pop);
  color: #fff;
}
</style>
