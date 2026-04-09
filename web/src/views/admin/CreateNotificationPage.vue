<template>
  <div class="create-notification-page">
    <div class="editor-card" v-loading="submitting">
      <!-- 头部 -->
      <div class="editor-header">
        <div class="header-left">
          <el-button class="back-btn" text @click="handleBack">
            <el-icon><ArrowLeft /></el-icon>
          </el-button>
          <h2 class="page-title">{{ isEdit ? '编辑通知' : '发布通知' }}</h2>
        </div>
        <div class="header-right">
          <el-tag v-if="isEdit" type="info" effect="plain">编辑模式</el-tag>
          <el-tag v-else type="success" effect="plain">新增</el-tag>
        </div>
      </div>

      <!-- 通知表单 -->
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="notification-form">
        <!-- 标题输入 -->
        <el-form-item label="通知标题" prop="title" class="title-form-item">
          <div class="title-input-wrapper">
            <input
              v-model="form.title"
              class="title-input"
              placeholder="请输入通知标题..."
              maxlength="100"
            />
            <span class="title-count" :class="{ 'near-limit': form.title.length > 80 }">
              {{ form.title.length }}/100
            </span>
          </div>
        </el-form-item>

        <!-- 通知内容（可选，纯图片时可留空） -->
        <el-form-item label="正文" prop="content" class="content-form-item">
          <div class="content-editor-wrapper">
            <el-input
              v-model="form.content"
              type="textarea"
              :rows="8"
              placeholder="选填。可只写文字、只发图片，或图文一起。"
              maxlength="5000"
              show-word-limit
              class="content-textarea"
            />
          </div>
        </el-form-item>

        <!-- 图片上传（可选） -->
        <el-form-item label="图片" class="image-form-item">
          <div class="image-upload-wrapper">
            <!-- 图片上传区域 -->
            <div v-if="!form.imageUrl" class="upload-area" @click="triggerUpload">
              <el-icon class="upload-icon"><Plus /></el-icon>
              <div class="upload-text">点击上传图片</div>
              <div class="upload-hint">支持 JPG、PNG 格式，最大 5MB</div>
              <input
                ref="fileInput"
                type="file"
                accept="image/jpeg,image/png,image/jpg"
                @change="handleFileChange"
                style="display: none"
              />
            </div>

            <!-- 图片预览 -->
            <div v-else class="image-preview-container">
              <div class="image-preview">
                <el-image
                  :src="form.imageUrl"
                  fit="cover"
                  :preview-src-list="[form.imageUrl]"
                  class="preview-image"
                >
                  <template #error>
                    <div class="image-error">
                      <el-icon><PictureFilled /></el-icon>
                      <span>图片加载失败</span>
                    </div>
                  </template>
                </el-image>
                <div class="image-overlay">
                  <el-button type="primary" size="small" circle class="overlay-btn" @click="triggerUpload">
                    <el-icon class="btn-icon"><Refresh /></el-icon>
                  </el-button>
                  <el-button type="danger" size="small" circle class="overlay-btn" @click="removeImage">
                    <el-icon class="btn-icon"><Delete /></el-icon>
                  </el-button>
                </div>
              </div>
              <div class="image-actions">
                <el-button type="primary" size="small" class="action-btn" @click="triggerUpload">
                  <el-icon class="btn-icon"><Refresh /></el-icon>
                  <span>更换图片</span>
                </el-button>
                <el-button type="danger" size="small" class="action-btn" @click="removeImage">
                  <el-icon class="btn-icon"><Delete /></el-icon>
                  <span>删除图片</span>
                </el-button>
              </div>
            </div>

            <!-- 上传 loading -->
            <div v-if="uploading" class="upload-loading">
              <el-icon class="is-loading"><Loading /></el-icon>
              <span>正在上传...</span>
            </div>
          </div>
        </el-form-item>

        <!-- 属性设置 -->
        <el-form-item label="属性设置" class="attributes-form-item">
          <div class="attributes-wrapper">
            <div class="attribute-item">
              <el-switch
                v-model="form.isPinned"
                :active-value="1"
                :inactive-value="0"
                size="large"
              />
              <div class="attribute-info">
                <span class="attribute-label">
                  <el-icon class="btn-icon"><Top /></el-icon>
                  置顶通知
                </span>
                <span class="attribute-desc">置顶的通知将显示在所有用户通知列表的最顶部</span>
              </div>
            </div>
            <div class="attribute-item">
              <el-switch
                v-model="form.isBold"
                :active-value="1"
                :inactive-value="0"
                size="large"
              />
              <div class="attribute-info">
                <span class="attribute-label">
                  <el-icon class="btn-icon"><EditPen /></el-icon>
                  粗体标题
                </span>
                <span class="attribute-desc">标题将以粗体显示，更加醒目</span>
              </div>
            </div>
          </div>
        </el-form-item>

        <!-- 预览区域 -->
        <el-form-item class="preview-form-item">
          <div class="previewSection">
            <div class="preview-header" @click="showPreview = !showPreview">
              <el-icon class="btn-icon"><View /></el-icon>
              <span>预览通知效果</span>
              <el-icon class="arrow-icon" :class="{ rotated: showPreview }">
                <ArrowDown />
              </el-icon>
            </div>
            <transition name="slide">
              <div v-if="showPreview" class="preview-content">
                <div class="preview-card">
                  <!-- 预览标题区域 -->
                  <div class="preview-title-wrapper">
                    <div class="preview-tags" v-if="form.isPinned || form.isBold">
                      <el-tag v-if="form.isPinned" type="danger" effect="dark" size="small">置顶</el-tag>
                      <el-tag v-if="form.isBold" type="warning" size="small">重要</el-tag>
                    </div>
                    <h1 class="preview-title" :class="{ bold: form.isBold }">
                      {{ form.title || '请输入通知标题' }}
                    </h1>
                  </div>

                  <!-- 预览元信息 -->
                  <div class="preview-meta">
                    <span class="meta-item">
                      <el-icon><Clock /></el-icon>
                      {{ isEdit ? '最后更新' : '即将发布' }}
                    </span>
                    <span class="meta-item system-tag">
                      <el-icon><Bell /></el-icon>
                      系统通知
                    </span>
                    <el-tag size="small" :type="effectiveContentTypeTag">
                      {{ effectiveContentTypeText }}
                    </el-tag>
                  </div>

                  <el-divider />

                  <!-- 预览内容 -->
                  <div class="preview-body">
                    <div v-if="form.imageUrl" class="preview-image">
                      <el-image
                        :src="form.imageUrl"
                        fit="cover"
                        :preview-src-list="[form.imageUrl]"
                      />
                    </div>
                    <div class="preview-text" :class="{ 'image-only': form.imageUrl && !hasTextContent }">
                      {{ hasTextContent ? form.content : (form.imageUrl ? '' : '请输入正文或上传图片') }}
                    </div>
                  </div>
                </div>
              </div>
            </transition>
          </div>
        </el-form-item>

        <!-- 底部操作栏 -->
        <div class="form-actions">
          <div class="actions-left">
            <el-button @click="handleBack" class="cancel-btn action-btn">
              <el-icon class="btn-icon"><ArrowLeft /></el-icon>
              <span>返回列表</span>
            </el-button>
          </div>
          <div class="actions-right">
            <el-button
              v-if="isEdit"
              type="danger"
              plain
              @click="handleDelete"
              :loading="deleting"
              class="delete-btn action-btn"
            >
              <el-icon class="btn-icon"><Delete /></el-icon>
              <span>删除通知</span>
            </el-button>
            <el-button type="primary" @click="handleSubmit" :loading="submitting" class="publish-btn action-btn">
              <el-icon class="btn-icon"><Promotion /></el-icon>
              <span>{{ isEdit ? '保存修改' : '发布通知' }}</span>
            </el-button>
          </div>
        </div>
      </el-form>
    </div>

    <!-- 发布成功提示 -->
    <el-dialog
      v-model="showSuccessDialog"
      title="发布成功"
      width="420px"
      :close-on-click-modal="false"
      class="success-dialog"
    >
      <div class="success-content">
        <div class="success-icon">
          <el-icon :size="64" color="#67c23a"><CircleCheckFilled /></el-icon>
        </div>
        <p class="success-text">通知已成功发布！</p>
        <p class="success-desc">系统已为所有用户创建通知</p>
      </div>
      <template #footer>
        <el-button type="primary" @click="handleSuccessConfirm">知道了</el-button>
        <el-button @click="handleSuccessContinue">继续发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft,
  ArrowDown,
  PictureFilled,
  View,
  Clock,
  Bell,
  Promotion,
  Delete,
  Top,
  EditPen,
  CircleCheckFilled,
  Plus,
  Refresh,
  Loading
} from '@element-plus/icons-vue'
import {
  createSystemNotification,
  updateSystemNotification,
  deleteSystemNotification,
  getSystemNotification
} from '@/api/notification'
import { uploadPostImage } from '@/api/post'

