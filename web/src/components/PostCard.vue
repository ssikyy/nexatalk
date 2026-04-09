<template>
  <div 
    class="post-card" 
    :class="{ 'is-detail': detailMode, 'is-hoverable': !detailMode }"
    @click="!detailMode && handleCardClick()"
    v-if="!blocked"
  >
    <!-- 头部：作者信息 -->
    <div class="card-header">
      <div class="author-area" @click.stop="$router.push(`/user/${post.authorId || post.userId}`)">
        <el-avatar :src="post.authorAvatar" :size="detailMode ? 48 : 40" class="avatar">
          {{ post.authorName?.charAt(0) }}
        </el-avatar>
        <div class="meta-info">
          <span class="nickname" :class="{ 'text-lg': detailMode }">{{ post.authorName }}</span>
          <span class="time">
            {{ formatTime(post.createdAt) }}
            <span v-if="detailMode && post.viewCount" class="views">· {{ post.viewCount }} 阅读</span>
          </span>
        </div>
      </div>
      <!-- 更多操作（列表模式下显示） -->
      <el-dropdown
        v-if="!detailMode"
        trigger="click"
        @command="handleMoreCommand"
      >
        <div class="more-btn" @click.stop>
          <el-icon><MoreFilled /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item
              v-if="!isOwner"
              command="follow"
            >
              {{ followText }}
            </el-dropdown-item>
            <el-dropdown-item
              v-if="!isOwner"
              divided
              command="block"
            >
              屏蔽用户
            </el-dropdown-item>
            <el-dropdown-item command="view">
              查看帖子
            </el-dropdown-item>
            <el-dropdown-item
              divided
              command="report"
            >
              举报帖子
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
      <div v-else class="detail-actions">
         <el-button v-if="isOwner" link type="primary" @click="$emit('edit')">编辑</el-button>
         <el-button v-if="isOwner" link type="danger" @click="$emit('delete')">删除</el-button>
      </div>
    </div>

      <!-- 内容区 -->
    <div class="card-content">
      <!-- 帖子标识 -->
      <div class="post-badges" v-if="!detailMode && (post.isPinned || post.isFeatured)">
        <span v-if="post.isPinned" class="badge badge-pinned">
          <el-icon><Top /></el-icon>
          置顶
        </span>
        <span v-if="post.isFeatured" class="badge badge-featured">
          <el-icon><StarFilled /></el-icon>
          精华
        </span>
      </div>

      <template v-if="detailMode">
        <h1 class="title detail-title" v-if="post.title">{{ post.title }}</h1>
        <div class="detail-body" v-html="renderedContent"></div>
      </template>
      <template v-else>
        <h3 class="title" v-if="post.title">{{ post.title }}</h3>
        <p class="summary">{{ plainSummary }}</p>
      </template>
      
      <!-- 图片九宫格 - 微博风格布局 -->
      <div
        class="image-grid"
        v-if="displayImages.length > 0"
        :class="getImageGridClass(displayImages.length)"
      >
        <div
          v-for="(img, index) in displayImages.slice(0, 9)"
          :key="index"
          class="image-item"
          :style="{ backgroundImage: `url(${img})` }"
          @click.stop="handleImagePreview(index)"
        ></div>
      </div>
    </div>

    <!-- 底部：操作栏 -->
    <div class="card-footer" @click.stop>
      <div 
        class="action-btn" 
        :class="{ active: isLiked }"
        @click="toggleLike"
      >
        <div class="icon-wrapper" :class="{ 'anim-scale': likeAnimating }">
          <el-icon><component :is="isLiked ? StarFilled : Star" /></el-icon>
        </div>
        <span class="count">{{ likeCount || '点赞' }}</span>
      </div>

      <div class="action-btn" @click="!detailMode && handleCardClick()">
        <el-icon><ChatDotSquare /></el-icon>
        <span class="count">{{ post.commentCount || '评论' }}</span>
      </div>

      <div 
        class="action-btn"
        :class="{ active: isFavorited }"
        @click="toggleFavorite"
      >
        <el-icon><component :is="isFavorited ? 'CollectionTag' : 'Collection'" /></el-icon>
        <span class="count">{{ isFavorited ? '已收藏' : '收藏' }}</span>
      </div>
      
      <div class="action-btn share-btn">
        <el-icon><Share /></el-icon>
      </div>
    </div>

    <!-- 举报弹窗 -->
    <el-dialog
      v-model="reportDialogVisible"
      title="举报帖子"
      width="400px"
      destroy-on-close
      append-to-body
    >
      <el-form :model="reportForm" label-width="80px">
        <el-form-item label="举报原因" required>
          <el-radio-group v-model="reportForm.reason">
            <el-radio
              v-for="item in reportReasons"
              :key="item.value"
              :value="item.value"
            >
              {{ item.label }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="详细说明" v-if="reportForm.reason === 4">
          <el-input
            v-model="reportForm.description"
            type="textarea"
            :rows="3"
            placeholder="请补充具体问题（必填）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reportDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="reportLoading"
          @click="submitReport"
        >
          提交举报
        </el-button>
      </template>
    </el-dialog>

    <!-- 图片预览：Twitter 风格全屏遮罩，仅图片 + 控件 -->
    <div
      v-if="imagePreviewVisible"
      class="twitter-image-overlay"
    >
      <!-- 关闭按钮 -->
      <button class="twitter-close-btn" @click="imagePreviewVisible = false">
        <el-icon><Close /></el-icon>
      </button>

      <div class="twitter-image-wrapper">
        <!-- 左侧点击区域 -->
        <div
          class="twitter-nav twitter-nav-left"
          v-if="previewImages.length > 1"
          @click.stop="prevImage"
        >
          <el-icon><ArrowLeft /></el-icon>
        </div>

        <!-- 中间图片 -->
        <img
          :src="previewImages[currentPreviewIndex]"
          :alt="'图片 ' + (currentPreviewIndex + 1)"
          class="twitter-image"
          @click.stop
        />

        <!-- 右侧点击区域 -->
        <div
          class="twitter-nav twitter-nav-right"
          v-if="previewImages.length > 1"
          @click.stop="nextImage"
        >
          <el-icon><ArrowRight /></el-icon>
        </div>
      </div>

      <!-- 底部计数器 -->
      <div
        class="twitter-counter"
        v-if="previewImages.length > 1"
      >
        {{ currentPreviewIndex + 1 }} / {{ previewImages.length }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Star, StarFilled, ChatDotSquare, Collection, CollectionTag, MoreFilled, Share, Top, ArrowLeft, ArrowRight, Close } from '@element-plus/icons-vue'
import { likePost, unlikePost } from '@/api/like'
import { favorite, unfavorite } from '@/api/favorite'
import { follow, unfollow, isFollowing } from '@/api/follow'
import { createReport } from '@/api/report'
import { addToBlacklist, checkBlacklist } from '@/api/blacklist'
import { useUserStore } from '@/stores/user'

const props = defineProps({
  post: { type: Object, required: true },
  detailMode: { type: Boolean, default: false }
})

// 统一的图片来源：优先使用后端 images 字段，其次 coverUrl，最后从正文 Markdown 中提取
const displayImages = computed(() => {
  const rawImages = Array.isArray(props.post.images)
    ? props.post.images
    : props.post.images
      ? [props.post.images]
      : []

  const normalized = rawImages
    .filter((url) => typeof url === 'string' && url.trim())
    .map((url) => url.trim())

  if (normalized.length > 0) {
    return normalized
  }

  const fallback = []

  if (typeof props.post.coverUrl === 'string' && props.post.coverUrl.trim()) {
    fallback.push(props.post.coverUrl.trim())
  }

  if (fallback.length === 0 && typeof props.post.content === 'string') {
    const content = props.post.content
    // 支持 HTML 中的 <img src="...">
    const imgRegex = /<img[^>]+src=["']([^"']+)["']/gi
    let match
    while ((match = imgRegex.exec(content)) !== null && fallback.length < 9) {
      const url = match[1]
      if (url && typeof url === 'string' && url.trim()) {
        fallback.push(url.trim())
      }
    }
    // 支持 Markdown 中的 ![alt](url)
    if (fallback.length === 0) {
      const mdRegex = /!\[[^\]]*]\((.*?)\)/g
      while ((match = mdRegex.exec(content)) !== null && fallback.length < 9) {
        const url = match[1]
        if (url && typeof url === 'string' && url.trim()) {
          fallback.push(url.trim())
        }
      }
    }
  }

  return fallback
})

