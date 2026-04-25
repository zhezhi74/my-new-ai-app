<template>
  <div class="dashboard-container">
    <div class="header-section">
      <h2 class="dashboard-title">全局态势大屏 <span class="sub-title">/ GLOBAL DASHBOARD</span></h2>
    </div>

    <el-row :gutter="20" class="top-matrix">
      <el-col :span="16">
        <el-card class="glass-card carousel-card">
          <swiper
            :effect="'fade'"
            :autoplay="{ delay: 5000, disableOnInteraction: false }"
            :modules="modules"
            class="hit-carousel"
          >
            <swiper-slide v-for="(slide, index) in slides" :key="index">
              <div class="slide-content" :style="{ backgroundImage: `url(${slide.bg})` }">
                <div class="slide-overlay">
                  <div class="hit-badge"><span class="blink-dot"></span> LIVE HIT</div>
                  <h3 class="slide-title">{{ slide.title }}</h3>
                  <p class="slide-desc">{{ slide.desc }}</p>
                  <div class="code-snippet">{{ slide.code }}</div>
                </div>
              </div>
            </swiper-slide>
          </swiper>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card class="glass-card stat-card">
          <div class="stat-item">
            <div class="stat-label">大模型平均响应延迟</div>
            <div class="stat-value">1,245 <span class="unit">ms</span></div>
            <div class="stat-trend up">比昨日 <el-icon><Top /></el-icon> 12%</div>
          </div>
          <el-divider class="cyber-divider" />
          <div class="stat-item">
            <div class="stat-label">向量检索成功率</div>
            <div class="stat-value">98.7 <span class="unit">%</span></div>
            <div class="stat-trend down">比昨日 <el-icon><Bottom /></el-icon> 0.3%</div>
          </div>
          <el-divider class="cyber-divider" />
          <div class="stat-item">
            <div class="stat-label">黑名单拦截次数</div>
            <div class="stat-value alert-value">42 <span class="unit">次</span></div>
            <div class="stat-trend">系统安全屏障生效中</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="chart-matrix">
      <el-col :span="14">
        <el-card class="glass-card chart-card">
          <div class="chart-header">今日问诊算力波动趋势 (Requests/h)</div>
          <div ref="lineChartRef" class="echart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card class="glass-card chart-card">
          <div class="chart-header">知识库意图识别雷达 (Intent Radar)</div>
          <div ref="radarChartRef" class="echart-container"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { Top, Bottom } from '@element-plus/icons-vue';
import * as echarts from 'echarts';
import { Swiper, SwiperSlide } from 'swiper/vue';
import { EffectFade, Autoplay } from 'swiper/modules';
import 'swiper/css';
import 'swiper/css/effect-fade';

const modules = [EffectFade, Autoplay];

// 轮播图数据：完美契合你的“方案二”
const slides = ref([
  {
    bg: new URL('../assets/images/rag.png', import.meta.url).href,
    title: '临床指南召回 (Cardiology Guideline)',
    desc: '系统成功从百万级向量库中召回高维心血管医学文献。',
    code: '{"status": 200, "vector_id": "V-9921", "similarity": 0.992}'
  },
  {
    bg: new URL('../assets/images/zhishiku1.png', import.meta.url).href,
    title: '结构化模板匹配 (SOAP Mapping)',
    desc: '患者主诉已成功剥离并映射至标准诊疗骨架。',
    code: '{"Subjective": true, "Objective": false, "Plan": "pending"}'
  },
  {
    bg: new URL('../assets/images/zhishiku3.png', import.meta.url).href,
    title: '意图降维解析 (NLP Intent Decoding)',
    desc: '口语化输入“喘不上气”已被精准映射为实体节点。',
    code: '{"Intent": "Urgent", "Entity": "Dyspnea", "Severity": "High"}'
  }
]);

const lineChartRef = ref(null);
const radarChartRef = ref(null);

onMounted(() => {
  initLineChart();
  initRadarChart();
});

const initLineChart = () => {
  const chart = echarts.init(lineChartRef.value);
  chart.setOption({
    tooltip: { trigger: 'axis', backgroundColor: 'rgba(13,17,23,0.9)', borderColor: '#00F5FF', textStyle: { color: '#fff' } },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '10%', containLabel: true },
    xAxis: { type: 'category', boundaryGap: false, data: ['08:00', '10:00', '12:00', '14:00', '16:00', '18:00', '20:00'], axisLine: { lineStyle: { color: '#484F58' } }, axisLabel: { color: '#8B949E' } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: 'rgba(255,255,255,0.05)' } }, axisLabel: { color: '#8B949E' } },
    series: [
      {
        name: '算力请求量', type: 'line', smooth: true, symbol: 'none',
        lineStyle: { color: '#00F5FF', width: 3, shadowColor: 'rgba(0,245,255,0.5)', shadowBlur: 10 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(0, 245, 255, 0.4)' },
            { offset: 1, color: 'rgba(0, 245, 255, 0)' }
          ])
        },
        data: [120, 280, 150, 420, 350, 510, 210]
      }
    ]
  });
  window.addEventListener('resize', () => chart.resize());
};

