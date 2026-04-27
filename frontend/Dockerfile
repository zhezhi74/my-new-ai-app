# --- 第一阶段：构建前端静态文件 ---
FROM docker.m.daocloud.io/library/node:18 AS builder

# 设置工作目录
WORKDIR /app

# 复制 package.json 和 package-lock.json
COPY package*.json ./

# 安装依赖
RUN npm install

# 复制所有源代码
COPY . .

# 构建生产版本
RUN npm run build

# --- 第二阶段：使用 Nginx 托管静态文件 ---
FROM docker.m.daocloud.io/library/nginx:alpine

# 复制构建好的静态文件到 Nginx 的 HTML 目录
COPY --from=builder /app/dist /usr/share/nginx/html

# 复制自定义的 Nginx 配置文件
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 暴露 80 端口
EXPOSE 80