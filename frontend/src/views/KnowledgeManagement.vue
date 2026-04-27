<template>
  <div class="editorial-knowledge-base">
    
    <div class="hero-banner">
      <img :src="heroImgUrl" class="hero-bg" alt="Hero Background">
      <div class="hero-overlay"></div>
      <div class="hero-content">
        <h1 class="hero-title">RAG 核心知识库</h1>
        <p class="hero-subtitle">SYSTEM KNOWLEDGE ARSENAL & DATA MATRIX</p>
        <div class="system-status">
          <div class="status-item"><span class="glow-dot"></span>当前挂载向量: 124,592</div>
          <div class="status-item"><span class="glow-dot"></span>微调引擎: v2.1-Beta</div>
        </div>
      </div>
    </div>

    <div class="filter-section">
      <div v-if="currentFolder" class="breadcrumb-nav" @click="backToFolders">
        <el-icon><ArrowLeft /></el-icon> 返回全局矩阵
      </div>
      
      <div v-else class="folder-title">
        选择知识基座集群 <span>/ SELECT KNOWLEDGE CLUSTER</span>
      </div>
      
      <div class="action-buttons">
        <el-button type="primary" class="neon-btn add-btn" @click="openAddDialog">
          <el-icon><Plus /></el-icon> 
          {{ currentFolder ? '注入新数据' : '注入基座集群' }}
        </el-button>
      </div>
    </div>

    <div v-if="!currentFolder" class="folder-grid">
      <div 
        v-for="folder in categoryFolders" 
        :key="folder.id" 
        class="folder-card"
        @click="enterFolder(folder.id)"
      >
        <div class="folder-bg" :style="{ backgroundImage: `url(${folder.img})` }"></div>
        <div class="folder-overlay">
          <h2 class="folder-name">{{ folder.title }}</h2>
          <p class="folder-desc">{{ folder.desc }}</p>
          <div class="folder-action">
            进入集群 <el-icon><Right /></el-icon>
          </div>
        </div>
      </div>
    </div>

    <div v-else class="data-list-container">
      <div 
        v-for="item in filteredKnowledge" 
        :key="item.id" 
        class="data-row"
        @click="openDossier(item)"
      >
        <div class="row-left">
          <div class="row-type-indicator" :class="item.type"></div>
          <div class="row-content">
            <h3 class="row-title">{{ item.title }}</h3>
            <p class="row-excerpt">{{ item.content }}</p>
          </div>
        </div>
        <div class="row-right">
          <span class="row-date">{{ item.createTime || '2026-04-24' }}</span>
          <el-icon class="row-arrow"><TopRight /></el-icon>
        </div>
      </div>
      
      <div v-if="filteredKnowledge.length === 0" class="empty-data">
        <el-icon :size="40"><Warning /></el-icon>
        <p>该集群当前未挂载任何向量或规则</p>
      </div>
    </div>

   

    <el-drawer v-model="drawerVisible" size="55%" :with-header="false" class="cyber-drawer" destroy-on-close>
      <div v-if="currentKnowledge" class="dossier-container">
        <div 
  class="dossier-hero" 
  :class="{ 'text-only-hero': !currentKnowledge.imageUrl }"
  :style="currentKnowledge.imageUrl ? { backgroundImage: `url(${currentKnowledge.imageUrl})` } : {}"
