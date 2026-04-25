<template>
  <el-container class="admin-layout">
    <el-aside width="240px" class="sidebar glass-sidebar">
      <div class="sidebar-header">
        <div class="pulse-logo"></div>
        <h3 class="admin-title">SYSTEM MATRIX</h3>
      </div>
      
      <el-menu :default-active="$route.path" router class="cyber-menu">
        <el-menu-item index="/admin/dashboard">
          <el-icon><Monitor /></el-icon>
          <span>全局态势大屏</span>
        </el-menu-item>

        <el-menu-item index="/chat">
          <el-icon><Cpu /></el-icon>
          <span>终端问诊沙盘</span>
        </el-menu-item>

        <el-menu-item index="/admin/knowledge">
          <el-icon><Platform /></el-icon>
          <span>RAG 核心知识库</span>
        </el-menu-item>

        <el-menu-item index="/admin/history">
          <el-icon><FolderOpened /></el-icon>
          <span>全息病例档案</span>
        </el-menu-item>

        <el-menu-item index="/admin/reports">
          <el-icon><DataAnalysis /></el-icon>
          <span>多模态报告引擎</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-main class="cyber-main">
      <router-view v-slot="{ Component }">
        <transition name="fade-transform" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </el-main>
  </el-container>
</template>

<script setup>
// ★ 这里的引入也已经彻底替换
import { Monitor, Cpu, Platform, FolderOpened, DataAnalysis } from '@element-plus/icons-vue'
</script>

<style scoped>
.admin-layout { height: 100vh; background-color: #0B0E14; }

/* 玻璃拟物化侧边栏 */
.glass-sidebar { 
  background: rgba(13, 17, 23, 0.7) !important; 
  backdrop-filter: blur(15px);
  border-right: 1px solid rgba(0, 245, 255, 0.1); 
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  height: 80px; display: flex; align-items: center; padding: 0 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}
.pulse-logo {
  width: 12px; height: 12px; border-radius: 50%; background: #00F5FF;
  box-shadow: 0 0 10px #00F5FF; margin-right: 15px; animation: pulse 2s infinite;
}
.admin-title { color: #00F5FF; font-family: 'Courier New', monospace; letter-spacing: 2px; font-size: 1.1rem; }

/* 赛博风格菜单 */
.cyber-menu { border-right: none; background: transparent; padding: 10px 0; }
.el-menu-item {
  margin: 8px 15px; border-radius: 8px; height: 50px; line-height: 50px;
  color: #8B949E; transition: all 0.3s;
}
.el-menu-item:hover {
  background-color: rgba(255, 255, 255, 0.05) !important; color: #FFF;
}
.el-menu-item.is-active {
  background: linear-gradient(90deg, rgba(0, 245, 255, 0.15) 0%, transparent 100%) !important;
  color: #00F5FF !important; border-left: 3px solid #00F5FF; font-weight: bold;
}

.cyber-main { 
  padding: 0; 
  overflow-y: auto; /* 核心修复：允许纵向滚动 */
  height: 100vh;    /* 确保容器高度等于屏幕高度 */
}
/* 自定义主区域滚动条样式 */
.cyber-main::-webkit-scrollbar {
  width: 6px;
}
.cyber-main::-webkit-scrollbar-track {
  background: rgba(13, 17, 23, 0.8);
}
.cyber-main::-webkit-scrollbar-thumb {
  background: rgba(0, 245, 255, 0.2);
  border-radius: 3px;
}
.cyber-main::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 245, 255, 0.5);
}
/* 路由切换动画 */
.fade-transform-enter-active, .fade-transform-leave-active { transition: all 0.4s ease; }
.fade-transform-enter-from, .fade-transform-leave-to { opacity: 0; transform: translateY(15px); }
@keyframes pulse { 0% { opacity: 1; box-shadow: 0 0 10px #00F5FF; } 50% { opacity: 0.4; box-shadow: 0 0 2px #00F5FF; } 100% { opacity: 1; box-shadow: 0 0 10px #00F5FF; } }
</style>