// 首页摘要：先解码 HTML 实体再去掉标签，避免 <img> 等以实体形式残留为明文
const plainSummary = computed(() => {
  const raw = props.post.summary || props.post.content || ''
  if (!raw || typeof raw !== 'string') return ''
  let text = raw
    .replace(/&nbsp;/g, ' ')
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
  text = text.replace(/<[^>]+>/g, '').trim()
  const maxLen = 150
  if (text.length <= maxLen) return text
  return text.slice(0, maxLen) + '...'
})

const emit = defineEmits(['edit', 'delete', 'block'])
const router = useRouter()
const userStore = useUserStore()

// 本地状态
const isLiked = ref(!!props.post.isLiked)
const likeCount = ref(props.post.likeCount || 0)
const likeAnimating = ref(false)
const isFavorited = ref(!!props.post.isFavorited)
const isFollowingAuthor = ref(false)
const blocked = ref(false)

// 图片预览相关
const imagePreviewVisible = ref(false)
const previewImages = ref([])
const currentPreviewIndex = ref(0)

// 举报弹窗相关
const reportDialogVisible = ref(false)
const reportForm = ref({
  reason: 1,
  description: ''
})
const reportReasons = [
  { value: 1, label: '垃圾广告' },
  { value: 2, label: '违规内容' },
  { value: 3, label: '侮辱漫骂' },
  { value: 4, label: '其他' }
]
const reportLoading = ref(false)

