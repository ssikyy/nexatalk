<template>
  <div class="profile-page" v-loading="pageLoading">
    <template v-if="user">
      <!-- 封面图区域 -->
      <div class="profile-banner">
        <div class="banner-image" v-if="user.bannerUrl" :style="{ backgroundImage: `url(${user.bannerUrl})` }"></div>
        <div class="banner-placeholder" v-else>
          <div class="banner-gradient"></div>
        </div>
      </div>

      <!-- 用户信息区域 -->
      <div class="profile-info">
        <!-- 头像 -->
        <div class="avatar-wrapper">
          <el-avatar :size="134" :src="user.avatarUrl" class="profile-avatar">
            {{ user.nickname?.charAt(0) }}
          </el-avatar>
        </div>
      </div>

      <!-- 用户详情 + 操作按钮 -->
      <div class="profile-details">
        <div class="details-header">
          <div class="user-names">
            <h1 class="display-name">{{ user.nickname }}</h1>
            <p class="username">@{{ user.username }}</p>
          </div>

          <!-- 操作按钮：放到封面图下方的右上角，不遮挡背景 -->
          <div class="profile-actions" v-if="isSelf">
            <el-button @click="$router.push('/edit-profile')" class="edit-btn">编辑资料</el-button>
          </div>
          <div class="profile-actions" v-else-if="userStore.isLoggedIn">
            <el-button
              :class="following ? 'following-btn' : 'follow-btn'"
              @click="toggleFollow"
            >
              {{ following ? '已关注' : '关注' }}
            </el-button>
            <el-button class="message-btn" @click="$router.push(`/messages?userId=${userId}`)">
              <el-icon><Message /></el-icon>
            </el-button>
          </div>
        </div>
        <p class="bio" v-if="user.bio">{{ user.bio }}</p>

        <!-- 关注信息 -->
        <div class="user-meta">
          <div class="meta-item" @click="showFollowing">
            <span class="meta-value">{{ followStats.following || 0 }}</span>
            <span class="meta-label">关注</span>
          </div>
          <div class="meta-item" @click="showFollowers">
            <span class="meta-value">{{ followStats.followers || 0 }}</span>
            <span class="meta-label">粉丝</span>
          </div>
        </div>

        <p class="join-date">
          <el-icon><Calendar /></el-icon>
          加入于 {{ formatDate(user.createTime) }}
        </p>
      </div>

      <!-- 标签页导航 -->
      <div class="profile-tabs">
        <div
          v-for="tab in tabs"
          :key="tab.key"
          :class="['tab-item', { active: activeTab === tab.key }]"
          @click="activeTab = tab.key"
        >
          {{ tab.label }}
        </div>
      </div>

      <!-- 内容区域 -->
      <div class="profile-content">
        <div v-if="activeTab === 'posts'" v-loading="postsLoading" class="post-list">
          <PostCard
            v-for="post in posts"
            :key="post.id"
            :post="post"
            @click="$router.push(`/posts/${post.id}`)"
          />
          <el-empty v-if="!postsLoading && posts.length === 0" description="暂无帖子" />
        </div>
        <div v-else-if="activeTab === 'likes'" v-loading="likesLoading" class="post-list">
          <PostCard
            v-for="post in likes"
            :key="post.id"
            :post="post"
            @click="$router.push(`/posts/${post.id}`)"
          />
          <el-empty v-if="!likesLoading && likes.length === 0" description="暂无点赞" />
        </div>
        <div v-else-if="activeTab === 'favorites'" v-loading="favoritesLoading" class="post-list">
          <PostCard
            v-for="post in favorites"
            :key="post.id"
            :post="post"
            @click="$router.push(`/posts/${post.id}`)"
          />
          <el-empty v-if="!favoritesLoading && favorites.length === 0" description="暂无收藏" />
        </div>
      </div>

      <el-pagination
        v-if="total > pageSize && activeTab === 'posts'"
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        background
        class="pagination"
        @current-change="loadPosts"
      />
      <el-pagination
        v-if="likesTotal > pageSize && activeTab === 'likes'"
        v-model:current-page="likesPage"
        :page-size="pageSize"
        :total="likesTotal"
        layout="prev, pager, next"
        background
        class="pagination"
        @current-change="loadLikes"
      />
      <el-pagination
        v-if="favoritesTotal > pageSize && activeTab === 'favorites'"
        v-model:current-page="favoritesPage"
        :page-size="pageSize"
        :total="favoritesTotal"
        layout="prev, pager, next"
        background
        class="pagination"
        @current-change="loadFavorites"
      />
    </template>

    <el-empty v-else-if="!pageLoading" description="用户不存在" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Message, Calendar, Star, StarFilled } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getUserById } from '@/api/user'
