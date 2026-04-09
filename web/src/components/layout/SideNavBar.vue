<template>
  <aside class="sidebar">
    <div class="logo-area">
      <router-link to="/" class="logo-link">
        <el-icon :size="28" color="var(--color-primary)"><ChatRound /></el-icon>
        <span class="logo-text">NexaTalk</span>
      </router-link>
    </div>

    <nav class="nav-menu">
      <router-link to="/" class="nav-item" active-class="active">
        <el-icon><HomeFilled /></el-icon>
        <span>首页</span>
      </router-link>
      
      <router-link to="/search" class="nav-item" active-class="active">
        <el-icon><Compass /></el-icon>
        <span>发现</span>
      </router-link>

      <router-link to="/notifications" class="nav-item" active-class="active">
        <div class="icon-badge">
          <el-icon><Bell /></el-icon>
          <span v-if="notifStore.unreadNotifications" class="dot"></span>
        </div>
        <span>通知</span>
      </router-link>

      <router-link to="/messages" class="nav-item" active-class="active">
        <div class="icon-badge">
          <el-icon><ChatDotSquare /></el-icon>
          <span v-if="notifStore.unreadMessages" class="dot"></span>
        </div>
        <span>私信</span>
      </router-link>

      <router-link v-if="userStore.isLoggedIn" :to="`/user/${userStore.userInfo?.userId}`" class="nav-item" active-class="active">
        <el-icon><User /></el-icon>
        <span>我的</span>
      </router-link>

      <!-- 管理入口：仅管理员可见 -->
      <router-link v-if="userStore.isLoggedIn && userStore.isAdmin" to="/admin" class="nav-item" active-class="active">
        <el-icon><Setting /></el-icon>
        <span>管理</span>
      </router-link>

      <router-link v-if="userStore.isLoggedIn" to="/settings" class="nav-item" active-class="active">
        <el-icon><Setting /></el-icon>
        <span>设置</span>
      </router-link>
      
      <div v-if="!userStore.isLoggedIn" class="auth-buttons">
         <el-button type="primary" round class="w-full mb-2" @click="$router.push('/login')">登录</el-button>
         <el-button round class="w-full" @click="$router.push('/register')">注册</el-button>
      </div>
    </nav>

    <div v-if="userStore.isLoggedIn" class="action-area">
      <el-button type="primary" size="large" round class="post-btn btn-animate" @click="$router.push('/posts/create')">
        <el-icon class="mr-1"><EditPen /></el-icon> 发布
      </el-button>
    </div>
    
    <div v-if="userStore.isLoggedIn" class="user-area">
       <el-dropdown trigger="click" @command="handleCommand">
        <div class="user-card btn-animate">
          <el-avatar :size="40" :src="userStore.userInfo?.avatarUrl">
             {{ userStore.userInfo?.nickname?.charAt(0) }}
          </el-avatar>
          <div class="user-info">
            <div class="nickname">{{ userStore.userInfo?.nickname }}</div>
            <div class="username">@{{ userStore.userInfo?.username || 'user' }}</div>
          </div>
          <el-icon><More /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="logout" class="text-danger">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </aside>
</template>

<script setup>
import { useUserStore } from '@/stores/user'
import { useNotificationStore } from '@/stores/notification'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'

const userStore = useUserStore()
const notifStore = useNotificationStore()
const router = useRouter()

const handleCommand = async (cmd) => {
  if (cmd === 'logout') {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
    userStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.sidebar {
  display: flex;
  flex-direction: column;
  height: 100vh;
  padding: 0 12px 20px;
  position: sticky;
  top: 0;
  overflow-y: auto;
}

.logo-area {
  padding: 24px 12px;
}

.logo-link {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 24px;
  font-weight: 800;
  color: var(--color-primary);
  letter-spacing: -0.5px;
}

.nav-menu {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 16px;
  border-radius: 99px; /* Pill shape */
  font-size: 18px;
  color: var(--text-main);
  transition: background-color 0.2s;
}

.nav-item:hover {
  background-color: var(--bg-hover);
}

.nav-item.active {
  font-weight: 700;
  color: var(--color-primary);
}

.nav-item .el-icon {
  font-size: 26px;
}

.icon-badge {
  position: relative;
  display: flex;
  align-items: center;
}

.dot {
  position: absolute;
  top: 0;
  right: -2px;
  width: 8px;
  height: 8px;
  background: #f56c6c;
  border-radius: 50%;
  border: 2px solid #fff;
}

.action-area {
  margin: 20px 0;
}

.post-btn {
  width: 100%;
  font-weight: 700;
  font-size: 16px;
  height: 52px;
}

.user-area {
  margin-top: auto;
}

.user-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px;
  border-radius: 99px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.user-card:hover {
  background-color: var(--bg-hover);
}

.user-info {
  flex: 1;
  overflow: hidden;
}

.nickname {
  font-weight: 700;
  font-size: 14px;
  color: var(--text-main);
}

.username {
  font-size: 12px;
  color: var(--text-secondary);
}

.auth-buttons {
  margin-top: 20px;
  padding: 0 10px;
}
.w-full { width: 100%; }
.mb-2 { margin-bottom: 8px; }
.mr-1 { margin-right: 4px; }
.text-danger { color: #f56c6c; }
</style>
