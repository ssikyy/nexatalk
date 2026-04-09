import { defineStore } from 'pinia'
import { ref } from 'vue'
import { countUnread } from '@/api/notification'
import { countUnreadMessages } from '@/api/message'
import { useUserStore } from '@/stores/user'
import { onNewMessage } from '@/utils/websocket'

export const useNotificationStore = defineStore('notification', () => {
  const unreadNotifications = ref(0)
  const unreadMessages = ref(0)

  /** 拉取最新未读数（仅统计系统通知未读，用于顶部导航徽章） */
  async function refresh() {
    const userStore = useUserStore()
    if (!userStore.isLoggedIn) return

    try {
      const [nRes, mRes] = await Promise.all([
        countUnread({ type: 10 }),
        countUnreadMessages()
      ])
      unreadNotifications.value = nRes.data ?? 0
      unreadMessages.value = mRes.data ?? 0
    } catch {
      // 静默失败
    }
  }

  // 注册 WebSocket 消息回调，实时更新未读数
  onNewMessage((notification) => {
    if (notification.type === 'new_message') {
      unreadMessages.value = (unreadMessages.value || 0) + 1
    }
  })

  return { unreadNotifications, unreadMessages, refresh }
})
