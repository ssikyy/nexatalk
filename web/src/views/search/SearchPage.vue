<template>
  <div class="discover-page">
    <!-- 顶部搜索区 -->
    <div class="search-section">
      <div class="search-wrapper">
        <div class="search-input-box">
          <el-icon class="search-icon"><Search /></el-icon>
          <input
            v-model="keyword"
            type="text"
            placeholder="搜索帖子、用户..."
            class="search-input"
            @keyup.enter="handleSearch"
          />
          <el-icon v-if="keyword" class="clear-icon" @click="keyword = ''; handleSearch()"><Close /></el-icon>
        </div>
        <el-button class="search-btn" type="primary" @click="handleSearch">
          <el-icon><Search /></el-icon>
          <span>搜索</span>
        </el-button>
      </div>
    </div>

    <!-- Tab 切换 -->
    <div class="content-tabs">
      <div
        class="tab-item"
        :class="{ active: currentTab === 'recommend' }"
        @click="switchTab('recommend')"
      >
        <el-icon><Promotion /></el-icon>
        <span>推荐</span>
      </div>
      <div
        class="tab-item"
        :class="{ active: currentTab === 'sections' }"
        @click="switchTab('sections')"
      >
        <el-icon><FolderOpened /></el-icon>
        <span>分区</span>
      </div>
      <div
        class="tab-item"
        :class="{ active: currentTab === 'users' }"
        @click="switchTab('users')"
      >
        <el-icon><User /></el-icon>
        <span>用户</span>
      </div>
      <div
        class="tab-item"
        :class="{ active: currentTab === 'posts' }"
        @click="switchTab('posts')"
      >
        <el-icon><Document /></el-icon>
        <span>帖子</span>
      </div>
    </div>

    <!-- 内容区域 -->
    <div class="content-area" v-loading="loading">
      <!-- 推荐内容 -->
      <template v-if="currentTab === 'recommend'">
        <div class="recommend-section">
          <!-- 横幅轮播 -->
          <div class="banner-carousel" v-if="banners.length > 0">
            <el-carousel height="180px" indicator-position="none" arrow="always">
              <el-carousel-item v-for="banner in banners" :key="banner.id">
                <div
                  class="banner-item"
                  :style="{ backgroundImage: `url(${banner.image})` }"
                  @click="handleBannerClick(banner)"
                >
                  <div class="banner-overlay">
                    <span class="banner-title">{{ banner.title }}</span>
                  </div>
                </div>
              </el-carousel-item>
            </el-carousel>
          </div>

          <!-- 热门推荐 -->
          <div class="hot-posts-section" v-if="hotPosts.length > 0">
            <div class="section-header">
              <h3 class="section-title">
                <el-icon class="title-icon"><Top /></el-icon>
                热门推荐
              </h3>
              <span class="more-link" @click="switchTab('posts')">查看更多</span>
            </div>
            <div class="hot-posts-grid">
              <div
                v-for="post in hotPosts"
                :key="post.id"
                class="hot-post-card"
                @click="$router.push(`/posts/${post.id}`)"
              >
                <div class="hot-post-cover" v-if="post.coverUrl">
                  <img :src="post.coverUrl" :alt="post.title" />
                </div>
                <div class="hot-post-info">
                  <h4 class="hot-post-title">{{ post.title || post.summary || '无标题' }}</h4>
                  <div class="hot-post-meta">
                    <span><el-icon><Star /></el-icon>{{ post.likeCount || 0 }}</span>
                    <span><el-icon><ChatDotSquare /></el-icon>{{ post.commentCount || 0 }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 活跃用户推荐 -->
          <div class="active-users-section" v-if="activeUsers.length > 0">
            <div class="section-header">
              <h3 class="section-title">
                <el-icon class="title-icon"><UserFilled /></el-icon>
                活跃用户
              </h3>
              <span class="more-link" @click="switchTab('users')">查看更多</span>
            </div>
            <div class="active-users-scroll">
              <div
                v-for="user in activeUsers"
                :key="user.id"
                class="active-user-card"
                @click="$router.push(`/user/${user.id}`)"
              >
                <el-avatar :src="user.avatarUrl" :size="56">
                  {{ user.nickname?.charAt(0) }}
                </el-avatar>
                <span class="user-nickname">{{ user.nickname }}</span>
                <span class="user-posts">{{ user.postCount || 0 }} 帖子</span>
              </div>
            </div>
          </div>
        </div>
      </template>

      <!-- 分区浏览 -->
      <template v-else-if="currentTab === 'sections'">
        <div class="sections-grid">
          <div
            v-for="section in sections"
            :key="section.id"
            class="section-card"
            @click="goToSection(section.id)"
          >
            <div class="section-icon" :style="{ backgroundColor: section.color || '#667eea' }">
              <el-icon><Folder /></el-icon>
            </div>
            <div class="section-info">
              <span class="section-name">{{ section.name }}</span>
              <span class="section-desc">{{ section.description || '暂无描述' }}</span>
            </div>
            <el-icon class="section-arrow"><ArrowRight /></el-icon>
          </div>
        </div>
        <el-empty v-if="sections.length === 0 && !loading" description="暂无分区" />
      </template>

      <!-- 用户列表 -->
      <template v-else-if="currentTab === 'users'">
        <div class="user-grid" v-if="users.length > 0">
          <article
            v-for="user in users"
            :key="user.id"
            class="user-card"
            @click="$router.push(`/user/${user.id}`)"
          >
            <div class="user-card-top">
              <el-avatar class="user-avatar" :src="user.avatarUrl" :size="64">
                {{ user.nickname?.charAt(0) }}
              </el-avatar>
              <div class="user-main">
                <div class="user-head">
                  <div class="user-title-group">
                    <span class="nickname">{{ user.nickname }}</span>
                    <span class="username">@{{ user.username }}</span>
                  </div>
                  <el-button
                    v-if="userStore.isLoggedIn && userStore.userInfo?.userId !== user.id"
                    :type="user.isFollowing ? 'default' : 'primary'"
                    :class="['user-card-follow', { 'is-following': user.isFollowing }]"
                    size="small"
                    round
                    @click.stop="handleFollowUser(user)"
                  >
                    {{ user.isFollowing ? '已关注' : '关注' }}
                  </el-button>
                </div>
                <p class="bio">{{ user.bio || '这个人很懒，什么都没写' }}</p>
              </div>
            </div>
            <div class="user-card-divider"></div>
            <div class="user-stats">
              <div class="user-stat">
                <span class="user-stat-value">{{ user.followingCount || 0 }}</span>
                <span class="user-stat-label">关注</span>
              </div>
              <div class="user-stat">
                <span class="user-stat-value">{{ user.followerCount || 0 }}</span>
                <span class="user-stat-label">粉丝</span>
              </div>
              <div class="user-stat">
                <span class="user-stat-value">{{ user.postCount || 0 }}</span>
                <span class="user-stat-label">帖子</span>
              </div>
            </div>
          </article>
        </div>
        <el-empty v-if="users.length === 0 && !loading" description="暂无用户数据" />
      </template>

      <!-- 帖子列表 / 搜索结果 -->
      <template v-else>
        <div class="post-list" v-if="posts.length > 0">
          <PostCard
            v-for="post in posts"
            :key="post.id"
            :post="post"
            @click="$router.push(`/posts/${post.id}`)"
          />
        </div>
        <el-empty v-if="posts.length === 0 && !loading" :description="keyword ? '未找到相关内容' : '暂无帖子'" />

        <!-- 加载更多 -->
        <div v-if="posts.length > 0" class="load-more">
          <template v-if="loadingMore">
            <el-icon class="is-loading"><Loading /></el-icon>
            <span>加载中...</span>
          </template>
          <template v-else-if="!hasMore">
            <span class="no-more">没有更多了</span>
          </template>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { searchPosts, searchUsers } from '@/api/search'
import { listUsers } from '@/api/user'
import { listSections } from '@/api/section'
import { getHotFeed } from '@/api/feed'
import { follow, unfollow } from '@/api/follow'
import PostCard from '@/components/PostCard.vue'
import {
  Search,
  Promotion,
  FolderOpened,
  User,
  Document,
  Top,
  Star,
  ChatDotSquare,
  UserFilled,
  ArrowRight,
  Loading,
  Close
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// 搜索相关
const keyword = ref(route.query.keyword || '')

// Tab 相关
const currentTab = ref('recommend')

// 数据相关
const loading = ref(false)
const loadingMore = ref(false)
const posts = ref([])
const users = ref([])
const sections = ref([])
const hotPosts = ref([])
const activeUsers = ref([])
const currentPage = ref(1)
const hasMore = ref(true)
const hasMoreUsers = ref(true)
const pageSize = 12

// 模拟数据 - 实际项目中应该从后端获取
const banners = ref([])

// 初始化
onMounted(async () => {
  await loadSections()
  if (keyword.value) {
    handleSearch()
  } else {
    loadRecommendData()
  }

  window.addEventListener('scroll', handleScroll)
})

// 监听关键词变化
watch(() => route.query.keyword, (val) => {
  if (val) {
    keyword.value = val
    // 不再强制切换 tab，保持用户当前选择的 tab
    handleSearch()
  }
})

// 加载分区
const loadSections = async () => {
  try {
    const res = await listSections()
    sections.value = res.data?.list || res.data || []
  } catch (error) {
    console.error('Failed to load sections:', error)
  }
}

// 加载推荐数据
const loadRecommendData = async () => {
  loading.value = true
  try {
    // 获取热门帖子
    const hotRes = await getHotFeed({ page: 1, pageSize: 6, sort: 'hot' })
    hotPosts.value = hotRes.data?.list || []

    // 模拟活跃用户 - 实际项目中应该从后端获取
    // 这里暂时使用空数组，因为后端没有提供活跃用户接口
    activeUsers.value = []
  } catch (error) {
    console.error('Failed to load recommend data:', error)
  } finally {
    loading.value = false
  }
}

// 切换 Tab
const switchTab = (tab) => {
  if (currentTab.value === tab) return
  currentTab.value = tab
  currentPage.value = 1
  posts.value = []
  users.value = []
  hasMore.value = true
  hasMoreUsers.value = true

  if (tab === 'recommend') {
    loadRecommendData()
  } else if (tab === 'sections') {
    loadSections()
  } else if (tab === 'users') {
    // 用户tab：有关键词时搜索用户，无关键词时加载全部用户
    if (keyword.value) {
      doSearchUsers()
    } else {
      loadUsers()
    }
  } else if (tab === 'posts') {
    // 帖子tab：有关键词时搜索，无关键词时加载热门帖子
    if (keyword.value) {
      handleSearch()
    } else {
      loadPosts()
    }
  }
}

// 加载帖子列表
const loadPosts = async () => {
  loading.value = true
  try {
    const res = await getHotFeed({ page: currentPage.value, pageSize })
    const newPosts = res.data?.list || []
    posts.value = newPosts
    hasMore.value = newPosts.length >= pageSize
  } catch (error) {
    console.error('Failed to load posts:', error)
  } finally {
    loading.value = false
  }
}

// 加载用户列表
const loadUsers = async () => {
  loading.value = true
  try {
    const res = await listUsers({ page: currentPage.value, pageSize: 20 })
    const newUsers = res.data?.list || []
    users.value = newUsers
    hasMoreUsers.value = newUsers.length >= 20
  } catch (error) {
    console.error('Failed to load users:', error)
  } finally {
    loading.value = false
  }
}

// 加载更多用户
const loadMoreUsers = async () => {
  if (loadingMore.value || !hasMoreUsers.value) return
  currentPage.value++
  loadingMore.value = true

  try {
    let res
    if (keyword.value) {
      // 搜索模式
      res = await searchUsers({ keyword: keyword.value, page: currentPage.value, pageSize: 20 })
    } else {
      // 全部用户模式
      res = await listUsers({ page: currentPage.value, pageSize: 20 })
    }
    const newUsers = res.data?.list || []
    users.value = [...users.value, ...newUsers]
    hasMoreUsers.value = newUsers.length >= 20
  } catch (error) {
    console.error('Load more users failed:', error)
  } finally {
    loadingMore.value = false
  }
}

// 搜索
async function handleSearch() {
  // 关键词为空时，根据当前Tab决定行为
  if (!keyword.value.trim()) {
    router.replace({ query: {} })
    if (currentTab.value === 'posts') {
      // 帖子Tab：加载热门帖子
      loadPosts()
    } else if (currentTab.value === 'users') {
      // 用户Tab：加载全部用户
      loadUsers()
    } else {
      // 其他Tab：切换到推荐
      currentTab.value = 'recommend'
      loadRecommendData()
    }
    return
  }

  // 有关键词时，根据当前Tab决定搜索什么
  currentPage.value = 1
  loading.value = true
  router.replace({ query: { keyword: keyword.value } })

  if (currentTab.value === 'users') {
    // 用户Tab：搜索用户
    await doSearchUsers()
  } else if (currentTab.value === 'posts') {
    // 帖子Tab：搜索帖子
    await doSearchPosts()
  } else {
    // 推荐或其他Tab：默认搜索帖子
    currentTab.value = 'posts'
    await doSearchPosts()
  }

  loading.value = false
}

// 执行搜索帖子
async function doSearchPosts() {
  try {
    const params = { keyword: keyword.value, page: currentPage.value, pageSize }
    const res = await searchPosts(params)
    posts.value = res.data?.list || []
    hasMore.value = posts.value.length >= pageSize
  } catch (error) {
    console.error('Search posts failed:', error)
  }
}

// 执行搜索用户
async function doSearchUsers() {
  try {
    const params = { keyword: keyword.value, page: currentPage.value, pageSize: 20 }
    const res = await searchUsers(params)
    users.value = res.data?.list || []
    hasMoreUsers.value = users.value.length >= 20
  } catch (error) {
    console.error('Search users failed:', error)
  }
}

// 加载更多
const loadMore = () => {
  if (loadingMore.value || !hasMore.value) return
  currentPage.value++
  loadingMore.value = true

  const loadData = async () => {
    try {
      let res
      if (keyword.value) {
        res = await searchPosts({ keyword: keyword.value, page: currentPage.value, pageSize })
      } else {
        res = await getHotFeed({ page: currentPage.value, pageSize })
      }
      const newPosts = res.data?.list || []
      posts.value = [...posts.value, ...newPosts]
      hasMore.value = newPosts.length >= pageSize
    } catch (error) {
      console.error('Load more failed:', error)
    } finally {
      loadingMore.value = false
    }
  }

  loadData()
}

// 无限滚动
const contentAreaRef = ref(null)
const handleScroll = () => {
  if (currentTab.value === 'posts' || currentTab.value === 'users') {
    const scrollHeight = document.documentElement.scrollHeight
    const scrollTop = document.documentElement.scrollTop
    const clientHeight = document.documentElement.clientHeight

    if (scrollTop + clientHeight >= scrollHeight - 100) {
      if (currentTab.value === 'posts') {
        loadMore()
      } else if (currentTab.value === 'users') {
        loadMoreUsers()
      }
    }
  }
}

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})

