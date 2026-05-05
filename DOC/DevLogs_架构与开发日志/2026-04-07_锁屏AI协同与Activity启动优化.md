# 2026-04-07 锁屏AI协同与Activity启动优化

## 一、问题描述

### 1. 锁屏AI高度不协同
锁屏悬浮窗的AI对话全屏显示，与首页 ChatBottomSheet 的 90% 高度不一致，体验不统一。

### 2. AI服务JSON解析异常
间歇性出现 `Illegal unquoted character ((CTRL-CHAR, code 10))` 错误，部分对话正常，部分失败。

### 3. 专注完成后未拉回应用
锁屏期间进入其他应用后，专注结束时未返回 FocusFlow。

### 4. 放弃任务导致APP重启与ANR
- 点击放弃会重启APP而非返回首页
- 一段时间后出现"FocusFlow没有响应"提示
- 确认后APP被系统杀死

---

## 二、根因分析

### 2.1 JSON解析错误

**根本原因**：`LockOverlayService` 手动拼接 JSON 字符串，未转义控制字符

```kotlin
// 错误示例：手动拼接
val messagesJson = contextMessages.joinToString(",") { 
    "{\"role\":\"${it["role"]}\",\"content\":\"${it["content"]}\"}" 
}
```

当用户输入包含换行符 `\n`（ASCII 10）时，JSON 格式被破坏。

**衍生问题**：错误消息被保存到数据库 → 作为历史上下文发送 → 再次解析失败 → 恶性循环

### 2.2 Activity重启与ANR

**原因一**：`FLAG_ACTIVITY_CLEAR_TASK` 清除任务栈并重建 Activity

```kotlin
// 问题代码
addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
```

这会导致 Activity 完全销毁重建，表现为"重启"。

**原因二**：重复调用 `startActivity`

| 调用位置 | 触发时机 |
|---------|---------|
| `onFocusComplete()` | 专注完成时 |
| 广播接收器 | 收到完成广播时 |

两次启动请求同时发出 → 系统排队处理 → 主线程阻塞 → ANR

---

## 三、解决方案

### 3.1 锁屏AI高度协同

```kotlin
// LockOverlayService.kt
val displayMetrics = resources.displayMetrics
val screenHeight = displayMetrics.heightPixels
val chatHeight = (screenHeight * 0.9f).toInt()  // 与首页一致

addView(mainContainer, FrameLayout.LayoutParams(
    FrameLayout.LayoutParams.MATCH_PARENT,
    chatHeight,
    Gravity.BOTTOM  // 底部对齐，模拟 BottomSheet
))
```

### 3.2 JSON序列化安全加固

**修复一**：使用 Gson 正确序列化

```kotlin
private fun sendSseRequest(userMessage: String) {
    val gson = com.google.gson.Gson()
    val contextMessages = chatMessages.takeLast(6).map { (isUser, content) ->
        mapOf("role" to if (isUser) "user" else "assistant", "content" to content)
    }
    val messagesJson = gson.toJson(contextMessages)  // 自动转义控制字符
    // ...
}
```

**修复二**：不保存错误消息到数据库

```kotlin
// LockOverlayService.kt & ChatViewModel.kt
private fun saveAiMessage(text: String) {
    if (text.isNotBlank() 
        && !text.startsWith("⚠️") 
        && !text.startsWith("网络")) {
        // 仅保存正常消息
        chatDao.insertMessage(...)
    }
}
```

**修复三**：后端过滤无效历史

```java
// ZhipuProxyService.java
if (!content.startsWith("⚠️") && !content.contains("AI 服务异常")) {
    messagesArray.add(msg);
}
```

### 3.3 Activity启动优化

**核心修改**：统一启动入口 + 使用 SINGLE_TOP

```kotlin
// 1. 广播接收器统一处理启动
private fun registerFocusCompleteReceiver() {
    focusCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            hideOverlay()
            val launchIntent = Intent(this@LockOverlayService, MainActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or 
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or 
                    Intent.FLAG_ACTIVITY_SINGLE_TOP  // 复用已存在的 Activity
                )
            }
            startActivity(launchIntent)
        }
    }
}

// 2. onFocusComplete 只发送广播，不启动 Activity
private fun onFocusComplete() {
    sendBroadcast(Intent(ACTION_FOCUS_COMPLETE))
    // 不再调用 startActivity
}

// 3. performAbandon 独立处理（放弃不走广播）
private fun performAbandon() {
    hideOverlay()
    sendBroadcast(Intent(ACTION_ABANDON_FOCUS))
    val intent = Intent(this, MainActivity::class.java).apply {
        addFlags(FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TOP or FLAG_ACTIVITY_SINGLE_TOP)
    }
    startActivity(intent)
}
```

**Flags 说明**：

| Flag | 作用 |
|------|------|
| `FLAG_ACTIVITY_NEW_TASK` | 在新任务中启动（Service 上下文必需） |
| `FLAG_ACTIVITY_CLEAR_TOP` | 清除目标之上的 Activity |
| `FLAG_ACTIVITY_SINGLE_TOP` | 复用已存在的实例，触发 `onNewIntent` |

---

## 四、验证结果

| 场景 | 预期行为 | 实际行为 |
|------|---------|---------|
| 应用内放弃 | 返回首页，不重启 | ✅ 通过 |
| 其他应用完成 | 拉回 FocusFlow | ✅ 通过 |
| AI 对话含换行 | 正常解析响应 | ✅ 通过 |
| 连续对话 | 历史上下文正确 | ✅ 通过 |

---

## 五、涉及文件

| 文件 | 修改内容 |
|------|---------|
| `LockOverlayService.kt` | AI高度、JSON序列化、Activity启动优化 |
| `ChatViewModel.kt` | 过滤错误消息保存 |
| `ZhipuProxyService.java` | 过滤无效历史上下文 |

