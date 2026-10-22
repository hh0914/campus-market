import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { authApi, type LoginPayload, type RegisterPayload } from '@/api/auth'
import { userApi } from '@/api/social'
import { clearSession, getAccessToken, getStoredUser, setSession, setStoredUser } from '@/api/http'
import type { TokenResponse, UserSummary } from '@/types'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(getAccessToken())
  const user = ref<UserSummary | null>(getStoredUser())

  const isLoggedIn = computed(() => Boolean(token.value))
  const isStaff = computed(() => user.value?.role === 'ADMIN' || user.value?.role === 'MODERATOR')
  const isAdmin = computed(() => user.value?.role === 'ADMIN')
  const isVerified = computed(() => Boolean(user.value?.verified))
  const displayName = computed(() => user.value?.nickname ?? '同学')

  function apply(tokens: TokenResponse) {
    setSession(tokens)
    token.value = tokens.accessToken
    user.value = tokens.user
  }

  async function login(payload: LoginPayload) {
    apply(await authApi.login(payload))
    return user.value
  }

  async function register(payload: RegisterPayload) {
    apply(await authApi.register(payload))
    return user.value
  }

  async function refreshMe() {
    if (!token.value) {
      return null
    }
    user.value = await userApi.me()
    setStoredUser(user.value)
    return user.value
  }

  function patchUser(patch: Partial<UserSummary>) {
    if (!user.value) {
      return
    }
    user.value = { ...user.value, ...patch }
    setStoredUser(user.value)
  }

  async function logout() {
    try {
      if (token.value) {
        await authApi.logout()
      }
    } catch {
      // 令牌已失效时忽略服务端错误，本地仍然清理
    } finally {
      clearSession()
      token.value = null
      user.value = null
    }
  }

  return {
    token,
    user,
    isLoggedIn,
    isStaff,
    isAdmin,
    isVerified,
    displayName,
    login,
    register,
    refreshMe,
    patchUser,
    logout,
  }
})
