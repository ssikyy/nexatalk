<template>
  <div class="notif-page">
    <div class="card">
      <div class="page-header">
        <h2 class="page-title">通知</h2>
        <p class="page-desc">管理员发布的系统公告与重要通知</p>
        <div class="header-actions">
          <el-button size="small" type="primary" plain @click="handleMarkAllRead" v-if="total > 0">
            全部已读
          </el-button>
        </div>
      </div>

      <!-- 置顶公告 -->
      <div v-if="pinnedList.length > 0" class="pinned-section">
        <div class="pinned-header">
          <el-icon><Top /></el-icon>
          <span>置顶公告</span>
        </div>
        <div
          v-for="n in pinnedList"
          :key="'pinned-' + n.id"
          :class="['notif-item', 'pinned', { unread: n.isRead === 0 }]"
          @click="goToDetail(n)"
        >
          <div class="notif-icon pinned-icon">
            <el-icon><Bell /></el-icon>
          </div>
          <div class="notif-content">
            <div class="notif-title" :class="{ bold: n.isBold === 1 }">
              <el-tag v-if="n.isPinned === 1" type="danger" size="small" effect="dark">置顶</el-tag>
              {{ n.title || '系统通知' }}
            </div>
            <div class="notif-preview">{{ getPreviewContent(n) }}</div>
            <div class="notif-meta">
              <span class="notif-time">{{ formatTime(n.createdAt) }}</span>
            </div>
          </div>
          <div class="notif-actions">
            <div v-if="n.imageUrl" class="notif-image">
              <img :src="n.imageUrl" alt="通知图片" />
            </div>
            <el-dropdown trigger="click" @command="(cmd) => handleCommand(cmd, n)">
              <el-button size="small" text circle>
                <el-icon><MoreFilled /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="read" v-if="n.isRead === 0">标记已读</el-dropdown-item>
                  <el-dropdown-item command="delete">删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </div>

      <!-- 公告列表 -->
      <div v-loading="loading">
        <div class="section-title" v-if="list.length > 0">全部公告</div>
        <div
          v-for="n in list"
          :key="n.id"
          :class="['notif-item', { unread: n.isRead === 0, bold: n.isBold === 1 }]"
          @click="goToDetail(n)"
        >
          <div v-if="n.isRead === 0" class="notif-dot"></div>
          <div class="notif-icon system-icon">
            <el-icon><Bell /></el-icon>
          </div>
          <div class="notif-content">
            <div class="notif-title">{{ n.title || '系统通知' }}</div>
            <p class="notif-text">{{ n.content }}</p>
            <div class="notif-meta">
              <span class="notif-time">{{ formatTime(n.createdAt) }}</span>
            </div>
          </div>
          <div class="notif-actions">
            <div v-if="n.imageUrl" class="notif-image">
              <img :src="n.imageUrl" alt="通知图片" />
            </div>
            <el-dropdown trigger="click" @command="(cmd) => handleCommand(cmd, n)">
              <el-button size="small" text circle>
                <el-icon><MoreFilled /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="read" v-if="n.isRead === 0">标记已读</el-dropdown-item>
                  <el-dropdown-item command="delete">删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
        <el-empty
          v-if="!loading && list.length === 0 && pinnedList.length === 0"
          description="暂无系统通知"
          :image-size="100"
        />
      </div>

      <el-pagination
        v-if="total > pageSize"
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        background
        class="pagination"
        @current-change="load"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listNotifications, markAllRead, markAsRead, deleteNotification } from '@/api/notification'
import { useNotificationStore } from '@/stores/notification'
import { Bell, Top, MoreFilled } from '@element-plus/icons-vue'

/** 仅展示管理员发布的系统通知（type=10） */
const SYSTEM_NOTIFICATION_TYPE = 10

const router = useRouter()
const notifStore = useNotificationStore()
const rawList = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = 20
const loading = ref(false)

const pinnedList = computed(() => rawList.value.filter(n => n.isPinned === 1))
const list = computed(() => rawList.value.filter(n => n.isPinned !== 1))

onMounted(() => load())

