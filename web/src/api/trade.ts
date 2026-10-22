import { request } from './http'
import type { Order, OrderDetail, PageResult, WalletInfo, LedgerEntry } from '@/types'

export const orderApi = {
  create: (itemId: number) => request<Order>({ url: '/orders', method: 'POST', data: { itemId } }),
  list: (params: { role: 'buyer' | 'seller'; status?: string; page?: number; size?: number }) =>
    request<PageResult<Order>>({ url: '/orders', params }),
  detail: (id: number | string) => request<OrderDetail>({ url: `/orders/${id}` }),
  pay: (id: number) => request<Order>({ url: `/orders/${id}/pay`, method: 'POST' }),
  cancel: (id: number, reason?: string) =>
    request<Order>({ url: `/orders/${id}/cancel`, method: 'POST', data: { reason } }),
  ship: (id: number) => request<Order>({ url: `/orders/${id}/ship`, method: 'POST' }),
  confirm: (id: number) => request<Order>({ url: `/orders/${id}/confirm`, method: 'POST' }),
  refund: (id: number, reason?: string) =>
    request<Order>({ url: `/orders/${id}/refund`, method: 'POST', data: { reason } }),
  refundApprove: (id: number) =>
    request<Order>({ url: `/orders/${id}/refund/approve`, method: 'POST' }),
  refundReject: (id: number, reason?: string) =>
    request<Order>({ url: `/orders/${id}/refund/reject`, method: 'POST', data: { reason } }),
}

export const walletApi = {
  view: () => request<WalletInfo>({ url: '/wallet' }),
  ledger: (params: { page?: number; size?: number }) =>
    request<PageResult<LedgerEntry>>({ url: '/wallet/ledger', params }),
  recharge: (amount: number) => request<WalletInfo>({ url: '/wallet/recharge', method: 'POST', data: { amount } }),
}
