import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, logout as logoutApi, getMe } from '@/api/user'
import { initGlobalWebSocket, disconnectWebSocket } from '@/utils/websocket'
import { persistAuthSession, readStoredToken, readStoredUserInfo, registerClearSessionHandler } from '@/utils/session'

export const useUserStore = defineStore('user', () => {
  // 当前登录用户信息
  const userInfo = ref(readStoredUserInfo())
  // JWT Token
  const token = ref(readStoredToken())

  // 是否已登录
  const isLoggedIn = computed(() => !!token.value)
  // 是否是管理员（包括管理员和超级管理员）
  const isAdmin = computed(() => userInfo.value?.role === 1 || userInfo.value?.role === 2)
  // 是否是超级管理员
  const isSuperAdmin = computed(() => userInfo.value?.role === 2)

  /**
   * 登录：调用 API、存储 token 和用户信息
   */
  async function login(credentials) {
    const res = await loginApi(credentials)
    const data = res.data
    token.value = data.token
    userInfo.value = {
      userId: data.userId,
      username: data.username,
      nickname: data.nickname,
      avatarUrl: data.avatarUrl,
      bannerUrl: data.bannerUrl,
      bio: data.bio,
      role: data.role
    }
    persistAuthSession(data.token, userInfo.value)
    // 登录成功后初始化全局 WebSocket
    initGlobalWebSocket()
    return data
  }

  function clearSession() {
    token.value = ''
    userInfo.value = null
    persistAuthSession('', null)
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    disconnectWebSocket()
  }

  /**
   * 登出：通知后端使 token 失效，清除本地状态
   */
  async function logout() {
    try {
      await logoutApi()
    } catch {
      // 即使后端调用失败也清除本地状态
    } finally {
      clearSession()
    }
  }

  /**
   * 刷新用户信息（个人资料更新后调用）
   */
  async function refreshUserInfo() {
    if (!isLoggedIn.value) return
    const res = await getMe()
    // 后端返回的是 id，前端存的是 userId，需要手动映射
    userInfo.value = {
      userId: res.data.id,
      username: res.data.username,
      nickname: res.data.nickname,
      avatarUrl: res.data.avatarUrl,
      bannerUrl: res.data.bannerUrl,
      bio: res.data.bio,
      role: res.data.role
    }
    persistAuthSession(token.value, userInfo.value)
  }

  registerClearSessionHandler(clearSession)

  return { userInfo, token, isLoggedIn, isAdmin, isSuperAdmin, login, logout, clearSession, refreshUserInfo }
})
