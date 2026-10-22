<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { orderApi } from '@/api/trade'
import { reviewApi } from '@/api/social'
import { useAuthStore } from '@/stores/auth'
import type { OrderDetail, Review } from '@/types'
import {
  LEDGER_TYPE_LABELS,
  ORDER_STATUS_LABELS,
  TRADE_METHOD_LABELS,
  formatDateTime,
  formatMoney,
  splitTags,
} from '@/utils/format'
import StatusTag from '@/components/StatusTag.vue'
import UserAvatar from '@/components/UserAvatar.vue'
import EmptyState from '@/components/EmptyState.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const detail = ref<OrderDetail | null>(null)
const reviews = ref<Review[]>([])
const loading = ref(true)
const busy = ref('')

const reviewVisible = ref(false)
const reviewForm = reactive({ rating: 5, tags: '', comment: '' })
const reviewTags = ['描述相符', '发货迅速', '沟通顺畅', '包装用心', '价格公道', '有耐心']

const order = computed(() => detail.value?.order ?? null)
const isBuyer = computed(() => order.value?.buyerId === auth.user?.id)
const isSeller = computed(() => order.value?.sellerId === auth.user?.id)
const canReview = computed(
  () => Boolean(order.value?.actions.review) && !detail.value?.reviewedByMe && order.value?.status === 'COMPLETED',
)
const feeRate = computed(() => detail.value?.platformFeeRate ?? 0.02)

async function load() {
  loading.value = true
  try {
    detail.value = await orderApi.detail(String(route.params.id))
    reviews.value = await reviewApi.byOrder(detail.value.order.id).catch(() => [])
  } catch {
    detail.value = null
  } finally {
    loading.value = false
  }
}

async function run(action: string, task: () => Promise<unknown>, success: string) {
  busy.value = action
  try {
    await task()
    ElMessage.success(success)
    await load()
  } finally {
    busy.value = ''
  }
}

async function pay() {
  if (!order.value) return
  const confirmed = await ElMessageBox.confirm(
    `从钱包余额托管支付 ${formatMoney(order.value.amount)}，确认收货前款项不会结算给卖家。`,
    '托管付款',
    { confirmButtonText: '确认支付', cancelButtonText: '稍后再说' },
  ).catch(() => false)
  if (confirmed) {
    await run('pay', () => orderApi.pay(order.value!.id), '支付成功')
  }
}

async function cancel() {
  if (!order.value) return
  const result = await ElMessageBox.prompt('取消原因（可选）', '取消订单', {
    confirmButtonText: '确认取消',
    cancelButtonText: '不取消',
  }).catch(() => null)
  if (result) {
    await run('cancel', () => orderApi.cancel(order.value!.id, result.value || undefined), '订单已取消')
  }
}

async function ship() {
  if (!order.value) return
  const confirmed = await ElMessageBox.confirm('确认已发货或已完成当面交易？', '确认发货', {
    confirmButtonText: '已发货',
    cancelButtonText: '还没',
  }).catch(() => false)
  if (confirmed) {
    await run('ship', () => orderApi.ship(order.value!.id), '已标记发货')
  }
}

async function confirmReceipt() {
  if (!order.value) return
  const net = Number(order.value.amount) * (1 - feeRate.value)
  const confirmed = await ElMessageBox.confirm(
    `确认收到商品？确认后 ${formatMoney(net)} 结算给卖家，平台收取 ${(feeRate.value * 100).toFixed(0)}% 服务费。`,
    '确认收货',
    { confirmButtonText: '确认收货', cancelButtonText: '再检查一下', type: 'warning' },
  ).catch(() => false)
  if (confirmed) {
    await run('confirm', () => orderApi.confirm(order.value!.id), '交易完成，去给卖家一个评价吧')
  }
}

async function requestRefund() {
  if (!order.value) return
  const result = await ElMessageBox.prompt('说明退款原因，卖家会在订单内处理。', '申请退款', {
    confirmButtonText: '提交申请',
    cancelButtonText: '取消',
    inputValidator: (value) => (value && value.trim().length >= 3 ? true : '请至少写 3 个字'),
  }).catch(() => null)
  if (result) {
    await run('refund', () => orderApi.refund(order.value!.id, result.value), '退款申请已提交')
  }
}

async function approveRefund() {
  if (!order.value) return
  const confirmed = await ElMessageBox.confirm('同意退款？托管款项会原路退回买家钱包。', '同意退款', {
    confirmButtonText: '同意退款',
    cancelButtonText: '再看看',
    type: 'warning',
  }).catch(() => false)
  if (confirmed) {
    await run('approve', () => orderApi.refundApprove(order.value!.id), '已同意退款')
  }
}

