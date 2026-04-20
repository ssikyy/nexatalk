<template>
  <div class="create-post-page">
    <div class="editor-card" v-loading="pageLoading">
      <!-- 头部 -->
      <div class="editor-header">
        <div class="header-left">
          <el-button class="back-btn" text @click="$router.back()">
            <el-icon><ArrowLeft /></el-icon>
          </el-button>
          <h2 class="page-title">发布新帖</h2>
        </div>
        <div class="header-right">
          <el-button @click="showDraftList = true" class="draft-btn" text>
            <el-icon><Document /></el-icon>
            草稿箱
            <el-badge v-if="drafts.length > 0" :value="drafts.length" class="draft-badge" />
          </el-button>
        </div>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <!-- 分区选择 -->
        <el-form-item label="选择分区" prop="sectionId" class="section-form-item">
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
              v-for="s in sections"
              :key="s.id"
              :label="s.name"
              :value="s.id"
            >
              <div class="section-option">
                <span class="section-name">{{ s.name }}</span>
                <span class="section-desc">{{ s.description }}</span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>

        <!-- 标题输入 -->
        <el-form-item label="标题" prop="title" class="title-form-item">
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

        <!-- 正文编辑器 -->
        <el-form-item label="正文内容" prop="content" class="content-form-item">
            <div class="editor-wrapper">
              <!-- AI 润色工具栏 -->
              <div class="ai-toolbar">
                <div class="ai-toolbar-copy">
                  <span class="ai-toolbar-title">AI 写作助手</span>
                </div>
                <div class="ai-toolbar-actions">
                  <el-dropdown trigger="click" @command="(cmd) => handlePolish(cmd)" :disabled="!aiEnabled || polishing">
                    <el-button
                      size="large"
                      type="primary"
                      :loading="polishing"
                      :disabled="!aiEnabled"
                      class="ai-polish-btn"
                    >
                      <el-icon><MagicStick /></el-icon>
                      {{ polishing ? '润色中...' : 'AI 润色' }}
                    </el-button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item
                          v-for="style in polishStyles"
                          :key="style.value"
                          :command="style.value"
                          :disabled="polishing"
                        >
                          <div class="style-option">
                            <span class="style-label">{{ style.label }}</span>
                            <span class="style-desc">{{ style.desc }}</span>
                          </div>
                        </el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                  <el-dropdown trigger="click" @command="(cmd) => handleExpand(cmd)" :disabled="!aiEnabled || expanding">
                    <el-button
                      size="large"
                      type="success"
                      :loading="expanding"
                      :disabled="!aiEnabled"
                      class="ai-expand-btn"
                    >
                      <el-icon><Grid /></el-icon>
                      {{ expanding ? '扩写中...' : 'AI 扩写' }}
                    </el-button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item
                          v-for="style in expandStyles"
                          :key="style.value"
                          :command="style.value"
                          :disabled="expanding"
                        >
                          <div class="style-option">
                            <span class="style-label">{{ style.label }}</span>
                            <span class="style-desc">{{ style.desc }}</span>
                          </div>
                        </el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>
              </div>

              <PostRichTextEditor
                ref="editorRef"
                v-model="form.content"
                placeholder="分享你的想法..."
                :min-height="340"
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

        <!-- 底部操作栏 -->
        <div class="form-actions">
          <div class="actions-left">
            <span class="action-hint">按 <kbd>Ctrl</kbd> + <kbd>S</kbd> 保存草稿</span>
          </div>
          <div class="actions-right">
            <el-button @click="handleCancel" class="cancel-btn">取消</el-button>
            <el-button @click="handleSubmit(true)" :loading="submitting" class="draft-btn">
              <el-icon><Document /></el-icon>
              保存草稿
            </el-button>
            <el-button type="primary" @click="handleSubmit(false)" :loading="submitting" class="publish-btn">
              <el-icon><Promotion /></el-icon>
              发布
            </el-button>
          </div>
        </div>
      </el-form>
    </div>

    <!-- 草稿箱弹窗 -->
    <el-dialog v-model="showDraftList" title="我的草稿" width="680px" destroy-on-close class="draft-dialog">
      <div v-if="drafts.length === 0" class="empty-drafts">
        <el-empty description="暂无草稿" :image-size="120">
          <el-button type="primary" @click="startFresh">开始撰写新帖子</el-button>
        </el-empty>
      </div>
      <div v-else class="draft-list">
        <div v-for="draft in drafts" :key="draft.id" class="draft-item" @click="loadDraft(draft)">
          <div class="draft-content">
            <div class="draft-title">{{ draft.title || '无标题' }}</div>
            <div class="draft-summary">{{ draft.summary || '暂无内容' }}</div>
            <div class="draft-meta">
              <el-tag size="small">{{ draft.sectionName }}</el-tag>
              <span class="draft-time">{{ formatTime(draft.createdAt) }}</span>
            </div>
          </div>
          <div class="draft-actions">
            <el-button size="small" type="danger" link @click.stop="deleteDraft(draft)">删除</el-button>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- AI 润色对比弹窗 -->
    <el-dialog
      v-model="showPolishDialog"
      title="润色结果对比"
      width="1040px"
      destroy-on-close
      class="polish-dialog"
      :close-on-click-modal="false"
    >
      <div class="polish-comparison">
        <div class="comparison-panel comparison-panel-before">
          <div class="comparison-header">
            <div class="comparison-header-main">
              <span class="comparison-badge">原文</span>
              <span class="comparison-title">当前正文</span>
            </div>
            <span class="comparison-meta">{{ originalPreviewContent.length }} 字</span>
          </div>
          <div class="comparison-content">{{ originalPreviewContent || '暂无内容' }}</div>
        </div>
        <div class="comparison-divider">
          <el-icon><Right /></el-icon>
        </div>
        <div class="comparison-panel comparison-panel-after">
          <div class="comparison-header">
            <div class="comparison-header-main">
              <span class="comparison-badge comparison-badge-polish">润色后</span>
              <span class="comparison-title">优化建议</span>
            </div>
            <span class="comparison-meta">{{ polishedPreviewContent.length }} 字</span>
          </div>
          <div class="comparison-content">{{ polishedPreviewContent || '暂无内容' }}</div>
        </div>
      </div>
      <template #footer>
        <div class="comparison-footer">
          <div class="comparison-footer-tip">确认后会直接替换正文内容。</div>
          <div class="comparison-footer-actions">
            <el-button @click="cancelPolish" class="comparison-cancel-btn">保留原文</el-button>
            <el-button type="primary" @click="confirmPolish" class="comparison-confirm-btn">使用润色</el-button>
          </div>
        </div>
      </template>
    </el-dialog>

    <!-- AI 扩写结果对比弹窗 -->
    <el-dialog
      v-model="showExpandDialog"
      title="扩写结果对比"
      width="1040px"
      destroy-on-close
      class="polish-dialog"
      :close-on-click-modal="false"
    >
      <div class="polish-comparison">
        <div class="comparison-panel comparison-panel-before">
          <div class="comparison-header">
            <div class="comparison-header-main">
              <span class="comparison-badge">原文</span>
              <span class="comparison-title">当前正文</span>
            </div>
            <span class="comparison-meta">{{ originalPreviewContent.length }} 字</span>
          </div>
          <div class="comparison-content">{{ originalPreviewContent || '暂无内容' }}</div>
        </div>
        <div class="comparison-divider">
          <el-icon><Right /></el-icon>
        </div>
        <div class="comparison-panel comparison-panel-after">
          <div class="comparison-header">
            <div class="comparison-header-main">
              <span class="comparison-badge comparison-badge-expand">扩写后</span>
              <span class="comparison-title">扩展建议</span>
            </div>
            <span class="comparison-meta">{{ expandedPreviewContent.length }} 字</span>
          </div>
          <div class="comparison-content">{{ expandedPreviewContent || '暂无内容' }}</div>
        </div>
      </div>
      <template #footer>
        <div class="comparison-footer">
          <div class="comparison-footer-tip">确认后会直接替换正文内容。</div>
          <div class="comparison-footer-actions">
            <el-button @click="cancelExpand" class="comparison-cancel-btn">保留原文</el-button>
            <el-button type="primary" @click="confirmExpand" class="comparison-confirm-btn comparison-confirm-btn-expand">使用扩写</el-button>
          </div>
        </div>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Document, Folder, Promotion, Grid, MagicStick, Right } from '@element-plus/icons-vue'