// 监听 props 变化，同步状态（用于详情页加载后更新）
watch(() => props.post, (newVal) => {
  if (newVal) {
    isLiked.value = !!newVal.isLiked
    likeCount.value = newVal.likeCount || 0
    isFavorited.value = !!newVal.isFavorited
  }
}, { deep: true, immediate: true })

const isOwner = computed(() => {
  return userStore.isLoggedIn && (props.post.userId === userStore.userInfo?.userId || props.post.authorId === userStore.userInfo?.userId)
})

const followText = computed(() => {
  if (!userStore.isLoggedIn || isOwner.value) return '关注'
  return isFollowingAuthor.value ? '已关注' : '关注'
})

// 详情模式：若内容含 HTML（如 Tiptap 输出）则直接渲染，否则转义后换行
const renderedContent = computed(() => {
  if (!props.post.content) return ''
  const c = props.post.content
  if (/<[a-z][\s\S]*>/i.test(c)) return c
  return c
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\n/g, '<br>')
})

function formatTime(timeStr) {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const now = new Date()
  const diff = (now - date) / 1000 
  
  if (diff < 60) return '刚刚'
  if (diff < 3600) return `${Math.floor(diff / 60)}分钟前`
  if (diff < 86400) return `${Math.floor(diff / 3600)}小时前`
  if (diff < 2592000) return `${Math.floor(diff / 86400)}天前`
  return date.toLocaleDateString()
}

function handleCardClick() {
  if (props.detailMode) return
  router.push(`/posts/${props.post.id}`)
}

function handleImagePreview(index) {
  previewImages.value = displayImages.value
  currentPreviewIndex.value = index
  imagePreviewVisible.value = true
}

// 微博风格图片网格布局
function getImageGridClass(count) {
  const classes = []
  // 微博风格：1张是大图，2张并排，3张左二右一，4张田字格，9宫格
  if (count === 1) {
    classes.push('weibo-1')
  } else if (count === 2) {
    classes.push('weibo-2')
  } else if (count === 3) {
    classes.push('weibo-3')
  } else if (count === 4) {
    classes.push('weibo-4')
  } else {
    classes.push('weibo-more')
  }
  return classes
}

function prevImage() {
  if (currentPreviewIndex.value > 0) {
    currentPreviewIndex.value--
  }
}

function nextImage() {
  if (currentPreviewIndex.value < previewImages.value.length - 1) {
    currentPreviewIndex.value++
  }
}

async function toggleLike() {
  if (!userStore.isLoggedIn) return router.push('/login')
  
  const originalState = isLiked.value
  isLiked.value = !isLiked.value
  likeCount.value += isLiked.value ? 1 : -1
  if (isLiked.value) {
    likeAnimating.value = true
    setTimeout(() => likeAnimating.value = false, 300)
  }

  try {
    if (isLiked.value) {
      await likePost(props.post.id)
    } else {
      await unlikePost(props.post.id)
    }
  } catch (error) {
    isLiked.value = originalState
    likeCount.value += isLiked.value ? 1 : -1
    ElMessage.error('操作失败')
  }
}

async function toggleFavorite() {
  if (!userStore.isLoggedIn) return router.push('/login')
  const originalState = isFavorited.value
  isFavorited.value = !isFavorited.value
  try {
    if (isFavorited.value) {
      await favorite(props.post.id)
      ElMessage.success('收藏成功')
    } else {
      await unfavorite(props.post.id)
    }
  } catch (error) {
    isFavorited.value = originalState
    ElMessage.error('操作失败')
  }
}

// 更多菜单相关
const BLOCK_STORAGE_KEY = 'blockedUserIds'