>
          <div class="dossier-hero-overlay">
            <div class="dossier-controls">
              <el-button link class="close-btn" @click="drawerVisible = false">
                <el-icon><Close /></el-icon> 返回矩阵
              </el-button>
              <div class="mode-switch">
                <span :class="{'active': !isEditing}">阅读</span>
                <el-switch v-model="isEditing" style="--el-switch-on-color: #00F5FF; --el-switch-off-color: #4C4D4F" />
                <span :class="{'active': isEditing}">编辑</span>
              </div>
            </div>
            <h2 class="dossier-title" v-if="!isEditing">{{ currentKnowledge.title }}</h2>
            <el-input v-else v-model="currentKnowledge.title" class="title-input edit-mode-input" />
          </div>
        </div>

        <div class="dossier-body">
          <div class="dossier-content">
            <div class="section-label">01 // CORE DATA CONTENT</div>
            <div v-if="!isEditing" class="markdown-reader">{{ currentKnowledge.content }}</div>
            <div v-else class="editor-workspace">
              <el-input v-model="currentKnowledge.content" type="textarea" :rows="15" class="cyber-textarea edit-mode-input" />
              <div class="editor-actions">
                <el-button type="primary" class="neon-btn" @click="submitUpdate">
                  <el-icon><Upload /></el-icon> 同步至向量引擎 (保存更新)
                </el-button>
              </div>
            </div>
          </div>
          <div class="dossier-sidebar">
            <div class="section-label">02 // METADATA & STATUS</div>
            <div class="meta-card">
              <div class="meta-item"><span class="meta-label">向量维度 (Dimensions)</span><span class="meta-value">1,024</span></div>
              <div class="meta-item"><span class="meta-label">分块策略 (Chunking)</span><span class="meta-value">512 Tokens / 跨度 50</span></div>
              <div class="meta-item"><span class="meta-label">挂载时间</span><span class="meta-value">{{ currentKnowledge.createTime || '2026-04-24 14:32:00' }}</span></div>
              <div class="meta-item"><span class="meta-label">活跃度 (RAG Hit Rate)</span><el-progress :percentage="85" color="#00F5FF" :stroke-width="8" /></div>
            </div>
            <div class="danger-zone">
              <div class="section-label" style="color: #F56C6C;">03 // DANGER ZONE</div>
              <p class="danger-tip">此操作将从系统中抹除该知识点，并同步从底层向量库中卸载。</p>
              <el-button type="danger" class="cyber-danger-btn" @click="handleDelete(currentKnowledge.id)">
                <el-icon><Delete /></el-icon> 物理擦除记录
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </el-drawer>

    <el-dialog 
      v-model="addDialogVisible" 
      title="[ 数据注入与向量化序列 ]" 
      width="650px" 
      class="cyber-dialog"
      :close-on-click-modal="false"
      @closed="resetInjection"
    >
      <div class="injection-pipeline">
        <div v-if="injectionStep === 0" class="pipeline-step-1">
          <el-form :model="newKnowledge" label-position="top" class="dark-form">
            <el-row :gutter="20">
              <el-col :span="11">
                <el-form-item label="载荷标识 (Title)" prop="title">
          <el-input 
            v-model="newKnowledge.title" 
            :placeholder="dynamicPlaceholders.title" 
            class="neon-input">
          </el-input>
        </el-form-item>
              </el-col>
              
            </el-row>
            <el-form-item label="数据矩阵 (Content)" prop="content">
          <el-input 
            type="textarea" 
            v-model="newKnowledge.content" 
            :rows="5" 
            :placeholder="dynamicPlaceholders.content" 
            class="neon-input">
          </el-input>
        </el-form-item>
          </el-form>
          
          <div class="pipeline-actions">
            <el-upload :auto-upload="false" :show-file-list="false" :on-change="handleFileChange" class="upload-btn">
              <el-button class="ghost-btn"><el-icon><Document /></el-icon> 解析本地 TXT</el-button>
            </el-upload>
            <el-button type="primary" class="neon-btn" @click="startVectorization">
              <el-icon><VideoPlay /></el-icon> 启动向量化引擎
            </el-button>
          </div>
        </div>

        <div v-if="injectionStep > 0" class="pipeline-step-2">
          <div class="terminal-window">
            <div class="terminal-header">
              <span class="dot red"></span><span class="dot yellow"></span><span class="dot green"></span>
              <span class="terminal-title">bash - vector-engine - root@sys</span>
            </div>
            <div class="terminal-body" ref="terminalBody">
              <div v-for="(log, idx) in consoleLogs" :key="idx" 
                   :class="{'log-success': log.includes('[SUCCESS]'), 'log-error': log.includes('[ERROR]')}">
                <span class="prompt-arrow">></span> {{ log }}
              </div>
              <div v-if="injectionStep === 1" class="cursor-blink">_</div>
            </div>
          </div>
          <div class="pipeline-actions" style="margin-top: 20px; justify-content: flex-end;" v-if="injectionStep === 2">
            <el-button type="primary" class="neon-btn" @click="finishInjection">确认并挂载至矩阵</el-button>
          </div>
        </div>
      </div>
    </el-dialog>

    <el-dialog 
      v-model="clusterDialogVisible" 
      title="[ 定义新基座集群元数据 ]" 
      width="500px" 
      class="cyber-dialog"
    >
      <div class="cluster-injection-form">
        <el-form :model="newCluster" label-position="top" class="dark-form">
          <el-form-item label="集群核心名称 (Cluster Identity)">
            <el-input v-model="newCluster.title" placeholder="例如：中药配伍禁忌知识库" />
          </el-form-item>
          <el-form-item label="功能描述 (Function Descriptor)">
            <el-input v-model="newCluster.desc" type="textarea" :rows="3" placeholder="描述该集群在 RAG 检索中的作用域..." />
          </el-form-item>
        </el-form>
        <div class="pipeline-actions" style="justify-content: flex-end; margin-top: 20px;">
          <el-button type="primary" class="neon-btn" @click="submitCluster">启动基座挂载</el-button>
        </div>
      </div>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive, nextTick } from 'vue';
