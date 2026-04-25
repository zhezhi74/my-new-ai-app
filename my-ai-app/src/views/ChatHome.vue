<template>
  <div class="chat-home-container">
    <div class="welcome-content">
      <el-icon :size="60" color="#409EFF"><ChatDotRound /></el-icon>
      <h1>你好！</h1>
      <h2>今天我能为你做些什么？</h2>
      <p>你可以从左侧选择一个历史问诊，或开启一个新问诊。</p>
    </div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.chat-home-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
}
.welcome-content {
  text-align: center;
  color: #606266;
}
h1 {
  margin: 20px 0 10px;
}
h2 {
  font-weight: normal;
  color: #909399;
}
</style>