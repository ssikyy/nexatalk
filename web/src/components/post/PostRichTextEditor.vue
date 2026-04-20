<template>
  <div ref="rootRef" class="rich-editor" :style="{ '--editor-min-height': normalizedMinHeight }">
    <div class="tiptap-toolbar">
      <div class="toolbar-group">
        <el-tooltip content="粗体" placement="top" :show-after="250">
          <div class="toolbar-btn-wrapper">
            <button
              type="button"
              :class="{ 'is-active': editor?.isActive('bold') }"
              @click="editor?.chain().focus().toggleBold().run()"
            >
              <strong>B</strong>
            </button>
          </div>
        </el-tooltip>
        <el-tooltip content="斜体" placement="top" :show-after="250">
          <div class="toolbar-btn-wrapper">
            <button
              type="button"
              :class="{ 'is-active': editor?.isActive('italic') }"
              @click="editor?.chain().focus().toggleItalic().run()"
            >
              <em>I</em>
            </button>
          </div>
        </el-tooltip>
        <el-tooltip content="删除线" placement="top" :show-after="250">
          <div class="toolbar-btn-wrapper">
            <button
              type="button"
              :class="{ 'is-active': editor?.isActive('strike') }"
              @click="editor?.chain().focus().toggleStrike().run()"
            >
              <s>S</s>
            </button>
          </div>
        </el-tooltip>
      </div>

      <div v-if="showHeadingTools" class="toolbar-divider"></div>
      <div v-if="showHeadingTools" class="toolbar-group">
        <el-tooltip content="标题 1" placement="top" :show-after="250">
          <div class="toolbar-btn-wrapper">
            <button
              type="button"
              :class="{ 'is-active': editor?.isActive('heading', { level: 1 }) }"
              @click="editor?.chain().focus().toggleHeading({ level: 1 }).run()"
            >
              H1
            </button>
          </div>
        </el-tooltip>
        <el-tooltip content="标题 2" placement="top" :show-after="250">
          <div class="toolbar-btn-wrapper">
            <button
              type="button"
              :class="{ 'is-active': editor?.isActive('heading', { level: 2 }) }"
              @click="editor?.chain().focus().toggleHeading({ level: 2 }).run()"
            >
              H2
            </button>
          </div>
        </el-tooltip>
        <el-tooltip content="标题 3" placement="top" :show-after="250">
          <div class="toolbar-btn-wrapper">
            <button
              type="button"
              :class="{ 'is-active': editor?.isActive('heading', { level: 3 }) }"
              @click="editor?.chain().focus().toggleHeading({ level: 3 }).run()"
            >
              H3
            </button>
          </div>
        </el-tooltip>
      </div>

      <div v-if="showStructureTools" class="toolbar-divider"></div>
      <div v-if="showStructureTools" class="toolbar-group">
        <el-tooltip v-if="props.allowBulletList" content="无序列表" placement="top" :show-after="250">
          <div class="toolbar-btn-wrapper">
            <button
              type="button"
              :class="{ 'is-active': editor?.isActive('bulletList') }"
              @click="editor?.chain().focus().toggleBulletList().run()"
            >
              •
            </button>
          </div>
        </el-tooltip>
        <el-tooltip v-if="props.allowOrderedList" content="有序列表" placement="top" :show-after="250">
          <div class="toolbar-btn-wrapper">
            <button
              type="button"
              :class="{ 'is-active': editor?.isActive('orderedList') }"
              @click="editor?.chain().focus().toggleOrderedList().run()"
            >
              1.
            </button>
          </div>
        </el-tooltip>
        <el-tooltip v-if="props.allowBlockquote" content="引用" placement="top" :show-after="250">
          <div class="toolbar-btn-wrapper">
            <button
              type="button"
              :class="{ 'is-active': editor?.isActive('blockquote') }"
              @click="editor?.chain().focus().toggleBlockquote().run()"
            >
              "
            </button>
          </div>
        </el-tooltip>
      </div>

      <div v-if="showInsertTools" class="toolbar-divider"></div>
      <div v-if="showInsertTools" class="toolbar-group">
        <el-tooltip v-if="props.allowLink" content="添加链接" placement="top" :show-after="250">
          <div class="toolbar-btn-wrapper">
            <button type="button" @click="addLink">🔗</button>
          </div>
        </el-tooltip>
        <el-tooltip v-if="props.allowImage" :content="imageUploading ? '上传中...' : '添加图片'" placement="top" :show-after="250">
          <div class="toolbar-btn-wrapper">
            <button type="button" :disabled="imageUploading" @click="addImage">
              <span v-if="imageUploading" class="loading-spinner"></span>
              <span v-else>🖼️</span>
            </button>
          </div>
        </el-tooltip>
        <el-tooltip v-if="props.allowCodeBlock" content="代码块" placement="top" :show-after="250">
          <div class="toolbar-btn-wrapper">
            <button
              type="button"
              :class="{ 'is-active': editor?.isActive('codeBlock') }"
              @click="editor?.chain().focus().toggleCodeBlock().run()"
            >
              &lt;&gt;
            </button>
          </div>
        </el-tooltip>
        <el-tooltip v-if="props.allowEmoji" content="插入 Emoji" placement="top" :show-after="250">
          <div class="toolbar-btn-wrapper">
            <button type="button" @click="showEmojiPicker = !showEmojiPicker">😊</button>
          </div>
        </el-tooltip>
      </div>
    </div>

    <editor-content :editor="editor" class="tiptap-content" />

    <transition name="fade">
      <div v-if="showEmojiPicker" class="emoji-picker-container">
        <EmojiPicker :native="true" @select="handleEmojiSelect" />
      </div>
    </transition>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { EditorContent, useEditor } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Image from '@tiptap/extension-image'
