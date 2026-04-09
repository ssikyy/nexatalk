<template>
  <router-view />
</template>

<script setup>
import { useUserStore } from '@/stores/user'
import { initGlobalWebSocket } from '@/utils/websocket'

const userStore = useUserStore()

// 页面加载时，如果已登录则立即初始化 WebSocket（不在 onMounted 中等待）
if (userStore.isLoggedIn) {
  console.log('【App.vue】已登录，初始化 WebSocket')
  initGlobalWebSocket()
}
</script>

<style>
/* 移除 App.vue 中可能冲突的样式，样式已迁移至 theme.css */
</style>
