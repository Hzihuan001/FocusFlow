# FocusFlow Agent 指南

## 项目架构
- **Android** (`FocusFlow_App/`): Kotlin 2.0.21 + Jetpack Compose + Room 2.6.1
- **Server** (`FocusFlow_Server/`): Spring Boot 3.2.3 + Java 17 + MyBatis-Plus 3.5.5
- **Admin** (`FocusFlow_Front/`): Vue 3.4 + Vite 5 + Element Plus

## 开发命令

### Android
```bash
cd FocusFlow_App
./gradlew assembleDebug   # 构建 Debug APK
./gradlew assembleRelease # 构建 Release APK
./gradlew clean          # 清理构建
```
注意事项:
- 模拟器访问宿主机用 `10.0.2.2` 而非 `localhost`
- API 地址硬编码在 `app/build.gradle.kts`: `http://101.34.249.20:8080/api/`

### Server
```bash
cd FocusFlow_Server
mvn spring-boot:run   # 开发模式运行
mvn clean package -DskipTests  # 打包
java -jar target/focusflow-server-1.0.0.jar
```
- 端口: 8080, Context Path: `/api`
- DB: `focus_flow`@101.34.249.20:3306

### Admin
```bash
cd FocusFlow_Front
npm install
npm run dev    # 端口 3000, 代理 /api -> localhost:8080
npm run build
```

## 核心配置

| 文件 | 关键信息 |
|------|----------|
| `FocusFlow_App/gradle/libs.versions.toml` | 依赖版本定义 (AGP 8.13.2, Kotlin 2.0.21) |
| `FocusFlow_App/app/build.gradle.kts` | minSdk 26, targetSdk 34, KSP 注解处理器 |
| `FocusFlow_Server/pom.xml` | Spring Boot 3.2.3, MyBatis-Plus 3.5.5 |
| `FocusFlow_Front/vite.config.js` | 端口 3000, API 代理配置 |

## 特殊约定

1. **本地优先同步**: 专注记录先写 Room (`syncStatus=0`), 联网时自动同步到云端
2. **防篡改签名**: 每条记录 SHA-256 签名, 云端验证拒绝篡改
3. **防逃逸机制**: UsageStatsManager 300ms 轮询 + 悬浮窗锁屏 + Intent 拉回
4. **植物状态机**: VIBRANT(<24h) → WARNING(24-48h) → WITHERED(>48h)
5. **开发日志**: 代码修改后需同步更新 `DOC/DevLogs_架构与开发日志/` (按日期)