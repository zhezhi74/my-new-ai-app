<template>
  <div class="medical-chat-wrapper">
    <transition name="fade-transform" mode="out-in">

      <div v-if="messages.length === 0" class="hero-empty-state">
        <div class="ai-pulse-container">
          <div class="pulse-ring delay-1"></div>
          <div class="pulse-ring delay-2"></div>
          <div class="pulse-ring delay-3"></div>
          <div class="pulse-core">
            <span class="core-text">AI DIAGNOSTICS...</span>
          </div>
        </div>

        <p class="hero-sub-hint">您可以从左侧选择一个历史问诊，或开启一个新问诊</p>

        <div class="hero-input-capsule">
          <div class="capsule-header">
            <span class="badge">问诊</span>
            <span class="hint">输入您的症状或医学疑问，AI 将立即启动推理...</span>
          </div>
          <div class="capsule-body">
            <el-input
              v-model="newMessage"
              placeholder="例如：我最近胸口闷痛，伴有轻微咳嗽..."
              @keyup.enter="sendMessage"
              :disabled="isStreaming"
            >
              <template #append>
                <el-button @click="sendMessage" :disabled="isStreaming || !newMessage.trim()">发送</el-button>
              </template>
            </el-input>
          </div>
        </div>
      </div>

      <div v-else class="active-workspace">
        <div class="chat-container glass-panel">
          <el-main class="chat-main">
            <el-scrollbar ref="scrollbarRef">
              <div class="message-list">
                <div v-for="(message, index) in messages" :key="index" class="message-item" :class="{'user-message': message.role === 'user', 'assistant-message': message.role === 'assistant'}">
                  <div class="avatar"></div>
                  <div v-if="message.thinking" class="content content-thinking">
                    <el-icon class="is-loading"><Loading /></el-icon>
                    <span>{{ message.content }}</span>
                  </div>
                  <div v-else class="content" v-html="renderMessage(message)"></div>
                </div>
              </div>
            </el-scrollbar>
          </el-main>
          
          <el-footer class="chat-footer">
            <el-input
                v-model="newMessage"
                placeholder="描述您的症状，例如：我最近胸口闷痛..."
                @keyup.enter="sendMessage"
                :disabled="isStreaming"
            >
              <template #append>
                <el-button @click="sendMessage" :disabled="isStreaming || !newMessage.trim()" class="neon-btn">发送</el-button>
              </template>
            </el-input>
          </el-footer>
        </div>

        <aside class="algo-insights glass-panel">
          <div class="insight-header">
            <span class="pulse-dot"></span> 算法透视面板
          </div>
          <div class="insight-group">
            <label>当前路由意图 (BERT)</label>
            <div class="intent-badge">等待后续接入...</div>
          </div>
          <div class="insight-group">
            <label>知识库召回 (RAG)</label>
            <ul class="fact-list">
              <li class="empty-hint">暂未触发向量检索</li>
            </ul>
          </div>
        </aside>
      </div>

    </transition>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import api from '../api';
import { ElMessage } from 'element-plus';
import MarkdownIt from 'markdown-it';
import { Loading } from '@element-plus/icons-vue';

const emit = defineEmits(['new-conversation-created']);
const props = defineProps({ id: { type: String, default: '0' } });

const router = useRouter();
const route = useRoute();
const messages = ref([]);
const newMessage = ref('');
const isStreaming = ref(false);
const scrollbarRef = ref();
const conversationId = ref(parseInt(props.id, 10));

const md = new MarkdownIt({ html: true, linkify: true, typographer: true });

const renderMessage = (message) => {
  const content = message.content || '';
  if (message.role === 'assistant') { return md.render(content); }
  return content.replace(/</g, "&lt;").replace(/>/g, "&gt;");
}

// 强制状态洗牌：只要挂载组件时没有指定的会话 ID，立刻清空屏幕，逼迫核心舱显示
onMounted(() => {
  if (!route.params.id || route.params.id === '0') {
    messages.value = [];
    conversationId.value = 0;
    newMessage.value = '';
  }
});

