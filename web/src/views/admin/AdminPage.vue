<!---- NexaTalk 管理后台（独立全屏布局） ---->
<template>
  <div class="admin-layout">
    <!-- 侧边栏 -->
    <aside class="admin-sidebar">
      <div class="admin-logo">
        <div class="logo-wrapper">
          <el-icon :size="28" color="#409eff"><ChatDotSquare /></el-icon>
          <div class="logo-text">
            <span class="logo-main">NexaTalk</span>
            <span class="logo-sub">管理后台</span>
          </div>
        </div>
      </div>

      <el-menu
        class="admin-menu"
        :default-active="activeMenu"
        :collapse="false"
        @select="handleMenuSelect"
      >
        <el-menu-item index="overview">
          <el-icon><DataAnalysis /></el-icon>
          <span>概览</span>
        </el-menu-item>
        <el-menu-item index="users">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="sections">
          <el-icon><Folder /></el-icon>
          <span>分区管理</span>
        </el-menu-item>
        <el-menu-item index="ai">
          <el-icon><ChatDotRound /></el-icon>
          <span>AI 管理</span>
        </el-menu-item>
        <el-menu-item index="reports">
          <el-icon><Warning /></el-icon>
          <span>举报审核</span>
          <template #suffix>
            <el-badge v-if="pendingReportCount > 0" :value="pendingReportCount" :max="99" type="danger" />
          </template>
        </el-menu-item>
        <el-menu-item index="punishments">
          <el-icon><Lock /></el-icon>
          <span>处罚管理</span>
        </el-menu-item>
        <el-menu-item index="logs">
          <el-icon><Document /></el-icon>
          <span>系统日志</span>
        </el-menu-item>
        <el-menu-item index="notifications">
          <el-icon><Bell /></el-icon>
          <span>通知管理</span>
        </el-menu-item>
      </el-menu>

      <div class="admin-sidebar-footer">
        <div class="footer-card">
          <div class="footer-user">
            <el-avatar :size="36" :src="userStore.userInfo?.avatar">
              {{ userStore.userInfo?.nickname?.charAt(0) || 'A' }}
            </el-avatar>
            <div class="footer-user-info">
              <div class="footer-nickname">{{ userStore.userInfo?.nickname || '管理员' }}</div>
              <div class="footer-role">
                <el-tag :type="userStore.isSuperAdmin ? 'warning' : 'danger'" size="small" effect="dark">
                  {{ userStore.isSuperAdmin ? '超级管理员' : '管理员' }}
                </el-tag>
              </div>
            </div>
          </div>
          <div class="footer-actions">
            <el-tooltip content="返回社区" placement="top">
              <el-button size="small" type="primary" plain @click="goBackHome">
                <el-icon><HomeFilled /></el-icon>
              </el-button>
            </el-tooltip>
            <el-tooltip content="退出登录" placement="top">
              <el-button size="small" type="danger" plain @click="handleLogout">
                <el-icon><SwitchButton /></el-icon>
              </el-button>
            </el-tooltip>
          </div>
        </div>
      </div>
    </aside>

    <!-- 主内容区 -->
    <div class="admin-main">
      <!-- 顶部导航 -->
      <header class="admin-header">
        <div class="header-left">
          <el-breadcrumb separator="/" class="header-breadcrumb">
            <el-breadcrumb-item>管理后台</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentMenuName }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-dropdown trigger="click" @command="handleCommand">
            <div class="header-user">
              <el-avatar :size="32" :src="userStore.userInfo?.avatar">
                {{ userStore.userInfo?.nickname?.charAt(0) || 'A' }}
              </el-avatar>
              <span class="header-username">{{ userStore.userInfo?.nickname || '管理员' }}</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>个人中心
                </el-dropdown-item>
                <el-dropdown-item command="ai">
                  <el-icon><ChatDotRound /></el-icon>AI 设置
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <!-- 内容区 -->
      <main class="admin-content">
        <!-- 概览页面 -->
        <div v-if="activeMenu === 'overview'" class="overview-page">
          <div class="overview-header">
            <h2 class="page-title">数据概览</h2>
            <span class="page-subtitle">欢迎回来，{{ userStore.userInfo?.nickname || '管理员' }}</span>
          </div>

          <div class="stats-grid">
            <div class="stat-card" @click="goToMenu('users')">
              <div class="stat-icon" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%)">
                <el-icon><User /></el-icon>
              </div>
              <div class="stat-content">
                <div class="stat-value">{{ stats.userCount }}</div>
                <div class="stat-label">用户总数</div>
              </div>
            </div>

            <div class="stat-card" @click="goToMenu('sections')">
              <div class="stat-icon" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%)">
                <el-icon><Folder /></el-icon>
              </div>
              <div class="stat-content">
                <div class="stat-value">{{ stats.sectionCount }}</div>
                <div class="stat-label">分区数量</div>
              </div>
            </div>

            <div class="stat-card" @click="goToMenu('reports')">
              <div class="stat-icon" style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)">
                <el-icon><Warning /></el-icon>
              </div>
              <div class="stat-content">
                <div class="stat-value">{{ pendingReportCount }}</div>
                <div class="stat-label">待审核举报</div>
              </div>
            </div>

            <div class="stat-card" @click="goToMenu('punishments')">
              <div class="stat-icon" style="background: linear-gradient(135deg, #fa709a 0%, #fee140 100%)">
                <el-icon><Lock /></el-icon>
              </div>
              <div class="stat-content">
                <div class="stat-value">{{ stats.activePunishmentCount }}</div>
                <div class="stat-label">生效处罚</div>
              </div>
            </div>
          </div>

          <div class="quick-actions">
            <div class="actions-header">
              <el-icon><Lightning /></el-icon>
              <span>快捷操作</span>
            </div>
            <div class="actions-grid">
              <div class="action-item" @click="$router.push('/admin/notification/create')">
                <el-icon><Bell /></el-icon>
                <span>发布通知</span>
              </div>
              <div class="action-item" @click="goToMenu('sections')">
                <el-icon><Plus /></el-icon>
                <span>新增分区</span>
              </div>
              <div class="action-item" @click="goToMenu('reports')">
                <el-icon><Warning /></el-icon>
                <span>审核举报</span>
              </div>
              <div class="action-item" @click="goToMenu('users')">
                <el-icon><Search /></el-icon>
                <span>搜索用户</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 其他管理页面 -->
        <div v-else class="content-card">
          <user-admin v-if="activeMenu === 'users'" />
          <section-admin v-else-if="activeMenu === 'sections'" />
          <ai-admin v-else-if="activeMenu === 'ai'" />
          <report-admin v-else-if="activeMenu === 'reports'" />
          <punishment-admin v-else-if="activeMenu === 'punishments'" />
          <log-admin v-else-if="activeMenu === 'logs'" />
          <notification-admin v-else-if="activeMenu === 'notifications'" />
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import {
  User, Folder, Warning, Lock, Document, Bell, ChatDotRound,
  ChatDotSquare, HomeFilled, SwitchButton, ArrowDown,
  DataAnalysis, Lightning, Plus, Search
} from '@element-plus/icons-vue'
import UserAdmin from './UserAdmin.vue'
import SectionAdmin from './SectionAdmin.vue'
import AiAdmin from './AiAdmin.vue'
import ReportAdmin from './ReportAdmin.vue'
import PunishmentAdmin from './PunishmentAdmin.vue'
import LogAdmin from './LogAdmin.vue'
import NotificationAdmin from './NotificationAdmin.vue'
import { adminListPendingReports } from '@/api/report'
import { adminListActivePunishments } from '@/api/punishment'
import { adminListAllSections } from '@/api/section'
import { adminListUsers } from '@/api/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const activeMenu = ref('overview')

