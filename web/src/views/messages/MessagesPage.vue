<template>
  <div class="messages-page">
    <transition name="fade-slide" mode="out-in">
      <div v-if="!activeConvId" class="view-container list-view" key="list">
        <div class="view-header">
          <h2 class="title">消息</h2>
          <div class="header-actions">
            <el-button
              circle
              size="small"
              :icon="EditPen"
              class="new-msg-btn"
              @click="openNewChatDialog"
            />
          </div>
        </div>

        <div class="search-bar" v-if="conversations.length > 0">
          <div class="search-wrapper">
            <el-input
              v-model="searchKeyword"
              prefix-icon="Search"
              placeholder="搜索会话"
              class="search-input"
              clearable
            />
          </div>
        </div>

        <div class="conversation-list" v-loading="convsLoading">
          <div
            v-for="conv in filteredConversations"
            :key="conv.id"
            class="conv-item"
            :class="{ active: String(activeConvId) === String(conv.id) }"
            @click="selectConversation(conv)"
          >
            <div class="avatar-container">
              <el-avatar
                :size="52"
                :src="getUserInfo(conv).avatarUrl"
                class="conv-avatar"
              >
                <span class="avatar-fallback">{{ getUserInfo(conv).nickname?.charAt(0) }}</span>
              </el-avatar>
              <span v-if="(conv.unreadCount || 0) > 0" class="unread-badge">
                {{ conv.unreadCount > 99 ? '99+' : conv.unreadCount }}
              </span>
            </div>

            <div class="conv-content">
              <div class="conv-top">
                <span class="nickname">{{ getUserInfo(conv).nickname }}</span>
                <span class="time">{{ formatTime(conv.lastMessageAt) }}</span>
              </div>
              <div class="conv-bottom">
                <span class="last-msg" :class="{ unread: (conv.unreadCount || 0) > 0 }">
                  <template v-if="conv.lastMessage">{{ conv.lastMessage }}</template>
                  <span v-else-if="conv.isAiConversation" class="empty-msg">点击开始与 AI 对话</span>
                  <span v-else class="empty-msg">暂无消息</span>
                </span>
                <el-icon v-if="(conv.unreadCount || 0) > 0" class="chevron-icon">
                  <ArrowRight />
                </el-icon>
              </div>
            </div>
          </div>

          <div v-if="!convsLoading && conversations.length === 0" class="empty-state">
            <div class="empty-icon">
              <el-icon :size="64"><ChatLineRound /></el-icon>
            </div>
            <h3 class="empty-title">暂无消息</h3>
            <p class="empty-desc">开始与他人对话吧</p>
            <el-button type="primary" round @click="showNewChatDialog = true">
              <el-icon style="margin-right: 6px;"><ChatDotRound /></el-icon>
              发起对话
            </el-button>
          </div>
        </div>
      </div>

      <div v-else class="view-container chat-view" key="chat">
        <header class="chat-header">
          <div class="header-left">
            <el-button link class="back-btn" @click="closeConversation">
              <el-icon :size="22"><ArrowLeft /></el-icon>
            </el-button>
            <div class="header-user-info" @click="goToProfile">
              <el-avatar
                :size="38"
                :src="activeUserInfo.avatarUrl"
                class="header-avatar"
              >
                <span class="avatar-fallback">{{ activeUserInfo.nickname?.charAt(0) }}</span>
              </el-avatar>
              <div class="header-name-wrapper">
                <span class="header-name">{{ activeUserInfo.nickname }}</span>
                <span class="header-subtitle" :class="{ status: headerSubtitleIsStatus }">
                  {{ headerSubtitle }}
                </span>
              </div>
            </div>
          </div>

          <div class="header-right">
            <span v-if="!isConnected" class="connection-pill">连接中断</span>
            <el-dropdown trigger="click" @command="handleHeaderCommand">
              <el-button circle text :icon="MoreFilled" class="more-btn" />
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="refresh">刷新会话</el-dropdown-item>
                  <el-dropdown-item command="clear">清空聊天记录</el-dropdown-item>
                  <el-dropdown-item command="delete" divided>删除对话</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </header>

        <div class="message-list" ref="messagesRef" v-loading="msgLoading">
          <div v-if="hasMoreMessages && messages.length > 0" class="load-older-wrapper">
            <el-button
              text
              class="load-older-btn"
              :loading="loadingMore"
              @click="loadOlderMessages"
            >
              加载更早消息
            </el-button>
          </div>

          <div v-if="messages.length === 0 && !msgLoading" class="empty-chat">
            <el-avatar
              :size="64"
              :src="activeUserInfo.avatarUrl"
              class="empty-chat-avatar"
            >
              <span class="avatar-fallback">{{ activeUserInfo.nickname?.charAt(0) }}</span>
            </el-avatar>
            <h3 class="empty-chat-title">{{ activeUserInfo.nickname }}</h3>
            <p class="empty-chat-desc">这是你们对话的开始</p>
          </div>

          <div
            v-for="msg in messages"
            :key="msg.id"
            class="message-row"
            :class="{ mine: msg.senderId === userStore.userInfo?.userId }"
          >
            <el-avatar
              v-if="msg.senderId !== userStore.userInfo?.userId"
              :size="40"
              :src="activeUserInfo.avatarUrl"
              class="msg-avatar"
            >
              <span class="avatar-fallback">{{ activeUserInfo.nickname?.charAt(0) }}</span>
            </el-avatar>

            <div class="msg-content-wrapper">
              <div class="msg-main">
                <el-dropdown
                  trigger="click"
                  placement="bottom-end"
                  class="message-actions"
                  @command="(command) => handleMessageCommand(command, msg)"
                >
                  <button type="button" class="message-action-trigger" aria-label="消息操作">
                    <el-icon><MoreFilled /></el-icon>
                  </button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item
                        v-if="canRecallMessage(msg)"
                        command="recall"
                      >
                        撤回消息
                      </el-dropdown-item>
                      <el-dropdown-item command="delete" :divided="canRecallMessage(msg)">
                        删除消息
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>

                <div class="msg-bubble" :class="{ recalled: msg.isRecalled === 1 }">
                  <div class="bubble-text">{{ msg.content }}</div>
                </div>
              </div>
              <div class="msg-meta">{{ formatTime(msg.createdAt, true) }}</div>
            </div>
          </div>

          <div v-if="isPeerTyping && !msgLoading" class="message-row typing-row">
            <el-avatar
              :size="40"
              :src="activeUserInfo.avatarUrl"
              class="msg-avatar"
            >
              <span class="avatar-fallback">{{ activeUserInfo.nickname?.charAt(0) }}</span>
            </el-avatar>
            <div class="typing-bubble">
              <span></span>
              <span></span>
              <span></span>
            </div>
          </div>
        </div>

        <div class="chat-input-wrapper">
          <div class="input-container" :class="{ disconnected: !isConnected }">
            <el-input
              v-model="inputContent"
              type="textarea"
              :rows="1"
              :autosize="{ minRows: 1, maxRows: 5 }"
              resize="none"
              placeholder="发送消息..."
              @keydown.enter.prevent="handleEnter"
              class="seamless-input"
            />
            <div class="input-actions">
              <el-button
                type="primary"
                circle
                :icon="Position"
                :loading="sending"
                @click="handleSend"
                :disabled="!inputContent.trim()"
                class="send-btn"
              />
            </div>
          </div>
          <div class="input-hint">
            {{ !isConnected ? '实时连接已断开，恢复后会自动重新同步。' : '按 Enter 发送，Shift + Enter 换行' }}
          </div>
        </div>
      </div>
    </transition>

    <el-dialog
      v-model="showNewChatDialog"
      title="新消息"
      width="520px"
      :close-on-click-modal="false"
      class="new-chat-dialog"
    >
      <div class="dialog-content">
        <div class="new-chat-header">
          <p class="new-chat-subtitle">选择一个联系人开始私信</p>
        </div>

        <el-input
          v-model="newChatSearch"
          placeholder="搜索昵称或用户名"
          class="rounded-input new-chat-search"
          clearable
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>

        <div class="new-chat-list" v-loading="newChatLoading">
          <div
            v-for="user in filteredNewChatUsers"
            :key="user.id"
            class="new-chat-item"
            @click="startChatWithUser(user)"
          >
            <el-avatar :size="44" :src="user.avatarUrl" class="new-chat-avatar">
              {{ user.nickname?.charAt(0) }}
            </el-avatar>
            <div class="new-chat-info">
              <div class="new-chat-name-row">
                <span class="new-chat-name">{{ user.nickname }}</span>
              </div>
              <div class="new-chat-username">
                @{{ user.username || ('user' + user.id) }}
              </div>
            </div>
          </div>

          <div
            v-if="!newChatLoading && filteredNewChatUsers.length === 0"
            class="new-chat-empty"
          >
            暂无可选择的联系人，请先关注一些用户～
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useNotificationStore } from '@/stores/notification'
import {
  clearMessages as clearMessagesApi,
  deleteConversation as deleteConversationApi,
  deleteMessage as deleteMessageApi,
  listConversations,
  listMessages,
  readConversation,
  recallMessage as recallMessageApi,
  sendMessage
} from '@/api/message'
import { getUserById } from '@/api/user'
import { listFollowers, listFollowing } from '@/api/follow'
import { onNewMessage, sendTypingStatus, useMessages } from '@/utils/websocket'
import {
  ArrowLeft,
  ArrowRight,
  ChatDotRound,
  ChatLineRound,
  EditPen,
  MoreFilled,
  Position,
  Search
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const MESSAGE_PAGE_SIZE = 50
const RECALLED_TEXT = '【此消息已被撤回】'
const PEER_TYPING_TIMEOUT = 3000
const LOCAL_TYPING_IDLE_TIMEOUT = 1200
const BOTTOM_SCROLL_RETRY_COUNT = 12
const BOTTOM_SCROLL_RETRY_DELAY = 16

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const notificationStore = useNotificationStore()
const { isConnected } = useMessages()

const conversations = ref([])
const convsLoading = ref(false)
const messages = ref([])
const msgLoading = ref(false)
const loadingMore = ref(false)
const messagePage = ref(1)
const hasMoreMessages = ref(false)
const activeConvId = ref(null)
const inputContent = ref('')
const sending = ref(false)
const messagesRef = ref(null)
const userInfoMap = ref({})
const showNewChatDialog = ref(false)
const newChatSearch = ref('')
const newChatCandidates = ref([])
const newChatLoading = ref(false)
const searchKeyword = ref('')
const isPeerTyping = ref(false)

let unsubscribeWebSocket = null
let peerTypingTimer = null
let localTypingTimer = null
let isLocalTyping = false
let pendingScrollToBottom = false

const activeConversation = computed(() =>
  conversations.value.find((item) => String(item.id) === String(activeConvId.value))
)

const activeUserInfo = computed(() => getUserInfo(activeConversation.value))

const headerSubtitle = computed(() => {
  if (!activeConversation.value) return ''
  if (!isConnected.value) return '实时连接已断开，正在重连…'
  if (isPeerTyping.value) return '正在输入…'
  return activeUserInfo.value.username ? `@${activeUserInfo.value.username}` : '私信会话'
})

const headerSubtitleIsStatus = computed(() =>
  !isConnected.value || isPeerTyping.value
)

const filteredConversations = computed(() => {
  if (!searchKeyword.value) return conversations.value
  const keyword = searchKeyword.value.toLowerCase()
  return conversations.value.filter((conv) => {
    const userInfo = getUserInfo(conv)
    return (
      userInfo.nickname?.toLowerCase().includes(keyword) ||
      userInfo.username?.toLowerCase().includes(keyword)
    )
  })
})

const filteredNewChatUsers = computed(() => {
  if (!newChatSearch.value) return newChatCandidates.value
  const keyword = newChatSearch.value.toLowerCase()
  return newChatCandidates.value.filter((user) =>
    user.nickname?.toLowerCase().includes(keyword) ||
    user.username?.toLowerCase().includes(keyword)
  )
})

watch(inputContent, () => {
  syncTypingState()
})

watch(isConnected, async (connected, wasConnected) => {
  if (!connected) {
    isPeerTyping.value = false
    return
  }

  if (wasConnected === false) {
    await loadConversations({ silent: true })
    if (activeConversation.value && !isPendingConversation(activeConversation.value.id)) {
      await loadConversationMessages(activeConversation.value.id, { reset: true, silent: true })
    }
  }
})

onMounted(async () => {
  await loadConversations()

  const targetUserId = Number(route.query.userId)
  if (targetUserId && targetUserId !== userStore.userInfo?.userId) {
    await handleOpenChatWithUser(targetUserId)
    router.replace({ path: route.path })
  }

  unsubscribeWebSocket = onNewMessage(handleSocketNotification)
})

onUnmounted(() => {
  stopPeerTyping()
  stopLocalTyping()
  if (unsubscribeWebSocket) {
    unsubscribeWebSocket()
    unsubscribeWebSocket = null
  }
})

function isPendingConversation(id) {
  return String(id).startsWith('pending_')
}

function getPeerId(conv) {
  if (!conv) return null
  return conv.user1Id === userStore.userInfo?.userId ? conv.user2Id : conv.user1Id
}

function hydrateUserInfoMap(convList) {
  convList.forEach((conv) => {
    if (!conv?.opponentId) return
    userInfoMap.value[conv.opponentId] = {
      id: conv.opponentId,
      nickname: conv.opponentNickname || `用户 ${conv.opponentId}`,
      avatarUrl: conv.opponentAvatar || '',
      username: conv.opponentUsername || ''
    }
  })
}

function getUserInfo(conv) {
  if (!conv) return {}

  const peerId = conv.opponentId || conv._pendingUserId || getPeerId(conv)
  if (peerId && userInfoMap.value[peerId]) {
    return userInfoMap.value[peerId]
  }

  const userInfo = {
    id: peerId,
    nickname: conv.opponentNickname || `用户 ${peerId ?? ''}`,
    avatarUrl: conv.opponentAvatar || '',
    username: conv.opponentUsername || ''
  }

  if (peerId) {
    userInfoMap.value[peerId] = userInfo
  }
  return userInfo
}

function dedupeMessages(list) {
  const seen = new Set()
  return list.filter((item) => {
    const key = String(item.id)
    if (seen.has(key)) {
      return false
    }
    seen.add(key)
    return true
  })
}

function buildPreview(content) {
  if (!content) return ''
  const normalized = String(content).replace(/\s+/g, ' ').trim()
  if (normalized.length <= 50) return normalized
  return `${normalized.slice(0, 50)}...`
}

function getErrorMessage(error) {
  return error?.response?.data?.message || error?.message || '操作失败'
}

function moveConversationToTopById(conversationId) {
  const index = conversations.value.findIndex((item) => String(item.id) === String(conversationId))
  if (index <= 0) return
  const [item] = conversations.value.splice(index, 1)
  conversations.value.unshift(item)
}

function upsertConversation(conv) {
  const index = conversations.value.findIndex((item) => String(item.id) === String(conv.id))
  if (index === -1) {
    conversations.value.unshift(conv)
  } else {
    conversations.value[index] = {
      ...conversations.value[index],
      ...conv
    }
    moveConversationToTopById(conv.id)
  }
}

function resetMessageState() {
  messages.value = []
  messagePage.value = 1
  hasMoreMessages.value = false
  loadingMore.value = false
  pendingScrollToBottom = false
}

function closeConversation() {
  stopPeerTyping()
  stopLocalTyping()
  pendingScrollToBottom = false
  activeConvId.value = null
  resetMessageState()
}

async function loadConversations({ silent = false } = {}) {
  if (!silent) {
    convsLoading.value = true
  }

  try {
    const res = await listConversations({ page: 1, pageSize: 50 })
    const nextConversations = res.data?.list || []
    hydrateUserInfoMap(nextConversations)

    const pendingConversations = conversations.value.filter((conv) => isPendingConversation(conv.id))
    pendingConversations.forEach((pendingConv) => {
      const pendingPeerId = pendingConv._pendingUserId || getPeerId(pendingConv)
      const hasRealConversation = nextConversations.some((item) => getPeerId(item) === pendingPeerId)
      if (!hasRealConversation) {
        nextConversations.unshift(pendingConv)
      }
    })

    conversations.value = nextConversations

    if (activeConvId.value && !conversations.value.some((item) => String(item.id) === String(activeConvId.value))) {
      closeConversation()
    }
  } catch (error) {
    if (!silent) {
      ElMessage.error(getErrorMessage(error) || '加载会话失败')
    }
  } finally {
    if (!silent) {
      convsLoading.value = false
    }
  }
}

async function loadConversationMessages(conversationId, { reset = false, silent = false } = {}) {
  if (!conversationId || isPendingConversation(conversationId)) {
    resetMessageState()
    return
  }

  if (reset) {
    msgLoading.value = true
  } else {
    if (!hasMoreMessages.value || loadingMore.value) return
    loadingMore.value = true
  }

  const targetPage = reset ? 1 : messagePage.value + 1

  try {
    const previousHeight = !reset && messagesRef.value ? messagesRef.value.scrollHeight : 0
    const res = await listMessages(conversationId, { page: targetPage, pageSize: MESSAGE_PAGE_SIZE })
    const pageList = dedupeMessages(res.data?.list || [])
    const currentPage = Number(res.data?.page || targetPage)
    const totalPages = Number(res.data?.totalPages || 0)

    if (reset) {
      messages.value = pageList
      messagePage.value = currentPage
      hasMoreMessages.value = currentPage < totalPages
      await scrollToBottom()
    } else {
      messages.value = dedupeMessages([...pageList, ...messages.value])
      messagePage.value = currentPage
      hasMoreMessages.value = currentPage < totalPages
      await nextTick()
      if (messagesRef.value) {
        messagesRef.value.scrollTop = messagesRef.value.scrollHeight - previousHeight
      }
    }
  } catch (error) {
    if (!silent) {
      ElMessage.error(getErrorMessage(error) || '加载消息失败')
    }
  } finally {
    if (reset) {
      msgLoading.value = false
    } else {
      loadingMore.value = false
    }
  }
}

async function loadOlderMessages() {
  if (!activeConversation.value) return
  await loadConversationMessages(activeConversation.value.id, { reset: false })
}

async function markConversationAsRead(conv, { silent = false } = {}) {
  if (!conv || isPendingConversation(conv.id)) return
  try {
    await readConversation(conv.id)
    conv.unreadCount = 0
    await notificationStore.refresh()
  } catch (error) {
    if (!silent) {
      ElMessage.error(getErrorMessage(error) || '更新已读状态失败')
    }
  }
}

async function selectConversation(conv) {
  if (!conv) return
  if (String(activeConvId.value) === String(conv.id) && !isPendingConversation(conv.id)) return

  stopPeerTyping()
  stopLocalTyping()
  activeConvId.value = conv.id

  if (isPendingConversation(conv.id)) {
    resetMessageState()
    return
  }

  await loadConversationMessages(conv.id, { reset: true })
  await markConversationAsRead(conv, { silent: true })
  await scrollToBottom()
}

async function handleSend() {
  const content = inputContent.value.trim()
  if (!content || !activeConversation.value) return

  const conv = activeConversation.value
  const receiverId = getPeerId(conv)
  if (!receiverId) return

  sending.value = true
  try {
    const res = await sendMessage({ receiverId, content })
    const localMessage = {
      ...res.data,
      senderId: res.data?.senderId ?? userStore.userInfo?.userId,
      receiverId: res.data?.receiverId ?? receiverId,
      content,
      isRecalled: res.data?.isRecalled ?? 0,
      createdAt: res.data?.createdAt || new Date().toISOString()
    }

    messages.value = dedupeMessages([...messages.value, localMessage])
    inputContent.value = ''
    stopLocalTyping()

    if (isPendingConversation(conv.id)) {
      await loadConversations({ silent: true })
      const realConv = conversations.value.find((item) => getPeerId(item) === receiverId && !isPendingConversation(item.id))
      if (realConv) {
        activeConvId.value = realConv.id
        await loadConversationMessages(realConv.id, { reset: true, silent: true })
      }
    } else {
      conv.lastMessage = buildPreview(content)
      conv.lastMessageAt = localMessage.createdAt
      conv.unreadCount = 0
      upsertConversation(conv)
      await scrollToBottom()
    }
  } catch (error) {
    ElMessage.error(getErrorMessage(error) || '发送失败')
  } finally {
    sending.value = false
  }
}

function handleEnter(event) {
  if (event.shiftKey) {
    inputContent.value += '\n'
    return
  }
  handleSend()
}

function canRecallMessage(msg) {
  if (!msg || msg.senderId !== userStore.userInfo?.userId || msg.isRecalled === 1 || !msg.createdAt) {
    return false
  }
  const createdAt = new Date(msg.createdAt).getTime()
  return Date.now() - createdAt <= 5 * 60 * 1000
}

async function handleMessageCommand(command, msg) {
  if (!msg?.id) return

  if (command === 'recall') {
    try {
      await recallMessageApi(msg.id)
      msg.content = RECALLED_TEXT
      msg.isRecalled = 1
      await loadConversations({ silent: true })
      ElMessage.success('消息已撤回')
    } catch (error) {
      ElMessage.error(getErrorMessage(error) || '撤回失败')
    }
    return
  }

  if (command === 'delete') {
    try {
      await ElMessageBox.confirm('确定仅在你的会话中隐藏这条消息吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      await deleteMessageApi(msg.id)
      messages.value = messages.value.filter((item) => item.id !== msg.id)
      await loadConversations({ silent: true })
      ElMessage.success('消息已删除')
    } catch (error) {
      if (error !== 'cancel') {
        ElMessage.error(getErrorMessage(error) || '删除失败')
      }
    }
  }
}

async function handleHeaderCommand(command) {
  if (!activeConversation.value || isPendingConversation(activeConversation.value.id)) {
    ElMessage.warning('请先选择一个有效的会话')
    return
  }

  if (command === 'refresh') {
    await Promise.all([
      loadConversations({ silent: true }),
      loadConversationMessages(activeConversation.value.id, { reset: true, silent: true })
    ])
    ElMessage.success('会话已刷新')
    return
  }

  if (command === 'clear') {
    try {
      await ElMessageBox.confirm('确定仅清空你这边的聊天记录吗？对方的记录不会受影响。', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      await clearMessagesApi(activeConversation.value.id)
      resetMessageState()
      await loadConversations({ silent: true })
      await notificationStore.refresh()
      ElMessage.success('聊天记录已清空')
    } catch (error) {
      if (error !== 'cancel') {
        ElMessage.error(getErrorMessage(error) || '操作失败')
      }
    }
    return
  }

  if (command === 'delete') {
    try {
      await ElMessageBox.confirm('确定仅删除你这边的对话入口吗？新的消息到来时会重新出现。', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      await deleteConversationApi(activeConversation.value.id)
      closeConversation()
      await loadConversations({ silent: true })
      await notificationStore.refresh()
      ElMessage.success('对话已删除')
    } catch (error) {
      if (error !== 'cancel') {
        ElMessage.error(getErrorMessage(error) || '操作失败')
      }
    }
  }
}

async function scrollToBottom() {
  pendingScrollToBottom = true

  for (let attempt = 0; attempt < BOTTOM_SCROLL_RETRY_COUNT; attempt += 1) {
    await nextTick()

    if (messagesRef.value) {
      await new Promise((resolve) => {
        requestAnimationFrame(() => {
          requestAnimationFrame(resolve)
        })
      })

      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
      pendingScrollToBottom = false
      return
    }

    await new Promise((resolve) => {
      setTimeout(resolve, BOTTOM_SCROLL_RETRY_DELAY)
    })
  }
}

watch(messagesRef, async (container) => {
  if (!container || !pendingScrollToBottom) {
    return
  }

  await scrollToBottom()
})

function formatTime(time, isMsg = false) {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()

  if (isMsg) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }

  const diff = now - date
  const oneDay = 24 * 60 * 60 * 1000

  if (diff < oneDay && now.getDate() === date.getDate()) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  if (diff < oneDay * 2 && now.getDate() - date.getDate() === 1) {
    return '昨天'
  }
  if (diff < oneDay * 7) {
    const weekDays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
    return weekDays[date.getDay()]
  }
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

function stopPeerTyping() {
  isPeerTyping.value = false
  if (peerTypingTimer) {
    clearTimeout(peerTypingTimer)
    peerTypingTimer = null
  }
}

function stopLocalTyping() {
  if (localTypingTimer) {
    clearTimeout(localTypingTimer)
    localTypingTimer = null
  }

  if (!isLocalTyping || !activeConversation.value || isPendingConversation(activeConversation.value.id)) {
    isLocalTyping = false
    return
  }

  isLocalTyping = false
  sendTypingStatus(getPeerId(activeConversation.value), activeConversation.value.id, false)
}

function syncTypingState() {
  if (!activeConversation.value || isPendingConversation(activeConversation.value.id)) {
    stopLocalTyping()
    return
  }

  const receiverId = getPeerId(activeConversation.value)
  if (!receiverId) return

  if (!inputContent.value.trim()) {
    stopLocalTyping()
    return
  }

  if (!isLocalTyping) {
    isLocalTyping = true
    sendTypingStatus(receiverId, activeConversation.value.id, true)
  }

  if (localTypingTimer) {
    clearTimeout(localTypingTimer)
  }
  localTypingTimer = setTimeout(() => {
    stopLocalTyping()
  }, LOCAL_TYPING_IDLE_TIMEOUT)
}

function buildMessageFromNotification(notification) {
  return {
    id: notification.messageId,
    conversationId: notification.conversationId,
    senderId: notification.senderId,
    receiverId: notification.receiverId,
    content: notification.content,
    type: notification.messageType ?? 0,
    isRead: 0,
    isRecalled: 0,
    createdAt: notification.createdAt || new Date().toISOString()
  }
}

function ensureConversationFromNotification(notification) {
  let conv = conversations.value.find((item) => String(item.id) === String(notification.conversationId))
  if (!conv) {
    const currentUserId = userStore.userInfo?.userId
    const peerId = notification.senderId === currentUserId ? notification.receiverId : notification.senderId

    if (peerId) {
      userInfoMap.value[peerId] = {
        id: peerId,
        nickname: notification.senderId === currentUserId
          ? userInfoMap.value[peerId]?.nickname || `用户 ${peerId}`
          : notification.senderNickname || userInfoMap.value[peerId]?.nickname || `用户 ${peerId}`,
        avatarUrl: notification.senderId === currentUserId
          ? userInfoMap.value[peerId]?.avatarUrl || ''
          : notification.senderAvatar || userInfoMap.value[peerId]?.avatarUrl || '',
        username: notification.senderId === currentUserId
          ? userInfoMap.value[peerId]?.username || ''
          : notification.senderUsername || userInfoMap.value[peerId]?.username || ''
      }
    }

    conv = {
      id: notification.conversationId,
      user1Id: Math.min(notification.senderId, notification.receiverId),
      user2Id: Math.max(notification.senderId, notification.receiverId),
      lastMessage: buildPreview(notification.content),
      lastMessageAt: notification.createdAt,
      unreadCount: 0,
      opponentId: peerId,
      opponentNickname: userInfoMap.value[peerId]?.nickname || `用户 ${peerId}`,
      opponentAvatar: userInfoMap.value[peerId]?.avatarUrl || '',
      opponentUsername: userInfoMap.value[peerId]?.username || ''
    }
    conversations.value.unshift(conv)
    return conv
  }

  conv.lastMessage = buildPreview(notification.content)
  conv.lastMessageAt = notification.createdAt
  upsertConversation(conv)
  return conv
}

async function handleIncomingMessage(notification) {
  const conv = ensureConversationFromNotification(notification)
  const isActiveConversation = String(activeConvId.value) === String(notification.conversationId)

  if (isActiveConversation) {
    messages.value = dedupeMessages([...messages.value, buildMessageFromNotification(notification)])
    conv.unreadCount = 0
    await scrollToBottom()
    await markConversationAsRead(conv, { silent: true })
  } else {
    conv.unreadCount = (conv.unreadCount || 0) + 1
  }

  moveConversationToTopById(conv.id)
}

async function handleRecalledMessage(notification) {
  const targetMessage = messages.value.find((item) => String(item.id) === String(notification.messageId))
  if (targetMessage) {
    targetMessage.content = RECALLED_TEXT
    targetMessage.isRecalled = 1
  }
  await loadConversations({ silent: true })
}

function handleTypingNotification(notification) {
  if (
    String(activeConvId.value) !== String(notification.conversationId) ||
    notification.senderId === userStore.userInfo?.userId
  ) {
    return
  }

  isPeerTyping.value = Boolean(notification.isTyping)
  if (peerTypingTimer) {
    clearTimeout(peerTypingTimer)
  }
  if (notification.isTyping) {
    peerTypingTimer = setTimeout(() => {
      isPeerTyping.value = false
      peerTypingTimer = null
    }, PEER_TYPING_TIMEOUT)
  }
}

async function handleSocketNotification(notification) {
  if (!notification?.type) return

  if (notification.type === 'new_message') {
    await handleIncomingMessage(notification)
    return
  }

  if (notification.type === 'typing') {
    handleTypingNotification(notification)
    return
  }

  if (notification.type === 'message_recalled') {
    await handleRecalledMessage(notification)
  }
}

function goToProfile() {
  const peerId = getPeerId(activeConversation.value)
  if (peerId) {
    router.push({ name: 'Profile', params: { id: peerId } })
  }
}

async function handleOpenChatWithUser(targetUserId) {
  const myId = userStore.userInfo?.userId
  const existingConv = conversations.value.find((conv) => getPeerId(conv) === targetUserId)
  if (existingConv) {
    await selectConversation(existingConv)
    return
  }

  if (!userInfoMap.value[targetUserId]) {
    try {
      const res = await getUserById(targetUserId)
      if (res.data) {
        userInfoMap.value[targetUserId] = res.data
      }
    } catch {
      // ignore fallback fetch error
    }
  }

  const placeholderConv = {
    id: `pending_${targetUserId}`,
    user1Id: Math.min(myId, targetUserId),
    user2Id: Math.max(myId, targetUserId),
    lastMessage: '',
    lastMessageAt: null,
    unreadCount: 0,
    _pendingUserId: targetUserId,
    opponentId: targetUserId,
    opponentNickname: userInfoMap.value[targetUserId]?.nickname || `用户 ${targetUserId}`,
    opponentAvatar: userInfoMap.value[targetUserId]?.avatarUrl || '',
    opponentUsername: userInfoMap.value[targetUserId]?.username || ''
  }

  conversations.value.unshift(placeholderConv)
  activeConvId.value = placeholderConv.id
  resetMessageState()
}

async function openNewChatDialog() {
  showNewChatDialog.value = true
  if (!userStore.userInfo?.userId || newChatCandidates.value.length > 0) {
    return
  }

  newChatLoading.value = true
  try {
    const myId = userStore.userInfo.userId
    const [followingRes, followersRes] = await Promise.all([
      listFollowing(myId, { page: 1, pageSize: 100 }),
      listFollowers(myId, { page: 1, pageSize: 100 })
    ])

    const map = new Map()
    const addList = (list) => {
      ;(list || []).forEach((user) => {
        if (!user || user.id === myId || map.has(user.id)) return
        map.set(user.id, user)
      })
    }

    addList(followingRes.data?.list)
    addList(followersRes.data?.list)
    newChatCandidates.value = Array.from(map.values())
  } catch (error) {
    console.error('加载联系人失败', error)
    ElMessage.error('加载联系人失败')
  } finally {
    newChatLoading.value = false
  }
}

async function startChatWithUser(user) {
  if (!user?.id || user.id === userStore.userInfo?.userId) return
  showNewChatDialog.value = false
  userInfoMap.value[user.id] = user
  await handleOpenChatWithUser(user.id)
}
</script>

<style scoped>
.messages-page {
  height: 100vh;
  max-width: 900px;
  margin: 0 auto;
  background: var(--bg-card);
  border-radius: 16px;
  box-shadow: var(--shadow-md);
  overflow: hidden;
  position: relative;
}

.view-container {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.list-view {
  background: var(--bg-card);
}

.view-header {
  padding: 16px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid var(--border-color);
  position: sticky;
  top: 0;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(12px);
  z-index: 10;
}

.title {
  font-size: 20px;
  font-weight: 800;
  color: var(--text-main);
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.new-msg-btn {
  background: var(--color-primary) !important;
  border: none !important;
  color: #fff !important;
  transition: transform 0.2s, background-color 0.2s;
}

.new-msg-btn:hover {
  background: var(--color-primary-hover) !important;
  transform: scale(1.05);
}

.search-bar {
  padding: 12px 20px;
  border-bottom: 1px solid var(--border-color);
}

.search-wrapper {
  position: relative;
}

:deep(.search-input .el-input__wrapper) {
  border-radius: 24px;
  background-color: var(--bg-hover);
  box-shadow: none !important;
  padding: 10px 16px;
  font-size: 15px;
  border: 1px solid transparent;
  transition: all 0.2s;
}

:deep(.search-input .el-input__wrapper:focus-within) {
  background: #fff;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.1) !important;
}

.conversation-list {
  flex: 1;
  overflow-y: auto;
}

.conv-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 20px;
  cursor: pointer;
  transition: background-color 0.2s;
  border-bottom: 1px solid var(--border-color);
}

.conv-item:hover {
  background-color: var(--bg-hover);
}

.conv-item.active {
  background-color: rgba(59, 130, 246, 0.08);
}

.avatar-container {
  position: relative;
  flex-shrink: 0;
}

.conv-avatar {
  border: 2px solid #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: transform 0.2s;
}

.conv-item:hover .conv-avatar {
  transform: scale(1.02);
}

.avatar-fallback {
  font-weight: 600;
  color: var(--text-secondary);
}

.unread-badge {
  position: absolute;
  bottom: -2px;
  right: -2px;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  background: var(--color-primary);
  color: #fff;
  border-radius: 9px;
  font-size: 11px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid #fff;
}

.conv-content {
  flex: 1;
  overflow: hidden;
  min-width: 0;
}

.conv-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.nickname {
  font-weight: 700;
  font-size: 15px;
  color: var(--text-main);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.time {
  font-size: 13px;
  color: var(--text-placeholder);
  flex-shrink: 0;
  margin-left: 8px;
}

.conv-bottom {
  display: flex;
  align-items: center;
  gap: 6px;
}

.last-msg {
  font-size: 14px;
  color: var(--text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
}

.last-msg.unread {
  color: var(--text-main);
  font-weight: 600;
}

.empty-msg {
  color: var(--text-placeholder);
}

.chevron-icon {
  color: var(--color-primary);
  font-size: 14px;
  flex-shrink: 0;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
}

.empty-icon {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, #e0f2fe 0%, #bae6fd 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
}

.empty-icon .el-icon {
  color: var(--color-primary);
}

.empty-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-main);
  margin: 0 0 8px;
}

.empty-desc {
  font-size: 15px;
  color: var(--text-secondary);
  margin: 0 0 24px;
}

.chat-view {
  background: #fff;
}

.chat-header {
  height: 64px;
  padding: 0 12px 0 4px;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(12px);
  z-index: 10;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 4px;
}

.back-btn {
  font-size: 20px;
  color: var(--text-main);
  padding: 8px;
  height: 40px;
  width: 40px;
  border-radius: 50%;
  transition: background-color 0.2s;
}

.back-btn:hover {
  background-color: var(--bg-hover);
}

.header-user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 4px 8px;
  margin-left: 4px;
  border-radius: 24px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.header-user-info:hover {
  background-color: var(--bg-hover);
}

.header-name-wrapper {
  display: flex;
  flex-direction: column;
}

.header-name {
  font-weight: 700;
  font-size: 15px;
  color: var(--text-main);
  line-height: 1.2;
}

.header-subtitle {
  font-size: 13px;
  color: var(--text-placeholder);
}

.header-subtitle.status {
  color: var(--color-primary);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.connection-pill {
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(248, 113, 113, 0.12);
  color: #dc2626;
  font-size: 12px;
  font-weight: 600;
}

.more-btn {
  color: var(--text-secondary);
}

.message-list {
  flex: 1;
  padding: 20px 16px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
  background: #fafafa;
}

.load-older-wrapper {
  display: flex;
  justify-content: center;
}

.load-older-btn {
  color: var(--color-primary);
  font-weight: 600;
}

.empty-chat {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
  height: 100%;
}

.empty-chat-avatar {
  margin-bottom: 16px;
  border: 3px solid #fff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.empty-chat-title {
  font-size: 22px;
  font-weight: 800;
  color: var(--text-main);
  margin: 0 0 8px;
}

.empty-chat-desc {
  font-size: 15px;
  color: var(--text-secondary);
  margin: 0;
}

.message-row {
  display: flex;
  gap: 12px;
  max-width: 85%;
  align-items: flex-end;
}

.message-row.mine {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.msg-avatar {
  flex-shrink: 0;
}

.msg-content-wrapper {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  max-width: 100%;
}

.mine .msg-content-wrapper {
  align-items: flex-end;
}

.msg-main {
  display: flex;
  align-items: center;
  gap: 8px;
  max-width: 100%;
}

.mine .msg-main {
  flex-direction: row-reverse;
}

.message-actions {
  opacity: 0;
  transition: opacity 0.2s ease;
}

.message-row:hover .message-actions,
.message-actions:focus-within {
  opacity: 1;
}

.message-action-trigger {
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 50%;
  background: rgba(148, 163, 184, 0.14);
  color: var(--text-secondary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background-color 0.2s, color 0.2s;
}

.message-action-trigger:hover {
  background: rgba(59, 130, 246, 0.12);
  color: var(--color-primary);
}

.msg-bubble {
  padding: 12px 16px;
  border-radius: 20px;
  font-size: 15px;
  line-height: 1.5;
  position: relative;
  word-wrap: break-word;
  max-width: 100%;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.message-row:not(.mine) .msg-bubble {
  background: #fff;
  color: var(--text-main);
  border-bottom-left-radius: 6px;
  border: 1px solid var(--border-color);
}

.message-row.mine .msg-bubble {
  background: var(--color-primary);
  color: #fff;
  border-bottom-right-radius: 6px;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.25);
}

.msg-bubble.recalled {
  background: #eef2f7 !important;
  color: var(--text-secondary) !important;
  border-color: #dbe3ee !important;
  box-shadow: none;
  font-style: italic;
}

.bubble-text {
  white-space: pre-wrap;
  word-break: break-word;
}

.msg-meta {
  font-size: 12px;
  color: var(--text-placeholder);
  margin-top: 4px;
  margin-left: 4px;
}

.mine .msg-meta {
  margin-left: 0;
  margin-right: 4px;
}

.typing-row {
  max-width: 160px;
}

.typing-bubble {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 12px 14px;
  border-radius: 18px;
  background: #fff;
  border: 1px solid var(--border-color);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.typing-bubble span {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #94a3b8;
  display: block;
  animation: typing-bounce 1.1s infinite ease-in-out;
}

.typing-bubble span:nth-child(2) {
  animation-delay: 0.15s;
}

.typing-bubble span:nth-child(3) {
  animation-delay: 0.3s;
}

@keyframes typing-bounce {
  0%,
  80%,
  100% {
    transform: translateY(0);
    opacity: 0.45;
  }
  40% {
    transform: translateY(-4px);
    opacity: 1;
  }
}

.chat-input-wrapper {
  padding: 16px 20px 20px;
  background: #fff;
  border-top: 1px solid var(--border-color);
}

.input-container {
  background: #f0f2f5;
  border-radius: 24px;
  padding: 8px 8px 8px 16px;
  display: flex;
  align-items: flex-end;
  gap: 8px;
  transition: box-shadow 0.2s, background 0.2s;
}

.input-container:focus-within {
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.15);
  background: #fff;
  border: 1px solid var(--color-primary);
}

.input-container.disconnected {
  background: #f8fafc;
}

.seamless-input :deep(.el-textarea__inner) {
  border: none !important;
  background: transparent !important;
  box-shadow: none !important;
  padding: 8px 0;
  font-size: 15px;
  max-height: 120px;
  line-height: 1.5;
}

.input-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.send-btn {
  width: 36px;
  height: 36px;
  margin-bottom: 2px;
}

.send-btn:disabled {
  background: #cbd5e1 !important;
  border-color: #cbd5e1 !important;
}

.input-hint {
  font-size: 12px;
  color: var(--text-placeholder);
  margin-top: 8px;
  text-align: center;
}

:deep(.new-chat-dialog .el-dialog) {
  border-radius: 16px;
  overflow: hidden;
}

:deep(.new-chat-dialog .el-dialog__header) {
  padding: 16px 20px 12px;
  border-bottom: 1px solid var(--border-color);
}

:deep(.new-chat-dialog .el-dialog__title) {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-main);
}

.dialog-content {
  padding: 12px 20px 20px;
}

.new-chat-header {
  margin-bottom: 10px;
}

.new-chat-subtitle {
  margin: 0 0 4px;
  font-size: 13px;
  color: var(--text-secondary);
}

.new-chat-search {
  margin-bottom: 12px;
}

:deep(.rounded-input .el-input__wrapper) {
  border-radius: 999px;
  box-shadow: none !important;
  border: 2px solid #e5edff;
  padding: 10px 14px;
}

:deep(.rounded-input .el-input__wrapper:hover) {
  border-color: var(--color-primary);
}

:deep(.rounded-input .el-input__wrapper:focus-within) {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 1px rgba(59, 130, 246, 0.5) !important;
}

.new-chat-list {
  max-height: 360px;
  margin-top: 4px;
  border-radius: 12px;
  overflow-y: auto;
}

.new-chat-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 8px;
  cursor: pointer;
  transition: background-color 0.15s;
}

.new-chat-item:hover {
  background-color: #f1f5f9;
}

.new-chat-avatar {
  flex-shrink: 0;
}

.new-chat-info {
  flex: 1;
  min-width: 0;
}

.new-chat-name-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.new-chat-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-main);
}

.new-chat-username {
  font-size: 13px;
  color: var(--text-secondary);
}

.new-chat-empty {
  padding: 40px 0;
  text-align: center;
  font-size: 14px;
  color: var(--text-placeholder);
}

.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.25s ease;
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateX(20px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}

.conversation-list::-webkit-scrollbar,
.message-list::-webkit-scrollbar {
  width: 6px;
}

.conversation-list::-webkit-scrollbar-track,
.message-list::-webkit-scrollbar-track {
  background: transparent;
}

.conversation-list::-webkit-scrollbar-thumb,
.message-list::-webkit-scrollbar-thumb {
  background: #d1d5db;
  border-radius: 3px;
}

.conversation-list::-webkit-scrollbar-thumb:hover,
.message-list::-webkit-scrollbar-thumb:hover {
  background: #9ca3af;
}
</style>