const loadHistory = async () => {
  if (conversationId.value === 0) {
    messages.value = []; 
    return;
  };
  try {
    const response = await api.getHistory(conversationId.value);
    if (response.data.code === 200) {
      messages.value = response.data.data;
      scrollToBottom();
    } else {
      ElMessage.error('加载历史记录失败');
      router.push('/chat');
    }
  } catch (error) {
    ElMessage.error('加载历史记录请求失败');
    router.push('/chat');
  }
};

const sendMessage = () => {
  if (!newMessage.value.trim() || isStreaming.value) return;

  const userMessage = { role: 'user', content: newMessage.value };
  messages.value.push(userMessage);

  const question = newMessage.value;
  newMessage.value = '';
  isStreaming.value = true;

  messages.value.push({ role: 'assistant', content: '正在思考中...', thinking: true });
  scrollToBottom();

  const token = localStorage.getItem('jwt_token');
  const eventSource = new EventSource(`http://localhost:8888/api/chat/stream?question=${encodeURIComponent(question)}&conversationId=${conversationId.value}&token=${token}`);

  let isFirstChunk = true;

  eventSource.onmessage = (event) => {
    const lastMessage = messages.value[messages.value.length - 1];
    if (lastMessage && lastMessage.role === 'assistant') {
      if (isFirstChunk) {
        lastMessage.content = event.data;
        lastMessage.thinking = false; 
        isFirstChunk = false;
      } else {
        lastMessage.content += event.data;
      }
    }
    scrollToBottom();
  };

  eventSource.onerror = (error) => {
    console.error('EventSource failed:', error);
    const lastMessage = messages.value[messages.value.length - 1];
    if (lastMessage && lastMessage.role === 'assistant') {
      lastMessage.content = '[出现错误，请重试]';
      lastMessage.thinking = false;
    }
    isStreaming.value = false;
    eventSource.close();
    scrollToBottom();
  };

  eventSource.addEventListener('close', async (event) => {
    isStreaming.value = false;
    eventSource.close();

    const finalConversationId = event.data;

    if (conversationId.value === 0 && finalConversationId && finalConversationId !== '0') {
      try {
        await api.getConversationTitle(finalConversationId);
      } catch (error) {
        console.error("自动生成对话标题失败:", error);
      }
      emit('new-conversation-created');
      router.push(`/chat/${finalConversationId}`);
    }
  });
};

const scrollToBottom = () => {
  nextTick(() => {
    if (scrollbarRef.value && scrollbarRef.value.wrapRef) {
      scrollbarRef.value.setScrollTop(scrollbarRef.value.wrapRef.scrollHeight);
    }
  });
};

watch(() => route.params.id, (newId) => {
  conversationId.value = parseInt(newId || '0', 10);
  loadHistory();
}, { immediate: true });
</script>

<style scoped>
/* ====== 深空医学风 UI 换皮 ====== */
.medical-chat-wrapper {
  display: flex;
  gap: 20px;
  height: 100%;
  padding: 10px;
  box-sizing: border-box;
}

.chat-container { 
  flex: 7;
  display: flex; 
  flex-direction: column; 
  overflow: hidden;
}

.chat-main { padding: 0; flex-grow: 1; }
.message-list { padding: 20px; }
.message-item { display: flex; margin-bottom: 20px; align-items: flex-start; }

.avatar {
  width: 40px; height: 40px; border-radius: 50%;
  margin-right: 15px; flex-shrink: 0;
  background-size: cover; background-position: center; background-repeat: no-repeat;
}
.user-message { flex-direction: row-reverse; }
.user-message .avatar {
  margin-left: 15px; margin-right: 0;
  background-image: url('../assets/images/user-avatar.png');
}
.assistant-message .avatar {
  background-image: url('../assets/images/ai-avatar.png');
}

/* 聊天气泡暗黑化重塑 */
.content {
  padding: 12px 18px;
  border-radius: 12px;
  max-width: 75%;
  word-wrap: break-word;
  background-color: #21262D; /* AI气泡深灰 */
  color: var(--medical-text);
  line-height: 1.6;
  border: 1px solid var(--medical-border);
}
.user-message .content {
background-color: rgba(0, 247, 255, 0.1); 
  
  /* 如果你想连边框颜色一起改，就修改这里 */
  border-color: rgba(0, 245, 255, 0.3);
}

