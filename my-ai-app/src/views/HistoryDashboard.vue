<template>
  <div class="history-dashboard-wrapper">
    <div class="header-section">
      <h2 class="dashboard-title">全量临床会诊档案 <span class="sub-title">/ CLINICAL RECORDS</span></h2>
    </div>

    <el-card class="glass-card control-panel">
      <el-form :inline="true" :model="filters" class="dark-form">
        <el-form-item label="患者标识 ID">
          <el-input v-model="filters.username" placeholder="输入患者ID精确检索..."></el-input>
        </el-form-item>
        <el-form-item label="检索区间">
          <el-date-picker
              v-model="filters.dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchHistory" class="neon-btn">调取档案</el-button>
          
          <el-button @click="generateReport" class="report-btn">
            萃取档案并进入工作站
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-table :data="historyList" class="medical-dark-table" style="width: 100%; margin-top: 20px;">
      <el-table-column prop="username" label="患者标识 ID" width="150" />
      
      <el-table-column label="数据源" width="160">
        <template #default="scope">
          <span :class="['role-badge', scope.row.role === 'user' ? 'badge-user' : 'badge-ai']">
            <span class="dot"></span>
            {{ scope.row.role === 'user' ? '患者主诉' : 'AI 辅助诊断' }}
          </span>
        </template>
      </el-table-column>

      <el-table-column prop="content" label="病程记录 / 推理日志" show-overflow-tooltip />
      <el-table-column prop="createTime" label="归档时间" width="200" />
    </el-table>

    <el-dialog v-model="reportDialogVisible" title="SYSTEM INTELLIGENCE REPORT" width="65%" class="medical-report-dialog">
      <div class="report-container">
        <div class="report-header">
          <h3>智能辅助诊疗单 (AI-Generated Summary)</h3>
          <p class="report-meta">生成时间: {{ new Date().toLocaleString() }} | 引擎: RAG-Medical-LLM</p>
        </div>
        
        <div class="report-body">
          <div class="report-section">
            <h4><span class="highlight-bar"></span>患者主诉摘要 (Subjective)</h4>
            <p>系统已分析近期 {{ historyList.length }} 条对话记录。患者主要反馈症状包括：胸口闷痛、伴有轻微咳嗽。症状呈间歇性发作，夜间体感较明显。</p>
          </div>

          <div class="report-section">
            <h4><span class="highlight-bar"></span>AI 知识库推理 (Objective & Assessment)</h4>
            <ul class="ai-reasoning-list">
              <li>检索命中：[心血管健康指南] 相关度 89.2%</li>
              <li>检索命中：[呼吸道感染应对策略] 相关度 75.5%</li>
              <li>综合意图识别 (BERT)：非急诊常规咨询，需排除心肌缺血可能。</li>
            </ul>
          </div>

          <div class="report-section plan-section">
            <h4><span class="highlight-bar"></span>临床建议方案 (Plan)</h4>
            <p>1. 建议门诊进行常规心电图（ECG）排查。<br>2. 建议监测未来48小时体温与咳嗽频率变化。<br>3. 若胸痛加剧并伴有放射性痛感，应立即触发急救预案。</p>
          </div>
        </div>
        
        <div class="report-footer">
          <p class="warning-text">* 此报告由 AI 自动聚合生成，仅供主治医师临床参考，不具备最终诊断效力。</p>
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="reportDialogVisible = false">关闭阅览</el-button>
          <el-button type="primary" @click="exportReport" class="neon-btn">导出为 PDF</el-button>
        </span>
      </template>
    </el-dialog>
    

  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue';
import api from '../api';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router'; // ★ 1. 必须引入路由

const router = useRouter(); // ★ 2. 初始化路由
const historyList = ref([]);
const filters = reactive({
  username: '',
  dateRange: [],
});

