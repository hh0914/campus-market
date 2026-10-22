import { request } from './http'
import type {
  Category,
  FavoriteToggle,
  ItemDetail,
  ItemPayload,
  ItemSummary,
  PageResult,
} from '@/types'

export interface ItemQuery {
  keyword?: string
  categoryId?: number | null
  condition?: string | null
  minPrice?: number | null
  maxPrice?: number | null
  sort?: string
  page?: number
  size?: number
}

export const itemApi = {
  search: (params: ItemQuery) => request<PageResult<ItemSummary>>({ url: '/items', params }),
  mine: (params: { status?: string; page?: number; size?: number }) =>
    request<PageResult<ItemSummary>>({ url: '/items/mine', params }),
  detail: (id: number | string) => request<ItemDetail>({ url: `/items/${id}` }),
  create: (payload: ItemPayload) => request<ItemDetail>({ url: '/items', method: 'POST', data: payload }),
  update: (id: number | string, payload: ItemPayload) =>
    request<ItemDetail>({ url: `/items/${id}`, method: 'PUT', data: payload }),
  submit: (id: number) => request<ItemDetail>({ url: `/items/${id}/submit`, method: 'POST' }),
  offShelf: (id: number) => request<void>({ url: `/items/${id}/off-shelf`, method: 'POST' }),
  related: (id: number | string) => request<ItemSummary[]>({ url: `/items/${id}/related` }),
  bySeller: (sellerId: number | string, params: { page?: number; size?: number }) =>
    request<PageResult<ItemSummary>>({ url: `/items/seller/${sellerId}`, params }),
  favorite: (id: number) => request<FavoriteToggle>({ url: `/items/${id}/favorite`, method: 'POST' }),
  unfavorite: (id: number) => request<FavoriteToggle>({ url: `/items/${id}/favorite`, method: 'DELETE' }),
  favorites: (params: { page?: number; size?: number }) =>
    request<PageResult<ItemSummary>>({ url: '/favorites', params }),
}

export const categoryApi = {
  list: () => request<Category[]>({ url: '/categories' }),
}