// 跳转到分区
const goToSection = (sectionId) => {
  router.push({ path: '/', query: { sectionId } })
}

// -banner点击
const handleBannerClick = (banner) => {
  if (banner.link) {
    window.location.href = banner.link
  }
}

// 关注/取消关注用户
const handleFollowUser = async (user) => {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }

  try {
    if (user.isFollowing) {
      await unfollow(user.id)
      user.isFollowing = false
      ElMessage.success('已取消关注')
    } else {
      await follow(user.id)
      user.isFollowing = true
      ElMessage.success('关注成功')
    }
  } catch (error) {
    ElMessage.error('操作失败')
  }
}
</script>

<style scoped>
.discover-page {
  max-width: 960px;
  margin: 0 auto;
  padding: 0 16px 40px;
}

/* 搜索区 */
.search-section {
  background: #fff;
  border-radius: 16px;
  padding: 20px 24px;
  margin-bottom: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.search-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
}

.search-input-box {
  flex: 1;
  display: flex;
  align-items: center;
  background: #f5f7fa;
  border-radius: 24px;
  padding: 6px 16px;
  transition: all 0.3s ease;
  border: 2px solid transparent;
}

.search-input-box:focus-within {
  background: #fff;
  border-color: #667eea;
  box-shadow: 0 2px 12px rgba(102, 126, 234, 0.15);
}