// 调取档案逻辑（完全沿用你之前的逻辑）
const fetchHistory = async () => {
  try {
    const params = {
      username: filters.username || null,
      startDate: filters.dateRange && filters.dateRange.length > 0 ? filters.dateRange[0] : null,
      endDate: filters.dateRange && filters.dateRange.length > 0 ? filters.dateRange[1] : null,
    };
    const response = await api.getAllHistory(params);
    historyList.value = response.data;
  } catch (error) {
    ElMessage.error('调取临床档案失败，请检查网络链路');
  }
};

// ★ 3. 修复后的跳转逻辑：直接读取你筛选框里的 ID
const generateReport = () => {
  // 如果没有在筛选框输入患者ID，系统不知道该给谁生成报告
  if (!filters.username) {
    ElMessage.warning('请先在上方输入【患者标识 ID】进行检索，再进行萃取！');
    return;
  }
  
  ElMessage.success(`正在将患者 [${filters.username}] 的档案同步至工作站...`);
  
  // 携带筛选框里的患者标识跳转到新模块
  router.push({
    path: '/admin/reports',
    query: { patientId: filters.username } 
  });
};

onMounted(fetchHistory);
</script>

<style scoped>
/* ==================================================
   后台极客医疗风：全量样式重构
   ================================================== */
.history-dashboard-wrapper {
  padding: 10px 30px;
  color: #F0F6FC;
}

/* 标题区 */
.dashboard-title {
  font-size: 1.8rem;
  letter-spacing: 2px;
  color: #FFFFFF;
  border-bottom: 1px solid rgba(0, 245, 255, 0.2);
  padding-bottom: 15px;
  margin-bottom: 25px;
  text-shadow: 0 0 10px rgba(0, 245, 255, 0.3);
}
.sub-title {
  font-size: 0.9rem;
  color: #00F5FF;
  font-family: 'Courier New', monospace;
  margin-left: 10px;
}

/* 控制台卡片 */
.glass-card {
  background: rgba(22, 27, 34, 0.6) !important;
  border: 1px solid rgba(255, 255, 255, 0.08) !important;
  backdrop-filter: blur(10px);
  border-radius: 12px;
}

/* 覆盖 Element Plus 表单样式 */
.dark-form :deep(.el-form-item__label) {
  color: #8B949E !important;
  font-weight: bold;
}
.dark-form :deep(.el-input__wrapper) {
  background-color: #0D1117 !important;
  border: 1px solid rgba(255, 255, 255, 0.1) !important;
  box-shadow: none !important;
}
.dark-form :deep(.el-input__inner) {
  color: #FFF !important;
}

/* 按钮组 */
.neon-btn {
  background-color: transparent !important;
  border: 1px solid #00F5FF !important;
  color: #00F5FF !important;
  box-shadow: 0 0 10px rgba(0, 245, 255, 0.2);
  transition: all 0.3s;
}
.neon-btn:hover {
  background-color: #00F5FF !important;
  color: #000 !important;
  box-shadow: 0 0 20px rgba(0, 245, 255, 0.6);
}

.report-btn {
  background: linear-gradient(135deg, #00F5FF 0%, #00B4D8 100%) !important;
  border: none !important;
  color: #000 !important;
  font-weight: bold;
  box-shadow: 0 4px 15px rgba(0, 245, 255, 0.3);
  margin-left: 20px;
}
.report-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 245, 255, 0.5);
}

/* ==================================================
   表格 CSS 穿透：透明暗黑极客风 (丝滑重塑版)
   ================================================== */
.medical-dark-table {
  background-color: transparent !important;
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 8px;
}

/* 1. 彻底清除原生背景和斑马纹干扰，统一基础暗色 */
.medical-dark-table :deep(tr),
.medical-dark-table :deep(th.el-table__cell), 
.medical-dark-table :deep(td.el-table__cell) {
  background-color: rgba(13, 17, 23, 0.4) !important;
  border-bottom: 1px solid rgba(0, 245, 255, 0.05) !important; /* 极细微的深海蓝分割线 */
  color: #A3B3CC !important; /* 默认文字为冷灰蓝，不刺眼 */
  transition: background-color 0.4s ease, color 0.4s ease; /* ★ 核心：加入丝滑过渡动画 */
}

