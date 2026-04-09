<template>
  <aside class="right-panel">
    <div class="panel-content">
      <!-- 搜索框 -->
      <div class="search-box">
        <el-input
          v-model="keyword"
          placeholder="搜索 NexaTalk"
          :prefix-icon="Search"
          class="rounded-input"
          @keyup.enter="handleSearch"
        />
      </div>

      <!-- 用户卡片视图 -->
      <div v-if="userStore.isLoggedIn" class="user-widget card">
        <div class="user-bg"></div>
        <div class="user-body">
          <el-avatar :size="64" :src="userStore.userInfo?.avatarUrl" class="user-avatar">
            {{ userStore.userInfo?.nickname?.charAt(0) }}
          </el-avatar>
          <h3 class="nickname">{{ userStore.userInfo?.nickname }}</h3>
          <p class="bio">{{ userStore.userInfo?.bio || '这个人很懒，什么都没写' }}</p>
          <div class="stats-row">
            <div class="stat clickable" @click="goToFollowing">
              <span class="num">{{ followStats.following || 0 }}</span>
              <span class="label">关注</span>
            </div>
            <div class="stat clickable" @click="goToFollowers">
              <span class="num">{{ followStats.followers || 0 }}</span>
              <span class="label">粉丝</span>
            </div>
            <div class="stat clickable" @click="goToPosts">
              <span class="num">{{ postCount }}</span>
              <span class="label">帖子</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 分区列表 -->
      <div class="widget-card card">
        <h3 class="widget-title">分区</h3>
        <ul class="section-list" v-loading="loadingSections">
          <li
            class="section-item btn-animate"
            :class="{ active: !currentSectionId }"
            @click="handleSectionClick(null)"
          >
            <div class="section-icon all-icon">
              <el-icon><Menu /></el-icon>
            </div>
            <span class="section-name">全部内容</span>
          </li>

          <li
            v-for="section in sections"
            :key="section.id"
            class="section-item btn-animate"
            :class="{ active: currentSectionId === section.id }"
            @click="handleSectionClick(section.id)"
          >
            <el-avatar
              v-if="section.coverUrl"
              :src="section.coverUrl"
              :size="24"
              shape="square"
              class="section-icon"
            />
            <div v-else class="section-icon default-icon">
              {{ section.name.charAt(0) }}
            </div>
            <span class="section-name">{{ section.name }}</span>
          </li>
        </ul>
      </div>

      <!-- 页脚 -->
      <footer class="footer">
        <p>&copy; 2024 NexaTalk. All rights reserved.</p>
        <div class="footer-links">
          <a href="#">关于我们</a>
          <a href="#">用户协议</a>
          <a href="#">隐私政策</a>
        </div>
      </footer>
    </div>
  </aside>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Search, Menu } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { listSections } from '@/api/section'
import { getFollowStats } from '@/api/follow'
import { listMyPosts } from '@/api/post'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const keyword = ref('')
const sections = ref([])
const loadingSections = ref(false)
const currentSectionId = ref(null)

// 用户统计相关
const followStats = ref({ following: 0, followers: 0 })
const postCount = ref(0)
const currentUserId = computed(() => userStore.userInfo?.userId)

onMounted(() => {
  loadSections()
  if (userStore.isLoggedIn) {
    loadUserStats()
  }
})

async function loadSections() {
  loadingSections.value = true
  try {
    const res = await listSections()
    sections.value = res.data || []
  } finally {
    loadingSections.value = false
  }
}

async function loadUserStats() {
  if (!currentUserId.value) return
  try {
    const [statsRes, postsRes] = await Promise.all([
      getFollowStats(currentUserId.value),
      listMyPosts({ page: 1, pageSize: 1 })
    ])
    followStats.value = statsRes.data || { following: 0, followers: 0 }
    postCount.value = postsRes.data?.total || 0
  } catch (e) {
    console.error('加载用户统计失败', e)
  }
}

function goToFollowing() {
  router.push({ name: 'UserFollowing', params: { id: currentUserId.value } })
}

function goToFollowers() {
  router.push({ name: 'UserFollowers', params: { id: currentUserId.value } })
}

function goToPosts() {
  router.push({ name: 'UserPosts', params: { id: currentUserId.value } })
}

const handleSearch = () => {
  if (keyword.value.trim()) {
    router.push({ name: 'Search', query: { keyword: keyword.value.trim() } })
  }
}

const handleSectionClick = (id) => {
  currentSectionId.value = id
  router.push({ name: 'Home', query: { sectionId: id } })
}
</script>

<style scoped>
.right-panel {
  padding: 20px 0 20px 30px;
  height: 100vh;
  position: sticky;
  top: 0;
  overflow-y: auto;
}

.panel-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.search-box {
  position: sticky;
  top: 0;
  background: var(--bg-body);
  z-index: 10;
  padding-bottom: 10px;
}

:deep(.el-input__wrapper) {
  border-radius: 99px;
  background-color: #fff;
  box-shadow: none;
  border: 1px solid var(--border-color);
  padding: 4px 15px;
}
:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--color-primary) inset;
}

/* User Widget */
.user-widget {
  padding: 0;
  overflow: hidden;
  text-align: center;
}

.user-bg {
  height: 60px;
  background: linear-gradient(135deg, #a1c4fd 0%, #c2e9fb 100%);
}

.user-body {
  padding: 0 16px 16px;
  margin-top: -32px;
}

.user-avatar {
  border: 4px solid #fff;
  background: #fff;
}

.nickname {
  margin-top: 8px;
  font-size: 18px;
  font-weight: 700;
  color: var(--text-main);
}

.bio {
  font-size: 13px;
  color: var(--text-secondary);
  margin: 4px 0 16px;
  min-height: 19px;
}

.stats-row {
  display: flex;
  justify-content: space-around;
  border-top: 1px solid var(--border-color);
  padding-top: 12px;
}

.stat {
  display: flex;
  flex-direction: column;
}

.stat.clickable {
  cursor: pointer;
  transition: opacity 0.2s;
}

.stat.clickable:hover {
  opacity: 0.7;
}

.num {
  font-weight: 700;
  font-size: 16px;
  color: var(--text-main);
}

.label {
  font-size: 12px;
  color: var(--text-secondary);
}

/* Sections */
.widget-card {
  padding: 16px;
  border: none;
}

.widget-title {
  font-size: 16px;
  font-weight: 700;
  margin-bottom: 16px;
  color: var(--text-main);
}

.section-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.section-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.section-item:hover {
  background-color: var(--bg-hover);
}

.section-item.active {
  background-color: #ecf5ff;
  color: var(--color-primary);
  font-weight: 600;
}

.section-icon {
  width: 24px;
  height: 24px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  color: #fff;
}

.all-icon {
  background: #909399;
}

.default-icon {
  background: var(--color-primary);
}

.section-name {
  font-size: 14px;
}

.footer {
  font-size: 12px;
  color: var(--text-placeholder);
  text-align: center;
  padding: 20px 0;
}

.footer-links {
  margin-top: 8px;
  display: flex;
  justify-content: center;
  gap: 12px;
}

.footer-links a:hover {
  text-decoration: underline;
}
</style>
