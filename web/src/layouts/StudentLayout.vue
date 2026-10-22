<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notification'
import { chatApi } from '@/api/social'
import UserAvatar from '@/components/UserAvatar.vue'

const router = useRouter()
const auth = useAuthStore()
const notifications = useNotificationStore()

const keyword = ref('')
const searchFocused = ref(false)

const balanceLabel = computed(() => auth.user?.creditScore ?? '—')

onMounted(async () => {
  if (auth.isLoggedIn) {
    await notifications.refresh().catch(() => undefined)
  }
})

function search() {
  router.push({ name: 'search', query: keyword.value ? { keyword: keyword.value } : {} })
}

function openChats() {
  router.push({ name: 'chat' })
}

async function openFirstConversation() {
  if (!auth.isLoggedIn) {
    router.push({ name: 'login' })
    return
  }
  const list = await chatApi.list()
  if (list.length === 0) {
    ElMessage.info('还没有会话，去商品详情页点「聊一聊」开始议价')
    return
  }
  openChats()
}

async function handleCommand(command: string) {
  switch (command) {
    case 'logout':
      await auth.logout()
      notifications.reset()
      ElMessage.success('已退出登录')
      router.push({ name: 'home' })
      break
    case 'orders':
      router.push({ name: 'orders' })
      break
    case 'items':
      router.push({ name: 'my-items' })
      break
    case 'favorites':
      router.push({ name: 'favorites' })
      break
    case 'profile':
      router.push({ name: 'profile' })
      break
    case 'wallet':
      router.push({ name: 'wallet' })
      break
    case 'admin':
      router.push({ name: 'admin-dashboard' })
      break
    default:
      break
  }
}
</script>

<template>
  <header class="site-head">
    <div class="page-shell head-inner">
      <router-link :to="{ name: 'home' }" class="brand" aria-label="校园集首页">
        <span class="brand-mark">集</span>
        <span class="brand-copy">
          <strong>校园集</strong>
          <small>CAMPUS MARKET</small>
        </span>
      </router-link>

      <nav class="nav">
        <router-link :to="{ name: 'home' }">首页</router-link>
        <router-link :to="{ name: 'search' }">逛市场</router-link>
        <router-link v-if="auth.isLoggedIn" :to="{ name: 'my-items' }">我的发布</router-link>
        <router-link v-if="auth.isLoggedIn" :to="{ name: 'orders' }">订单</router-link>
      </nav>

      <form class="head-search" :class="{ focused: searchFocused }" @submit.prevent="search">
        <input
          v-model="keyword"
          type="search"
          placeholder="搜教材、数码、闲置…"
          aria-label="搜索商品"
          @focus="searchFocused = true"
          @blur="searchFocused = false"
        />
        <button type="submit" aria-label="搜索">🔍</button>
      </form>

      <div class="head-actions">
        <el-button type="primary" round @click="router.push({ name: 'item-new' })">＋ 发布闲置</el-button>

        <template v-if="auth.isLoggedIn">
          <button class="icon-btn" type="button" aria-label="消息" @click="openFirstConversation">💬</button>
          <el-badge :value="notifications.unread" :hidden="notifications.unread === 0" :max="99">
            <button class="icon-btn" type="button" aria-label="通知" @click="router.push({ name: 'notifications' })">
              🔔
            </button>
          </el-badge>
          <el-dropdown trigger="click" @command="handleCommand">
            <button class="user-btn" type="button">
              <UserAvatar :name="auth.user?.nickname" :src="auth.user?.avatarUrl" :size="30" />
              <span class="user-name ellipsis">{{ auth.displayName }}</span>
              <span class="credit-dot" :title="`信用分 ${balanceLabel}`">{{ balanceLabel }}</span>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="items">我的发布</el-dropdown-item>
                <el-dropdown-item command="favorites">我的收藏</el-dropdown-item>
                <el-dropdown-item command="orders">我的订单</el-dropdown-item>
                <el-dropdown-item command="wallet">我的钱包</el-dropdown-item>
                <el-dropdown-item v-if="auth.isStaff" command="admin" divided>进入后台</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>

        <template v-else>
          <el-button text @click="router.push({ name: 'login' })">登录</el-button>
          <el-button round plain @click="router.push({ name: 'register' })">注册</el-button>
        </template>
      </div>
    </div>
  </header>

  <main class="site-main">
    <router-view />
  </main>

  <footer class="site-foot">
    <div class="page-shell foot-inner">
      <div class="foot-brand">
        <span class="foot-mark">集</span>
        <div>
          <strong>校园集</strong>
          <p class="tiny muted">校园二手交易与信用评价平台 · 让闲置在校园里继续流动</p>
        </div>
      </div>
      <div class="foot-links">
        <router-link :to="{ name: 'search' }">逛市场</router-link>
        <router-link :to="{ name: 'item-new' }">发布闲置</router-link>
        <router-link :to="{ name: 'orders' }">我的订单</router-link>
        <router-link :to="{ name: 'notifications' }">消息通知</router-link>
      </div>
      <p class="tiny muted foot-note">平台托管交易资金，确认收货后才会结算给卖家 · 遇到纠纷可在商品或用户页发起举报</p>
    </div>
  </footer>
