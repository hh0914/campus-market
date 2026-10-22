import { request } from './http'
import type {
  ChatMessage,
  Conversation,
  NotificationItem,
  PageResult,
  PublicProfile,
  ReportItem,
  Review,
  UserSummary,
} from '@/types'

export const chatApi = {
  open: (itemId: number) => request<Conversation>({ url: '/conversations', method: 'POST', data: { itemId } }),
  list: () => request<Conversation[]>({ url: '/conversations' }),
  messages: (id: number, afterId?: number) =>
    request<ChatMessage[]>({ url: `/conversations/${id}/messages`, params: { afterId } }),
  send: (id: number, payload: { content: string; type?: string; offerAmount?: number | null }) =>
    request<ChatMessage>({ url: `/conversations/${id}/messages`, method: 'POST', data: payload }),
  markRead: (id: number) => request<void>({ url: `/conversations/${id}/read`, method: 'POST' }),
}

export const userApi = {
  me: () => request<UserSummary>({ url: '/users/me' }),
  update: (payload: { nickname?: string; avatarUrl?: string; bio?: string; campus?: string }) =>
    request<UserSummary>({ url: '/users/me', method: 'PUT', data: payload }),
  changePassword: (payload: { oldPassword: string; newPassword: string }) =>
    request<void>({ url: '/users/me/password', method: 'PUT', data: payload }),
  profile: (id: number | string) => request<PublicProfile>({ url: `/users/${id}/profile` }),
  reviews: (id: number | string, params: { page?: number; size?: number }) =>
    request<PageResult<Review>>({ url: `/users/${id}/reviews`, params }),
}

export const notificationApi = {
  list: (params: { page?: number; size?: number }) =>
    request<PageResult<NotificationItem>>({ url: '/notifications', params }),
  unreadCount: () => request<number>({ url: '/notifications/unread-count' }),
  markRead: (id: number) => request<void>({ url: `/notifications/${id}/read`, method: 'POST' }),
  markAllRead: () => request<void>({ url: '/notifications/read-all', method: 'POST' }),
}

export const reviewApi = {
  create: (orderId: number, payload: { rating: number; tags?: string; comment?: string }) =>
    request<Review>({ url: `/orders/${orderId}/reviews`, method: 'POST', data: payload }),
  byOrder: (orderId: number) => request<Review[]>({ url: `/orders/${orderId}/reviews` }),
}

export const reportApi = {
  create: (payload: {
    targetType: 'ITEM' | 'USER'
    targetId: number
    reasonType: string
    description: string
    evidenceUrl?: string
  }) => request<ReportItem>({ url: '/reports', method: 'POST', data: payload }),
  mine: (params: { page?: number; size?: number }) =>
    request<PageResult<ReportItem>>({ url: '/reports/mine', params }),
}