import { Plus, TopRight, Close, Delete, Upload, VideoPlay, Document, ArrowLeft, Right, Warning } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import api from '../api';

const heroImgUrl = new URL('../assets/images/zhishiku0.png', import.meta.url).href;
const defaultCoverUrl = new URL('../assets/images/zhishiku1.png', import.meta.url).href;

const activeCategory = ref('all');
// ========== 新增：双层钻取导航状态 ==========
// null 表示当前在“第一层：大类入口”，有值（如 'rag'）表示在“第二层：具体数据列表”
const currentFolder = ref(null); 

// 第一层的大类卡片配置 (绑定你之前找好的科幻图)
const categoryFolders = ref([
  { 
    id: 'rag', 
    title: 'RAG 检索源', 
    desc: '包含临床指南、药品说明书等高维向量化医学语料。', 
    img: new URL('../assets/images/rag.png', import.meta.url).href 
  },
  { 
    id: 'template', 
    title: '诊疗思维链模板', // 【核心修复】：加上“思维链”三个字，精准触发后端的 CoT 逻辑
    desc: '结构化的 SOAP 报告模板与标准临床路径骨架 (JSON/Markdown)。', 
    img: new URL('../assets/images/zhishiku1.png', import.meta.url).href 
  },
  { 
    id: 'intent', 
    title: '意图识别语料', 
    desc: '用于微调 BERT 模型的口语化患者主诉样本与实体映射。', 
    img: new URL('../assets/images/zhishiku3.png', import.meta.url).href 
  },
  { 
    id: 'blacklist', 
    title: '幻觉抑制锁', 
    desc: '最高安全级别：严禁大模型生成的越权处方与虚假医学结论拦截规则。', 
    img: new URL('../assets/images/zhishiku2.png', import.meta.url).href 
  }
]);

const clusterDialogVisible = ref(false); // 控制“注入基座集群”弹窗的显示/隐藏
const newCluster = reactive({ title: '', desc: '' }); // 存储新集群的表单数据

// 进入某个具体分类
const enterFolder = (categoryId) => {
  activeCategory.value = categoryId; // 联动底层的筛选器
  currentFolder.value = categoryId;
};

// 返回第一层分类矩阵
const backToFolders = () => {
  currentFolder.value = null;
  activeCategory.value = 'all';
};
const knowledgeList = ref([]);
const drawerVisible = ref(false);
const currentKnowledge = ref(null);
const isEditing = ref(false);

// ========== 数据注入状态管理 ==========
const addDialogVisible = ref(false);
const injectionStep = ref(0); // 0: 输入, 1: 向量化模拟中, 2: 完成
const consoleLogs = ref([]);
const terminalBody = ref(null);
const newKnowledge = reactive({ title: '', content: '', type: 'rag' });
// ==================================================
//  动态感知：根据当前所在集群，切换输入框提示语
// ==================================================
const dynamicPlaceholders = computed(() => {
  const folderId = currentFolder.value;
  switch (folderId) {
    case 'blacklist':
      return { 
        title: '例如：禁止推荐未经证实的偏方', 
        content: '例如：明确声明AI无法替代真实医生的处方，遇到严重症状强制建议线下就医...' 
      };
    case 'template':
      return { 
        title: '例如：发热问诊标准路径 / SOAP模板', 
        content: '例如：1. 询问体温与持续时间； 2. 排查伴随症状； 3. 给出居家观察或就医建议...' 
      };
    case 'intent':
      return { 
        title: '例如：心悸用药咨询', 
        content: '例如：患者原话“我最近心跳很快，需要吃什么药吗？”（用于扩充BERT意图识别的微调语料）' 
      };
    case 'rag':
    default:
      return { 
        title: '例如：2026心血管疾病用药指南', 
        content: '例如：阿司匹林主要用于抑制血小板聚集，预防心肌梗死等心血管事件发生...' 
      };
  }
});

// ========== 辅助函数：智能分类器与视觉映射 ==========
const getVisualConfig = (item, index) => {
  let itemType = item.type;
  
  // 【核心修复1】：优先读取 Java 后端传来的 category 字段！
  if (!itemType && item.category) {
    if (item.category.includes('黑名单') || item.category.includes('幻觉')) itemType = 'blacklist';
    else if (item.category.includes('模板') || item.category.includes('思维链')) itemType = 'template';
    else if (item.category.includes('意图')) itemType = 'intent';
    else if (item.category.includes('RAG')) itemType = 'rag';
  }

  // 如果连 category 都没有，再用 title 兜底
  if (!itemType) {
    const title = item.title || '';
    if (/禁止|幻觉|黑名单|拦截|驳回|越权/.test(title)) itemType = 'blacklist';
    else if (/模板|骨架|SOAP|格式/.test(title)) itemType = 'template';
    else if (/意图|语料|映射|样本/.test(title)) itemType = 'intent';
    else itemType = 'rag'; 
  }

  switch (itemType) {
    case 'blacklist': return { type: 'blacklist', typeLabel: '幻觉抑制锁', imageUrl: new URL('../assets/images/zhishiku2.png', import.meta.url).href };
    case 'template': return { type: 'template', typeLabel: '诊疗模板骨架', imageUrl: new URL('../assets/images/zhishiku1.png', import.meta.url).href };
    case 'intent': return { type: 'intent', typeLabel: '意图识别语料', imageUrl: new URL('../assets/images/zhishiku3.png', import.meta.url).href };
    case 'rag':
    default: return { type: 'rag', typeLabel: 'RAG 检索源', imageUrl: new URL('../assets/images/rag.png', import.meta.url).href };
  }
};

