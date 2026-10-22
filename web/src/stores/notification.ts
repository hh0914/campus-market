import { ref } from 'vue'
import { defineStore } from 'pinia'
import { notificationApi } from '@/api/social'
import type { NotificationItem } from '@/types'

export const useNotificationStore = defineStore('notification', () => {
  const unread = ref(0)
  const latest = ref<NotificationItem[]>([])

  async function refresh() {
    unread.value = await notificationApi.unreadCount()
  }

  async function loadLatest() {
    const page = await notificationApi.list({ page: 1, size: 5 })
    latest.value = page.list
    unread.value = page.list.filter((item) => !item.read).length
  }

  function reset() {
    unread.value = 0
    latest.value = []
  }

  return { unread, latest, refresh, loadLatest, reset }
})
