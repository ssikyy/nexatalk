<template>
  <div class="create-post-page">
    <div class="editor-card" v-loading="pageLoading">
      <div class="editor-header">
        <h2 class="page-title">编辑帖子</h2>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="0">
        <!-- 分区选择 -->
        <div class="section-selector">
          <el-select v-model="form.sectionId" placeholder="选择分区" size="large">
            <template #prefix>
              <el-icon><Folder /></el-icon>
            </template>
            <el-option v-for="s in sections" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </div>

        <!-- 标题输入 -->
        <div class="title-input-wrapper">
          <input
            v-model="form.title"
            class="title-input"
            placeholder="请输入帖子标题..."
            maxlength="255"
          />
          <span class="title-count">{{ form.title.length }}/255</span>
        </div>

        <!-- 富文本编辑器 -->
        <div class="editor-wrapper">
          <MdEditor
            v-model="form.content"
            :editorId="editorId"
            :preview="false"
            :toolbars="toolbars"
            :style="{ height: editorHeight }"
            placeholder="分享你的想法... 支持 Markdown 语法"
            :onUploadImg="handleUploadImage"
          />
        </div>

        <!-- 工具栏 -->
        <div class="editor-toolbar">
          <div class="toolbar-left">
            <el-tooltip content="插入图片" placement="top">
              <div class="toolbar-btn" @click="triggerImageUpload">
                <el-icon><Picture /></el-icon>
                <span>图片</span>
              </div>
            </el-tooltip>
            <el-tooltip content="插入emoji" placement="top">
              <div class="toolbar-btn" @click="showEmojiPicker = !showEmojiPicker">
                <el-icon><Star /></el-icon>
                <span>Emoji</span>
              </div>
            </el-tooltip>
          </div>
          <div class="toolbar-right">
            <span class="content-hint">正文至少 10 字</span>
          </div>
        </div>

        <!-- Emoji选择器 -->
        <transition name="fade">
          <div v-if="showEmojiPicker" class="emoji-picker-container">
            <EmojiPicker
              :native="true"
              @select="handleEmojiSelect"
            />
          </div>
        </transition>

        <!-- 底部操作栏 -->
        <div class="form-actions">
          <div class="actions-left">
            <el-button @click="$router.back()">取消</el-button>
          </div>
          <div class="actions-right">
            <el-button @click="handleSubmit(true)" :loading="submitting" class="draft-btn">
              <el-icon><Document /></el-icon>
              存为草稿
            </el-button>
            <el-button type="primary" @click="handleSubmit(false)" :loading="submitting" class="publish-btn">
              <el-icon><Promotion /></el-icon>
              保存
            </el-button>
          </div>
        </div>
      </el-form>
    </div>

    <!-- 隐藏的文件输入 -->
    <input
      ref="imageInput"
      type="file"
      accept="image/*"
      multiple
      style="display: none"
      @change="handleImageSelect"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { MdEditor } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import EmojiPicker from 'emoji-picker-react'
import { Document, Folder, Picture, Star, Promotion } from '@element-plus/icons-vue'
import { listSections } from '@/api/section'
import { getPost, updatePost } from '@/api/post'
import { uploadPostImage } from '@/api/post'

const route = useRoute()
const router = useRouter()
const postId = Number(route.params.id)

const formRef = ref()
const submitting = ref(false)
const pageLoading = ref(false)
const sections = ref([])
const showEmojiPicker = ref(false)
const loadedDraftId = ref(null)

// 编辑器配置
const editorId = 'post-editor-edit'
const editorHeight = '400px'
const toolbars = [
  'bold', 'italic', 'strikeThrough', '-',
  'title', 'quote', 'unorderedList', 'orderedList', '-',
  'code', 'codeRow', 'link', 'image', '-',
  'revoke', 'next', '=',
  'pageFullscreen', 'fullscreen', 'preview', 'catalog'
]

const form = ref({
  sectionId: null,
  title: '',
  content: '',
  images: [],
  draft: false
})

// 上传的图片列表
const uploadedImages = ref([])

