<template>
  <el-dialog
    v-model="dialogVisible"
    :title="title"
    width="500px"
    :close-on-click-modal="false"
    class="user-list-dialog"
  >
    <div class="user-list-container" v-loading="loading">
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
            @click.stop="handleFollow(user)"
          >
            关注
          </el-button>
          <el-button
            v-else
            size="small"
            @click.stop="handleUnfollow(user)"
          >
            已关注
          </el-button>
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
        @current-change="loadUsers"
      />
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listFollowing, listFollowers, follow, unfollow } from '@/api/follow'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
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

const emit = defineEmits(['update:visible'])

const router = useRouter()

const dialogVisible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

const loading = ref(false)
const userList = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = 20

watch(() => props.visible, (val) => {
  if (val) {
    currentPage.value = 1
    loadUsers()
  }
})

async function loadUsers() {
  loading.value = true
  try {
    const fetchFn = props.listType === 'following' ? listFollowing : listFollowers
    const res = await fetchFn(props.userId, {
      page: currentPage.value,
      pageSize
    })
    userList.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch (e) {
    console.error('加载用户列表失败', e)
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

function handleUserClick(user) {
  dialogVisible.value = false
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
.user-list-container {
  max-height: 400px;
  overflow-y: auto;
}

.empty-tip {
  text-align: center;
  color: var(--text-placeholder, #909399);
  padding: 40px 0;
}

.user-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.user-item:hover {
  background-color: var(--bg-hover, #f5f7fa);
}

.user-info {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-weight: 600;
  font-size: 15px;
  color: var(--text-main, #303133);
  margin-bottom: 4px;
}

.user-bio {
  font-size: 13px;
  color: var(--text-secondary, #909399);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.follow-btn {
  flex-shrink: 0;
}

:deep(.el-dialog__footer) {
  text-align: center;
}
</style>
