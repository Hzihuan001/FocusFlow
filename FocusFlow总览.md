# FocusFlow 注意力管理系统

## 项目概述

FocusFlow 是一款注意力管理应用，结合游戏化元素帮助用户提高专注力。采用前后端分离架构：

- **Android 端** (`FocusFlow_App`): Kotlin 2.0.21 + Jetpack Compose，支持 Android 8.0+ (minSdk 26, targetSdk 34)
- **后端服务** (`FocusFlow_Server`): Spring Boot 3.2.3 + Java 17 + MyBatis-Plus 3.5.5 + MySQL 8.0
- **管理后台** (`FocusFlow_Front`): Vue 3.4 + Vite 5 + Element Plus 2.5 + ECharts 5.5

### 核心功能

1. **专注计时器**: 支持番茄钟模式、自定义时长、悬浮窗锁屏模式
2. **防逃逸机制**: 基于 UsageStatsManager 的高频轮询暗哨，检测逃逸并毫秒级拉回
3. **AI 助手**: 智谱大模型驱动的学习助手，支持 SSE 流式对话、多会话管理
4. **游戏化系统**:
   - 花园系统：种植和培育虚拟赛博植物
   - 背包系统：管理植物种子和道具
   - 光流货币：专注时长转换为虚拟货币
5. **数据统计**: 专注记录可视化、历史数据查询
6. **社交系统**: 好友系统、花园互访、充能留言

---

## 目录结构

