<template>
  <div class="user-list-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="back-btn" @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
      </div>
      <div class="header-title">
        <h2>{{ user?.nickname }}</h2>
        <span class="header-subtitle">{{ user?.username }}</span>
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
        class="tab-item" 
        :class="{ active: currentTab === 'following' }"
        @click="switchTab('following')"
      >
        关注
      </div>
      <div 
        class="tab-item" 
        :class="{ active: currentTab === 'followers' }"
        @click="switchTab('followers')"
      >
        粉丝
      </div>
    </div>

    <!-- 用户列表 -->
    <div class="user-list" v-loading="loading">
      <div v-if="!loading && userList.length === 0" class="empty-tip">
        暂无{{ currentTab === 'following' ? '关注' : '粉丝' }}
      </div>
      <div
        v-for="userItem in userList"
        :key="userItem.id"
        class="user-item"
        @click="handleUserClick(userItem)"
      >
        <el-avatar :src="userItem.avatarUrl" :size="48">
          {{ userItem.nickname?.charAt(0) }}
        </el-avatar>
        <div class="user-item-info">
          <div class="user-name">{{ userItem.nickname }}</div>
          <div class="user-bio">{{ userItem.bio || '这个人很懒，什么都没写' }}</div>
        </div>
        <div v-if="userItem.isFollowing != null && !userItem.isSelf" class="follow-btn">
          <el-button
            v-if="!userItem.isFollowing"
            type="primary"
            size="small"
            round
            @click.stop="handleFollow(userItem)"
          >
            关注
          </el-button>
          <el-button
            v-else
            size="small"
            round
            @click.stop="handleUnfollow(userItem)"
          >
            已关注
          </el-button>
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
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { getUserById } from '@/api/user'
import { listFollowing, listFollowers, follow, unfollow, getFollowStats } from '@/api/follow'

const router = useRouter()
const route = useRoute()

const userId = computed(() => Number(route.params.id))
const currentTab = ref('following')

const user = ref(null)
const followStats = ref({ following: 0, followers: 0 })
const loading = ref(false)
const loadingMore = ref(false)
const userList = ref([])
const currentPage = ref(1)
const pageSize = 20
const total = ref(0)
const hasMore = ref(false)

onMounted(async () => {
  await loadUserInfo()
  loadUsers()
})

watch(() => route.params.id, async () => {
  currentPage.value = 1
  userList.value = []
  await loadUserInfo()
  loadUsers()
})

watch(currentTab, () => {
  currentPage.value = 1
  userList.value = []
  loadUsers()
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

async function loadUsers(isLoadMore = false) {
  if (isLoadMore) {
    loadingMore.value = true
  } else {
    loading.value = true
  }

  try {
    const fetchFn = currentTab.value === 'following' ? listFollowing : listFollowers
    const res = await fetchFn(userId.value, {
      page: currentPage.value,
      pageSize
    })

    const list = res.data?.list || []
    total.value = res.data?.total || 0

    if (isLoadMore) {
      userList.value = [...userList.value, ...list]
    } else {
      userList.value = list
    }

    hasMore.value = userList.value.length < total.value
  } catch (e) {
    console.error('加载用户列表失败', e)
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

function loadMore() {
  currentPage.value++
  loadUsers(true)
}

function switchTab(tab) {
  if (currentTab.value === tab) return
  currentTab.value = tab
}

function goBack() {
  router.back()
}

function handleUserClick(userItem) {
  router.push(`/user/${userItem.id}`)
}

async function handleFollow(userItem) {
  try {
    await follow(userItem.id)
    userItem.isFollowing = true
    ElMessage.success('关注成功')
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

async function handleUnfollow(userItem) {
  try {
    await unfollow(userItem.id)
    userItem.isFollowing = false
    ElMessage.success('已取消关注')
  } catch (e) {
    ElMessage.error('操作失败')
  }
}
</script>

<style scoped>
.user-list-page {
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

.tab-item:hover {
  color: var(--text-main);
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

.user-list {
  background: #fff;
  min-height: 200px;
}

.empty-tip {
  text-align: center;
  color: var(--text-placeholder);
  padding: 60px 0;
}

.user-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-color);
  cursor: pointer;
  transition: background-color 0.2s;
}

.user-item:hover {
  background-color: var(--bg-hover);
}

.user-item-info {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-weight: 600;
  font-size: 15px;
  color: var(--text-main);
  margin-bottom: 4px;
}

.user-bio {
  font-size: 13px;
  color: var(--text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.follow-btn {
  flex-shrink: 0;
}

.load-more {
  text-align: center;
  padding: 16px;
  background: #fff;
}
</style>
