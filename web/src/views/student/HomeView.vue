<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { categoryApi, itemApi } from '@/api/items'
import type { Category, ItemSummary } from '@/types'
import ItemCard from '@/components/ItemCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import { formatMoney } from '@/utils/format'

const router = useRouter()

const keyword = ref('')
const categories = ref<Category[]>([])
const latest = ref<ItemSummary[]>([])
const hot = ref<ItemSummary[]>([])
const loading = ref(true)
const failed = ref(false)

function patch(list: ItemSummary[], next: ItemSummary) {
  const index = list.findIndex((item) => item.id === next.id)
  if (index >= 0) {
    list[index] = next
  }
}

function search() {
  router.push({ name: 'search', query: keyword.value.trim() ? { keyword: keyword.value.trim() } : {} })
}

function openCategory(id: number) {
  router.push({ name: 'search', query: { categoryId: String(id) } })
}

onMounted(async () => {
  try {
    const [categoryList, newestPage, hotList] = await Promise.all([
      categoryApi.list(),
      itemApi.search({ sort: 'newest', page: 1, size: 8 }),
      itemApi.search({ sort: 'hot', page: 1, size: 4 }),
    ])
    categories.value = categoryList.filter((category) => category.enabled)
    latest.value = newestPage.list
    hot.value = hotList.list
  } catch {
    failed.value = true
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div>
    <section class="hero">
      <div class="page-shell hero-grid">
        <div>
          <span class="hero-eyebrow">🌿 校园内部流转 · 学生实名认证</span>
          <h1 class="display">
            闲置不闲，<br />
            在校园里找到<em>下一位主人</em>
          </h1>
          <p class="lead">
            教材、数码、宿舍电器、运动装备……用不上的东西，让同校同学用合理的价格接手。平台托管资金，确认收货才结算。
          </p>

          <form class="hero-search" @submit.prevent="search">
            <input v-model="keyword" type="search" placeholder="搜一搜：高数教材、iPad、电煮锅…" aria-label="搜索商品" />
            <el-button type="primary" round size="large" @click="search">搜索</el-button>
          </form>

          <div class="hero-notes">
            <span>🛡️ 资金托管</span>
            <span>🤝 面交 / 快递</span>
            <span>⭐ 双向信用评价</span>
            <span>🚩 举报有回应</span>
          </div>
        </div>

        <div class="hero-art">
          <span class="hero-stamp">🛍️ 今日上新 · 8 件闲置待认领</span>
          <div class="shot main">
            <img src="/images/items/ipad-air-4-1.svg" alt="iPad Air 4 深空灰实拍示意图" />
            <div class="shot-meta">
              <strong>iPad Air 4 · 深空灰</strong>
              <span class="price">{{ formatMoney(1980) }}</span>
            </div>
          </div>
          <div class="shot-row">
            <div class="shot">
              <img src="/images/items/keychron-k2-1.svg" alt="Keychron K2 机械键盘实拍示意图" />
              <div class="shot-meta">
                <strong>Keychron K2</strong>
                <span class="price">{{ formatMoney(260) }}</span>
              </div>
            </div>
            <div class="shot">
              <img src="/images/items/advanced-math-1.svg" alt="高等数学教材实拍示意图" />
              <div class="shot-meta">
                <strong>高数 同济七版</strong>
                <span class="price">{{ formatMoney(25) }}</span>
              </div>
            </div>
          </div>
          <div class="hero-note">🛡️ 资金托管 · 确认收货后才结算给卖家</div>
        </div>
      </div>

      <div class="page-shell">
        <div class="trust-strip">
          <div class="cell">
            <strong>学生身份认证</strong>
            <span>校内邮箱验证后才能发布商品</span>
          </div>
          <div class="cell">
            <strong>资金托管</strong>
            <span>下单即冻结，确认收货再结算</span>
          </div>
          <div class="cell">
            <strong>信用分体系</strong>
            <span>交易与评价共同决定曝光权重</span>
          </div>
          <div class="cell">
            <strong>人工审核</strong>
            <span>商品先审后上架，举报 24h 内响应</span>
          </div>
        </div>
      </div>
    </section>

    <section class="page-shell section">
      <div class="section-head">
        <div>
          <h2>按分类逛逛</h2>
          <p class="sub">从熟悉的角落开始，几秒钟找到你需要的</p>
        </div>
        <router-link :to="{ name: 'search' }" class="small muted">全部分类 →</router-link>
      </div>
      <div v-loading="loading" class="category-grid">
        <button
          v-for="category in categories"
          :key="category.id"
          type="button"
          class="category-tile"
          @click="openCategory(category.id)"
        >
          <span class="icon">{{ category.icon ?? '📦' }}</span>
          <span style="font-weight: 600; font-size: 14px">{{ category.name }}</span>
        </button>
      </div>
    </section>

    <section class="page-shell section">
      <div class="section-head">
        <div>
          <h2>刚上架</h2>
          <p class="sub">审核通过的新鲜闲置，先到先得</p>
        </div>
        <router-link :to="{ name: 'search', query: { sort: 'newest' } }" class="small muted">查看更多 →</router-link>
      </div>

      <div v-if="latest.length" v-loading="loading" class="grid-items">
        <ItemCard v-for="item in latest" :key="item.id" :item="item" @updated="patch(latest, $event)" />
      </div>
      <EmptyState
        v-else-if="!loading"
        icon="🗂️"
        title="货架还是空的"
        :hint="failed ? '连接后端失败，请确认服务已启动' : '成为第一个发布闲置的同学吧'"
      >
        <el-button type="primary" round style="margin-top: 14px" @click="router.push({ name: 'item-new' })">
          去发布
        </el-button>
      </EmptyState>
    </section>

    <section v-if="hot.length" class="page-shell section">
      <div class="section-head">
        <div>
          <h2>大家都在看</h2>
          <p class="sub">浏览量最高的闲置，手慢无</p>
        </div>
        <router-link :to="{ name: 'search', query: { sort: 'hot' } }" class="small muted">更多热门 →</router-link>
      </div>
      <div class="grid-items">
        <ItemCard v-for="item in hot" :key="item.id" :item="item" @updated="patch(hot, $event)" />
      </div>
    </section>

    <section class="page-shell section">
      <div class="cta-band">
        <div>
          <h2 class="display">有闲置？三分钟挂上货架。</h2>
          <p class="small" style="color: rgba(246, 242, 234, 0.78)">
            上传实拍图、写清成色与校区，审核通过后立刻出现在「刚上架」。
          </p>
        </div>
        <el-button size="large" round class="cta-btn" @click="router.push({ name: 'item-new' })">＋ 发布闲置</el-button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.cta-band {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 32px 36px;
  border-radius: var(--radius-lg);
  background:
    radial-gradient(500px 240px at 92% 10%, rgba(232, 98, 44, 0.25), transparent 65%),
    linear-gradient(135deg, #0c4a39, #146b52);
  color: #f6f2ea;
  box-shadow: var(--shadow);
}

.cta-band h2 {
  font-size: clamp(22px, 2.8vw, 30px);
}

.cta-band p {
  margin-top: 8px;
}

.cta-btn {
  background: #f6f2ea;
  border-color: #f6f2ea;
  color: var(--brand-deep);
  font-weight: 700;
}

.cta-btn:hover {
  background: #fff;
  border-color: #fff;
  color: var(--brand-deep);
}
</style>