```
F:\desktop\iflowtest\
├── FocusFlow_App/                # Android 应用
│   ├── app/src/main/java/com/example/focusflow/
│   │   ├── api/                  # 网络层：Retrofit 服务接口
│   │   ├── data/                 # 数据层：Room 实体、DAO、Repository
│   │   │   ├── entity/           # 数据库实体 (UserEntity, FocusRecordEntity, PlantDictEntity)
│   │   │   ├── dao/              # Room DAO 接口
│   │   │   ├── repository/       # 数据仓库层
│   │   │   ├── session/          # 会话管理 (SessionManager)
│   │   │   ├── AppDatabase.kt    # Room 数据库 (version 15)
│   │   │   ├── ChatDao.kt        # AI 消息 DAO
│   │   │   ├── ChatMessageEntity.kt    # AI 消息实体
│   │   │   ├── ChatSessionDao.kt       # AI 会话 DAO
│   │   │   ├── ChatSessionEntity.kt    # AI 会话实体
│   │   │   └── ConfigManager.kt        # 配置管理器
│   │   ├── service/              # 后台服务
│   │   │   ├── FocusService.kt         # 专注计时服务
│   │   │   ├── FocusStateManager.kt   # 专注状态管理
│   │   │   ├── LockOverlayService.kt  # 悬浮窗锁屏服务
│   │   │   ├── NetworkMonitor.kt      # 网络状态监控
│   │   │   └── TimeFluxSyncService.kt # 光流同步服务
│   │   ├── ui/                   # UI 层：Screen、ViewModel、组件
│   │   │   ├── screens/          # 各页面
│   │   │   │   ├── HomeScreen.kt       # 主页（专注入口）
│   │   │   │   ├── StatsScreen.kt     # 数据统计页
│   │   │   │   ├── MineScreen.kt      # 个人中心
│   │   │   │   ├── GardenScreen.kt    # 花园页面
│   │   │   │   ├── BagScreen.kt       # 背包页面
│   │   │   │   ├── SocialScreen.kt    # 社交页面
│   │   │   │   ├── AuthScreen.kt      # 登录注册页
│   │   │   │   └── SplashScreen.kt    # 启动页
│   │   │   ├── viewmodel/        # ViewModel 定义
│   │   │   ├── components/       # 可复用 UI 组件
│   │   │   ├── widget/           # 桌面小组件
│   │   │   └── theme/            # 主题配置 (赛博朋克风格)
│   │   ├── utils/                # 工具类
│   │   │   ├── FocusLockHelper.kt      # 锁屏增强权限检测
│   │   │   ├── UsageStatsWatcher.kt    # 防逃逸轮询引擎
│   │   │   ├── SoundManager.kt         # 音效管理器
│   │   │   ├── SecurityUtils.kt        # 安全工具类
│   │   │   ├── GardenUtils.kt          # 花园工具类
│   │   │   ├── ApiCacheManager.kt      # API 缓存管理
│   │   │   ├── PlantBitmapLoader.kt    # 植物位图加载器
│   │   │   └── PlantImageLoader.kt     # 植物图片加载器
│   │   ├── receiver/             # 广播接收器
│   │   ├── MainActivity.kt       # 主 Activity
│   │   └── MyApplication.kt      # Application 类
│   ├── gradle/libs.versions.toml # 依赖版本目录
│   └── build.gradle.kts          # 模块级构建配置
│
├── FocusFlow_Server/             # Spring Boot 后端
│   ├── src/main/java/com/focusflow/server/
│   │   ├── controller/           # REST API 控制器
│   │   │   ├── AdminAIController.java     # AI 配置管理
│   │   │   ├── AdminAuthController.java   # 管理员认证
│   │   │   ├── AdminBagController.java    # 背包管理
│   │   │   ├── AdminConfigController.java # 系统配置管理
│   │   │   ├── AdminFocusController.java  # 专注记录管理
│   │   │   ├── AdminPlantController.java  # 植物管理
│   │   │   ├── AdminStatsController.java  # 数据看板
│   │   │   ├── AdminUserController.java  # 用户管理
│   │   │   ├── AiProxyController.java     # AI 代理接口
│   │   │   ├── AuthController.java        # 用户认证
│   │   │   ├── BagController.java         # 背包接口
│   │   │   ├── CaptchaController.java     # 验证码
│   │   │   ├── ConfigController.java      # 配置接口
│   │   │   ├── FocusRecordController.java # 专注记录
│   │   │   ├── FriendController.java      # 好友关系
│   │   │   ├── GardenController.java      # 花园接口
│   │   │   ├── HealthController.java      # 健康检查
│   │   │   ├── PlantController.java       # 植物图鉴
│   │   │   ├── SyncController.java        # 数据同步
│   │   │   ├── UserController.java        # 用户接口
│   │   │   └── VisitController.java       # 访问日志
│   │   ├── service/              # 业务逻辑层
│   │   ├── mapper/               # MyBatis-Plus Mapper
│   │   ├── entity/               # 数据库实体
│   │   │   ├── User.java               # 用户实体
│   │   │   ├── FocusRecord.java       # 专注记录
│   │   │   ├── PlantDict.java         # 植物图鉴
│   │   │   ├── UserBag.java           # 用户背包
│   │   │   ├── GardenTile.java        # 花园地块
│   │   │   ├── Friendship.java        # 好友关系
│   │   │   ├── VisitLog.java          # 访问日志
│   │   │   ├── SysAdmin.java          # 管理员
│   │   │   ├── SysAdminLoginLog.java  # 管理员登录日志
│   │   │   └── SystemConfig.java      # 系统配置
│   │   ├── dto/                  # 数据传输对象
│   │   ├── config/               # 配置类 (CORS, MyBatis 等)
│   │   └── common/               # 通用类 (Result, SecurityUtils)
│   ├── uploads/plants/           # 植物图片上传目录
│   └── pom.xml                   # Maven 配置
│
├── FocusFlow_Front/              # Vue3 管理后台
│   ├── src/
│   │   ├── views/                # 页面组件
│   │   │   ├── dashboard/        # 数据看板
│   │   │   ├── user/             # 用户管理
│   │   │   ├── plant/            # 植物图鉴
│   │   │   ├── bag/              # 背包管理
│   │   │   ├── focus/            # 专注记录
│   │   │   ├── config/           # 系统配置（卡片化分组）
│   │   │   ├── ai/               # AI 配置（独立模块）
│   │   │   └── login/            # 登录页
│   │   ├── api/                  # API 请求封装
│   │   │   ├── request.js        # Axios 封装
│   │   │   ├── config.js         # 配置 API
│   │   │   ├── dashboard.js      # 看板 API
│   │   │   ├── plant.js          # 植物 API
│   │   │   └── user.js           # 用户 API
│   │   ├── router/               # 路由配置
│   │   ├── layout/               # 布局组件
│   │   └── styles/               # 样式文件
│   ├── vite.config.js            # Vite 配置 (端口 3000)
│   └── package.json
│
├── DOC/                          # 项目文档
│   ├── DevLogs_架构与开发日志/    # 开发日志 (按日期)
│   ├── sql/                      # SQL 初始化脚本
│   ├── FocusFlow端侧Room核心表设计.docx
│   ├── FocusFlow云端MySQL核心表设计.docx
│   ├── FocusFlow项目技术总结.md
│   ├── DEPLOYMENT_GUIDE.md       # 部署指南
│   └── QUICK_DEPLOY_UBUNTU2404.md # Ubuntu 快速部署
│
└── 毕业论文/                     # 毕业论文相关文档
```