import PostRichTextEditor from '@/components/post/PostRichTextEditor.vue'
import { listSections } from '@/api/section'
import { createPost, updatePost, listMyPosts, getPost, deletePost } from '@/api/post'
import { polish as polishApi, expand as expandApi, getAiStatus } from '@/api/ai'
import { TITLE_MAX_LENGTH, extractImageUrls, formatPreviewText, getMeaningfulContent, stripHtmlParagraphWrapper } from '@/utils/postContent'

const router = useRouter()
const route = useRoute()
const formRef = ref()
const editorRef = ref(null)
const submitting = ref(false)
const pageLoading = ref(false)
const sections = ref([])
const showDraftList = ref(false)
const drafts = ref([])
const loadedDraftId = ref(route.query.draftId ? parseInt(route.query.draftId) : null)

// AI 润色相关
const aiEnabled = ref(false)
const polishing = ref(false)
const polishedContent = ref('')
const originalContent = ref('')
const showPolishDialog = ref(false)

const expanding = ref(false)
const expandedContent = ref('')
const showExpandDialog = ref(false)

const polishStyles = [
  { value: 'standard', label: '标准润色', desc: '修正语法、优化措辞' },
  { value: 'formal', label: '正式商务', desc: '语气正式、用词规范' },
  { value: 'concise', label: '简洁精炼', desc: '精简内容、去除冗余' },
  { value: 'friendly', label: '活泼亲切', desc: '语气友好、亲切自然' },
  { value: 'professional', label: '专业深度', desc: '专业术语、详细阐述' }
]

