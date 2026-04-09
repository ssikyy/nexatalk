<!---- 系统设置管理 ---->
<template>
  <div class="system-admin">
    <!-- 操作栏 -->
    <div class="action-bar">
      <div class="action-left">
        <el-input
          v-model="keyword"
          placeholder="搜索配置键或描述..."
          clearable
          class="search-input"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" @click="handleSearch">
          <el-icon><Search /></el-icon>
          搜索
        </el-button>
      </div>
      <div class="action-right">
        <el-button type="success" @click="handleBatchSave">
          <el-icon><Check /></el-icon>
          批量保存
        </el-button>
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增配置
        </el-button>
      </div>
    </div>

    <!-- 配置列表 -->
    <el-table
      :data="configList"
      v-loading="loading"
      stripe
      border
      class="config-table"
    >
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="configKey" label="配置键" min-width="200">
        <template #default="{ row }">
          <el-input
            v-if="row.editing"
            v-model="row.configKey"
            size="small"
            :disabled="row.isExisting"
          />
          <span v-else class="config-key">{{ row.configKey }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="configValue" label="配置值" min-width="250">
        <template #default="{ row }">
          <el-input
            v-if="row.editing"
            v-model="row.configValue"
            size="small"
            type="textarea"
            :rows="2"
          />
          <span v-else class="config-value">{{ row.configValue }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="configType" label="类型" width="100">
        <template #default="{ row }">
          <el-select
            v-if="row.editing"
            v-model="row.configType"
            size="small"
            style="width: 100%"
          >
            <el-option label="字符串" value="string" />
            <el-option label="数字" value="number" />
            <el-option label="布尔值" value="boolean" />
          </el-select>
          <el-tag v-else :type="getTypeTagType(row.configType)">
            {{ getTypeLabel(row.configType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="180">
        <template #default="{ row }">
          <el-input
            v-if="row.editing"
            v-model="row.description"
            size="small"
          />
          <span v-else class="config-desc">{{ row.description }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <div class="action-buttons">
            <template v-if="row.editing">
              <el-button type="success" size="small" @click="handleSave(row)">
                保存
              </el-button>
              <el-button size="small" @click="handleCancel(row)">
                取消
              </el-button>
            </template>
            <template v-else>
              <el-button type="primary" size="small" @click="handleEdit(row)">
                编辑
              </el-button>
              <el-button
                type="danger"
                size="small"
                :disabled="row.isSystem"
                @click="handleDelete(row)"
              >
                删除
              </el-button>
            </template>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑配置' : '新增配置'"
      width="500px"
      destroy-on-close
    >
      <el-form :model="form" label-width="100px">
        <el-form-item label="配置键" required>
          <el-input
            v-model="form.configKey"
            placeholder="例如: site_name"
            :disabled="isEdit"
          />
        </el-form-item>
        <el-form-item label="配置值" required>
          <el-input
            v-model="form.configValue"
            type="textarea"
            :rows="3"
            placeholder="配置值"
          />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.configType" style="width: 100%">
            <el-option label="字符串" value="string" />
            <el-option label="数字" value="number" />
            <el-option label="布尔值" value="boolean" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="2"
            placeholder="配置描述"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 批量保存对话框 -->
    <el-dialog
      v-model="batchDialogVisible"
      title="批量保存配置"
      width="600px"
      destroy-on-close
    >
      <div class="batch-tip">
        <el-alert type="info" :closable="false">
          以下配置将被批量更新，请确认后再保存
        </el-alert>
      </div>
      <el-table :data="batchConfigs" max-height="300" border>
        <el-table-column prop="configKey" label="配置键" />
        <el-table-column prop="configValue" label="配置值">
          <template #default="{ row }">
            <el-input v-model="row.configValue" size="small" />
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" />
      </el-table>
      <template #footer>
        <el-button @click="batchDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="batchSubmitting" @click="submitBatch">
          确认保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Check } from '@element-plus/icons-vue'
import {
  getSystemConfigList,
  getAllSystemConfigs,
  createSystemConfig,
  updateSystemConfig,
  deleteSystemConfig,
  batchUpdateSystemConfigs
} from '@/api/systemConfig'

const loading = ref(false)
const submitting = ref(false)
const batchSubmitting = ref(false)

const configList = ref([])
const keyword = ref('')
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)

const dialogVisible = ref(false)
const batchDialogVisible = ref(false)
const isEdit = ref(false)

const form = ref({
  id: null,
  configKey: '',
  configValue: '',
  configType: 'string',
  description: ''
})

const batchConfigs = ref([])

// 获取配置列表
const loadConfigs = async () => {
  loading.value = true
  try {
    const res = await getSystemConfigList({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: keyword.value
    })
    if (res.data) {
      configList.value = (res.data.records || []).map(item => ({
        ...item,
        editing: false,
        isExisting: true
      }))
      total.value = res.data.total || 0
    }
  } catch (error) {
    console.error('加载配置失败:', error)
  } finally {
    loading.value = false
  }
}

// 加载所有配置用于批量编辑
const loadAllConfigs = async () => {
  try {
    const res = await getAllSystemConfigs()
    if (res.data) {
      batchConfigs.value = res.data.map(item => ({ ...item }))
      batchDialogVisible.value = true
    }
  } catch (error) {
    console.error('加载所有配置失败:', error)
  }
}

// 搜索
const handleSearch = () => {
  pageNum.value = 1
  loadConfigs()
}

// 新增
const handleAdd = () => {
  form.value = {
    id: null,
    configKey: '',
    configValue: '',
    configType: 'string',
    description: ''
  }
  isEdit.value = false
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row) => {
  row.editing = true
  row._backup = { ...row }
}

// 保存单个
const handleSave = async (row) => {
  if (!row.configKey || !row.configValue) {
    ElMessage.warning('配置键和值不能为空')
    return
  }

  submitting.value = true
  try {
    if (row.isExisting) {
      await updateSystemConfig(row.id, {
        configKey: row.configKey,
        configValue: row.configValue,
        configType: row.configType,
        description: row.description
      })
      ElMessage.success('配置更新成功')
    } else {
      await createSystemConfig({
        configKey: row.configKey,
        configValue: row.configValue,
        configType: row.configType,
        description: row.description
      })
      ElMessage.success('配置创建成功')
    }
    row.editing = false
    delete row._backup
    loadConfigs()
  } catch (error) {
    ElMessage.error('操作失败: ' + (error.message || '未知错误'))
  } finally {
    submitting.value = false
  }
}

// 取消编辑
const handleCancel = (row) => {
  if (row._backup) {
    Object.assign(row, row._backup)
    delete row._backup
  }
  row.editing = false
}

// 删除
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该配置吗？', '提示', { type: 'warning' })
    await deleteSystemConfig(row.id)
    ElMessage.success('删除成功')
    loadConfigs()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败: ' + (error.message || '未知错误'))
    }
  }
}

// 提交新增/编辑表单
const handleSubmit = async () => {
  if (!form.value.configKey || !form.value.configValue) {
    ElMessage.warning('配置键和值不能为空')
    return
  }

  submitting.value = true
  try {
    if (isEdit.value) {
      await updateSystemConfig(form.value.id, form.value)
      ElMessage.success('配置更新成功')
    } else {
      await createSystemConfig(form.value)
      ElMessage.success('配置创建成功')
    }
    dialogVisible.value = false
    loadConfigs()
  } catch (error) {
    ElMessage.error('操作失败: ' + (error.message || '未知错误'))
  } finally {
    submitting.value = false
  }
}

// 批量保存
const handleBatchSave = async () => {
  await loadAllConfigs()
}

// 提交批量更新
const submitBatch = async () => {
  batchSubmitting.value = true
  try {
    const configs = batchConfigs.value.map(item => ({
      configKey: item.configKey,
      configValue: item.configValue,
      description: item.description
    }))
    await batchUpdateSystemConfigs(configs)
    ElMessage.success('批量保存成功')
    batchDialogVisible.value = false
    loadConfigs()
  } catch (error) {
    ElMessage.error('批量保存失败: ' + (error.message || '未知错误'))
  } finally {
    batchSubmitting.value = false
  }
}

// 分页变化
const handleSizeChange = () => {
  pageNum.value = 1
  loadConfigs()
}

const handleCurrentChange = () => {
  loadConfigs()
}

// 类型标签
const getTypeTagType = (type) => {
  const map = {
    string: '',
    number: 'success',
    boolean: 'warning'
  }
  return map[type] || ''
}

const getTypeLabel = (type) => {
  const map = {
    string: '字符串',
    number: '数字',
    boolean: '布尔值'
  }
  return map[type] || type
}

onMounted(() => {
  loadConfigs()
})
</script>

<style scoped>
.system-admin {
  padding: 0;
}

.action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 12px;
}

.action-left {
  display: flex;
  gap: 12px;
  align-items: center;
}

.action-right {
  display: flex;
  gap: 12px;
}

.search-input {
  width: 280px;
}

.config-table {
  width: 100%;
}

.config-key {
  font-family: monospace;
  color: #409eff;
  font-weight: 500;
}

.config-value {
  font-family: monospace;
  color: #67c23a;
  word-break: break-all;
}

.config-desc {
  color: #909399;
  font-size: 13px;
}

.action-buttons {
  display: flex;
  gap: 8px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.batch-tip {
  margin-bottom: 16px;
}
</style>
