<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { itemApi } from '@/api/items'
import { useAuthStore } from '@/stores/auth'
import type { ItemSummary } from '@/types'
import { CONDITION_LABELS, TRADE_METHOD_LABELS, formatMoney } from '@/utils/format'
import StatusTag from './StatusTag.vue'
import UserAvatar from './UserAvatar.vue'

const props = defineProps<{ item: ItemSummary }>()
const emit = defineEmits<{
  updated: [item: ItemSummary]
}>()

const router = useRouter()
const auth = useAuthStore()
const busy = ref(false)
const failed = ref(false)

const cover = computed(() => props.item.coverUrl ?? '')
const condition = computed(() => CONDITION_LABELS[props.item.conditionLevel] ?? props.item.conditionLevel)

async function toggleFavorite() {
  if (!auth.isLoggedIn) {
    router.push({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } })
    return
  }
  if (busy.value) {
    return
  }
  busy.value = true
  try {
    const result = props.item.favorited
      ? await itemApi.unfavorite(props.item.id)
      : await itemApi.favorite(props.item.id)
    emit('updated', { ...props.item, favorited: result.favorited, favoriteCount: result.favoriteCount })
    ElMessage.success(result.favorited ? '已加入收藏' : '已取消收藏')
  } finally {
    busy.value = false
  }
}

function open() {
  router.push({ name: 'item-detail', params: { id: props.item.id } })
}
</script>

<template>
  <article class="item-card" @click="open">
    <div class="thumb">
      <img v-if="cover && !failed" :src="cover" :alt="item.title" loading="lazy" @error="failed = true" />
      <div v-else class="fallback">集</div>
      <StatusTag class="badge" :status="item.status" kind="item" />
      <span class="condition">{{ condition }}</span>
      <span class="peek">查看详情 →</span>
      <button
        class="fav"
        :class="{ on: item.favorited }"
        type="button"
        :aria-label="item.favorited ? '取消收藏' : '收藏'"
        @click.stop="toggleFavorite"
      >
        {{ item.favorited ? '♥' : '♡' }}
      </button>
    </div>

    <div class="body">
      <h3 class="title clamp-2">{{ item.title }}</h3>
      <div class="row wrap" style="gap: 6px">
        <span class="pill plain">{{ item.categoryName ?? '未分类' }}</span>
        <span class="pill plain">{{ TRADE_METHOD_LABELS[item.tradeMethod] ?? item.tradeMethod }}</span>
      </div>
      <div class="foot">
        <div>
          <div class="price" style="font-size: 21px">{{ formatMoney(item.price) }}</div>
          <div v-if="item.originalPrice" class="price-origin">原价 {{ formatMoney(item.originalPrice) }}</div>
        </div>
        <div class="seller">
          <UserAvatar :name="item.sellerNickname" :size="22" />
          <span class="ellipsis" style="max-width: 74px">{{ item.sellerNickname ?? '同学' }}</span>
        </div>
      </div>
    </div>
  </article>
</template>

<style scoped>
.fallback {
  display: grid;
  place-items: center;
  width: 100%;
  height: 100%;
  font-family: var(--font-display);
  font-size: 46px;
  color: rgba(20, 107, 82, 0.35);
  background: linear-gradient(135deg, #e9e2d2, #d9e6de);
}

.fav {
  position: absolute;
  right: 10px;
  top: 10px;
  width: 30px;
  height: 30px;
  display: grid;
  place-items: center;
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 50%;
  background: rgba(255, 253, 248, 0.86);
  color: #8b6a5d;
  font-size: 16px;
  line-height: 1;
  cursor: pointer;
  transition: all 0.16s ease;
}

.fav:hover {
  background: #fff;
  transform: scale(1.06);
}

.fav.on {
  background: var(--pop);
  border-color: var(--pop);
  color: #fff;
}
</style>