---

## 六、构建验证

```
FocusFlow_App: BUILD SUCCESSFUL
FocusFlow_Server: BUILD SUCCESS
FocusFlow_Front: ✓ built in 25.49s
```

---

## 七、部署注意事项

### Android 端
- 修改 `app/build.gradle.kts` 中的 `API_BASE_URL` 为生产环境地址
- Release 构建：`./gradlew assembleRelease`

### 后端服务
- 配置环境变量：`DB_HOST`, `DB_PASSWORD`, `SSL_ENABLED`
- AI 代理接口：`/api/ai/chat`（需配置智谱 API Key）

### 前端管理后台
- 创建 `.env.production` 配置生产 API 地址
- 构建：`npm run build`

---

## 八、后管端登录页改造

### 8.1 需求描述

1. **炫酷动态背景**：鼠标跟随的光效 + 粒子动画
2. **移除默认账号密码提示**：提升安全性
3. **添加验证码功能**：类似若依登录系统

### 8.2 实现方案

#### 后端验证码生成

新增 `CaptchaController.java`：

| 接口 | 方法 | 说明 |
|------|------|------|
| `/admin/captcha/image` | GET | 生成验证码图片（Base64） |

**验证码特性**：
- 4位字母数字组合（排除易混淆字符）
- 赛博朋克风格配色（绿色调）
- 干扰线 + 噪点 + 字符旋转
- 存储于 Session，验证后立即删除

#### 前端登录页改造

**动态背景**：
- Canvas 粒子系统（100个粒子）
- 粒子间连线效果（距离<150px）
- 鼠标跟随径向渐变光效

**验证码组件**：
- 输入框 + 图片组合布局
- 点击图片刷新验证码
- 登录失败自动刷新

### 8.3 涉及文件

| 文件 | 修改内容 |
|------|---------|
| `CaptchaController.java` | 新增验证码生成控制器 |
| `AdminAuthController.java` | 登录接口添加验证码校验 |
| `login/index.vue` | 登录页 UI 重构 |

### 8.4 构建验证

```
FocusFlow_Server: BUILD SUCCESS
FocusFlow_Front: ✓ built in 11.64s
```

### 8.5 问题修复

**问题一：验证码接口被拦截**

| 现象 | 原因 |
|------|------|
| 页面无限刷新 | 验证码接口 `/admin/captcha/image` 被拦截器拦截返回401 |

**修复**：`WebMvcConfig.java` 添加排除路径

```java
.excludePathPatterns(
    "/admin/auth/login",
    "/admin/captcha/**",  // 新增
    "/admin/redirect/**"
)
```

**问题二：验证码返回格式不兼容**

| 现象 | 原因 |
|------|------|
| 前端报错"请求失败" | 返回的是普通 Map，缺少 `code` 字段 |

**修复**：`CaptchaController.java` 使用 `Result` 包装返回

```java
return Result.success(data);  // 原来是直接返回 Map
```

**问题三：管理员密码无法登录**

| 现象 | 原因 |
|------|------|
| admin/admin123 登录失败 | SQL 中的密码哈希值计算错误 |

**修复**：重新计算正确的哈希值

```bash
# 正确计算方式
SHA256("admin123" + "FocusFlow_Admin_2024")
# = 252ae4b107135304abf3b076fd57bb61f234f160e32ac52cf6df51d2826d7e6e
```

---

## 九、后端日志配置优化

### 9.1 需求

1. 日志目录：`/opt/focusflow/logs`
2. 按日期滚动：每天一个日志文件
3. 自动清理：保留最近30天
4. 过滤无关日志：只保留业务日志

### 9.2 实现方案

新增 `logback-spring.xml` 配置文件：

| 配置项 | 值 |
|-------|-----|
| 日志目录 | `${LOG_PATH:-/opt/focusflow/logs}` |
| 文件命名 | `focusflow.yyyy-MM-dd.log` |
| 滚动策略 | 按日期滚动 |
| 保留天数 | 30天 |
| 总大小限制 | 1GB |

**日志级别过滤**：

| 日志来源 | 级别 |
|---------|------|
| `com.focusflow`（业务） | DEBUG |
| Spring 框架 | WARN |
| MyBatis-Plus | WARN |
| HikariCP | WARN |
| Tomcat | WARN |
| Hibernate | ERROR |

### 9.3 日志文件结构

```
/opt/focusflow/logs/
├── focusflow.log              # 当前日志
├── focusflow.2026-04-08.log   # 历史日志（按日期）
├── focusflow-error.log        # 当前错误日志
└── focusflow-error.2026-04-08.log
```

### 9.4 涉及文件

| 文件 | 修改内容 |
|------|---------|
| `logback-spring.xml` | 新增日志配置文件 |
| `application.yml` | 移除日志配置，由 logback 接管 |

---

## 十、部署清单

### 10.1 服务器目录结构

```
/opt/focusflow/
├── focusflow-server-1.0.0.jar   # JAR 包
├── uploads/                      # 上传文件目录
│   └── plants/                   # 植物图片
└── logs/                         # 日志目录

/var/www/focusflow-admin/
└── dist/                         # 前端构建产物
```

### 10.2 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `UPLOAD_DIR` | 上传目录 | `/opt/focusflow/uploads` |
| `LOG_PATH` | 日志目录 | `/opt/focusflow/logs` |
| `DB_HOST` | 数据库地址 | - |
| `DB_PASSWORD` | 数据库密码 | - |

### 10.3 启动命令

```bash
cd /opt/focusflow
java -Xms512m -Xmx1024m -jar focusflow-server-1.0.0.jar
```
