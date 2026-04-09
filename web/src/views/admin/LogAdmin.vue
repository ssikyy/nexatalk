<template>
  <div class="admin-page">
    <!-- 搜索区域 -->
    <div class="search-section">
      <div class="search-title">
        <el-icon><Search /></el-icon>
        <span>筛选条件</span>
      </div>
      <div class="search-bar">
        <el-input
          v-model="searchUsername"
          placeholder="搜索操作用户"
          clearable
          @clear="fetchLogs"
          @keyup.enter="fetchLogs"
          class="search-input"
        >
          <template #prefix>
            <el-icon><User /></el-icon>
          </template>
        </el-input>
        <el-select v-model="searchModule" placeholder="模块" clearable @change="fetchLogs" class="search-select">
          <el-option label="全部模块" :value="null" />
          <el-option label="用户管理" value="users" />
          <el-option label="分区管理" value="sections" />
          <el-option label="帖子管理" value="posts" />
          <el-option label="评论管理" value="comments" />
          <el-option label="举报管理" value="reports" />
          <el-option label="处罚管理" value="punishments" />
          <el-option label="消息管理" value="messages" />
        </el-select>
        <el-select v-model="searchOperation" placeholder="操作类型" clearable @change="fetchLogs" class="search-select">
          <el-option label="全部操作" :value="null" />
          <el-option label="新增" value="create" />
          <el-option label="更新" value="update" />
          <el-option label="删除" value="delete" />
          <el-option label="查询" value="list" />
          <el-option label="禁言用户" value="禁言用户" />
          <el-option label="封号用户" value="封号用户" />
          <el-option label="解除禁言" value="解除禁言" />
          <el-option label="解除封号" value="解除封号" />
        </el-select>
        <el-select v-model="searchStatus" placeholder="结果" clearable @change="fetchLogs" class="search-select">
          <el-option label="全部结果" :value="null" />
          <el-option label="成功" :value="1" />
          <el-option label="失败" :value="0" />
        </el-select>
        <el-date-picker
          v-model="dateRange"
          type="datetimerange"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          format="YYYY-MM-DD HH:mm:ss"
          value-format="YYYY-MM-DD HH:mm:ss"
          :shortcuts="dateShortcuts"
          class="date-picker"
          @change="handleDateChange"
        />
        <el-button type="primary" @click="fetchLogs" class="search-btn">
          <el-icon><Search /></el-icon>
          搜索
        </el-button>
        <el-button @click="resetSearch" class="reset-btn">
          <el-icon><RefreshLeft /></el-icon>
          重置
        </el-button>
        <el-button
          v-if="userStore.isSuperAdmin"
          type="danger"
          @click="handleClearLogs"
          class="clear-btn"
        >
          <el-icon><Delete /></el-icon>
          清空日志
        </el-button>
      </div>
    </div>

    <!-- 数据表格 -->
    <el-table
      :data="logs"
      border
      stripe
      style="width: 100%"
      v-loading="loading"
      class="data-table"
    >
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="操作用户" min-width="120" />
      <el-table-column prop="module" label="模块" min-width="100">
        <template #default="{ row }">
          {{ getModuleName(row.module) }}
        </template>
      </el-table-column>
      <el-table-column prop="operation" label="操作" min-width="120" />
      <el-table-column prop="method" label="请求方法" min-width="100" />
      <el-table-column prop="ip" label="IP地址" min-width="140" />
      <el-table-column prop="status" label="结果" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" effect="light">
            {{ row.status === 1 ? '成功' : '失败' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="duration" label="耗时" width="80">
        <template #default="{ row }">
          {{ row.duration }}ms
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="操作时间" width="180">
        <template #default="{ row }">
          {{ formatDate(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="80" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" link @click="viewDetail(row)">
            <el-icon><View /></el-icon>详情
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="page"
      v-model:page-size="pageSize"
      :total="total"
      :page-sizes="[20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      @size-change="fetchLogs"
      @current-change="fetchLogs"
      class="pagination"
    />

    <!-- 详情弹窗 -->
    <el-dialog
      v-model="detailVisible"
      title="日志详情"
      width="700px"
      destroy-on-close
    >
      <el-descriptions :column="2" border>
        <el-descriptions-item label="ID">{{ currentLog.id }}</el-descriptions-item>
        <el-descriptions-item label="操作用户">{{ currentLog.username }}</el-descriptions-item>
        <el-descriptions-item label="模块">{{ currentLog.module }}</el-descriptions-item>
        <el-descriptions-item label="操作">{{ currentLog.operation }}</el-descriptions-item>
        <el-descriptions-item label="请求方法">{{ currentLog.method }}</el-descriptions-item>
        <el-descriptions-item label="IP地址">{{ currentLog.ip }}</el-descriptions-item>
        <el-descriptions-item label="结果">
          <el-tag :type="currentLog.status === 1 ? 'success' : 'danger'" effect="light">
            {{ currentLog.status === 1 ? '成功' : '失败' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="耗时">{{ currentLog.duration }}ms</el-descriptions-item>
        <el-descriptions-item label="操作时间" :span="2">
          {{ formatDate(currentLog.createdAt) }}
        </el-descriptions-item>
        <el-descriptions-item label="请求参数" :span="2">
          <pre class="log-content">{{ currentLog.params || '-' }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="返回结果" :span="2">
          <pre class="log-content">{{ currentLog.result || '-' }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="错误信息" :span="2" v-if="currentLog.errorMsg">
          <pre class="log-content error">{{ currentLog.errorMsg }}</pre>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { adminListLogs, adminClearLogs } from '@/api/log'
import { Search, User, RefreshLeft, View, Delete } from '@element-plus/icons-vue'

const userStore = useUserStore()

const logs = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)

const searchUsername = ref('')
const searchModule = ref(null)
const searchOperation = ref(null)
const searchStatus = ref(null)
const dateRange = ref(null)

const dateShortcuts = [
  {
    text: '今天',
    value: () => {
      const start = new Date()
      start.setHours(0, 0, 0, 0)
      const end = new Date()
      return [start, end]
    }
  },
  {
    text: '最近7天',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setDate(start.getDate() - 7)
      return [start, end]
    }
  }
]

const detailVisible = ref(false)
const currentLog = ref({})

const fetchLogs = async () => {
  loading.value = true
  try {
    const res = await adminListLogs({
      page: page.value,
      pageSize: pageSize.value,
      username: searchUsername.value || undefined,
      module: searchModule.value,
      operation: searchOperation.value,
      status: searchStatus.value,
      startTime: dateRange.value?.[0],
      endTime: dateRange.value?.[1]
    })
    const data = res.data
    logs.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

const resetSearch = () => {
  searchUsername.value = ''
  searchModule.value = null
  searchOperation.value = null
  searchStatus.value = null
  dateRange.value = null
  page.value = 1
  fetchLogs()
}

const handleDateChange = () => {
  page.value = 1
  fetchLogs()
}

const viewDetail = (row) => {
  currentLog.value = row
  detailVisible.value = true
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

const moduleMap = {
  users: '用户管理',
  sections: '分区管理',
  posts: '帖子管理',
  comments: '评论管理',
  reports: '举报管理',
  punishments: '处罚管理',
  messages: '消息管理',
  feeds: '动态管理',
  favorites: '收藏管理',
  follows: '关注管理',
  likes: '点赞管理'
}

const getModuleName = (module) => {
  return moduleMap[module] || module
}

const handleClearLogs = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要清空所有操作日志吗？此操作不可恢复！',
      '警告',
      {
        confirmButtonText: '确定清空',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    await adminClearLogs()
    ElMessage.success('日志已清空')
    fetchLogs()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.response?.data?.message || '清空失败')
    }
  }
}

onMounted(fetchLogs)
</script>

<style scoped>
.admin-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.search-section {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 16px 20px;
  border: 1px solid #e8e8e8;
}

.search-title {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #606266;
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 12px;
}

.search-bar {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.search-input {
  width: 200px;
}

.search-select {
  width: 140px;
}

.search-btn,
.reset-btn {
  display: flex;
  align-items: center;
  gap: 4px;
}

.data-table {
  border-radius: 8px;
  overflow: hidden;
}

.data-table :deep(.el-table__header-wrapper th) {
  background: #f5f7fa;
  color: #606266;
  font-weight: 600;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  padding: 16px 0 0;
}

.log-content {
  max-height: 200px;
  overflow: auto;
  background: #f5f7fa;
  padding: 8px;
  border-radius: 4px;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
  margin: 0;
}

.log-content.error {
  color: #f56c6c;
  background: #fef0f0;
}
</style>
