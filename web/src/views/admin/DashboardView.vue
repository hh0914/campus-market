<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { adminApi } from '@/api/admin'
import type { CategoryStat, DashboardSummary, TrendPoint } from '@/types'
import { formatMoney, formatNumber } from '@/utils/format'

const router = useRouter()

const summary = ref<DashboardSummary | null>(null)
const trend = ref<TrendPoint[]>([])
const categories = ref<CategoryStat[]>([])
const loading = ref(true)

const trendRef = ref<HTMLElement | null>(null)
const categoryRef = ref<HTMLElement | null>(null)
let trendChart: echarts.ECharts | null = null
let categoryChart: echarts.ECharts | null = null

const palette = ['#146b52', '#e8622c', '#b98a24', '#4c9179', '#7db3a0', '#c4801f']

function renderTrend() {
  if (!trendRef.value || trend.value.length === 0) {
    return
  }
  trendChart ??= echarts.init(trendRef.value)
  const days = trend.value.map((point) => point.date.slice(5))
  trendChart.setOption({
    color: palette,
    tooltip: { trigger: 'axis' },
    legend: { data: ['订单数', '成交额'], right: 0, top: 0, textStyle: { color: '#77857d' } },
    grid: { left: 8, right: 12, top: 44, bottom: 4, containLabel: true },
    xAxis: {
      type: 'category',
      data: days,
      axisLine: { lineStyle: { color: '#e0d9ca' } },
      axisLabel: { color: '#77857d' },
    },
    yAxis: [
      {
        type: 'value',
        name: '订单',
        splitLine: { lineStyle: { color: '#f0ece1' } },
        axisLabel: { color: '#77857d' },
      },
      {
        type: 'value',
        name: '成交额',
        splitLine: { show: false },
        axisLabel: { color: '#77857d', formatter: (value: number) => `¥${value}` },
      },
    ],
    series: [
      {
        name: '订单数',
        type: 'bar',
        barWidth: 14,
        itemStyle: { borderRadius: [4, 4, 0, 0], color: '#146b52' },
        data: trend.value.map((point) => point.orders),
      },
      {
        name: '成交额',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        symbolSize: 6,
        lineStyle: { width: 2, color: '#e8622c' },
        itemStyle: { color: '#e8622c' },
        areaStyle: { color: 'rgba(232, 98, 44, 0.1)' },
        data: trend.value.map((point) => Number(point.gmv)),
      },
    ],
  })
}

function renderCategories() {
  if (!categoryRef.value || categories.value.length === 0) {
    return
  }
  categoryChart ??= echarts.init(categoryRef.value)
  categoryChart.setOption({
    color: palette,
    tooltip: {
      trigger: 'item',
      formatter: (params: unknown) => {
        const item = params as { name: string; value: number; percent: number }
        return `${item.name}<br/>成交额 ¥${Number(item.value).toFixed(2)}（${item.percent}%）`
      },
    },
    legend: { orient: 'vertical', right: 0, top: 'center', textStyle: { color: '#77857d', fontSize: 12 } },
    series: [
      {
        name: '分类成交额',
        type: 'pie',
        radius: ['46%', '70%'],
        center: ['36%', '52%'],
        avoidLabelOverlap: true,
        itemStyle: { borderColor: '#fffdf8', borderWidth: 2, borderRadius: 6 },
        label: { show: false },
        emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold', formatter: '{b}\n¥{c}' } },
        data: categories.value.map((item) => ({ name: item.categoryName, value: Number(item.gmv) })),
      },
    ],
  })
}

function resize() {
  trendChart?.resize()
  categoryChart?.resize()
}

onMounted(async () => {
  try {
    const [summaryData, trendData, categoryData] = await Promise.all([
      adminApi.summary(),
      adminApi.trend(14),
      adminApi.categoryStats(),
    ])
    summary.value = summaryData
    trend.value = trendData
    categories.value = categoryData
    await nextTick()
    renderTrend()
    renderCategories()
  } finally {
    loading.value = false
  }
  window.addEventListener('resize', resize)
})

onUnmounted(() => {
  window.removeEventListener('resize', resize)
  trendChart?.dispose()
  categoryChart?.dispose()
})
</script>