const initRadarChart = () => {
  const chart = echarts.init(radarChartRef.value);
  chart.setOption({
    tooltip: { backgroundColor: 'rgba(13,17,23,0.9)', borderColor: '#00F5FF', textStyle: { color: '#fff' } },
    radar: {
      indicator: [
        { name: '指南命中', max: 100 }, { name: '模板匹配', max: 100 },
        { name: '意图识别', max: 100 }, { name: '防幻觉拦截', max: 100 }, { name: '推理深度', max: 100 }
      ],
      axisName: { color: '#A3B3CC' },
      splitArea: { areaStyle: { color: ['rgba(0,245,255,0.02)', 'rgba(0,245,255,0.05)'] } },
      axisLine: { lineStyle: { color: 'rgba(0,245,255,0.2)' } },
      splitLine: { lineStyle: { color: 'rgba(0,245,255,0.2)' } }
    },
    series: [
      {
        name: 'RAG 引擎矩阵', type: 'radar',
        data: [{
          value: [88, 92, 95, 100, 85], name: '实时状态',
          itemStyle: { color: '#00F5FF' }, areaStyle: { color: 'rgba(0, 245, 255, 0.4)' }
        }]
      }
    ]
  });
  window.addEventListener('resize', () => chart.resize());
};
</script>

<style scoped>
.dashboard-container { padding: 10px 30px; height: 100%; overflow-y: auto; color: #F0F6FC; }
.header-section { margin-bottom: 25px; }
.dashboard-title { font-size: 1.8rem; color: #FFF; border-bottom: 1px solid rgba(0,245,255,0.2); padding-bottom: 15px; margin: 0; text-shadow: 0 0 10px rgba(0,245,255,0.3); }
.sub-title { font-size: 0.9rem; color: #00F5FF; font-family: 'Courier New', monospace; margin-left: 10px; }

.glass-card { background: rgba(22, 27, 34, 0.6) !important; border: 1px solid rgba(255, 255, 255, 0.08) !important; border-radius: 12px; }

.top-matrix { margin-bottom: 20px; }
.carousel-card { padding: 0 !important; overflow: hidden; height: 260px; }
.hit-carousel { height: 100%; width: 100%; }
.slide-content { height: 100%; background-size: cover; background-position: center; position: relative; }
.slide-overlay { position: absolute; top: 0; left: 0; right: 0; bottom: 0; background: linear-gradient(to right, rgba(13,17,23,0.95) 0%, rgba(13,17,23,0.4) 60%, transparent 100%); padding: 30px 40px; display: flex; flex-direction: column; justify-content: center; }

.hit-badge { display: inline-flex; align-items: center; background: rgba(0,245,255,0.1); color: #00F5FF; padding: 5px 12px; border-radius: 4px; font-size: 0.8rem; font-weight: bold; border: 1px solid rgba(0,245,255,0.3); width: max-content; margin-bottom: 15px; }
.blink-dot { width: 8px; height: 8px; background: #00F5FF; border-radius: 50%; margin-right: 8px; box-shadow: 0 0 8px #00F5FF; animation: blink 1s step-end infinite; }
@keyframes blink { 0%, 100% { opacity: 1; } 50% { opacity: 0; } }

.slide-title { font-size: 1.8rem; color: #FFF; margin: 0 0 10px 0; text-shadow: 0 2px 10px rgba(0,0,0,0.8); }
.slide-desc { color: #A3B3CC; font-size: 1rem; margin: 0 0 20px 0; max-width: 70%; }
.code-snippet { font-family: 'Courier New', monospace; color: #7EE787; background: rgba(0,0,0,0.5); padding: 10px 15px; border-radius: 6px; border-left: 3px solid #7EE787; width: max-content; font-size: 0.9rem; }

.stat-card { height: 260px; display: flex; flex-direction: column; justify-content: center; padding: 20px; }
.stat-item { text-align: center; }
.stat-label { color: #8B949E; font-size: 0.9rem; margin-bottom: 5px; }
.stat-value { font-size: 2rem; color: #FFF; font-weight: bold; font-family: 'Courier New', monospace; text-shadow: 0 0 10px rgba(255,255,255,0.2); }
.alert-value { color: #F56C6C; text-shadow: 0 0 10px rgba(245,108,108,0.4); }
.unit { font-size: 1rem; color: #A3B3CC; font-weight: normal; }
.stat-trend { font-size: 0.8rem; margin-top: 5px; color: #8B949E; }
.stat-trend.up { color: #F56C6C; }
.stat-trend.down { color: #7EE787; }
.cyber-divider { margin: 15px 0 !important; border-color: rgba(255,255,255,0.05) !important; }

.chart-matrix { height: 350px; }
.chart-card { height: 100%; display: flex; flex-direction: column; }
.chart-header { color: #00F5FF; font-family: 'Courier New', monospace; font-size: 0.9rem; margin-bottom: 15px; font-weight: bold; }
.echart-container { flex: 1; width: 100%; min-height: 250px; }
</style>