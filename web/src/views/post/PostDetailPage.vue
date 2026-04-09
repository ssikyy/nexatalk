<template>
  <div class="post-detail-container" v-loading="loading">
    <!-- 返回按钮 -->
    <div class="back-nav">
      <el-button class="back-btn" text @click="$router.back()">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>
    </div>

    <!-- 帖子主体 -->
    <div class="post-main" v-if="post">
      <!-- 作者信息大卡片 -->
      <div class="post-author-card">
        <div class="post-header-row">
          <div class="author-header" @click="$router.push(`/user/${post.authorId || post.userId}`)">
            <el-avatar :src="post.authorAvatar" :size="56" class="author-avatar">
              {{ post.authorName?.charAt(0) }}
            </el-avatar>
            <div class="author-info">
              <span class="author-name">{{ post.authorName }}</span>
              <span class="author-handle">@{{ post.authorName?.toLowerCase() }}</span>
            </div>
          </div>

          <div class="post-more" v-if="isOwner">
            <el-dropdown trigger="click" @command="handleMoreCommand">
              <button class="icon-button" type="button">
                <el-icon><MoreFilled /></el-icon>
              </button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="edit">编辑帖子</el-dropdown-item>
                  <el-dropdown-item command="delete" divided>删除帖子</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>

        <!-- 帖子内容 -->
        <div class="post-content-area">
          <h1 class="post-title" v-if="post.title">{{ post.title }}</h1>
          <div class="post-text" v-html="renderedContent"></div>
          
          <!-- 图片 - 微博风格布局 -->
          <div
            class="post-images"
            v-if="displayImages.length > 0"
            :class="getImageGridClass(displayImages.length)"
          >
            <div
              v-for="(img, index) in displayImages.slice(0, 9)"
              :key="index"
              class="post-image"
              :style="{ backgroundImage: `url(${img})` }"
              @click.stop="handleImagePreview(index)"
            ></div>
          </div>
          
          <!-- 元信息 -->
          <div class="post-meta">
            <span class="meta-time">{{ formatFullTime(post.createdAt) }}</span>
            <span class="meta-divider">·</span>
            <span class="meta-views">{{ post.viewCount || 0 }} 次浏览</span>
          </div>
          
          <!-- 统计数据 -->
          <div class="post-stats">
            <div class="stat-item" v-if="post.commentCount">
              <span class="stat-value">{{ post.commentCount }}</span>
              <span class="stat-label">回复</span>
            </div>
            <div class="stat-item" v-if="post.likeCount">
              <span class="stat-value">{{ post.likeCount }}</span>
              <span class="stat-label">喜欢</span>
            </div>
            <div class="stat-item" v-if="post.favoriteCount">
              <span class="stat-value">{{ post.favoriteCount }}</span>
              <span class="stat-label">收藏</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 评论区 -->
      <div class="comments-section">
        <div class="comments-header">
          <h2 class="comments-title">{{ commentTotal }} 条回复</h2>
        </div>
        
        <!-- 评论输入框 -->
        <div class="comment-input-area" v-if="userStore.isLoggedIn">
          <el-avatar :src="userStore.userInfo?.avatarUrl" :size="40" class="comment-avatar">
            {{ userStore.userInfo?.nickname?.charAt(0) }}
          </el-avatar>
          <div class="comment-input-wrapper">
            <el-input
              v-model="commentContent"
              type="textarea"
              :rows="2"
              placeholder="有什么想法？"
              resize="none"
              @keyup.enter.ctrl="submitComment"
            />
            <div class="comment-input-footer">
              <el-button type="primary" size="small" :loading="submitting" @click="submitComment">
                回复
              </el-button>
            </div>
          </div>
        </div>
        
        <div class="comment-list" v-if="comments.length > 0">
          <TransitionGroup name="list">
            <CommentItem 
              v-for="comment in comments" 
              :key="comment.id" 
              :comment="comment" 
              :post-id="post.id"
              :is-new="newCommentId === comment.id"
              @reply-success="handleReplySuccess"
              @delete-success="loadComments"
            />
          </TransitionGroup>
        </div>
        <el-empty v-else description="还没有回复，快来抢沙发！" :image-size="80" />
        
        <div class="load-more" v-if="hasMoreComments">
          <el-button link @click="loadMoreComments" :loading="loadingMoreComments">查看更多回复</el-button>
        </div>
      </div>
    </div>

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
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Star, StarFilled, Collection, CollectionTag, Share, MoreFilled, ArrowLeft, ArrowRight, Close } from '@element-plus/icons-vue'
import { getPost, deletePost } from '@/api/post'
import { listComments, createComment } from '@/api/comment'
import { likePost, unlikePost, hasLikedPost } from '@/api/like'
import { favorite, unfavorite, hasFavorited } from '@/api/favorite'
import { useUserStore } from '@/stores/user'
import CommentItem from '@/components/CommentItem.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const postId = Number(route.params.id)
const post = ref(null)
const comments = ref([])
const commentTotal = ref(0)
const loading = ref(false)
const loadingMoreComments = ref(false)
const commentPage = ref(1)
const commentContent = ref('')
const submitting = ref(false)
const liked = ref(false)
const favorited = ref(false)
const newCommentId = ref(null)

