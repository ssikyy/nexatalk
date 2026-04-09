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
              maxlength="255"
            />
            <span class="title-count" :class="{ 'near-limit': form.title.length > 200 }">
              {{ form.title.length }}/255
            </span>
          </div>
        </el-form-item>

        <!-- 正文编辑器 -->
        <el-form-item label="正文内容" prop="content" class="content-form-item">
            <div class="editor-wrapper">
              <!-- AI 润色工具栏 -->
              <div class="ai-toolbar">
                <el-dropdown trigger="click" @command="(cmd) => handlePolish(cmd)" :disabled="!aiEnabled || polishing">
                  <el-button
                    size="small"
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
                    size="small"
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
                <span v-if="!aiEnabled" class="ai-hint">AI 功能未开启</span>
              </div>

              <!-- Tiptap 富文本编辑器 -->
              <div class="tiptap-editor">
                <div class="tiptap-toolbar">
                  <el-tooltip content="粗体" placement="top" :show-after="300">
                    <div class="toolbar-btn-wrapper">
                      <button type="button" @click="editor?.chain().focus().toggleBold().run()" :class="{ 'is-active': editor?.isActive('bold') }"><strong>B</strong></button>
                    </div>
                  </el-tooltip>
                  <el-tooltip content="斜体" placement="top" :show-after="300">
                    <div class="toolbar-btn-wrapper">
                      <button type="button" @click="editor?.chain().focus().toggleItalic().run()" :class="{ 'is-active': editor?.isActive('italic') }"><em>I</em></button>
                    </div>
                  </el-tooltip>
                  <el-tooltip content="删除线" placement="top" :show-after="300">
                    <div class="toolbar-btn-wrapper">
                      <button type="button" @click="editor?.chain().focus().toggleStrike().run()" :class="{ 'is-active': editor?.isActive('strike') }"><s>S</s></button>
                    </div>
                  </el-tooltip>
                  <span class="toolbar-divider">|</span>
                  <el-tooltip content="标题1" placement="top" :show-after="300">
                    <div class="toolbar-btn-wrapper">
                      <button type="button" @click="editor?.chain().focus().toggleHeading({ level: 1 }).run()" :class="{ 'is-active': editor?.isActive('heading', { level: 1 }) }">H1</button>
                    </div>
                  </el-tooltip>
                  <el-tooltip content="标题2" placement="top" :show-after="300">
                    <div class="toolbar-btn-wrapper">
                      <button type="button" @click="editor?.chain().focus().toggleHeading({ level: 2 }).run()" :class="{ 'is-active': editor?.isActive('heading', { level: 2 }) }">H2</button>
                    </div>
                  </el-tooltip>
                  <el-tooltip content="标题3" placement="top" :show-after="300">
                    <div class="toolbar-btn-wrapper">
                      <button type="button" @click="editor?.chain().focus().toggleHeading({ level: 3 }).run()" :class="{ 'is-active': editor?.isActive('heading', { level: 3 }) }">H3</button>
                    </div>
                  </el-tooltip>
                  <span class="toolbar-divider">|</span>
                  <el-tooltip content="无序列表" placement="top" :show-after="300">
                    <div class="toolbar-btn-wrapper">
                      <button type="button" @click="editor?.chain().focus().toggleBulletList().run()" :class="{ 'is-active': editor?.isActive('bulletList') }">•</button>
                    </div>
                  </el-tooltip>
                  <el-tooltip content="有序列表" placement="top" :show-after="300">
                    <div class="toolbar-btn-wrapper">
                      <button type="button" @click="editor?.chain().focus().toggleOrderedList().run()" :class="{ 'is-active': editor?.isActive('orderedList') }">1.</button>
                    </div>
                  </el-tooltip>
                  <el-tooltip content="引用" placement="top" :show-after="300">
                    <div class="toolbar-btn-wrapper">
                      <button type="button" @click="editor?.chain().focus().toggleBlockquote().run()" :class="{ 'is-active': editor?.isActive('blockquote') }">"</button>
                    </div>
                  </el-tooltip>
                  <span class="toolbar-divider">|</span>
                  <el-tooltip content="添加链接" placement="top" :show-after="300">
                    <div class="toolbar-btn-wrapper">
                      <button type="button" @click="addLink">🔗</button>
                    </div>
                  </el-tooltip>
                  <el-tooltip :content="imageUploading ? '上传中...' : '添加图片'" placement="top" :show-after="300">
                    <div class="toolbar-btn-wrapper">
                      <button type="button" @click="addImage" :disabled="imageUploading">
                        <span v-if="imageUploading" class="loading-spinner"></span>
                        <span v-else>🖼️</span>
                      </button>
                    </div>
                  </el-tooltip>
                  <el-tooltip content="代码块" placement="top" :show-after="300">
                    <div class="toolbar-btn-wrapper">
                      <button type="button" @click="editor?.chain().focus().toggleCodeBlock().run()" :class="{ 'is-active': editor?.isActive('codeBlock') }">&lt;&gt;</button>
                    </div>
                  </el-tooltip>
                </div>
                <editor-content :editor="editor" class="tiptap-content" />
              </div>
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
      width="800px"
      destroy-on-close
      class="polish-dialog"
    >
      <div class="polish-comparison">
        <div class="comparison-panel">
          <div class="comparison-header">
            <span class="comparison-title">原文</span>
          </div>
          <div class="comparison-content">{{ originalContent }}</div>
        </div>
        <div class="comparison-divider">
          <el-icon><Right /></el-icon>
        </div>
        <div class="comparison-panel">
          <div class="comparison-header">
            <span class="comparison-title">润色后</span>
          </div>
          <div class="comparison-content">{{ polishedContent }}</div>
        </div>
      </div>
      <template #footer>
        <el-button @click="cancelPolish">保留原文</el-button>
        <el-button type="primary" @click="confirmPolish">使用润色</el-button>
      </template>
    </el-dialog>

    <!-- AI 扩写结果对比弹窗 -->
    <el-dialog
      v-model="showExpandDialog"
      title="扩写结果对比"
      width="800px"
      destroy-on-close
      class="polish-dialog"
    >
      <div class="polish-comparison">
        <div class="comparison-panel">
          <div class="comparison-header">
            <span class="comparison-title">原文</span>
          </div>
          <div class="comparison-content">{{ originalContent }}</div>
        </div>
        <div class="comparison-divider">
          <el-icon><Right /></el-icon>
        </div>
        <div class="comparison-panel">
          <div class="comparison-header">
            <span class="comparison-title">扩写后</span>
          </div>
          <div class="comparison-content">{{ expandedContent }}</div>
        </div>
      </div>
      <template #footer>
        <el-button @click="cancelExpand">保留原文</el-button>
        <el-button type="primary" @click="confirmExpand">使用扩写</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Image from '@tiptap/extension-image'
