<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { itemApi } from '@/api/items'
import { reportApi, userApi } from '@/api/social'
import { useAuthStore } from '@/stores/auth'
import type { ItemSummary, PublicProfile, ReportReason, Review } from '@/types'
import { REPORT_REASON_LABELS, formatDate, formatNumber, splitTags } from '@/utils/format'
import CreditBadge from '@/components/CreditBadge.vue'
import UserAvatar from '@/components/UserAvatar.vue'
import ItemCard from '@/components/ItemCard.vue'
import EmptyState from '@/components/EmptyState.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const profile = ref<PublicProfile | null>(null)
const items = ref<ItemSummary[]>([])
const reviews = ref<Review[]>([])
const reviewTotal = ref(0)
const reviewPage = ref(1)
const loading = ref(true)
const tab = ref<'items' | 'reviews'>('items')

const reportVisible = ref(false)
const reportBusy = ref(false)
const reportForm = reactive({
  reasonType: 'ABUSE' as ReportReason,
  description: '',
})

const reasons = Object.entries(REPORT_REASON_LABELS).map(([value, label]) => ({ value, label }))

const userId = computed(() => String(route.params.id))
const isSelf = computed(() => auth.user?.id === profile.value?.user.id)

async function loadProfile() {
  loading.value = true
  try {
    profile.value = await userApi.profile(userId.value)
    const page = await itemApi.bySeller(userId.value, { page: 1, size: 12 }).catch(() => null)
    items.value = page?.list ?? []
    await loadReviews(1)
  } catch {
    ElMessage.error('用户不存在或已注销')
    router.replace({ name: 'home' })
  } finally {
    loading.value = false
  }
}

async function loadReviews(page: number) {
  reviewPage.value = page
  const result = await userApi.reviews(userId.value, { page, size: 5 }).catch(() => null)
  reviews.value = result?.list ?? []
  reviewTotal.value = result?.total ?? 0
}

function patch(next: ItemSummary) {
  const index = items.value.findIndex((item) => item.id === next.id)
  if (index >= 0) {
    items.value[index] = next
  }
}

function openReport() {
  if (!auth.isLoggedIn) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  reportVisible.value = true
}

async function submitReport() {
  if (!profile.value) {
    return
  }
  if (reportForm.description.trim().length < 5) {
    ElMessage.warning('请至少写 5 个字说明问题')
    return
  }
  reportBusy.value = true
  try {
    await reportApi.create({
      targetType: 'USER',
      targetId: profile.value.user.id,
      reasonType: reportForm.reasonType,
      description: reportForm.description.trim(),
    })
    ElMessage.success('举报已提交，审核员会尽快核实')
    reportVisible.value = false
    reportForm.description = ''
  } finally {
    reportBusy.value = false
  }
}

watch(
  () => route.params.id,
  () => {
    if (route.name === 'user-profile') {
      tab.value = 'items'
      loadProfile()
    }
  },
)

onMounted(loadProfile)
</script>