import Link from '@tiptap/extension-link'
import Placeholder from '@tiptap/extension-placeholder'
import EmojiPicker from 'emoji-picker-react'
import { uploadPostImage } from '@/api/post'
import { extractImageUrls } from '@/utils/postContent'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  placeholder: {
    type: String,
    default: '分享你的想法...'
  },
  minHeight: {
    type: [Number, String],
    default: 360
  },
  allowHeading: {
    type: Boolean,
    default: true
  },
  allowBulletList: {
    type: Boolean,
    default: true
  },
  allowOrderedList: {
    type: Boolean,
    default: true
  },
  allowBlockquote: {
    type: Boolean,
    default: true
  },
  allowLink: {
    type: Boolean,
    default: true
  },
  allowCodeBlock: {
    type: Boolean,
    default: true
  },
  allowImage: {
    type: Boolean,
    default: true
  },
  allowEmoji: {
    type: Boolean,
    default: true
  },
  maxImages: {
    type: Number,
    default: 9
  }
})

const emit = defineEmits(['update:modelValue', 'images-change'])

const rootRef = ref(null)
const showEmojiPicker = ref(false)
const imageUploading = ref(false)

const normalizedMinHeight = computed(() => {
  return typeof props.minHeight === 'number' ? `${props.minHeight}px` : props.minHeight
})

const showHeadingTools = computed(() => props.allowHeading)
const showStructureTools = computed(() => {
  return props.allowBulletList || props.allowOrderedList || props.allowBlockquote
})
const showInsertTools = computed(() => {
  return props.allowLink || props.allowImage || props.allowCodeBlock || props.allowEmoji
})

const extensions = [
  StarterKit.configure({
    heading: props.allowHeading
      ? {
          levels: [1, 2, 3]
        }
      : false,
    bulletList: props.allowBulletList,
    orderedList: props.allowOrderedList,
    listItem: props.allowBulletList || props.allowOrderedList,
    blockquote: props.allowBlockquote,
    codeBlock: props.allowCodeBlock
  }),
  Image.configure({
    inline: true,
    allowBase64: false
  }),
  Placeholder.configure({
    placeholder: props.placeholder
  })
]

if (props.allowLink) {
  extensions.push(
    Link.configure({
      openOnClick: false,
      autolink: true
    })
  )
}

const editor = useEditor({
  content: props.modelValue || '',
  extensions,
  onUpdate: ({ editor }) => {
    const html = editor.getHTML()
    emit('update:modelValue', html)
    emit('images-change', extractImageUrls(html))
  }
})

watch(
  () => props.modelValue,
  (value) => {
    if (!editor.value) return
    const nextValue = value || ''
    if (editor.value.getHTML() === nextValue) return
    editor.value.commands.setContent(nextValue, false)
    emit('images-change', extractImageUrls(nextValue))
  }
)

function handleDocumentClick(event) {
  if (!showEmojiPicker.value) return
  if (rootRef.value && !rootRef.value.contains(event.target)) {
    showEmojiPicker.value = false
  }
}

function handleEmojiSelect(emoji) {
  editor.value?.chain().focus().insertContent(emoji.emoji).run()
  showEmojiPicker.value = false
}

function addLink() {
  const url = window.prompt('请输入链接地址:')
  if (!url) return
  editor.value?.chain().focus().setLink({ href: url }).run()
}

async function addImage() {
  if (!props.allowImage) return
  if (imageUploading.value) return

  const currentImages = extractImageUrls(editor.value?.getHTML() || '')
  const remainingSlots = props.maxImages - currentImages.length
  if (remainingSlots <= 0) {
    ElMessage.warning(`最多只能上传 ${props.maxImages} 张图片`)
    return
  }

  const input = document.createElement('input')
  input.type = 'file'
  input.accept = 'image/*'
  input.multiple = true
  input.onchange = async (event) => {
    const files = Array.from(event.target.files || []).slice(0, remainingSlots)
    if (files.length === 0) return

    imageUploading.value = true
    let successCount = 0

    try {
      for (const file of files) {
        if (file.size > 10 * 1024 * 1024) {
          ElMessage.warning(`${file.name} 超过 10MB 限制`)
          continue
        }
        if (!file.type.startsWith('image/')) {
          ElMessage.warning(`${file.name} 不是图片文件`)
          continue
        }

        const res = await uploadPostImage(file)
        const url = res.data
        if (!url) continue
        editor.value?.chain().focus().setImage({ src: url }).run()
        successCount++
      }

      if (successCount > 0) {
        ElMessage.success(`已添加 ${successCount} 张图片`)
      }
    } catch (error) {
      ElMessage.error('图片上传失败，请稍后重试')
    } finally {
      imageUploading.value = false
      input.value = ''
    }
  }
  input.click()
}

