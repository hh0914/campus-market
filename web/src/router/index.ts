import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('@/layouts/StudentLayout.vue'),
    children: [
      { path: '', name: 'home', component: () => import('@/views/student/HomeView.vue'), meta: { public: true } },
      { path: 'search', name: 'search', component: () => import('@/views/student/SearchView.vue'), meta: { public: true } },
      { path: 'items/new', name: 'item-new', component: () => import('@/views/student/ItemEditView.vue'), meta: { auth: true } },
      { path: 'items/:id', name: 'item-detail', component: () => import('@/views/student/ItemDetailView.vue'), meta: { public: true } },
      { path: 'items/:id/edit', name: 'item-edit', component: () => import('@/views/student/ItemEditView.vue'), meta: { auth: true } },
      { path: 'users/:id', name: 'user-profile', component: () => import('@/views/student/UserProfileView.vue'), meta: { public: true } },
      { path: 'my-items', name: 'my-items', component: () => import('@/views/student/MyItemsView.vue'), meta: { auth: true } },
      { path: 'favorites', name: 'favorites', component: () => import('@/views/student/FavoritesView.vue'), meta: { auth: true } },
      { path: 'chat', name: 'chat', component: () => import('@/views/student/ChatView.vue'), meta: { auth: true } },
      { path: 'orders', name: 'orders', component: () => import('@/views/student/OrdersView.vue'), meta: { auth: true } },
      { path: 'orders/:id', name: 'order-detail', component: () => import('@/views/student/OrderDetailView.vue'), meta: { auth: true } },
      { path: 'wallet', name: 'wallet', component: () => import('@/views/student/WalletView.vue'), meta: { auth: true } },
      { path: 'notifications', name: 'notifications', component: () => import('@/views/student/NotificationsView.vue'), meta: { auth: true } },
      { path: 'profile', name: 'profile', component: () => import('@/views/student/ProfileView.vue'), meta: { auth: true } },
    ],
  },
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { roles: ['MODERATOR', 'ADMIN'] },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', name: 'admin-dashboard', component: () => import('@/views/admin/DashboardView.vue'), meta: { title: '数据看板' } },
      { path: 'items', name: 'admin-items', component: () => import('@/views/admin/ItemAuditView.vue'), meta: { title: '商品审核' } },
      { path: 'reports', name: 'admin-reports', component: () => import('@/views/admin/ReportsView.vue'), meta: { title: '举报工单' } },
      { path: 'users', name: 'admin-users', component: () => import('@/views/admin/UsersView.vue'), meta: { title: '用户治理' } },
      {
        path: 'categories',
        name: 'admin-categories',
        component: () => import('@/views/admin/CategoriesView.vue'),
        meta: { roles: ['ADMIN'], title: '分类维护' },
      },
      { path: 'audit-logs', name: 'admin-audit-logs', component: () => import('@/views/admin/AuditLogsView.vue'), meta: { title: '审计日志' } },
    ],
  },
  { path: '/login', name: 'login', component: () => import('@/views/LoginView.vue'), meta: { public: true } },
  { path: '/register', name: 'register', component: () => import('@/views/RegisterView.vue'), meta: { public: true } },
  { path: '/:pathMatch(.*)*', name: 'not-found', component: () => import('@/views/NotFoundView.vue'), meta: { public: true } },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, _from, saved) {
    if (saved) {
      return saved
    }
    if (to.hash) {
      return { el: to.hash, behavior: 'smooth' }
    }
    return { top: 0 }
  },
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  const roles = to.matched.flatMap((record) => (record.meta.roles as string[] | undefined) ?? [])

  if (to.meta.auth && !auth.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (roles.length > 0) {
    if (!auth.isLoggedIn) {
      return { name: 'login', query: { redirect: to.fullPath } }
    }
    if (!auth.user || !roles.includes(auth.user.role)) {
      ElMessage.error('该页面仅对审核员与管理员开放')
      return { name: 'home' }
    }
  }
  if ((to.name === 'login' || to.name === 'register') && auth.isLoggedIn) {
    return { name: 'home' }
  }
  return true
})

export default router
