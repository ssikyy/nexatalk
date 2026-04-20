<template>
  <div class="edit-post-page">
    <div class="editor-card" v-loading="pageLoading">
      <div class="editor-header">
        <div class="header-left">
          <el-button class="back-btn" text @click="handleCancel">
            <el-icon><ArrowLeft /></el-icon>
          </el-button>
          <div>
            <h2 class="page-title">编辑帖子</h2>
            <p class="page-subtitle">编辑页已精简为基础富文本，保留更稳定的正文能力。</p>
          </div>
        </div>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="选择分区" prop="sectionId">
          <el-select
            v-model="form.sectionId"
            placeholder="请选择一个分区"
            size="large"
            class="section-select"
          >
            <template #prefix>
              <el-icon><Folder /></el-icon>
            </template>
            <el-option
              v-for="section in sections"
              :key="section.id"
              :label="section.name"
              :value="section.id"
            >
              <div class="section-option">
                <span class="section-name">{{ section.name }}</span>
                <span class="section-desc">{{ section.description }}</span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="标题" prop="title">
          <div class="title-input-wrapper">
            <input
              v-model="form.title"
              class="title-input"
              placeholder="请输入有意义的标题..."
              :maxlength="TITLE_MAX_LENGTH"
            />
            <span class="title-count" :class="{ 'near-limit': form.title.length > TITLE_MAX_LENGTH - 20 }">
              {{ form.title.length }}/{{ TITLE_MAX_LENGTH }}
            </span>
          </div>
        </el-form-item>

        <el-form-item label="正文内容" prop="content">
          <div class="editor-wrapper">
            <div class="editor-note">
              编辑页已移除 Emoji、引用、代码块、无序列表、有序列表和添加链接功能。
            </div>
            <PostRichTextEditor
              ref="editorRef"
              v-model="form.content"
              placeholder="继续完善这篇帖子..."
              :allow-bullet-list="false"
              :allow-ordered-list="false"
              :allow-blockquote="false"
              :allow-link="false"
              :allow-code-block="false"
              :allow-emoji="false"
              @images-change="handleEditorImagesChange"
            />
          </div>
        </el-form-item>

        <div class="form-actions">
          <div class="actions-left">
            <span class="action-hint">支持标题、粗体、斜体、删除线和图片</span>
          </div>
          <div class="actions-right">
            <el-button @click="handleCancel" class="cancel-btn">取消</el-button>
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Document, Folder, Promotion } from '@element-plus/icons-vue'
import PostRichTextEditor from '@/components/post/PostRichTextEditor.vue'
import { listSections } from '@/api/section'
import { getPost, updatePost } from '@/api/post'
import { TITLE_MAX_LENGTH, extractImageUrls, getMeaningfulContent } from '@/utils/postContent'

const route = useRoute()
const router = useRouter()
const postId = Number(route.params.id)

const formRef = ref()
const editorRef = ref(null)
const submitting = ref(false)
const pageLoading = ref(false)
const sections = ref([])
const initialSnapshot = ref('')

const form = ref({
  sectionId: null,
  title: '',
  content: '',
  images: [],
  draft: false
})

const uploadedImages = ref([])

