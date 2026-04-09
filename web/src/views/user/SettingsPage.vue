<template>
  <div class="settings-page">
    <div class="settings-card">
      <h2 class="page-title">设置</h2>

      <!-- 设置列表 -->
      <div class="settings-list">
        <!-- 修改密码 -->
        <div class="settings-item" @click="showPasswordDialog = true">
          <div class="settings-item-left">
            <el-icon class="settings-icon"><Lock /></el-icon>
            <span>修改密码</span>
          </div>
          <el-icon class="arrow-icon"><ArrowRight /></el-icon>
        </div>

        <!-- 黑名单 -->
        <div class="settings-item" @click="showBlacklistDialog = true">
          <div class="settings-item-left">
            <el-icon class="settings-icon"><UserFilled /></el-icon>
            <span>黑名单</span>
          </div>
          <div class="settings-item-right">
            <span v-if="blacklistCount > 0" class="badge">{{ blacklistCount }} 人</span>
            <el-icon class="arrow-icon"><ArrowRight /></el-icon>
          </div>
        </div>
      </div>
    </div>

    <!-- 修改密码弹窗 -->
    <el-dialog
      v-model="showPasswordDialog"
      title="修改密码"
      width="400px"
      destroy-on-close
      append-to-body
    >
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="100px">
        <el-form-item label="当前密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入当前密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPasswordDialog = false">取消</el-button>
        <el-button type="primary" @click="savePassword" :loading="pwdLoading">保存</el-button>
      </template>
    </el-dialog>

    <!-- 黑名单弹窗 -->
    <el-dialog
      v-model="showBlacklistDialog"
      title="黑名单"
      width="500px"
      destroy-on-close
      append-to-body
    >
      <div v-if="blacklistLoading" class="loading-container">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>加载中...</span>
      </div>
      <div v-else-if="blacklist.length === 0" class="empty-container">
        <el-icon class="empty-icon"><UserFilled /></el-icon>
        <span>暂无黑名单用户</span>
      </div>
      <div v-else class="blacklist-list">
        <div v-for="user in blacklist" :key="user.id" class="blacklist-item">
          <div class="user-info">
            <el-avatar :src="user.avatarUrl" :size="40">{{ user.nickname?.charAt(0) }}</el-avatar>
            <div class="user-detail">
              <div class="nickname">{{ user.nickname }}</div>
              <div class="bio">{{ user.bio || '暂无简介' }}</div>
            </div>
          </div>
          <el-button size="small" type="danger" plain @click="handleRemoveFromBlacklist(user.id)">
            移除
          </el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Lock, UserFilled, ArrowRight, Loading } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { updatePassword } from '@/api/user'
import { getBlacklist, removeFromBlacklist } from '@/api/blacklist'

const userStore = useUserStore()

// 修改密码相关
const showPasswordDialog = ref(false)
const pwdFormRef = ref()
const pwdLoading = ref(false)
const pwdForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入当前密码' }],
  newPassword: [
    { required: true, message: '请输入新密码' },
    { min: 6, message: '密码至少 6 位' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码' },
    {
      validator: (rule, val, cb) => {
        val !== pwdForm.value.newPassword ? cb(new Error('两次密码不一致')) : cb()
      }
    }
  ]
}

async function savePassword() {
  await pwdFormRef.value.validate()
  pwdLoading.value = true
  try {
    await updatePassword({ oldPassword: pwdForm.value.oldPassword, newPassword: pwdForm.value.newPassword })
    ElMessage.success('密码已修改，请重新登录')
    showPasswordDialog.value = false
    pwdForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
    await userStore.logout()
  } catch (e) {
    // error handling
  } finally {
    pwdLoading.value = false
  }
}

// 黑名单相关
const showBlacklistDialog = ref(false)
const blacklist = ref([])
const blacklistLoading = ref(false)

const blacklistCount = ref(0)

async function loadBlacklist() {
  blacklistLoading.value = true
  try {
    const res = await getBlacklist()
    blacklist.value = res.data || []
    blacklistCount.value = blacklist.value.length
  } catch (e) {
    // error handling
  } finally {
    blacklistLoading.value = false
  }
}

async function handleRemoveFromBlacklist(userId) {
  try {
    await removeFromBlacklist(userId)
    ElMessage.success('已从黑名单移除')
    blacklist.value = blacklist.value.filter(u => u.id !== userId)
    blacklistCount.value = blacklist.value.length
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

// 打开黑名单弹窗时加载数据
import { watch } from 'vue'
watch(showBlacklistDialog, (val) => {
  if (val) {
    loadBlacklist()
  }
})
</script>

<style scoped>
.settings-page { max-width: 600px; margin: 0 auto; }

.settings-card { background: #fff; border-radius: 16px; padding: 24px; border: 1px solid #eff3f4; }

.page-title { font-size: 20px; font-weight: 700; margin-bottom: 24px; color: #0f1419; }

.settings-list { display: flex; flex-direction: column; }

.settings-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 0;
  border-bottom: 1px solid #eff3f4;
  cursor: pointer;
  transition: background 0.2s;
}

.settings-item:last-child { border-bottom: none; }

.settings-item:hover { background: #f7f9f9; margin: 0 -12px; padding-left: 12px; padding-right: 12px; border-radius: 8px; }

.settings-item-left { display: flex; align-items: center; gap: 12px; }

.settings-icon { font-size: 20px; color: #0f1419; }

.arrow-icon { color: #536471; }

.settings-item-right { display: flex; align-items: center; gap: 8px; }

.badge { background: #eff3f4; color: #536471; padding: 2px 8px; border-radius: 12px; font-size: 13px; }

.loading-container, .empty-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  color: #536471;
  gap: 12px;
}

.empty-icon { font-size: 48px; }

.blacklist-list { display: flex; flex-direction: column; gap: 12px; }

.blacklist-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  border-radius: 8px;
  background: #f7f9f9;
}

.user-info { display: flex; align-items: center; gap: 12px; }

.user-detail { display: flex; flex-direction: column; }

.nickname { font-weight: 600; color: #0f1419; }

.bio { font-size: 13px; color: #536471; max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
</style>
