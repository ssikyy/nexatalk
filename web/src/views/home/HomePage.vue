<template>
  <div class="home-container">
    <!-- 顶部操作区 -->
    <div class="feed-header">
      <!-- Tab 切换 -->
      <div class="tab-switcher">
        <div 
          class="tab-item" 
          :class="{ active: currentTab === 'latest' }"
          @click="switchTab('latest')"
        >
          最新
        </div>
        <div 
          class="tab-item" 
          :class="{ active: currentTab === 'hot' }"
          @click="switchTab('hot')"
        >
          热门
        </div>
        <div 
          v-if="userStore.isLoggedIn"
          class="tab-item" 
          :class="{ active: currentTab === 'follow' }"
          @click="switchTab('follow')"
        >
          关注
        </div>
      </div>

      <!-- 排序和分区选择 -->
      <div class="header-actions">
        <!-- 时间范围选择 -->
        <el-dropdown trigger="click" @command="handleTimeRangeChange" v-if="currentTab === 'hot'">
          <div class="sort-btn">
            <span>{{ timeRangeLabels[currentTimeRange] }}</span>
            <el-icon class="arrow"><ArrowDown /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="day">📅 24小时内</el-dropdown-item>
              <el-dropdown-item command="week">📅 7天内</el-dropdown-item>
              <el-dropdown-item command="month">📅 30天内</el-dropdown-item>
              <el-dropdown-item command="all">📅 全部</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

        <!-- 排序下拉 -->
        <el-dropdown trigger="click" @command="handleSortChange" v-if="currentTab === 'hot'">
          <div class="sort-btn">
            <el-icon><Sort /></el-icon>
            <span>{{ sortLabels[currentSort] }}</span>
            <el-icon class="arrow"><ArrowDown /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="hot">🔥 热门</el-dropdown-item>
              <el-dropdown-item command="like">👍 点赞最多</el-dropdown-item>
              <el-dropdown-item command="comment">💬 评论最多</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

        <!-- 分区选择器 -->
        <el-dropdown trigger="click" @command="handleSectionChange" v-if="currentTab !== 'follow'">
          <div class="section-btn">
            <el-icon><Folder /></el-icon>
            <span>{{ currentSectionName }}</span>
            <el-icon class="arrow"><ArrowDown /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item :command="null">全部分区</el-dropdown-item>
              <el-dropdown-item 
                v-for="section in sections" 
                :key="section.id" 
                :command="section.id"
              >
                {{ section.name }}
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <!-- 帖子列表 -->
    <div 
      class="feed-list" 
      ref="feedListRef"
      v-loading="loading"
      element-loading-background="rgba(245, 247, 250, 0.8)"
    >
      <!-- 骨架屏 -->
      <template v-if="loading && posts.length === 0">
        <PostCardSkeleton v-for="i in 3" :key="i" />
      </template>

      <!-- 帖子列表 -->
      <TransitionGroup name="list" tag="div" class="posts-wrapper">
        <PostCard 
          v-for="post in filteredPosts" 
          :key="post.id" 
          :post="post" 
          @block="handleBlock"
        />
      </TransitionGroup>

      <!-- 空状态 -->
      <div v-if="!loading && posts.length === 0" class="empty-state">
        <div class="empty-illustration">
          <el-icon :size="80" color="#dcdfe6"><DocumentDelete /></el-icon>
        </div>
        <p class="empty-text">{{ emptyText }}</p>
        <el-button type="primary" round @click="$router.push('/posts/create')">立即发布</el-button>
      </div>
      
      <!-- 加载更多 / 无限滚动触发器 -->
      <div v-if="posts.length > 0" class="load-more" ref="loadMoreRef">
        <template v-if="loadingMore">
          <div class="loading-indicator">
            <el-icon class="is-loading"><Loading /></el-icon>
            <span>加载中...</span>
          </div>
        </template>
        <template v-else-if="!hasMore && posts.length > 0">
          <div class="no-more">没有更多了</div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, provide, computed, nextTick, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { getLatestFeed, getFollowFeed, getHotFeed } from '@/api/feed'