const expandStyles = [
  { value: 'detailed', label: '详细展开', desc: '在保持原意基础上补充细节' },
  { value: 'vivid', label: '生动描写', desc: '增加画面感与细节' },
  { value: 'logical', label: '逻辑递进', desc: '按逻辑补充论据或步骤' },
  { value: 'narrative', label: '叙述扩充', desc: '以叙述方式扩充情节' },
  { value: 'professional', label: '专业延伸', desc: '从专业角度延伸解释' }
]

const originalPreviewContent = computed(() => formatPreviewText(originalContent.value))
const polishedPreviewContent = computed(() => formatPreviewText(polishedContent.value))
const expandedPreviewContent = computed(() => formatPreviewText(expandedContent.value))

function syncEditorContent(content = '') {
  editorRef.value?.setContent(content)
  uploadedImages.value = extractImageUrls(content)
}

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
  sectionId: [
    { required: true, message: '请选择分区', trigger: 'change' }
  ],
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

function handleEditorImagesChange(images) {
  uploadedImages.value = images
}

async function handlePolish(style) {
  if (!form.value.content) {
    ElMessage.warning('请先输入正文内容')
    return
  }

  if (!aiEnabled.value) {
    ElMessage.warning('AI 功能未开启，请联系管理员')
    return
  }

  polishing.value = true

  try {
    const res = await polishApi({ content: form.value.content, style })
    originalContent.value = form.value.content
    polishedContent.value = stripHtmlParagraphWrapper(res.data) || res.data
    showPolishDialog.value = true
  } catch (error) {
    console.error('Polish error:', error)
    ElMessage.error(error?.message || '润色失败，请稍后重试')
  } finally {
    polishing.value = false
  }
}

// 确认使用润色后的内容，同步到表单并立即写入编辑器
function confirmPolish() {
  const content = stripHtmlParagraphWrapper(polishedContent.value) || polishedContent.value
  form.value.content = content
  editorRef.value?.setContent(content)
  showPolishDialog.value = false
  ElMessage.success('已替换为润色后的内容')
}