async function load() {
  loading.value = true
  try {
    const res = await listNotifications({
      page: currentPage.value,
      pageSize,
      type: SYSTEM_NOTIFICATION_TYPE
    })
    rawList.value = res.data?.list || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

async function handleMarkAllRead() {
  await markAllRead(SYSTEM_NOTIFICATION_TYPE)
  rawList.value.forEach(n => (n.isRead = 1))
  notifStore.refresh()
  ElMessage.success('已全部标记已读')
}

async function handleCommand(cmd, n) {
  if (cmd === 'read') {
    await markAsRead(n.id)
    n.isRead = 1
    notifStore.refresh()
    ElMessage.success('标记已读成功')
  } else if (cmd === 'delete') {
    try {
      await ElMessageBox.confirm('确定删除这条通知吗？', '提示', { type: 'warning' })
      await deleteNotification(n.id)
      rawList.value = rawList.value.filter(item => item.id !== n.id)
      total.value = Math.max(0, total.value - 1)
      if (n.isRead === 0) notifStore.refresh()
      ElMessage.success('删除成功')
    } catch {}
  }
}

async function goToDetail(n) {
  if (n.isRead === 0) {
    try {
      await markAsRead(n.id)
      n.isRead = 1
      notifStore.refresh()
    } catch (error) {
      console.error('Failed to mark notification as read before navigation:', error)
    }
  }
  router.push(`/notification/${n.id}`)
}

function getPreviewContent(n) {
  if (!n.content) return ''
  return n.content.length > 80 ? n.content.substring(0, 80) + '...' : n.content
}

function formatTime(time) {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  if (days === 0) {
    const hours = Math.floor(diff / (1000 * 60 * 60))
    if (hours === 0) {
      const minutes = Math.floor(diff / (1000 * 60))
      return minutes <= 1 ? '刚刚' : `${minutes}分钟前`
    }
    return `${hours}小时前`
  } else if (days === 1) return '昨天'
  else if (days < 7) return `${days}天前`
  return date.toLocaleDateString('zh-CN')
}
</script>

<style scoped>
.notif-page {
  max-width: 720px;
  margin: 0 auto;
}

.card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  border: 1px solid #ebeef5;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 12px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.page-desc {
  width: 100%;
  font-size: 13px;
  color: #909399;
  margin: 4px 0 0 0;
}

.header-actions {
  flex-shrink: 0;
}

/* 置顶区域 */
.pinned-section {
  margin-bottom: 20px;
}

.pinned-header {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #e6a23c;
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 12px;
}

.pinned-header .el-icon {
  font-weight: bold;
}

.notif-item {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 16px;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.2s;
  margin-bottom: 10px;
  border: 1px solid #f0f2f5;
  position: relative;
}

.notif-item:hover {
  background: #fafafa;
}

.notif-item.unread {
  background: #fef9ec;
  border-color: #fdf6ec;
  padding-left: 28px;
}

.notif-item.pinned {
  background: linear-gradient(135deg, #fff7e6 0%, #fff 100%);
  border-color: #fdf6ec;
}

.notif-item.bold .notif-title {
  font-weight: 600;
}

.notif-dot {
  position: absolute;
  left: 14px;
  top: 22px;
  width: 8px;
  height: 8px;
  background: #409eff;
  border-radius: 50%;
  flex-shrink: 0;
}

.notif-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.notif-icon.system-icon {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

.notif-icon.pinned-icon {
  background: #e6a23c;
  color: #fff;
}

.notif-content {
  flex: 1;
  min-width: 0;
}

.notif-title {
  font-size: 15px;
  color: #303133;
  margin-bottom: 6px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.notif-preview,
.notif-text {
  font-size: 14px;
  color: #606266;
  line-height: 1.5;
  margin: 0 0 8px 0;
}

.notif-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.notif-time {
  font-size: 12px;
  color: #c0c4cc;
}

.notif-actions {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  flex-shrink: 0;
}

.notif-image {
  width: 56px;
  height: 56px;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
}

.notif-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.section-title {
  font-size: 13px;
  color: #909399;
  margin: 16px 0 10px;
  padding-left: 8px;
  border-left: 3px solid #409eff;
}

.pagination {
  margin-top: 20px;
  justify-content: center;
}
</style>
