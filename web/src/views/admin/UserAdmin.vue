<template>
  <div class="admin-page">
    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%)">
          <el-icon><User /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ total }}</div>
          <div class="stat-label">用户总数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%)">
          <el-icon><CircleCheck /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ normalCount }}</div>
          <div class="stat-label">正常用户</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%)">
          <el-icon><Mute /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ mutedCount }}</div>
          <div class="stat-label">禁言用户</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #eb3349 0%, #f45c43 100%)">
          <el-icon><Lock /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ bannedCount }}</div>
          <div class="stat-label">封禁用户</div>
        </div>
      </div>
    </div>

    <!-- 搜索区域 -->
    <div class="search-section">
      <div class="search-bar">
        <el-input
          v-model="searchUsername"
          placeholder="搜索用户名或昵称"
          clearable
          @clear="fetchUsers"
          @keyup.enter="fetchUsers"
          class="search-input"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select v-model="searchRole" placeholder="角色" clearable @change="fetchUsers" class="search-select">
          <el-option label="全部角色" :value="null" />
          <el-option label="普通用户" :value="0" />
          <el-option v-if="userStore.isSuperAdmin" label="管理员" :value="1" />
          <el-option v-if="userStore.isSuperAdmin" label="超级管理员" :value="2" />
        </el-select>
        <el-select v-model="searchStatus" placeholder="状态" clearable @change="fetchUsers" class="search-select">
          <el-option label="全部状态" :value="null" />
          <el-option label="正常" :value="0" />
          <el-option label="禁言" :value="1" />
          <el-option label="封禁" :value="2" />
        </el-select>
        <el-button type="primary" @click="fetchUsers">
          <el-icon><Search /></el-icon>
          搜索
        </el-button>
        <el-button @click="resetSearch">
          <el-icon><RefreshLeft /></el-icon>
          重置
        </el-button>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="table-container">
      <el-table
        :data="users"
        border
        stripe
        style="width: 100%"
        v-loading="loading"
        class="data-table"
      >
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="用户信息" min-width="180">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="40" :src="row.avatarUrl">
                {{ row.nickname?.charAt(0) || 'U' }}
              </el-avatar>
              <div class="user-info">
                <div class="user-nickname">{{ row.nickname || '-' }}</div>
                <div class="user-username">@{{ row.username }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="role" label="角色" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.role === 2" type="warning" effect="dark" size="small">超级管理员</el-tag>
            <el-tag v-else-if="row.role === 1" type="danger" effect="light" size="small">管理员</el-tag>
            <el-tag v-else type="info" effect="light" size="small">普通用户</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.status === 0" type="success" effect="light" size="small">正常</el-tag>
            <el-tag v-else-if="row.status === 1" type="warning" effect="light" size="small">禁言</el-tag>
            <el-tag v-else-if="row.status === 2" type="danger" effect="light" size="small">封禁</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="注册时间" width="170">
          <template #default="{ row }">
            <span class="time-text">{{ formatDate(row.createdAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button v-if="canEdit(row)" size="small" type="primary" link @click="openEditDialog(row)">
                <el-icon><Edit /></el-icon>编辑
              </el-button>
              <el-button
                v-if="canResetPassword(row)"
                size="small"
                type="warning"
                link
                :loading="resettingUserId === row.id"
                @click="handleResetPassword(row)"
              >
                重置密码
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchUsers"
          @current-change="fetchUsers"
        />
      </div>
    </div>

    <!-- 编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      title="编辑用户"
      width="500px"
      destroy-on-close
      class="edit-dialog"
    >
      <el-form :model="form" label-width="80px">
        <el-form-item label="头像">
          <div class="avatar-edit">
            <el-avatar :size="60" :src="form.avatarUrl">
              {{ form.nickname?.charAt(0) || 'U' }}
            </el-avatar>
          </div>
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="form.bio" type="textarea" :rows="3" placeholder="请输入简介" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select
            v-if="userStore.isSuperAdmin"
            v-model="form.role"
            class="role-select"
            placeholder="请选择角色"
          >
            <el-option label="普通用户" :value="0" />
            <el-option label="管理员" :value="1" />
          </el-select>
          <template v-else>
            <el-tag v-if="form.role === 2" type="warning" effect="dark">超级管理员</el-tag>
            <el-tag v-else-if="form.role === 1" type="danger" effect="light">管理员</el-tag>
            <el-tag v-else type="info" effect="light">普通用户</el-tag>
          </template>
          <div class="form-tip">
            {{ userStore.isSuperAdmin ? '只能在普通用户和管理员之间切换，不能授予超级管理员。' : '仅超级管理员可调整角色。' }}
          </div>
        </el-form-item>
        <el-form-item label="状态">
          <el-tag v-if="form.status === 0" type="success" effect="light">正常</el-tag>
          <el-tag v-else-if="form.status === 1" type="warning" effect="light">禁言</el-tag>
          <el-tag v-else-if="form.status === 2" type="danger" effect="light">封禁</el-tag>
          <div class="form-tip">处罚请到"处罚管理"页面操作</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import {
  adminListUsers,
  adminResetPassword,
  adminUpdateUser
} from '@/api/user'
import {
  Search, User, RefreshLeft, Edit, CircleCheck, Mute, Lock
} from '@element-plus/icons-vue'

const userStore = useUserStore()

const users = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const searchUsername = ref('')
const searchRole = ref(null)
const searchStatus = ref(null)

const dialogVisible = ref(false)
const saving = ref(false)
const resettingUserId = ref(null)
const form = ref({
  id: null,
  nickname: '',
  bio: '',
  avatarUrl: '',
  role: 0,
  status: 0
})

// 统计数据
const normalCount = computed(() => users.value.filter(u => u.status === 0).length)
const mutedCount = computed(() => users.value.filter(u => u.status === 1).length)
const bannedCount = computed(() => users.value.filter(u => u.status === 2).length)

// 判断是否可以编辑（不能编辑超级管理员）
const canEdit = (row) => {
  // 超级管理员不能被编辑
  if (row.role === 2) return false
  // 管理员只能由超级管理员编辑
  if (row.role === 1 && !userStore.isSuperAdmin) return false
  return true
}

const canResetPassword = (row) => {
  if (row.role === 2) return false
  if (userStore.isSuperAdmin) return true
  return row.role === 0
}

const fetchUsers = async () => {
  loading.value = true
  try {
    const res = await adminListUsers({
      page: page.value,
      pageSize: pageSize.value,
      username: searchUsername.value || undefined,
      role: searchRole.value,
      status: searchStatus.value
    })
    const data = res.data
    users.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

const resetSearch = () => {
  searchUsername.value = ''
  searchRole.value = null
  searchStatus.value = null
  page.value = 1
  fetchUsers()
}

const openEditDialog = (row) => {
  form.value = {
    id: row.id,
    nickname: row.nickname,
    bio: row.bio,
    avatarUrl: row.avatarUrl,
    role: row.role,
    status: row.status
  }
  dialogVisible.value = true
}

const submit = async () => {
  if (!form.value.nickname.trim()) {
    return ElMessage.warning('昵称不能为空')
  }
  saving.value = true
  try {
    const payload = {
      nickname: form.value.nickname,
      bio: form.value.bio
    }

    if (userStore.isSuperAdmin) {
      payload.role = form.value.role
    }

    await adminUpdateUser(form.value.id, payload)
    ElMessage.success('更新成功')
    dialogVisible.value = false
    await fetchUsers()
  } finally {
    saving.value = false
  }
}

const handleResetPassword = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要重置用户 @${row.username} 的密码吗？`,
      '重置密码',
      {
        confirmButtonText: '确定重置',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    resettingUserId.value = row.id
    const res = await adminResetPassword(row.id)
    await ElMessageBox.alert(res.data || '密码已重置为默认值', '重置成功', {
      confirmButtonText: '知道了',
      type: 'success'
    })
    await fetchUsers()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      throw error
    }
  } finally {
    resettingUserId.value = null
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

onMounted(fetchUsers)
</script>

<style scoped>
.admin-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 统计卡片 */
.stats-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  border: 1px solid #f0f0f0;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-icon .el-icon {
  font-size: 24px;
  color: #fff;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a2e;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

/* 搜索区域 */
.search-section {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  border: 1px solid #f0f0f0;
}

.search-bar {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  align-items: center;
}

.search-input {
  width: 220px;
}

.search-select {
  width: 140px;
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

.user-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-info {
  display: flex;
  flex-direction: column;
}

.user-nickname {
  font-weight: 500;
  color: #303133;
}

.user-username {
  font-size: 12px;
  color: #909399;
}

.time-text {
  font-size: 13px;
  color: #909399;
}

.table-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding: 16px 0 0;
}

/* 编辑弹窗 */
.avatar-edit {
  display: flex;
  align-items: center;
  gap: 16px;
}

.role-select {
  width: 100%;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 6px;
}

:deep(.edit-dialog .el-dialog__header) {
  border-bottom: 1px solid #f0f0f0;
  padding-bottom: 16px;
}

:deep(.edit-dialog .el-dialog__footer) {
  border-top: 1px solid #f0f0f0;
  padding-top: 16px;
}
</style>