import { listPosts } from '@/api/post'
import { follow, unfollow, isFollowing, getFollowStats } from '@/api/follow'
import { listMyLikes } from '@/api/like'
import { listMyFavorites } from '@/api/favorite'
import PostCard from '@/components/PostCard.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 当前查看的用户 ID，如果路由上没有 id（例如跳转到其它页面时），则为 null
const userId = computed(() => {
  const raw = route.params.id
  const n = Number(raw)
  return Number.isNaN(n) ? null : n
})
const isSelf = computed(() => userStore.userInfo?.userId === userId.value)

const user = ref(null)
const pageLoading = ref(false)
const following = ref(false)
const followStats = ref({ following: 0, followers: 0 })

const activeTab = ref('posts')
const tabs = [
  { key: 'posts', label: '帖子' },
  { key: 'likes', label: '点赞' },
  { key: 'favorites', label: '收藏' }
]

// 帖子（该用户发的）
const posts = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = 20
const postsLoading = ref(false)

// 我的点赞帖子
const likes = ref([])
const likesTotal = ref(0)
const likesPage = ref(1)
const likesLoading = ref(false)

// 我的收藏帖子
const favorites = ref([])
const favoritesTotal = ref(0)
const favoritesPage = ref(1)
const favoritesLoading = ref(false)

// 路由参数变化时，仅在当前路由是 Profile 页面时重新加载数据，避免 /settings 等页面触发 NaN 请求
watch(
  () => route.params.id,
  async () => {
    if (route.name !== 'Profile') return
    await loadUserData()
  }
)

// tab 切换时，按需加载“点赞/收藏”数据（仅自己可见）
watch(
  () => activeTab.value,
  async (tab) => {
    if (!isSelf.value) return
    if (tab === 'likes') {
      await loadLikes()
    } else if (tab === 'favorites') {
      await loadFavorites()
    }
  }
)

onMounted(async () => {
  await loadUserData()
})

async function loadUserData() {
  // 路由上没有有效的用户 ID 时，不发请求，直接退出
  if (!userId.value) {
    user.value = null
    pageLoading.value = false
    return
  }

  pageLoading.value = true
  try {
    const res = await getUserById(userId.value)
    user.value = res.data
    await Promise.all([loadPosts(), loadFollowInfo()])
  } finally {
    pageLoading.value = false
  }
}

async function loadFollowInfo() {
  const statsRes = await getFollowStats(userId.value)
  followStats.value = statsRes.data
  if (userStore.isLoggedIn && !isSelf.value) {
    const res = await isFollowing(userId.value)
    following.value = res.data
  }
}

async function loadPosts() {
  postsLoading.value = true
  try {
    const res = await listPosts({ userId: userId.value, page: currentPage.value, pageSize })
    posts.value = res.data?.list || []
    total.value = res.data?.total || 0
  } finally {
    postsLoading.value = false
  }
}

// 我的点赞列表（只加载当前登录用户自己的）
async function loadLikes() {
  if (!isSelf.value) return
  likesLoading.value = true
  try {
    const res = await listMyLikes({ page: likesPage.value, pageSize })
    likes.value = res.data?.list || []
    likesTotal.value = res.data?.total || 0
  } finally {
    likesLoading.value = false
  }
}

// 我的收藏列表（只加载当前登录用户自己的）
async function loadFavorites() {
  if (!isSelf.value) return
  favoritesLoading.value = true
  try {
    const res = await listMyFavorites({ page: favoritesPage.value, pageSize })
    favorites.value = res.data?.list || []
    favoritesTotal.value = res.data?.total || 0
  } finally {
    favoritesLoading.value = false
  }
}

