<template>
  <div class="admin-page">
    <!-- 操作栏 -->
    <div class="action-bar">
      <div class="action-title">
        <el-icon><Bell /></el-icon>
        <span>系统通知管理</span>
      </div>
      <el-button type="primary" @click="$router.push('/admin/notification/create')">
        <el-icon><Plus />发布通知</el-icon>
      </el-button>
    </div>

    <!-- 数据表格 -->
    <el-table
      :data="notifications"
      border
      stripe
      style="width: 100%"
      v-loading="loading"
      class="data-table"
    >
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="title" label="标题" min-width="150">
        <template #default="{ row }">
          <span :class="{ 'title-bold': row.isBold === 1 }">{{ row.title }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="content" label="内容摘要" min-width="200">
        <template #default="{ row }">
          <span class="content-preview">{{ getContentPreview(row.content) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="内容类型" width="100">
        <template #default="{ row }">
          <el-tag :type="getContentTypeTag(row.contentType)" size="small">
            {{ getContentTypeText(row.contentType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="属性" width="140">
        <template #default="{ row }">
          <div class="tag-group">
            <el-tag v-if="row.isPinned === 1" type="danger" size="small">置顶</el-tag>
            <el-tag v-if="row.isBold === 1" type="warning" size="small">粗体</el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="发布时间" width="170">
        <template #default="{ row }">
          {{ formatTime(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" link @click="$router.push(`/admin/notification/${row.id}/edit`)">
            <el-icon><Edit /></el-icon>编辑
          </el-button>
          <el-button size="small" type="danger" link @click="handleDelete(row)">
            <el-icon><Delete /></el-icon>删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <el-pagination
      v-if="total > pageSize"
      v-model:current-page="currentPage"
      :page-size="pageSize"
      :total="total"
      layout="prev, pager, next"
      background
      class="pagination"
      @current-change="loadNotifications"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listSystemNotifications,
  deleteSystemNotification
} from '@/api/notification'
import { Bell, Plus, Edit, Delete } from '@element-plus/icons-vue'

const notifications = ref([])
const loading = ref(false)

// 分页
const currentPage = ref(1)
const pageSize = 10
const total = ref(0)

const loadNotifications = async () => {
  loading.value = true
  try {
    const res = await listSystemNotifications({ page: currentPage.value, pageSize })
    notifications.value = res.data?.list || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除这条通知吗？', '提示', { type: 'warning' })
    await deleteSystemNotification(row.id)
    ElMessage.success('删除成功')
    await loadNotifications()
  } catch {}
}

const getContentPreview = (content) => {
  if (!content) return ''
  return content.length > 50 ? content.substring(0, 50) + '...' : content
}

const getContentTypeText = (type) => {
  const map = { 1: '纯文字', 2: '图片', 3: '图文' }
  return map[type] || '未知'
}

const getContentTypeTag = (type) => {
  const map = { 1: 'info', 2: 'success', 3: 'warning' }
  return map[type] || 'info'
}

const formatTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

onMounted(loadNotifications)
</script>

<style scoped>
.admin-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.action-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a2e;
}

.action-title .el-icon {
  color: #409eff;
}

/* 表格容器 */
.table-container {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  border: 1px solid #f0f0f0;
}

.data-table {
  border-radius: 8px;
  overflow: hidden;
}

.data-table :deep(.el-table__header-wrapper th) {
  background: #f8f9fa;
  color: #606266;
  font-weight: 600;
}

.title-bold {
  font-weight: 600;
}

.content-preview {
  color: #606266;
  font-size: 13px;
}

.tag-group {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding: 16px 0 0;
}

:deep(.el-dialog__header) {
  border-bottom: 1px solid #f0f0f0;
  padding-bottom: 16px;
}

:deep(.el-dialog__footer) {
  border-top: 1px solid #f0f0f0;
  padding-top: 16px;
}
</style>
