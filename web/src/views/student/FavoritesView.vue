<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { itemApi } from '@/api/items'
import { usePagination } from '@/composables/usePagination'
import type { ItemSummary } from '@/types'
import ItemCard from '@/components/ItemCard.vue'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()

const items = ref<ItemSummary[]>([])
const loading = ref(false)
const { page, size, total } = usePagination(12)

async function load() {
  loading.value = true
  try {
    const result = await itemApi.favorites({ page: page.value, size: size.value })
    items.value = result.list
    total.value = result.total
  } catch {
    items.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function patch(next: ItemSummary) {
  const index = items.value.findIndex((item) => item.id === next.id)
  if (index >= 0) {
    if (next.favorited) {
      items.value[index] = next
    } else {
      items.value.splice(index, 1)
      total.value = Math.max(0, total.value - 1)
    }
  }
}

function changePage(next: number) {
  page.value = next
  load()
}

onMounted(load)
</script>

<template>
  <div class="page-shell page">
    <div class="row between wrap" style="gap: 14px; margin-bottom: 20px">
      <div>
        <h1 class="display" style="font-size: clamp(26px, 3.2vw, 34px)">我的收藏</h1>
        <p class="muted small" style="margin-top: 6px">共 {{ total }} 件 · 商品售出或下架后仍会保留在这里</p>
      </div>
      <el-button round plain @click="router.push({ name: 'search' })">继续逛市场</el-button>
    </div>

    <div v-loading="loading">
      <div v-if="items.length" class="grid-items">
        <ItemCard v-for="item in items" :key="item.id" :item="item" @updated="patch" />
      </div>
    </div>

    <EmptyState v-if="!loading && items.length === 0" icon="♡" title="收藏夹还是空的" hint="看到心动的闲置，点右上角的心形就收进来了">
      <el-button type="primary" round style="margin-top: 14px" @click="router.push({ name: 'search' })">去逛逛</el-button>
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