async function rejectRefund() {
  if (!order.value) return
  const result = await ElMessageBox.prompt('说明拒绝退款的理由', '拒绝退款', {
    confirmButtonText: '提交',
    cancelButtonText: '取消',
    inputValidator: (value) => (value && value.trim().length >= 3 ? true : '请至少写 3 个字'),
  }).catch(() => null)
  if (result) {
    await run('reject', () => orderApi.refundReject(order.value!.id, result.value), '已拒绝退款，等待买家确认')
  }
}

function toggleTag(tag: string) {
  const tags = reviewForm.tags ? reviewForm.tags.split(/[,，\s]+/).filter(Boolean) : []
  const next = tags.includes(tag) ? tags.filter((item) => item !== tag) : [...tags, tag]
  reviewForm.tags = next.join(',')
}

function hasTag(tag: string) {
  return splitTags(reviewForm.tags).includes(tag)
}

async function submitReview() {
  if (!order.value) return
  busy.value = 'review'
  try {
    await reviewApi.create(order.value.id, {
      rating: reviewForm.rating,
      tags: reviewForm.tags || undefined,
      comment: reviewForm.comment || undefined,
    })
    ElMessage.success('评价已提交，感谢你的反馈')
    reviewVisible.value = false
    reviewForm.comment = ''
    await load()
  } finally {
    busy.value = ''
  }
}

watch(
  () => route.params.id,
  () => {
    if (route.name === 'order-detail') {
      load()
    }
  },
)

onMounted(load)
</script>