import Link from '@tiptap/extension-link'
import Placeholder from '@tiptap/extension-placeholder'
import EmojiPicker from 'emoji-picker-react'
import { ArrowLeft, Document, Folder, Picture, Star, Promotion, Grid, MagicStick, Right } from '@element-plus/icons-vue'
import { listSections } from '@/api/section'
import { createPost, updatePost, listMyPosts, getPost, deletePost } from '@/api/post'
import { uploadPostImage } from '@/api/post'
import { polish as polishApi, expand as expandApi, getAiStatus } from '@/api/ai'

const router = useRouter()
const route = useRoute()
const formRef = ref()
const submitting = ref(false)
const pageLoading = ref(false)
const sections = ref([])
const showEmojiPicker = ref(false)
const showDraftList = ref(false)
const drafts = ref([])
const loadedDraftId = ref(route.query.draftId ? parseInt(route.query.draftId) : null)

// AI 润色相关
const aiEnabled = ref(false)
const polishStyle = ref('standard')
const polishing = ref(false)
const showPolishMenu = ref(false)
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

/** 去除 AI 返回的 <p> 等包裹标签，避免写入编辑器后显示为标签文本 */
function stripHtmlP(text) {
  if (!text || typeof text !== 'string') return ''
  let s = text.trim()
  if (s.startsWith('<p>') && s.endsWith('</p>')) s = s.slice(3, -4).trim()
  return s.replace(/<\/p>\s*<p>/gi, '\n').replace(/<p>/gi, '').replace(/<\/p>/gi, '').trim()
}

// Tiptap 富文本编辑器配置
const editor = useEditor({
  content: '',
  extensions: [
    StarterKit,
    Image.configure({
      inline: true,
      allowBase64: true
    }),
    Link.configure({
      openOnClick: false
    }),
    Placeholder.configure({
      placeholder: '分享你的想法...'
    })
  ],
  onUpdate: ({ editor }) => {
    form.value.content = editor.getHTML()
  }
})

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
    { min: 5, max: 255, message: '标题长度为 5-255 个字符', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请输入正文', trigger: 'blur' }
  ]
}