.search-icon {
  color: #909399;
  font-size: 18px;
  margin-right: 10px;
}

.search-input {
  flex: 1;
  border: none;
  background: transparent;
  outline: none;
  font-size: 15px;
  color: #303133;
}

.search-input::placeholder {
  color: #c0c4cc;
}

.clear-icon {
  color: #c0c4cc;
  font-size: 16px;
  cursor: pointer;
  padding: 4px;
  border-radius: 50%;
  transition: all 0.2s;
}

.clear-icon:hover {
  color: #909399;
  background: #f0f2f5;
}

.search-btn {
  height: 42px;
  padding: 0 20px;
  border-radius: 21px;
  font-size: 15px;
  font-weight: 500;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 6px;
}

.search-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

/* Tab 切换 */
.content-tabs {
  display: flex;
  gap: 4px;
  margin-bottom: 20px;
  background: #fff;
  padding: 6px;
  border-radius: 14px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.tab-item {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 16px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  color: #606266;
  cursor: pointer;
  transition: all 0.25s ease;
  position: relative;
  overflow: hidden;
}

.tab-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  opacity: 0;
  transition: opacity 0.25s ease;
  border-radius: 10px;
}

.tab-item:hover {
  color: #303133;
  background: #f5f7fa;
}

.tab-item.active {
  color: #fff;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.tab-item.active::before {
  opacity: 1;
}

