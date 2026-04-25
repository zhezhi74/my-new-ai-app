# 基于细粒度意图路由与 RAG 的可靠医疗大模型问诊系统
## Reliable Medical LLM Consultation System Based on Fine-grained Intent Routing and RAG

[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2023.x-green.svg)](https://spring.io/projects/spring-cloud)
[![Vue](https://img.shields.io/badge/Vue-3.x-brightgreen.svg)](https://vuejs.org/)
[![BERT](https://img.shields.io/badge/Model-BERT-orange.svg)](https://huggingface.co/docs/transformers/model_doc/bert)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

---

> **?? 给后续接手 AI 的指令 (Instruction for Future AI):**
> 这是一个高度模块化的微服务项目。请优先阅读本文件以了解架构细节，并参考根目录下的 `ROADMAP.md` 获取接下来的开发任务（如 BERT 微调、Python FastAPI 接口对接等）。

---

## ?? 项目愿景
本项目旨在解决当前大语言模型在医疗问诊中存在的**意图识别模糊**与**幻觉隐患**问题。通过引入 **BERT 细粒度意图路由** 和 **RAG (检索增强生成)** 架构，系统能够根据用户主诉精准分配处理路径，并强制通过医疗指南进行事实修正，生成符合 SOAP 标准的专业病历。

## ??? 技术架构 (Technical Stack)

### 1. 后端微服务集群 (Spring Cloud Alibaba)
* **网关层 (`gatewayservice`)**: 基于 Spring Cloud Gateway 实现 JWT 统一鉴权与动态路由。
* **对话核心 (`chatservice`)**: 集成 LangChain4j，负责 LLM 编排、会话上下文管理与 SOAP 报告生成。
* **知识治理 (`knowledgeservice`)**: 负责医疗语料的 CRUD、向量化触发以及黑名单规则管理。
* **服务治理**: Nacos (注册/配置中心), Sentinel (限流熔断)。

### 2. AI 算法中枢 (Python / FastAPI)
* **意图识别**: 基于 HuggingFace 的 **BERT (RoBERTa-wwm-ext)** 预训练模型进行微调，实现细粒度意图分类。
* **向量引擎**: 集成 **Milvus/Qdrant** 向量数据库，使用 BGE-m3 模型进行 Embedding 与 Rerank 重排。
* **基础模型**: 通过 Ollama 部署本地 **Llama 3 / Qwen** 系列模型。

### 3. 极客化前端 (Vue 3 / Composition API)
* **UI 体系**: Element Plus + 玻璃拟物化设计 (Glassmorphism)。
* **可视化**: ECharts 动态算力态势图。

---

## ?? 系统功能展示

### 01. 全局态势大屏 (Global Dashboard)
展示系统实时的算力负载、RAG 检索命中率及黑名单拦截态势。
![全局态势大屏](./docs/images/01_dashboard.png)

### 02. RAG 核心知识库 (Knowledge)
支持“RAG检索源”、“诊疗模板”、“意图语料”、“幻觉黑名单”四大集群的钻取管理。
![核心知识库首页](./docs/images/02_knowledge_home.png)
![知识库详情展示](./docs/images/03_knowledge_detail.png)

### 03. 终端问诊沙盘 (Terminal)
模拟沉浸式问诊环境，支持引用溯源显示，确保回答有据可查。
![问诊终端](./docs/images/04_chat_terminal.png)
![问诊详情](./docs/images/05_chat_terminal.png)

### 04. 多模态报告引擎 (Report Engine)
自动从历史对话中提取关键点，生成符合临床标准的 SOAP 格式电子病历。
![报告生成展示](./docs/images/06_report_engine.png)

### 05. 全息病例档案 (Archives)
基于时间轴管理的患者历史问诊记录与数字化档案。
![病例档案](./docs/images/07_archives.png)

---

## ?? 项目结构
```text
├── chatservice         # 对话业务服务 (Java)
├── knowledgeservice    # 知识库管理服务 (Java)
├── gatewayservice      # API 网关 (Java)
├── userservice         # 用户与权限服务 (Java)
├── ai-engine-python    # Python 算法中枢 (BERT/FastAPI) [开发中]
└── frontend            # Vue 3 前端工程

### ?? 快速启动
环境准备: JDK 17, Node.js 18+, Python 3.9+, MySQL 8.0, Redis, Nacos。

算法端: 进入 ai-engine-python，执行 pip install -r requirements.txt。

后端: 修改各模块 application.yml 中的 Nacos 地址，启动微服务。

前端: 进入 frontend，执行 npm install 然后 npm run dev。

### ?? 开发路线图 (Roadmap)
详细的开发进度与未来计划请参阅：ROADMAP.md

Author: 张权柄 (Jiangnan University - IoT Engineering)

Expected Graduation: June 2026