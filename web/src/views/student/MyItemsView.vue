<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { itemApi } from '@/api/items'
import { usePagination } from '@/composables/usePagination'
import type { ItemStatus, ItemSummary } from '@/types'
import { ITEM_STATUS_LABELS, formatMoney, formatNumber, relativeTime } from '@/utils/format'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()

const items = ref<ItemSummary[]>([])
const loading = ref(false)
const busyId = ref<number | null>(null)
const status = ref<ItemStatus | ''>('')
const { page, size, total } = usePagination(10)

const tabs: { value: ItemStatus | ''; label: string }[] = [
  { value: '', label: '全部' },
  { value: 'ON_SALE', label: ITEM_STATUS_LABELS.ON_SALE },
  { value: 'PENDING_REVIEW', label: ITEM_STATUS_LABELS.PENDING_REVIEW },
  { value: 'DRAFT', label: ITEM_STATUS_LABELS.DRAFT },
  { value: 'REJECTED', label: ITEM_STATUS_LABELS.REJECTED },
  { value: 'LOCKED', label: ITEM_STATUS_LABELS.LOCKED },
  { value: 'SOLD', label: ITEM_STATUS_LABELS.SOLD },
  { value: 'OFF_SHELF', label: ITEM_STATUS_LABELS.OFF_SHELF },
]

async function load() {
  loading.value = true
  try {
    const result = await itemApi.mine({ status: status.value || undefined, page: page.value, size: size.value })
    items.value = result.list
    total.value = result.total
  } catch {
    items.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function pick(next: ItemStatus | '') {
  status.value = next
  page.value = 1
  load()
}

function changePage(next: number) {
  page.value = next
  load()
}

async function submit(item: ItemSummary) {
  const confirmed = await ElMessageBox.confirm(`将《${item.title}》提交审核？审核通过后自动上架。`, '提交审核', {
    confirmButtonText: '提交',
    cancelButtonText: '取消',
  }).catch(() => false)
  if (!confirmed) {
    return
  }
  busyId.value = item.id
  try {
    await itemApi.submit(item.id)
    ElMessage.success('已提交审核')
    await load()
  } finally {
    busyId.value = null
  }
}

async function offShelf(item: ItemSummary) {
  const confirmed = await ElMessageBox.confirm(`确认下架《${item.title}》？`, '下架商品', {
    confirmButtonText: '下架',
    cancelButtonText: '取消',
    type: 'warning',
  }).catch(() => false)
  if (!confirmed) {
    return
  }
  busyId.value = item.id
  try {
    await itemApi.offShelf(item.id)
    ElMessage.success('已下架')
    await load()
  } finally {
    busyId.value = null
  }
}

onMounted(load)
</script>

<template>
  <div class="page-shell page">
    <div class="row between wrap" style="gap: 14px; margin-bottom: 20px">
      <div>
        <h1 class="display" style="font-size: clamp(26px, 3.2vw, 34px)">我的发布</h1>
        <p class="muted small" style="margin-top: 6px">共 {{ total }} 件 · 审核通过后才会出现在市场</p>
      </div>
      <el-button type="primary" round @click="router.push({ name: 'item-new' })">＋ 发布闲置</el-button>
    </div>

    <div class="chip-row" style="margin-bottom: 18px">
      <button
        v-for="tab in tabs"
        :key="tab.value || 'all'"
        type="button"
        class="chip"
        :class="{ active: status === tab.value }"
        @click="pick(tab.value)"
      >
        {{ tab.label }}
      </button>
    </div>

    <div v-loading="loading" class="stack">
      <article v-for="item in items" :key="item.id" class="surface item-row">
        <button type="button" class="thumb" @click="router.push({ name: 'item-detail', params: { id: item.id } })">
          <img v-if="item.coverUrl" :src="item.coverUrl" :alt="item.title" />
          <span v-else class="thumb-fallback">集</span>
        </button>

        <div class="row-main">
          <div class="row wrap" style="gap: 8px">
            <StatusTag :status="item.status" />
            <span class="tiny muted">{{ relativeTime(item.createdAt) }}发布</span>
          </div>
          <h3 class="title ellipsis" style="margin-top: 6px">{{ item.title }}</h3>
          <div class="row wrap" style="gap: 14px; margin-top: 6px">
            <span class="price" style="font-size: 20px">{{ formatMoney(item.price) }}</span>
            <span class="tiny muted">浏览 {{ formatNumber(item.viewCount) }}</span>
            <span class="tiny muted">收藏 {{ formatNumber(item.favoriteCount) }}</span>
          </div>
        </div>

        <div class="row actions">
          <el-button size="small" round @click="router.push({ name: 'item-detail', params: { id: item.id } })">查看</el-button>
          <el-button
            v-if="item.status === 'DRAFT' || item.status === 'REJECTED' || item.status === 'OFF_SHELF'"
            size="small"
            round
            @click="router.push({ name: 'item-edit', params: { id: item.id } })"
          >
            编辑
          </el-button>
          <el-button
            v-if="item.status === 'DRAFT' || item.status === 'REJECTED' || item.status === 'OFF_SHELF'"
            size="small"
            round
            type="primary"
            :loading="busyId === item.id"
            @click="submit(item)"
          >
            提交审核
          </el-button>
          <el-button
            v-if="item.status === 'ON_SALE'"
            size="small"
            round
            plain
            :loading="busyId === item.id"
            @click="offShelf(item)"
          >
            下架
          </el-button>
        </div>
      </article>
    </div>

    <EmptyState
      v-if="!loading && items.length === 0"
      icon="🗂️"
      :title="status ? '这个状态下还没有商品' : '你还没有发布过闲置'"
      hint="拍几张实拍图，写清成色，很快就能成交"
    >
      <el-button type="primary" round style="margin-top: 14px" @click="router.push({ name: 'item-new' })">
        发布第一件闲置
      </el-button>
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
.item-row {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 16px 18px;
}

.thumb {
  width: 104px;
  height: 78px;
  flex: 0 0 auto;
  padding: 0;
  border: 1px solid var(--line);
  border-radius: 12px;
  overflow: hidden;
  background: var(--paper-deep);
  cursor: pointer;
  transition: border-color 0.16s ease, transform 0.16s ease;
}

.thumb:hover {
  border-color: var(--brand);
  transform: translateY(-2px);
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
  font-size: 30px;
  color: rgba(20, 107, 82, 0.35);
}

.row-main {
  flex: 1;
  min-width: 0;
}

.title {
  font-size: 16px;
  font-weight: 600;
}

.actions {
  flex: 0 0 auto;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
}

@media (max-width: 760px) {
  .item-row {
    flex-wrap: wrap;
  }

  .actions {
    width: 100%;
    justify-content: flex-start;
  }
}
</style>