import { listSections } from '@/api/section'
import PostCard from '@/components/PostCard.vue'
import PostCardSkeleton from '@/components/PostCardSkeleton.vue'
import { Sort, ArrowDown, Folder, DocumentDelete, Loading } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const currentTab = ref('latest')
const posts = ref([])
const sections = ref([])
const loading = ref(false)
const loadingMore = ref(false)
const currentPage = ref(1)
const hasMore = ref(true)
const currentSectionId = ref(null)
const currentSort = ref('hot') // hot, like, comment
const currentTimeRange = ref('week') // day, week, month, all

// DOM 引用
const feedListRef = ref(null)
const loadMoreRef = ref(null)

// 排序标签映射
const sortLabels = {
  hot: '热门',
  like: '点赞最多',
  comment: '评论最多'
}

// 时间范围标签映射
const timeRangeLabels = {
  day: '24小时内',
  week: '7天内',
  month: '30天内',
  all: '全部'
}

// 当前分区名称
const currentSectionName = computed(() => {
  if (!currentSectionId.value) return '全部分区'
  const section = sections.value.find(s => s.id === currentSectionId.value)
  return section?.name || '全部分区'
})

// 空状态文案
const emptyText = computed(() => {
  if (currentTab.value === 'follow') return '关注一些用户吧'
  if (currentTab.value === 'hot') return '暂无热门内容'
  return '暂无动态，去发布一条吧！'
})

// 存储被屏蔽的用户ID列表
const blockedUserIds = ref(new Set())

// 无限滚动 observer
let observer = null

// 过滤掉被屏蔽用户的帖子
const filteredPosts = computed(() => {
  if (blockedUserIds.value.size === 0) {
    return posts.value
  }
  return posts.value.filter(post => {
    const authorId = post.userId || post.authorId
    return !blockedUserIds.value.has(authorId)
  })
})

// 提供给子组件使用的刷新方法
provide('refreshBlockedUsers', () => {
  // 刷新被屏蔽用户列表
  blockedUserIds.value = new Set([...blockedUserIds.value])
})

// 处理屏蔽事件 - 将被屏蔽用户加入列表，computed 会自动过滤
const handleBlock = (blockedUserId) => {
  blockedUserIds.value.add(blockedUserId)
}

// 加载分区列表
const loadSections = async () => {
  try {
    const res = await listSections()
    sections.value = res.data?.list || res.data || []
  } catch (error) {
    console.error('Failed to load sections:', error)
  }
}

// 初始化
onMounted(async () => {
  await loadSections()
  
  // 从 URL 参数获取 sectionId
  currentSectionId.value = route.query.sectionId ? Number(route.query.sectionId) : null
  fetchPosts(true)
  
  // 设置无限滚动 observer
  setupInfiniteScroll()
})

// 设置无限滚动
const setupInfiniteScroll = () => {
  if (!loadMoreRef.value) return
  
  observer = new IntersectionObserver(
    (entries) => {
      const entry = entries[0]
      if (entry.isIntersecting && !loadingMore.value && hasMore.value && !loading.value) {
        loadMore()
      }
    },
    { threshold: 0.1 }
  )
  
  observer.observe(loadMoreRef.value)
}

onUnmounted(() => {
  if (observer) {
    observer.disconnect()
  }
})

// 监听路由参数变化（支持分区切换）
watch(() => route.query.sectionId, (newSectionId) => {
  currentSectionId.value = newSectionId ? Number(newSectionId) : null
  currentPage.value = 1
  posts.value = []
  fetchPosts(true)
})

// 切换 Tab
const switchTab = (tab) => {
  if (currentTab.value === tab) return

  // 未登录用户不能切换到关注tab
  if (tab === 'follow' && !userStore.isLoggedIn) {
    ElMessage.warning('请先登录查看关注动态')
    router.push('/login')
    return
  }

  currentTab.value = tab
  currentPage.value = 1
  posts.value = []

  // 切换到关注 tab 时清除分区筛选，同时更新 URL
  if (tab === 'follow') {
    currentSectionId.value = null
    router.replace({ name: 'Home' })
  } else {
    // 切换回最新 tab 时，恢复之前的分区筛选状态
    currentSectionId.value = route.query.sectionId ? Number(route.query.sectionId) : null
  }

  fetchPosts(true)
}

// 处理排序变化
const handleSortChange = (sort) => {
  if (currentSort.value === sort) return
  currentSort.value = sort
  currentPage.value = 1
  posts.value = []
  fetchPosts(true)
}

// 处理时间范围变化
const handleTimeRangeChange = (timeRange) => {
  if (currentTimeRange.value === timeRange) return
  currentTimeRange.value = timeRange
  currentPage.value = 1
  posts.value = []
  fetchPosts(true)
}

