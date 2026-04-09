<template>
  <div class="notif-detail-page">
    <div class="card">
      <div class="detail-header">
        <el-button text @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
      </div>

      <div v-loading="loading" class="detail-content">
        <template v-if="notification">
          <!-- 标题区域 -->
          <div class="detail-title-wrapper">
            <div class="detail-tags" v-if="notification.isPinned === 1 || notification.isBold === 1">
              <el-tag v-if="notification.isPinned === 1" type="danger" effect="dark">置顶</el-tag>
              <el-tag v-if="notification.isBold === 1" type="warning">重要</el-tag>
            </div>
            <h1 class="detail-title" :class="{ bold: notification.isBold === 1 }">
              {{ notification.title || '通知' }}
            </h1>
          </div>

          <!-- 元信息 -->
          <div class="detail-meta">
            <span class="meta-item">
              <el-icon><Clock /></el-icon>
              {{ formatTime(notification.createdAt) }}
            </span>
            <span v-if="notification.type === 10" class="meta-item system-tag">
              <el-icon><Bell /></el-icon>
              系统通知
            </span>
          </div>

          <!-- 分隔线 -->
          <el-divider />

          <!-- 内容区域 -->
          <div class="detail-body">
            <!-- 图片展示 -->
            <div v-if="notification.imageUrl" class="detail-image">
              <el-image
                :src="notification.imageUrl"
                :preview-src-list="[notification.imageUrl]"
                fit="cover"
                loading="lazy"
              />
            </div>

            <!-- 文字内容 -->
            <div class="detail-text" :class="getContentClass()">
              {{ notification.content }}
            </div>
          </div>

          <!-- 底部操作 -->
          <div class="detail-footer">
            <el-button type="primary" @click="goBack">返回通知列表</el-button>
          </div>
        </template>

        <el-empty v-else-if="!loading" description="通知不存在" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Clock, Bell } from '@element-plus/icons-vue'
import { getNotificationDetail } from '@/api/notification'

const route = useRoute()
const router = useRouter()
const notification = ref(null)
const loading = ref(false)

onMounted(() => {
  loadNotification()
})

async function loadNotification() {
  loading.value = true
  try {
    const id = route.params.id
    const res = await getNotificationDetail(id)
    notification.value = res.data
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.push('/notifications')
}

function formatTime(time) {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function getContentClass() {
  if (notification.value?.contentType === 2) {
    return 'image-only'
  }
  return ''
}
</script>

<style scoped>
.notif-detail-page { max-width: 800px; margin: 0 auto; padding: 20px; }
.card { background: #fff; border-radius: 12px; padding: 24px; border: 1px solid #ebeef5; box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05); }

.detail-header { margin-bottom: 20px; }

.detail-content { min-height: 300px; }

.detail-title-wrapper { margin-bottom: 16px; }

.detail-tags { display: flex; gap: 8px; margin-bottom: 12px; }

.detail-title {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  line-height: 1.5;
  margin: 0;
}
.detail-title.bold { font-weight: 700; color: #1a1a1a; }

.detail-meta { display: flex; align-items: center; gap: 20px; color: #909399; font-size: 14px; }
.meta-item { display: flex; align-items: center; gap: 4px; }
.system-tag { color: #409eff; }

.detail-body { padding: 20px 0; min-height: 200px; }

.detail-image {
  margin-bottom: 20px;
  border-radius: 8px;
  overflow: hidden;
}
.detail-image .el-image { width: 100%; max-height: 500px; }

.detail-text {
  font-size: 16px;
  line-height: 1.8;
  color: #303133;
  white-space: pre-wrap;
  word-break: break-word;
}
.detail-text.image-only { font-size: 18px; }

.detail-footer {
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
  display: flex;
  justify-content: center;
}
</style>