---

## 技术栈

### Android 端

| 领域 | 技术选型 |
|------|----------|
| AGP | 8.13.2 |
| Kotlin | 2.0.21 |
| UI | Jetpack Compose + Material3 (BOM 2024.09.00) |
| 导航 | Navigation Compose 2.7.7 |
| 数据库 | Room 2.6.1 + KSP 注解处理器 (version 15) |
| 网络 | Retrofit 2.9.0 + OkHttp 4.12.0 + SSE |
| 异步 | Kotlin Coroutines 1.7.3 + StateFlow |
| 持久化 | DataStore 1.1.1 (替代 SharedPreferences) |
| 安全 | Security Crypto 1.1.0-alpha06 |
| 图片加载 | Coil 2.5.0 |

### 后端服务

| 领域 | 技术选型 |
|------|----------|
| 框架 | Spring Boot 3.2.3 |
| 数据库 | MySQL 8.0 + MyBatis-Plus 3.5.5 |
| 连接池 | HikariCP |
| 工具库 | Hutool 5.8.26 + Lombok |
| JDK | Java 17 |

### 管理后台

| 领域 | 技术选型 |
|------|----------|
| 框架 | Vue 3.4.21 + Vite 5.1.6 |
| UI 组件 | Element Plus 2.5.6 |
| 状态管理 | Pinia 2.1.7 |
| 图表 | ECharts 5.5.0 |
| HTTP | Axios 1.6.7 |
| 日期 | Day.js 1.11.10 |
| 样式 | SCSS |

---

## 构建与运行

### Android 端

```bash
cd FocusFlow_App

# 构建 Debug APK (连接云服务器)
./gradlew assembleDebug

# 构建 Release APK
./gradlew assembleRelease

# 清理构建
./gradlew clean

# 运行单元测试
./gradlew test
```

**注意事项**:
- 需要 JDK 11+
- 使用 KSP 处理 Room 注解，首次构建可能需要较长时间
- API 地址通过 BuildConfig 注入，默认连接云服务器 `101.34.249.20:8080`
- 模拟器访问宿主机使用 `10.0.2.2`
- Room 数据库版本 15，支持 AI 会话管理

### 后端服务

```bash
cd FocusFlow_Server

# 编译
mvn clean compile

# 运行 (开发模式)
mvn spring-boot:run

# 打包
mvn clean package -DskipTests

# 运行 JAR
java -jar target/focusflow-server-1.0.0.jar
```

**配置文件**: `src/main/resources/application.yml`
- 服务端口: 8080
- Context Path: `/api`
- 数据库: `focus_flow` (MySQL)
- HTTPS: 可通过环境变量 `SSL_ENABLED=true` 启用
- 文件上传: 最大 10MB，请求最大 50MB

### 管理后台

```bash
cd FocusFlow_Front

# 安装依赖
npm install

# 开发模式 (端口 3000)
npm run dev

# 构建
npm run build

# 预览构建结果
npm run preview
```

---

## 数据库设计

### 云端 MySQL 核心表

| 表名 | 说明 |
|------|------|
| `biz_user` | 用户主表：账号、昵称、光流余额、连续专注天数 |
| `biz_focus_record` | 专注记录：任务名、时长、防篡改签名 |
| `biz_plant_dict` | 植物图鉴：植物定义、稀有度、净化范围、占地面积 |
| `biz_user_bag` | 用户背包：持有的种子/道具 |
| `biz_garden_tile` | 花园地块：种植状态、净化状态 |
| `biz_friendship` | 好友关系 |
| `biz_visit_log` | 互访日志 |
| `sys_admin` | 管理员账户 |
| `sys_admin_login_log` | 管理员登录日志 |
| `sys_config` | 系统配置：支持分组（连续专注奖励、花园净化、充能系统、AI 配置等） |

