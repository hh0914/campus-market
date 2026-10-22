<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { orderApi } from '@/api/trade'
import { usePagination } from '@/composables/usePagination'
import type { Order, OrderStatus } from '@/types'
import { ORDER_STATUS_LABELS, formatDateTime, formatMoney } from '@/utils/format'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()

const role = ref<'buyer' | 'seller'>('buyer')
const status = ref<OrderStatus | ''>('')
const orders = ref<Order[]>([])
const loading = ref(false)
const busyId = ref<number | null>(null)
const { page, size, total } = usePagination(8)

const statuses: (OrderStatus | '')[] = [
  '',
  'PENDING_PAY',
  'PAID',
  'SHIPPED',
  'REFUNDING',
  'COMPLETED',
  'CANCELLED',
  'REFUNDED',
]

const roleHint = computed(() =>
  role.value === 'buyer' ? '我买到的 · 付款后资金由平台托管' : '我卖出的 · 买家确认收货后自动结算',
)

async function load() {
  loading.value = true
  try {
    const result = await orderApi.list({
      role: role.value,
      status: status.value || undefined,
      page: page.value,
      size: size.value,
    })
    orders.value = result.list
    total.value = result.total
  } catch {
    orders.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function switchRole(next: 'buyer' | 'seller') {
  if (role.value === next) {
    return
  }
  role.value = next
  page.value = 1
  load()
}

function pick(next: OrderStatus | '') {
  status.value = next
  page.value = 1
  load()
}

function changePage(next: number) {
  page.value = next
  load()
}

async function pay(order: Order) {
  const confirmed = await ElMessageBox.confirm(
    `从钱包余额中托管支付 ${formatMoney(order.amount)}，确认收货前卖家拿不到这笔钱。`,
    '托管付款',
    { confirmButtonText: '确认支付', cancelButtonText: '稍后再说' },
  ).catch(() => false)
  if (!confirmed) {
    return
  }
  busyId.value = order.id
  try {
    await orderApi.pay(order.id)
    ElMessage.success('支付成功，等待卖家发货或约定面交')
    await load()
  } finally {
    busyId.value = null
  }
}

async function cancel(order: Order) {
  const result = await ElMessageBox.prompt('取消原因（可选）', '取消订单', {
    confirmButtonText: '确认取消',
    cancelButtonText: '不取消',
    inputPlaceholder: '例如：和卖家约好改天再买',
  }).catch(() => null)
  if (!result) {
    return
  }
  busyId.value = order.id
  try {
    await orderApi.cancel(order.id, result.value || undefined)
    ElMessage.success('订单已取消，商品重新上架')
    await load()
  } finally {
    busyId.value = null
  }
}

async function ship(order: Order) {
  const confirmed = await ElMessageBox.confirm('确认已发货或已完成面交？买家确认收货后货款将结算给你。', '确认发货', {
    confirmButtonText: '已发货',
    cancelButtonText: '还没',
  }).catch(() => false)
  if (!confirmed) {
    return
  }
  busyId.value = order.id
  try {
    await orderApi.ship(order.id)
    ElMessage.success('已标记发货，等待买家确认收货')
    await load()
  } finally {
    busyId.value = null
  }
}

async function confirmReceipt(order: Order) {
  const confirmed = await ElMessageBox.confirm(
    `确认收到商品？确认后 ${formatMoney(Number(order.amount) * (1 - 0.02))} 将结算给卖家（扣除 2% 平台服务费）。`,
    '确认收货',
    { confirmButtonText: '确认收货', cancelButtonText: '再检查一下', type: 'warning' },
  ).catch(() => false)
  if (!confirmed) {
    return
  }
  busyId.value = order.id
  try {
    await orderApi.confirm(order.id)
    ElMessage.success('交易完成，别忘了给卖家评价')
    await load()
  } finally {
    busyId.value = null
  }
}

async function requestRefund(order: Order) {
  const result = await ElMessageBox.prompt('说明退款原因，卖家或平台会据此处理。', '申请退款', {
    confirmButtonText: '提交申请',
    cancelButtonText: '取消',
    inputPlaceholder: '例如：商品与描述不符，键帽缺失',
    inputValidator: (value) => (value && value.trim().length >= 3 ? true : '请至少写 3 个字'),
  }).catch(() => null)
  if (!result) {
    return
  }
  busyId.value = order.id
  try {
    await orderApi.refund(order.id, result.value)
    ElMessage.success('退款申请已提交')
    await load()
  } finally {
    busyId.value = null
  }
}

onMounted(load)
</script>

<template>
  <div class="page-shell page">
    <div class="row between wrap" style="gap: 14px; margin-bottom: 18px">
      <div>
        <h1 class="display" style="font-size: clamp(26px, 3.2vw, 34px)">我的订单</h1>
        <p class="muted small" style="margin-top: 6px">{{ roleHint }}</p>
      </div>
      <el-button round plain @click="router.push({ name: 'wallet' })">💰 我的钱包</el-button>
    </div>

    <div class="surface pad" style="padding: 14px 18px">
      <el-radio-group :model-value="role" @change="switchRole($event as 'buyer' | 'seller')">
        <el-radio-button value="buyer">我买到的</el-radio-button>
        <el-radio-button value="seller">我卖出的</el-radio-button>
      </el-radio-group>

      <div class="chip-row" style="margin-top: 14px">
        <button
          v-for="item in statuses"
          :key="item || 'all'"
          type="button"
          class="chip"
          :class="{ active: status === item }"
          @click="pick(item)"
        >
          {{ item ? ORDER_STATUS_LABELS[item] : '全部' }}
        </button>
      </div>
    </div>

    <div v-loading="loading" class="stack" style="margin-top: 18px">
      <article v-for="order in orders" :key="order.id" class="surface order-card">
        <header class="row between wrap" style="gap: 10px; padding: 12px 18px; border-bottom: 1px solid var(--line)">
          <div class="row wrap" style="gap: 12px">
            <span class="tiny muted">订单号 {{ order.orderNo }}</span>
            <span class="tiny muted">{{ formatDateTime(order.createdAt) }}</span>
          </div>
          <StatusTag :status="order.status" kind="order" />
        </header>

        <div class="order-body">
          <button
            type="button"
            class="thumb"
            @click="router.push({ name: 'order-detail', params: { id: order.id } })"
          >
            <img v-if="order.itemCover" :src="order.itemCover" :alt="order.itemTitle ?? '商品'" />
            <span v-else class="thumb-fallback">集</span>
          </button>

          <div style="flex: 1; min-width: 0">
            <h3 class="ellipsis" style="font-size: 15px; font-weight: 600" @click="router.push({ name: 'order-detail', params: { id: order.id } })">
              {{ order.itemTitle ?? '商品已删除' }}
            </h3>
            <div class="row wrap tiny muted" style="gap: 12px; margin-top: 6px">
              <span>{{ role === 'buyer' ? `卖家 ${order.sellerNickname ?? '—'}` : `买家 ${order.buyerNickname ?? '—'}` }}</span>
              <span>{{ order.tradeMethod === 'MEETUP' ? '当面交易' : '快递寄送' }}</span>
              <span v-if="order.status === 'PENDING_PAY'">请在 {{ formatDateTime(order.payDeadline) }} 前付款</span>
            </div>
          </div>

          <div class="amount">
            <span class="tiny muted">金额</span>
            <span class="price" style="font-size: 22px">{{ formatMoney(order.amount) }}</span>
          </div>
        </div>

        <footer class="row wrap actions" style="gap: 8px">
          <el-button size="small" round @click="router.push({ name: 'order-detail', params: { id: order.id } })">订单详情</el-button>
          <el-button v-if="order.actions.pay" size="small" type="primary" round :loading="busyId === order.id" @click="pay(order)">
            立即付款
          </el-button>
          <el-button v-if="order.actions.ship" size="small" type="primary" round :loading="busyId === order.id" @click="ship(order)">
            确认发货
          </el-button>
          <el-button v-if="order.actions.confirm" size="small" type="primary" round :loading="busyId === order.id" @click="confirmReceipt(order)">
            确认收货
          </el-button>
          <el-button v-if="order.actions.refundRequest" size="small" round plain :loading="busyId === order.id" @click="requestRefund(order)">
            申请退款
          </el-button>
          <el-button v-if="order.actions.cancel" size="small" round plain :loading="busyId === order.id" @click="cancel(order)">
            取消订单
          </el-button>
        </footer>
      </article>
    </div>

    <EmptyState
      v-if="!loading && orders.length === 0"
      icon="🧾"
      :title="role === 'buyer' ? '还没有买到的订单' : '还没有卖出的订单'"
      hint="去市场看看，或把闲置发布出去等买家来问"
    >
      <div class="row" style="gap: 10px; margin-top: 14px; justify-content: center">
        <el-button round @click="router.push({ name: 'search' })">逛市场</el-button>
        <el-button type="primary" round @click="router.push({ name: 'item-new' })">发布闲置</el-button>
      </div>
    </EmptyState>

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
.order-card {
  overflow: hidden;
}

.order-body {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 18px;
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

.amount {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.actions {
  justify-content: flex-end;
  padding: 12px 18px;
  border-top: 1px dashed var(--line);
  background: #fbf8f1;
}

@media (max-width: 640px) {
  .order-body {
    flex-wrap: wrap;
  }

  .amount {
    align-items: flex-start;
  }

  .actions {
    justify-content: flex-start;
  }
}
</style>