async function toggleFollow() {
  if (following.value) {
    await unfollow(userId.value)
    following.value = false
    followStats.value.followers--
  } else {
    await follow(userId.value)
    following.value = true
    followStats.value.followers++
  }
}

function showFollowing() {
  router.push({ name: 'UserFollowing', params: { id: userId.value } })
}

function showFollowers() {
  router.push({ name: 'UserFollowers', params: { id: userId.value } })
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getFullYear()}年${date.getMonth() + 1}月`
}
</script>

<style scoped>
.profile-page {
  min-height: 100vh;
  background: #f7f9f9;
}

/* 封面图 */
.profile-banner {
  position: relative;
  height: 200px;
  background: #cfd9df;
}

.banner-image {
  width: 100%;
  height: 100%;
  background-size: cover;
  background-position: center;
}

.banner-placeholder {
  width: 100%;
  height: 100%;
}

.banner-gradient {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 50%, #f093fb 100%);
}

/* 用户信息 */
.profile-info {
  position: relative;
  padding: 0 16px;
  margin-top: -67px;
}

.avatar-wrapper {
  display: inline-block;
  padding: 4px;
  background: #f7f9f9;
  border-radius: 50%;
}

.profile-avatar {
  border: 4px solid #f7f9f9;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  font-size: 48px;
  font-weight: 600;
  color: #fff;
}

.details-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.profile-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.edit-btn {
  border-radius: 20px;
  padding: 8px 20px;
  font-weight: 600;
  border: 1px solid #cfd9df;
  background: #fff;
  color: #0f1419;
}

.edit-btn:hover {
  background: #f7f9f9;
  border-color: #cfd9df;
}

.follow-btn {
  border-radius: 20px;
  padding: 8px 20px;
  font-weight: 600;
  background: #0f1419;
  border: none;
  color: #fff;
}

.follow-btn:hover {
  background: #333;
}

.following-btn {
  border-radius: 20px;
  padding: 8px 20px;
  font-weight: 600;
  background: #fff;
  border: 1px solid #cfd9df;
  color: #0f1419;
}

.following-btn:hover {
  border-color: #f4212e;
  color: #f4212e;
}

.message-btn {
  border-radius: 50%;
  width: 40px;
  height: 40px;
  padding: 0;
  border: 1px solid #cfd9df;
  background: #fff;
}

/* 用户详情 */
.profile-details {
  padding: 12px 16px 16px;
}

.display-name {
  font-size: 22px;
  font-weight: 800;
  color: #0f1419;
  margin: 0 0 4px;
}

.username {
  font-size: 15px;
  color: #536471;
  margin: 0 0 12px;
}

.bio {
  font-size: 15px;
  color: #0f1419;
  margin: 0 0 16px;
  line-height: 1.5;
}

.user-meta {
  display: flex;
  gap: 24px;
  margin-bottom: 12px;
}

.meta-item {
  display: flex;
  gap: 4px;
  cursor: pointer;
}

.meta-item:hover .meta-value {
  text-decoration: underline;
}

.meta-value {
  font-weight: 700;
  color: #0f1419;
}

.meta-label {
  color: #536471;
}

.join-date {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #536471;
  margin: 0;
}

/* 标签页 */
.profile-tabs {
  display: flex;
  border-bottom: 1px solid #eff3f4;
  background: #fff;
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 16px 0;
  font-size: 15px;
  font-weight: 500;
  color: #536471;
  cursor: pointer;
  position: relative;
  transition: background 0.2s;
}

.tab-item:hover {
  background: #f7f9f9;
}

.tab-item.active {
  color: #0f1419;
  font-weight: 700;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 56px;
  height: 4px;
  background: #1d9bf0;
  border-radius: 2px;
}

/* 内容区域 */
.profile-content {
  background: #fff;
  min-height: 200px;
}

.post-list {
  display: flex;
  flex-direction: column;
}

.empty-tab {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  color: #536471;
}

.empty-tab p {
  margin: 12px 0 0;
  font-size: 15px;
}

.pagination {
  margin-top: 16px;
  justify-content: center;
  padding: 16px;
  background: #fff;
}
</style>