### 端侧 Room 核心表

| 实体 | 说明 |
|------|------|
| `UserEntity` | 用户缓存数据 |
| `FocusRecordEntity` | 本地专注记录 (支持离线) |
| `PlantDictEntity` | 植物图鉴缓存 |
| `ChatSessionEntity` | AI 会话列表（含 lastMessage、systemPrompt、isDeleted 等字段） |
| `ChatMessageEntity` | AI 对话消息（含 tokenCount 字段） |

---

## 核心架构

### 防逃逸架构

专注模式采用多层防护机制：
1. **悬浮窗锁屏**: 全屏覆盖层阻止用户切出应用
2. **防逃逸暗哨**: UsageStatsManager 高频轮询 (300ms) 检测前台应用
3. **雷霆拉回机制**: 检测到逃逸时立即通过 Intent 拉回应用

权限要求：
- 悬浮窗权限 (`SYSTEM_ALERT_WINDOW`)
- 使用情况访问权限 (`PACKAGE_USAGE_STATS`)

### 本地优先同步架构

专注记录采用**本地优先**架构：
1. 专注完成 → 直接写入本地 Room 数据库 (`syncStatus=0`)
2. 联网时 → 自动上传待同步记录到云端
3. 云端验证签名 → 返回成功 → 更新本地 `syncStatus=1`

### 防篡改签名机制

每条专注记录生成 SHA-256 签名：
```
signature = SHA256(recordId + userId + taskName + durationMinutes + startTime + SECRET_KEY)
```

云端验证签名拒绝篡改请求。

### 惰性枯萎状态机

花园植物根据最后专注时间动态渲染三种状态：
| 状态 | 时间范围 | 视觉效果 |
|------|----------|----------|
| VIBRANT | < 24h | 鲜艳、霓虹光晕 |
| WARNING | 24-48h | 饱和度降低、脉冲警告 |
| WITHERED | > 48h | 灰度、红色警示 |

### AI 会话管理架构

支持多会话管理和历史上下文：
- **会话切换**: Job 订阅管理，确保消息正确清空
- **消息持久化**: 用户消息和 AI 回复均保存到 Room
- **历史上下文**: 取最近 6 条消息作为对话上下文
- **锁屏 AI**: 专注锁屏时 AI 助手支持完整的会话管理功能

---

## 植物系统配置

### 植物属性说明

| 字段 | 说明 | 取值范围 |
|------|------|----------|
| `width` | 占地面积 | 1-4（1×1格 至 4×4格） |
| `purifyRange` | 净化范围 | 1-4（解锁 4-40 格） |
| `dropWeight` | 掉落权重 | 1-100（越大越容易获得） |

### 占地面积 vs 净化范围

- **占地面积 (`width`)**: 植物本身占用的格子数，影响显示大小
- **净化范围 (`purifyRange`)**: 种植时解锁周围格子数（曼哈顿距离菱形区域）

两者相互独立，可灵活配置：
- 小植物大净化：`width=1, purifyRange=4`
- 大植物小净化：`width=3, purifyRange=1`

---

## API 参考

### 后端接口 (Spring Boot)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/health` | 健康检查 |
| POST | `/api/auth/register` | 用户注册 |
| POST | `/api/auth/login` | 用户登录 |
| GET | `/api/user/{userId}` | 查询用户信息 |
| PUT | `/api/user/{userId}` | 更新用户信息 |
| GET | `/api/plants` | 植物图鉴列表 |
| POST | `/api/sync/focus-records/batch` | 批量同步专注记录 |
| GET | `/api/garden/tiles/{userId}` | 获取花园地块 |
| POST | `/api/garden/plant` | 种植植物 |
| POST | `/api/bag/parse` | 解析种子 |
| GET | `/api/bag/{userId}` | 获取背包 |
| GET | `/api/config` | 获取配置 |
| GET | `/api/captcha` | 获取验证码 |

