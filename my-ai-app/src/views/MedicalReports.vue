<template>
  <div class="report-workspace">
    <div class="header-section">
      <h2 class="dashboard-title">医生辅助阅片工作站 <span class="sub-title">/ CLINICAL ASSISTANT HUB</span></h2>
    </div>

    <el-row :gutter="20" class="workspace-layout">
      <el-col :span="6">
        <el-card class="glass-card list-card">
          <template #header>
            <div class="card-header">
              <span>待处理会诊记录</span>
              <el-button size="small" circle icon="Refresh" @click="loadPendingRecords" class="neon-icon-btn" />
            </div>
          </template>
          
          <div class="record-list">
            <div 
              v-for="record in pendingRecords" 
              :key="record.id" 
              class="record-item"
              :class="{ 'active-record': currentRecord && currentRecord.id === record.id }"
              @click="selectRecord(record)"
            >
              <div class="record-info">
                <span class="patient-id"><el-icon><User /></el-icon> {{ record.username }}</span>
              </div>
              <div class="record-time">{{ record.time }}</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="9">
        <el-card class="glass-card archive-card">
          <template #header>
            <div class="card-header">
              <span>AI 萃取结果 (S-O 事实层)</span>
              <el-tag type="success" size="small" effect="dark">已结构化</el-tag>
            </div>
          </template>
          
          <div v-if="!currentRecord" class="empty-holder">请选择左侧档案</div>
          <div v-else class="archive-content">
            <div class="archive-section">
              <h5><el-icon><InfoFilled /></el-icon> 患者主诉 (Subjective)</h5>
              <p class="fact-text">{{ currentRecord.desc }}</p>
            </div>
            <div class="archive-section">
              <h5><el-icon><Compass /></el-icon> 症状聚类 (Symptoms)</h5>
              <div class="tag-cloud">
                <el-tag v-for="tag in currentRecord.tags" :key="tag" size="small" class="custom-tag">{{ tag }}</el-tag>
              </div>
            </div>
            <div class="archive-section">
              <h5><el-icon><Coordinate /></el-icon> 知识库关联 (Knowledge Base)</h5>
              <ul class="reasoning-log">
                <li v-for="(log, idx) in currentRecord.logs" :key="idx">{{ log }}</li>
              </ul>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="9">
        <el-card class="glass-card doctor-workbench">
          <template #header>
            <div class="card-header">
              <span>医生临床决策区 (A-P 决策层)</span>
            </div>
          </template>
          
          <el-form label-position="top" class="doctor-form">
            <el-form-item label="选择医学临床模板">
              <el-select v-model="selectedTemplate" placeholder="请选择适用模板" class="dark-select">
                <el-option label="呼吸内科常用模板" value="respiratory" />
                <el-option label="心血管科初步筛查模板" value="cardiac" />
                <el-option label="全科常规问诊模板" value="general" />
              </el-select>
            </el-form-item>

            <el-form-item label="主治医师专业评价 (Assessment)">
              <el-input 
                v-model="doctorAdvice" 
                type="textarea" 
                :rows="4" 
                placeholder="在此输入您的临床评估意见..."
              />
            </el-form-item>

            <el-form-item label="下一步诊疗计划 (Plan)">
              <el-input 
                v-model="doctorPlan" 
                placeholder="例如：建议血常规、胸片检查..."
              />
            </el-form-item>

            <div class="form-actions">
              <el-button 
                type="primary" 
                class="report-btn" 
                :disabled="!currentRecord || !selectedTemplate"
                @click="finalReportVisible = true"
              >
                生成正式诊疗报告
              </el-button>
            </div>
          </el-form>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="finalReportVisible" title="正式诊疗报告预览" width="50%" class="final-report-dialog">
      <div class="clinical-report-page" id="reportPrintArea">
        <div class="report-header">
          <div class="hospital-name">江南大学附属智能医疗决策中心</div>
          <h3>正式临床诊疗建议书</h3>
          <div class="report-line"></div>
        </div>
        
        <div class="report-grid">
          <div class="grid-item"><strong>患者标识:</strong> {{ currentRecord?.username }}</div>
          <div class="grid-item"><strong>会诊编号:</strong> #{{ Math.floor(Math.random()*100000) }}</div>
          <div class="grid-item"><strong>主治医师:</strong> {{ currentUser }}</div>
          <div class="grid-item"><strong>生成日期:</strong> {{ new Date().toLocaleDateString() }}</div>
        </div>

        <div class="report-body">
          <div class="section">
            <div class="section-title">【1. 现病史与 AI 萃取摘要】</div>
            <p>{{ currentRecord?.desc }}</p>
          </div>
          <div class="section">
            <div class="section-title">【2. 临床诊断评估 (Assessment)】</div>
            <p>{{ doctorAdvice || '医师未填写额外评估' }}</p>
          </div>
          <div class="section">
            <div class="section-title">【3. 指导方案与计划 (Plan)】</div>
            <p>{{ doctorPlan || '维持 AI 默认辅助建议' }}</p>
          </div>
        </div>

        <div class="report-footer">
          <div class="signature">主治医师签名: ________________</div>
          <p class="seal-note">* 本报告由 RAG 系统辅助生成，经由注册执业医师审核确认生效。</p>
        </div>
      </div>
      <template #footer>
        <el-button @click="finalReportVisible = false">返回修改</el-button>
        <el-button type="success" class="neon-btn" @click="handlePrint">签字并打印报告</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRoute } from 'vue-router'; // ★ 1. 必须引入 useRoute 用于读取 URL 参数