.content-thinking { display: inline-flex; align-items: center; gap: 8px; color: var(--medical-accent); }

.chat-footer {
  /* 增加底部 Padding，确保“最下面的框”不会被页面边缘顶掉 */
  padding: 10px 25px 30px 25px !important;
  background: transparent !important;
  box-sizing: border-box !important;
  width: 100% !important;
}

.chat-footer :deep(.el-input) {
  /* 回归你喜欢的：清晰的白色外边框 */
  border: 1.5px solid #FFFFFF !important; 
  border-radius: 24px !important;
  background: rgba(255, 255, 255, 0.05) !important;
  
  /* 解决遮挡关键：确保内部元素不溢出，且不被父级切除 */
  overflow: hidden !important;
  box-sizing: border-box !important;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
}

.chat-footer :deep(.el-input__wrapper) {
  background-color: transparent !important;
  box-shadow: none !important;
  border: none !important;
  height: 36px;
  padding-left: 20px;
}

.chat-footer :deep(.el-input__inner) {
  color: #FFFFFF !important; /* 输入文字改为白色 */
  font-size: 0.95rem;
}

.chat-footer :deep(.el-input-group__append) {
  background-color: transparent !important;
  border: none !important;
  padding: 0 !important;
  margin: 0 !important;
  display: flex;
}

.chat-footer :deep(.el-button) {
  /* 按钮回归：高对比度白色背景 + 黑色文字 */
  background-color: #FFFFFF !important;
  color: #000000 !important;
  border: none !important;
  border-radius: 0 !important; /* 依靠父级的 overflow:hidden 实现右侧圆角 */
  height: 38px; /* 比 wrapper 略高一点点以撑满高度 */
  padding: 0 30px !important;
  font-weight: bold;
  margin: 0 !important;
  transition: all 0.2s;
}