const hasMoreComments = computed(() => comments.value.length < commentTotal.value)

// 图片预览相关
const imagePreviewVisible = ref(false)
const previewImages = ref([])
const currentPreviewIndex = ref(0)

// 统一的图片来源：优先使用后端 images 字段，其次 coverUrl，最后从正文 Markdown 中提取
const displayImages = computed(() => {
  if (!post.value) return []

  const rawImages = Array.isArray(post.value.images)
    ? post.value.images
    : post.value.images
      ? [post.value.images]
      : []

  const normalized = rawImages
    .filter((url) => typeof url === 'string' && url.trim())
    .map((url) => url.trim())

  if (normalized.length > 0) {
    return normalized
  }

  const fallback = []

  if (typeof post.value.coverUrl === 'string' && post.value.coverUrl.trim()) {
    fallback.push(post.value.coverUrl.trim())
  }

  if (fallback.length === 0 && typeof post.value.content === 'string') {
    const content = post.value.content
    const imgRegex = /<img[^>]+src=["']([^"']+)["']/gi
    let match
    while ((match = imgRegex.exec(content)) !== null && fallback.length < 9) {
      const url = match[1]
      if (url && typeof url === 'string' && url.trim()) {
        fallback.push(url.trim())
      }
    }
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

// 正文渲染：去掉内联图片，图片由下方微博风格网格统一展示，避免重复和混乱
const renderedContent = computed(() => {
  if (!post.value?.content) return ''
  return post.value.content.replace(/<img[^>]*>/gi, '')
})

const isOwner = computed(() => {
  if (!userStore.isLoggedIn || !post.value) return false
  return post.value.userId === userStore.userInfo?.userId || post.value.authorId === userStore.userInfo?.userId
})

onMounted(async () => {
  loading.value = true
  try {
    await loadPost()
    await Promise.all([loadComments(), checkInteraction()])
  } finally {
    loading.value = false
  }
})

async function loadPost() {
  const res = await getPost(postId)
  post.value = res.data
}

async function loadComments(append = false) {
  const res = await listComments(postId, { page: commentPage.value, pageSize: 20 })
  if (append) {
    comments.value.push(...(res.data?.list || []))
  } else {
    comments.value = res.data?.list || []
  }
  commentTotal.value = res.data?.total || 0
}

async function loadMoreComments() {
  loadingMoreComments.value = true
  commentPage.value++
  await loadComments(true)
  loadingMoreComments.value = false
}

async function checkInteraction() {
  if (!userStore.isLoggedIn) return
  const [l, f] = await Promise.all([hasLikedPost(postId), hasFavorited(postId)])
  liked.value = l.data
  favorited.value = f.data
}

async function submitComment() {
  if (!userStore.isLoggedIn) return router.push('/login')
  if (!commentContent.value.trim()) return ElMessage.warning('评论内容不能为空')
  
  submitting.value = true
  try {
    await createComment({ postId, content: commentContent.value })
    commentContent.value = ''
    ElMessage.success('评论成功')
    
    commentPage.value = 1
    await loadComments()
    if (comments.value.length > 0) {
      newCommentId.value = comments.value[0].id
      setTimeout(() => newCommentId.value = null, 2000)
    }
  } finally {
    submitting.value = false
  }
}

function handleReplySuccess() {
  loadComments()
}

async function handleDelete() {
  await ElMessageBox.confirm('确定要删除帖子吗？', '提示', { type: 'warning' })
  await deletePost(postId)
  router.replace('/')
}

function handleMoreCommand(command) {
  if (command === 'edit') {
    router.push(`/posts/${postId}/edit`)
  } else if (command === 'delete') {
    handleDelete()
  }
}

function formatFullTime(timeStr) {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function handleImagePreview(index) {
  previewImages.value = displayImages.value
  currentPreviewIndex.value = index
  imagePreviewVisible.value = true
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

async function toggleLike() {
  if (!userStore.isLoggedIn) return router.push('/login')
  liked.value = !liked.value
  post.value.likeCount += liked.value ? 1 : -1
  if (liked.value) await likePost(postId)
  else await unlikePost(postId)
}

async function toggleFavorite() {
  if (!userStore.isLoggedIn) return router.push('/login')
  favorited.value = !favorited.value
  post.value.favoriteCount += favorited.value ? 1 : -1
  if (favorited.value) await favorite(postId)
  else await unfavorite(postId)
}
</script>

<style scoped>
.post-detail-container {
  min-height: 100vh;
  background: var(--bg-body);
}

.back-nav {
  max-width: var(--max-content-width);
  margin: 0 auto;
  padding: 12px 16px;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  color: var(--text-secondary);
}

.back-btn:hover {
  color: var(--color-primary);
}

.post-main {
  max-width: var(--max-content-width);
  margin: 16px auto 24px;
  background: var(--bg-card);
  border-radius: var(--radius-md);
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}

/* Author Card - Twitter Style */
.post-author-card {
  padding: 16px;
  border-bottom: 1px solid var(--border-color);
}

.post-header-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.author-header {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  margin-bottom: 12px;
}

.author-avatar {
  border: 1px solid var(--border-color);
}

.author-info {
  display: flex;
  flex-direction: column;
}

.author-name {
  font-weight: 700;
  font-size: 16px;
  color: var(--text-main);
}

.author-handle {
  font-size: 14px;
  color: var(--text-secondary);
}

.post-more {
  flex-shrink: 0;
}

.icon-button {
  border: none;
  background: transparent;
  padding: 4px;
  border-radius: 999px;
  cursor: pointer;
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.2s ease, color 0.2s ease;
}

.icon-button:hover {
  background-color: var(--bg-hover);
  color: var(--text-main);
}

/* Post Content */
.post-content-area {
  padding-left: 68px;
}

.post-title {
  font-size: 20px;
  font-weight: 800;
  color: var(--text-main);
  margin: 0 0 12px 0;
  line-height: 1.3;
}

.post-text {
  font-size: 16px;
  color: var(--text-main);
  line-height: 1.6;
  margin-bottom: 12px;
  word-break: break-word;
}

/* Markdown 内容样式 */
.post-text h1,
.post-text h2,
.post-text h3,
.post-text h4,
.post-text h5,
.post-text h6 {
  margin: 16px 0 8px;
  font-weight: 600;
  line-height: 1.3;
}

.post-text h1 { font-size: 1.5em; }
.post-text h2 { font-size: 1.4em; }
.post-text h3 { font-size: 1.3em; }
.post-text h4 { font-size: 1.2em; }
.post-text h5 { font-size: 1.1em; }
.post-text h6 { font-size: 1em; }

.post-text p {
  margin: 8px 0;
}

.post-text strong,
.post-text b {
  font-weight: 600;
  color: var(--text-main);
}

.post-text em,
.post-text i {
  font-style: italic;
}

.post-text u {
  text-decoration: underline;
}

.post-text s,
.post-text strike,
.post-text del {
  text-decoration: line-through;
}

.post-text blockquote {
  margin: 12px 0;
  padding: 12px 16px;
  border-left: 4px solid #409eff;
  background: #f5f7fa;
  color: #606266;
}

.post-text code {
  padding: 2px 6px;
  background: #f0f0f0;
  border-radius: 4px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 0.9em;
}

.post-text pre {
  margin: 12px 0;
  padding: 16px;
  background: #282c34;
  border-radius: 8px;
  overflow-x: auto;
}

.post-text pre code {
  padding: 0;
  background: transparent;
  color: #abb2bf;
}

.post-text ul,
.post-text ol {
  margin: 8px 0;
  padding-left: 24px;
}

.post-text li {
  margin: 4px 0;
}

.post-text a {
  color: #409eff;
  text-decoration: none;
}

.post-text a:hover {
  text-decoration: underline;
}

.post-text img {
  max-width: 100%;
  border-radius: 8px;
  margin: 8px 0;
}

.post-text hr {
  margin: 16px 0;
  border: none;
  border-top: 1px solid #e4e7ed;
}

.post-text table {
  width: 100%;
  border-collapse: collapse;
  margin: 12px 0;
}

.post-text th,
.post-text td {
  padding: 8px 12px;
  border: 1px solid #e4e7ed;
  text-align: left;
}

.post-text th {
  background: #f5f7fa;
  font-weight: 600;
}

/* Post Images（缩略图 Twitter 风格，与列表保持一致） */
.post-images {
  display: grid;
  margin-top: 12px;
  width: 100%;
  border-radius: 12px;
  overflow: hidden;
  gap: 2px;
  background: #000;
  max-height: 240px;
}

.post-images.weibo-1 {
  grid-template-columns: 1fr;
}

.post-images.weibo-1 .post-image {
  aspect-ratio: 4 / 3;
}

.post-images.weibo-2 {
  grid-template-columns: repeat(2, 1fr);
}

.post-images.weibo-2 .post-image {
  aspect-ratio: 4 / 3;
}

.post-images.weibo-3 {
  grid-template-columns: 2fr 1fr;
  grid-template-rows: repeat(2, 1fr);
}

.post-images.weibo-3 .post-image:first-child {
  grid-row: 1 / 3;
  aspect-ratio: 4 / 3;
}

.post-images.weibo-3 .post-image:not(:first-child) {
  aspect-ratio: 4 / 3;
}

.post-images.weibo-4 {
  grid-template-columns: repeat(2, 1fr);
  grid-auto-rows: 1fr;
}

.post-images.weibo-4 .post-image {
  aspect-ratio: 1;
}

.post-images.weibo-more {
  grid-template-columns: repeat(3, 1fr);
}

.post-images.weibo-more .post-image {
  aspect-ratio: 1;
}

.post-image {
  width: 100%;
  background-size: cover;
  background-position: center;
  background-color: #000;
  cursor: pointer;
  transition: opacity 0.2s;
}

.post-image:hover {
  opacity: 0.9;
}

/* Post Meta */
.post-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 0;
  border-bottom: 1px solid var(--border-color);
  color: var(--text-secondary);
  font-size: 14px;
}

.meta-divider {
  color: var(--border-color);
}

/* Post Stats */
.post-stats {
  display: flex;
  gap: 24px;
  padding: 12px 0;
  border-bottom: 1px solid var(--border-color);
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.stat-value {
  font-weight: 700;
  color: var(--text-main);
}

.stat-label {
  color: var(--text-secondary);
  font-size: 14px;
}

/* Comments Section */
.comments-section {
  padding: 16px;
}

.comments-header {
  margin-bottom: 16px;
}

.comments-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-main);
  margin: 0;
}

/* Comment Input */
.comment-input-area {
  display: flex;
  gap: 12px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-color);
  margin-bottom: 16px;
}

.comment-avatar {
  flex-shrink: 0;
}

.comment-input-wrapper {
  flex: 1;
}

.comment-input-wrapper :deep(.el-textarea__inner) {
  border: none;
  background: var(--bg-hover);
  border-radius: 20px;
  padding: 12px 16px;
  font-size: 15px;
}

.comment-input-wrapper :deep(.el-textarea__inner:focus) {
  box-shadow: none;
}

.comment-input-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}

.comment-input-footer .el-button--primary {
  background: var(--color-primary);
  border-color: var(--color-primary);
  border-radius: 20px;
  padding: 8px 20px;
}

/* Comment List */
.comment-list {
  display: flex;
  flex-direction: column;
}

/* Load More */
.load-more {
  text-align: center;
  margin-top: 20px;
}

/* Transitions */
.list-enter-active,
.list-leave-active {
  transition: all 0.4s ease;
}
.list-enter-from,
.list-leave-to {
  opacity: 0;
  transform: translateY(20px);
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

/* Responsive */
@media (max-width: 640px) {
  .post-content-area {
    padding-left: 0;
  }
  
  .author-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  
  .post-stats {
    gap: 16px;
  }
  
  .post-actions {
    justify-content: space-between;
  }
}
</style>
