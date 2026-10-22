<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { notificationApi } from '@/api/social'
import { useNotificationStore } from '@/stores/notification'
import { usePagination } from '@/composables/usePagination'
import type { NotificationItem } from '@/types'
import { formatDateTime } from '@/utils/format'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()
const store = useNotificationStore()

const items = ref<NotificationItem[]>([])
const loading = ref(false)
const onlyUnread = ref(false)
const marking = ref(false)
const { page, size, total } = usePagination(10)

const ICONS: Record<string, string> = {
  ITEM_AUDIT: '🗂️',
  ORDER: '🧾',
  REPORT: '🚩',
  REVIEW: '⭐',
  SYSTEM: '📢',
}

const visible = computed(() => (onlyUnread.value ? items.value.filter((item) => !item.read) : items.value))
const unreadOnPage = computed(() => items.value.filter((item) => !item.read).length)

async function load() {
  loading.value = true
  try {
    const result = await notificationApi.list({ page: page.value, size: size.value })
    items.value = result.list
    total.value = result.total
    await store.refresh().catch(() => undefined)
  } catch {
    items.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function resolveLink(link?: string | null) {
  if (!link) {
    return null
  }
  const userMatch = link.match(/^\/users\/(\d+)(\/profile)?$/)
  if (userMatch) {
    return { name: 'user-profile', params: { id: userMatch[1] } }
  }
  const orderMatch = link.match(/^\/orders\/(\d+)$/)
  if (orderMatch) {
    return { name: 'order-detail', params: { id: orderMatch[1] } }
  }
  const itemMatch = link.match(/^\/items\/(\d+)$/)
  if (itemMatch) {
    return { name: 'item-detail', params: { id: itemMatch[1] } }
  }
  if (link.startsWith('/admin')) {
    return link
  }
  return null
}

async function open(item: NotificationItem) {
  if (!item.read) {
    await notificationApi.markRead(item.id).catch(() => undefined)
    item.read = true
    await store.refresh().catch(() => undefined)
  }
  const target = resolveLink(item.link)
  if (target) {
    router.push(target)
  }
}

async function markAll() {
  marking.value = true
  try {
    await notificationApi.markAllRead()
    items.value = items.value.map((item) => ({ ...item, read: true }))
    await store.refresh().catch(() => undefined)
    ElMessage.success('已全部标记为已读')
  } finally {
    marking.value = false
  }
}

function changePage(next: number) {
  page.value = next
  load()
}

onMounted(load)
</script>

<template>
  <div class="page-shell narrow page">
    <div class="row between wrap" style="gap: 14px; margin-bottom: 18px">
      <div>
        <h1 class="display" style="font-size: clamp(26px, 3.2vw, 34px)">消息通知</h1>
        <p class="muted small" style="margin-top: 6px">
          共 {{ total }} 条 · 本页 {{ unreadOnPage }} 条未读 · 当前未读总数 {{ store.unread }}
        </p>
      </div>
      <div class="row" style="gap: 8px">
        <el-switch v-model="onlyUnread" active-text="只看未读" />
        <el-button round plain :loading="marking" @click="markAll">全部标记已读</el-button>
      </div>
    </div>

    <div v-loading="loading" class="stack" style="gap: 10px">
      <article
        v-for="item in visible"
        :key="item.id"
        class="surface notice"
        :class="{ unread: !item.read }"
        @click="open(item)"
      >
        <span class="notice-icon">{{ ICONS[item.type] ?? '📩' }}</span>
        <div style="flex: 1; min-width: 0">
          <div class="row between wrap" style="gap: 8px">
            <strong class="small">{{ item.title }}</strong>
            <span class="tiny muted">{{ formatDateTime(item.createdAt) }}</span>
          </div>
          <p class="small ink-soft" style="margin-top: 4px">{{ item.content }}</p>
          <div class="row" style="gap: 10px; margin-top: 6px">
            <span v-if="!item.read" class="pill pop tiny">未读</span>
            <span v-if="item.link" class="tiny brand-text">查看详情 →</span>
          </div>
        </div>
      </article>
    </div>

    <EmptyState
      v-if="!loading && visible.length === 0"
      :icon="onlyUnread ? '✅' : '📭'"
      :title="onlyUnread ? '本页没有未读消息' : '还没有收到通知'"
      hint="订单状态变化、审核结果和举报处理都会在这里提醒你"
    />

    <div v-if="total > size" class="pager">
      <el-pagination
        background
        layout="prev, pager, next"
        :current-page="page"
        :page-size="size"
        :total="total"
        @current-change="changePage"
      />
    </div>
  </div>
</template>

<style scoped>
.notice {
  display: flex;
  gap: 14px;
  padding: 16px 18px;
  cursor: pointer;
  transition: border-color 0.16s ease, transform 0.16s ease;
}

.notice:hover {
  border-color: var(--brand);
  transform: translateX(2px);
}

.notice.unread {
  border-left: 3px solid var(--brand);
  background: #fffefa;
}

.notice-icon {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  flex: 0 0 auto;
  border-radius: 12px;
  background: var(--brand-soft);
  font-size: 17px;
}
</style>
