import { ref, readonly } from 'vue'
import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'
import { useUserStore } from '@/stores/user'

let stompClient = null
const isConnected = ref(false)
const messageHandlers = []

function waitForConnection(timeout = 10000) {
  return new Promise((resolve, reject) => {
    if (isConnected.value && stompClient?.connected) {
      resolve()
      return
    }

    const startTime = Date.now()
    const checkInterval = setInterval(() => {
      if (isConnected.value && stompClient?.connected) {
        clearInterval(checkInterval)
        resolve()
      } else if (Date.now() - startTime > timeout) {
        clearInterval(checkInterval)
        reject(new Error('WebSocket 连接超时'))
      }
    }, 100)
  })
}

export function onNewMessage(handler) {
  messageHandlers.push(handler)
  return () => {
    const index = messageHandlers.indexOf(handler)
    if (index > -1) {
      messageHandlers.splice(index, 1)
    }
  }
}

export function useMessages() {
  return {
    isConnected: readonly(isConnected)
  }
}

export function initGlobalWebSocket() {
  const userStore = useUserStore()
  if (!userStore.token || !userStore.isLoggedIn) {
    return
  }
  if (stompClient && isConnected.value) {
    return
  }

  const socket = new SockJS('/ws')
  stompClient = new Client({
    webSocketFactory: () => socket,
    connectHeaders: {
      Authorization: `Bearer ${userStore.token}`
    },
    onConnect: () => {
      isConnected.value = true
      stompClient.subscribe('/user/queue/messages', (frame) => {
        const notification = JSON.parse(frame.body)
        messageHandlers.forEach((handler) => {
          try {
            handler(notification)
          } catch (error) {
            console.error('Message handler error:', error)
          }
        })
      })
    },
    onDisconnect: () => {
      isConnected.value = false
    },
    onStompError: (frame) => {
      console.error('STOMP error', frame)
    },
    onWebSocketError: (event) => {
      console.error('WebSocket error', event)
    },
    reconnectDelay: 5000,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000
  })

  stompClient.activate()
}

export function disconnectWebSocket() {
  if (stompClient) {
    stompClient.deactivate()
    stompClient = null
  }
  isConnected.value = false
}

export async function sendTypingStatus(receiverId, conversationId, isTyping) {
  try {
    await waitForConnection(10000)
  } catch {
    return
  }

  const userStore = useUserStore()
  if (!userStore.userInfo?.userId || !receiverId || !conversationId) {
    return
  }

  try {
    stompClient.publish({
      destination: '/app/typing',
      body: JSON.stringify({
        receiverId,
        conversationId,
        isTyping
      })
    })
  } catch (error) {
    console.error('发送 typing 失败:', error)
  }
}
