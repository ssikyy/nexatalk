<template>
  <div class="user-posts-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="back-btn" @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
      </div>
      <div class="header-title">
        <h2>{{ user?.nickname }}</h2>
        <span class="header-subtitle">{{ postCount }} 条帖子</span>
      </div>
    </div>

    <!-- 用户基本信息 -->
    <div class="user-info-section" v-if="user">
      <el-avatar :src="user.avatarUrl" :size="64">
        {{ user.nickname?.charAt(0) }}
      </el-avatar>
      <div class="user-info-text">
        <h3>{{ user.nickname }}</h3>
        <p class="user-bio">{{ user.bio || '这个人很懒，什么都没写' }}</p>
        <div class="user-stats">
          <span><strong>{{ followStats.following || 0 }}</strong> 关注</span>
          <span><strong>{{ followStats.followers || 0 }}</strong> 粉丝</span>
        </div>
      </div>
    </div>

    <!-- Tab 切换 -->
    <div class="tab-bar">
      <div 
        class="tab-item active"
      >
        帖子
      </div>
    </div>

    <!-- 帖子列表 -->
    <div class="post-list" v-loading="loading">
      <div v-if="!loading && posts.length === 0" class="empty-tip">
        暂无帖子
      </div>
      <PostCard 
        v-for="post in posts" 
        :key="post.id" 
        :post="post" 
      />
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
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getUserById } from '@/api/user'
import { listPosts, listMyPosts } from '@/api/post'
import { getFollowStats } from '@/api/follow'
import PostCard from '@/components/PostCard.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const userId = computed(() => Number(route.params.id))
const isSelf = computed(() => userStore.userInfo?.userId === userId.value)

const user = ref(null)
const followStats = ref({ following: 0, followers: 0 })
const postCount = ref(0)
const loading = ref(false)
const loadingMore = ref(false)
const posts = ref([])
const currentPage = ref(1)
const pageSize = 10
const total = ref(0)
const hasMore = ref(false)

onMounted(async () => {
  await loadUserInfo()
  loadPosts()
})

watch(() => route.params.id, async () => {
  currentPage.value = 1
  posts.value = []
  await loadUserInfo()
  loadPosts()
})

async function loadUserInfo() {
  try {
    const res = await getUserById(userId.value)
    user.value = res.data
    const statsRes = await getFollowStats(userId.value)
    followStats.value = statsRes.data || { following: 0, followers: 0 }
  } catch (e) {
    console.error('加载用户信息失败', e)
  }
}

async function loadPosts(isLoadMore = false) {
  if (isLoadMore) {
    loadingMore.value = true
  } else {
    loading.value = true
  }

  try {
    // 判断是否是自己的帖子
    const fetchFn = isSelf.value ? listMyPosts : listPosts
    const res = await fetchFn({
      userId: userId.value,
      page: currentPage.value,
      pageSize
    })

    const list = res.data?.list || []
    total.value = res.data?.total || 0
    postCount.value = total.value

    if (isLoadMore) {
      posts.value = [...posts.value, ...list]
    } else {
      posts.value = list
    }

    hasMore.value = posts.value.length < total.value
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

function goBack() {
  router.back()
}
</script>

<style scoped>
.user-posts-page {
  max-width: 640px;
  margin: 0 auto;
  min-height: 100vh;
  background: var(--bg-body);
}

.page-header {
  position: sticky;
  top: 0;
  background: rgba(245, 247, 250, 0.95);
  backdrop-filter: blur(10px);
  z-index: 100;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-color);
}

.back-btn {
  width: 36px;
  height: 36px;
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
  font-size: 20px;
}

.header-title h2 {
  font-size: 18px;
  font-weight: 700;
  margin: 0;
  color: var(--text-main);
}

.header-subtitle {
  font-size: 13px;
  color: var(--text-secondary);
}

.user-info-section {
  display: flex;
  gap: 16px;
  padding: 20px 16px;
  background: #fff;
  border-bottom: 1px solid var(--border-color);
}

.user-info-text h3 {
  font-size: 18px;
  font-weight: 700;
  margin: 0 0 4px;
  color: var(--text-main);
}

.user-bio {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.user-stats {
  display: flex;
  gap: 16px;
  font-size: 14px;
  color: var(--text-secondary);
}

.user-stats strong {
  color: var(--text-main);
}

.tab-bar {
  display: flex;
  background: #fff;
  border-bottom: 1px solid var(--border-color);
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 12px 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-secondary);
  cursor: pointer;
  position: relative;
  transition: color 0.2s;
}

.tab-item.active {
  color: var(--text-main);
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 60px;
  height: 4px;
  background: var(--color-primary);
  border-radius: 2px;
}

.post-list {
  background: #fff;
  min-height: 200px;
}

.empty-tip {
  text-align: center;
  color: var(--text-placeholder);
  padding: 60px 0;
}

.load-more {
  text-align: center;
  padding: 16px;
  background: #fff;
}
</style>