<template>
  <div class="page-shell narrow page">
    <div class="row between" style="margin-bottom: 16px">
      <el-button text @click="router.push({ name: 'orders' })">← 返回订单列表</el-button>
      <span v-if="order" class="tiny muted">订单号 {{ order.orderNo }}</span>
    </div>

    <div v-if="loading" v-loading="true" style="min-height: 380px" />

    <EmptyState v-else-if="!detail || !order" icon="🧾" title="订单不存在或无权查看" hint="请确认你使用的是下单时的账号">
      <el-button type="primary" round style="margin-top: 14px" @click="router.push({ name: 'orders' })">返回我的订单</el-button>
    </EmptyState>

    <template v-else>
      <section class="surface pad status-hero">
        <div class="row between wrap" style="gap: 14px">
          <div>
            <div class="row wrap" style="gap: 10px">
              <StatusTag :status="order.status" kind="order" />
              <span class="pill plain">{{ TRADE_METHOD_LABELS[order.tradeMethod] }}</span>
              <span class="pill plain">{{ isBuyer ? '我是买家' : isSeller ? '我是卖家' : '相关方' }}</span>
            </div>
            <h1 class="display" style="font-size: clamp(24px, 3.2vw, 32px); margin-top: 10px">
              {{ ORDER_STATUS_LABELS[order.status] }}
            </h1>
            <p class="small muted" style="margin-top: 6px">
              <template v-if="order.status === 'PENDING_PAY'">请在 {{ formatDateTime(order.payDeadline) }} 前完成托管付款</template>
              <template v-else-if="order.status === 'PAID'">等待卖家发货或约定面交时间</template>
              <template v-else-if="order.status === 'SHIPPED'">卖家已发货，收到商品后请及时确认</template>
              <template v-else-if="order.status === 'REFUNDING'">退款申请处理中，卖家可在下方同意或拒绝</template>
              <template v-else-if="order.status === 'COMPLETED'">交易已完成，款项已结算给卖家</template>
              <template v-else-if="order.status === 'CANCELLED'">订单已取消，商品已重新上架</template>
              <template v-else>退款已完成，款项已退回买家钱包</template>
            </p>
          </div>

          <div class="row wrap" style="gap: 8px">
            <el-button v-if="order.actions.pay" type="primary" round :loading="busy === 'pay'" @click="pay">立即付款</el-button>
            <el-button v-if="order.actions.ship" type="primary" round :loading="busy === 'ship'" @click="ship">确认发货</el-button>
            <el-button v-if="order.actions.confirm" type="primary" round :loading="busy === 'confirm'" @click="confirmReceipt">确认收货</el-button>
            <el-button v-if="order.actions.refundApprove" type="primary" round :loading="busy === 'approve'" @click="approveRefund">同意退款</el-button>
            <el-button v-if="order.actions.refundReject" round :loading="busy === 'reject'" @click="rejectRefund">拒绝退款</el-button>
            <el-button v-if="order.actions.refundRequest" round plain :loading="busy === 'refund'" @click="requestRefund">申请退款</el-button>
            <el-button v-if="order.actions.cancel" round plain :loading="busy === 'cancel'" @click="cancel">取消订单</el-button>
            <el-button v-if="canReview" round type="primary" plain @click="reviewVisible = true">去评价</el-button>
          </div>
        </div>
      </section>

      <section class="surface pad" style="margin-top: 18px">
        <div class="row" style="gap: 16px; align-items: center">
          <button type="button" class="thumb" @click="router.push({ name: 'item-detail', params: { id: order.itemId } })">
            <img v-if="order.itemCover" :src="order.itemCover" :alt="order.itemTitle ?? '商品'" />
            <span v-else class="thumb-fallback">集</span>
          </button>
          <div style="flex: 1; min-width: 0">
            <h2 class="ellipsis" style="font-size: 17px; font-weight: 600">{{ order.itemTitle ?? '商品已删除' }}</h2>
            <div class="row wrap tiny muted" style="gap: 12px; margin-top: 6px">
              <span>金额 {{ formatMoney(order.amount) }}</span>
              <span v-if="order.feeAmount !== null && order.feeAmount !== undefined">
                服务费 {{ formatMoney(order.feeAmount) }}
              </span>
              <span v-if="order.sellerIncome !== null && order.sellerIncome !== undefined">
                卖家实收 {{ formatMoney(order.sellerIncome) }}
              </span>
            </div>
          </div>
          <el-button size="small" round @click="router.push({ name: 'item-detail', params: { id: order.itemId } })">查看商品</el-button>
        </div>

        <div class="divider" />

        <div class="info-grid">
          <div>
            <div class="info-label">买家</div>
            <button type="button" class="row payer" @click="router.push({ name: 'user-profile', params: { id: order.buyerId } })">
              <UserAvatar :name="order.buyerNickname" :size="26" />
              <span class="small">{{ order.buyerNickname ?? '—' }}</span>
            </button>
          </div>
          <div>
            <div class="info-label">卖家</div>
            <button type="button" class="row payer" @click="router.push({ name: 'user-profile', params: { id: order.sellerId } })">
              <UserAvatar :name="order.sellerNickname" :size="26" />
              <span class="small">{{ order.sellerNickname ?? '—' }}</span>
            </button>
          </div>
          <div>
            <div class="info-label">下单时间</div>
            <div class="small">{{ formatDateTime(order.createdAt) }}</div>
          </div>
          <div>
            <div class="info-label">付款时间</div>
            <div class="small">{{ formatDateTime(order.paidAt) }}</div>
          </div>
          <div>
            <div class="info-label">发货时间</div>
            <div class="small">{{ formatDateTime(order.shippedAt) }}</div>
          </div>
          <div>
            <div class="info-label">完成时间</div>
            <div class="small">{{ formatDateTime(order.completedAt) }}</div>
          </div>
          <div v-if="order.cancelReason" style="grid-column: 1 / -1">
            <div class="info-label">取消/退款原因</div>
            <div class="small">{{ order.cancelReason }}</div>
          </div>
        </div>
      </section>

      <div class="two-panel">
        <section class="surface pad">
          <h2 class="display" style="font-size: 20px; margin-bottom: 16px">订单流转</h2>
          <el-timeline>
            <el-timeline-item
              v-for="(log, index) in detail.timeline"
              :key="`${log.createdAt}-${index}`"
              :timestamp="formatDateTime(log.createdAt)"
              :type="index === detail.timeline.length - 1 ? 'primary' : undefined"
            >
              <strong class="small">
                {{ ORDER_STATUS_LABELS[log.toStatus as keyof typeof ORDER_STATUS_LABELS] ?? log.toStatus }}
              </strong>
              <p v-if="log.remark" class="tiny muted" style="margin-top: 2px">{{ log.remark }}</p>
              <p v-if="log.operatorNickname" class="tiny muted">操作人：{{ log.operatorNickname }}</p>
            </el-timeline-item>
          </el-timeline>
          <p v-if="detail.timeline.length === 0" class="small muted">暂无流转记录</p>
        </section>

        <section class="surface pad">
          <h2 class="display" style="font-size: 20px; margin-bottom: 16px">资金流水</h2>
          <div v-if="detail.payments.length" class="stack" style="gap: 10px">
            <div v-for="payment in detail.payments" :key="payment.id" class="row between payment-row">
              <div>
                <div class="small">{{ LEDGER_TYPE_LABELS[payment.type] ?? payment.type }}</div>
                <div class="tiny muted">{{ payment.tradeNo }} · {{ formatDateTime(payment.createdAt) }}</div>
              </div>
              <div style="text-align: right">
                <div class="price" style="font-size: 16px">{{ formatMoney(payment.amount) }}</div>
                <div class="tiny muted">{{ payment.status }}</div>
              </div>
            </div>
          </div>
          <p v-else class="small muted">尚未产生资金流水</p>

          <div class="divider" />

          <div class="row between small">
            <span class="muted">平台服务费率</span>
            <strong>{{ (feeRate * 100).toFixed(0) }}%</strong>
          </div>
          <div class="row between small" style="margin-top: 6px">
            <span class="muted">托管说明</span>
            <span class="tiny" style="max-width: 220px; text-align: right">付款后资金冻结在平台，确认收货才结算</span>
          </div>
        </section>
      </div>

      <section class="surface pad" style="margin-top: 18px">
        <div class="row between" style="margin-bottom: 12px">
          <h2 class="display" style="font-size: 20px">交易评价</h2>
          <el-button v-if="canReview" size="small" type="primary" round @click="reviewVisible = true">写评价</el-button>
        </div>

        <div v-if="reviews.length" class="stack" style="gap: 10px">
          <article v-for="review in reviews" :key="review.id" class="review-line">
            <div class="row between wrap" style="gap: 10px">
              <div class="row" style="gap: 8px">
                <UserAvatar :name="review.reviewerNickname" :size="28" />
                <span class="small">{{ review.reviewerNickname ?? '匿名同学' }}</span>
                <span class="pill plain tiny">{{ review.direction === 'BUYER_TO_SELLER' ? '买家评价卖家' : '卖家评价买家' }}</span>
              </div>
              <el-rate :model-value="review.rating" disabled size="small" />
            </div>
            <p v-if="review.comment" class="small ink-soft" style="margin-top: 8px">{{ review.comment }}</p>
            <div v-if="splitTags(review.tags).length" class="row wrap" style="gap: 6px; margin-top: 6px">
              <span v-for="tag in splitTags(review.tags)" :key="tag" class="pill plain">{{ tag }}</span>
            </div>
          </article>
        </div>
        <p v-else class="small muted">订单完成后，买卖双方可以互相评价，评价会影响信用分。</p>
      </section>
    </template>

    <el-dialog v-model="reviewVisible" title="评价这笔交易" width="min(480px, 92vw)">
      <el-form label-position="top">
        <el-form-item label="评分">
          <el-rate v-model="reviewForm.rating" show-score />
        </el-form-item>
        <el-form-item label="印象标签">
          <div class="row wrap" style="gap: 8px">
            <button
              v-for="tag in reviewTags"
              :key="tag"
              type="button"
              class="chip"
              :class="{ active: hasTag(tag) }"
              @click="toggleTag(tag)"
            >
              {{ tag }}
            </button>
          </div>
        </el-form-item>
        <el-form-item label="评价内容（可选）">
          <el-input
            v-model="reviewForm.comment"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="说说这次交易的体验，给其他同学一个参考"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" :loading="busy === 'review'" @click="submitReview">提交评价</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.status-hero {
  background:
    radial-gradient(500px 220px at 96% 0%, rgba(20, 107, 82, 0.08), transparent 65%),
    var(--card);
}

.thumb {
  width: 74px;
  height: 74px;
  flex: 0 0 auto;
  padding: 0;
  border: 1px solid var(--line);
  border-radius: 10px;
  overflow: hidden;
  background: var(--paper-deep);
  cursor: pointer;
}

.thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.thumb-fallback {
  display: grid;
  place-items: center;
  width: 100%;
  height: 100%;
  font-family: var(--font-display);
  font-size: 24px;
  color: rgba(20, 107, 82, 0.35);
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.info-label {
  color: var(--muted);
  font-size: 12px;
  margin-bottom: 3px;
}

.payer {
  border: 0;
  background: transparent;
  padding: 0;
  cursor: pointer;
  font-family: inherit;
}

.payer:hover .small {
  color: var(--brand);
}

.two-panel {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 18px;
  margin-top: 18px;
  align-items: start;
}

.payment-row {
  padding: 10px 12px;
  background: var(--paper);
  border-radius: 10px;
}

.review-line {
  padding: 12px 14px;
  background: var(--paper);
  border-radius: 12px;
}

@media (max-width: 820px) {
  .two-panel {
    grid-template-columns: 1fr;
  }

  .info-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