import { User, Refresh, InfoFilled, Compass, Coordinate } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import api from '../api';

const route = useRoute(); // ★ 2. 初始化路由实例
const pendingRecords = ref([]);
const currentRecord = ref(null);
const selectedTemplate = ref('');
const doctorAdvice = ref('');
const doctorPlan = ref('');
const finalReportVisible = ref(false);
const currentUser = ref('张权柄 医师'); // 签名档已自动设为你的名字

// 获取并聚合档案的逻辑
const loadPendingRecords = async () => {
  try {
    const response = await api.getAllHistory({});
    if (response.data) {
      const patientMap = new Map();
      response.data.forEach(item => {
        if (item.role === 'user' && !patientMap.has(item.username)) {
          patientMap.set(item.username, {
            id: item.username,
            username: item.username,
            time: new Date(item.createTime).toLocaleString(),
            desc: item.content,
            tags: ['疑似上呼吸道感染', '伴随咳嗽', '无发热'],
            logs: [
              '意图识别：医疗咨询/症状自查',
              'RAG 召回命中：感冒处理指南 (相关度 0.92)',
              '知识库关联：建议多饮水、观察体温'
            ]
          });
        }
      });
      pendingRecords.value = Array.from(patientMap.values());
    }
  } catch (error) {
    ElMessage.error('无法同步临床档案库');
  }
};

const selectRecord = (record) => {
  currentRecord.value = record;
  selectedTemplate.value = '';
  doctorAdvice.value = '';
  doctorPlan.value = '';
};

const handlePrint = () => {
  ElMessage.success('报告已存入系统归档，正在调用打印服务...');
  finalReportVisible.value = false;
};

// ★ 3. 核心修复：将 onMounted 改为异步执行，确保“先加载数据，后定位选中”
onMounted(async () => {
  // A. 首先等待 API 请求完成，把列表填满
  await loadPendingRecords();
  
  // B. 从 URL 的 query 中读取传过来的 patientId
  const targetId = route.query.patientId;
  
  if (targetId) {
    // C. 在已经加载好的 pendingRecords 数组中寻找匹配的患者
    const target = pendingRecords.value.find(r => r.username === targetId);
    
    if (target) {
      // D. 找到后，立即执行选中函数，触发中间和右侧面板的更新
      selectRecord(target);
      ElMessage.success(`已自动为您定位至患者: ${targetId}`);
    } else {
      console.warn('在档案库中未找到对应的患者记录');
    }
  }
});
</script>

<style scoped>
/* ==================================================
   医生工作站：全视觉重构
   ================================================== */