function cancelPolish() {
  showPolishDialog.value = false
}

async function handleExpand(style) {
  if (!form.value.content?.trim()) {
    ElMessage.warning('请先输入正文内容')
    return
  }
  if (!aiEnabled.value) {
    ElMessage.warning('AI 功能未开启，请联系管理员')
    return
  }
  expanding.value = true
  try {
    const res = await expandApi({ content: form.value.content, style })
    originalContent.value = form.value.content
    expandedContent.value = stripHtmlParagraphWrapper(res.data) || res.data
    showExpandDialog.value = true
  } catch (error) {
    console.error('Expand error:', error)
    ElMessage.error(error?.message || '扩写失败，请稍后重试')
  } finally {
    expanding.value = false
  }
}

function confirmExpand() {
  const content = stripHtmlParagraphWrapper(expandedContent.value) || expandedContent.value
  form.value.content = content
  editorRef.value?.setContent(content)
  showExpandDialog.value = false
  ElMessage.success('已替换为扩写后的内容')
}

function cancelExpand() {
  showExpandDialog.value = false
}

function formatTime(timeStr) {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const now = new Date()
  const diff = now - date

  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)} 分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)} 小时前`
  if (diff < 604800000) return `${Math.floor(diff / 86400000)} 天前`

  return date.toLocaleDateString()
}

function startFresh() {
  loadedDraftId.value = null
  form.value = {
    sectionId: null,
    title: '',
    content: '',
    images: [],
    draft: false
  }
  uploadedImages.value = []
  syncEditorContent('')
  showDraftList.value = false
}

async function loadDraft(draft) {
  try {
    const res = await getPost(draft.id)
    const post = res.data
    const extractedImages = extractImageUrls(post.content || '')
    const draftImages = Array.isArray(post.images) && post.images.length > 0 ? post.images : extractedImages
    loadedDraftId.value = post.id
    form.value = {
      sectionId: post.sectionId,
      title: post.title,
      content: post.content,
      images: draftImages,
      draft: true
    }
    uploadedImages.value = draftImages
    syncEditorContent(post.content || '')
    showDraftList.value = false
    ElMessage.success('已加载草稿')
  } catch {
    ElMessage.error('加载草稿失败')
  }
}

async function deleteDraft(draft) {
  try {
    await ElMessageBox.confirm('确定要删除这个草稿吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deletePost(draft.id)
    drafts.value = drafts.value.filter(d => d.id !== draft.id)
    if (loadedDraftId.value === draft.id) {
      startFresh()
    }
    ElMessage.success('草稿已删除')
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error('删除失败')
    }
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

    const submitData = {
      sectionId: form.value.sectionId,
      title: form.value.title,
      content: form.value.content,
      images: form.value.images,
      draft: form.value.draft
    }

    if (loadedDraftId.value) {
      await updatePost(loadedDraftId.value, submitData)
    } else {
      const res = await createPost(submitData)
      loadedDraftId.value = res.data
    }

    ElMessage.success(asDraft ? '草稿保存成功' : '发布成功')

    if (!asDraft) {
      router.push('/')
    }
  } catch (error) {
    console.error('Submit error:', error)
    ElMessage.error(error?.message || (asDraft ? '保存草稿失败' : '发布失败'))
  } finally {
    submitting.value = false
  }
}

function handleCancel() {
  const { text, images } = getMeaningfulContent(form.value.content)
  if (form.value.title || text || images.length > 0) {
    ElMessageBox.confirm('内容尚未保存，确定要返回吗？', '提示', {
      confirmButtonText: '返回',
      cancelButtonText: '继续编辑',
      type: 'warning'
    }).then(() => {
      router.back()
    }).catch(() => {})
  } else {
    router.back()
  }
}

onMounted(async () => {
  pageLoading.value = true
  try {
    // 加载分区
    const sRes = await listSections()
    sections.value = sRes.data || []

    if (sections.value.length === 0) {
      ElMessage.warning('暂无可用分区，请联系管理员')
    }

    // 检查 AI 功能是否开启
    try {
      const aiRes = await getAiStatus()
      aiEnabled.value = aiRes.data === true
    } catch {
      aiEnabled.value = false
    }

    // 加载草稿列表
    try {
      const myRes = await listMyPosts({ page: 1, pageSize: 50 })
      const records = myRes.data?.list || []
      drafts.value = records.filter((p) => p.status === 1)

      // 如果有指定加载的草稿
      if (loadedDraftId.value) {
        const targetDraft = drafts.value.find(d => d.id === loadedDraftId.value)
        if (targetDraft) {
          await loadDraft(targetDraft)
        }
      } else if (drafts.value.length > 0) {
        // 自动加载最近草稿
        const latestDraft = drafts.value[0]
        await loadDraft(latestDraft)
      }
    } catch {
      // 忽略错误
    }
  } finally {
    pageLoading.value = false
  }
})
</script>

<style scoped>
.create-post-page {
  width: 100%;
  max-width: none;
  margin: 0;
  padding: 0;
  min-height: 100vh;
  background: #f5f7fa;
}

.editor-card {
  width: 100%;
  background: #fff;
  border: none;
  border-radius: 0;
  padding: 28px 32px 40px;
  box-shadow: none;
}

.editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 28px;
  padding-bottom: 20px;
  border-bottom: 1px solid #edf1f6;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 14px;
  font-size: 22px;
  color: #64748b;
}

.back-btn:hover {
  color: #2563eb;
  background: #eff6ff;
}

.page-title {
  margin: 0;
  font-size: 26px;
  font-weight: 800;
  color: #1f2937;
  letter-spacing: -0.02em;
}

.header-right {
  display: flex;
  align-items: center;
}

.header-right .draft-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #4b5563;
  font-weight: 600;
}

.draft-badge {
  margin-left: 4px;
}

:deep(.section-form-item),
:deep(.title-form-item),
:deep(.content-form-item) {
  margin-bottom: 26px;
}

:deep(.section-form-item .el-form-item__label),
:deep(.title-form-item .el-form-item__label),
:deep(.content-form-item .el-form-item__label) {
  padding-bottom: 12px;
  font-size: 17px;
  font-weight: 700;
  color: #1f2937;
}

:deep(.section-form-item .el-form-item__content),
:deep(.content-form-item .el-form-item__content) {
  width: 100%;
}

.section-select {
  width: 100%;
  max-width: 760px;
}

.section-select :deep(.el-input__wrapper),
.section-select :deep(.el-select__wrapper) {
  min-height: 54px;
  padding: 0 16px;
  border-radius: 14px;
  box-shadow: 0 1px 0 rgba(255, 255, 255, 0.8), 0 0 0 1px #d9e2ee inset;
}

.section-select :deep(.el-input__wrapper.is-focus),
.section-select :deep(.el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.16), 0 0 0 1px #6aa5ff inset;
}

.section-option {
  display: flex;
  flex-direction: column;
  padding: 4px 0;
}

.section-name {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.section-desc {
  margin-top: 2px;
  font-size: 12px;
  color: #94a3b8;
}

.title-input-wrapper {
  position: relative;
  width: 100%;
  max-width: 760px;
}

.title-input {
  width: 100%;
  padding: 16px 76px 16px 18px;
  font-size: 18px;
  font-weight: 500;
  color: #1f2937;
  border: 2px solid #dcdfe6;
  border-radius: 12px;
  outline: none;
  transition: all 0.25s ease;
}

.title-input:focus {
  border-color: #409eff;
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.1);
}

.title-input::placeholder {
  color: #c4ccd7;
}

.title-count {
  position: absolute;
  right: 18px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 14px;
  color: #9aa4b2;
}

.title-count.near-limit {
  color: #f59e0b;
}

.editor-wrapper {
  width: 100%;
  overflow: hidden;
  border: 1px solid #d6e2f0;
  border-radius: 22px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.05), inset 0 1px 0 rgba(255, 255, 255, 0.85);
}

.editor-wrapper :deep(.rich-editor) {
  border: none;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
}

.editor-wrapper :deep(.tiptap-toolbar) {
  gap: 14px;
  padding: 14px 18px;
  border-bottom: 1px solid #dbe7f4;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.95) 0%, rgba(247, 251, 255, 0.98) 100%);
}

.editor-wrapper :deep(.toolbar-group) {
  gap: 10px;
}

.editor-wrapper :deep(.toolbar-divider) {
  width: 1px;
  height: 24px;
  margin: 0 2px;
  background: #d9e3f0;
}

.editor-wrapper :deep(.toolbar-group button) {
  min-width: 46px;
  height: 46px;
  padding: 0 12px;
  border-radius: 14px;
  border: 1px solid #d5e0ee;
  background: #fff;
  color: #334155;
  font-size: 15px;
  font-weight: 700;
  transition: all 0.22s ease;
}

.editor-wrapper :deep(.toolbar-group button:hover:not(:disabled)) {
  border-color: #7ea7ff;
  color: #2563eb;
  box-shadow: 0 10px 22px rgba(37, 99, 235, 0.14);
  transform: translateY(-1px);
}

.editor-wrapper :deep(.toolbar-group button.is-active) {
  color: #fff;
  border-color: transparent;
  background: linear-gradient(180deg, #5aa7ff 0%, #2f7df6 100%);
  box-shadow: 0 10px 18px rgba(47, 125, 246, 0.22);
}

.editor-wrapper :deep(.tiptap-content .ProseMirror) {
  min-height: 340px;
  padding: 26px 28px 34px;
  background: rgba(255, 255, 255, 0.96);
  font-size: 17px;
  line-height: 1.92;
  color: #1e293b;
}

.editor-wrapper :deep(.tiptap-content .ProseMirror p.is-editor-empty:first-child::before) {
  color: #a6b0bf;
}

.editor-wrapper :deep(.tiptap-content .ProseMirror img) {
  max-width: min(100%, 440px);
  max-height: 320px;
  margin: 16px 0;
  border-radius: 14px;
  box-shadow: 0 10px 20px rgba(15, 23, 42, 0.11);
}

.ai-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 18px;
  border-bottom: 1px solid #dde7f2;
  background: linear-gradient(180deg, rgba(249, 252, 255, 0.96) 0%, rgba(243, 248, 255, 0.98) 100%);
}

.ai-toolbar-copy {
  display: flex;
  align-items: center;
  min-width: 0;
}

.ai-toolbar-title {
  font-size: 16px;
  font-weight: 800;
  color: #0f172a;
  letter-spacing: -0.02em;
}

.ai-toolbar-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-left: auto;
  flex-shrink: 0;
}

.ai-polish-btn,
.ai-expand-btn {
  min-width: 112px;
  height: 40px;
  padding: 0 16px;
  border: none;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 700;
}

.ai-polish-btn {
  box-shadow: 0 10px 20px rgba(59, 130, 246, 0.18);
}

.ai-expand-btn {
  box-shadow: 0 10px 20px rgba(101, 163, 13, 0.18);
}

.style-option {
  display: flex;
  flex-direction: column;
  padding: 2px 0;
}

.style-label {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.style-desc {
  margin-top: 2px;
  font-size: 12px;
  color: #94a3b8;
}

.form-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-top: 28px;
  padding-top: 22px;
  border-top: 1px solid #edf1f6;
}

.actions-left {
  display: flex;
  align-items: center;
}

.action-hint {
  font-size: 14px;
  color: #94a3b8;
}

.action-hint kbd {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 28px;
  height: 28px;
  padding: 0 8px;
  margin: 0 2px;
  font-size: 13px;
  font-family: inherit;
  background: #f8fafc;
  border: 1px solid #d8e1ec;
  border-radius: 8px;
  box-shadow: 0 4px 10px rgba(148, 163, 184, 0.12);
}

.actions-right {
  display: flex;
  gap: 12px;
}

.actions-right .cancel-btn,
.actions-right .draft-btn,
.actions-right .publish-btn {
  min-width: 110px;
  height: 44px;
  border-radius: 14px;
  font-weight: 700;
}

/* 草稿箱样式 */
.draft-dialog :deep(.el-dialog) {
  border-radius: 24px;
  overflow: hidden;
}

.draft-dialog :deep(.el-dialog__body) {
  padding: 0;
}

.empty-drafts {
  padding: 40px 20px;
}

.draft-list {
  max-height: 500px;
  overflow-y: auto;
}

.draft-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 22px;
  border-bottom: 1px solid #f1f5f9;
  cursor: pointer;
  transition: background 0.2s ease;
}

.draft-item:hover {
  background: #f8fbff;
}

.draft-item:last-child {
  border-bottom: none;
}

.draft-content {
  flex: 1;
  min-width: 0;
}

.draft-title {
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
}

.draft-summary {
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  color: #94a3b8;
}

.draft-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.draft-time {
  font-size: 12px;
  color: #c0cad7;
}

.draft-actions {
  flex-shrink: 0;
  margin-left: 12px;
}

/* 润色/扩写对比弹窗 */
:deep(.polish-dialog .el-dialog) {
  border: 1px solid #dfe8f3;
  border-radius: 28px;
  overflow: hidden;
  box-shadow: 0 28px 80px rgba(15, 23, 42, 0.22);
}

:deep(.polish-dialog .el-dialog__header) {
  margin: 0;
  padding: 24px 28px 18px;
  border-bottom: 1px solid #e4edf7;
  background: linear-gradient(180deg, #ffffff 0%, #f7fbff 100%);
}

:deep(.polish-dialog .el-dialog__title) {
  font-size: 28px;
  font-weight: 800;
  color: #0f172a;
  letter-spacing: -0.02em;
}

:deep(.polish-dialog .el-dialog__headerbtn) {
  top: 24px;
  right: 24px;
}

:deep(.polish-dialog .el-dialog__body) {
  padding: 24px 28px 20px;
  background: linear-gradient(180deg, #fbfdff 0%, #f4f8fc 100%);
}

:deep(.polish-dialog .el-dialog__footer) {
  padding: 0 28px 24px;
  background: linear-gradient(180deg, #f4f8fc 0%, #f8fbff 100%);
}

.polish-comparison {
  display: flex;
  align-items: stretch;
  gap: 18px;
  min-height: 480px;
}

.comparison-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  border: 1px solid #dbe4ef;
  border-radius: 22px;
  overflow: hidden;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.08);
}

.comparison-panel-before {
  background: linear-gradient(180deg, #ffffff 0%, #fafcff 100%);
}

.comparison-panel-after {
  background: linear-gradient(180deg, #ffffff 0%, #f5fbff 100%);
}

.comparison-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 18px 20px;
  border-bottom: 1px solid #dde6f1;
  background: linear-gradient(180deg, #fcfeff 0%, #f4f8fd 100%);
}

.comparison-header-main {
  display: flex;
  align-items: center;
  gap: 10px;
}

.comparison-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 60px;
  height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  background: #eff4fb;
  color: #475569;
  font-size: 13px;
  font-weight: 700;
}

.comparison-badge-polish {
  background: rgba(59, 130, 246, 0.14);
  color: #1d4ed8;
}

.comparison-badge-expand {
  background: rgba(101, 163, 13, 0.16);
  color: #3f6212;
}

.comparison-title {
  font-size: 17px;
  font-weight: 700;
  color: #0f172a;
}

.comparison-meta {
  font-size: 13px;
  font-weight: 600;
  color: #94a3b8;
}

.comparison-content {
  flex: 1;
  min-height: 0;
  padding: 24px 22px 26px;
  overflow-y: auto;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 16px;
  line-height: 1.95;
  color: #334155;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96) 0%, rgba(249, 252, 255, 0.98) 100%);
}

.comparison-divider {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  flex-shrink: 0;
  color: #5b7ab3;
}

.comparison-divider :deep(.el-icon) {
  width: 56px;
  height: 56px;
  border: 1px solid #d6e4f6;
  border-radius: 50%;
  background: #f3f8ff;
  box-shadow: 0 12px 24px rgba(59, 130, 246, 0.12);
  font-size: 20px;
}

.comparison-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  width: 100%;
}

.comparison-footer-tip {
  font-size: 14px;
  color: #64748b;
}

.comparison-footer-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.comparison-cancel-btn,
.comparison-confirm-btn {
  min-width: 124px;
  height: 46px;
  border-radius: 14px;
  font-weight: 700;
}

.comparison-confirm-btn {
  box-shadow: 0 14px 26px rgba(59, 130, 246, 0.18);
}

.comparison-confirm-btn-expand {
  --el-button-bg-color: #67c23a;
  --el-button-border-color: #67c23a;
  --el-button-hover-bg-color: #7bce52;
  --el-button-hover-border-color: #7bce52;
  --el-button-active-bg-color: #59b12f;
  --el-button-active-border-color: #59b12f;
  box-shadow: 0 14px 26px rgba(101, 163, 13, 0.18);
}

@media (max-width: 1200px) {
  .create-post-page {
    padding: 24px;
  }

  .editor-card {
    padding: 30px 28px;
  }

  .polish-comparison {
    flex-direction: column;
    min-height: auto;
  }

  .comparison-divider {
    width: 100%;
    height: 52px;
  }

  .comparison-divider :deep(.el-icon) {
    transform: rotate(90deg);
  }

  .comparison-footer {
    flex-direction: column;
    align-items: stretch;
  }

  .comparison-footer-actions {
    justify-content: flex-end;
  }
}

@media (max-width: 960px) {
  .ai-toolbar {
    flex-wrap: wrap;
  }

  .ai-toolbar-actions {
    width: 100%;
    margin-left: 0;
    justify-content: flex-start;
  }
}

@media (max-width: 768px) {
  .create-post-page {
    padding: 16px;
  }

  .editor-card {
    padding: 20px;
    border-radius: 20px;
  }

  .editor-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
    margin-bottom: 24px;
  }

  .page-title {
    font-size: 24px;
  }

  .section-select,
  .title-input-wrapper {
    max-width: none;
  }

  .title-input {
    min-height: 88px;
    padding: 0 96px 0 22px;
    font-size: 17px;
    border-radius: 18px;
  }

  .title-count {
    right: 20px;
    font-size: 16px;
  }

  .ai-toolbar {
    padding: 18px 18px 16px;
  }

  .ai-toolbar-title {
    font-size: 18px;
  }

  .ai-polish-btn,
  .ai-expand-btn {
    min-width: 122px;
    height: 44px;
    font-size: 16px;
  }

  .editor-wrapper :deep(.tiptap-toolbar) {
    padding: 16px 18px;
  }

  .editor-wrapper :deep(.toolbar-group button) {
    min-width: 50px;
    height: 50px;
    border-radius: 16px;
  }

  .editor-wrapper :deep(.tiptap-content .ProseMirror) {
    min-height: 300px;
    padding: 24px 22px 30px;
    font-size: 17px;
    line-height: 1.9;
  }

  .form-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .actions-right {
    width: 100%;
    justify-content: flex-end;
    flex-wrap: wrap;
  }

  :deep(.polish-dialog .el-dialog) {
    width: calc(100vw - 24px) !important;
  }

  :deep(.polish-dialog .el-dialog__header) {
    padding: 20px 20px 16px;
  }

  :deep(.polish-dialog .el-dialog__title) {
    font-size: 22px;
  }

  :deep(.polish-dialog .el-dialog__body) {
    padding: 18px 20px 16px;
  }

  :deep(.polish-dialog .el-dialog__footer) {
    padding: 0 20px 20px;
  }
}
</style>