async function loadBlockStatus() {
  const authorId = props.post.userId || props.post.authorId
  if (!authorId) return
  // 优先从后端获取黑名单状态，如果没有登录则用本地存储
  if (userStore.isLoggedIn) {
    try {
      const res = await checkBlacklist(authorId)
      blocked.value = !!res.data
    } catch {
      // 后端失败时使用本地存储
      const stored = JSON.parse(localStorage.getItem(BLOCK_STORAGE_KEY) || '[]')
      blocked.value = stored.includes(authorId)
    }
  } else {
    const stored = JSON.parse(localStorage.getItem(BLOCK_STORAGE_KEY) || '[]')
    blocked.value = stored.includes(authorId)
  }
}

async function loadFollowStatus() {
  if (!userStore.isLoggedIn) return
  const authorId = props.post.userId || props.post.authorId
  if (!authorId || isOwner.value) return
  try {
    const res = await isFollowing(authorId)
    isFollowingAuthor.value = !!res.data
  } catch {
    isFollowingAuthor.value = false
  }
}

async function handleToggleFollow() {
  if (!userStore.isLoggedIn) return router.push('/login')
  const authorId = props.post.userId || props.post.authorId
  if (!authorId || isOwner.value) return
  try {
    if (isFollowingAuthor.value) {
      await unfollow(authorId)
      isFollowingAuthor.value = false
      ElMessage.success('已取消关注')
    } else {
      await follow(authorId)
      isFollowingAuthor.value = true
      ElMessage.success('关注成功')
    }
  } catch {
    ElMessage.error('操作失败')
  }
}

async function handleBlockUser() {
  const authorId = props.post.userId || props.post.authorId
  if (!authorId) return
  if (!userStore.isLoggedIn) return router.push('/login')
  try {
    await addToBlacklist(authorId)
    blocked.value = true
    emit('block', authorId)
    ElMessage.success('已拉黑该用户，可在设置-黑名单中管理')
  } catch {
    ElMessage.error('操作失败')
  }
}

function handleReportPost() {
  if (!userStore.isLoggedIn) return router.push('/login')
  reportForm.value = { reason: 1, description: '' }
  reportDialogVisible.value = true
}

async function submitReport() {
  if (reportForm.value.reason === 4 && !reportForm.value.description.trim()) {
    ElMessage.warning('请补充详细说明')
    return
  }
  reportLoading.value = true
  try {
    await createReport({
      entityType: 1,
      entityId: props.post.id,
      reason: reportForm.value.reason,
      description: reportForm.value.description.trim()
    })
    ElMessage.success('举报已提交，感谢你的反馈')
    reportDialogVisible.value = false
  } catch (e) {
    ElMessage.error('举报失败，请稍后重试')
  } finally {
    reportLoading.value = false
  }
}

function handleMoreCommand(command) {
  if (command === 'follow') {
    handleToggleFollow()
  } else if (command === 'block') {
    handleBlockUser()
  } else if (command === 'view') {
    handleCardClick()
  } else if (command === 'report') {
    handleReportPost()
  }
}

onMounted(() => {
  loadBlockStatus()
  loadFollowStatus()
})
</script>

<style scoped>
.post-card {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  border: 1px solid #f0f0f0;
  transition: all 0.25s ease;
}

.post-card.is-hoverable {
  cursor: pointer;
}

.post-card.is-hoverable:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  border-color: #e8e8e8;
}

.post-card.is-detail {
  border: none;
  border-radius: 0;
  box-shadow: none;
  padding: 0;
  background: transparent;
}

/* Header */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.author-area {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}

.avatar {
  border: 2px solid #f5f5f5;
}

.meta-info {
  display: flex;
  flex-direction: column;
}

.nickname {
  font-weight: 600;
  font-size: 15px;
  color: #1f1f1f;
}
.nickname.text-lg {
  font-size: 16px;
}

.time {
  font-size: 12px;
  color: #86909c;
}

.views {
  margin-left: 4px;
}

/* Content */
.card-content {
  margin-bottom: 16px;
}

.title {
  font-size: 17px;
  font-weight: 700;
  color: #1f1f1f;
  margin-bottom: 10px;
  line-height: 1.4;
}

.detail-title {
  font-size: 24px;
  margin-bottom: 16px;
}

.summary {
  font-size: 15px;
  color: #4a5568;
  line-height: 1.65;
}

.detail-body {
  font-size: 16px;
  color: #1f1f1f;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
}

/* 帖子标识 */
.post-badges {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}

.badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.badge-pinned {
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a24 100%);
  color: #fff;
}