<template>
  <div class="page-shell page">
    <div v-if="loading" v-loading="true" style="min-height: 380px" />

    <template v-else-if="profile">
      <div class="surface profile-head">
        <UserAvatar :name="profile.user.nickname" :src="profile.user.avatarUrl" :size="78" />
        <div style="flex: 1; min-width: 0">
          <div class="row wrap" style="gap: 10px">
            <h1 class="display" style="font-size: clamp(24px, 3vw, 32px)">{{ profile.user.nickname }}</h1>
            <CreditBadge :score="profile.reputation.creditScore" :badge="profile.reputation.badge" size="md" />
            <span v-if="profile.verified" class="pill">✓ 校园认证</span>
            <span v-else class="pill plain">未认证</span>
          </div>
          <p class="muted small" style="margin-top: 6px">
            {{ profile.user.campus ?? '未填写校区' }} · 学号 {{ profile.user.studentNo }}
          </p>
          <div class="row wrap stats-row">
            <div class="stat"><strong>{{ formatNumber(profile.onSaleItems) }}</strong><span>在售</span></div>
            <div class="stat"><strong>{{ formatNumber(profile.reputation.completedDeals) }}</strong><span>完成交易</span></div>
            <div class="stat"><strong>{{ profile.reputation.avgRating?.toFixed(1) ?? '—' }}</strong><span>平均评分</span></div>
            <div class="stat"><strong>{{ formatNumber(profile.reputation.totalReviews) }}</strong><span>收到评价</span></div>
          </div>
        </div>
        <div class="row" style="gap: 8px; align-self: flex-start">
          <el-button v-if="isSelf" round @click="router.push({ name: 'profile' })">编辑我的资料</el-button>
          <el-button v-else round plain @click="openReport">举报用户</el-button>
        </div>
      </div>

      <div class="profile-grid">
        <aside class="surface pad reputation-card">
          <h2 class="display" style="font-size: 20px">信用档案</h2>
          <div class="score-ring">
            <span class="score">{{ profile.reputation.creditScore }}</span>
            <span class="tiny muted">信用分</span>
          </div>
          <p class="center small" style="margin-top: 4px">{{ profile.reputation.badgeLabel }}</p>

          <div class="divider" />

          <dl class="rep-list">
            <div><dt>完成交易</dt><dd>{{ profile.reputation.completedDeals }}</dd></div>
            <div><dt>取消订单</dt><dd>{{ profile.reputation.cancelledDeals }}</dd></div>
            <div><dt>退款订单</dt><dd>{{ profile.reputation.refundedDeals }}</dd></div>
            <div><dt>好评 / 差评</dt><dd>{{ profile.reputation.goodReviews }} / {{ profile.reputation.badReviews }}</dd></div>
            <div><dt>纠纷次数</dt><dd>{{ profile.reputation.disputeCount }}</dd></div>
            <div><dt>被举报成立</dt><dd>{{ profile.reputation.reportCount }}</dd></div>
          </dl>

          <p class="form-hint" style="margin-top: 12px">
            信用分由完成交易、评价与违规记录共同计算，低于 60 分将暂停发布权限。
          </p>
        </aside>

        <section class="surface pad">
          <div class="row between" style="margin-bottom: 8px">
            <el-tabs v-model="tab" style="flex: 1">
              <el-tab-pane label="在售商品" name="items" />
              <el-tab-pane :label="`收到的评价（${reviewTotal}）`" name="reviews" />
            </el-tabs>
          </div>

          <div v-if="tab === 'items'">
            <div v-if="items.length" class="grid-items">
              <ItemCard v-for="item in items" :key="item.id" :item="item" @updated="patch" />
            </div>
            <EmptyState v-else icon="🌱" title="TA 暂时没有在售商品" hint="关注一下，下次上新就能看到" />
          </div>

          <div v-else>
            <div v-if="reviews.length" class="stack" style="gap: 0">
              <article v-for="review in reviews" :key="review.id" class="review-item">
                <div class="row between wrap" style="gap: 10px">
                  <div class="row" style="gap: 10px">
                    <UserAvatar :name="review.reviewerNickname" :size="34" />
                    <div>
                      <strong class="small">{{ review.reviewerNickname ?? '匿名同学' }}</strong>
                      <div class="tiny muted">{{ formatDate(review.createdAt) }}</div>
                    </div>
                  </div>
                  <el-rate :model-value="review.rating" disabled size="small" />
                </div>
                <p v-if="review.comment" class="small ink-soft" style="margin-top: 10px">{{ review.comment }}</p>
                <div v-if="splitTags(review.tags).length" class="row wrap" style="gap: 6px; margin-top: 8px">
                  <span v-for="tag in splitTags(review.tags)" :key="tag" class="pill plain">{{ tag }}</span>
                </div>
              </article>
            </div>
            <EmptyState v-else icon="💬" title="还没有收到评价" hint="完成交易后，买卖双方可以互相评价" />

            <div v-if="reviewTotal > 5" class="pager">
              <el-pagination
                background
                layout="prev, pager, next"
                :current-page="reviewPage"
                :page-size="5"
                :total="reviewTotal"
                @current-change="loadReviews"
              />
            </div>
          </div>
        </section>
      </div>
    </template>

    <el-dialog v-model="reportVisible" title="举报用户" width="min(500px, 92vw)">
      <el-form label-position="top">
        <el-form-item label="举报类型">
          <el-select v-model="reportForm.reasonType" style="width: 100%">
            <el-option v-for="reason in reasons" :key="reason.value" :label="reason.label" :value="reason.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="问题描述">
          <el-input
            v-model="reportForm.description"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="请描述发生了什么，例如：收款后不发货、辱骂骚扰等"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reportVisible = false">取消</el-button>
        <el-button type="primary" :loading="reportBusy" @click="submitReport">提交举报</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.profile-head {
  display: flex;
  gap: 22px;
  align-items: center;
  padding: 26px 28px;
}

.stats-row {
  display: flex;
  gap: 26px;
  margin-top: 16px;
}

.stat {
  display: flex;
  flex-direction: column;
}

.stat strong {
  font-family: var(--font-num);
  font-variant-numeric: lining-nums tabular-nums;
  font-size: 22px;
  line-height: 1.1;
}

.stat span {
  color: var(--muted);
  font-size: 12px;
}

.profile-grid {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 22px;
  margin-top: 22px;
  align-items: start;
}

.score-ring {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 132px;
  height: 132px;
  margin: 18px auto 6px;
  border-radius: 50%;
  border: 8px solid var(--brand-soft);
  border-top-color: var(--brand);
  background: #fff;
}

.score {
  font-family: var(--font-num);
  font-variant-numeric: lining-nums tabular-nums;
  font-size: 36px;
  font-weight: 700;
  line-height: 1;
  color: var(--brand-deep);
}

.rep-list {
  display: grid;
  gap: 8px;
  margin: 0;
}

.rep-list div {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  font-size: 13px;
}

.rep-list dt {
  color: var(--muted);
}

.rep-list dd {
  margin: 0;
  font-weight: 600;
}

.review-item {
  padding: 16px 0;
  border-bottom: 1px dashed var(--line);
}

.review-item:last-child {
  border-bottom: 0;
}

@media (max-width: 900px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }

  .profile-head {
    flex-wrap: wrap;
  }
}
</style>
