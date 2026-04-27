# 🧠 Core Algorithm: 医疗细粒度意图路由分类器 (Fine-tuned BERT)

本项目子模块包含了本医疗问诊系统的核心算法层实现：**基于 BERT 的细粒度意图分类模型**。
该模块负责在用户输入到达大语言模型 (LLM) 之前，进行前置的意图阻断与动态路由，是实现 **Classify-then-Retrieve (先分类后检索)** 架构、彻底根除医疗幻觉的第一道防线。

## 🎯 模块使命 (Objective)

直接使用大模型进行 Prompt 意图分类存在严重的“越狱”风险与长尾识别率低的问题。本模块通过基于真实医疗挂号与问诊对话语料（已脱敏），对轻量级中文预训练模型（如 `RoBERTa-wwm-ext-chinese` 或 `BERT-base`）进行监督微调 (SFT)。

**核心路由标签包含：**
1. `Medical_Symptom` (疾病症状与问诊) -> 触发 RAG 医疗指南检索与 SOAP 思维链。
2. `Medical_Advice` (用药与健康建议) -> 触发 RAG 处方知识库。
3. `General_Chat` (日常闲聊/非医疗意图) -> 直接阻断或走普通闲聊通道，节约大模型算力。

## 🛠️ 算法亮点 (Algorithm Highlights)

* **L2 正则化与 Dropout 结合**：在分类头 (Classification Head) 中引入正则化机制，有效防止在小样本医疗数据集上的过拟合现象。
* **动态动态学习率衰减 (Linear Warmup)**：确保模型在初始阶段稳定收敛，提取深层语义特征。
* **微秒级推断延迟**：相比于调用庞大的 LLM 进行意图判断，本地化部署的 BERT 模型可将路由延迟压缩至 20ms 以内，极大提升全双工系统的吞吐量。

## 📦 环境依赖 (Dependencies)

请确保你的 Python 环境为 `3.8+`，并安装以下依赖：

```bash
cd algorithm
pip install -r requirements.txt
```
*(主要依赖库：`torch`, `transformers`, `scikit-learn`, `pandas`)*

## 🚀 快速开始 (Quick Start)

### 1. 准备数据集
由于医疗隐私保护，本仓库不提供全量真实患者数据集。请参考 `dataset/sample_medical_data.csv` 的格式，自行构建你的 `train.csv` 与 `val.csv`。
数据格式示例：
| text | label |
| :--- | :--- |
| 我最近老是咳嗽，晚上有点低烧 | Medical_Symptom |
| 帮我写一段关于春天的代码 | General_Chat |

### 2. 启动微调训练 (Training)
执行以下脚本开始微调训练：
```bash
python train_bert.py
```
训练过程中的 Loss 和 Accuracy 会打印在控制台。训练完成后，最佳权重将自动保存在 `./saved_model` 目录下。

### 3. 本地推断测试 (Inference)
模型导出后，可使用测试脚本验证其实际路由能力：
```bash
python inference_test.py --text "吃完辛辣食物后胃部有灼烧感怎么办？"
# 预期输出: {"intent": "Medical_Symptom", "confidence": 0.985}
```

## 🔗 与微服务后端的通信
在生产环境中，本模块训练导出的模型可通过 ONNX 转换，或直接通过 FastAPI 封装为内部 RPC 接口，供 Java 后端的 `chat-service` 微服务进行高并发调用，完成全链路的闭环协同。