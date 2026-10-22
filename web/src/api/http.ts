import axios, { type AxiosError, type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse, TokenResponse, UserSummary } from '@/types'

const ACCESS_KEY = 'cm.access'
const REFRESH_KEY = 'cm.refresh'
const USER_KEY = 'cm.user'

interface RetryConfig extends AxiosRequestConfig {
  _retry?: boolean
}

export const http = axios.create({
  baseURL: '/api/v1',
  timeout: 20000,
})

export function getAccessToken(): string | null {
  return localStorage.getItem(ACCESS_KEY)
}

export function getStoredUser(): UserSummary | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) {
    return null
  }
  try {
    return JSON.parse(raw) as UserSummary
  } catch {
    return null
  }
}

export function setStoredUser(user: UserSummary | null) {
  if (user) {
    localStorage.setItem(USER_KEY, JSON.stringify(user))
  } else {
    localStorage.removeItem(USER_KEY)
  }
}

export function setSession(tokens: TokenResponse) {
  localStorage.setItem(ACCESS_KEY, tokens.accessToken)
  localStorage.setItem(REFRESH_KEY, tokens.refreshToken)
  setStoredUser(tokens.user)
}

export function clearSession() {
  localStorage.removeItem(ACCESS_KEY)
  localStorage.removeItem(REFRESH_KEY)
  localStorage.removeItem(USER_KEY)
}

let refreshPromise: Promise<string> | null = null

function refreshAccessToken(): Promise<string> {
  if (!refreshPromise) {
    const refreshToken = localStorage.getItem(REFRESH_KEY)
    if (!refreshToken) {
      return Promise.reject(new Error('没有可用的刷新令牌'))
    }
    refreshPromise = axios
      .post<ApiResponse<TokenResponse>>('/api/v1/auth/refresh', { refreshToken })
      .then((response) => {
        setSession(response.data.data)
        return response.data.data.accessToken
      })
      .finally(() => {
        refreshPromise = null
      })
  }
  return refreshPromise
}

http.interceptors.request.use((config) => {
  const token = getAccessToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResponse<unknown>
    if (body && typeof body.code === 'number' && body.code !== 0) {
      ElMessage.error(body.message || '请求失败')
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return response
  },
  async (error: AxiosError<ApiResponse<unknown>>) => {
    const original = error.config as RetryConfig | undefined
    const status = error.response?.status
    const body = error.response?.data
    const isAuthCall = original?.url?.includes('/auth/') ?? false

    if (status === 401 && original && !original._retry && !isAuthCall) {
      original._retry = true
      try {
        const token = await refreshAccessToken()
        original.headers = { ...(original.headers ?? {}), Authorization: `Bearer ${token}` }
        return http(original)
      } catch {
        clearSession()
        if (!window.location.pathname.startsWith('/login')) {
          const redirect = encodeURIComponent(window.location.pathname + window.location.search)
          window.location.href = `/login?redirect=${redirect}`
        }
        return Promise.reject(error)
      }
    }

    if (status === 401) {
      clearSession()
    }

    const message =
      body?.message ?? (status ? `请求失败（HTTP ${status}）` : '网络异常，请确认后端已启动')
    ElMessage.error(message)
    return Promise.reject(error)
  },
)

export async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await http.request<ApiResponse<T>>(config)
  return response.data.data
}

export async function uploadImage(file: File): Promise<string> {
  const form = new FormData()
  form.append('file', file)
  const result = await request<{ url: string }>({
    url: '/files',
    method: 'POST',
    data: form,
  })
  return result.url
}