// ========== API Read (重构后的加载逻辑) ==========
const fetchKnowledge = async () => {
  try {
    const response = await api.getAllKnowledge();
    // 使用上面的智能分类器来处理后端返回的真实数据
    knowledgeList.value = response.data.map((item, index) => {
      const config = getVisualConfig(item, index);
      return {
        ...item,
        type: config.type,
        typeLabel: config.typeLabel,
        // 优先使用后端返回的 imageUrl（为你日后的自定义上传留好后路），如果没有则用本地分配的图
        imageUrl: item.imageUrl || config.imageUrl 
      };
    });
  } catch (error) {
    ElMessage.error('底层引擎掉线，系统进入脱机演示模式');
    // 如果后端挂了，这里使用假数据兜底...
  }
};



const filteredKnowledge = computed(() => {
  if (activeCategory.value === 'all') return knowledgeList.value;
  return knowledgeList.value.filter(item => item.type === activeCategory.value);
});

// ========== Drawer 逻辑 ==========
const openDossier = (item) => {
  currentKnowledge.value = { ...item };
  isEditing.value = false;
  drawerVisible.value = true;
};

// ========== API Update & Delete ==========
const submitUpdate = async () => {
  try {
    await api.updateKnowledge(currentKnowledge.value);
    ElMessage.success('底层指令更新成功，已同步至向量引擎');
    isEditing.value = false;
    drawerVisible.value = false;
    fetchKnowledge();
  } catch (error) { ElMessage.error('同步失败'); }
};

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定要进行物理擦除吗？此操作不可逆！', '高危操作', { confirmButtonText: '强制擦除', type: 'error' });
    await api.deleteKnowledge(id);
    ElMessage.success('数据记录已被物理擦除');
    drawerVisible.value = false;
    fetchKnowledge();
  } catch (error) { }
};

// ========== 2. 统一调度入口：根据当前层级打开不同弹窗 ==========
const openAddDialog = () => {
  if (currentFolder.value) {
    // 在子页：打开现有的“数据注入流水线”
    addDialogVisible.value = true;
    // 自动将新数据的类型锁定为当前所在分类
    newKnowledge.type = currentFolder.value; 
  } else {
    // 在首页：打开新做的“基座集群注入”弹窗
    clusterDialogVisible.value = true;
  }
};

// ========== 3. 新功能：提交新集群 (前端模拟) ==========
const submitCluster = () => {
  if (!newCluster.title || !newCluster.desc) {
    ElMessage.warning('集群元数据定义不完整');
    return;
  }
  
  // 生成一个唯一的 ID
  const newId = 'custom_' + Date.now();
  
  // 向首页矩阵添加新卡片 (使用一张通用的科技背景)
  categoryFolders.value.push({
    id: newId,
    title: newCluster.title,
    desc: newCluster.desc,
    img: defaultCoverUrl // 默认使用你之前的科技大图
  });
  
  ElMessage.success(`基座集群 [${newCluster.title}] 已挂载至系统主矩阵`);
  clusterDialogVisible.value = false;
  
  // 重置表单
  newCluster.title = '';
  newCluster.desc = '';
};

const resetInjection = () => {
  injectionStep.value = 0;
  consoleLogs.value = [];
  newKnowledge.title = '';
  newKnowledge.content = '';
  newKnowledge.type = 'rag';
};

// 本地读取 TXT 到文本框，替代以前的静默批量上传，视觉上更友好
const handleFileChange = (file) => {
  const reader = new FileReader();
  reader.onload = (e) => {
    newKnowledge.content = e.target.result;
    ElMessage.success('TXT 文件读取完毕，请检查语料并启动向量化');
  };
  reader.readAsText(file.raw);
};

// 伪装的极客控制台滚动函数
const pushLog = async (msg) => {
  consoleLogs.value.push(msg);
  await nextTick();
  if (terminalBody.value) terminalBody.value.scrollTop = terminalBody.value.scrollHeight;
};