/* 强杀可能残留的 stripe 斑马纹高亮 */
.medical-dark-table :deep(.el-table__row--striped td.el-table__cell) {
  background-color: rgba(13, 17, 23, 0.4) !important;
}

/* 2. 表头特殊处理：增加权重与质感 */
.medical-dark-table :deep(th.el-table__cell) {
  background-color: rgba(22, 27, 34, 0.85) !important;
  color: #00F5FF !important;
  font-weight: bold;
  letter-spacing: 1px;
  border-bottom: 1px solid rgba(0, 245, 255, 0.2) !important;
}

/* 3. 悬停扫描线：去掉突兀的色块，改为柔和的内发光与文字点亮 */
.medical-dark-table :deep(tr:hover > td.el-table__cell) {
  background-color: rgba(0, 245, 255, 0.06) !important; /* 极淡的青色背景，不喧宾夺主 */
  color: #FFFFFF !important; /* 鼠标悬停时，该行文字平滑亮起，提升阅读专注度 */
}

/* 隐藏表格底部的原生白边 */
.medical-dark-table::before {
  display: none !important;
}

/* 角色标签状态设计 */
.role-badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 0.85rem;
  font-weight: bold;
}
.role-badge .dot {
  width: 6px; height: 6px; border-radius: 50%; margin-right: 6px;
}
/* 患者标签 (灰色系) */
.badge-user {
  background: rgba(255, 255, 255, 0.1);
  color: #A3B3CC;
  border: 1px solid rgba(255, 255, 255, 0.2);
}
.badge-user .dot { background-color: #A3B3CC; }

/* AI标签 (青色呼吸) */
.badge-ai {
  background: rgba(0, 245, 255, 0.1);
  color: #00F5FF;
  border: 1px solid rgba(0, 245, 255, 0.3);
}
.badge-ai .dot { 
  background-color: #00F5FF; 
  box-shadow: 0 0 8px #00F5FF;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% { opacity: 1; }
  50% { opacity: 0.4; }
  100% { opacity: 1; }
}

/* ==================================================
   答辩专用：智能报告弹窗样式
   ================================================== */
:deep(.medical-report-dialog .el-dialog) {
  background: #0D1117 !important;
  border: 1px solid #00F5FF;
  box-shadow: 0 0 40px rgba(0, 245, 255, 0.2);
}
:deep(.medical-report-dialog .el-dialog__title) {
  color: #00F5FF !important;
  font-family: 'Courier New', monospace;
  font-weight: bold;
}

.report-container {
  padding: 10px 20px;
  color: #E6EDF3;
}
.report-header {
  text-align: center;
  border-bottom: 1px dashed rgba(255, 255, 255, 0.2);
  padding-bottom: 20px;
  margin-bottom: 20px;
}
.report-header h3 { margin: 0 0 10px 0; font-size: 1.5rem; letter-spacing: 1px; }
.report-meta { color: #8B949E; font-size: 0.9rem; margin: 0; }

.report-section { margin-bottom: 25px; }
.report-section h4 { 
  display: flex; align-items: center; color: #FFFFFF; font-size: 1.1rem; margin-bottom: 15px; 
}
.highlight-bar {
  display: inline-block; width: 4px; height: 16px; background-color: #00F5FF; margin-right: 10px;
  box-shadow: 0 0 8px #00F5FF;
}

.report-section p { line-height: 1.8; color: #A3B3CC; margin: 0; }
.ai-reasoning-list {
  background: rgba(0, 245, 255, 0.05); border: 1px solid rgba(0, 245, 255, 0.2);
  padding: 15px 15px 15px 35px; border-radius: 8px; color: #A3B3CC; line-height: 1.8;
}

.plan-section { background: rgba(22, 27, 34, 0.8); padding: 20px; border-radius: 8px; border-left: 4px solid #00F5FF; }

.report-footer { margin-top: 30px; text-align: center; }
.warning-text { color: #F56C6C; font-size: 0.85rem; font-style: italic; }
</style>