.tab-item .el-icon {
  font-size: 17px;
  position: relative;
  z-index: 1;
}

.tab-item span {
  position: relative;
  z-index: 1;
}

/* 内容区域 */
.content-area {
  min-height: 400px;
}

/* 推荐页面 */
.recommend-section {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* 轮播图 */
.banner-carousel {
  border-radius: 16px;
  overflow: hidden;
}

.banner-item {
  width: 100%;
  height: 100%;
  background-size: cover;
  background-position: center;
  cursor: pointer;
  position: relative;
}

.banner-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 20px;
  background: linear-gradient(transparent, rgba(0, 0, 0, 0.7));
}

.banner-title {
  color: #fff;
  font-size: 18px;
  font-weight: 600;
}

/* 区块标题 */
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.title-icon {
  color: #667eea;
}

.more-link {
  font-size: 13px;
  color: #909399;
  cursor: pointer;
  transition: color 0.2s;
}

.more-link:hover {
  color: #667eea;
}

/* 热门帖子卡片 */
.hot-posts-section {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.hot-posts-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.hot-post-card {
  background: linear-gradient(145deg, #fafafa 0%, #f0f2f5 100%);
  border-radius: 14px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid transparent;
}

.hot-post-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.15);
  border-color: rgba(102, 126, 234, 0.2);
}

