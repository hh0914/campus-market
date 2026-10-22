<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { walletApi } from '@/api/trade'
import { usePagination } from '@/composables/usePagination'
import type { LedgerEntry, WalletInfo } from '@/types'
import { LEDGER_TYPE_LABELS, formatDateTime, formatMoney } from '@/utils/format'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()

const wallet = ref<WalletInfo | null>(null)
const entries = ref<LedgerEntry[]>([])
const loading = ref(true)
const ledgerLoading = ref(false)
const rechargeVisible = ref(false)
const rechargeAmount = ref<number | null>(100)
const recharging = ref(false)
const { page, size, total } = usePagination(10)

const quickAmounts = [50, 100, 200, 500]

async function loadWallet() {
  wallet.value = await walletApi.view()
}

async function loadLedger() {
  ledgerLoading.value = true
  try {
    const result = await walletApi.ledger({ page: page.value, size: size.value })
    entries.value = result.list
    total.value = result.total
  } catch {
    entries.value = []
    total.value = 0
  } finally {
    ledgerLoading.value = false
  }
}

async function recharge() {
  if (!rechargeAmount.value || rechargeAmount.value <= 0) {
    ElMessage.warning('请输入有效的充值金额')
    return
  }
  recharging.value = true
  try {
    wallet.value = await walletApi.recharge(rechargeAmount.value)
    ElMessage.success(`已充值 ${formatMoney(rechargeAmount.value)}`)
    rechargeVisible.value = false
    page.value = 1
    await loadLedger()
  } finally {
    recharging.value = false
  }
}

function changePage(next: number) {
  page.value = next
  loadLedger()
}

onMounted(async () => {
  try {
    await Promise.all([loadWallet(), loadLedger()])
  } catch {
    wallet.value = null
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="page-shell page">
    <div class="row between wrap" style="gap: 14px; margin-bottom: 20px">
      <div>
        <h1 class="display" style="font-size: clamp(26px, 3.2vw, 34px)">我的钱包</h1>
        <p class="muted small" style="margin-top: 6px">演示环境使用站内余额，下单付款即进入平台托管</p>
      </div>
      <div class="row" style="gap: 8px">
        <el-button round plain @click="router.push({ name: 'orders' })">我的订单</el-button>
        <el-button type="primary" round @click="rechargeVisible = true">＋ 充值</el-button>
      </div>
    </div>

    <div v-loading="loading" class="stat-grid">
      <div class="stat-card accent">
        <div class="label">可用余额</div>
        <div class="value">{{ formatMoney(wallet?.balance) }}</div>
        <div class="hint">可用于下单托管付款</div>
      </div>
      <div class="stat-card">
        <div class="label">托管冻结</div>
        <div class="value">{{ formatMoney(wallet?.frozen) }}</div>
        <div class="hint">订单进行中的款项</div>
      </div>
      <div class="stat-card">
        <div class="label">累计收入</div>
        <div class="value">{{ formatMoney(wallet?.totalIncome) }}</div>
        <div class="hint">卖闲置结算所得</div>
      </div>
      <div class="stat-card">
        <div class="label">累计支出</div>
        <div class="value">{{ formatMoney(wallet?.totalSpent) }}</div>
        <div class="hint">含托管付款与平台服务费</div>
      </div>
    </div>

    <section class="surface" style="margin-top: 22px">
      <div class="row between" style="padding: 16px 20px; border-bottom: 1px solid var(--line)">
        <h2 class="display" style="font-size: 20px">资金明细</h2>
        <span class="tiny muted">共 {{ total }} 条</span>
      </div>

      <div v-loading="ledgerLoading">
        <div v-if="entries.length" class="ledger-list">
          <div v-for="entry in entries" :key="entry.id" class="ledger-row">
            <div class="row" style="gap: 10px; min-width: 0">
              <span class="ledger-icon">{{ LEDGER_TYPE_LABELS[entry.type]?.slice(0, 1) ?? '·' }}</span>
              <div style="min-width: 0">
                <div class="small">{{ LEDGER_TYPE_LABELS[entry.type] ?? entry.type }}</div>
                <div class="tiny muted ellipsis">
                  {{ formatDateTime(entry.createdAt) }}
                  <template v-if="entry.orderId"> · 订单 #{{ entry.orderId }}</template>
                  <template v-if="entry.remark"> · {{ entry.remark }}</template>
                </div>
              </div>
            </div>
            <div style="text-align: right">
              <div :class="['amount', Number(entry.amount) >= 0 ? 'income' : 'expense']">
                {{ Number(entry.amount) >= 0 ? '+' : '' }}{{ formatMoney(entry.amount) }}
              </div>
              <div class="tiny muted">余额 {{ formatMoney(entry.balanceAfter) }}</div>
            </div>
          </div>
        </div>

        <EmptyState v-else-if="!ledgerLoading" icon="💰" title="还没有资金记录" hint="充值或完成一笔交易后，明细会出现在这里" />
      </div>

      <div v-if="total > size" class="pager" style="margin: 0; padding: 16px 0 20px">
        <el-pagination
          background
          layout="prev, pager, next"
          :current-page="page"
          :page-size="size"
          :total="total"
          @current-change="changePage"
        />
      </div>
    </section>

    <el-dialog v-model="rechargeVisible" title="钱包充值" width="min(420px, 92vw)">
      <p class="small muted" style="margin-bottom: 14px">演示环境直接增加余额，不接入真实支付渠道。</p>
      <div class="row wrap" style="gap: 8px; margin-bottom: 14px">
        <button
          v-for="amount in quickAmounts"
          :key="amount"
          type="button"
          class="chip"
          :class="{ active: rechargeAmount === amount }"
          @click="rechargeAmount = amount"
        >
          ¥{{ amount }}
        </button>
      </div>
      <el-input-number v-model="rechargeAmount" :min="1" :max="100000" :precision="2" :step="50" controls-position="right" style="width: 100%" />
      <template #footer>
        <el-button @click="rechargeVisible = false">取消</el-button>
        <el-button type="primary" :loading="recharging" @click="recharge">确认充值</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.ledger-list {
  display: flex;
  flex-direction: column;
}

.ledger-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 20px;
  border-bottom: 1px dashed var(--line);
}

.ledger-row:last-child {
  border-bottom: 0;
}

.ledger-icon {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  flex: 0 0 auto;
  border-radius: 50%;
  background: var(--brand-soft);
  color: var(--brand-deep);
  font-size: 13px;
  font-weight: 700;
}

.amount {
  font-family: var(--font-num);
  font-variant-numeric: lining-nums tabular-nums;
  font-size: 17px;
  font-weight: 700;
}

.amount.income {
  color: var(--brand);
}

.amount.expense {
  color: var(--pop);
}
</style>
