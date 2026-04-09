<template>
  <div class="messages-page">
    <!-- 视图 1: 会话列表 -->
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
              prefix-icon="Search"
              placeholder="搜索会话"
              v-model="searchKeyword"
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
            :class="{ active: activeConvId === conv.id }"
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
              <span v-if="getMyUnread(conv) > 0" class="unread-badge">{{ getMyUnread(conv) > 99 ? '99+' : getMyUnread(conv) }}</span>
            </div>

            <div class="conv-content">
              <div class="conv-top">
                <span class="nickname">{{ getUserInfo(conv).nickname }}</span>
                <span class="time">{{ formatTime(conv.lastMessageAt) }}</span>
              </div>
              <div class="conv-bottom">
                <span class="last-msg" :class="{ 'unread': getMyUnread(conv) > 0 }">
                  <template v-if="conv.lastMessage">{{ conv.lastMessage }}</template>
                  <template v-else-if="conv.isAiConversation" class="empty-msg">点击开始与AI对话</template>
                  <template v-else class="empty-msg">暂无消息</template>
                </span>
                <el-icon v-if="getMyUnread(conv) > 0" class="chevron-icon"><ArrowRight /></el-icon>
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

      <!-- 视图 2: 聊天窗口 -->
      <div v-else class="view-container chat-view" key="chat">
        <!-- 聊天头部 -->
        <header class="chat-header">
          <div class="header-left">
            <el-button link class="back-btn" @click="activeConvId = null">
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
                <span class="header-subtitle">@{{ activeUserInfo.username || '用户' }}</span>
              </div>
            </div>
          </div>
          <div class="header-right">
            <el-dropdown trigger="click" @command="handleHeaderCommand">
              <el-button circle text :icon="MoreFilled" class="more-btn" />
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="clear">清空聊天记录</el-dropdown-item>
                  <el-dropdown-item command="delete" divided>删除对话</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </header>

        <!-- 消息列表 -->
        <div class="message-list" ref="messagesRef" v-loading="msgLoading">
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
            :class="{ 'mine': msg.senderId === userStore.userInfo?.userId }"
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
              <div class="msg-bubble">
                <div class="bubble-text">{{ msg.content }}</div>
              </div>
              <div class="msg-meta">{{ formatTime(msg.createdAt, true) }}</div>
            </div>
          </div>
        </div>

        <!-- 输入框区域 -->
        <div class="chat-input-wrapper">
          <div class="input-container">
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
          <div class="input-hint">按 Enter 发送，Shift + Enter 换行</div>
        </div>
      </div>
    </transition>

    <!-- 新建会话弹窗 -->
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
import { ref, onMounted, nextTick, computed, watch, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { listConversations, listMessages, sendMessage, readConversation, deleteConversation, clearMessages } from '@/api/message'
import { getUserById } from '@/api/user'
import { listFollowing, listFollowers } from '@/api/follow'
import { onNewMessage } from '@/utils/websocket'
import {
  Plus,
  MoreFilled,
  ChatDotRound,
  Search,
  Position,
  ArrowLeft,
  ArrowRight,
  ChatLineRound,
  EditPen,
  User
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()

const userStore = useUserStore()
const conversations = ref([])
const convsLoading = ref(false)
const messages = ref([])
const msgLoading = ref(false)
const activeConvId = ref(null)
const inputContent = ref('')
const sending = ref(false)
const messagesRef = ref()
const userInfoMap = ref({})
const showNewChatDialog = ref(false)
const newChatSearch = ref('')
const newChatCandidates = ref([])
const newChatLoading = ref(false)
const searchKeyword = ref('')
const userIdInput = ref(null)
const contentInput = ref(null)
const refreshTimer = ref(null)

// 监听消息变化，自动滚动到底部
watch(messages, async () => {
  await scrollToBottom()
}, { deep: true })

const activeUserInfo = computed(() => {
  if (!activeConvId.value) return {}
  const conv = conversations.value.find(c => c.id === activeConvId.value)
  if (!conv) return {}
  return getUserInfo(conv)
})

const filteredConversations = computed(() => {
  if (!searchKeyword.value) return conversations.value
  const keyword = searchKeyword.value.toLowerCase()
  return conversations.value.filter(conv => {
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
  return newChatCandidates.value.filter(user => {
    return (
      user.nickname?.toLowerCase().includes(keyword) ||
      user.username?.toLowerCase().includes(keyword)
    )
  })
})

onMounted(async () => {
  await loadConversations()
  const targetUserId = Number(route.query.userId)
  if (targetUserId && targetUserId !== userStore.userInfo?.userId) {
    await handleOpenChatWithUser(targetUserId)
    router.replace({ path: route.path })
  }
  // 注册全局 WebSocket 消息回调（用于会话列表更新提醒）
  const unsubscribe = onNewMessage((notification) => {
    // 更新会话列表的未读数
    if (notification.type !== 'typing') {
      const conv = conversations.value.find(c => c.id === notification.conversationId)
      if (conv) {
        if (conv.user1Id === userStore.userInfo?.userId) {
          conv.user1Unread = (conv.user1Unread || 0) + 1
        } else {
          conv.user2Unread = (conv.user2Unread || 0) + 1
        }
        conv.lastMessage = notification.content
        conv.lastMessageAt = notification.createdAt
      }
    }
  })
  // 保存取消订阅函数，在组件卸载时调用
  window.__wsUnsubscribe = unsubscribe
  
  // 开启自动刷新：每2秒刷新一次消息列表（性能友好）
  refreshTimer.value = setInterval(async () => {
    if (activeConvId.value && !String(activeConvId.value).startsWith('pending_')) {
      try {
        const res = await listMessages(activeConvId.value, { page: 1, pageSize: 50 })
        const newMessages = res.data?.list || []
        if (newMessages.length !== messages.value.length) {
          messages.value = newMessages
        }
      } catch (e) {
        // 忽略刷新错误
      }
    }
  }, 2000)
})

onUnmounted(() => {
  // 停止自动刷新
  if (refreshTimer.value) {
    clearInterval(refreshTimer.value)
    refreshTimer.value = null
  }
  // 调用全局取消订阅函数
  if (window.__wsUnsubscribe) {
    window.__wsUnsubscribe()
    window.__wsUnsubscribe = null
  }
})

// 处理新消息通知
function handleNewMessageNotification(notification) {
  // WebSocket 消息会触发2秒定时刷新，这里只需标记已读
  if (notification.conversationId && String(activeConvId.value) === String(notification.conversationId)) {
    readConversation(notification.conversationId)
  }
}

async function loadConversations() {
  convsLoading.value = true
  try {
    const res = await listConversations({ page: 1, pageSize: 50 })
    conversations.value = res.data?.list || []
    await fetchUsersInfo(conversations.value)
  } finally {
    convsLoading.value = false
  }
}

async function fetchUsersInfo(convList) {
  const idsToFetch = new Set()
  convList.forEach(conv => {
    const peerId = getPeerId(conv)
    if (!userInfoMap.value[peerId]) idsToFetch.add(peerId)
  })
  
  const promises = Array.from(idsToFetch).map(id => 
    getUserById(id).then(res => {
      if (res.data) userInfoMap.value[id] = res.data
    }).catch(() => {})
  )
  await Promise.all(promises)
}

function getPeerId(conv) {
  return conv.user1Id === userStore.userInfo?.userId ? conv.user2Id : conv.user1Id
}

function getUserInfo(conv) {
  const peerId = getPeerId(conv)
  return userInfoMap.value[peerId] || { nickname: `用户 ${peerId}`, avatarUrl: '' }
}

function getMyUnread(conv) {
  return conv.user1Id === userStore.userInfo?.userId ? conv.user1Unread : conv.user2Unread
}

async function selectConversation(conv) {
  if (activeConvId.value === conv.id) return
  activeConvId.value = conv.id

  if (String(conv.id).startsWith('pending_')) {
    messages.value = []
    return
  }

  msgLoading.value = true
  try {
    const res = await listMessages(conv.id, { page: 1, pageSize: 50 })
    messages.value = res.data?.list || []
    await readConversation(conv.id)
    if (conv.user1Id === userStore.userInfo?.userId) conv.user1Unread = 0
    else conv.user2Unread = 0
    await scrollToBottom()
  } finally {
    msgLoading.value = false
  }
}

async function handleSend() {
  const content = inputContent.value.trim()
  if (!content || !activeConvId.value) return
  
  const conv = conversations.value.find(c => c.id === activeConvId.value)
  if (!conv) return
  
  sending.value = true
  try {
    const receiverId = getPeerId(conv)
    const res = await sendMessage({ receiverId, content })
    messages.value.push(res.data)
    inputContent.value = ''

    if (String(conv.id).startsWith('pending_')) {
      await loadConversations()
      const myId = userStore.userInfo?.userId
      const realConv = conversations.value.find(c => {
        const peerId = c.user1Id === myId ? c.user2Id : c.user1Id
        return peerId === receiverId
      })
      if (realConv) {
        activeConvId.value = realConv.id
      }
    } else {
      conv.lastMessage = content
      conv.lastMessageAt = new Date().toISOString()
      const idx = conversations.value.indexOf(conv)
      if (idx > 0) {
        conversations.value.splice(idx, 1)
        conversations.value.unshift(conv)
      }
    }
    
    await scrollToBottom()
  } finally {
    sending.value = false
  }
}

function handleEnter(e) {
  if (e.ctrlKey) {
    inputContent.value += '\n'
  } else {
    handleSend()
  }
}

async function handleHeaderCommand(command) {
  if (!activeConvId.value || String(activeConvId.value).startsWith('pending_')) {
    ElMessage.warning('请先选择一个有效的会话')
    return
  }

  if (command === 'clear') {
    // 清空聊天记录
    try {
      await ElMessageBox.confirm('确定要清空此会话的所有聊天记录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      await clearMessages(activeConvId.value)
      messages.value = []
      // 更新会话列表
      const conv = conversations.value.find(c => c.id === activeConvId.value)
      if (conv) {
        conv.lastMessage = null
        conv.lastMessageAt = null
      }
      ElMessage.success('聊天记录已清空')
    } catch (e) {
      if (e !== 'cancel') {
        ElMessage.error('操作失败')
      }
    }
  } else if (command === 'delete') {
    // 删除对话
    try {
      await ElMessageBox.confirm('确定要删除此对话吗？删除后无法恢复。', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      await deleteConversation(activeConvId.value)
      // 从会话列表中移除
      const idx = conversations.value.findIndex(c => c.id === activeConvId.value)
      if (idx !== -1) {
        conversations.value.splice(idx, 1)
      }
      activeConvId.value = null
      messages.value = []
      ElMessage.success('对话已删除')
    } catch (e) {
      if (e !== 'cancel') {
        ElMessage.error('操作失败')
      }
    }
  }
}

async function scrollToBottom() {
  await nextTick()
  // 等待 DOM 更新完成
  await new Promise(resolve => setTimeout(resolve, 100))
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
}

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
  } else if (diff < oneDay * 2 && now.getDate() - date.getDate() === 1) {
    return '昨天'
  } else if (diff < oneDay * 7) {
    const weekDays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
    return weekDays[date.getDay()]
  } else {
    return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
  }
}

function goToProfile() {
  const peerId = getPeerId(conversations.value.find(c => c.id === activeConvId.value))
  if (peerId) {
    router.push({ name: 'Profile', params: { id: peerId } })
  }
}

async function handleOpenChatWithUser(targetUserId) {
  const myId = userStore.userInfo?.userId
  const existingConv = conversations.value.find(c => {
    const peerId = c.user1Id === myId ? c.user2Id : c.user1Id
    return peerId === targetUserId
  })
  if (existingConv) {
    await selectConversation(existingConv)
    return
  }

  if (!userInfoMap.value[targetUserId]) {
    try {
      const res = await getUserById(targetUserId)
      if (res.data) userInfoMap.value[targetUserId] = res.data
    } catch {
      // user info will show fallback
    }
  }

  const user1Id = Math.min(myId, targetUserId)
  const user2Id = Math.max(myId, targetUserId)
  const placeholderConv = {
    id: `pending_${targetUserId}`,
    user1Id,
    user2Id,
    lastMessage: '',
    lastMessageAt: null,
    user1Unread: 0,
    user2Unread: 0,
    _pendingUserId: targetUserId,
  }
  conversations.value.unshift(placeholderConv)
  activeConvId.value = placeholderConv.id
  messages.value = []
}

async function openNewChatDialog() {
  showNewChatDialog.value = true
  if (!userStore.userInfo?.userId) return
  if (newChatCandidates.value.length > 0) return

  newChatLoading.value = true
  try {
    const myId = userStore.userInfo.userId
    const [followingRes, followersRes] = await Promise.all([
      listFollowing(myId, { page: 1, pageSize: 100 }),
      listFollowers(myId, { page: 1, pageSize: 100 })
    ])

    const map = new Map()
    const addList = (list) => {
      ;(list || []).forEach((u) => {
        if (!u || u.id === myId) return
        if (!map.has(u.id)) {
          map.set(u.id, u)
        }
      })
    }

    addList(followingRes.data?.list)
    addList(followersRes.data?.list)

    newChatCandidates.value = Array.from(map.values())
  } catch (e) {
    console.error('加载联系人失败', e)
    ElMessage.error('加载联系人失败')
  } finally {
    newChatLoading.value = false
  }
}

async function startChatWithUser(user) {
  if (!user || !user.id || user.id === userStore.userInfo?.userId) return
  showNewChatDialog.value = false
  await handleOpenChatWithUser(user.id)
}
</script>

<style scoped>
.messages-page {
  /* 占满中间列高度，不留底部空白 */
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

/* List View Styles */
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

.chevron-icon {
  color: var(--color-primary);
  font-size: 14px;
  flex-shrink: 0;
}

/* Empty State */
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

/* Chat View Styles */
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

.header-right {
  display: flex;
  gap: 4px;
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
  cursor: pointer;
  transition: transform 0.2s;
}

.msg-avatar:hover {
  transform: scale(1.05);
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

/* Dialog Styles - New Message */
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

/* Transitions */
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

/* Scrollbar */
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
