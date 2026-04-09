<template>
  <div class="comment-item" :class="{ 'highlight': isNew }">
    <el-avatar :src="comment.authorAvatar" :size="40" class="comment-avatar" :style="avatarStyle(comment)">
      {{ comment.authorName?.charAt(0) }}
    </el-avatar>
    
    <div class="comment-body">
      <!-- 头部：用户信息 + 内容 -->
      <div class="comment-header">
        <div class="comment-author">
          <span class="author-name">{{ comment.authorName }}</span>
          <span class="author-meta">
            <span class="time">{{ formatTime(comment.createdAt) }}</span>
          </span>
        </div>
      </div>
      
      <!-- 内容 -->
      <div class="comment-text">{{ comment.content }}</div>
      
      <!-- 操作栏 -->
      <div class="comment-actions">
        <div class="action-item" :class="{ active: liked }" @click="toggleLike">
          <el-icon class="action-icon" :class="{ active: liked }"><component :is="liked ? StarFilled : Star" /></el-icon>
          <span class="action-count">{{ likeCount || 0 }}</span>
        </div>
        <div class="action-item" @click="toggleReplyBox">
          <el-icon class="action-icon"><ChatDotSquare /></el-icon>
          <span class="action-label">回复</span>
        </div>
        <div class="action-item action-item-delete" v-if="canDelete" @click="handleDelete">
          <el-icon class="action-icon"><Delete /></el-icon>
          <span class="action-label">删除</span>
        </div>
      </div>

      <!-- 回复输入框 -->
      <div class="reply-box-wrapper" v-if="replyBoxVisible">
        <transition name="fade">
           <div class="reply-box">
             <el-input 
                v-model="replyContent" 
                placeholder="写下你的回复..." 
                size="small"
                @keyup.enter="submitReply"
             />
             <div class="reply-footer">
               <el-button size="small" link @click="replyBoxVisible = false">取消</el-button>
               <el-button type="primary" size="small" :loading="submitting" @click="submitReply">发送</el-button>
             </div>
           </div>
        </transition>
      </div>

      <!-- 子回复列表 -->
      <div class="replies-list" v-if="hasReplies">
        <div class="reply-item" v-for="reply in comment.replies" :key="reply.id">
          <el-avatar :src="reply.authorAvatar" :size="32" class="reply-avatar" :style="avatarStyle(reply)">{{ reply.authorName?.charAt(0) }}</el-avatar>
          <div class="reply-content-wrapper">
            <div class="reply-header">
              <span class="reply-author">{{ reply.authorName }}</span>
              <span class="reply-time">{{ formatTime(reply.createdAt) }}</span>
            </div>
            <div class="reply-text">
              <span v-if="reply.replyToUser" class="reply-target">@{{ reply.replyToUser }} </span>
              {{ reply.content }}
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useUserStore } from '@/stores/user'
import { createComment, deleteComment } from '@/api/comment'
import { likeComment, unlikeComment } from '@/api/like'
import { ElMessage } from 'element-plus'
import { Star, StarFilled, ChatDotSquare, Delete } from '@element-plus/icons-vue'

const props = defineProps({
  comment: { type: Object, required: true },
  postId: { type: Number, required: true },
  isNew: { type: Boolean, default: false }
})

const emit = defineEmits(['reply-success', 'delete-success'])
const userStore = useUserStore()

const liked = ref(false) // 暂无 isLiked 字段，需后端支持，先模拟
const likeCount = ref(props.comment.likeCount || 0)
const replyBoxVisible = ref(false)
const replyContent = ref('')
const submitting = ref(false)

const hasReplies = computed(() => props.comment.replies && props.comment.replies.length > 0)
const canDelete = computed(() => {
  return userStore.isLoggedIn && (userStore.userInfo?.userId === props.comment.userId || userStore.isAdmin)
})

function formatTime(time) {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = (now - date) / 1000
  if (diff < 60) return '刚刚'
  if (diff < 3600) return `${Math.floor(diff / 60)}分钟前`
  if (diff < 86400) return `${Math.floor(diff / 3600)}小时前`
  return date.toLocaleDateString()
}

/** 无头像时使用渐变背景，根据昵称首字生成稳定色相 */
function avatarStyle(user) {
  if (user?.authorAvatar) return {}
  const char = (user?.authorName || '?').charAt(0)
  const hue = (char.charCodeAt(0) * 137) % 360
  return {
    background: `linear-gradient(135deg, hsl(${hue}, 65%, 55%), hsl(${hue}, 55%, 45%))`,
    color: '#fff',
    fontWeight: '600'
  }
}