// ==================================================
//  极速版：终端日志瞬间注入 (无延迟版)
// ==================================================
const startVectorization = async () => {
  if (!newKnowledge.title || !newKnowledge.content) {
    ElMessage.warning('请填写完整的载荷信息');
    return;
  }

  injectionStep.value = 1;
  consoleLogs.value = [];

  // 【核心修复2】：删除所有 delay 延迟，直接满速连发指令
  await pushLog('[SYSTEM] 初始化向量化引擎...');
  await pushLog(`[TASK] 目标载荷: ${newKnowledge.title}`);
  await pushLog('[PROCESS] 启动 BGE-M3 嵌入模型...');
  await pushLog('[NETWORK] 分配 Weaviate 物理隔离存储索引...');

  try {
    let categoryTitle = '默认分类';
    if (currentFolder.value) {
      const folderObj = categoryFolders.value.find(f => f.id === currentFolder.value);
      if (folderObj) categoryTitle = folderObj.title;
    }

    // 这里是唯一耗时的地方（取决于你的网速和 BGE-M3 向量化速度）
    await api.addKnowledge({ 
      title: newKnowledge.title, 
      content: newKnowledge.content,
      category: categoryTitle 
    });
    
    await pushLog('[SUCCESS] 200 OK: 数据已硬核写入 MySQL & Weaviate！');
    
    // API 请求一完成，瞬间切到“确认并挂载”状态
    injectionStep.value = 2;
    
  } catch (error) {
    await pushLog(`[ERROR] 注入失败: ${error.message}`);
  }
};

const finishInjection = () => {
  addDialogVisible.value = false;
  fetchKnowledge(); // 刷新外面的瀑布流，展示刚加的数据
};

onMounted(fetchKnowledge);
</script>

