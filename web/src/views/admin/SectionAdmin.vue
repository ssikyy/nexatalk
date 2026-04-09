<template>
  <div class="admin-page">
    <!-- 操作栏 -->
    <div class="action-bar">
      <div class="action-title">
        <el-icon><Folder /></el-icon>
        <span>分区列表</span>
      </div>
      <el-button type="primary" @click="openCreateDialog">
        <el-icon><Plus /></el-icon>新增分区
      </el-button>
    </div>

    <!-- 数据表格 -->
    <el-table
      :data="sections"
      border
      stripe
      style="width: 100%"
      v-loading="loading"
      class="data-table"
    >
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="名称" min-width="150" />
      <el-table-column prop="description" label="描述" min-width="200" />
      <el-table-column prop="sortOrder" label="排序" width="80" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 0 ? 'success' : 'info'" effect="light">
            {{ row.status === 0 ? '正常' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" link @click="openEditDialog(row)">
            <el-icon><Edit /></el-icon>编辑
          </el-button>
          <el-button
            size="small"
            type="warning"
            link
            v-if="row.status === 0"
            @click="disable(row)"
          >
            <el-icon><Close /></el-icon>禁用
          </el-button>
          <el-button
            size="small"
            type="success"
            link
            v-else
            @click="enable(row)"
          >
            <el-icon><Check /></el-icon>启用
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑分区' : '新增分区'"
      width="500px"
      destroy-on-close
    >
      <el-form :model="form" label-width="90px">
        <el-form-item label="名称">
          <el-input v-model="form.name" placeholder="请输入分区名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" placeholder="请输入分区描述" />
        </el-form-item>
        <el-form-item label="图标 URL">
          <el-input v-model="form.iconUrl" placeholder="可为空" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" />
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
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  adminListAllSections,
  adminCreateSection,
  adminUpdateSection,
  adminDisableSection,
  adminEnableSection
} from '@/api/section'
import { Folder, Plus, Edit, Close, Check } from '@element-plus/icons-vue'

const sections = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const currentId = ref(null)

const form = ref({
  name: '',
  description: '',
  iconUrl: '',
  sortOrder: 0
})

const fetchSections = async () => {
  loading.value = true
  try {
    const res = await adminListAllSections()
    sections.value = res.data || []
  } finally {
    loading.value = false
  }
}

const openCreateDialog = () => {
  isEdit.value = false
  currentId.value = null
  form.value = {
    name: '',
    description: '',
    iconUrl: '',
    sortOrder: 0
  }
  dialogVisible.value = true
}

const openEditDialog = (row) => {
  isEdit.value = true
  currentId.value = row.id
  form.value = {
    name: row.name,
    description: row.description,
    iconUrl: row.iconUrl,
    sortOrder: row.sortOrder
  }
  dialogVisible.value = true
}

const submit = async () => {
  if (!form.value.name.trim()) {
    return ElMessage.warning('名称不能为空')
  }
  saving.value = true
  try {
    if (isEdit.value) {
      await adminUpdateSection(currentId.value, form.value)
      ElMessage.success('更新成功')
    } else {
      await adminCreateSection(form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await fetchSections()
  } finally {
    saving.value = false
  }
}

const disable = async (row) => {
  await adminDisableSection(row.id)
  ElMessage.success('已禁用')
  await fetchSections()
}

const enable = async (row) => {
  await adminEnableSection(row.id)
  ElMessage.success('已启用')
  await fetchSections()
}

onMounted(fetchSections)
</script>

<style scoped>
.admin-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
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
