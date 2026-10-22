import { request } from './http'
import type { TokenResponse, UserSummary } from '@/types'

export interface LoginPayload {
  account: string
  password: string
}

export interface RegisterPayload {
  studentNo: string
  nickname: string
  email: string
  password: string
}

export const authApi = {
  login: (payload: LoginPayload) =>
    request<TokenResponse>({ url: '/auth/login', method: 'POST', data: payload }),
  register: (payload: RegisterPayload) =>
    request<TokenResponse>({ url: '/auth/register', method: 'POST', data: payload }),
  logout: () => request<void>({ url: '/auth/logout', method: 'POST' }),
  me: () => request<UserSummary>({ url: '/auth/me' }),
  sendVerification: () =>
    request<{ email: string; expiresInSeconds: number; hint: string; code: string | null }>({
      url: '/auth/verification/send',
      method: 'POST',
    }),
  confirmVerification: (code: string) =>
    request<UserSummary>({ url: '/auth/verification/confirm', method: 'POST', data: { code } }),
}