### 管理后台接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/admin/login` | 管理员登录 |
| GET | `/api/admin/stats/overview` | 数据看板概览 |
| GET | `/api/admin/users` | 用户列表 |
| GET | `/api/admin/focus-records` | 专注记录列表 |
| POST | `/api/admin/plants` | 新增植物 |
| PUT | `/api/admin/plants/{id}` | 编辑植物 |
| GET | `/api/admin/ai/config` | 获取 AI 配置 |
| PUT | `/api/admin/ai/config` | 更新 AI API 配置 |
| PUT | `/api/admin/ai/prompt` | 更新 AI Prompt |
| PUT | `/api/admin/ai/switch` | AI 熔断开关 |
| GET | `/api/admin/config` | 获取系统配置列表 |
| PUT | `/api/admin/config/batch` | 批量更新配置 |

---

## 开发约定

### UI 设计风格

应用采用赛博朋克视觉风格，主题色定义在 `ui/theme/Color.kt`：
- `CyberBgDeep`: 深色背景 (#0D0D1A)
- `CyberPrimary`: 主色调霓虹绿 (#00FF9F)
- `CyberAccent`: 强调色霓虹粉 (#FF00FF)

### 导航结构

底部导航栏三个 Tab：
- **专注** (`home`): 主页，快速开始专注
- **数据** (`stats`): 统计数据、花园入口
- **我的** (`mine`): 个人中心

锁屏页面 (`lock/*`) 隐藏底部导航栏。

### 后台管理模块划分

| 模块 | 菜单 | 说明 |
|------|------|------|
| 数据看板 | dashboard | 用户统计、专注数据可视化 |
| 用户管理 | user | 用户列表、状态管理 |
| 植物图鉴 | plant | 植物 CRUD、图片上传 |
| 背包管理 | bag | 用户背包查看、道具发放 |
| 专注记录 | focus | 专注记录查询、导出 |
| 系统配置 | config | 运行参数配置（卡片化分组展示） |
| AI 配置 | ai | API 配置、Prompt 管理、熔断开关 |

### 代码风格

- Android: 遵循 Android 官方 Kotlin 风格指南
- 使用 StateFlow 进行状态管理
- Repository 模式隔离数据源
- ViewModel 负责业务逻辑
- 开发规范：修改和更新代码后，需要同步更新开发日志（`DOC/DevLogs_架构与开发日志/`）

---

## 常见问题

### 数据库连接

确保 MySQL 服务已启动，数据库 `focus_flow` 已创建：
```sql
CREATE DATABASE focus_flow DEFAULT CHARACTER SET utf8mb4;
```

初始化脚本: `DOC/db_init_focusflow.sql`

### 端口冲突

- Spring Boot 默认端口: 8080
- Vue 开发服务器默认端口: 3000
- 如端口被占用，修改相应配置文件

### 模拟器网络

模拟器访问本机服务使用 `10.0.2.2`，而非 `localhost` 或 `127.0.0.1`。

---

## 相关文档

- `DOC/端到端核心链路测试指南.md` - 完整测试流程
- `DOC/FocusFlow端侧Room核心表设计.docx` - 端侧数据库设计
- `DOC/FocusFlow云端MySQL核心表设计.docx` - 云端数据库设计
- `DOC/DEPLOYMENT_GUIDE.md` - 部署指南
- `DOC/QUICK_DEPLOY_UBUNTU2404.md` - Ubuntu 快速部署
- `DOC/DevLogs_架构与开发日志/` - 按日期的开发日志
  - `2026-04-09_后管端功能优化与体验改进.md` - 后管端优化
  - `2026-04-09_植物图鉴云端同步优化.md` - 植物同步优化
  - `2026-04-08_后管端登录页改造与部署优化.md` - 登录页改造
  - `2026-04-07_锁屏AI协同与Activity启动优化.md` - 锁屏AI协同
  - `2026-04-05_AI会话管理优化与体验改进.md` - AI 会话管理实现
  - `2026-04-05_后台管理系统优化与安全加固.md` - 安全加固
  - `2026-04-03_AI助手配置云端化重构.md` - AI 配置独立模块
  - `2026-03-31_防逃逸暗哨与UsageStats高频轮询引擎.md` - 防逃逸机制实现
  - `2026-03-30_悬浮窗锁屏与结算闭环实现.md` - 悬浮窗锁屏方案
  - `2026-03-30_原生Canvas粒子引擎与盲盒UI重构.md` - 孵化舱UI实现