const rules = {
  sectionId: [{ required: true, message: '请选择分区', trigger: 'change' }],
  title: [
    { required: true, message: '请输入标题', trigger: 'blur' },
    { min: 5, max: TITLE_MAX_LENGTH, message: `标题长度为 5-${TITLE_MAX_LENGTH} 个字符`, trigger: 'blur' }
  ],
  content: [
    {
      validator: (_, value, callback) => {
        const { text, images } = getMeaningfulContent(value)
        if (!text && images.length === 0) {
          callback(new Error('请输入正文内容或至少插入一张图片'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
}

function buildSnapshot() {
  return JSON.stringify({
    sectionId: form.value.sectionId,
    title: form.value.title.trim(),
    content: form.value.content,
    images: extractImageUrls(form.value.content),
    draft: !!form.value.draft
  })
}

function syncEditorContent(content = '') {
  editorRef.value?.setContent(content)
  uploadedImages.value = extractImageUrls(content)
}

function handleEditorImagesChange(images) {
  uploadedImages.value = images
}

async function loadPageData() {
  pageLoading.value = true
  try {
    const [sectionRes, postRes] = await Promise.all([
      listSections(),
      getPost(postId)
    ])

    sections.value = sectionRes.data || []

    const post = postRes.data
    const content = typeof post.content === 'string' ? post.content : ''
    const images = Array.isArray(post.images) && post.images.length > 0
      ? post.images
      : extractImageUrls(content)

    form.value = {
      sectionId: post.sectionId,
      title: post.title || '',
      content,
      images,
      draft: post.status === 1
    }

    syncEditorContent(content)
    initialSnapshot.value = buildSnapshot()
  } finally {
    pageLoading.value = false
  }
}

async function handleSubmit(asDraft) {
  try {
    await formRef.value.validate()
  } catch {
    ElMessage.warning('请完善帖子信息')
    return
  }

  submitting.value = true

  try {
    form.value.draft = asDraft
    form.value.images = extractImageUrls(form.value.content)

    await updatePost(postId, {
      sectionId: form.value.sectionId,
      title: form.value.title,
      content: form.value.content,
      images: form.value.images,
      draft: form.value.draft
    })

    initialSnapshot.value = buildSnapshot()
    ElMessage.success(asDraft ? '草稿保存成功' : '帖子保存成功')

    if (asDraft) {
      return
    }

    router.push(`/posts/${postId}`)
  } catch (error) {
    ElMessage.error(error?.message || (asDraft ? '保存草稿失败' : '保存失败'))
  } finally {
    submitting.value = false
  }
}

function handleCancel() {
  if (buildSnapshot() === initialSnapshot.value) {
    router.back()
    return
  }

  ElMessageBox.confirm('内容尚未保存，确定要返回吗？', '提示', {
    confirmButtonText: '返回',
    cancelButtonText: '继续编辑',
    type: 'warning'
  }).then(() => {
    router.back()
  }).catch(() => {})
}

onMounted(async () => {
  await loadPageData()
})
</script>

<style scoped>
.edit-post-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
  min-height: 100vh;
  background: #f5f7fa;
}

.editor-card {
  background: #fff;
  border-radius: 18px;
  padding: 28px 32px;
  border: 1px solid #e4e7ed;
  box-shadow: 0 6px 24px rgba(15, 23, 42, 0.06);
}

.editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 28px;
  padding-bottom: 20px;
  border-bottom: 1px solid #eef2f7;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.back-btn {
  font-size: 20px;
  color: #64748b;
}

.back-btn:hover {
  color: #3b82f6;
}

.page-title {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: #1e293b;
}

.page-subtitle {
  margin: 6px 0 0;
  font-size: 14px;
  color: #94a3b8;
}

.section-select {
  width: 100%;
  max-width: 340px;
}

.section-option {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.section-name {
  font-weight: 600;
  color: #1e293b;
}

.section-desc {
  color: #94a3b8;
  font-size: 13px;
}

.title-input-wrapper {
  position: relative;
}

.title-input {
  width: 100%;
  padding: 16px 72px 16px 16px;
  border: 1px solid #dbe4ee;
  border-radius: 14px;
  font-size: 20px;
  font-weight: 600;
  color: #0f172a;
  outline: none;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.title-input:focus {
  border-color: #60a5fa;
  box-shadow: 0 0 0 4px rgba(96, 165, 250, 0.12);
}

.title-count {
  position: absolute;
  top: 50%;
  right: 16px;
  transform: translateY(-50%);
  color: #94a3b8;
  font-size: 13px;
}

.title-count.near-limit {
  color: #f59e0b;
}

.editor-wrapper {
  overflow: hidden;
  background: #fff;
}

.editor-note {
  padding: 14px 18px;
  background: linear-gradient(180deg, #fffbeb 0%, #fff7ed 100%);
  border-bottom: 1px solid #fde7c2;
  color: #9a6b16;
  font-size: 14px;
}

.form-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-top: 28px;
  padding-top: 24px;
  border-top: 1px solid #eef2f7;
}

.actions-left {
  color: #94a3b8;
  font-size: 14px;
}

.actions-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.cancel-btn,
.draft-btn,
.publish-btn {
  min-width: 116px;
  height: 44px;
  border-radius: 12px;
}

@media (max-width: 768px) {
  .edit-post-page {
    padding: 16px;
  }

  .editor-card {
    padding: 20px;
  }

  .editor-header,
  .form-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .actions-right {
    width: 100%;
    justify-content: space-between;
  }

  .cancel-btn,
  .draft-btn,
  .publish-btn {
    flex: 1;
    min-width: 0;
  }
}
</style>
