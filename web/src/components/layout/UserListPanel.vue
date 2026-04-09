<template>
  <div class="user-list-panel">
    <!-- 返回按钮和标题 -->
    <div class="panel-header">
      <div class="back-btn" @click="$emit('back')">
        <el-icon><ArrowLeft /></el-icon>
      </div>
      <h3 class="panel-title">{{ title }}</h3>
    </div>

    <!-- 用户列表 -->
    <div class="user-list" v-loading="loading">
      <div v-if="!loading && userList.length === 0" class="empty-tip">
        暂无数据
      </div>
      <div
        v-for="user in userList"
        :key="user.id"
        class="user-item"
        @click="handleUserClick(user)"
      >
        <el-avatar :src="user.avatarUrl" :size="48">
          {{ user.nickname?.charAt(0) }}
        </el-avatar>
        <div class="user-info">
          <div class="user-name">{{ user.nickname }}</div>
          <div class="user-bio">{{ user.bio || '这个人很懒，什么都没写' }}</div>
        </div>
        <div v-if="!user.isSelf && user.isFollowing != null" class="follow-btn">
          <el-button
            v-if="!user.isFollowing"
            type="primary"
            size="small"
            round
            @click.stop="handleFollow(user)"
          >
            关注
          </el-button>
          <el-button
            v-else
            size="small"
            round
            @click.stop="handleUnfollow(user)"
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
import { ref, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { listFollowing, listFollowers, follow, unfollow } from '@/api/follow'

const props = defineProps({
  userId: {
    type: Number,
    required: true
  },
  listType: {
    type: String,
    required: true,
    validator: (value) => ['following', 'followers'].includes(value)
  },
  title: {
    type: String,
    default: '用户列表'
  }
})

const emit = defineEmits(['back'])

const router = useRouter()

const loading = ref(false)
const loadingMore = ref(false)
const userList = ref([])
const currentPage = ref(1)
const pageSize = 20
const total = ref(0)

const hasMore = ref(false)

watch(() => [props.userId, props.listType], () => {
  currentPage.value = 1
  loadUsers()
}, { immediate: true })

onMounted(() => {
  loadUsers()
})

async function loadUsers(isLoadMore = false) {
  if (isLoadMore) {
    loadingMore.value = true
  } else {
    loading.value = true
  }

  try {
    const fetchFn = props.listType === 'following' ? listFollowing : listFollowers
    const res = await fetchFn(props.userId, {
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

function handleUserClick(user) {
  router.push(`/user/${user.id}`)
}

async function handleFollow(user) {
  try {
    await follow(user.id)
    user.isFollowing = true
    ElMessage.success('关注成功')
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

async function handleUnfollow(user) {
  try {
    await unfollow(user.id)
    user.isFollowing = false
    ElMessage.success('已取消关注')
  } catch (e) {
    ElMessage.error('操作失败')
  }
}
</script>

<style scoped>
.user-list-panel {
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

.user-list {
  flex: 1;
  overflow-y: auto;
}

.empty-tip {
  text-align: center;
  color: var(--text-placeholder);
  padding: 40px 0;
}

.user-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.user-item:hover {
  background-color: var(--bg-hover);
}

.user-info {
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
  padding: 16px;
  text-align: center;
  border-top: 1px solid var(--border-color);
}
</style>