// 添加链接
function addLink() {
  const url = window.prompt('请输入链接地址:')
  if (url) {
    editor.value?.chain().focus().setLink({ href: url }).run()
  }
}

// 添加图片
const imageUploading = ref(false)

async function addImage() {
  if (imageUploading.value) return

  const input = document.createElement('input')
  input.type = 'file'
  input.accept = 'image/*'
  input.onchange = async (e) => {
    const file = e.target.files[0]
    if (file) {
      // 验证文件大小
      if (file.size > 10 * 1024 * 1024) {
        ElMessage.warning('图片大小不能超过10MB')
        return
      }

      // 验证文件类型
      if (!file.type.startsWith('image/')) {
        ElMessage.warning('请选择图片文件')
        return
      }

      imageUploading.value = true
      try {
        const res = await uploadPostImage(file)
        const url = res.data
        if (url) {
          editor.value?.chain().focus().setImage({ src: url }).run()
          // 同时将图片URL添加到 uploadedImages，确保提交时能发送到后端
          if (!uploadedImages.value.includes(url)) {
            uploadedImages.value.push(url)
          }
          ElMessage.success('图片添加成功')
        }
      } catch (error) {
        console.error('Image upload error:', error)
        ElMessage.error('图片上传失败，请稍后重试')
      } finally {
        imageUploading.value = false
      }
    }
  }
  input.click()
}

const selectedSectionName = computed(() => {
  const section = sections.value.find(s => s.id === form.value.sectionId)
  return section?.name || ''
})

function removeImage(index) {
  uploadedImages.value.splice(index, 1)
}

function clearAllImages() {
  uploadedImages.value = []
}

function handleEmojiSelect(emoji) {
  form.value.content += emoji.emoji
  showEmojiPicker.value = false
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
  showPolishMenu.value = false

  try {
    const res = await polishApi({ content: form.value.content, style })
    originalContent.value = form.value.content
    polishedContent.value = stripHtmlP(res.data) || res.data
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
  const content = stripHtmlP(polishedContent.value) || polishedContent.value
  form.value.content = content
  editor.value?.commands.setContent(content, false)
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
    expandedContent.value = stripHtmlP(res.data) || res.data
    showExpandDialog.value = true
  } catch (error) {
    console.error('Expand error:', error)
    ElMessage.error(error?.message || '扩写失败，请稍后重试')
  } finally {
    expanding.value = false
  }
}

function confirmExpand() {
  const content = stripHtmlP(expandedContent.value) || expandedContent.value
  form.value.content = content
  editor.value?.commands.setContent(content, false)
  showExpandDialog.value = false
  ElMessage.success('已替换为扩写后的内容')
}

function cancelExpand() {
  showExpandDialog.value = false
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
  showDraftList.value = false
}