.badge-featured {
  background: linear-gradient(135deg, #ffd93d 0%, #f39c12 100%);
  color: #fff;
}

/* Images（缩略图 Twitter 风格布局） */
.image-grid {
  display: grid;
  margin-top: 12px;
  width: 100%;
  border-radius: 12px;
  overflow: hidden;
  gap: 2px;
  background: #000; /* 细黑线分隔效果 */
  max-height: 200px;
}

/* 单图：使用较小比例 */
.image-grid.weibo-1 {
  grid-template-columns: 1fr;
}

.image-grid.weibo-1 .image-item {
  aspect-ratio: 4 / 3;
}

/* 双图：左右平均 */
.image-grid.weibo-2 {
  grid-template-columns: repeat(2, 1fr);
}

.image-grid.weibo-2 .image-item {
  aspect-ratio: 4 / 3;
}

/* 三图：左 1 大图，右侧上下两张 */
.image-grid.weibo-3 {
  grid-template-columns: 2fr 1fr;
  grid-template-rows: repeat(2, 1fr);
}

.image-grid.weibo-3 .image-item:first-child {
  grid-row: 1 / 3;
  aspect-ratio: 4 / 3;
}

.image-grid.weibo-3 .image-item:not(:first-child) {
  aspect-ratio: 4 / 3;
}

/* 四图：2x2 网格 */
.image-grid.weibo-4 {
  grid-template-columns: repeat(2, 1fr);
  grid-auto-rows: 1fr;
}

.image-grid.weibo-4 .image-item {
  aspect-ratio: 1;
}

/* 多于 4 张：3 列九宫格 */
.image-grid.weibo-more {
  grid-template-columns: repeat(3, 1fr);
}

.image-grid.weibo-more .image-item {
  aspect-ratio: 1;
}

.image-item {
  width: 100%;
  background-size: cover;
  background-position: center;
  background-color: #000;
  cursor: zoom-in;
  transition: transform 0.2s;
}

.image-item:hover {
  transform: scale(1.02);
}

/* Footer */
.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 14px;
  border-top: 1px solid #f0f0f0;
  margin-top: 12px;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #86909c;
  font-size: 14px;
  padding: 8px 14px;
  border-radius: 20px;
  transition: all 0.2s;
  cursor: pointer;
}

.action-btn:hover {
  background: #f5f7fa;
  color: #4a5568;
}

.action-btn.active {
  color: #667eea;
}
.action-btn.active .icon-wrapper {
  color: #667eea;
}

.icon-wrapper {
  display: flex;
  align-items: center;
  font-size: 16px;
}

.anim-scale {
  animation: scale-up 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

@keyframes scale-up {
  0% { transform: scale(1); }
  50% { transform: scale(1.4); }
  100% { transform: scale(1); }
}

.share-btn {
  margin-left: auto;
}

.more-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #86909c;
  transition: all 0.2s;
}

.more-btn:hover {
  background: #f5f7fa;
  color: #4a5568;
}

.detail-actions {
  display: flex;
  gap: 8px;
}

@media (max-width: 768px) {
  .post-card {
    padding: 16px;
    border-radius: 12px;
  }
  
  .title {
    font-size: 16px;
  }
  
  .summary {
    font-size: 14px;
  }
}

/* 图片预览 - Twitter 风格全屏遮罩 */
.twitter-image-overlay {
  position: fixed;
  inset: 0;
  z-index: 2000;
  background: rgba(0, 0, 0, 0.9);
  display: flex;
  align-items: center;
  justify-content: center;
}

.twitter-image-wrapper {
  position: relative;
  max-width: 90vw;
  max-height: 90vh;
  display: flex;
  align-items: center;
  justify-content: center;
}

.twitter-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  display: block;
}

.twitter-close-btn {
  position: fixed;
  top: 16px;
  left: 16px;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: none;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 18px;
  transition: background 0.2s;
}

.twitter-close-btn:hover {
  background: rgba(0, 0, 0, 0.8);
}

.twitter-nav {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 80px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.2s;
}

.twitter-nav-left {
  left: 0;
}

.twitter-nav-right {
  right: 0;
}

.twitter-image-wrapper:hover .twitter-nav {
  opacity: 1;
}

.twitter-nav .el-icon {
  font-size: 32px;
  color: #fff;
  background: rgba(0, 0, 0, 0.6);
  border-radius: 50%;
  padding: 10px;
}

.twitter-counter {
  position: fixed;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  color: #fff;
  font-size: 13px;
  background: rgba(0, 0, 0, 0.6);
  padding: 4px 14px;
  border-radius: 14px;
}
</style>