function toggleReplyBox() {
  if (!userStore.isLoggedIn) return ElMessage.warning('请先登录')
  replyBoxVisible.value = !replyBoxVisible.value
}

async function toggleLike() {
  if (!userStore.isLoggedIn) return ElMessage.warning('请先登录')
  // 乐观更新
  liked.value = !liked.value
  likeCount.value += liked.value ? 1 : -1
  try {
    if (liked.value) await likeComment(props.comment.id)
    else await unlikeComment(props.comment.id)
  } catch (e) {
    liked.value = !liked.value
    likeCount.value += liked.value ? 1 : -1
  }
}

async function submitReply() {
  if (!replyContent.value.trim()) return
  submitting.value = true
  try {
    const res = await createComment({
      postId: props.postId,
      parentId: props.comment.id,
      content: replyContent.value
    })
    
    // 模拟添加回复到列表 (或由父组件刷新)
    // 这里简单处理：清空并提示，通知父组件刷新
    replyContent.value = ''
    replyBoxVisible.value = false
    ElMessage.success('回复成功')
    emit('reply-success')
  } catch (e) {
    console.error(e)
  } finally {
    submitting.value = false
  }
}

async function handleDelete() {
  try {
    await deleteComment(props.comment.id)
    emit('delete-success')
  } catch (e) {
    ElMessage.error('删除失败')
  }
}
</script>

<style scoped>
.comment-item {
  display: flex;
  gap: 14px;
  padding: 14px 18px;
  border-bottom: 1px solid var(--border-color, #f0f0f0);
  transition: background-color 0.2s ease;
}

.comment-item:hover {
  background: var(--bg-hover, #fafafa);
}

.comment-item.highlight {
  animation: flash 1s ease-out;
}

@keyframes flash {
  0% { background-color: rgba(59, 130, 246, 0.08); }
  100% { background-color: transparent; }
}

.comment-avatar {
  flex-shrink: 0;
  cursor: pointer;
  font-size: 16px;
}

.comment-body {
  flex: 1;
  min-width: 0;
}

.comment-header {
  margin-bottom: 6px;
}

.comment-author {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.author-name {
  font-weight: 600;
  font-size: 15px;
  color: var(--text-main, #1f2937);
}

.author-meta {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.time {
  font-size: 13px;
  color: var(--text-secondary, #6b7280);
}

.comment-text {
  font-size: 15px;
  color: var(--text-main, #374151);
  line-height: 1.6;
  margin-bottom: 10px;
  word-break: break-word;
}

.comment-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 2px;
}

.action-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  margin: 0 -4px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
}

.action-item:hover {
  background: rgba(59, 130, 246, 0.06);
  color: var(--color-primary, #409eff);
}

.action-icon {
  font-size: 15px;
  color: var(--text-secondary, #6b7280);
  transition: color 0.2s;
}

.action-item .action-icon.active {
  color: var(--color-primary, #409eff);
}

.action-item-delete:hover {
  background: rgba(239, 68, 68, 0.08);
  color: #ef4444;
}

.action-item-delete:hover .action-icon,
.action-item-delete:hover .action-label {
  color: #ef4444;
}

.action-count,
.action-label {
  font-size: 13px;
  color: var(--text-secondary, #6b7280);
}

.action-item.active .action-count,
.action-item:hover .action-label {
  color: var(--color-primary, #409eff);
}

/* Reply Box */
.reply-box-wrapper {
  margin-top: 12px;
  margin-bottom: 4px;
}

.reply-box {
  background: var(--bg-hover, #f5f5f5);
  border-radius: 10px;
  padding: 12px;
  border: 1px solid var(--border-color, #eee);
}

.reply-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
  gap: 8px;
}

/* Nested Replies */
.replies-list {
  margin-top: 12px;
  padding: 10px 12px;
  background: var(--bg-hover, #fafafa);
  border-radius: 10px;
  border-left: 3px solid var(--color-primary, #409eff);
}

.reply-item {
  display: flex;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.04);
}

.reply-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.reply-item:first-child {
  padding-top: 0;
}

.reply-avatar {
  flex-shrink: 0;
  cursor: pointer;
  font-size: 14px;
}

.reply-content-wrapper {
  flex: 1;
  min-width: 0;
}

.reply-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
  flex-wrap: wrap;
}

.reply-author {
  font-weight: 600;
  font-size: 14px;
  color: var(--text-main, #1f2937);
}

.reply-time {
  font-size: 12px;
  color: var(--text-secondary, #9ca3af);
}

.reply-text {
  font-size: 14px;
  color: var(--text-main, #374151);
  line-height: 1.5;
  word-break: break-word;
}

.reply-target {
  color: var(--color-primary, #409eff);
  font-weight: 500;
}

/* Fade transition */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
