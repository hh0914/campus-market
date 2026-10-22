<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { itemApi } from '@/api/items'
import { chatApi, reportApi } from '@/api/social'
import { orderApi } from '@/api/trade'
import { useAuthStore } from '@/stores/auth'
import type { ItemDetail, ItemSummary, ReportReason } from '@/types'
import {
  CONDITION_LABELS,
  REPORT_REASON_LABELS,
  TRADE_METHOD_LABELS,
  formatMoney,
  formatNumber,
  relativeTime,
  splitTags,
} from '@/utils/format'
import StatusTag from '@/components/StatusTag.vue'
import CreditBadge from '@/components/CreditBadge.vue'
import UserAvatar from '@/components/UserAvatar.vue'
import ItemCard from '@/components/ItemCard.vue'
import EmptyState from '@/components/EmptyState.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const detail = ref<ItemDetail | null>(null)
const related = ref<ItemSummary[]>([])
const loading = ref(true)
const notFound = ref(false)
const activeImage = ref(0)
const busy = ref('')
const imageFailed = ref(false)

const reportVisible = ref(false)
const reportBusy = ref(false)
const reportForm = reactive({
  reasonType: 'COUNTERFEIT' as ReportReason,
  description: '',
  evidenceUrl: '',
})

const reasons = Object.entries(REPORT_REASON_LABELS).map(([value, label]) => ({ value, label }))

const images = computed(() => {
  if (!detail.value) {
    return []
  }
  return detail.value.images.length ? detail.value.images : ['']
})

const tags = computed(() => splitTags(detail.value?.tags))
const onSale = computed(() => detail.value?.status === 'ON_SALE')
const canManage = computed(() => Boolean(detail.value?.mine))

function goLogin() {
  router.push({ name: 'login', query: { redirect: route.fullPath } })
}

function patch(next: ItemSummary) {
  const index = related.value.findIndex((item) => item.id === next.id)
  if (index >= 0) {
    related.value[index] = next
  }
}

async function load() {
  loading.value = true
  notFound.value = false
  detail.value = null
  related.value = []
  activeImage.value = 0
  imageFailed.value = false
  try {
    detail.value = await itemApi.detail(String(route.params.id))
    related.value = await itemApi.related(String(route.params.id)).catch(() => [])
  } catch {
    notFound.value = true
  } finally {
    loading.value = false
  }
}

async function toggleFavorite() {
  if (!auth.isLoggedIn) {
    goLogin()
    return
  }
  if (!detail.value || busy.value) {
    return
  }
  busy.value = 'favorite'
  try {
    const result = detail.value.favorited
      ? await itemApi.unfavorite(detail.value.id)
      : await itemApi.favorite(detail.value.id)
    detail.value = { ...detail.value, favorited: result.favorited, favoriteCount: result.favoriteCount }
    ElMessage.success(result.favorited ? '已加入收藏' : '已取消收藏')
  } finally {
    busy.value = ''
  }
}

async function startChat() {
  if (!detail.value) {
    return
  }
  if (!auth.isLoggedIn) {
    goLogin()
    return
  }
  busy.value = 'chat'
  try {
    const conversation = await chatApi.open(detail.value.id)
    router.push({ name: 'chat', query: { conv: String(conversation.id) } })
  } finally {
    busy.value = ''
  }
}

async function buy() {
  if (!detail.value) {
    return
  }
  if (!auth.isLoggedIn) {
    goLogin()
    return
  }
  if (!auth.isVerified) {
    ElMessage.warning('下单前需要先完成校园认证')
    router.push({ name: 'profile', query: { verify: '1' } })
    return
  }
  const confirmed = await ElMessageBox.confirm(
    `订单金额 ${formatMoney(detail.value.price)}，下单后商品会为你锁定 30 分钟，付款后资金进入平台托管。`,
    '确认下单',
    { confirmButtonText: '创建订单', cancelButtonText: '再想想', type: 'warning' },
  ).catch(() => false)
  if (!confirmed) {
    return
  }
  busy.value = 'order'
  try {
    const order = await orderApi.create(detail.value.id)
    ElMessage.success('订单已创建，请在 30 分钟内完成付款')
    router.push({ name: 'order-detail', params: { id: order.id } })
  } finally {
    busy.value = ''
  }
}

async function offShelf() {
  if (!detail.value) {
    return
  }
  const confirmed = await ElMessageBox.confirm('下架后商品会从市场移除，可随时重新提交审核。', '下架商品', {
    confirmButtonText: '确认下架',
    cancelButtonText: '取消',
    type: 'warning',
  }).catch(() => false)
  if (!confirmed) {
    return
  }
  busy.value = 'off'
  try {
    await itemApi.offShelf(detail.value.id)
    ElMessage.success('商品已下架')
    await load()
  } finally {
    busy.value = ''
  }
}

async function resubmit() {
  if (!detail.value) {
    return
  }
  busy.value = 'submit'
  try {
    detail.value = await itemApi.submit(detail.value.id)
    ElMessage.success('已提交审核，审核员会尽快处理')
  } finally {
    busy.value = ''
  }
}

