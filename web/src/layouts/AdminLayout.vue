<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notification'
import UserAvatar from '@/components/UserAvatar.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const notifications = useNotificationStore()

const collapsed = ref(false)

const title = computed(() => (route.meta.title as string | undefined) ?? '运营后台')

const menus = computed(() =>
  [
    { name: 'admin-dashboard', label: '数据看板', icon: '📊', roles: ['MODERATOR', 'ADMIN'] },
    { name: 'admin-items', label: '商品审核', icon: '🗂️', roles: ['MODERATOR', 'ADMIN'] },
    { name: 'admin-reports', label: '举报工单', icon: '🚩', roles: ['MODERATOR', 'ADMIN'] },
    { name: 'admin-users', label: '用户治理', icon: '👥', roles: ['MODERATOR', 'ADMIN'] },
    { name: 'admin-categories', label: '分类维护', icon: '🏷️', roles: ['ADMIN'] },
    { name: 'admin-audit-logs', label: '审计日志', icon: '📜', roles: ['MODERATOR', 'ADMIN'] },
  ].filter((menu) => auth.user && menu.roles.includes(auth.user.role)),
)

onMounted(async () => {
  await notifications.refresh().catch(() => undefined)
})

function goStudent() {
  router.push({ name: 'home' })
}

async function logout() {
  await auth.logout()
  notifications.reset()
  ElMessage.success('已退出登录')
  router.push({ name: 'login' })
}
</script>

<template>
  <div class="admin-shell" :class="{ collapsed }">
    <aside class="admin-side">
      <router-link :to="{ name: 'admin-dashboard' }" class="brand">
        <div class="brand-mark">集</div>
        <div class="brand-copy">
          <strong>校园集 · 后台</strong>
          <small>运营控制台</small>
        </div>
      </router-link>

      <div class="menu-group">运营</div>
      <router-link v-for="menu in menus" :key="menu.name" :to="{ name: menu.name }" :title="menu.label">
        <span class="ico">{{ menu.icon }}</span>
        <span class="label">{{ menu.label }}</span>
      </router-link>

      <div class="side-foot">
        <div class="row" style="gap: 8px">
          <UserAvatar :name="auth.user?.nickname" :size="30" />
          <div style="min-width: 0">
            <div class="ellipsis" style="color: #f6f2ea">{{ auth.displayName }}</div>
            <div class="tiny" style="color: rgba(246, 242, 234, 0.5)">
              {{ auth.user?.role === 'ADMIN' ? '管理员' : '审核员' }}
            </div>
          </div>
        </div>
        <div class="row foot-actions" style="gap: 6px; margin-top: 10px">
          <el-button size="small" text style="color: rgba(246, 242, 234, 0.8)" @click="goStudent">回到前台</el-button>
          <el-button size="small" text style="color: rgba(246, 242, 234, 0.8)" @click="logout">退出</el-button>
        </div>
      </div>
    </aside>

    <div class="admin-main">
      <header class="admin-topbar">
        <div class="row" style="gap: 12px">
          <button class="toggle" type="button" aria-label="折叠菜单" @click="collapsed = !collapsed">☰</button>
          <h1 class="page-title">{{ title }}</h1>
        </div>
        <div class="row" style="gap: 10px">
          <el-badge :value="notifications.unread" :hidden="notifications.unread === 0" :max="99">
            <el-button size="small" round @click="router.push({ name: 'notifications' })">通知</el-button>
          </el-badge>
          <el-button size="small" round @click="goStudent">学生端视图</el-button>
        </div>
      </header>

      <main class="admin-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<style scoped>
.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 4px 10px 18px;
  color: #f6f2ea;
}

.brand-mark {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  flex: 0 0 auto;
  border-radius: 10px;
  background: var(--brand);
  font-family: var(--font-display);
  font-size: 19px;
  font-weight: 700;
}

.brand-copy strong {
  display: block;
  font-size: 14px;
}

.brand-copy small {
  color: rgba(246, 242, 234, 0.5);
  font-size: 10px;
  letter-spacing: 0.08em;
}

.ico {
  font-size: 14px;
  line-height: 1;
}

.toggle {
  width: 30px;
  height: 30px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--card);
  cursor: pointer;
  line-height: 1;
}

.admin-shell.collapsed {
  grid-template-columns: 78px minmax(0, 1fr);
}

.admin-shell.collapsed .brand-copy,
.admin-shell.collapsed .menu-group,
.admin-shell.collapsed a .label,
.admin-shell.collapsed .side-foot .row > div,
.admin-shell.collapsed .foot-actions {
  display: none;
}

.admin-shell.collapsed .admin-side a {
  justify-content: center;
}

.admin-shell.collapsed .side-foot .row {
  justify-content: center;
}

@media (max-width: 980px) {
  .admin-shell.collapsed {
    grid-template-columns: 1fr;
  }
}
</style>