.hot-post-cover {
  height: 110px;
  overflow: hidden;
  position: relative;
}

.hot-post-cover::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 40px;
  background: linear-gradient(transparent, rgba(0, 0, 0, 0.3));
}

.hot-post-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.hot-post-card:hover .hot-post-cover img {
  transform: scale(1.05);
}

.hot-post-info {
  padding: 14px;
}

.hot-post-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 10px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.5;
}

.hot-post-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #909399;
}

.hot-post-meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 活跃用户 */
.active-users-section {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.active-users-scroll {
  display: flex;
  gap: 16px;
  overflow-x: auto;
  padding-bottom: 8px;
}

.active-user-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px;
  background: #fafafa;
  border-radius: 12px;
  min-width: 100px;
  cursor: pointer;
  transition: all 0.2s;
}

.active-user-card:hover {
  background: #f0f2f5;
}

.user-nickname {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
  text-align: center;
}

.user-posts {
  font-size: 11px;
  color: #909399;
}

/* 分区 */
.sections-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.section-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: #fff;
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  border: 1px solid transparent;
}

.section-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  border-color: rgba(102, 126, 234, 0.2);
}

.section-icon {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 24px;
  flex-shrink: 0;
}

.section-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.section-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.section-desc {
  font-size: 13px;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.section-arrow {
  color: #c0c4cc;
  font-size: 16px;
  transition: transform 0.2s ease;
}

.section-card:hover .section-arrow {
  transform: translateX(4px);
  color: #667eea;
}

/* 用户网格 */
.user-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.user-card {
  display: flex;
  flex-direction: column;
  padding: 20px;
  background: #fff;
  border-radius: 18px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.08);
  border: 1px solid rgba(226, 232, 240, 0.95);
  min-width: 0;
  overflow: hidden;
}