const rules = {
  sectionId: [{ required: true, message: '请选择分区', trigger: 'change' }],
  title: [
    { required: true, message: '请输入标题', trigger: 'blur' },
    { min: 5, max: 255, message: '标题长度为 5-255 字', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请输入正文', trigger: 'blur' },
    { min: 10, message: '正文至少 10 字', trigger: 'blur' }
  ]
}

// 图片上传
const imageInput = ref(null)

function triggerImageUpload() {
  imageInput.value?.click()
}

async function handleImageSelect(event) {
  const files = Array.from(event.target.files)
  if (files.length === 0) return

  const remainingSlots = 9 - uploadedImages.value.length
  const filesToUpload = files.slice(0, remainingSlots)

  for (const file of filesToUpload) {
    // 验证文件大小 (10MB)
    if (file.size > 10 * 1024 * 1024) {
      ElMessage.warning(`${file.name} 超过10MB限制`)
      continue
    }

    // 验证文件类型
    if (!file.type.startsWith('image/')) {
      ElMessage.warning(`${file.name} 不是图片文件`)
      continue
    }

    try {
      const res = await uploadPostImage(file)
      const url = res.data
      uploadedImages.value.push(url)
      // 同时将 Markdown 图片语法插入到正文中
      const markdownImage = `![image](${url})\n`
      form.value.content += markdownImage
    } catch (error) {
      ElMessage.error(`上传 ${file.name} 失败`)
    }
  }

  // 清空input以允许重新选择相同文件
  event.target.value = ''
}

function handleEmojiSelect(emoji) {
  form.value.content += emoji.emoji
  showEmojiPicker.value = false
}

async function handleUploadImage(files, callback) {
  const urls = []

  for (const file of files) {
    try {
      const res = await uploadPostImage(file)
      const url = res.data
      urls.push(url)
      if (!uploadedImages.value.includes(url)) {
        uploadedImages.value.push(url)
      }
    } catch (error) {
      ElMessage.error('图片上传失败')
    }
  }

  if (urls.length > 0) {
    callback(urls)
  }
}

onMounted(async () => {
  pageLoading.value = true

  try {
    // 加载分区
    const sRes = await listSections()
    sections.value = sRes.data || []

    // 加载帖子详情
    const pRes = await getPost(postId)
    const post = pRes.data

    form.value = {
      sectionId: post.sectionId,
      title: post.title,
      content: post.content,
      images: post.images || [],
      draft: post.status === 1
    }
    uploadedImages.value = post.images || []
  } finally {
    pageLoading.value = false
  }

  // 点击外部关闭emoji选择器
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})

function handleClickOutside(e) {
  if (showEmojiPicker.value && !e.target.closest('.toolbar-btn') && !e.target.closest('.emoji-picker-container')) {
    showEmojiPicker.value = false
  }
}

async function handleSubmit(asDraft) {
  await formRef.value.validate()
  submitting.value = true

  try {
    form.value.draft = asDraft
    form.value.images = uploadedImages.value

    const submitData = {
      sectionId: form.value.sectionId,
      title: form.value.title,
      content: form.value.content,
      images: form.value.images,
      draft: form.value.draft
    }

    await updatePost(postId, submitData)
    ElMessage.success(asDraft ? '草稿保存成功' : '保存成功')
    router.push(`/posts/${postId}`)
  } catch (error) {
    ElMessage.error(asDraft ? '保存草稿失败' : '保存失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.create-post-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
}

.editor-card {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
}

.editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  font-size: 22px;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
}

.section-selector {
  margin-bottom: 16px;
}

.section-selector :deep(.el-select) {
  width: 200px;
}

.title-input-wrapper {
  position: relative;
  margin-bottom: 16px;
}

.title-input {
  width: 100%;
  padding: 16px 60px 16px 16px;
  font-size: 20px;
  font-weight: 600;
  border: 2px solid #e2e8f0;
  border-radius: 12px;
  outline: none;
  transition: border-color 0.2s;
}

.title-input:focus {
  border-color: #3b82f6;
}

.title-count {
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 13px;
  color: #94a3b8;
}

.editor-wrapper {
  border: 2px solid #e2e8f0;
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 12px;
}

.editor-wrapper :deep(.md-editor) {
  border: none;
}

.editor-wrapper :deep(.md-editor-toolbar) {
  border-bottom: 1px solid #e2e8f0;
}

.editor-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #e2e8f0;
  margin-bottom: 12px;
}

.toolbar-left {
  display: flex;
  gap: 8px;
}

.toolbar-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border-radius: 8px;
  cursor: pointer;
  color: #64748b;
  font-size: 14px;
  transition: all 0.2s;
}

.toolbar-btn:hover {
  background: #f1f5f9;
  color: #3b82f6;
}

.toolbar-right {
  display: flex;
  align-items: center;
}

.content-hint {
  font-size: 13px;
  color: #94a3b8;
}

.emoji-picker-container {
  position: absolute;
  z-index: 100;
  margin-top: 8px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
  border-radius: 12px;
  overflow: hidden;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.uploaded-images {
  margin: 16px 0;
  padding: 16px;
  background: #f8fafc;
  border-radius: 12px;
}

.images-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.images-title {
  font-size: 14px;
  font-weight: 500;
  color: #475569;
}

.image-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.image-item {
  position: relative;
  width: 100px;
  height: 100px;
  border-radius: 10px;
  overflow: hidden;
}

.image-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s;
}

.image-item:hover .image-overlay {
  opacity: 1;
}

.add-more {
  width: 100px;
  height: 100px;
  border: 2px dashed #cbd5e1;
  border-radius: 10px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #94a3b8;
  font-size: 12px;
  transition: all 0.2s;
}

.add-more:hover {
  border-color: #3b82f6;
  color: #3b82f6;
}

.form-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #e2e8f0;
}

.actions-left {
  display: flex;
  align-items: center;
}

.actions-right {
  display: flex;
  gap: 12px;
}

.draft-btn :deep(.el-icon) {
  margin-right: 4px;
}

.publish-btn :deep(.el-icon) {
  margin-right: 4px;
}

@media (max-width: 768px) {
  .create-post-page {
    padding: 12px;
  }

  .editor-card {
    padding: 16px;
  }

  .title-input {
    font-size: 18px;
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
