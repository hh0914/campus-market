<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { categoryApi, itemApi } from '@/api/items'
import { usePagination } from '@/composables/usePagination'
import type { Category, ItemSummary } from '@/types'
import { CONDITION_LABELS } from '@/utils/format'
import ItemCard from '@/components/ItemCard.vue'
import EmptyState from '@/components/EmptyState.vue'

const route = useRoute()
const router = useRouter()

const categories = ref<Category[]>([])
const items = ref<ItemSummary[]>([])
const loading = ref(false)
const { page, size, total } = usePagination(12)

function toNumber(value: unknown): number | null {
  const parsed = Number(value)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : null
}

const keyword = ref(typeof route.query.keyword === 'string' ? route.query.keyword : '')
const categoryId = ref<number | null>(toNumber(route.query.categoryId))
const condition = ref<string | null>(typeof route.query.condition === 'string' ? route.query.condition : null)
const minPrice = ref<number | null>(toNumber(route.query.minPrice))
const maxPrice = ref<number | null>(toNumber(route.query.maxPrice))
const sort = ref(typeof route.query.sort === 'string' ? route.query.sort : 'newest')

const conditions = Object.entries(CONDITION_LABELS).map(([value, label]) => ({ value, label }))
const sorts = [
  { value: 'newest', label: '最新发布' },
  { value: 'hot', label: '最多浏览' },
  { value: 'priceAsc', label: '价格从低到高' },
  { value: 'priceDesc', label: '价格从高到低' },
]

const activeCategory = computed(() => categories.value.find((category) => category.id === categoryId.value) ?? null)
const hasFilter = computed(
  () =>
    Boolean(keyword.value.trim()) ||
    categoryId.value !== null ||
    condition.value !== null ||
    minPrice.value !== null ||
    maxPrice.value !== null,
)

function syncQuery() {
  const query: Record<string, string> = {}
  if (keyword.value.trim()) query.keyword = keyword.value.trim()
  if (categoryId.value !== null) query.categoryId = String(categoryId.value)
  if (condition.value) query.condition = condition.value
  if (minPrice.value !== null) query.minPrice = String(minPrice.value)
  if (maxPrice.value !== null) query.maxPrice = String(maxPrice.value)
  if (sort.value !== 'newest') query.sort = sort.value
  router.replace({ name: 'search', query })
}

async function load() {
  loading.value = true
  try {
    const result = await itemApi.search({
      keyword: keyword.value.trim() || undefined,
      categoryId: categoryId.value,
      condition: condition.value,
      minPrice: minPrice.value,
      maxPrice: maxPrice.value,
      sort: sort.value,
      page: page.value,
      size: size.value,
    })
    items.value = result.list
    total.value = result.total
  } catch {
    items.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function apply() {
  page.value = 1
  syncQuery()
  load()
}

function pickCategory(id: number | null) {
  categoryId.value = id
  apply()
}

function pickCondition(value: string | null) {
  condition.value = condition.value === value ? null : value
  apply()
}

function changeSort(value: string) {
  sort.value = value
  apply()
}

function reset() {
  keyword.value = ''
  categoryId.value = null
  condition.value = null
  minPrice.value = null
  maxPrice.value = null
  sort.value = 'newest'
  apply()
}

function changePage(next: number) {
  page.value = next
  load()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function patch(next: ItemSummary) {
  const index = items.value.findIndex((item) => item.id === next.id)
  if (index >= 0) {
    items.value[index] = next
  }
}

onMounted(async () => {
  await load()
  categories.value = await categoryApi.list().catch(() => [])
})
</script>

<template>
  <div class="page-shell page">
    <div class="search-head">
      <div>
        <h1 class="display">逛市场</h1>
        <p class="muted small" style="margin-top: 6px">
          {{ activeCategory ? `正在浏览「${activeCategory.name}」` : '全部闲置' }} · 共 {{ total }} 件
        </p>
      </div>
      <el-button v-if="hasFilter" round plain @click="reset">清空筛选</el-button>
    </div>

    <div class="surface pad filter-panel">
      <form class="row wrap" style="gap: 10px" @submit.prevent="apply">
        <el-input v-model="keyword" placeholder="搜索标题、描述或标签" clearable style="max-width: 360px" @clear="apply">
          <template #prefix>🔍</template>
        </el-input>
        <div class="row" style="gap: 6px">
          <el-input-number v-model="minPrice" :min="0" :controls="false" placeholder="最低价" style="width: 104px" />
          <span class="muted small">—</span>
          <el-input-number v-model="maxPrice" :min="0" :controls="false" placeholder="最高价" style="width: 104px" />
        </div>
        <el-button type="primary" round native-type="submit">筛选</el-button>
        <div style="margin-left: auto">
          <el-select :model-value="sort" style="width: 150px" @change="changeSort">
            <el-option v-for="item in sorts" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </div>
      </form>

      <div class="divider" style="margin: 16px 0" />

      <div class="chip-row">
        <button type="button" class="chip" :class="{ active: categoryId === null }" @click="pickCategory(null)">全部分类</button>
        <button
          v-for="category in categories"
          :key="category.id"
          type="button"
          class="chip"
          :class="{ active: categoryId === category.id }"
          @click="pickCategory(category.id)"
        >
          <span>{{ category.icon ?? '📦' }}</span>
          {{ category.name }}
        </button>
      </div>

      <div class="chip-row" style="margin-top: 10px">
        <button
          v-for="item in conditions"
          :key="item.value"
          type="button"
          class="chip"
          :class="{ active: condition === item.value }"
          @click="pickCondition(item.value)"
        >
          {{ item.label }}
        </button>
      </div>
    </div>

    <div v-loading="loading" style="margin-top: 24px">
      <div v-if="items.length" class="grid-items">
        <ItemCard v-for="item in items" :key="item.id" :item="item" @updated="patch" />
      </div>
      <EmptyState
        v-else-if="!loading"
        icon="🔍"
        title="没有匹配的闲置"
        hint="换个关键词，或者放宽价格和成色条件试试"
      >
        <el-button v-if="hasFilter" round plain style="margin-top: 14px" @click="reset">清空筛选</el-button>
      </EmptyState>
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
  </div>
</template>

<style scoped>
.search-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.search-head h1 {
  font-size: clamp(28px, 3.6vw, 38px);
}

.filter-panel {
  padding: 20px 22px;
}
</style>
