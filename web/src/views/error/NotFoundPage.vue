<template>
  <div class="not-found">
    <div class="content">
      <h1 class="code">404</h1>
      <p class="msg">你访问的页面不存在</p>
      <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
    </div>
  </div>
</template>

<style scoped>
.not-found {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f4f6fa;
}
.content {
  text-align: center;
}
.code {
  font-size: 96px;
  font-weight: 700;
  color: #dcdfe6;
  line-height: 1;
  margin-bottom: 16px;
}
.msg {
  font-size: 18px;
  color: #909399;
  margin-bottom: 24px;
}
</style>