.chat-footer :deep(.el-button:hover) {
  background-color: var(--medical-accent, #00F5FF) !important; /* 悬停时才透出一点医学青 */
}

/* ====== 右侧算法面板样式 ====== */
/* ==========================================
   1. 重塑右侧算法面板：增加边框发光与内发光 
   ========================================== */
.algo-insights { 
  flex: 3; 
  padding: 20px; 
  color: #8B949E; 
  overflow-y: auto; 
  
  /* 强制覆盖玻璃面板的默认边框，改成左侧霓虹分界线 */
  border: none !important;
  border-left: 2px solid rgba(0, 245, 255, 0.4) !important; 
  
  /* 整体面板增加深海呼吸光晕（内发光 + 外发光） */
  box-shadow: inset 15px 0 30px rgba(0, 245, 255, 0.03), 
              -5px 0 15px rgba(0, 245, 255, 0.05) !important;
  background: rgba(13, 17, 23, 0.4) !important;
}

/* ==========================================
   2. 修复消失的脉冲小球：强制块级展示
   ========================================== */
.insight-header { 
  font-weight: bold; 
  margin-bottom: 24px; 
  display: flex; 
  align-items: center; /* 确保小球和文字垂直居中 */
  color: #fff; 
}

.pulse-dot { 
  display: inline-block !important; /* 强制行内块展示 */
  width: 10px !important; 
  height: 10px !important; 
  margin-right: 12px;
  
  /* 直接写死高亮蓝/青色，消灭黑色本体 */
  background-color: #00F5FF !important; 
  border-radius: 50%; 
  
  /* 调整光晕，使其内外通透 */
  box-shadow: 0 0 10px 2px rgba(0, 245, 255, 0.8), 
              0 0 20px 6px rgba(0, 245, 255, 0.3) !important; 
}
.empty-hint { 
  display: block !important;       /* 强制作为块级元素 */
  width: 100% !important;          /* 宽度强制拉满 */
  box-sizing: border-box !important; /* 边框和内边距计算在总宽度内 */
  padding: 12px 16px !important;   /* 【关键】统一内边距 */
  margin: 0 !important;            /* 抹平额外的外边距 */
  
  background-color: rgba(22, 27, 34, 0.8) !important; 
  color: #8B949E !important; 
  border-radius: 6px; 
  font-size: 0.9rem;
  font-style: normal !important; 
  
  border-left: 3px solid #00F5FF !important; 
  box-shadow: inset 0 0 10px rgba(0, 0, 0, 0.5); 
}
/* 确保存放 empty-hint 的列表容器不要有多余的缩进 */
.fact-list {
  width: 100% !important;
  padding: 0 !important;
  margin: 0 !important;
}

.insight-group { margin-bottom: 20px; }
.insight-group label { display: block; font-size: 0.85rem; margin-bottom: 8px; }
/* 字体发光与背景内发光 */
/* ==========================================
   3. 修复意图标签发光
   ========================================== */
.intent-badge { 
  display: block !important;       /* 强制作为块级元素 */
  width: 100% !important;          /* 宽度强制拉满 */
  box-sizing: border-box !important; /* 边框和内边距计算在总宽度内 */
  padding: 12px 16px !important;   /* 【关键】统一内边距 */
  margin: 0 !important;
  
  background: rgba(0, 245, 255, 0.05); 
  color: var(--medical-accent); 
  border-radius: 6px; 
  font-family: 'Courier New', Courier, monospace; 
  font-size: 0.9rem; 
  font-weight: bold;
  
  text-shadow: 0 0 10px var(--medical-accent);
  border: 1px solid rgba(0, 245, 255, 0.5); 
  box-shadow: inset 0 0 15px rgba(0, 245, 255, 0.15),
              0 0 10px rgba(0, 245, 255, 0.1);
}
/* ==================================================
   全局核心舱模式 (Omnibox / Hero State)
   ================================================== */
.main-workspace {
  flex: 1;
  display: flex;
  height: 100%;
  overflow: hidden;
  background-color: #0D1117;
}

.active-workspace {
  display: flex;
  width: 100%;
  height: 100%;
}

.hero-empty-state {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background: radial-gradient(circle at center, rgba(0, 245, 255, 0.05) 0%, transparent 60%);
}

/* 1. 神经突触脉冲动效 */
.ai-pulse-container {
  position: relative;
  width: 320px;  /* 从 240px 放大到 320px */
  height: 320px; /* 从 240px 放大到 320px */
  display: flex;
  justify-content: center;
  align-items: center;
  margin-bottom: 25px; /* 缩减底部留白，给下面的文字让出空间 */
}

/* 核心药丸框同步放大 */
.pulse-core {
  z-index: 10;
  background: rgba(13, 17, 23, 0.8);
  padding: 18px 32px; /* 增加内边距 */
  border-radius: 40px;
  border: 1px solid rgba(0, 245, 255, 0.5);
  box-shadow: 0 0 30px rgba(0, 245, 255, 0.4);
  backdrop-filter: blur(4px);
}
/* 内部文字同步放大 */
.core-text {
  color: #00F5FF;
  font-family: 'Courier New', monospace;
  font-size: 1.15rem; /* 字体加大 */
  font-weight: bold;
  letter-spacing: 3px;
  text-shadow: 0 0 10px #00F5FF;
}
/* ==================================================
   调整 2：新增引导语的专属排版
   ================================================== */
.hero-sub-hint {
  color: #8B949E;
  font-size: 0.95rem;
  letter-spacing: 1.5px;
  margin: 0 0 45px 0; /* 控制与下方输入框的距离 */
  text-align: center;
  /* 增加一个极其微妙的淡入动画，显得更高级 */
  animation: fadeIn 1.4s ease-in-out; 
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(-5px); }
  to { opacity: 1; transform: translateY(0); }
}

/* ==================================================
   调整 3：悬浮指令舱 (外部框) 显著拉长，并透出微蓝
   ================================================== */
