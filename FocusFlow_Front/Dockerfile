# 阶段 1：使用 Node.js 进行 Vue 3 项目的安装与打包
FROM docker.m.daocloud.io/library/node:20-alpine AS builder
WORKDIR /app
# 复制 package.json 并安装依赖
COPY package*.json ./
# 如果由于网络原因 npm install 太慢，可以把下面这行换成 RUN npm config set registry https://registry.npmmirror.com && npm install
RUN npm install
# 复制所有源代码并打包
COPY . .
RUN npm run build

# 阶段 2：使用轻量级 Nginx 镜像运行产物
FROM nginx:alpine
# 从 builder 阶段把打包好的 dist 文件夹复制到 Nginx 默认的网页目录
COPY --from=builder /app/dist /usr/share/nginx/html

# 用我们刚刚写好的自定义 nginx.conf 替换掉 Nginx 自带的默认配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 暴露 80 端口
EXPOSE 80

# 启动 Nginx
CMD ["nginx", "-g", "daemon off;"]