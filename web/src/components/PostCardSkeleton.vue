<template>
  <div class="post-card-skeleton">
    <!-- 头部骨架 -->
    <div class="skeleton-header">
      <el-skeleton :animated="true" :loading="true" animated>
        <template #template>
          <div class="header-content">
            <el-skeleton-item variant="circle" style="width: 40px; height: 40px" />
            <div class="meta-skeleton">
              <el-skeleton-item variant="text" style="width: 80px; height: 16px" />
              <el-skeleton-item variant="text" style="width: 60px; height: 12px; margin-top: 4px" />
            </div>
          </div>
        </template>
      </el-skeleton>
    </div>

    <!-- 内容骨架 -->
    <div class="skeleton-content">
      <el-skeleton :animated="true" :loading="true" animated>
        <template #template>
          <div class="content-skeleton">
            <el-skeleton-item variant="h3" style="width: 70%; height: 20px; margin-bottom: 8px" />
            <el-skeleton-item variant="text" style="width: 100%; height: 14px; margin-bottom: 6px" />
            <el-skeleton-item variant="text" style="width: 90%; height: 14px; margin-bottom: 6px" />
            <el-skeleton-item variant="text" style="width: 60%; height: 14px" />
          </div>
        </template>
      </el-skeleton>
    </div>

    <!-- 底部骨架 -->
    <div class="skeleton-footer">
      <el-skeleton :animated="true" :loading="true" animated>
        <template #template>
          <div class="footer-content">
            <div class="action-skeleton">
              <el-skeleton-item variant="circle" style="width: 20px; height: 20px" />
              <el-skeleton-item variant="text" style="width: 30px; height: 14px" />
            </div>
            <div class="action-skeleton">
              <el-skeleton-item variant="circle" style="width: 20px; height: 20px" />
              <el-skeleton-item variant="text" style="width: 30px; height: 14px" />
            </div>
            <div class="action-skeleton">
              <el-skeleton-item variant="circle" style="width: 20px; height: 20px" />
              <el-skeleton-item variant="text" style="width: 30px; height: 14px" />
            </div>
          </div>
        </template>
      </el-skeleton>
    </div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.post-card-skeleton {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  border: 1px solid #f0f0f0;
}

.skeleton-header {
  margin-bottom: 16px;
}

.header-content {
  display: flex;
  align-items: center;
  gap: 12px;
}

.meta-skeleton {
  display: flex;
  flex-direction: column;
}

.skeleton-content {
  margin-bottom: 16px;
}

.content-skeleton {
  display: flex;
  flex-direction: column;
}

.skeleton-footer {
  padding-top: 14px;
  border-top: 1px solid #f0f0f0;
}

.footer-content {
  display: flex;
  gap: 24px;
}

.action-skeleton {
  display: flex;
  align-items: center;
  gap: 6px;
}

@media (max-width: 768px) {
  .post-card-skeleton {
    padding: 16px;
    border-radius: 12px;
  }
}
</style>