<template>
  <div v-loading="loading">
    <div class="stat-grid">
      <div class="stat-card accent">
        <div class="label">平台成交额（GMV）</div>
        <div class="value">{{ formatMoney(summary?.gmv) }}</div>
        <div class="hint">近 7 天 {{ formatMoney(summary?.gmv7d) }}</div>
      </div>
      <div class="stat-card">
        <div class="label">注册用户</div>
        <div class="value">{{ formatNumber(summary?.totalUsers) }}</div>
        <div class="hint">近 7 天新增 {{ formatNumber(summary?.newUsers7d) }} · 今日 {{ formatNumber(summary?.todayUsers) }}</div>
      </div>
      <div class="stat-card">
        <div class="label">在售商品</div>
        <div class="value">{{ formatNumber(summary?.onSaleItems) }}</div>
        <div class="hint">待审核 {{ formatNumber(summary?.pendingItems) }} 件</div>
      </div>
      <div class="stat-card">
        <div class="label">订单总数</div>
        <div class="value">{{ formatNumber(summary?.totalOrders) }}</div>
        <div class="hint">已完成 {{ formatNumber(summary?.completedOrders) }} · 今日 {{ formatNumber(summary?.todayOrders) }}</div>
      </div>
      <div class="stat-card">
        <div class="label">待处理举报</div>
        <div class="value">{{ formatNumber(summary?.pendingReports) }}</div>
        <div class="hint">建议 24 小时内响应</div>
      </div>
    </div>

    <div class="todo-row">
      <button type="button" class="todo" @click="router.push({ name: 'admin-items' })">
        <span class="todo-icon">🗂️</span>
        <span>
          <strong>{{ formatNumber(summary?.pendingItems) }} 件商品待审核</strong>
          <small>先审后上架，去处理审核队列 →</small>
        </span>
      </button>
      <button type="button" class="todo" @click="router.push({ name: 'admin-reports' })">
        <span class="todo-icon">🚩</span>
        <span>
          <strong>{{ formatNumber(summary?.pendingReports) }} 条举报待处理</strong>
          <small>核实后可下架商品、警告或封禁账号 →</small>
        </span>
      </button>
    </div>

    <div class="chart-grid">
      <div class="chart-card">
        <div class="row between" style="margin-bottom: 4px">
          <h2 class="chart-title">近 14 天订单与成交额</h2>
          <span class="tiny muted">按日统计</span>
        </div>
        <div ref="trendRef" class="chart" />
      </div>

      <div class="chart-card">
        <div class="row between" style="margin-bottom: 4px">
          <h2 class="chart-title">分类成交额占比</h2>
          <span class="tiny muted">已售出商品</span>
        </div>
        <div v-if="categories.length === 0" class="chart empty-chart">暂无成交数据</div>
        <div v-else ref="categoryRef" class="chart" />
      </div>
    </div>

    <div class="table-card" style="margin-top: 22px">
      <div class="row between" style="padding: 14px 20px; border-bottom: 1px solid var(--line)">
        <h2 class="chart-title">分类成交排行</h2>
        <span class="tiny muted">共 {{ categories.length }} 个分类</span>
      </div>
      <el-table :data="categories" style="width: 100%" empty-text="暂无数据">
        <el-table-column type="index" label="#" width="60" />
        <el-table-column prop="categoryName" label="分类" min-width="160" />
        <el-table-column prop="soldCount" label="成交量" width="120" />
        <el-table-column label="成交额" width="160" align="right">
          <template #default="{ row }">{{ formatMoney(row.gmv) }}</template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<style scoped>
.todo-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 16px;
  margin-top: 18px;
}

.todo {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 18px;
  text-align: left;
  background: var(--card);
  border: 1px solid var(--line);
  border-radius: var(--radius);
  box-shadow: var(--shadow-sm);
  cursor: pointer;
  font-family: inherit;
  transition: border-color 0.16s ease, transform 0.16s ease;
}

.todo:hover {
  border-color: var(--brand);
  transform: translateY(-2px);
}

.todo strong {
  display: block;
  font-size: 14px;
}

.todo small {
  color: var(--muted);
  font-size: 12px;
}

.todo-icon {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  flex: 0 0 auto;
  border-radius: 12px;
  background: var(--brand-soft);
  font-size: 20px;
}

.chart-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(0, 1fr);
  gap: 18px;
  margin-top: 18px;
}

.chart-title {
  font-family: var(--font-display);
  font-size: 17px;
  font-weight: 700;
}

.empty-chart {
  display: grid;
  place-items: center;
  color: var(--muted);
  font-size: 13px;
}

@media (max-width: 1080px) {
  .chart-grid {
    grid-template-columns: 1fr;
  }
}
</style>
