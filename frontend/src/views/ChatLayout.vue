<template>
  <el-container class="chat-layout">
    <el-aside :width="isSidebarCollapsed ? '80px' : '260px'" class="sidebar">
      <div class="sidebar-header">
        <el-button :icon="Menu" @click="toggleSidebar" text circle />
        <span v-if="!isSidebarCollapsed" class="glass-panel">医疗 AI 智能决策系统</span>
      </div>
      <div class="new-chat-button-container">
        <el-button :icon="Plus" @click="startNewChat" :round="!isSidebarCollapsed">
          <span v-if="!isSidebarCollapsed">开启新问诊</span>
        </el-button>
      </div>
      <el-scrollbar class="history-list">
        <div v-for="conv in conversations" :key="conv.id" class="history-item" @click="switchToConversation(conv.id)" :class="{'is-active': currentConversationId === conv.id}">
          <el-icon><ChatLineSquare /></el-icon>
          <span v-if="!isSidebarCollapsed" class="history-title">{{ conv.title }}</span>
          <el-icon v-if="!isSidebarCollapsed" class="delete-icon" @click.stop="handleDeleteConversation(conv.id)"><Delete /></el-icon>
        </div>
      </el-scrollbar>

      <div v-if="userRole === 'admin'" class="admin-panel-link">
        <el-button :icon="SetUp" @click="goToAdmin" :round="!isSidebarCollapsed">
          <span v-if="!isSidebarCollapsed">后台管理</span>
        </el-button>
      </div>

    </el-aside>
    <el-main class="main-content">
      <router-view @new-conversation-created="refreshConversations"/>
    </el-main>
  </el-container>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import api from '../api';
// 【新增】: 额外导入 SetUp 图标
import { Plus, ChatLineSquare, Menu, Delete, SetUp } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';

const router = useRouter();
const route = useRoute();
const conversations = ref([]);
const isSidebarCollapsed = ref(false);

// 【新增】: 从本地存储读取用户角色
const userRole = ref(localStorage.getItem('user_role') || 'user');

const currentConversationId = computed(() => parseInt(route.params.id, 10));

const fetchConversations = async () => {
  try {
    const response = await api.getConversations();
    conversations.value = response.data;
  } catch (error) {
    ElMessage.error('获取历史会话列表失败');
  }
};

const startNewChat = () => {
  if (route.path !== '/chat/0') {
    router.push('/chat/0');
  }
};

const switchToConversation = (id) => {
  if (currentConversationId.value === id) return;
  router.push(`/chat/${id}`);
};

const toggleSidebar = () => {
  isSidebarCollapsed.value = !isSidebarCollapsed.value;
}

const refreshConversations = () => {
  fetchConversations();
}

const handleDeleteConversation = async (id) => {
  try {
    await ElMessageBox.confirm(
        '此操作将永久删除该对话及其所有聊天记录, 是否继续?',
        '警告',
        {
          confirmButtonText: '确认删除',
          cancelButtonText: '取消',
          type: 'warning',
        }
    );
    await api.deleteConversation(id);
    ElMessage.success('删除成功!');
    conversations.value = conversations.value.filter(c => c.id !== id);
    if (currentConversationId.value === id) {
      router.push('/chat');
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败，请稍后重试');
    }
  }
};

// 【新增】: 跳转到后台管理的函数
const goToAdmin = () => {
  router.push('/admin');
};

onMounted(fetchConversations);
</script>

<style scoped>
.chat-layout { height: 100vh; }
.sidebar {
  /* 1. 将背景调为稍浅的深灰色（医疗设备金属质感） */
  background-color: #161B22 !important; 
  
  /* 2. 彻底移除分割线，消除割裂感 */
  border-right: none !important;
  
  /* 3. 增加极细微的右向阴影，产生一种柔和的重叠层级感 */
  box-shadow: 10px 0 30px rgba(0, 0, 0, 0.4);
  
  display: flex;
  flex-direction: column;
  transition: width 0.3s ease;
  z-index: 10;
}
/* 确保侧边栏内的背景也是一致的 */
.el-aside {
  background-color: #161B22 !important;
}
.sidebar-header { padding: 15px; display: flex; align-items: center; flex-shrink: 0; }
.new-chat-text { margin-left: 10px; font-weight: bold; font-size: 20px; white-space: nowrap; }
.new-chat-button-container { padding: 0 15px 15px; flex-shrink: 0; }
.new-chat-button-container .el-button {
  width: 100%;
  background-color: transparent !important;
  border: 1px solid var(--medical-accent) !important;
  color: var(--medical-accent) !important;
  /* 增加青色外发光 */
  box-shadow: 0 0 10px rgba(0, 245, 255, 0.3) !important;
  transition: all 0.3s ease;
}

.new-chat-button-container .el-button:hover {
  /* 悬停时增强发光 */
  box-shadow: 0 0 20px rgba(0, 245, 255, 0.6) !important;
  transform: translateY(-1px);
}
.history-list { flex-grow: 1; }
.history-item {
  display: flex;
  align-items: center;
  padding: 12px 22px;
  cursor: pointer;
  transition: all 0.3s ease;
  border-radius: 8px;
  margin: 4px 8px;
  position: relative;
  border: 1px solid transparent; /* 为选中状态预留边框位置，防止跳动 */
  color: #A3B3CC; /* 默认冷灰色文字 */
}

/* 2. 鼠标悬浮状态：微微亮起起 */
.history-item:hover {
  background-color: rgba(255, 255, 255, 0.08) !important;
  color: #FFFFFF !important;
}

/* 3. 终极修复：当前选中的对话（高亮青色透视感） */
.history-item.is-active {
  background-color: rgba(0, 245, 255, 0.15) !important; /* 透底的霓虹青背景 */
  color: #00F5FF !important; /* 字体变成发光的青色，绝不是黑色 */
  font-weight: bold;
  border: 1px solid rgba(0, 245, 255, 0.4) !important; /* 外边框高亮 */
  box-shadow: inset 0 0 10px rgba(0, 245, 255, 0.1) !important; /* 内侧微微发光 */
}

/* 确保证选中状态下的垃圾桶图标也是同色系 */
.history-item.is-active .delete-icon {
  color: #00F5FF;
}
.history-title {
  margin-left: 10px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 14px;
  padding-right: 25px;
}
.main-content { padding: 0; }
.delete-icon {
  display: none;
  position: absolute;
  right: 15px;
  top: 50%;
  transform: translateY(-50%);
  color: #909399;
}
.delete-icon:hover {
  color: #F56C6C;
}
.history-item:hover .delete-icon {
  display: block;
}

/* 【新增】: 后台管理按钮的样式 */
.admin-panel-link {
  padding: 10px 15px;
  margin-top: auto; /* 将按钮推到底部 */
  border-top: 1px solid #e0e0e0; /* 分隔线 */
}
.admin-panel-link .el-button {
  width: 100%;
}
</style>