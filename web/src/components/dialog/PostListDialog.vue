<template>
  <el-dialog
    v-model="dialogVisible"
    :title="title"
    width="600px"
    :close-on-click-modal="false"
    class="post-list-dialog"
  >
    <div class="post-list-container" v-loading="loading">
      <div v-if="!loading && postList.length === 0" class="empty-tip">
        暂无帖子
      </div>
      <div
        v-for="post in postList"
        :key="post.id"
        class="post-item"
        @click="handlePostClick(post)"
      >
        <div class="post-header">
          <el-avatar :src="post.author?.avatarUrl" :size="36">
            {{ post.author?.nickname?.charAt(0) }}
          </el-avatar>
          <div class="post-author-info">
            <span class="author-name">{{ post.author?.nickname }}</span>
            <span class="post-time">{{ formatTime(post.createdAt) }}</span>
          </div>
        </div>
        <div class="post-content">
          {{ post.content }}
        </div>
        <div class="post-stats">
          <span><el-icon><View /></el-icon> {{ post.viewCount || 0 }}</span>
          <span><el-icon><Star /></el-icon> {{ post.likeCount || 0 }}</span>
          <span><el-icon><ChatDotRound /></el-icon> {{ post.commentCount || 0 }}</span>
        </div>
      </div>
    </div>

    <template #footer>
      <el-pagination
        v-if="total > pageSize"
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        small
        @current-change="loadPosts"
      />
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { useRouter } from 'vue-router'
import { View, Star, ChatDotRound } from '@element-plus/icons-vue'
import { listMyPosts, listPosts } from '@/api/post'
import { useUserStore } from '@/stores/user'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  userId: {
    type: Number,
    required: true
  },
  title: {
    type: String,
    default: '帖子列表'
  }
})

const emit = defineEmits(['update:visible'])

const router = useRouter()
const userStore = useUserStore()

const dialogVisible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

const loading = ref(false)
const postList = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = 10

watch(() => props.visible, (val) => {
  if (val) {
    currentPage.value = 1
    loadPosts()
  }
})

async function loadPosts() {
  loading.value = true
  try {
    // 判断是否是自己的帖子
    const isSelf = props.userId === userStore.userInfo?.userId
    const fetchFn = isSelf ? listMyPosts : listPosts

    const res = await fetchFn({
      userId: props.userId,
      page: currentPage.value,
      pageSize
    })

    postList.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch (e) {
    console.error('加载帖子列表失败', e)
  } finally {
    loading.value = false
  }
}

function handlePostClick(post) {
  dialogVisible.value = false
  router.push(`/posts/${post.id}`)
}

function formatTime(time) {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))

  if (days === 0) {
    const hours = Math.floor(diff / (1000 * 60 * 60))
    if (hours === 0) {
      const minutes = Math.floor(diff / (1000 * 60))
      return minutes <= 1 ? '刚刚' : `${minutes}分钟前`
    }
    return `${hours}小时前`
  } else if (days === 1) {
    return '昨天'
  } else if (days < 7) {
    return `${days}天前`
  } else {
    return `${date.getMonth() + 1}-${date.getDate()}`
  }
}
</script>

<style scoped>
.post-list-container {
  max-height: 500px;
  overflow-y: auto;
}

.empty-tip {
  text-align: center;
  color: var(--text-placeholder, #909399);
  padding: 40px 0;
}

.post-item {
  padding: 16px;
  border-bottom: 1px solid var(--border-color, #ebeef5);
  cursor: pointer;
  transition: background-color 0.2s;
}

.post-item:last-child {
  border-bottom: none;
}

.post-item:hover {
  background-color: var(--bg-hover, #f5f7fa);
}

.post-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.post-author-info {
  display: flex;
  flex-direction: column;
}

.author-name {
  font-weight: 600;
  font-size: 14px;
  color: var(--text-main, #303133);
}

.post-time {
  font-size: 12px;
  color: var(--text-placeholder, #909399);
}

.post-content {
  font-size: 14px;
  color: var(--text-main, #303133);
  line-height: 1.5;
  margin-bottom: 10px;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.post-stats {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: var(--text-secondary, #909399);
}

.post-stats span {
  display: flex;
  align-items: center;
  gap: 4px;
}

:deep(.el-dialog__footer) {
  text-align: center;
}
</style>
