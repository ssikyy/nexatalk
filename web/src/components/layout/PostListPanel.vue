<template>
  <div class="post-list-panel">
    <!-- 返回按钮和标题 -->
    <div class="panel-header">
      <div class="back-btn" @click="$emit('back')">
        <el-icon><ArrowLeft /></el-icon>
      </div>
      <h3 class="panel-title">{{ title }}</h3>
    </div>

    <!-- 帖子列表 -->
    <div class="post-list" v-loading="loading">
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
          <el-avatar :src="post.author?.avatarUrl" :size="40">
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
        <div class="post-media" v-if="post.mediaUrls?.length">
          <el-image
            v-for="(url, index) in post.mediaUrls.slice(0, 4)"
            :key="index"
            :src="url"
            :preview-src-list="post.mediaUrls"
            :initial-index="index"
            fit="cover"
            class="media-image"
          />
        </div>
        <div class="post-stats">
          <span><el-icon><View /></el-icon> {{ post.viewCount || 0 }}</span>
          <span><el-icon><Star /></el-icon> {{ post.likeCount || 0 }}</span>
          <span><el-icon><ChatDotRound /></el-icon> {{ post.commentCount || 0 }}</span>
        </div>
      </div>
    </div>

    <!-- 加载更多 -->
    <div class="load-more" v-if="hasMore">
      <el-button
        :loading="loadingMore"
        @click="loadMore"
        link
      >
        加载更多
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { View, Star, ChatDotRound, ArrowLeft } from '@element-plus/icons-vue'
import { listMyPosts, listPosts } from '@/api/post'
import { useUserStore } from '@/stores/user'

const props = defineProps({
  userId: {
    type: Number,
    required: true
  },
  title: {
    type: String,
    default: '我的帖子'
  }
})

const emit = defineEmits(['back'])

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const loadingMore = ref(false)
const postList = ref([])
const currentPage = ref(1)
const pageSize = 10
const total = ref(0)

const hasMore = ref(false)

watch(() => props.userId, () => {
  currentPage.value = 1
  loadPosts()
}, { immediate: true })

onMounted(() => {
  loadPosts()
})

async function loadPosts(isLoadMore = false) {
  if (isLoadMore) {
    loadingMore.value = true
  } else {
    loading.value = true
  }

  try {
    // 判断是否是自己的帖子
    const isSelf = props.userId === userStore.userInfo?.userId
    const fetchFn = isSelf ? listMyPosts : listPosts

    const res = await fetchFn({
      userId: props.userId,
      page: currentPage.value,
      pageSize
    })

    const list = res.data?.list || []
    total.value = res.data?.total || 0

    if (isLoadMore) {
      postList.value = [...postList.value, ...list]
    } else {
      postList.value = list
    }

    hasMore.value = postList.value.length < total.value
  } catch (e) {
    console.error('加载帖子列表失败', e)
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

function loadMore() {
  currentPage.value++
  loadPosts(true)
}

function handlePostClick(post) {
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
.post-list-panel {
  display: flex;
  flex-direction: column;
}

.panel-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-color);
  position: sticky;
  top: 0;
  background: var(--bg-card);
  z-index: 10;
}

.back-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background-color 0.2s;
}

.back-btn:hover {
  background-color: var(--bg-hover);
}

.back-btn .el-icon {
  font-size: 18px;
}

.panel-title {
  font-size: 16px;
  font-weight: 700;
  margin: 0;
  color: var(--text-main);
}

.post-list {
  flex: 1;
  overflow-y: auto;
}

.empty-tip {
  text-align: center;
  color: var(--text-placeholder);
  padding: 40px 0;
}

.post-item {
  padding: 16px;
  border-bottom: 1px solid var(--border-color);
  cursor: pointer;
  transition: background-color 0.2s;
}

.post-item:hover {
  background-color: var(--bg-hover);
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
  color: var(--text-main);
}

.post-time {
  font-size: 12px;
  color: var(--text-placeholder);
}

.post-content {
  font-size: 14px;
  color: var(--text-main);
  line-height: 1.5;
  margin-bottom: 10px;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.post-media {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
  margin-bottom: 10px;
  border-radius: 8px;
  overflow: hidden;
}

.media-image {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
  border-radius: 4px;
}

.post-stats {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: var(--text-secondary);
}

.post-stats span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.load-more {
  padding: 16px;
  text-align: center;
  border-top: 1px solid var(--border-color);
}
</style>
