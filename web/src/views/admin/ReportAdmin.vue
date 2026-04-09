<template>
  <div class="admin-page">
    <!-- 操作栏 -->
    <div class="action-bar">
      <div class="action-title">
        <el-icon><Warning /></el-icon>
        <span>举报列表</span>
        <el-tag v-if="pendingCount > 0" type="danger" size="small" style="margin-left: 12px">
          {{ pendingCount }} 条待审核
        </el-tag>
      </div>
      <el-radio-group v-model="tab" size="default" class="tab-group">
        <el-radio-button label="pending">
          <el-badge :value="pendingCount" :hidden="pendingCount === 0" type="warning">待审核</el-badge>
        </el-radio-button>
        <el-radio-button label="all">全部</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 数据表格 -->
    <div class="table-container">
      <el-table
        :data="reports"
        border
        stripe
        style="width: 100%"
        v-loading="loading"
        class="data-table"
      >
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="entityType" label="类型" width="80">
          <template #default="{ row }">
            <el-tag :type="row.entityType === 1 ? 'primary' : 'info'" effect="light" size="small">
              {{ row.entityType === 1 ? '帖子' : '评论' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="entityTitle" label="举报内容" min-width="200">
          <template #default="{ row }">
            <div class="entity-content">
              <span class="content-text">{{ row.entityTitle || '-' }}</span>
              <span class="author-text">作者: {{ row.entityAuthor || '未知' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="reasonText" label="原因" width="100">
          <template #default="{ row }">
            <el-tag type="danger" effect="light" size="small">
              {{ row.reasonText }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reporterNickname" label="举报人" width="100">
          <template #default="{ row }">
            {{ row.reporterNickname || row.reporterUsername || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="detail" label="详情" min-width="120">
          <template #default="{ row }">
            <el-tooltip :content="row.detail" placement="top" :show-after="500" v-if="row.detail">
              <span class="detail-text">{{ row.detail.length > 15 ? row.detail.substring(0, 15) + '...' : row.detail }}</span>
            </el-tooltip>
            <span v-else class="detail-empty">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="statusText" label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)" effect="light" size="small">
              {{ row.statusText }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="举报时间" width="160">
          <template #default="{ row }">
            <span class="time-text">{{ formatDate(row.createdAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="viewDetail(row)">
              <el-icon><View /></el-icon>查看
            </el-button>
            <el-button size="small" type="success" link v-if="row.status === 0" @click="openReviewDialog(row, 2)">
              <el-icon><CircleCheck /></el-icon>通过
            </el-button>
            <el-button size="small" type="danger" link v-if="row.status === 0" @click="openReviewDialog(row, 1)">
              <el-icon><CircleClose /></el-icon>违规
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchReports"
          @current-change="fetchReports"
        />
      </div>
    </div>

    <!-- 查看详情弹窗 -->
    <el-dialog
      v-model="detailVisible"
      title="举报详情"
      width="650px"
      destroy-on-close
      class="detail-dialog"
    >
      <el-descriptions :column="2" border v-if="currentReport">
        <el-descriptions-item label="举报ID">{{ currentReport.id }}</el-descriptions-item>
        <el-descriptions-item label="举报人">{{ currentReport.reporterNickname || currentReport.reporterUsername || '-' }}</el-descriptions-item>
        <el-descriptions-item label="对象类型">
          <el-tag :type="currentReport.entityType === 1 ? 'primary' : 'info'" size="small">
            {{ currentReport.entityType === 1 ? '帖子' : '评论' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="对象ID">{{ currentReport.entityId }}</el-descriptions-item>
        <el-descriptions-item label="被举报内容" :span="2">
          <div class="detail-content">{{ currentReport.entityTitle || '(无内容)' }}</div>
          <div class="detail-author">作者: {{ currentReport.entityAuthor || '未知' }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="举报原因">
          <el-tag type="danger" size="small">{{ currentReport.reasonText }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="举报时间">{{ formatDate(currentReport.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="详细说明" :span="2">
          {{ currentReport.detail || '无' }}
        </el-descriptions-item>
        <el-descriptions-item label="当前状态">
          <el-tag :type="getStatusTagType(currentReport.status)" size="small">
            {{ currentReport.statusText }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="审核时间" v-if="currentReport.reviewedAt">
          {{ formatDate(currentReport.reviewedAt) }}
        </el-descriptions-item>
        <el-descriptions-item label="审核备注" :span="2" v-if="currentReport.reviewNote">
          {{ currentReport.reviewNote }}
        </el-descriptions-item>
      </el-descriptions>
      <template #footer v-if="currentReport && currentReport.status === 0">
        <div class="dialog-footer">
          <el-button @click="detailVisible = false">取消</el-button>
          <el-button type="success" @click="openReviewDialog(currentReport, 2)">
            <el-icon><CircleCheck /></el-icon>标记为无问题
          </el-button>
          <el-button type="danger" @click="openReviewDialog(currentReport, 1)">
            <el-icon><CircleClose /></el-icon>标记为违规
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 审核弹窗 -->
    <el-dialog
      v-model="reviewVisible"
      :title="reviewForm.status === 1 ? '标记为违规' : '标记为无问题'"
      width="500px"
      destroy-on-close
      class="review-dialog"
    >
      <div class="review-info" v-if="currentReport">
        <div class="info-row">
          <span class="label">举报ID:</span>
          <span>{{ currentReport.id }}</span>
        </div>
        <div class="info-row">
          <span class="label">对象类型:</span>
          <el-tag :type="currentReport.entityType === 1 ? 'primary' : 'info'" size="small">
            {{ currentReport.entityType === 1 ? '帖子' : '评论' }}
          </el-tag>
        </div>
        <div class="info-row">
          <span class="label">举报原因:</span>
          <el-tag type="danger" size="small">{{ currentReport.reasonText }}</el-tag>
        </div>
      </div>
      <el-form :model="reviewForm" label-width="80px">
        <el-form-item label="审核结果">
          <el-radio-group v-model="reviewForm.status" class="review-radio-group">
            <div class="radio-item">
              <el-radio :value="1">
                <span class="radio-label danger">违规</span>
              </el-radio>
              <span class="radio-desc">将对被举报内容进行处理</span>
            </div>
            <div class="radio-item">
              <el-radio :value="2">
                <span class="radio-label success">无问题</span>
              </el-radio>
              <span class="radio-desc">举报无效，不处理内容</span>
            </div>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核备注">
          <el-input
            v-model="reviewForm.reviewNote"
            type="textarea"
            :rows="3"
            placeholder="可选，记录你的判断依据"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitReview">提交审核</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  adminListReports,
  adminListPendingReports,
  adminReviewReport
} from '@/api/report'
import { Warning, View, CircleCheck, CircleClose } from '@element-plus/icons-vue'

const tab = ref('pending')
const reports = ref([])
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const pendingCount = ref(0)
const loading = ref(false)

const detailVisible = ref(false)
const reviewVisible = ref(false)
const saving = ref(false)
const currentReport = ref(null)
const reviewForm = ref({
  status: 1,
  reviewNote: ''
})

const getStatusTagType = (status) => {
  if (status === 0) return 'warning'
  if (status === 1) return 'danger'
  if (status === 2) return 'success'
  return 'info'
}

const fetchReports = async () => {
  loading.value = true
  try {
    let res
    const params = { page: page.value, pageSize: pageSize.value }
    if (tab.value === 'pending') {
      res = await adminListPendingReports(params)
    } else {
      res = await adminListReports(params)
    }
    total.value = res.data?.total ?? 0
    reports.value = res.data?.list ?? []
    if (tab.value === 'pending') {
      pendingCount.value = res.data?.total ?? 0
    }
  } finally {
    loading.value = false
  }
}

const viewDetail = (row) => {
  currentReport.value = row
  detailVisible.value = true
}

const openReviewDialog = (row, status) => {
  currentReport.value = row
  reviewForm.value = {
    status: status,
    reviewNote: ''
  }
  detailVisible.value = false
  reviewVisible.value = true
}

const submitReview = async () => {
  if (!currentReport.value) return
  saving.value = true
  try {
    await adminReviewReport(currentReport.value.id, reviewForm.value)
    ElMessage.success('审核成功')
    reviewVisible.value = false
    fetchReports()
  } finally {
    saving.value = false
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

watch(tab, () => {
  page.value = 1
  fetchReports()
})

onMounted(fetchReports)
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
  flex-wrap: wrap;
  gap: 16px;
}

.action-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 500;
  color: #1a1a2e;
}

.action-title .el-icon {
  color: #409eff;
}

.tab-group :deep(.el-radio-button__inner) {
  padding: 8px 16px;
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

.entity-content {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.content-text {
  color: #303133;
  font-size: 13px;
}

.author-text {
  color: #909399;
  font-size: 12px;
}

.detail-text {
  cursor: pointer;
  color: #606266;
}

.detail-empty {
  color: #c0c4cc;
}

.time-text {
  font-size: 13px;
  color: #909399;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding: 16px 0 0;
}

/* 详情弹窗 */
.detail-content {
  background: #f5f7fa;
  padding: 10px;
  border-radius: 6px;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 100px;
  overflow-y: auto;
}

.detail-author {
  margin-top: 8px;
  color: #909399;
  font-size: 12px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

/* 审核弹窗 */
.review-info {
  background: #f8f9fa;
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.info-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.info-row .label {
  color: #909399;
  min-width: 70px;
}

.review-radio-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.radio-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.radio-label.danger {
  color: #f56c6c;
  font-weight: 500;
}

.radio-label.success {
  color: #67c23a;
  font-weight: 500;
}

.radio-desc {
  margin-left: 8px;
  color: #909399;
  font-size: 12px;
}

:deep(.detail-dialog .el-dialog__header) {
  border-bottom: 1px solid #f0f0f0;
}

:deep(.review-dialog .el-dialog__header) {
  border-bottom: 1px solid #f0f0f0;
}
</style>
