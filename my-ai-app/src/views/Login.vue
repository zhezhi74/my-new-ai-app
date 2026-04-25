<template>
  <div class="split-login-container">
    <div class="info-section">
      <div class="info-content">
        <div class="icon-box">
          <el-icon><DataBoard /></el-icon>
        </div>
        
        <h1 class="main-title">
          基于RAG与意图识别的智能医疗<br/>问诊交互系统
        </h1>
        
        <p class="subtitle">
          融合医学知识库与大语言模型技术，为临床决策提供智能化、个性化的辅助建议。
        </p>
        
        <ul class="feature-list">
          <li><span class="dot"></span> 医疗文献检索与深度分析</li>
          <li><span class="dot"></span> 基于患者画像的症状推理</li>
          <li><span class="dot"></span> 智能流式对话与历史追溯</li>
          <li><span class="dot"></span> RAG 检索增强生成的精准问答</li>
        </ul>
      </div>
      
      <div class="footer-info">
        <el-icon><User /></el-icon> {{ currentYear }} 江南大学物联网工程本科毕设 · 张权柄
      </div>
    </div>

    <div class="form-section">
      <div class="form-wrapper">
        <div class="welcome-header">
          <h2 class="welcome-title">欢迎回来</h2>
          <p class="welcome-subtitle">登录您的账户以继续使用</p>
        </div>

        <div class="tab-header">
          <span class="tab active-tab">登 录</span>
          <span class="tab inactive-tab" @click="goToRegister">注 册</span>
        </div>

        <el-form :model="loginForm" :rules="rules" ref="loginFormRef" @keyup.enter="handleLogin" class="custom-form">
          <el-form-item prop="username">
            <el-input v-model="loginForm.username" placeholder="请输入用户名" :prefix-icon="UserIcon" class="dark-input" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" show-password :prefix-icon="Lock" class="dark-input"/>
          </el-form-item>
          
          <el-form-item>
            <el-button class="submit-btn" @click="handleLogin" :loading="isLoading">登 录</el-button>
          </el-form-item>
        </el-form>
        
        <div class="admin-login-link-container">
          <el-link :underline="false" class="admin-link" @click="fillAdminCredentials">
            <el-icon><Setting /></el-icon> 管理员快速登录
          </el-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { User as UserIcon, Lock, DataBoard, Setting, User } from '@element-plus/icons-vue';
import api from '../api';

const router = useRouter();
const loginFormRef = ref(null);
const isLoading = ref(false);
const currentYear = computed(() => new Date().getFullYear());

const loginForm = ref({ username: '', password: '' });
const rules = ref({
  username: [{ required: true, message: '用户名不能为空', trigger: 'blur' }],
  password: [{ required: true, message: '密码不能为空', trigger: 'blur' }],
});

const handleLogin = async () => {
  loginFormRef.value.validate(async (valid) => {
    if (valid) {
      isLoading.value = true;
      try {
        const response = await api.login(loginForm.value.username, loginForm.value.password);
        if (response.data.code === 200) {
          localStorage.setItem('jwt_token', response.data.data.token);
          localStorage.setItem('user_role', response.data.data.role);
          // 修改为无延迟跳转
ElMessage.success('登录成功'); // 可以把文案改短点，因为闪太快看不清
          
router.push('/chat');
        } else {
          ElMessage.error(response.data.message || '登录失败');
        }
      } catch (error) {
        ElMessage.error('服务器连接异常，请检查网络');
      } finally {
        isLoading.value = false;
      }
    }
  });
};

const goToRegister = () => router.push('/register');
const fillAdminCredentials = () => {
  loginForm.value.username = 'zhezhi74';
  loginForm.value.password = '123456'; 
};
</script>

<style scoped>
.split-login-container {
  display: flex;
  height: 100vh;
  width: 100vw;
  background-color: #0B0E14;
  color: #F0F6FC;
  overflow: hidden;
}

/* --- 左侧系统信息区 --- */
.info-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 8%;
  background: radial-gradient(circle at 0% 0%, rgba(0, 245, 255, 0.08) 0%, transparent 50%);
  border-right: 1px solid rgba(255, 255, 255, 0.05);
  position: relative;
}

.icon-box {
  width: 56px;
  height: 56px;
  background: rgba(0, 245, 255, 0.1);
  border: 1px solid rgba(0, 245, 255, 0.3);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: #00F5FF;
  margin-bottom: 30px;
}

/* --- 核心修改：标题楷体样式 --- */
.main-title {
  /* 优先使用楷体，兼容不同系统 */
  font-size: 2.6rem;
  font-weight: 500;
  line-height: 1.4; /* 增加行高，让换行更自然 */
  margin: 0 0 25px 0;
  letter-spacing: 2px;
  color: #FFFFFF;
}

.subtitle {
  font-size: 1.1rem;
  color: #8B949E;
  line-height: 1.8;
  margin-bottom: 50px;
  max-width: 85%;
}

.feature-list { list-style: none; padding: 0; margin: 0; }
.feature-list li {
  font-size: 1rem;
  color: #A3B3CC;
  margin-bottom: 18px;
  display: flex;
  align-items: center;
}
.feature-list .dot {
  width: 6px;
  height: 6px;
  background-color: #00F5FF;
  border-radius: 50%;
  margin-right: 15px;
  box-shadow: 0 0 8px #00F5FF;
}

.footer-info {
  position: absolute;
  bottom: 40px;
  left: 8%;
  font-size: 0.85rem;
  color: #484F58;
  display: flex;
  align-items: center;
  gap: 8px;
}

/* --- 右侧无边框表单区 --- */
.form-section {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #0D1117;
}

.form-wrapper { width: 100%; max-width: 420px; padding: 40px; }
.welcome-title { font-size: 2rem; color: #FFFFFF; margin: 0 0 10px 0; }
.welcome-subtitle { font-size: 0.95rem; color: #8B949E; margin-bottom: 40px; }

.tab-header {
  display: flex;
  gap: 30px;
  margin-bottom: 30px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  padding-bottom: 15px;
}

.tab { font-size: 1.1rem; cursor: pointer; transition: all 0.3s; }
.active-tab { color: #00F5FF; font-weight: bold; position: relative; }
.active-tab::after {
  content: ''; position: absolute; bottom: -16px; left: 0; width: 100%; height: 2px;
  background-color: #00F5FF; box-shadow: 0 -2px 10px rgba(0, 245, 255, 0.5);
}
.inactive-tab { color: #484F58; }

.custom-form :deep(.el-input__wrapper) {
  background-color: #161B22 !important;
  border: 1px solid rgba(255, 255, 255, 0.1) !important;
  box-shadow: none !important;
  border-radius: 8px;
  height: 55px;
}
.custom-form :deep(.el-input__inner) { color: #FFFFFF !important; font-size: 1rem; }

.submit-btn {
  width: 100%; height: 55px; margin-top: 10px;
  background: linear-gradient(135deg, #00F5FF 0%, #00B4D8 100%) !important;
  border: none !important; border-radius: 8px;
  color: #000 !important; font-size: 1.1rem; font-weight: bold;
  letter-spacing: 2px; box-shadow: 0 4px 15px rgba(0, 245, 255, 0.3);
}

.admin-login-link-container { margin-top: 25px; text-align: right; }
.admin-link { color: #8B949E; font-size: 0.9rem; transition: color 0.3s; cursor: pointer; }
.admin-link:hover { color: #00F5FF; }

@media (max-width: 900px) {
  .split-login-container { flex-direction: column; }
  .info-section, .form-section { flex: none; height: 50%; }
  .main-title { font-size: 2rem; }
  .footer-info { display: none; }
}
</style>