</template>

<style scoped>
.site-head {
  position: sticky;
  top: 0;
  z-index: 40;
  background: rgba(245, 241, 232, 0.86);
  border-bottom: 1px solid var(--line);
  backdrop-filter: blur(10px);
}

.head-inner {
  display: flex;
  align-items: center;
  gap: 22px;
  height: 70px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 0 0 auto;
}

.brand-mark {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  border-radius: 12px;
  background: var(--brand);
  color: #f6f2ea;
  font-family: var(--font-display);
  font-size: 22px;
  font-weight: 700;
  box-shadow: 0 6px 16px rgba(20, 107, 82, 0.25);
}

.brand-copy {
  display: flex;
  flex-direction: column;
  line-height: 1.1;
}

.brand-copy strong {
  font-family: var(--font-display);
  font-size: 19px;
  letter-spacing: 0.02em;
}

.brand-copy small {
  color: var(--muted);
  font-size: 9px;
  letter-spacing: 0.16em;
}

.nav {
  display: flex;
  gap: 4px;
  flex: 0 0 auto;
}

.nav a {
  padding: 7px 12px;
  border-radius: 8px;
  color: var(--ink-soft);
  font-size: 14px;
  transition: all 0.16s ease;
}

.nav a:hover {
  background: rgba(20, 107, 82, 0.08);
  color: var(--brand-deep);
}

.nav a.router-link-exact-active {
  background: rgba(20, 107, 82, 0.09);
  color: var(--brand-deep);
  font-weight: 600;
}

.head-search {
  display: flex;
  align-items: center;
  flex: 1;
  min-width: 120px;
  max-width: 320px;
  margin-left: auto;
  padding: 3px 4px 3px 14px;
  background: var(--card);
  border: 1px solid var(--line);
  border-radius: 999px;
  transition: border-color 0.16s ease, box-shadow 0.16s ease;
}

.head-search.focused {
  border-color: var(--brand);
  box-shadow: 0 0 0 3px rgba(20, 107, 82, 0.1);
}

.head-search input {
  flex: 1;
  min-width: 0;
  border: 0;
  background: transparent;
  outline: none;
  font-size: 13.5px;
  color: var(--ink);
}

.head-search button {
  width: 30px;
  height: 30px;
  border: 0;
  border-radius: 50%;
  background: var(--brand-soft);
  cursor: pointer;
  font-size: 13px;
}

.head-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 0 0 auto;
}

.icon-btn {
  position: relative;
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border: 1px solid var(--line);
  border-radius: 50%;
  background: var(--card);
  cursor: pointer;
  font-size: 15px;
  transition: all 0.16s ease;
}

.icon-btn:hover {
  border-color: var(--brand);
  background: var(--brand-soft);
}

.user-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 3px 10px 3px 3px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: var(--card);
  cursor: pointer;
}

.user-name {
  max-width: 72px;
  font-size: 13px;
}

.credit-dot {
  display: grid;
  place-items: center;
  min-width: 26px;
  height: 20px;
  padding: 0 6px;
  border-radius: 999px;
  background: var(--brand-soft);
  color: var(--brand-deep);
  font-family: var(--font-num);
  font-variant-numeric: lining-nums tabular-nums;
  font-size: 12px;
  font-weight: 700;
}

.site-main {
  min-height: calc(100vh - 70px - 168px);
}

.site-foot {
  margin-top: 8px;
  padding: 30px 0 26px;
  border-top: 1px solid var(--line);
  background: rgba(255, 253, 248, 0.5);
}

.foot-inner {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.foot-brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.foot-brand strong {
  font-family: var(--font-display);
  font-size: 16px;
}

.foot-mark {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border-radius: 10px;
  background: var(--brand-soft);
  color: var(--brand-deep);
  font-family: var(--font-display);
  font-weight: 700;
}

.foot-links {
  display: flex;
  gap: 18px;
  color: var(--ink-soft);
  font-size: 13px;
}

.foot-links a:hover {
  color: var(--brand);
}

.foot-note {
  flex-basis: 100%;
  padding-top: 12px;
  border-top: 1px dashed var(--line);
}

@media (max-width: 1040px) {
  .head-search {
    display: none;
  }

  .nav {
    margin-left: auto;
  }
}

@media (max-width: 760px) {
  .nav,
  .user-name,
  .credit-dot {
    display: none;
  }

  .head-actions {
    margin-left: auto;
  }
}
</style>
