<template>
  <div class="admin-page">
    <!-- 下处罚区域 -->
    <div class="page-section">
      <div class="section-title">
        <el-icon><Lock /></el-icon>
        <span>下处罚</span>
      </div>
      <el-card class="form-card">
        <el-form :model="punishForm" label-width="100px">
          <!-- 用户搜索 -->
          <el-form-item label="选择用户">
            <div class="user-select-wrapper">
              <el-select
                v-model="punishForm.userId"
                filterable
                remote
                reserve-keyword
                placeholder="搜索用户名或昵称"
                :remote-method="searchUsers"
                :loading="searchingUser"
                @change="handleUserSelect"
                style="width: 100%"
              >
                <el-option
                  v-for="user in searchResults"
                  :key="user.id"
                  :label="getUserOptionLabel(user)"
                  :value="user.id"
                >
                  <div class="user-option">
                    <el-avatar :size="28" :src="user.avatarUrl">
                      {{ user.nickname?.charAt(0) || 'U' }}
                    </el-avatar>
                    <span class="user-name">{{ user.nickname || user.username }}</span>
                    <span class="user-username">@{{ user.username }}</span>
                  </div>
                </el-option>
              </el-select>
            </div>
          </el-form-item>
          <!-- 已选用户信息 -->
          <el-form-item label="已选用户" v-if="selectedUser">
            <div class="selected-user">
              <el-avatar :size="40" :src="selectedUser.avatarUrl">
                {{ selectedUser.nickname?.charAt(0) || 'U' }}
              </el-avatar>
              <div class="user-info">
                <div class="user-nickname">{{ selectedUser.nickname || selectedUser.username }}</div>
                <div class="user-detail">@{{ selectedUser.username }} · ID: {{ selectedUser.id }}</div>
              </div>
              <el-tag v-if="selectedUser.role === 2" type="warning" size="small">超级管理员</el-tag>
              <el-tag v-else-if="selectedUser.role === 1" type="danger" size="small">管理员</el-tag>
              <el-tag v-else :type="selectedUser.status === 0 ? 'success' : 'danger'" size="small">
                {{ selectedUser.status === 0 ? '正常' : selectedUser.status === 1 ? '禁言' : '封禁' }}
              </el-tag>
            </div>
            <div v-if="selectedUser.role === 1 || selectedUser.role === 2" class="admin-warning">
              <el-icon><Warning /></el-icon> 管理员和超级管理员不能被处罚
            </div>
          </el-form-item>
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="处罚类型">
                <el-select v-model="punishForm.type" style="width: 100%">
                  <el-option :value="1" label="禁言" />
                  <el-option :value="2" label="封号" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="时长(小时)">
                <el-input-number
                  v-model="punishForm.durationHours"
                  :min="1"
                  :max="24 * 365"
                  placeholder="为空则永久"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="处罚原因">
            <el-input
              v-model="punishForm.reason"
              placeholder="请输入处罚原因"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="punishing" :disabled="!punishForm.userId || (selectedUser && (selectedUser.role === 1 || selectedUser.role === 2))" @click="submitPunish">
              <el-icon><Select /></el-icon>下处罚
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>

    <!-- 待处罚违规列表 -->
    <div class="page-section">
      <div class="section-title">
        <el-icon><Document /></el-icon>
        <span>待处罚违规列表</span>
        <el-button size="small" @click="fetchViolatedReports">
          <el-icon><RefreshRight /></el-icon>刷新
        </el-button>
      </div>
      <el-card class="table-card">
        <el-table
          :data="violatedReports"
          border
          stripe
          v-loading="loadingViolated"
          class="data-table"
        >
          <el-table-column prop="id" label="举报ID" width="80" />
          <el-table-column prop="entityTypeText" label="类型" width="80">
            <template #default="{ row }">
              <el-tag :type="row.entityType === 1 ? 'primary' : 'info'" size="small">
                {{ row.entityTypeText }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="entityTitle" label="内容标题" min-width="150" show-overflow-tooltip />
          <el-table-column prop="reportedNickname" label="违规用户" width="120">
            <template #default="{ row }">
              <span>{{ row.reportedNickname || row.reportedUsername }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="reasonText" label="举报原因" width="100" />
          <el-table-column prop="statusText" label="审核状态" width="100">
            <template #default="{ row }">
              <el-tag type="danger" size="small">{{ row.statusText }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="reviewedAt" label="审核时间" width="160">
            <template #default="{ row }">
              {{ formatDate(row.reviewedAt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="primary" link @click="viewContent(row)">
                <el-icon><View /></el-icon>查看
              </el-button>
              <el-button size="small" type="warning" link @click="fillPunishForm(row)">
                <el-icon><Lock /></el-icon>处罚
              </el-button>
              <el-button
                v-if="row.entityType === 1"
                size="small"
                type="danger"
                link
                @click="deletePost(row)"
              >
                <el-icon><Delete /></el-icon>删帖
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>

    <!-- 当前生效的处罚 -->
    <div class="page-section">
      <div class="section-title">
        <el-icon><Warning /></el-icon>
        <span>当前生效的处罚</span>
        <el-button size="small" @click="fetchActivePunishments">
          <el-icon><RefreshRight /></el-icon>刷新
        </el-button>
      </div>
      <el-card class="table-card">
        <el-table
          :data="activePunishments"
          border
          stripe
          v-loading="loadingActive"
          class="data-table"
        >
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="userId" label="用户ID" width="100" />
          <el-table-column prop="userNickname" label="用户昵称" min-width="120" />
          <el-table-column prop="type" label="类型" width="100">
            <template #default="{ row }">
              <el-tag :type="row.type === 1 ? 'warning' : 'danger'" effect="light">
                {{ row.type === 1 ? '禁言' : '封号' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="reason" label="原因" min-width="200" />
          <el-table-column prop="expireAt" label="到期时间" width="180">
            <template #default="{ row }">
              {{ row.expireAt || '永久' }}
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="180">
            <template #default="{ row }">
              {{ formatDate(row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="success" link @click="lift(row)">
                <el-icon><CircleClose /></el-icon>解除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>

    <!-- 用户处罚历史 -->
    <div class="page-section">
      <div class="section-title">
        <el-icon><Clock /></el-icon>
        <span>用户处罚历史</span>
      </div>
      <el-card class="table-card">
        <div class="search-bar">
          <el-select
            v-model="historyUserId"
            filterable
            remote
            reserve-keyword
            placeholder="搜索用户名或昵称"
            :remote-method="searchHistoryUsers"
            :loading="searchingHistoryUser"
            @change="handleHistoryUserChange"
            style="width: 280px"
          >
            <el-option
              v-for="user in historySearchResults"
              :key="user.id"
              :label="getUserOptionLabel(user)"
              :value="user.id"
            >
              <div class="user-option">
                <el-avatar :size="24" :src="user.avatarUrl">
                  {{ user.nickname?.charAt(0) || 'U' }}
                </el-avatar>
                <span class="user-name">{{ user.nickname || user.username }}</span>
                <span class="user-username">@{{ user.username }}</span>
              </div>
            </el-option>
          </el-select>
          <el-button type="primary" @click="fetchUserHistory" :disabled="!historyUserId">
            <el-icon><Search /></el-icon>查询
          </el-button>
        </div>
        <el-table
          :data="historyPunishments"
          border
          stripe
          v-loading="loadingHistory"
          class="data-table"
        >
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="userId" label="用户ID" width="100" />
          <el-table-column prop="userNickname" label="用户昵称" min-width="120" />
          <el-table-column prop="type" label="类型" width="100">
            <template #default="{ row }">
              <el-tag :type="row.type === 1 ? 'warning' : 'danger'" effect="light">
                {{ row.type === 1 ? '禁言' : '封号' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="reason" label="原因" min-width="200" />
          <el-table-column prop="isActive" label="是否生效" width="100">
            <template #default="{ row }">
              <el-tag :type="row.isActive === 1 ? 'danger' : 'info'" effect="light">
                {{ row.isActive === 1 ? '生效中' : '已解除' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="expireAt" label="到期时间" width="180">
            <template #default="{ row }">
              {{ row.expireAt || '永久' }}
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="180">
            <template #default="{ row }">
              {{ formatDate(row.createdAt) }}
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>
  </div>

  <!-- 内容查看对话框 -->
  <el-dialog v-model="contentDialogVisible" title="查看内容详情" width="700px">
    <div v-if="selectedReport" class="content-detail">
      <div class="detail-item">
        <span class="label">内容类型：</span>
        <el-tag :type="selectedReport.entityType === 1 ? 'primary' : 'info'">
          {{ selectedReport.entityTypeText }}
        </el-tag>
      </div>
      <div class="detail-item">
        <span class="label">举报原因：</span>
        <span>{{ selectedReport.reasonText }}</span>
      </div>
      <div class="detail-item">
        <span class="label">举报详情：</span>
        <span>{{ selectedReport.detail || '无' }}</span>
      </div>
      <div class="detail-item">
        <span class="label">审核备注：</span>
        <span>{{ selectedReport.reviewNote || '无' }}</span>
      </div>
      <div class="detail-item">
        <span class="label">内容标题：</span>
        <span>{{ selectedReport.entityTitle || '无' }}</span>
      </div>
      <div v-if="selectedReport.postContent" class="detail-item">
        <span class="label">内容正文：</span>
        <div class="post-content">{{ selectedReport.postContent }}</div>
      </div>
    </div>
    <template #footer>
      <el-button @click="contentDialogVisible = false">关闭</el-button>
      <el-button type="primary" @click="fillPunishForm(selectedReport); contentDialogVisible = false">
        处罚该用户
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  adminCreatePunishment,
  adminLiftPunishment,
  adminListActivePunishments,
  adminListUserPunishments
} from '@/api/punishment'
import { adminListUsers } from '@/api/user'
import { adminListReports, adminClearReportsByEntity } from '@/api/report'
import { deletePost as deletePostApi } from '@/api/post'
import { Lock, Warning, Clock, Select, RefreshRight, CircleClose, Search, Document, View, Delete, WarningFilled } from '@element-plus/icons-vue'

const punishForm = ref({
  userId: null,
  type: 1,
  durationHours: null,
  reason: ''
})

const punishing = ref(false)
const searchingUser = ref(false)
const searchResults = ref([])
const selectedUser = ref(null)

const activePunishments = ref([])
const loadingActive = ref(false)

const historyUserId = ref(null)
const historySearchResults = ref([])
const searchingHistoryUser = ref(false)
const historyPunishments = ref([])
const loadingHistory = ref(false)

// 待处罚违规列表相关变量
const violatedReports = ref([])
const loadingViolated = ref(false)
const selectedReport = ref(null)
const contentDialogVisible = ref(false)

// 待处罚违规列表（去重后，一条内容只显示一行）
const fetchViolatedReports = async () => {
  loadingViolated.value = true
  try {
    // 查询已违规的举报列表（status = 1）
    const res = await adminListReports({ status: 1, page: 1, pageSize: 200 })
    const list = res.data?.list || []
    const map = new Map()
    list.forEach((item) => {
      const key = `${item.entityType}-${item.entityId}`
      if (!map.has(key)) {
        map.set(key, item)
      } else {
        const existing = map.get(key)
        const existingTime = new Date(existing.reviewedAt || existing.createdAt || 0).getTime()
        const currentTime = new Date(item.reviewedAt || item.createdAt || 0).getTime()
        if (currentTime > existingTime) {
          map.set(key, item)
        }
      }
    })
    violatedReports.value = Array.from(map.values())
  } catch (e) {
    console.error('获取违规列表失败', e)
  } finally {
    loadingViolated.value = false
  }
}

// 查看帖子/评论内容
const viewContent = (row) => {
  selectedReport.value = row
  contentDialogVisible.value = true
}

// 填充处罚表单（从违规列表中选择用户）
const fillPunishForm = async (row) => {
  punishForm.value.userId = row.reportedUserId
  punishForm.value.reason = `因${row.reasonText}被举报，违规内容：${row.entityTitle || ''}`
  // 查找用户信息
  await searchUsers(row.reportedNickname || row.reportedUsername)
  const user = searchResults.value.find(u => u.id === row.reportedUserId)
  if (user) {
    selectedUser.value = user
  }
  // 滚动到页面顶部
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

// 删除帖子
const deletePost = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该帖子吗？此操作不可恢复。', '删除确认', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    })

    // 先尝试清理举报记录（即使删帖失败也要清理记录）
    const entityType = row.entityType
    const entityId = row.entityId

    // 删除帖子（可能失败，比如帖子已不存在）
    try {
      await deletePostApi(row.entityId)
      ElMessage.success('帖子已删除')
    } catch (deleteError) {
      // 帖子可能已经删除或不存在，但仍然继续清理举报记录
      console.warn('删除帖子可能失败:', deleteError)
      ElMessage.warning('帖子可能已不存在，但将继续清理举报记录')
    }

    // 无论删帖成功与否，都清理该帖子的所有举报记录
    await clearReportsForEntity(entityType, entityId)
    ElMessage.success('已清理相关举报记录')
  } catch (e) {
    if (e !== 'cancel') {
      console.error('操作失败', e)
      ElMessage.error(e?.response?.data?.message || '操作失败')
    }
  }
}

// 清理某个被举报实体的所有举报记录（前端列表 + 尝试调用后端）
const clearReportsForEntity = async (entityType, entityId) => {
  if (!entityType || !entityId) return

  // 前端本地先移除（确保用户体验）
  violatedReports.value = violatedReports.value.filter(
    (r) => !(r.entityType === entityType && r.entityId === entityId)
  )

  // 尝试调用后端清理（失败不影响前端）
  try {
    await adminClearReportsByEntity({
      entityType: Number(entityType),
      entityId: Number(entityId)
    })
  } catch (e) {
    console.warn('后端清理举报记录失败（前端已移除）:', e)
  }
}

let punishSearchTimer = null
let historySearchTimer = null

const getUserOptionLabel = (user) => {
  if (!user) return ''
  const nickname = (user.nickname || '').trim()
  const username = (user.username || '').trim()
  if (nickname && username && nickname !== username) {
    return `${nickname} (@${username})`
  }
  return nickname || username
}

// 搜索用户（处罚表单用）
const searchUsers = (keyword) => {
  const normalizedKeyword = (keyword || '').trim()
  if (!normalizedKeyword) {
    searchResults.value = []
    return Promise.resolve()
  }
  clearTimeout(punishSearchTimer)
  return new Promise((resolve) => {
    punishSearchTimer = setTimeout(async () => {
      searchingUser.value = true
      try {
        const res = await adminListUsers({ page: 1, pageSize: 10, username: normalizedKeyword })
        searchResults.value = res.data?.list || []
      } catch (e) {
        console.error('搜索用户失败', e)
      } finally {
        searchingUser.value = false
        resolve()
      }
    }, 300)
  })
}

// 搜索用户（历史查询用）
const searchHistoryUsers = (keyword) => {
  const normalizedKeyword = (keyword || '').trim()
  if (!normalizedKeyword) {
    historySearchResults.value = []
    return
  }
  clearTimeout(historySearchTimer)
  historySearchTimer = setTimeout(async () => {
    searchingHistoryUser.value = true
    try {
      const res = await adminListUsers({ page: 1, pageSize: 10, username: normalizedKeyword })
      historySearchResults.value = res.data?.list || []
    } catch (e) {
      console.error('搜索用户失败', e)
    } finally {
      searchingHistoryUser.value = false
    }
  }, 300)
}

// 选择用户
const handleUserSelect = (userId) => {
  const user = searchResults.value.find(u => u.id === userId)
  selectedUser.value = user || null
}

const handleHistoryUserChange = async (userId) => {
  if (!userId) {
    historyPunishments.value = []
    return
  }
  await fetchUserHistory()
}

const submitPunish = async () => {
  if (!punishForm.value.userId) {
    return ElMessage.warning('请选择用户')
  }
  if (!punishForm.value.reason.trim()) {
    return ElMessage.warning('请填写处罚原因')
  }
  punishing.value = true
  try {
    const payload = { ...punishForm.value }
    if (!payload.durationHours) {
      payload.durationHours = null
    }
    await adminCreatePunishment(payload)
    ElMessage.success('处罚已生效')

    // 如果是从某条举报记录进入的处罚，处罚成功后清理该内容的所有举报记录
    if (selectedReport.value && selectedReport.value.entityType && selectedReport.value.entityId) {
      await clearReportsForEntity(selectedReport.value.entityType, selectedReport.value.entityId)
      selectedReport.value = null
    }

    // 重置表单
    punishForm.value = {
      userId: null,
      type: 1,
      durationHours: null,
      reason: ''
    }
    selectedUser.value = null
    searchResults.value = []
    await fetchActivePunishments()
  } finally {
    punishing.value = false
  }
}

const fetchActivePunishments = async () => {
  loadingActive.value = true
  try {
    const res = await adminListActivePunishments({ page: 1, pageSize: 50 })
    activePunishments.value = res.data?.list ?? []
  } finally {
    loadingActive.value = false
  }
}

const lift = async (row) => {
  await adminLiftPunishment(row.id)
  ElMessage.success('处罚已解除')
  await fetchActivePunishments()
  if (historyUserId.value) {
    await fetchUserHistory()
  }
}

const fetchUserHistory = async () => {
  if (!historyUserId.value) {
    return ElMessage.warning('请先选择用户')
  }
  loadingHistory.value = true
  try {
    const res = await adminListUserPunishments(historyUserId.value, {
      page: 1,
      pageSize: 50
    })
    historyPunishments.value = res.data?.list ?? []
  } finally {
    loadingHistory.value = false
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

onMounted(() => {
  fetchActivePunishments()
  fetchViolatedReports()
})
</script>

<style scoped>
.admin-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.page-section {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  border: 1px solid #f0f0f0;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a2e;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.section-title .el-icon {
  color: #409eff;
}

.section-title .el-button {
  margin-left: auto;
}

.user-select-wrapper {
  width: 100%;
}

.user-option {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-name {
  flex: 1;
}

.user-username {
  color: #909399;
  font-size: 12px;
}

.selected-user {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #f8f9fa;
  padding: 12px;
  border-radius: 10px;
  width: 100%;
}

.admin-warning {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #e6a23c;
  font-size: 13px;
  margin-top: 10px;
  padding: 10px 14px;
  background: #fdf6ec;
  border-radius: 6px;
}

.user-info {
  flex: 1;
}

.user-nickname {
  font-weight: 500;
  color: #303133;
}

.user-detail {
  font-size: 12px;
  color: #909399;
}

.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.search-input {
  width: 200px;
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

.content-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.detail-item .label {
  color: #909399;
  min-width: 80px;
  font-weight: 500;
}

.post-content {
  flex: 1;
  background: #f5f7fa;
  padding: 12px;
  border-radius: 6px;
  max-height: 300px;
  overflow-y: auto;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