function openReport() {
  if (!auth.isLoggedIn) {
    goLogin()
    return
  }
  reportVisible.value = true
}

async function submitReport() {
  if (!detail.value) {
    return
  }
  if (reportForm.description.trim().length < 5) {
    ElMessage.warning('请至少写 5 个字说明问题')
    return
  }
  reportBusy.value = true
  try {
    await reportApi.create({
      targetType: 'ITEM',
      targetId: detail.value.id,
      reasonType: reportForm.reasonType,
      description: reportForm.description.trim(),
      evidenceUrl: reportForm.evidenceUrl.trim() || undefined,
    })
    ElMessage.success('举报已提交，审核员会尽快核实')
    reportVisible.value = false
    reportForm.description = ''
    reportForm.evidenceUrl = ''
  } finally {
    reportBusy.value = false
  }
}

watch(
  () => route.params.id,
  () => {
    if (route.name === 'item-detail') {
      load()
    }
  },
)

onMounted(load)
</script>

<template>
  <div class="page-shell page">
    <div v-if="loading" v-loading="true" style="min-height: 420px" />

    <EmptyState v-else-if="notFound || !detail" icon="📦" title="商品不存在或已被删除" hint="它可能已经被卖家下架，去看看别的闲置吧">
      <el-button type="primary" round style="margin-top: 14px" @click="router.push({ name: 'search' })">逛市场</el-button>
    </EmptyState>

    <template v-else>
      <nav class="row small muted crumbs">
        <router-link :to="{ name: 'home' }">首页</router-link>
        <span>/</span>
        <router-link :to="{ name: 'search', query: { categoryId: String(detail.categoryId ?? '') } }">
          {{ detail.categoryName ?? '全部闲置' }}
        </router-link>
        <span>/</span>
        <span class="ellipsis" style="max-width: 340px">{{ detail.title }}</span>
      </nav>

      <div class="detail-grid" style="margin-top: 18px">
        <div>
          <div class="gallery-main">
            <img
              v-if="images[activeImage] && !imageFailed"
              :src="images[activeImage]"
              :alt="detail.title"
              @error="imageFailed = true"
            />
            <div v-else class="gallery-fallback">
              <span>集</span>
              <p class="small muted">卖家暂未上传实拍图</p>
            </div>
            <span v-if="images.length > 1 && images[activeImage] && !imageFailed" class="gallery-count">
              {{ activeImage + 1 }} / {{ images.length }}
            </span>
          </div>
          <div v-if="images.length > 1" class="gallery-thumbs">
            <button
              v-for="(url, index) in images"
              :key="url"
              type="button"
              :class="{ active: index === activeImage }"
              @click="activeImage = index; imageFailed = false"
            >
              <img :src="url" :alt="`${detail.title} 图 ${index + 1}`" />
            </button>
          </div>
        </div>

        <div class="surface pad detail-panel">
          <div class="row wrap" style="gap: 8px">
            <StatusTag :status="detail.status" />
            <span class="pill plain">{{ CONDITION_LABELS[detail.conditionLevel] }}</span>
            <span class="pill plain">{{ TRADE_METHOD_LABELS[detail.tradeMethod] }}</span>
            <span v-if="detail.sellerVerified" class="pill">✓ 已校园认证</span>
          </div>

          <h1 class="detail-title" style="margin-top: 14px">{{ detail.title }}</h1>

          <div class="row wrap" style="gap: 14px; margin-top: 16px; align-items: baseline">
            <span class="price detail-price">{{ formatMoney(detail.price) }}</span>
            <span v-if="detail.originalPrice" class="price-origin">原价 {{ formatMoney(detail.originalPrice) }}</span>
            <span class="tiny muted">{{ relativeTime(detail.publishedAt ?? detail.createdAt) }}发布</span>
          </div>

          <div class="spec-grid" style="margin-top: 18px">
            <div>
              <div class="spec-label">成色</div>
              <div class="spec-value">{{ CONDITION_LABELS[detail.conditionLevel] }}</div>
            </div>
            <div>
              <div class="spec-label">分类</div>
              <div class="spec-value">{{ detail.categoryName ?? '未分类' }}</div>
            </div>
            <div>
              <div class="spec-label">校区</div>
              <div class="spec-value">{{ detail.campus ?? '未填写' }}</div>
            </div>
            <div>
              <div class="spec-label">交易方式</div>
              <div class="spec-value">{{ TRADE_METHOD_LABELS[detail.tradeMethod] }}</div>
            </div>
            <div>
              <div class="spec-label">浏览 / 收藏</div>
              <div class="spec-value">{{ formatNumber(detail.viewCount) }} / {{ formatNumber(detail.favoriteCount) }}</div>
            </div>
            <div>
              <div class="spec-label">上架时间</div>
              <div class="spec-value">{{ relativeTime(detail.publishedAt ?? detail.createdAt) }}</div>
            </div>
          </div>

          <button type="button" class="seller-box seller-link" @click="router.push({ name: 'user-profile', params: { id: detail.sellerId } })">
            <UserAvatar :name="detail.sellerNickname" :src="detail.sellerAvatar" :size="44" />
            <div style="min-width: 0; text-align: left">
              <div class="row" style="gap: 8px">
                <strong>{{ detail.sellerNickname ?? '同学' }}</strong>
                <CreditBadge :score="detail.sellerCreditScore" />
              </div>
              <div class="tiny muted">查看卖家主页与历史评价 →</div>
            </div>
          </button>

          <el-alert
            v-if="!onSale"
            style="margin-top: 16px"
            :type="detail.status === 'LOCKED' ? 'warning' : 'info'"
            :closable="false"
            :title="detail.status === 'LOCKED' ? '该商品正在交易中，暂时无法下单' : `当前状态：${detail.status === 'PENDING_REVIEW' ? '等待审核' : detail.status === 'REJECTED' ? '审核未通过' : detail.status === 'SOLD' ? '已售出' : '已下架'}`"
          />
          <el-alert
            v-if="detail.status === 'REJECTED' && detail.rejectReason"
            style="margin-top: 10px"
            type="error"
            :closable="false"
            :title="`驳回原因：${detail.rejectReason}`"
          />

          <div v-if="!canManage" class="stack" style="margin-top: 18px">
            <el-button type="primary" size="large" round :disabled="!onSale" :loading="busy === 'order'" @click="buy">
              立即下单 · {{ formatMoney(detail.price) }}
            </el-button>
            <div class="row" style="gap: 10px">
              <el-button round style="flex: 1" :loading="busy === 'chat'" :disabled="!onSale" @click="startChat">💬 聊一聊</el-button>
              <el-button round style="flex: 1" :loading="busy === 'favorite'" @click="toggleFavorite">
                {{ detail.favorited ? '♥ 已收藏' : '♡ 收藏' }}
              </el-button>
            </div>
            <p class="form-hint">下单后 30 分钟内未付款订单会自动取消，资金在确认收货前一直由平台托管。</p>
          </div>

          <div v-else class="stack" style="margin-top: 18px">
            <el-button type="primary" size="large" round @click="router.push({ name: 'item-edit', params: { id: detail.id } })">
              编辑商品
            </el-button>
            <div class="row" style="gap: 10px">
              <el-button
                v-if="detail.status === 'DRAFT' || detail.status === 'REJECTED' || detail.status === 'OFF_SHELF'"
                round
                style="flex: 1"
                :loading="busy === 'submit'"
                @click="resubmit"
              >
                提交审核
              </el-button>
              <el-button
                v-if="onSale || detail.status === 'PENDING_REVIEW'"
                round
                style="flex: 1"
                :loading="busy === 'off'"
                @click="offShelf"
              >
                下架
              </el-button>
            </div>
            <p class="form-hint">这是你自己发布的商品，可以在「我的发布」里统一管理。</p>
          </div>

          <div class="row" style="justify-content: center; margin-top: 14px">
            <el-button text size="small" class="muted" @click="openReport">🚩 举报该商品</el-button>
          </div>
        </div>
      </div>

      <div class="surface pad" style="margin-top: 26px">
        <h2 class="display" style="font-size: 22px">商品描述</h2>
        <p class="description-body" style="margin-top: 12px">{{ detail.description }}</p>
        <div v-if="tags.length" class="chip-row" style="margin-top: 18px">
          <span v-for="tag in tags" :key="tag" class="pill plain"># {{ tag }}</span>
        </div>
      </div>

      <section v-if="related.length" class="section">
        <div class="section-head">
          <h2>相似闲置</h2>
          <router-link :to="{ name: 'search', query: { categoryId: String(detail.categoryId ?? '') } }" class="small muted">
            查看更多 →
          </router-link>
        </div>
        <div class="grid-items">
          <ItemCard v-for="item in related" :key="item.id" :item="item" @updated="patch" />
        </div>
      </section>
    </template>

    <el-dialog v-model="reportVisible" title="举报商品" width="min(520px, 92vw)">
      <el-form label-position="top">
        <el-form-item label="举报类型">
          <el-select v-model="reportForm.reasonType" style="width: 100%">
            <el-option v-for="reason in reasons" :key="reason.value" :label="reason.label" :value="reason.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="问题描述">
          <el-input
            v-model="reportForm.description"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="请描述具体情况，例如：商品与描述严重不符、要求私下转账等"
          />
        </el-form-item>
        <el-form-item label="证据链接（可选）">
          <el-input v-model="reportForm.evidenceUrl" placeholder="聊天截图或图片链接" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reportVisible = false">取消</el-button>
        <el-button type="primary" :loading="reportBusy" @click="submitReport">提交举报</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.crumbs {
  gap: 8px;
  padding: 4px 2px;
}

.crumbs a:hover {
  color: var(--brand);
}

.gallery-fallback {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 6px;
}

.gallery-fallback span {
  font-family: var(--font-display);
  font-size: 64px;
  color: rgba(20, 107, 82, 0.3);
}

.seller-link {
  width: 100%;
  margin-top: 14px;
  cursor: pointer;
  transition: border-color 0.16s ease, background 0.16s ease;
}

.seller-link:hover {
  border-color: var(--brand);
  background: var(--brand-soft);
}
</style>
