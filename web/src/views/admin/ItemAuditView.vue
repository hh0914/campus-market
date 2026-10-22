<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api/admin'
import { itemApi } from '@/api/items'
import type { ItemDetail, ItemSummary } from '@/types'
import { CONDITION_LABELS, formatDateTime, formatMoney, splitTags } from '@/utils/format'

const items = ref<ItemSummary[]>([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const busyId = ref<number | null>(null)

const drawerVisible = ref(false)
const preview = ref<ItemDetail | null>(null)
const previewLoading = ref(false)

function conditionLabel(level: string) {
  return CONDITION_LABELS[level as keyof typeof CONDITION_LABELS] ?? level
}

async function load() {
  loading.value = true
  try {
    const result = await adminApi.pendingItems({ page: page.value, size: size.value })
    items.value = result.list
    total.value = result.total
  } catch {
    items.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function openPreview(item: ItemSummary) {
  drawerVisible.value = true
  previewLoading.value = true
  preview.value = null
  try {
    preview.value = await itemApi.detail(item.id)
  } finally {
    previewLoading.value = false
  }
}

async function approve(item: { id: number; title: string }) {
  const confirmed = await ElMessageBox.confirm(`通过《${item.title}》的审核并上架？`, '审核通过', {
    confirmButtonText: '通过',
    cancelButtonText: '取消',
  }).catch(() => false)
  if (!confirmed) {
    return
  }
  busyId.value = item.id
  try {
    await adminApi.approveItem(item.id)
    ElMessage.success('已通过审核，商品已上架')
    drawerVisible.value = false
    await load()
  } finally {
    busyId.value = null
  }
}

async function reject(item: { id: number; title: string }) {
  const result = await ElMessageBox.prompt('填写驳回原因，卖家会在通知中看到。', `驳回《${item.title}》`, {
    confirmButtonText: '确认驳回',
    cancelButtonText: '取消',
    inputPlaceholder: '例如：描述与图片不符，请补充实拍图',
    inputValidator: (value) => (value && value.trim().length >= 2 ? true : '驳回原因至少 2 个字'),
  }).catch(() => null)
  if (!result) {
    return
  }
  busyId.value = item.id
  try {
    await adminApi.rejectItem(item.id, result.value.trim())
    ElMessage.success('已驳回，卖家会收到通知')
    drawerVisible.value = false
    await load()
  } finally {
    busyId.value = null
  }
}

function changePage(next: number) {
  page.value = next
  load()
}

onMounted(load)
</script>

<template>
  <div>
    <div class="row between wrap" style="gap: 12px; margin-bottom: 16px">
      <div>
        <p class="small muted">先审后上架，共 <strong>{{ total }}</strong> 件商品待处理</p>
      </div>
      <el-button round plain :loading="loading" @click="load">刷新队列</el-button>
    </div>

    <div class="table-card">
      <el-table v-loading="loading" :data="items" style="width: 100%" empty-text="审核队列已清空">
        <el-table-column label="商品" min-width="300">
          <template #default="{ row }">
            <div class="row" style="gap: 12px">
              <div class="thumb">
                <img v-if="row.coverUrl" :src="row.coverUrl" :alt="row.title" />
                <span v-else class="thumb-fallback">集</span>
              </div>
              <div style="min-width: 0">
                <div class="ellipsis" style="font-weight: 600">{{ row.title }}</div>
                <div class="tiny muted">{{ row.categoryName ?? '未分类' }} · {{ conditionLabel(row.conditionLevel) }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="卖家" width="140">
          <template #default="{ row }">{{ row.sellerNickname ?? '—' }}</template>
        </el-table-column>
        <el-table-column label="价格" width="120" align="right">
          <template #default="{ row }">
            <span class="price" style="font-size: 16px">{{ formatMoney(row.price) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="170">
          <template #default="{ row }">
            <span class="tiny muted">{{ formatDateTime(row.createdAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" align="right">
          <template #default="{ row }">
            <el-button size="small" round @click="openPreview(row)">预览</el-button>
            <el-button size="small" type="primary" round :loading="busyId === row.id" @click="approve(row)">通过</el-button>
            <el-button size="small" type="danger" round plain :loading="busyId === row.id" @click="reject(row)">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

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

    <el-drawer v-model="drawerVisible" title="商品审核详情" size="min(560px, 94vw)">
      <div v-loading="previewLoading">
        <template v-if="preview">
          <div class="preview-cover">
            <img v-if="preview.images.length" :src="preview.images[0]" :alt="preview.title" />
            <span v-else class="thumb-fallback">无实拍图</span>
          </div>
          <div v-if="preview.images.length > 1" class="preview-thumbs">
            <img v-for="url in preview.images.slice(1)" :key="url" :src="url" :alt="preview.title" />
          </div>

          <h2 class="display" style="font-size: 22px; margin-top: 16px">{{ preview.title }}</h2>
          <div class="row wrap" style="gap: 10px; margin-top: 8px">
            <span class="pill">{{ preview.categoryName ?? '未分类' }}</span>
            <span class="pill plain">{{ conditionLabel(preview.conditionLevel) }}</span>
            <span class="pill plain">{{ preview.tradeMethod === 'MEETUP' ? '当面交易' : '快递寄送' }}</span>
            <span class="pill plain">{{ preview.campus ?? '未填写校区' }}</span>
          </div>

          <div class="row" style="gap: 14px; align-items: baseline; margin-top: 12px">
            <span class="price" style="font-size: 28px">{{ formatMoney(preview.price) }}</span>
            <span v-if="preview.originalPrice" class="price-origin">原价 {{ formatMoney(preview.originalPrice) }}</span>
          </div>

          <div class="row" style="gap: 8px; margin-top: 14px">
            <span class="tiny muted">卖家 {{ preview.sellerNickname ?? '—' }}</span>
            <span v-if="preview.sellerCreditScore !== null && preview.sellerCreditScore !== undefined" class="pill plain">
              信用分 {{ preview.sellerCreditScore }}
            </span>
          </div>

          <div class="divider" />
          <h3 class="small" style="font-weight: 600">商品描述</h3>
          <p class="description-body small" style="margin-top: 8px">{{ preview.description }}</p>

          <div v-if="splitTags(preview.tags).length" class="row wrap" style="gap: 6px; margin-top: 12px">
            <span v-for="tag in splitTags(preview.tags)" :key="tag" class="pill plain">{{ tag }}</span>
          </div>

          <div class="row" style="gap: 10px; margin-top: 24px">
            <el-button type="primary" round style="flex: 1" :loading="busyId === preview.id" @click="approve(preview)">通过并上架</el-button>
            <el-button type="danger" plain round style="flex: 1" :loading="busyId === preview.id" @click="reject(preview)">驳回</el-button>
          </div>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.thumb {
  width: 52px;
  height: 52px;
  flex: 0 0 auto;
  border: 1px solid var(--line);
  border-radius: 8px;
  overflow: hidden;
  background: var(--paper-deep);
}

.thumb img,
.preview-thumbs img {
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
  font-size: 18px;
  color: rgba(20, 107, 82, 0.35);
}

.preview-cover {
  aspect-ratio: 4 / 3;
  border-radius: 12px;
  overflow: hidden;
  background: var(--paper-deep);
  border: 1px solid var(--line);
}

.preview-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.preview-thumbs {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  margin-top: 8px;
}

.preview-thumbs img {
  aspect-ratio: 1;
  border-radius: 8px;
  border: 1px solid var(--line);
}
</style>
