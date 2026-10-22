import { request } from './http'
import type {
  AdminUser,
  AuditLogEntry,
  Category,
  CategoryStat,
  DashboardSummary,
  ItemSummary,
  PageResult,
  ReportItem,
  TrendPoint,
} from '@/types'

export const adminApi = {
  summary: () => request<DashboardSummary>({ url: '/admin/dashboard/summary' }),
  trend: (days = 14) => request<TrendPoint[]>({ url: '/admin/dashboard/trend', params: { days } }),
  categoryStats: () => request<CategoryStat[]>({ url: '/admin/dashboard/categories' }),

  pendingItems: (params: { page?: number; size?: number }) =>
    request<PageResult<ItemSummary>>({ url: '/admin/items/pending', params }),
  approveItem: (id: number) => request<unknown>({ url: `/admin/items/${id}/approve`, method: 'POST' }),
  rejectItem: (id: number, reason: string) =>
    request<unknown>({ url: `/admin/items/${id}/reject`, method: 'POST', data: { reason } }),

  reports: (params: { status?: string; page?: number; size?: number }) =>
    request<PageResult<ReportItem>>({ url: '/admin/reports', params }),
  claimReport: (id: number) => request<ReportItem>({ url: `/admin/reports/${id}/claim`, method: 'POST' }),
  resolveReport: (id: number, payload: { action: string; remark: string }) =>
    request<ReportItem>({ url: `/admin/reports/${id}/resolve`, method: 'POST', data: payload }),
  rejectReport: (id: number, remark: string) =>
    request<ReportItem>({ url: `/admin/reports/${id}/reject`, method: 'POST', data: { remark } }),

  users: (params: { keyword?: string; role?: string; status?: string; page?: number; size?: number }) =>
    request<PageResult<AdminUser>>({ url: '/admin/users', params }),
  warnUser: (id: number, reason: string) =>
    request<AdminUser>({ url: `/admin/users/${id}/warn`, method: 'POST', data: { reason } }),
  banUser: (id: number, reason: string, days: number) =>
    request<AdminUser>({ url: `/admin/users/${id}/ban`, method: 'POST', data: { reason, days } }),
  unbanUser: (id: number, reason?: string) =>
    request<AdminUser>({ url: `/admin/users/${id}/unban`, method: 'POST', data: { reason } }),
  changeRole: (id: number, role: string) =>
    request<AdminUser>({ url: `/admin/users/${id}/role`, method: 'PUT', data: { role } }),

  categories: () => request<Category[]>({ url: '/admin/categories' }),
  createCategory: (payload: { name: string; slug: string; icon?: string; sortOrder: number; enabled: boolean }) =>
    request<Category>({ url: '/admin/categories', method: 'POST', data: payload }),
  updateCategory: (
    id: number,
    payload: { name: string; slug: string; icon?: string; sortOrder: number; enabled: boolean },
  ) => request<Category>({ url: `/admin/categories/${id}`, method: 'PUT', data: payload }),
  deleteCategory: (id: number) => request<void>({ url: `/admin/categories/${id}`, method: 'DELETE' }),

  auditLogs: (params: { page?: number; size?: number }) =>
    request<PageResult<AuditLogEntry>>({ url: '/admin/audit-logs', params }),
}