const pendingReportCount = ref(0)

const stats = ref({
  userCount: 0,
  sectionCount: 0,
  activePunishmentCount: 0
})

const menuNames = {
  overview: '概览',
  users: '用户管理',
  sections: '分区管理',
  ai: 'AI 管理',
  reports: '举报审核',
  punishments: '处罚管理',
  logs: '系统日志',
  notifications: '通知管理'
}

const currentMenuName = computed(() => menuNames[activeMenu.value] || '')

const handleMenuSelect = (key) => {
  activeMenu.value = key
}

const goToMenu = (menu) => {
  activeMenu.value = menu
}

const goBackHome = () => {
  router.push('/')
}

const handleCommand = (command) => {
  if (command === 'logout') {
    handleLogout()
  } else if (command === 'profile') {
    router.push(`/user/${userStore.userInfo?.userId}`)
  } else if (command === 'ai') {
    activeMenu.value = 'ai'
  }
}

const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
    await userStore.logout()
    router.push('/login')
  } catch {}
}

const loadStats = async () => {
  try {
    // 并行加载统计数据
    const [usersRes, sectionsRes, punishmentsRes, reportsRes] = await Promise.all([
      adminListUsers({ page: 1, pageSize: 1 }),
      adminListAllSections(),
      adminListActivePunishments({ page: 1, pageSize: 1 }),
      adminListPendingReports({ page: 1, pageSize: 1 })
    ])

    stats.value = {
      userCount: usersRes.data?.total || 0,
      sectionCount: sectionsRes.data?.length || 0,
      activePunishmentCount: punishmentsRes.data?.total || 0
    }

    pendingReportCount.value = reportsRes.data?.total || 0
  } catch (e) {
    console.error('加载统计数据失败', e)
  }
}