const route = useRoute()
const router = useRouter()

const notificationId = route.params.id
const isEdit = computed(() => !!notificationId)
const formRef = ref(null)
const fileInput = ref(null)
const submitting = ref(false)
const deleting = ref(false)
const uploading = ref(false)
const showPreview = ref(false)
const showSuccessDialog = ref(false)

const form = ref({
  title: '',
  content: '',
  imageUrl: '',
  isPinned: 0,
  isBold: 0
})

const rules = {
  title: [
    { required: true, message: '请输入通知标题', trigger: 'blur' },
    { min: 1, max: 100, message: '标题长度应为 1-100 个字符', trigger: 'blur' }
  ],
  content: [
    { max: 5000, message: '内容长度不能超过 5000 个字符', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        const text = (value || '').trim()
        if (!text && !(form.value.imageUrl || '').trim()) {
          callback(new Error('请填写正文或上传图片，至少填一项'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const MAX_FILE_SIZE = 5 * 1024 * 1024 // 5MB

const hasTextContent = computed(() => (form.value.content || '').trim().length > 0)

const effectiveContentTypeText = computed(() => {
  const hasImg = !!(form.value.imageUrl || '').trim()
  if (hasImg && hasTextContent.value) return '图文'
  if (hasImg) return '图片'
  return '纯文字'
})

const effectiveContentTypeTag = computed(() => {
  const hasImg = !!(form.value.imageUrl || '').trim()
  if (hasImg && hasTextContent.value) return 'warning'
  if (hasImg) return 'success'
  return 'info'
})

const triggerUpload = () => {
  fileInput.value?.click()
}

const handleFileChange = async (event) => {
  const file = event.target.files?.[0]
  if (!file) return

  // 验证文件类型
  if (!['image/jpeg', 'image/png', 'image/jpg'].includes(file.type)) {
    ElMessage.error('仅支持 JPG、PNG 格式的图片')
    return
  }

  // 验证文件大小
  if (file.size > MAX_FILE_SIZE) {
    ElMessage.error('图片大小不能超过 5MB')
    return
  }

  // 上传图片
  uploading.value = true
  try {
    const res = await uploadPostImage(file)
    form.value.imageUrl = res.data
    ElMessage.success('图片上传成功')
  } catch (e) {
    console.error('上传失败:', e)
    ElMessage.error('图片上传失败，请重试')
  } finally {
    uploading.value = false
    // 清空 input，允许重复选择同一文件
    event.target.value = ''
  }
}

const removeImage = () => {
  form.value.imageUrl = ''
  formRef.value?.validateField('content')
}

const handleBack = () => {
  if (form.value.title || form.value.content || form.value.imageUrl) {
    ElMessageBox.confirm('确定要返回吗？未保存的内容将会丢失。', '提示', {
      confirmButtonText: '确定返回',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      router.push('/admin?tab=notifications')
    }).catch(() => {})
  } else {
    router.push('/admin?tab=notifications')
  }
}

const handleSuccessConfirm = () => {
  showSuccessDialog.value = false
  router.push('/admin?tab=notifications')
}

const handleSuccessContinue = () => {
  showSuccessDialog.value = false
  form.value = {
    title: '',
    content: '',
    imageUrl: '',
    isPinned: 0,
    isBold: 0
  }
  showPreview.value = false
}

onMounted(async () => {
  if (isEdit.value) {
    await loadNotification()
  }
})

async function loadNotification() {
  try {
    const res = await getSystemNotification(notificationId)
    if (res.data) {
      form.value = {
        title: res.data.title || '',
        content: res.data.content || '',
        imageUrl: res.data.imageUrl || '',
        isPinned: res.data.isPinned || 0,
        isBold: res.data.isBold || 0
      }
    }
  } catch (e) {
    ElMessage.error('加载通知失败')
    router.push('/admin?tab=notifications')
  }
}

async function handleSubmit() {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    submitting.value = true
    try {
      const payload = {
        title: form.value.title,
        content: form.value.content || '',
        imageUrl: form.value.imageUrl || '',
        isPinned: form.value.isPinned,
        isBold: form.value.isBold
      }
      if (isEdit.value) {
        await updateSystemNotification(notificationId, payload)
        ElMessage.success('保存成功')
        router.push('/admin?tab=notifications')
      } else {
        await createSystemNotification(payload)
        showSuccessDialog.value = true
      }
    } catch (e) {
      console.error(e)
      ElMessage.error(isEdit.value ? '保存失败：' + (e.response?.data?.message || '服务器错误') : '发布失败：' + (e.response?.data?.message || '服务器错误'))
    } finally {
      submitting.value = false
    }
  })
}

async function handleDelete() {
  try {
    await ElMessageBox.confirm(
      '确定要删除这条通知吗？此操作不可恢复。',
      '删除确认',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )

    deleting.value = true
    await deleteSystemNotification(notificationId)
    ElMessage.success('删除成功')
    router.push('/admin?tab=notifications')
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  } finally {
    deleting.value = false
  }
}
</script>

<style scoped>
.create-notification-page {
  min-height: 100vh;
  background: #f0f2f5;
  padding: 24px;
}

.editor-card {
  max-width: 900px;
  margin: 0 auto;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 2px 16px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

/* 头部 */
.editor-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid #f0f0f0;
  background: #fff;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.back-btn {
  padding: 8px;
  font-size: 20px;
  color: #606266;
}

.back-btn:hover {
  color: #409eff;
}

.page-title {
  font-size: 22px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0;
}

/* 表单样式 */
.notification-form {
  padding: 24px 28px;
}

.title-form-item {
  margin-bottom: 24px;
}

.title-input-wrapper {
  position: relative;
}

.title-input {
  width: 100%;
  padding: 14px 18px;
  font-size: 18px;
  font-weight: 600;
  border: 2px solid #e8e8e8;
  border-radius: 10px;
  background: #fafafa;
  color: #1a1a2e;
  transition: all 0.2s;
}

.title-input:focus {
  outline: none;
  border-color: #409eff;
  background: #fff;
  box-shadow: 0 0 0 4px rgba(64, 158, 255, 0.1);
}

.title-input::placeholder {
  color: #c0c4cc;
}

.title-count {
  position: absolute;
  right: 14px;
  bottom: 14px;
  font-size: 12px;
  color: #909399;
}

.title-count.near-limit {
  color: #f56c6c;
}

/* 图标统一对齐 */
.btn-icon {
  width: 1em;
  height: 1em;
  margin-right: 6px;
  vertical-align: middle;
  font-size: inherit;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.action-btn .btn-icon {
  margin-right: 6px;
}

.overlay-btn .btn-icon {
  margin-right: 0;
}

.attribute-label .btn-icon {
  margin-right: 0;
}

/* 内容编辑 */
.content-form-item {
  margin-bottom: 24px;
}

.content-editor-wrapper {
  width: 100%;
}

.content-textarea :deep(.el-textarea__inner) {
  padding: 16px;
  font-size: 15px;
  line-height: 1.8;
  border-radius: 10px;
  background: #fafafa;
  border: 2px solid #e8e8e8;
  color: #1a1a2e;
  min-height: 240px;
}

.content-textarea :deep(.el-textarea__inner:focus) {
  border-color: #409eff;
  background: #fff;
  box-shadow: 0 0 0 4px rgba(64, 158, 255, 0.1);
}

/* 图片上传 */
.image-form-item {
  margin-bottom: 24px;
}

.image-upload-wrapper {
  position: relative;
}

.upload-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  border: 2px dashed #dcdfe6;
  border-radius: 12px;
  background: #fafafa;
  cursor: pointer;
  transition: all 0.2s;
}

.upload-area:hover {
  border-color: #409eff;
  background: #f0f7ff;
}

.upload-icon {
  font-size: 48px;
  color: #c0c4cc;
  margin-bottom: 12px;
}

.upload-text {
  font-size: 16px;
  font-weight: 500;
  color: #606266;
  margin-bottom: 8px;
}

.upload-hint {
  font-size: 13px;
  color: #909399;
}

.image-preview-container {
  display: flex;
  align-items: flex-start;
  gap: 20px;
}

.image-preview {
  position: relative;
  width: 280px;
  height: 160px;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #e8e8e8;
}

.preview-image {
  width: 100%;
  height: 100%;
}

.image-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  opacity: 0;
  transition: opacity 0.2s;
}

.image-preview:hover .image-overlay {
  opacity: 1;
}

.image-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background: #f5f7fa;
  color: #909399;
  gap: 8px;
}

.image-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.upload-loading {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.9);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  border-radius: 12px;
  color: #409eff;
  font-size: 14px;
}

/* 属性设置 */
.attributes-form-item {
  margin-bottom: 24px;
}

.attributes-wrapper {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.attribute-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 16px 20px;
  background: #f8f9fa;
  border-radius: 12px;
  border: 1px solid #f0f0f0;
}

.attribute-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.attribute-label {
  font-weight: 600;
  color: #1a1a2e;
  font-size: 15px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.attribute-desc {
  font-size: 13px;
  color: #909399;
}

/* 预览区域 */
.preview-form-item {
  margin-bottom: 0;
}

.previewSection {
  border: 2px dashed #e8e8e8;
  border-radius: 12px;
  overflow: hidden;
}

.preview-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 20px;
  background: #fafafa;
  cursor: pointer;
  color: #606266;
  font-weight: 500;
  transition: all 0.2s;
}

.preview-header:hover {
  background: #f0f2f5;
  color: #409eff;
}

.arrow-icon {
  margin-left: auto;
  transition: transform 0.3s;
}

.arrow-icon.rotated {
  transform: rotate(180deg);
}

.preview-content {
  background: #fff;
  border-top: 1px dashed #e8e8e8;
  padding: 24px;
}

.preview-card {
  max-width: 100%;
}

.preview-title-wrapper {
  margin-bottom: 16px;
}

.preview-tags {
  display: flex;
  gap: 8px;
  margin-bottom: 14px;
}

.preview-title {
  font-size: 24px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0;
  line-height: 1.4;
}

.preview-title.bold {
  font-weight: 700;
}

.preview-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  color: #909399;
  font-size: 14px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 5px;
}