<style scoped>
/* 原有瀑布流 & 抽屉样式保持原样 (节约篇幅，此处省略你之前那些完美生效的 CSS) */
.editorial-knowledge-base { color: #E6EDF3; padding-bottom: 50px; }
.hero-banner { position: relative; width: 100%; height: 320px; border-radius: 16px; overflow: hidden; margin-bottom: 30px; box-shadow: 0 10px 30px rgba(0, 0, 0, 0.5); }
.hero-bg { width: 100%; height: 100%; object-fit: cover; filter: brightness(1.2); }
.hero-overlay { position: absolute; top: 0; left: 0; right: 0; bottom: 0; background: linear-gradient(to right, rgba(13, 17, 23, 0.9) 0%, rgba(13, 17, 23, 0.4) 100%); }
.hero-content { position: absolute; top: 50%; left: 50px; transform: translateY(-50%); z-index: 2; }
.hero-title { font-size: 2.8rem; margin: 0 0 10px 0; color: #FFFFFF; letter-spacing: 2px; text-shadow: 0 0 20px rgba(0, 245, 255, 0.4); }
.hero-subtitle { font-family: 'Courier New', monospace; color: #00F5FF; font-size: 1.1rem; letter-spacing: 3px; margin-bottom: 25px; }
.system-status { display: flex; gap: 20px; }
.status-item { background: rgba(0, 245, 255, 0.1); border: 1px solid rgba(0, 245, 255, 0.3); padding: 6px 15px; border-radius: 20px; font-size: 0.9rem; color: #A3B3CC; display: flex; align-items: center; backdrop-filter: blur(5px); }
.glow-dot { width: 8px; height: 8px; background-color: #00F5FF; border-radius: 50%; margin-right: 8px; box-shadow: 0 0 8px #00F5FF; animation: pulse 2s infinite; }
.filter-section { display: flex; justify-content: space-between; align-items: center; margin-bottom: 30px; padding: 0 10px; }
.cyber-radio-group :deep(.el-radio-button__inner) { background-color: transparent !important; border: 1px solid rgba(255, 255, 255, 0.1) !important; color: #8B949E; transition: all 0.3s; }
.cyber-radio-group :deep(.el-radio-button:first-child .el-radio-button__inner) { border-radius: 8px 0 0 8px; }
.cyber-radio-group :deep(.el-radio-button:last-child .el-radio-button__inner) { border-radius: 0 8px 8px 0; }
.cyber-radio-group :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) { background-color: rgba(0, 245, 255, 0.1) !important; color: #00F5FF; border-color: #00F5FF !important; box-shadow: -1px 0 0 0 #00F5FF; }
.neon-btn { background: transparent !important; border: 1px solid #00F5FF !important; color: #00F5FF !important; }
.neon-btn:hover { background: #00F5FF !important; color: #000 !important; box-shadow: 0 0 15px rgba(0, 245, 255, 0.5); }
.masonry-grid { column-count: 4; column-gap: 20px; }
@media (max-width: 1400px) { .masonry-grid { column-count: 3; } }
@media (max-width: 1000px) { .masonry-grid { column-count: 2; } }
.masonry-item { break-inside: avoid; margin-bottom: 20px; cursor: pointer; }
.editorial-card { background: #0D1117 !important; border: 1px solid rgba(255, 255, 255, 0.05) !important; border-radius: 12px; overflow: hidden; transition: transform 0.4s cubic-bezier(0.165, 0.84, 0.44, 1), box-shadow 0.4s; }
.editorial-card:hover { transform: translateY(-5px); border-color: rgba(0, 245, 255, 0.3) !important; box-shadow: 0 10px 20px rgba(0, 0, 0, 0.4), 0 0 15px rgba(0, 245, 255, 0.1); }
.editorial-card :deep(.el-card__body) { padding: 0; }
.card-image-wrapper { position: relative; width: 100%; height: 180px; overflow: hidden; }
.card-image { width: 100%; height: 100%; object-fit: cover; transition: transform 0.6s ease; }
.editorial-card:hover .card-image { transform: scale(1.05); }
.type-badge { position: absolute; top: 15px; left: 15px; background: rgba(0, 0, 0, 0.6); backdrop-filter: blur(4px); color: #00F5FF; padding: 4px 10px; border-radius: 4px; font-size: 0.75rem; font-weight: bold; border: 1px solid rgba(0, 245, 255, 0.2); }
.standalone-badge { position: relative; display: inline-block; top: 0; left: 0; margin-bottom: 15px; }
.editorial-card.blacklist .type-badge { color: #F56C6C; border-color: rgba(245, 108, 108, 0.3); }
.card-body { padding: 20px; }
.card-title { color: #FFFFFF; font-size: 1.1rem; margin: 0 0 10px 0; line-height: 1.4; }
.editorial-card.blacklist .card-title { color: #F56C6C; }
.card-excerpt { color: #8B949E; font-size: 0.9rem; line-height: 1.6; margin: 0 0 20px 0; display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden; }
.card-footer { display: flex; justify-content: space-between; align-items: center; border-top: 1px solid rgba(255, 255, 255, 0.05); padding-top: 15px; }
.card-date { font-family: 'Courier New', monospace; font-size: 0.8rem; color: #6E7681; }
.arrow-icon { color: #00F5FF; font-size: 1.2rem; opacity: 0; transform: translateX(-10px); transition: all 0.3s; }
.editorial-card:hover .arrow-icon { opacity: 1; transform: translateX(0); }
@keyframes pulse { 0% { opacity: 1; box-shadow: 0 0 8px #00F5FF; } 50% { opacity: 0.4; box-shadow: 0 0 2px #00F5FF; } 100% { opacity: 1; box-shadow: 0 0 8px #00F5FF; } }
:deep(.cyber-drawer) { background-color: #0D1117 !important; border-left: 1px solid rgba(0, 245, 255, 0.3); box-shadow: -10px 0 30px rgba(0, 0, 0, 0.8); }
:deep(.cyber-drawer .el-drawer__body) { padding: 0; overflow-y: auto; }
.dossier-container { display: flex; flex-direction: column; min-height: 100%; color: #E6EDF3; }
.dossier-hero { height: 240px;flex-shrink: 0; background-size: cover; background-position: center; position: relative; }
.dossier-hero-overlay { position: absolute; top: 0; left: 0; right: 0; bottom: 0; background: linear-gradient(to bottom, rgba(13, 17, 23, 0.2) 0%, #0D1117 100%); padding: 30px; display: flex; flex-direction: column; justify-content: space-between; }
.dossier-controls { display: flex; justify-content: space-between; align-items: center; }
.close-btn { color: #A3B3CC !important; font-size: 1rem; }
.close-btn:hover { color: #00F5FF !important; }
.mode-switch { display: flex; align-items: center; gap: 10px; font-size: 0.85rem; color: #6E7681; }
.mode-switch .active { color: #00F5FF; font-weight: bold; }
.dossier-title { font-size: 2.2rem; color: #FFF; margin: 0; text-shadow: 0 2px 10px rgba(0,0,0,0.8); }
.edit-mode-input :deep(.el-input__wrapper), .edit-mode-input :deep(.el-textarea__inner) { background-color: rgba(22, 27, 34, 0.6) !important; border: 1px solid rgba(0, 245, 255, 0.3) !important; box-shadow: none !important; color: #FFF; font-family: inherit; }
.title-input :deep(.el-input__inner) { font-size: 2rem; font-weight: bold; padding: 10px; }
.dossier-body { display: flex; padding: 30px; gap: 40px; flex: 1; }
.section-label { font-family: 'Courier New', monospace; font-size: 0.8rem; color: #00F5FF; letter-spacing: 2px; margin-bottom: 20px; border-bottom: 1px solid rgba(0, 245, 255, 0.1); padding-bottom: 5px; }
.dossier-content { flex: 7; }
.markdown-reader { font-size: 1.05rem; line-height: 1.8; color: #C9D1D9; white-space: pre-wrap; }
.editor-workspace { display: flex; flex-direction: column; gap: 20px; }
.editor-actions { display: flex; justify-content: flex-end; }
.dossier-sidebar { flex: 3; display: flex; flex-direction: column; gap: 30px; }
.meta-card { background: rgba(22, 27, 34, 0.5); border: 1px solid rgba(255,255,255,0.05); padding: 20px; border-radius: 8px; }
.meta-item { margin-bottom: 15px; }
.meta-item:last-child { margin-bottom: 0; }
.meta-label { display: block; font-size: 0.8rem; color: #8B949E; margin-bottom: 5px; }
.meta-value { font-family: 'Courier New', monospace; font-size: 0.95rem; color: #FFF; }
.danger-zone { margin-top: auto; }
.danger-tip { font-size: 0.8rem; color: #8B949E; line-height: 1.5; margin-bottom: 15px; }
.cyber-danger-btn { width: 100%; background: transparent !important; border: 1px solid #F56C6C !important; color: #F56C6C !important; }
.cyber-danger-btn:hover { background: rgba(245, 108, 108, 0.1) !important; box-shadow: 0 0 15px rgba(245, 108, 108, 0.3); }

/* ==================================================
   新增：注入与向量化产线 (Modal) 样式
   ================================================== */
:deep(.cyber-dialog .el-dialog) { background: #0D1117 !important; border: 1px solid rgba(0, 245, 255, 0.3); box-shadow: 0 0 40px rgba(0, 245, 255, 0.2); }
:deep(.cyber-dialog .el-dialog__title) { color: #00F5FF !important; font-family: 'Courier New', monospace; font-weight: bold; }
.dark-form :deep(.el-form-item__label) { color: #8B949E !important; font-weight: bold; }
.dark-form :deep(.el-input__wrapper), .dark-form :deep(.el-textarea__inner) { background-color: rgba(22, 27, 34, 0.6) !important; border: 1px solid rgba(255, 255, 255, 0.1) !important; box-shadow: none !important; color: #FFF; }
.pipeline-actions { display: flex; justify-content: space-between; margin-top: 25px; }
.ghost-btn { background: transparent !important; border: 1px solid #6E7681 !important; color: #A3B3CC !important; }
.ghost-btn:hover { border-color: #FFF !important; color: #FFF !important; }

/* 极客终端界面 */
.terminal-window { background: #010409; border: 1px solid #30363D; border-radius: 8px; overflow: hidden; font-family: 'Courier New', Courier, monospace; }
.terminal-header { background: #161B22; padding: 10px 15px; display: flex; align-items: center; border-bottom: 1px solid #30363D; }
.terminal-header .dot { width: 12px; height: 12px; border-radius: 50%; margin-right: 8px; }
.terminal-header .red { background: #FF5F56; }
.terminal-header .yellow { background: #FFBD2E; }
.terminal-header .green { background: #27C93F; }
.terminal-title { margin-left: 15px; color: #8B949E; font-size: 0.85rem; }
.terminal-body { padding: 20px; height: 250px; overflow-y: auto; color: #E6EDF3; font-size: 0.9rem; line-height: 1.6; }
.prompt-arrow { color: #00F5FF; margin-right: 8px; font-weight: bold; }
.log-success { color: #7EE787; }
.log-error { color: #FF7B72; }
.cursor-blink { display: inline-block; width: 10px; height: 15px; background-color: #00F5FF; animation: blink 1s step-end infinite; margin-top: 5px; }
/* ==================================================
   无图卡片 & 无图抽屉专属美化 (解决视觉割裂感)
   ================================================== */

/* 1. 瀑布流中的纯数据卡片：全息蓝图网格 */
.editorial-card.text-only-card {
  background-color: #0D1117 !important;
  /* 绘制科技感极强的网格底纹 */
  background-image: 
    linear-gradient(rgba(0, 245, 255, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0, 245, 255, 0.03) 1px, transparent 1px) !important;
  background-size: 20px 20px !important;
  /* 顶部加上发光数据带，让它看起来像是一块高亮的代码主板 */
  border-top: 3px solid rgba(0, 245, 255, 0.4) !important; 
}

.text-only-card .card-body {
  padding-top: 30px; /* 因为没图，把文字往下推一点，增加呼吸感 */
}

/* 2. 抽屉中的无图头部：收缩高度，纯净排版 */
.text-only-hero {
  height: 160px; /* 因为没有图片展示，强制压低头部高度，给下方正文留出更多空间 */
  background-color: #0D1117;
  /* 同样延伸网格感，并在右上角打一束极其微弱的青色柔光 */
  background-image: 
    radial-gradient(circle at 80% 0%, rgba(0, 245, 255, 0.1) 0%, transparent 60%),
    linear-gradient(rgba(255, 255, 255, 0.02) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.02) 1px, transparent 1px);
  background-size: 100% 100%, 30px 30px, 30px 30px;
  border-bottom: 1px solid rgba(0, 245, 255, 0.15);
}

.text-only-hero .dossier-hero-overlay {
  background: transparent; /* 去掉之前为了压暗背景图片而加的黑色渐变遮罩 */
  padding-bottom: 20px;
}
/* ==================================================
   第一层：大类文件夹卡片 (Folder Grid)
   ================================================== */
.folder-title { font-size: 1.2rem; color: #FFF; font-weight: bold; }
.folder-title span { color: #00F5FF; font-size: 0.85rem; font-family: 'Courier New', monospace; margin-left: 10px; }
.breadcrumb-nav { 
  display: flex; align-items: center; gap: 8px; color: #00F5FF; font-size: 1.1rem; 
  cursor: pointer; transition: all 0.3s; font-weight: bold;
}
.breadcrumb-nav:hover { text-shadow: 0 0 10px rgba(0, 245, 255, 0.5); transform: translateX(-5px); }

.folder-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr); /* 2x2 的完美大图矩阵 */
  gap: 30px;
  margin-top: 10px;
}
.folder-card {
  position: relative;
  height: 260px;
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 10px 30px rgba(0,0,0,0.5);
}
.folder-bg {
  width: 100%; height: 100%; background-size: cover; background-position: center;
  transition: transform 0.6s cubic-bezier(0.165, 0.84, 0.44, 1);
}
.folder-overlay {
  position: absolute; top: 0; left: 0; right: 0; bottom: 0;
  background: linear-gradient(to top, rgba(13,17,23, 0.95) 0%, rgba(13,17,23, 0.4) 60%, transparent 100%);
  padding: 30px; display: flex; flex-direction: column; justify-content: flex-end;
  transition: background 0.4s;
}
.folder-card:hover .folder-bg { transform: scale(1.08); }
.folder-card:hover .folder-overlay { background: linear-gradient(to top, rgba(13,17,23, 0.98) 0%, rgba(0,245,255, 0.2) 100%); border: 1px solid rgba(0,245,255,0.4); }

.folder-name { color: #FFF; font-size: 1.8rem; margin: 0 0 10px 0; text-shadow: 0 2px 5px rgba(0,0,0,0.8); }
.folder-desc { color: #A3B3CC; font-size: 0.95rem; line-height: 1.5; margin: 0 0 20px 0; max-width: 80%; }
.folder-action { color: #00F5FF; font-weight: bold; display: flex; align-items: center; gap: 8px; opacity: 0; transform: translateY(10px); transition: all 0.3s; }
.folder-card:hover .folder-action { opacity: 1; transform: translateY(0); }

/* ==================================================
   第二层：底层纯净数据列表 (Data Row)
   ================================================== */
.data-list-container { display: flex; flex-direction: column; gap: 15px; }
.data-row {
  display: flex; justify-content: space-between; align-items: center;
  background: rgba(22, 27, 34, 0.6); border: 1px solid rgba(255, 255, 255, 0.05);
  padding: 20px 25px; border-radius: 12px; cursor: pointer; transition: all 0.3s;
}
.data-row:hover { background: rgba(0, 245, 255, 0.05); border-color: rgba(0, 245, 255, 0.3); transform: translateX(5px); }
.row-left { display: flex; gap: 20px; align-items: center; flex: 1; }
.row-type-indicator { width: 4px; height: 40px; border-radius: 2px; background: #00F5FF; box-shadow: 0 0 8px #00F5FF; }
.row-type-indicator.blacklist { background: #F56C6C; box-shadow: 0 0 8px #F56C6C; }
.row-content { flex: 1; }
.row-title { color: #FFF; font-size: 1.1rem; margin: 0 0 8px 0; }
.row-excerpt { color: #8B949E; font-size: 0.9rem; margin: 0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 800px; }
.row-right { display: flex; align-items: center; gap: 20px; }
.row-date { font-family: 'Courier New', monospace; color: #6E7681; font-size: 0.85rem; }
.row-arrow { color: #00F5FF; font-size: 1.2rem; opacity: 0; transition: all 0.3s; }
.data-row:hover .row-arrow { opacity: 1; }

.empty-data { text-align: center; padding: 60px 0; color: #6E7681; }
@keyframes blink { 0%, 100% { opacity: 1; } 50% { opacity: 0; } }
</style>