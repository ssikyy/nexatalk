import { ref, readonly } from 'vue'
import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'
import { useUserStore } from '@/stores/user'
import { listConversations, readConversation as apiReadConversation } from '@/api/message'

// 全局 WebSocket 客户端
let stompClient = null

// 全局消息状态
const messages = ref([])
const conversations = ref([])
const activeConvId = ref(null)
const isConnected = ref(false)

// 消息回调函数
const messageHandlers = []

// 等待 WebSocket 连接就绪
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

/**
 * 注册消息回调
 * @param {Function} handler - 处理新消息的函数
 */
export function onNewMessage(handler) {
  messageHandlers.push(handler)
  return () => {
    const index = messageHandlers.indexOf(handler)
    if (index > -1) messageHandlers.splice(index, 1)
  }
}

/**
 * 获取当前消息列表
 */
export function useMessages() {
  return {
    messages: readonly(messages),
    conversations: readonly(conversations),
    activeConvId: readonly(activeConvId),
    isConnected: readonly(isConnected)
  }
}

/**
 * 初始化全局 WebSocket 连接
 */
export function initGlobalWebSocket() {
  const userStore = useUserStore()
  
  console.log('【WebSocket】====== 开始初始化 ======')
  console.log('【WebSocket】userStore.token 存在:', !!userStore.token)
  console.log('【WebSocket】userStore.isLoggedIn:', userStore.isLoggedIn)
  console.log('【WebSocket】localStorage.token:', !!localStorage.getItem('token'))
  
  if (!userStore.token || !userStore.isLoggedIn) {
    console.log('【WebSocket】条件不满足，跳过初始化')
    return
  }

  // 如果已经连接，不重复初始化
  if (stompClient && isConnected.value) {
    console.log('【WebSocket】已连接，跳过')
    return
  }

  console.log('【WebSocket】开始创建连接, userId:', userStore.userInfo?.userId)
  
  const socket = new SockJS(`/ws`)
  console.log('【WebSocket】SockJS socket created, url:', `/ws`)
  stompClient = new Client({
    webSocketFactory: () => socket,
    connectHeaders: {
      Authorization: `Bearer ${userStore.token}`
    },
    onConnect: () => {
      console.log('Global WebSocket connected!')
      isConnected.value = true
      
      // 订阅个人消息队列
      console.log('Subscribing to /user/queue/messages')
      stompClient.subscribe('/user/queue/messages', (frame) => {
        console.log('【WebSocket收到消息】', frame.body)
        const notification = JSON.parse(frame.body)
        handleNewMessageNotification(notification)
      }, (err) => {
        console.error('【订阅失败】', err)
      })
    },
    onDisconnect: () => {
      console.log('Global WebSocket disconnected')
      isConnected.value = false
    },
    onStompError: (frame) => {
      console.error('STOMP error', frame)
    },
    onWebSocketError: (event) => {
      console.error('WebSocket error', event)
    },
    // 断开后自动重连
    reconnectDelay: 5000,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000
  })

  stompClient.activate()
}

/**
 * 处理新消息通知
 */
function handleNewMessageNotification(notification) {
  // 触发所有注册的回调（让组件处理具体逻辑）
  messageHandlers.forEach(handler => {
    try {
      handler(notification)
    } catch (e) {
      console.error('Message handler error:', e)
    }
  })
  
  // 注意：activeConvId 检查已移除，由组件根据 notification 判断是否刷新
}

/**
 * 更新会话未读数
 */
function updateConversationUnread(notification) {
  const userStore = useUserStore()
  const userId = userStore.userInfo?.userId
  
  const conv = conversations.value.find(c => c.id === notification.conversationId)
  if (conv) {
    if (conv.user1Id === userId) {
      conv.user1Unread = (conv.user1Unread || 0) + 1
    } else {
      conv.user2Unread = (conv.user2Unread || 0) + 1
    }
    conv.lastMessage = notification.content
    conv.lastMessageAt = notification.createdAt
  }
}

/**
 * 加载会话列表
 */
export async function loadConversations() {
  try {
    const res = await listConversations({ page: 1, pageSize: 50 })
    conversations.value = res.data?.list || []
  } catch (e) {
    console.error('Failed to load conversations:', e)
  }
}

/**
 * 加载消息列表
 */
export async function loadMessages(conversationId) {
  activeConvId.value = conversationId
  try {
    const res = await listConversations({ 
      conversationId, 
      page: 1, 
      pageSize: 50 
    })
    messages.value = res.data?.list || []
    // 自动标记已读
    await markAsRead(conversationId)
  } catch (e) {
    console.error('Failed to load messages:', e)
  }
}

/**
 * 标记会话为已读
 */
export async function markAsRead(conversationId) {
  try {
    await apiReadConversation(conversationId)
    // 更新本地未读数
    const userStore = useUserStore()
    const userId = userStore.userInfo?.userId
    const conv = conversations.value.find(c => c.id === conversationId)
    if (conv) {
      if (conv.user1Id === userId) {
        conv.user1Unread = 0
      } else {
        conv.user2Unread = 0
      }
    }
  } catch (e) {
    console.error('Failed to mark as read:', e)
  }
}

/**
 * 断开 WebSocket 连接
 */
export function disconnectWebSocket() {
  if (stompClient) {
    stompClient.deactivate()
    stompClient = null
  }
  isConnected.value = false
  messages.value = []
  conversations.value = []
  activeConvId.value = null
}

/**
 * 切换会话
 */
export function switchConversation(conversationId) {
  if (activeConvId.value !== conversationId) {
    loadMessages(conversationId)
  }
}

/**
 * 发送正在输入状态
 * @param {number} receiverId - 接收方用户ID
 * @param {number} conversationId - 会话ID
 * @param {boolean} isTyping - 是否正在输入
 */
export async function sendTypingStatus(receiverId, conversationId, isTyping) {
  // 等待连接就绪（最多等待10秒）
  try {
    await waitForConnection(10000)
  } catch (e) {
    console.warn('【WebSocket】连接未就绪，跳过 typing 通知')
    return
  }

  const userStore = useUserStore()
  if (!userStore.userInfo?.userId) return

  try {
    const payload = {
      receiverId: receiverId,
      conversationId: conversationId,
      isTyping: isTyping
    }

    stompClient.publish({
      destination: '/app/typing',
      body: JSON.stringify(payload)
    })
    console.log('【WebSocket】typing 状态已发送:', isTyping)
  } catch (e) {
    console.error('【WebSocket】发送 typing 失败:', e)
  }
}