.system-tag {
  color: #409eff;
}

.preview-body {
  padding: 20px 0;
}

.preview-image {
  margin-bottom: 20px;
  border-radius: 12px;
  overflow: hidden;
}

.preview-image :deep(.el-image) {
  width: 100%;
  max-height: 400px;
}

.preview-text {
  font-size: 16px;
  line-height: 1.8;
  color: #303133;
  white-space: pre-wrap;
  word-break: break-word;
}

.preview-text.image-only {
  font-size: 18px;
}

/* 底部操作栏 */
.form-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 24px;
  border-top: 1px solid #f0f0f0;
  margin-top: 24px;
}

.actions-right {
  display: flex;
  gap: 12px;
}

.cancel-btn {
  padding: 12px 20px;
}

.delete-btn {
  padding: 12px 20px;
}

.publish-btn {
  padding: 12px 28px;
  font-weight: 600;
  font-size: 15px;
}

/* 过渡动画 */
.slide-enter-active,
.slide-leave-active {
  transition: all 0.3s ease;
}

.slide-enter-from,
.slide-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

/* 成功弹窗 */
.success-content {
  text-align: center;
  padding: 20px 0;
}

.success-icon {
  margin-bottom: 20px;
}

.success-text {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0 0 8px 0;
}

.success-desc {
  font-size: 14px;
  color: #909399;
  margin: 0;
}

/* 响应式 */
@media (max-width: 768px) {
  .create-notification-page {
    padding: 12px;
  }

  .notification-form {
    padding: 16px;
  }

  .image-preview-container {
    flex-direction: column;
  }

  .image-preview {
    width: 100%;
    height: auto;
    aspect-ratio: 16/9;
  }

  .image-actions {
    flex-direction: row;
    width: 100%;
  }

  .image-actions .el-button {
    flex: 1;
  }

  .form-actions {
    flex-direction: column;
    gap: 12px;
  }

  .actions-right {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>