.hero-input-capsule {
  width: 65%; 
  max-width: 1200px; 
  background: rgba(22, 27, 34, 0.6);
  border: 1px solid rgba(0, 245, 255, 0.15);
  border-radius: 16px;
  padding: 20px 30px; /* 稍微增加左右内边距（30px），让里面的元素不至于太靠边 */
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(10px);
  transform: translateY(0);
  transition: all 0.3s ease;
}

.hero-input-capsule:hover {
  border-color: rgba(0, 245, 255, 0.4);
  box-shadow: 0 20px 50px rgba(0, 245, 255, 0.15);
}

.pulse-ring {
  position: absolute;
  width: 100%;
  height: 100%;
  border: 1px solid rgba(0, 245, 255, 0.6);
  border-radius: 40% 60% 70% 30% / 40% 50% 60% 50%; /* 不规则仿生圆 */
  animation: morphing 4s linear infinite;
  box-shadow: 0 0 20px rgba(0, 245, 255, 0.2) inset;
}

.delay-1 { animation-duration: 5s; }
.delay-2 { animation-duration: 7s; animation-direction: reverse; border-color: rgba(0, 245, 255, 0.3); }
.delay-3 { animation-duration: 9s; border-color: rgba(0, 245, 255, 0.1); }

@keyframes morphing {
  0% { transform: rotate(0deg) scale(0.8); }
  50% { transform: rotate(180deg) scale(1.1); }
  100% { transform: rotate(360deg) scale(0.8); }
}

.pulse-core {
  z-index: 10;
  background: rgba(13, 17, 23, 0.8);
  padding: 15px 25px;
  border-radius: 30px;
  border: 1px solid rgba(0, 245, 255, 0.5);
  box-shadow: 0 0 30px rgba(0, 245, 255, 0.4);
  backdrop-filter: blur(4px);
}

.core-text {
  color: #00F5FF;
  font-family: 'Courier New', monospace;
  font-weight: bold;
  letter-spacing: 2px;
  text-shadow: 0 0 8px #00F5FF;
}



.capsule-header {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
  padding-left: 10px;
}

.capsule-header .badge {
  background: rgba(0, 245, 255, 0.1);
  color: #00F5FF;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 0.8rem;
  font-weight: bold;
  border: 1px solid rgba(0, 245, 255, 0.3);
  margin-right: 12px;
}

.capsule-header .hint {
  color: #8B949E;
  font-size: 0.9rem;
}

/* ==================================================
   精致化调整：缩减高度并完美对齐按钮
   ================================================== */

/* 1. 调整输入框整体高度 (从 55px 降至 48px) */
.capsule-body :deep(.el-input) {
  box-sizing: border-box !important;
  border: 1.5px solid #FFFFFF !important; 
  border-radius: 24px !important;
  background: rgba(0, 0, 0, 0.3) !important;
  overflow: hidden !important;
  height: 48px !important; /* 降低高度，显得更灵动 */
  display: flex;
  align-items: center;
}

/* 2. 调整内部文字的对齐方式 */
.capsule-body :deep(.el-input__wrapper) {
  background-color: transparent !important;
  box-shadow: none !important;
  padding-left: 22px;
  height: 100%;
  display: flex;
  align-items: center;
}

/* 3. 完美居中发送按钮 */
.capsule-body :deep(.el-button) {
  height: 48px !important; /* 必须与父容器高度完全一致 */
  border-radius: 0 !important;
  background-color: #FFFFFF !important;
  color: #000 !important;
  border: none !important;
  padding: 0 18px !important;
  font-weight: bold;
  margin: 0 !important;
  
  /* 核心对齐逻辑：确保文字在白色区域绝对居中 */
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 1; /* 消除默认行高干扰 */
  transition: all 0.2s;
}

.capsule-body :deep(.el-button:hover) {
  background-color: #00F5FF !important;
}

/* 切换动画 */
.fade-transform-enter-active,
.fade-transform-leave-active {
  transition: all 0.4s ease;
}
.fade-transform-enter-from,
.fade-transform-leave-to {
  opacity: 0;
  transform: scale(0.98);
}
</style>