<template>
  <div class="app-layout">
    <!-- 背景层 -->
    <div class="layout-container">
      <!-- 左侧导航 -->
      <SideNavBar class="layout-sidebar" />

      <!-- 中间内容 -->
      <main class="layout-main" :class="{ 'layout-main-full': hideRightPanel }">
        <!-- 路由过渡动画 -->
        <router-view v-slot="{ Component }">
          <transition name="page" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>

      <!-- 右侧面板（发帖 / 编辑帖子时隐藏） -->
      <RightPanel v-if="!hideRightPanel" class="layout-right" />
    </div>

    <!-- 移动端底部导航 (只在小屏幕显示) -->
    <nav class="mobile-nav">
      <router-link to="/" class="mobile-nav-item" active-class="active">
        <el-icon><HomeFilled /></el-icon>
      </router-link>
      <router-link to="/search" class="mobile-nav-item" active-class="active">
        <el-icon><Compass /></el-icon>
      </router-link>
      <router-link to="/posts/create" class="mobile-nav-item add-btn">
        <el-icon><Plus /></el-icon>
      </router-link>
      <router-link to="/messages" class="mobile-nav-item" active-class="active">
        <el-icon><ChatDotSquare /></el-icon>
      </router-link>
      <router-link :to="userStore.isLoggedIn ? `/user/${userStore.userInfo?.userId}` : '/login'" class="mobile-nav-item" active-class="active">
        <el-icon><User /></el-icon>
      </router-link>
    </nav>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import SideNavBar from '@/components/layout/SideNavBar.vue'
import RightPanel from '@/components/layout/RightPanel.vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const route = useRoute()

// 发帖、编辑帖子页面：隐藏右侧推荐栏，让内容区域占据更多宽度
const hideRightPanel = computed(() => {
  return route.name === 'CreatePost' || route.name === 'EditPost'
})
</script>

<style scoped>
.app-layout {
  min-height: 100vh;
  background-color: var(--bg-body);
}

.layout-container {
  max-width: 1280px;
  margin: 0 auto;
  display: flex;
  justify-content: center;
  align-items: flex-start;
}

/* 左侧 Sidebar */
.layout-sidebar {
  width: var(--sidebar-width);
  flex-shrink: 0;
  /* 隐藏滚动条 */
  scrollbar-width: none; 
}
.layout-sidebar::-webkit-scrollbar {
  display: none;
}

/* 中间内容 */
.layout-main {
  width: 100%;
  max-width: var(--max-content-width);
  min-height: 100vh;
  border-left: 1px solid var(--border-color);
  border-right: 1px solid var(--border-color);
  background: var(--bg-body); /* 保持背景一致 */
}

/* 发帖 / 编辑帖子时：占据右侧空间，页面更宽一些 */
.layout-main-full {
  max-width: calc(var(--max-content-width) + var(--right-panel-width));
  border-right: none;
}

/* 右侧面板 */
.layout-right {
  width: var(--right-panel-width);
  flex-shrink: 0;
  display: block;
}

/* 移动端底部导航默认隐藏 */
.mobile-nav {
  display: none;
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 56px;
  background: #fff;
  border-top: 1px solid var(--border-color);
  z-index: 1000;
  justify-content: space-around;
  align-items: center;
}

.mobile-nav-item {
  color: var(--text-secondary);
  font-size: 24px;
  padding: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.mobile-nav-item.active {
  color: var(--color-primary);
}

.mobile-nav-item.add-btn {
  background: var(--color-primary);
  color: #fff;
  border-radius: 50%;
  width: 40px;
  height: 40px;
  font-size: 20px;
  margin-bottom: 4px;
}

/* === 响应式媒体查询 === */

/* 平板/窄屏 (Medium Screens): 隐藏右栏 */
@media (max-width: 1100px) {
  .layout-right {
    display: none;
  }
  .layout-main {
    border-right: none;
  }
}

/* 移动端 (Mobile Screens): 隐藏侧边栏，显示底部导航 */
@media (max-width: 768px) {
  .layout-sidebar {
    display: none;
  }
  
  .layout-container {
    display: block; /* 取消 flex */
  }

  .layout-main {
    max-width: 100%;
    border-left: none;
    border-right: none;
    padding-bottom: 60px; /* 留出底部导航空间 */
  }

  .mobile-nav {
    display: flex;
  }
}
</style>