// 检查 URL query 参数
onMounted(() => {
  const tab = route.query.tab
  if (tab && menuNames[tab]) {
    activeMenu.value = tab
  }
  loadStats()
})
</script>

<style scoped>
.admin-layout {
  display: flex;
  height: 100vh;
  background: #f0f2f5;
  overflow: hidden;
}

.admin-sidebar {
  width: 240px;
  background: linear-gradient(180deg, #1a1a2e 0%, #16213e 100%);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.admin-logo {
  padding: 24px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.logo-wrapper {
  display: flex;
  align-items: center;
  gap: 14px;
}

.logo-text {
  display: flex;
  flex-direction: column;
}

.logo-main {
  font-size: 20px;
  font-weight: 700;
  color: #fff;
  line-height: 1.3;
}

.logo-sub {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.5);
  letter-spacing: 1px;
}

.admin-menu {
  border-right: none;
  background-color: transparent;
  flex: 1;
  padding: 16px 12px;
}

.admin-menu :deep(.el-menu-item) {
  color: rgba(255, 255, 255, 0.65);
  margin: 4px 0;
  border-radius: 10px;
  height: 48px;
  line-height: 48px;
  justify-content: flex-start;
  padding-left: 20px !important;
}

.admin-menu :deep(.el-menu-item:hover) {
  background-color: rgba(255, 255, 255, 0.08);
  color: #fff;
}

.admin-menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, rgba(64, 158, 255, 0.2) 0%, rgba(64, 158, 255, 0.1) 100%);
  color: #409eff;
  border-left: 3px solid #409eff;
}

.admin-menu :deep(.el-menu-item .el-icon) {
  font-size: 18px;
  margin-right: 12px;
}

.admin-sidebar-footer {
  padding: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  margin-top: auto;
}

.footer-card {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 12px;
  padding: 16px;
}

.footer-user {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.footer-user :deep(.el-avatar) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  font-weight: 600;
}

.footer-user-info {
  flex: 1;
  min-width: 0;
}

.footer-nickname {
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 4px;
}

.footer-role {
  display: flex;
  align-items: center;
}

.footer-actions {
  display: flex;
  gap: 8px;
}

.footer-actions .el-button {
  flex: 1;
  justify-content: center;
}

.admin-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.admin-header {
  height: 64px;
  padding: 0 28px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
}

.header-left {
  display: flex;
  align-items: center;
}

.header-breadcrumb :deep(.el-breadcrumb__inner) {
  font-size: 15px;
}

.header-right {
  display: flex;
  align-items: center;
}

.header-user {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 8px 16px;
  border-radius: 10px;
  transition: background 0.2s;
}

.header-user:hover {
  background: #f5f7fa;
}

.header-user :deep(.el-avatar) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  font-weight: 600;
}

.header-username {
  color: #303133;
  font-size: 14px;
  font-weight: 500;
}

.admin-content {
  flex: 1;
  padding: 24px 28px;
  overflow-y: auto;
}

/* 概览页面样式 */
.overview-page {
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.overview-header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0 0 8px 0;
}

.page-subtitle {
  color: #909399;
  font-size: 14px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 20px;
  margin-bottom: 28px;
}

.stat-card {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  border: 1px solid #f0f0f0;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-icon .el-icon {
  font-size: 28px;
  color: #fff;
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #1a1a2e;
  line-height: 1.2;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}

.quick-actions {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  border: 1px solid #f0f0f0;
}

.actions-header {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a2e;
  margin-bottom: 20px;
}

.actions-header .el-icon {
  color: #409eff;
}

.actions-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 16px;
}

.action-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 20px;
  border-radius: 12px;
  background: #f8f9fa;
  cursor: pointer;
  transition: all 0.2s ease;
}

.action-item:hover {
  background: #ecf5ff;
}

.action-item .el-icon {
  font-size: 24px;
  color: #409eff;
}

.action-item span {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.content-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  padding: 24px;
  min-height: calc(100vh - 160px);
  animation: fadeIn 0.3s ease;
}
</style>