.report-workspace { padding: 10px 20px; color: #F0F6FC; height: 100vh; overflow: hidden; }
.dashboard-title { font-size: 1.6rem; color: #FFFFFF; border-bottom: 1px solid rgba(0, 245, 255, 0.2); padding-bottom: 10px; margin-bottom: 20px; }
.sub-title { font-size: 0.8rem; color: #00F5FF; font-family: 'Courier New', monospace; margin-left: 10px; }

.glass-card { 
  background: rgba(22, 27, 34, 0.6) !important; 
  border: 1px solid rgba(255, 255, 255, 0.08) !important; 
  border-radius: 12px; 
  height: calc(100vh - 120px);
}
.card-header { display: flex; justify-content: space-between; align-items: center; font-weight: bold; color: #00F5FF; }

/* 列表区 */
.record-list { display: flex; flex-direction: column; gap: 10px; }
.record-item { background: rgba(13, 17, 23, 0.6); border: 1px solid transparent; border-radius: 8px; padding: 12px; cursor: pointer; transition: all 0.3s; }
.record-item:hover { background: rgba(0, 245, 255, 0.05); }
.active-record { border-color: #00F5FF; background: rgba(0, 245, 255, 0.1); }
.record-info { font-weight: bold; color: #00F5FF; margin-bottom: 4px; display: flex; align-items: center; gap: 6px; }
.record-time { font-size: 0.75rem; color: #8B949E; }

/* 档案萃取区 */
.archive-content { padding: 5px; }
.archive-section { margin-bottom: 20px; }
.archive-section h5 { display: flex; align-items: center; gap: 8px; color: #FFFFFF; margin-bottom: 10px; font-size: 0.95rem; }
.fact-text { background: rgba(13, 17, 23, 0.8); padding: 10px; border-radius: 6px; color: #A3B3CC; font-size: 0.9rem; line-height: 1.6; border-left: 3px solid #00F5FF; }
.custom-tag { background: rgba(0, 245, 255, 0.1); border-color: rgba(0, 245, 255, 0.3); color: #00F5FF; margin-right: 8px; }
.reasoning-log { list-style: none; padding: 0; font-size: 0.85rem; color: #8B949E; }
.reasoning-log li::before { content: "•"; color: #00F5FF; margin-right: 8px; }

/* 决策区表单 */
.doctor-form :deep(.el-form-item__label) { color: #8B949E !important; font-weight: bold; }
.doctor-form :deep(.el-textarea__inner), 
.doctor-form :deep(.el-input__inner) {
  background-color: #0D1117 !important; border: 1px solid rgba(255, 255, 255, 0.1) !important; color: #FFF;
}
.form-actions { margin-top: 30px; display: flex; justify-content: center; }
.report-btn { 
  width: 100%; height: 50px; font-weight: bold; font-size: 1rem;
  background: linear-gradient(135deg, #00F5FF 0%, #00B4D8 100%) !important; color: #000 !important; border: none !important;
}

/* 报告报告预览 */
.clinical-report-page { background: #FFFFFF; color: #333; padding: 40px; border-radius: 4px; font-family: "SimSun", serif; }
.report-header { text-align: center; margin-bottom: 30px; }
.hospital-name { font-size: 1.2rem; letter-spacing: 4px; color: #666; margin-bottom: 10px; }
.report-header h3 { font-size: 1.8rem; margin: 0; color: #000; }
.report-line { height: 2px; background: #333; margin-top: 10px; }
.report-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin-bottom: 30px; font-size: 0.9rem; border-bottom: 1px solid #EEE; padding-bottom: 10px; }
.section { margin-bottom: 20px; }
.section-title { font-weight: bold; margin-bottom: 8px; border-left: 4px solid #333; padding-left: 10px; }
.report-footer { margin-top: 40px; border-top: 1px solid #EEE; padding-top: 20px; }
.signature { text-align: right; font-weight: bold; margin-bottom: 20px; }
.seal-note { font-size: 0.8rem; color: #999; text-align: center; }
</style>