async function loadDraft(draft) {
  try {
    const res = await getPost(draft.id)
    const post = res.data
    loadedDraftId.value = post.id
    form.value = {
      sectionId: post.sectionId,
      title: post.title,
      content: post.content,
      images: post.images || [],
      draft: true
    }
    uploadedImages.value = post.images || []
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
    form.value.images = uploadedImages.value

    // 从 HTML 内容中提取图片 URL，作为后备
    if (form.value.content && (!form.value.images || form.value.images.length === 0)) {
      const imgRegex = /<img[^>]+src=["']([^"']+)["']/gi
      const extractedImages = []
      let match
      while ((match = imgRegex.exec(form.value.content)) !== null && extractedImages.length < 9) {
        const src = match[1]
        if (src && typeof src === 'string' && src.trim() && !extractedImages.includes(src.trim())) {
          extractedImages.push(src.trim())
        }
      }
      if (extractedImages.length > 0) {
        form.value.images = extractedImages
      }
    }

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
  if (form.value.title || form.value.content) {
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
</script>

<style scoped>
.create-post-page {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
  min-height: 100vh;
  background: #f5f7fa;
}

.editor-card {
  background: #fff;
  border-radius: 16px;
  padding: 28px 32px;
  border: 1px solid #e4e7ed;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06);
}

.editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 28px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.back-btn {
  font-size: 20px;
  color: #606266;
}

.back-btn:hover {
  color: #409eff;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.header-right {
  display: flex;
  align-items: center;
}

.draft-btn {
  display: flex;
  align-items: center;
  gap: 6px;
}

.draft-badge {
  margin-left: 4px;
}

:deep(.section-form-item) {
  margin-bottom: 20px;
}

:deep(.section-form-item .el-form-item__label) {
  font-size: 15px;
  font-weight: 500;
  color: #303133;
  padding-bottom: 8px;
}

.section-select {
  width: 100%;
}

.section-select :deep(.el-input__wrapper) {
  padding: 12px 16px;
  border-radius: 10px;
}

.section-option {
  display: flex;
  flex-direction: column;
  padding: 4px 0;
}

.section-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.section-desc {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

:deep(.title-form-item) {
  margin-bottom: 20px;
}

:deep(.title-form-item .el-form-item__label) {
  font-size: 15px;
  font-weight: 500;
  color: #303133;
  padding-bottom: 8px;
}

.title-input-wrapper {
  position: relative;
}

.title-input {
  width: 100%;
  padding: 16px 70px 16px 16px;
  font-size: 18px;
  font-weight: 500;
  border: 2px solid #dcdfe6;
  border-radius: 10px;
  outline: none;
  transition: all 0.3s;
}

.title-input:focus {
  border-color: #409eff;
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.1);
}

.title-input::placeholder {
  color: #c0c4cc;
}

.title-count {
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 13px;
  color: #909399;
}

.title-count.near-limit {
  color: #e6a23c;
}

:deep(.content-form-item) {
  margin-bottom: 16px;
}

:deep(.content-form-item .el-form-item__label) {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 15px;
  font-weight: 500;
  color: #303133;
  padding-bottom: 8px;
}

.content-label {
  font-weight: 500;
}

/* Tiptap 编辑器样式 */
.tiptap-editor {
  border: 2px solid #dcdfe6;
  border-radius: 12px;
  overflow-y: auto;
  overflow-x: hidden;
  transition: border-color 0.3s;
  flex: 1;
  display: flex;
  flex-direction: column;
  max-height: 500px;
}

.tiptap-editor:focus-within {
  border-color: #409eff;
}

.tiptap-toolbar {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 12px;
  background: #fafafa;
  border-bottom: 1px solid #ebeef5;
  position: relative;
  z-index: 20;
  overflow: visible;
  flex-shrink: 0;
  border-radius: 10px 10px 0 0;
}

/* el-tooltip 触发器保持行内，避免破坏工具栏 flex 布局 */
.tiptap-toolbar :deep(.el-tooltip__trigger) {
  display: inline-flex;
}

.tiptap-toolbar .toolbar-btn-wrapper {
  position: relative;
  overflow: visible;
}

.tiptap-toolbar button {
  width: 28px;
  height: 28px;
  border: none;
  background: transparent;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  color: #606266;
}

.tiptap-toolbar button:hover {
  background: #e8e8e8;
}

.tiptap-toolbar button.is-active {
  background: #409eff;
  color: #fff;
}

.tiptap-toolbar button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.tiptap-toolbar button:disabled:hover {
  background: transparent;
}

/* 加载动画 */
.loading-spinner {
  display: inline-block;
  width: 14px;
  height: 14px;
  border: 2px solid #409eff;
  border-radius: 50%;
  border-top-color: transparent;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.toolbar-divider {
  color: #dcdfe6;
  margin: 0 4px;
}

.tiptap-content {
  flex: 1;
  min-height: 280px;
  overflow-y: auto;
  overflow-x: hidden;
  border-radius: 0 0 10px 10px;
}

.tiptap-content :deep(.ProseMirror) {
  padding: 16px;
  min-height: 280px;
  outline: none;
  font-size: 15px;
  line-height: 1.7;
}

.tiptap-content :deep(.ProseMirror p.is-editor-empty:first-child::before) {
  content: attr(data-placeholder);
  float: left;
  color: #adb5bd;
  pointer-events: none;
  height: 0;
}

/* Tiptap 编辑器内图片样式 - 限制默认显示尺寸，不占满整屏 */
.tiptap-content :deep(.ProseMirror img) {
  max-width: min(100%, 360px);
  max-height: 280px;
  width: auto;
  height: auto;
  object-fit: contain;
  border-radius: 8px;
  margin: 12px 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  display: block;
  cursor: pointer;
}

.tiptap-content :deep(.ProseMirror img.ProseMirror-selectednode) {
  outline: 3px solid #409eff;
  outline-offset: 2px;
}

/* Tiptap 编辑器内链接样式 */
.tiptap-content :deep(.ProseMirror a) {
  color: #409eff;
  text-decoration: none;
}

.tiptap-content :deep(.ProseMirror a:hover) {
  text-decoration: underline;
}

/* Tiptap 编辑器内代码样式 */
.tiptap-content :deep(.ProseMirror code) {
  background: #f0f0f0;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 0.9em;
}

.tiptap-content :deep(.ProseMirror pre) {
  background: #282c34;
  color: #abb2bf;
  padding: 16px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 12px 0;
}

.tiptap-content :deep(.ProseMirror pre code) {
  background: transparent;
  padding: 0;
  color: inherit;
}

/* Tiptap 编辑器内引用样式 */
.tiptap-content :deep(.ProseMirror blockquote) {
  border-left: 4px solid #409eff;
  background: #f5f7fa;
  padding: 12px 16px;
  margin: 12px 0;
  color: #606266;
}

/* 自定义滚动条样式 */
.tiptap-editor::-webkit-scrollbar {
  width: 8px;
}

.tiptap-editor::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

.tiptap-editor::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 4px;
}

.tiptap-editor::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

.toolbar-group button:hover {
  background: #e8e8e8;
  color: #303133;
}

.toolbar-group button.is-active {
  background: #409eff;
  color: #fff;
}

.toolbar-divider {
  width: 1px;
  height: 24px;
  background: #dcdfe6;
  margin: 0 8px;
}

/* AI 润色工具栏 */
.ai-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: #fafafa;
  border-bottom: 1px solid #ebeef5;
}

.ai-polish-btn,
.ai-expand-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 500;
}

.ai-expand-btn {
  margin-left: 4px;
}

.ai-hint {
  font-size: 12px;
  color: #909399;
}

.style-option {
  display: flex;
  flex-direction: column;
  padding: 2px 0;
}

.style-label {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.style-desc {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

/* 润色对比弹窗 */
.polish-comparison {
  display: flex;
  gap: 16px;
  height: 400px;
}

.comparison-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
}

.comparison-header {
  padding: 12px 16px;
  background: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
}

.comparison-title {
  font-weight: 500;
  color: #303133;
}

.comparison-content {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 14px;
  line-height: 1.8;
  color: #606266;
}

.comparison-divider {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  font-size: 20px;
}

.editor-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  margin-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
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
  color: #606266;
  font-size: 14px;
  transition: all 0.2s;
}

.toolbar-btn:hover {
  background: #ecf5ff;
  color: #409eff;
}

.toolbar-right {
  display: flex;
  align-items: center;
}

.emoji-picker-container {
  position: absolute;
  z-index: 100;
  margin-top: 8px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
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

:deep(.images-form-item) {
  margin-bottom: 20px;
}

:deep(.images-form-item .el-form-item__label) {
  font-size: 15px;
  font-weight: 500;
  color: #303133;
  padding-bottom: 8px;
}

.uploaded-images {
  padding: 16px;
  background: #fafafa;
  border-radius: 10px;
  border: 1px dashed #dcdfe6;
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
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
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
  border: 2px dashed #c0c4cc;
  border-radius: 10px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #909399;
  font-size: 12px;
  transition: all 0.2s;
}

.add-more:hover {
  border-color: #409eff;
  color: #409eff;
  background: #f5f7fa;
}

.images-actions {
  margin-top: 12px;
  text-align: right;
}

.form-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
}

.actions-left {
  display: flex;
  align-items: center;
}

.action-hint {
  font-size: 13px;
  color: #909399;
}

.action-hint kbd {
  display: inline-block;
  padding: 2px 6px;
  font-size: 12px;
  font-family: inherit;
  background: #f5f7fa;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.actions-left .el-checkbox {
  display: flex;
  align-items: center;
  gap: 4px;
}

.actions-right {
  display: flex;
  gap: 12px;
}

.cancel-btn {
  min-width: 80px;
}

.draft-btn {
  min-width: 110px;
}

.publish-btn {
  min-width: 90px;
}

/* 草稿箱样式 */
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
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.2s;
}

.draft-item:hover {
  background: #f5f7fa;
}

.draft-item:last-child {
  border-bottom: none;
}

.draft-content {
  flex: 1;
  min-width: 0;
}

.draft-title {
  font-size: 15px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.draft-summary {
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.draft-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.draft-time {
  font-size: 12px;
  color: #c0c4cc;
}

.draft-actions {
  flex-shrink: 0;
  margin-left: 12px;
}

/* 响应式 */
@media (max-width: 768px) {
  .create-post-page {
    padding: 12px;
  }

  .editor-card {
    padding: 16px;
  }

  .editor-header {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }

  .page-title {
    font-size: 20px;
  }

  .title-input {
    font-size: 16px;
    padding: 14px 60px 14px 14px;
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
