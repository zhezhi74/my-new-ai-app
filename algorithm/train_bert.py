import torch
from torch.utils.data import Dataset, DataLoader
from transformers import BertTokenizer, BertForSequenceClassification
from torch.optim import AdamW
import warnings

# 忽略一些不必要的 huggingface 后台警告，保持控制台清爽
warnings.filterwarnings("ignore")

# ==========================================
# 1. 定义极其精简的“微型医疗数据集” (已重命名防止冲突)
# ==========================================
train_texts = [
    "我今天早上起床头有点晕，是不是高血压？",  # 0: 诊断
    "大夫，我最近一直咳嗽，咳痰带着血丝。",  # 0: 诊断
    "阿司匹林肠溶片能和感冒清热颗粒一起吃吗？",  # 1: 用药
    "吃完布洛芬胃疼怎么办？还能继续吃吗？",  # 1: 用药
    "你好，医生在吗？",  # 2: 闲聊
    "周末愉快，感谢医生的解答！"  # 2: 闲聊
]
train_labels = [0, 0, 1, 1, 2, 2]


# ==========================================
# 2. 构建 PyTorch 数据集类
# ==========================================
# ==========================================
# 2. 构建 PyTorch 数据集类 (已更新为最新版 Transformers 语法)
# ==========================================
class MedicalIntentDataset(Dataset):
    def __init__(self, texts, labels, tokenizer, max_len=64):
        self.texts = texts
        self.labels = labels
        self.tokenizer = tokenizer
        self.max_len = max_len

    def __len__(self):
        return len(self.texts)

    def __getitem__(self, item):
        text = str(self.texts[item])
        label = self.labels[item]

        # 【核心修复点】：放弃老旧的 encode_plus，直接使用 tokenizer()
        encoding = self.tokenizer(
            text,
            add_special_tokens=True,
            max_length=self.max_len,
            padding='max_length',
            truncation=True,
            return_attention_mask=True,
            return_tensors='pt',
        )
        return {
            'input_ids': encoding['input_ids'].flatten(),
            'attention_mask': encoding['attention_mask'].flatten(),
            'labels': torch.tensor(label, dtype=torch.long)
        }


def main():
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    print(f"当前使用的计算设备: {device}")

    # ==========================================
    # 3. 加载预训练的哈工大中文 RoBERTa 模型
    # ==========================================
    model_name = "hfl/chinese-roberta-wwm-ext"
    tokenizer = BertTokenizer.from_pretrained(model_name)
    # num_labels=3 因为我们有3个意图
    model = BertForSequenceClassification.from_pretrained(model_name, num_labels=3)
    model.to(device)

    # ==========================================
    # 使用重命名后的全局变量传入数据集
    # ==========================================
    dataset = MedicalIntentDataset(train_texts, train_labels, tokenizer)
    dataloader = DataLoader(dataset, batch_size=2, shuffle=True)

    # 4. 核心创新点：配置优化器与 L2 正则化 (weight_decay=0.01)
    optimizer = AdamW(model.parameters(), lr=2e-5, weight_decay=0.01)

    # ==========================================
    # 5. 开始微调训练 (Training Loop)
    # ==========================================
    epochs = 5
    model.train()
    print("开始训练模型...")

    for epoch in range(epochs):
        total_loss = 0
        for batch in dataloader:
            optimizer.zero_grad()

            # 将数据推送到 RTX 3060 上
            input_ids = batch['input_ids'].to(device)
            attention_mask = batch['attention_mask'].to(device)
            # 【修复点】：将 batch 里的 labels 改名为 batch_labels
            batch_labels = batch['labels'].to(device)

            # 前向传播计算 Loss
            outputs = model(input_ids=input_ids, attention_mask=attention_mask, labels=batch_labels)
            loss = outputs.loss
            total_loss += loss.item()

            # 反向传播与权重更新
            loss.backward()
            optimizer.step()

        avg_loss = total_loss / len(dataloader)
        print(f"Epoch {epoch + 1}/{epochs} | Average Loss: {avg_loss:.4f}")

    # 保存咱们微调后的模型！
    model.save_pretrained("./medical_intent_model")
    tokenizer.save_pretrained("./medical_intent_model")
    print("\n训练完成！你的医疗专属意图大脑已保存到 ./medical_intent_model 目录下。")


if __name__ == "__main__":
    main()