// 处理分区变化
const handleSectionChange = (sectionId) => {
  currentSectionId.value = sectionId
  currentPage.value = 1
  posts.value = []
  
  // 更新 URL 参数
  if (sectionId) {
    router.replace({ query: { ...route.query, sectionId } })
  } else {
    const newQuery = { ...route.query }
    delete newQuery.sectionId
    router.replace({ query: newQuery })
  }
  
  fetchPosts(true)
}

// 获取数据
const fetchPosts = async (isRefresh = false) => {
  if (isRefresh) loading.value = true
  else loadingMore.value = true

  try {
    let api
    let params = {
      page: currentPage.value,
      pageSize: 20
    }
    
    // 根据当前 tab 选择 API
    if (currentTab.value === 'follow') {
      // 未登录用户不能访问关注流
      if (!userStore.isLoggedIn) {
        ElMessage.warning('请先登录')
        currentTab.value = 'latest'
        return
      }
      api = getFollowFeed
    } else if (currentTab.value === 'hot') {
      api = getHotFeed
      params.sort = currentSort.value
      params.timeRange = currentTimeRange.value
    } else {
      api = getLatestFeed
    }
    
    // 添加分区过滤参数
    if (currentSectionId.value && currentTab.value !== 'follow') {
      params.sectionId = currentSectionId.value
    }
    
    const res = await api(params)
    
    const newPosts = res.data?.list || []
    if (isRefresh) {
      posts.value = newPosts
    } else {
      posts.value = [...posts.value, ...newPosts]
    }
    
    // 判断是否还有更多 (假设后端返回 total)
    const total = res.data?.total || 0
    hasMore.value = posts.value.length < total
  } catch (error) {
    console.error('Failed to fetch posts:', error)
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

const loadMore = () => {
  currentPage.value++
  fetchPosts()
}
</script>

<style scoped>
.home-container {
  max-width: 640px;
  margin: 0 auto;
  min-height: 100vh;
  background: #f5f7fa;
}

.feed-header {
  position: sticky;
  top: 0;
  background: #fff;
  backdrop-filter: blur(10px);
  z-index: 10;
  padding: 16px 20px;
  border-bottom: 1px solid #eee;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02);
}

.tab-switcher {
  display: flex;
  gap: 32px;
}

.tab-item {
  font-size: 17px;
  font-weight: 500;
  color: #86909c;
  cursor: pointer;
  position: relative;
  padding: 8px 0;
  transition: all 0.2s ease;
}

.tab-item:hover {
  color: #1f1f1f;
}

.tab-item.active {
  color: #1f1f1f;
  font-weight: 600;
  font-size: 18px;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 24px;
  height: 4px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 2px;
}

/* 头部操作按钮 */
.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
}

.sort-btn,
.section-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: #f5f7fa;
  border-radius: 16px;
  font-size: 13px;
  color: #4a5568;
  cursor: pointer;
  transition: all 0.2s;
}

.sort-btn:hover,
.section-btn:hover {
  background: #e8eaed;
  color: #1f1f1f;
}

.sort-btn .arrow,
.section-btn .arrow {
  font-size: 12px;
}

.feed-list {
  padding: 16px;
  padding-bottom: 40px;
}

.posts-wrapper {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  background: #fff;
  border-radius: 12px;
  margin: 0 16px;
}

.empty-illustration {
  margin-bottom: 20px;
}

.empty-text {
  color: #86909c;
  font-size: 15px;
  margin-bottom: 20px;
}

.load-more {
  text-align: center;
  padding: 20px 0;
}

.loading-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #86909c;
  font-size: 14px;
}

.loading-indicator .el-icon {
  animation: rotate 1s linear infinite;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.no-more {
  color: #c0c4cc;
  font-size: 13px;
  padding: 10px 0;
}

/* 列表动效 (Vue TransitionGroup) */
.list-enter-active,
.list-leave-active {
  transition: all 0.3s ease;
}
.list-enter-from,
.list-leave-to {
  opacity: 0;
  transform: translateY(10px);
}

@media (max-width: 768px) {
  .feed-list {
    padding: 12px;
    gap: 10px;
  }
  
  .empty-state {
    margin: 0 12px;
    padding: 40px 20px;
  }
}
</style>