.user-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 16px 34px rgba(79, 112, 255, 0.14);
  border-color: rgba(102, 126, 234, 0.24);
}

.user-card-top {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  width: 100%;
  min-width: 0;
}

.user-avatar {
  flex-shrink: 0;
  background: linear-gradient(135deg, #d1d5db 0%, #bcc5d3 100%);
  color: #fff;
  font-weight: 700;
}

.user-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.user-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  width: 100%;
  min-width: 0;
}

.user-title-group {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.nickname {
  display: block;
  font-size: 18px;
  line-height: 1.2;
  font-weight: 700;
  color: #0f172a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.username {
  display: block;
  font-size: 14px;
  color: #94a3b8;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.bio {
  margin: 0;
  font-size: 14px;
  line-height: 1.6;
  color: #64748b;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.user-card-follow {
  flex-shrink: 0;
  min-width: 78px;
  height: 34px;
  margin: 0;
  padding: 0 14px;
  border: none;
  font-weight: 700;
  letter-spacing: 0.01em;
  box-shadow: 0 10px 18px rgba(59, 130, 246, 0.18);
}

.user-card-follow.el-button--primary {
  background: linear-gradient(135deg, #4f9cf9 0%, #2f8cff 100%);
}

.user-card-follow.is-following {
  box-shadow: none;
}

.user-card-follow.is-following.el-button--default {
  background: #eef2f7;
  border: 1px solid rgba(203, 213, 225, 0.92);
  color: #475569;
}

.user-card-divider {
  width: 100%;
  height: 1px;
  margin: 18px 0 14px;
  background: linear-gradient(90deg, rgba(226, 232, 240, 0.95) 0%, rgba(226, 232, 240, 0.4) 100%);
}

.user-stats {
  width: 100%;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.user-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.user-stat-value {
  font-size: 22px;
  line-height: 1;
  font-weight: 800;
  color: #0f172a;
}

.user-stat-label {
  font-size: 13px;
  color: #94a3b8;
}

/* 帖子列表 */
.post-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 加载更多 */
.load-more {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
  padding: 32px 20px;
  color: #909399;
  font-size: 14px;
}

.load-more .el-icon {
  font-size: 20px;
  animation: rotate 1s linear infinite;
  color: #667eea;
}

.load-more:hover {
  cursor: pointer;
}

.no-more {
  color: #c0c4cc;
  font-size: 13px;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* 响应式 */
@media (max-width: 768px) {
  .hot-posts-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .sections-grid,
  .user-grid {
    grid-template-columns: 1fr;
  }

  .user-card {
    padding: 18px 16px;
  }

  .user-card-top {
    gap: 14px;
  }

  .nickname {
    font-size: 17px;
  }

  .bio {
    font-size: 13px;
  }

  .user-card-follow {
    min-width: 72px;
    height: 32px;
    padding: 0 12px;
  }

  .user-stat-value {
    font-size: 20px;
  }

  .content-tabs {
    overflow-x: auto;
  }

  .tab-item {
    padding: 8px 16px;
    font-size: 14px;
  }
}
</style>
