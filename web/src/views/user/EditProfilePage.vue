<template>
  <div class="edit-profile-page">
    <!-- 顶部导航栏 -->
    <div class="edit-profile-header">
      <div class="header-left">
        <el-button :icon="ArrowLeft" circle @click="goBack" class="back-btn" />
        <h2 class="header-title">编辑资料</h2>
      </div>
      <el-button type="primary" @click="saveProfile" :loading="loading" class="save-btn">保存</el-button>
    </div>

    <div class="edit-profile-content">
      <!-- 封面图区域 -->
      <div class="banner-area">
        <div
          class="banner-image"
          v-if="bannerPreview || userStore.userInfo?.bannerUrl"
          :style="{ backgroundImage: `url(${bannerPreview || userStore.userInfo?.bannerUrl})` }"
        ></div>
        <div class="banner-placeholder" v-else>
          <div class="banner-gradient"></div>
        </div>
        <div class="banner-overlay" @click="triggerBannerUpload">
          <el-icon :size="24"><Camera /></el-icon>
          <span>更换背景</span>
        </div>
        <input
          ref="bannerInput"
          type="file"
          accept="image/jpeg,image/png,image/gif,image/webp"
          style="display: none"
          @change="handleBannerChange"
        />
      </div>

      <!-- 头像区域 -->
      <div class="avatar-area">
        <div class="avatar-container">
          <el-avatar :size="120" :src="userStore.userInfo?.avatarUrl" class="avatar">
            {{ userStore.userInfo?.nickname?.charAt(0) || 'U' }}
          </el-avatar>
          <div class="avatar-overlay" @click="triggerAvatarUpload">
            <el-icon :size="28"><Camera /></el-icon>
          </div>
        </div>
        <input
          ref="avatarInput"
          type="file"
          accept="image/jpeg,image/png,image/gif,image/webp"
          style="display: none"
          @change="handleAvatarChange"
        />
      </div>

      <!-- 表单区域 -->
      <div class="form-area">
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
          <!-- 昵称 -->
          <el-form-item label="昵称" prop="nickname">
            <el-input
              v-model="form.nickname"
              maxlength="20"
              show-word-limit
              placeholder="请输入昵称"
              class="custom-input"
            />
          </el-form-item>

          <!-- 个人简介 -->
          <el-form-item label="简介" prop="bio">
            <el-input
              v-model="form.bio"
              type="textarea"
              :rows="4"
              maxlength="200"
              show-word-limit
              placeholder="介绍一下你自己"
              class="custom-input"
            />
          </el-form-item>
        </el-form>

        <!-- 提示信息 -->
        <div class="tips">
          <p class="tip-item">头像支持 JPG、PNG、GIF、WebP，最大 2MB</p>
          <p class="tip-item">背景图支持 JPG、PNG、GIF、WebP，最大 5MB</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Camera } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { updateProfile, uploadAvatar, uploadBanner } from '@/api/user'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref()
const loading = ref(false)
const form = ref({ nickname: '', bio: '' })

const rules = {
  nickname: [
    { required: true, message: '昵称不能为空' },
    { max: 20, message: '昵称不超过 20 字' }
  ]
}

const avatarInput = ref(null)
const bannerInput = ref(null)
const bannerPreview = ref('')

onMounted(() => {
  form.value.nickname = userStore.userInfo?.nickname || ''
  form.value.bio = userStore.userInfo?.bio || ''
})

async function saveProfile() {
  await formRef.value.validate()
  loading.value = true
  try {
    await updateProfile(form.value)
    await userStore.refreshUserInfo()
    ElMessage.success('资料已更新')
    router.back()
  } catch (err) {
    ElMessage.error(err.message || '保存失败')
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.back()
}

// 头像上传
function triggerAvatarUpload() {
  avatarInput.value?.click()
}

async function handleAvatarChange(e) {
  const file = e.target.files?.[0]
  if (!file) return

  if (file.size > 2 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过 2MB')
    return
  }

  try {
    await uploadAvatar(file)
    await userStore.refreshUserInfo()
    ElMessage.success('头像已更新')
  } catch (err) {
    console.error('头像上传失败:', err)
    ElMessage.error(err?.message || err?.data?.message || '头像上传失败')
  } finally {
    e.target.value = ''
  }
}

// 背景图上传
function triggerBannerUpload() {
  bannerInput.value?.click()
}

async function handleBannerChange(e) {
  const file = e.target.files?.[0]
  if (!file) return

  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过 5MB')
    return
  }

  // 预览
  bannerPreview.value = URL.createObjectURL(file)

  try {
    await uploadBanner(file)
    await userStore.refreshUserInfo()
    bannerPreview.value = ''
    ElMessage.success('背景图已更新')
  } catch (err) {
    console.error('背景图上传失败:', err)
    bannerPreview.value = ''
    ElMessage.error(err?.message || err?.data?.message || '背景图上传失败')
  } finally {
    e.target.value = ''
  }
}
</script>

<style scoped>
.edit-profile-page {
  min-height: 100vh;
  background: #f7f9f9;
}

.edit-profile-header {
  position: sticky;
  top: 0;
  z-index: 100;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid #eff3f4;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.back-btn {
  border: none;
  background: transparent;
}

.back-btn:hover {
  background: #e7e7e8;
}

.header-title {
  font-size: 18px;
  font-weight: 700;
  color: #0f1419;
  margin: 0;
}

.save-btn {
  background: #0f1419;
  border: none;
  border-radius: 20px;
  padding: 8px 20px;
  font-weight: 600;
}

.save-btn:hover {
  background: #333;
}

.edit-profile-content {
  max-width: 600px;
  margin: 0 auto;
  background: #fff;
}

.banner-area {
  position: relative;
  height: 200px;
  background: #cfd9df;
  cursor: pointer;
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

.banner-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #fff;
  opacity: 0;
  transition: opacity 0.2s;
}

.banner-area:hover .banner-overlay {
  opacity: 1;
}

.avatar-area {
  position: relative;
  padding: 0 16px;
  margin-top: -60px;
}

.avatar-container {
  position: relative;
  width: fit-content;
}

.avatar {
  border: 4px solid #fff;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  font-size: 48px;
  font-weight: 600;
  color: #fff;
}

.avatar-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.2s;
}

.avatar-container:hover .avatar-overlay {
  opacity: 1;
}

.form-area {
  padding: 20px 16px 40px;
}

.custom-input :deep(.el-input__wrapper) {
  border-radius: 8px;
  box-shadow: none;
  border: 1px solid #cfd9df;
  background: #f7f9f9;
}

.custom-input :deep(.el-input__wrapper:hover),
.custom-input :deep(.el-input__wrapper.is-focus) {
  border-color: #1d9bf0;
  box-shadow: 0 0 0 2px rgba(29, 155, 240, 0.1);
}

.custom-input :deep(.el-textarea__inner) {
  border-radius: 8px;
  box-shadow: none;
  border: 1px solid #cfd9df;
  background: #f7f9f9;
  resize: none;
}

.custom-input :deep(.el-textarea__inner:hover),
.custom-input :deep(.el-textarea__inner:focus) {
  border-color: #1d9bf0;
  box-shadow: 0 0 0 2px rgba(29, 155, 240, 0.1);
}

.custom-input :deep(.el-form-item__label) {
  font-weight: 600;
  color: #0f1419;
  padding-bottom: 8px;
}

.tips {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #eff3f4;
}

.tip-item {
  font-size: 13px;
  color: #536471;
  margin: 0 0 8px;
}

.tip-item:last-child {
  margin-bottom: 0;
}
</style>