function setContent(content = '') {
  if (!editor.value) return
  editor.value.commands.setContent(content, false)
  const html = editor.value.getHTML()
  emit('update:modelValue', html)
  emit('images-change', extractImageUrls(html))
}

function getHTML() {
  return editor.value?.getHTML() || ''
}

function focus() {
  editor.value?.chain().focus().run()
}

onMounted(() => {
  document.addEventListener('click', handleDocumentClick)
})

onUnmounted(() => {
  document.removeEventListener('click', handleDocumentClick)
  editor.value?.destroy()
})

defineExpose({
  setContent,
  getHTML,
  focus
})
</script>

<style scoped>
.rich-editor {
  position: relative;
  overflow: hidden;
  border: 1px solid #dbe4ee;
  border-radius: 14px;
  background: #fff;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.75);
}

.tiptap-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  border-bottom: 1px solid #e7edf5;
  background: linear-gradient(180deg, #fbfdff 0%, #f6f9fc 100%);
}

.toolbar-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tiptap-toolbar :deep(.el-tooltip__trigger) {
  display: inline-flex;
}

.toolbar-btn-wrapper {
  display: inline-flex;
}

.toolbar-divider {
  width: 1px;
  height: 22px;
  background: #d8e1ec;
}

.toolbar-group button {
  min-width: 38px;
  height: 38px;
  padding: 0 10px;
  border: 1px solid #d9e3f0;
  border-radius: 10px;
  background: #fff;
  color: #334155;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.toolbar-group button:hover:not(:disabled) {
  border-color: #7aa8ff;
  color: #2563eb;
  box-shadow: 0 4px 10px rgba(59, 130, 246, 0.1);
}

.toolbar-group button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.toolbar-group button.is-active {
  color: #2563eb;
  border-color: #8bb4ff;
  background: #eff6ff;
}

.tiptap-content :deep(.ProseMirror) {
  min-height: var(--editor-min-height);
  padding: 22px 20px;
  background: #fff;
  outline: none;
  font-size: 16px;
  line-height: 1.9;
  color: #1e293b;
}

.tiptap-content :deep(.ProseMirror p.is-editor-empty:first-child::before) {
  content: attr(data-placeholder);
  float: left;
  color: #9aa4b2;
  pointer-events: none;
  height: 0;
}

.tiptap-content :deep(.ProseMirror h1),
.tiptap-content :deep(.ProseMirror h2),
.tiptap-content :deep(.ProseMirror h3) {
  margin: 1.2em 0 0.6em;
  color: #0f172a;
  font-weight: 700;
  line-height: 1.35;
}

.tiptap-content :deep(.ProseMirror h1) {
  font-size: 28px;
}

.tiptap-content :deep(.ProseMirror h2) {
  font-size: 24px;
}

.tiptap-content :deep(.ProseMirror h3) {
  font-size: 20px;
}

.tiptap-content :deep(.ProseMirror ul),
.tiptap-content :deep(.ProseMirror ol) {
  padding-left: 1.4em;
}

.tiptap-content :deep(.ProseMirror blockquote) {
  margin: 1em 0;
  padding-left: 14px;
  border-left: 4px solid #bfdbfe;
  color: #475569;
  background: #f8fbff;
}

.tiptap-content :deep(.ProseMirror pre) {
  padding: 14px 16px;
  border-radius: 12px;
  background: #0f172a;
  color: #e2e8f0;
  overflow-x: auto;
}

.tiptap-content :deep(.ProseMirror code) {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
}

.tiptap-content :deep(.ProseMirror a) {
  color: #2563eb;
  text-decoration: underline;
}

.tiptap-content :deep(.ProseMirror img) {
  display: block;
  max-width: min(100%, 560px);
  max-height: 420px;
  margin: 18px auto;
  border-radius: 14px;
  object-fit: cover;
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.12);
}

.emoji-picker-container {
  position: absolute;
  z-index: 20;
  top: 70px;
  right: 16px;
  box-shadow: 0 18px 48px rgba(15, 23, 42, 0.16);
  border-radius: 16px;
  overflow: hidden;
}

.loading-spinner {
  display: inline-block;
  width: 14px;
  height: 14px;
  border: 2px solid rgba(37, 99, 235, 0.18);
  border-top-color: #2563eb;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
