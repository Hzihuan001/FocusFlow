# FocusFlow 完整代码文档

## 一、项目概述

FocusFlow 是一个基于番茄工作法的专注力管理应用，采用端云分离架构，包含 Android 客户端、Spring Boot 后端和 Vue 3 管理后台。

### 核心特性
- **专注模式**：番茄钟计时、防逃逸机制、悬浮窗锁屏
- **赛博花园**：虚拟植物培养、地块净化、好友互访
- **数据统计**：专注时长统计、周报月报、排行榜
- **社交系统**：好友添加、花园访问、留言互动
- **AI 助手**：智谱 AI 集成、流式对话、会话管理

### 技术栈
- **Android**: Kotlin 2.0.21 + Jetpack Compose + Room 2.6.1 + Retrofit 2.9.0
- **后端**: Spring Boot 3.2.3 + MyBatis-Plus 3.5.5 + MySQL 8.0
- **前端**: Vue 3.4 + Vite 5 + Element Plus + Axios

---

## 二、项目结构

### 2.1 Android 端 (FocusFlow_App)

```
FocusFlow_App/
├── app/
│   ├── build.gradle.kts                    # 应用级构建配置
│   └── src/main/
│       ├── AndroidManifest.xml             # 应用清单文件
│       └── java/com/example/focusflow/
│           ├── MainActivity.kt             # 主Activity
│           ├── MyApplication.kt            # 应用入口
│           ├── api/                        # 网络API层
│           │   ├── AuthService.kt          # 认证服务
│           │   ├── BagService.kt           # 背包服务
│           │   ├── ConfigService.kt        # 配置服务
│           │   ├── FocusService.kt         # 专注记录服务
│           │   ├── GardenService.kt        # 花园服务
│           │   ├── RetrofitClient.kt       # Retrofit客户端
│           │   ├── SocialService.kt        # 社交服务
│           │   ├── SyncService.kt          # 同步服务
│           │   ├── UserPreferences.kt      # 用户偏好设置
│           │   └── ZhipuService.kt         # 智谱AI服务
│           ├── data/                       # 数据层
│           │   ├── AppDatabase.kt          # Room数据库
│           │   ├── ConfigManager.kt        # 配置管理器
│           │   ├── dao/                    # 数据访问对象
│           │   │   ├── FocusRecordDao.kt
│           │   │   ├── PlantDictDao.kt
│           │   │   └── UserDao.kt
│           │   ├── entity/                 # 数据实体
│           │   │   ├── FocusRecordEntity.kt
│           │   │   ├── PlantDictEntity.kt
│           │   │   └── UserEntity.kt
│           │   ├── repository/             # 仓库层
│           │   │   ├── BagRepository.kt
│           │   │   ├── FocusRepository.kt
│           │   │   ├── SyncRepository.kt
│           │   │   └── UserRepository.kt
│           │   └── session/
│           │       └── SessionManager.kt   # 会话管理器
│           ├── receiver/
│           │   └── FocusFlowDeviceAdminReceiver.kt
│           ├── service/                    # 服务层
│           │   ├── FocusService.kt         # 专注计时服务
│           │   ├── FocusStateManager.kt    # 专注状态管理器
│           │   ├── LockOverlayService.kt   # 悬浮窗锁屏服务
│           │   ├── NetworkMonitor.kt       # 网络监控服务
│           │   └── TimeFluxSyncService.kt  # 光流同步服务
│           ├── ui/                         # UI层
│           │   ├── FocusViewModel.kt       # 专注ViewModel
│           │   ├── MainViewModel.kt        # 主ViewModel
│           │   ├── components/             # UI组件
│           │   ├── screens/                # 页面
│           │   │   ├── AuthScreen.kt       # 登录注册页
│           │   │   ├── BagScreen.kt        # 背包页
│           │   │   ├── GardenScreen.kt     # 花园页
│           │   │   ├── HomeScreen.kt       # 首页
│           │   │   ├── MineScreen.kt       # 我的页
│           │   │   ├── SocialScreen.kt     # 社交页
│           │   │   ├── SplashScreen.kt     # 启动页
│           │   │   ├── StatsScreen.kt      # 数据统计页
│           │   │   └── ThemeSettingsScreen.kt
│           │   ├── theme/                  # 主题
│           │   │   ├── AppTheme.kt
│           │   │   ├── Color.kt
│           │   │   ├── Theme.kt
│           │   │   └── Type.kt
│           │   ├── viewmodel/              # ViewModel层
│           │   │   ├── AuthViewModel.kt
│           │   │   ├── BagViewModel.kt
│           │   │   ├── ChatViewModel.kt
│           │   │   ├── GardenViewModel.kt
│           │   │   ├── ProfileViewModel.kt
│           │   │   └── SocialViewModel.kt
│           │   └── widget/
│           │       └── CapsuleHatchView.kt
│           └── utils/                      # 工具类
│               ├── ApiCacheManager.kt
│               ├── FocusLockHelper.kt
│               ├── GardenUtils.kt
│               ├── PlantBitmapLoader.kt
│               ├── PlantImageLoader.kt
│               ├── SecurityUtils.kt
│               ├── SoundManager.kt
│               └── UsageStatsWatcher.kt
├── gradle/
│   └── libs.versions.toml                  # 依赖版本管理
├── build.gradle.kts                        # 项目级构建配置
└── settings.gradle.kts                     # 项目设置
```

### 2.2 后端 (FocusFlow_Server)

```
FocusFlow_Server/
├── src/main/
│   ├── java/com/focusflow/server/
│   │   ├── FocusFlowServerApplication.java # 应用入口
│   │   ├── common/                         # 公共类
│   │   │   ├── Result.java                 # 统一响应结果
│   │   │   └── SecurityUtils.java          # 安全工具类
│   │   ├── config/                         # 配置类
│   │   │   ├── AdminAuthInterceptor.java   # 管理员认证拦截器
│   │   │   ├── CorsConfig.java             # 跨域配置
│   │   │   ├── MybatisPlusConfig.java      # MyBatis-Plus配置
│   │   │   └── WebMvcConfig.java           # Web MVC配置
│   │   ├── controller/                     # 控制器层
│   │   │   ├── AdminAIController.java
│   │   │   ├── AdminAuthController.java
│   │   │   ├── AdminBagController.java
│   │   │   ├── AdminConfigController.java
│   │   │   ├── AdminFocusController.java
│   │   │   ├── AdminPlantController.java
│   │   │   ├── AdminRedirectController.java
│   │   │   ├── AdminStatsController.java
│   │   │   ├── AdminUserController.java
│   │   │   ├── AiProxyController.java
│   │   │   ├── AuthController.java
│   │   │   ├── BagController.java
│   │   │   ├── CaptchaController.java
│   │   │   ├── ConfigController.java
│   │   │   ├── FocusRecordController.java
│   │   │   ├── FriendController.java
│   │   │   ├── GardenController.java
│   │   │   ├── HealthController.java
│   │   │   ├── PlantController.java
│   │   │   ├── SyncController.java
│   │   │   ├── UserController.java
│   │   │   └── VisitController.java
│   │   ├── dto/                            # 数据传输对象
│   │   │   ├── AddFriendRequest.java
│   │   │   ├── AuthResponse.java
│   │   │   ├── BagItemResponse.java
│   │   │   ├── BatchSyncRequest.java
│   │   │   ├── ChangePasswordRequest.java
│   │   │   ├── ConfigUpdateRequest.java
│   │   │   ├── FocusRecordSyncDTO.java
│   │   │   ├── FriendInfoResponse.java
│   │   │   ├── GardenTileResponse.java
│   │   │   ├── GardenTilesResponse.java
│   │   │   ├── LeaderboardEntry.java
│   │   │   ├── LeaveMessageRequest.java
│   │   │   ├── LoginRequest.java
│   │   │   ├── PlantDictRequest.java
│   │   │   ├── PlantDictResponse.java
│   │   │   ├── PlantRequest.java
│   │   │   ├── RegisterRequest.java
│   │   │   ├── SyncResultDTO.java
│   │   │   ├── UpdateProfileRequest.java
│   │   │   └── VisitLogResponse.java
│   │   ├── entity/                         # 实体类
│   │   │   ├── FocusRecord.java
│   │   │   ├── Friendship.java
│   │   │   ├── GardenTile.java
│   │   │   ├── PlantDict.java
│   │   │   ├── SysAdmin.java
│   │   │   ├── SysAdminLoginLog.java
│   │   │   ├── SystemConfig.java
│   │   │   ├── User.java
│   │   │   ├── UserBag.java
│   │   │   └── VisitLog.java
│   │   ├── mapper/                         # MyBatis Mapper
│   │   │   ├── FocusRecordMapper.java
│   │   │   ├── FriendshipMapper.java
│   │   │   ├── GardenTileMapper.java
│   │   │   ├── PlantDictMapper.java
│   │   │   ├── SysAdminLoginLogMapper.java
│   │   │   ├── SysAdminMapper.java
│   │   │   ├── SystemConfigMapper.java
│   │   │   ├── UserBagMapper.java
│   │   │   ├── UserMapper.java
│   │   │   └── VisitLogMapper.java
│   │   └── service/                        # 服务层
│   │       ├── FocusRecordService.java
│   │       ├── FriendshipService.java
│   │       ├── GardenTileService.java
│   │       ├── ImageOptimizationService.java
│   │       ├── PlantDictService.java
│   │       ├── SystemConfigService.java
│   │       ├── UserBagService.java
│   │       ├── UserService.java
│   │       ├── VisitLogService.java
│   │       ├── ZhipuProxyService.java
│   │       └── impl/                       # 服务实现
│   │           ├── FocusRecordServiceImpl.java
│   │           ├── FriendshipServiceImpl.java
│   │           ├── GardenTileServiceImpl.java
│   │           ├── PlantDictServiceImpl.java
│   │           ├── SystemConfigServiceImpl.java
│   │           ├── UserBagServiceImpl.java
│   │           ├── UserServiceImpl.java
│   │           └── VisitLogServiceImpl.java
│   └── resources/
│       ├── application.yml                 # 应用配置
│       └── logback-spring.xml              # 日志配置
└── pom.xml                                 # Maven配置
```

### 2.3 前端 (FocusFlow_Front)

```
FocusFlow_Front/
├── src/
│   ├── api/                                # API接口
│   │   ├── config.js                       # 配置管理API
│   │   ├── dashboard.js                    # 数据看板API
│   │   ├── plant.js                        # 植物管理API
│   │   ├── request.js                      # Axios封装
│   │   └── user.js                         # 用户管理API
│   ├── layout/
│   │   └── index.vue                       # 布局组件
│   ├── router/
│   │   └── index.js                        # 路由配置
│   ├── styles/
│   │   └── index.scss                      # 全局样式
│   ├── utils/
│   │   └── format.js                       # 格式化工具
│   ├── views/                              # 页面组件
│   │   ├── ai/
│   │   │   └── index.vue                   # AI管理页
│   │   ├── bag/
│   │   │   └── index.vue                   # 背包管理页
│   │   ├── config/
│   │   │   └── index.vue                   # 配置管理页
│   │   ├── dashboard/
│   │   │   └── index.vue                   # 数据看板页
│   │   ├── focus/
│   │   │   └── index.vue                   # 专注记录页
│   │   ├── login/
│   │   │   └── index.vue                   # 登录页
│   │   ├── plant/
│   │   │   └── index.vue                   # 植物管理页
│   │   └── user/
│   │       └── index.vue                   # 用户管理页
│   ├── App.vue                             # 根组件
│   └── main.js                             # 应用入口
├── index.html                              # HTML模板
├── package.json                            # 依赖配置
└── vite.config.js                          # Vite配置
```

---

## 三、Android 端代码

### 3.1 主入口

#### MainActivity.kt
**路径**: `FocusFlow_App/app/src/main/java/com/example/focusflow/MainActivity.kt`

```kotlin
// 文件被截断，需要完整读取
// 主要功能：应用主Activity，管理导航、专注服务绑定、权限请求
```

#### MyApplication.kt
**路径**: `FocusFlow_App/app/src/main/java/com/example/focusflow/MyApplication.kt`

```kotlin
package com.example.focusflow

import android.app.Application
import com.example.focusflow.api.RetrofitClient
import com.example.focusflow.api.UserPreferences
import com.example.focusflow.data.AppConfigManager
import com.example.focusflow.data.AppDatabase
import com.example.focusflow.data.repository.BatchSettlementResult
import com.example.focusflow.data.repository.FocusRepository
import com.example.focusflow.data.repository.SyncRepository
import com.example.focusflow.data.session.SessionManager
import com.example.focusflow.service.TimeFluxSyncService
import com.example.focusflow.utils.PlantImageLoader
import com.example.focusflow.utils.PlantBitmapLoader
import com.example.focusflow.utils.ApiCacheManager
import com.example.focusflow.utils.SoundManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * FocusFlow 应用程序入口
 *
 * 【职责】
 * 1. 初始化全局单例组件（数据库、SessionManager）
 * 2. 启动网络状态监听（端云同步基础设施）
 * 3. 加载服务端配置
 * 4. 初始化图片加载器
 * 5. 设置同步后奖励结算回调
 */
class MyApplication : Application() {

    companion object {
        @Volatile
        private var INSTANCE: MyApplication? = null

        fun getInstance(): MyApplication =
            INSTANCE ?: throw IllegalStateException("Application not initialized")
            
        // 全局结算回调（用于同步后通知 UI 显示结算弹窗）
        @Volatile
        var onBatchSettlement: ((BatchSettlementResult) -> Unit)? = null
    }

    // 全局依赖实例
    lateinit var sessionManager: SessionManager
        private set

    lateinit var syncRepository: SyncRepository
        private set
        
    lateinit var focusRepository: FocusRepository
        private set

    // 应用级协程作用域
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this

        // 初始化会话管理器
        sessionManager = SessionManager.getInstance(this)

        // 初始化数据库
        val database = AppDatabase.getDatabase(this)

        // 初始化 FocusRepository
        focusRepository = FocusRepository(
            focusRecordDao = database.focusRecordDao(),
            userDao = database.userDao(),
            sessionManager = sessionManager
        )

        // 初始化同步仓库并启动网络监听
        syncRepository = SyncRepository(
            context = this,
            focusRecordDao = database.focusRecordDao(),
            sessionManager = sessionManager
        )
        
        // 设置 FocusRepository 引用到 SyncRepository（用于结算种子）
        syncRepository.setFocusRepository(focusRepository)
        
        syncRepository.startNetworkMonitoring()

        // 初始化图片加载器
        PlantImageLoader.init(this)
        PlantBitmapLoader.init(this)
        
        // 初始化API缓存管理器
        ApiCacheManager.init(this)
        
        // 初始化音效管理器
        SoundManager.init(this)

        // 加载服务端配置
        loadConfigs()
        
        // 设置同步仓库引用到 TimeFluxSyncService（联动同步）
        val timeFluxSyncService = TimeFluxSyncService.getInstance(this)
        timeFluxSyncService.setSyncRepository(syncRepository)
        
        // 🟢 应用启动时立即启动光流同步服务
        timeFluxSyncService.startSync()
        android.util.Log.d("MyApplication", "光流同步服务已启动")
        
        // 设置同步完成监听器：同步成功后更新待同步数
        syncRepository.setOnSyncCompleteListener {
            applicationScope.launch {
                timeFluxSyncService.updatePendingSyncCount()
            }
        }
        
        // 设置奖励结算监听器：同步成功后触发结算弹窗
        syncRepository.setOnRewardSettlementListener { result ->
            android.util.Log.d("MyApplication", "奖励结算回调触发: ${result.totalRecords} 条记录")
            // 通知 UI 显示结算弹窗
            onBatchSettlement?.invoke(result)
        }
    }

    /**
     * 从服务端加载配置
     */
    private fun loadConfigs() {
        applicationScope.launch {
            try {
                val success = AppConfigManager.loadConfigs(RetrofitClient.configService)
                android.util.Log.d("MyApplication", if (success) "配置加载成功" else "配置加载失败")
                
                // 加载音效设置
                SoundManager.loadSettings(this@MyApplication)
            } catch (e: Exception) {
                android.util.Log.e("MyApplication", "配置加载异常", e)
            }
        }
    }
}
```

### 3.2 服务层

#### FocusService.kt
**路径**: `FocusFlow_App/app/src/main/java/com/example/focusflow/service/FocusService.kt`

```kotlin
// 已完整收集，见上文
```

#### LockOverlayService.kt
**路径**: `FocusFlow_App/app/src/main/java/com/example/focusflow/service/LockOverlayService.kt`

```kotlin
// 文件被截断，需要完整读取
// 主要功能：悬浮窗锁屏服务，防逃逸机制，AI对话集成
```

#### TimeFluxSyncService.kt
**路径**: `FocusFlow_App/app/src/main/java/com/example/focusflow/service/TimeFluxSyncService.kt`

```kotlin
// 已完整收集，见上文
```

#### FocusStateManager.kt
**路径**: `FocusFlow_App/app/src/main/java/com/example/focusflow/service/FocusStateManager.kt`

```kotlin
// 已完整收集，见上文
```

#### NetworkMonitor.kt
**路径**: `FocusFlow_App/app/src/main/java/com/example/focusflow/service/NetworkMonitor.kt`

```kotlin
// 已完整收集，见上文
```

### 3.3 ViewModel 层

#### AuthViewModel.kt
**路径**: `FocusFlow_App/app/src/main/java/com/example/focusflow/ui/viewmodel/AuthViewModel.kt`

```kotlin
// 已完整收集，见上文
```

#### BagViewModel.kt
**路径**: `FocusFlow_App/app/src/main/java/com/example/focusflow/ui/viewmodel/BagViewModel.kt`

```kotlin
// 已完整收集，见上文
```

#### ChatViewModel.kt
**路径**: `FocusFlow_App/app/src/main/java/com/example/focusflow/ui/viewmodel/ChatViewModel.kt`

```kotlin
// 已完整收集，见上文
```

#### GardenViewModel.kt
**路径**: `FocusFlow_App/app/src/main/java/com/example/focusflow/ui/viewmodel/GardenViewModel.kt`

```kotlin
// 已完整收集，见上文
```

#### ProfileViewModel.kt
**路径**: `FocusFlow_App/app/src/main/java/com/example/focusflow/ui/viewmodel/ProfileViewModel.kt`

```kotlin
// 已完整收集，见上文
```

#### SocialViewModel.kt
**路径**: `FocusFlow_App/app/src/main/java/com/example/focusflow/ui/viewmodel/SocialViewModel.kt`

```kotlin
// 已完整收集，见上文
```

#### FocusViewModel.kt
**路径**: `FocusFlow_App/app/src/main/java/com/example/focusflow/ui/FocusViewModel.kt`

```kotlin
// 已完整收集，见上文
```

#### MainViewModel.kt
**路径**: `FocusFlow_App/app/src/main/java/com/example/focusflow/ui/MainViewModel.kt`

```kotlin
// 已完整收集，见上文
```

### 3.4 配置文件

#### AndroidManifest.xml
**路径**: `FocusFlow_App/app/src/main/AndroidManifest.xml`

```xml
// 已完整收集，见上文
```

#### build.gradle.kts (app级别)
**路径**: `FocusFlow_App/app/build.gradle.kts`

```kotlin
// 已完整收集，见上文
```

#### libs.versions.toml
**路径**: `FocusFlow_App/gradle/libs.versions.toml`

```toml
// 已完整收集，见上文
```

---

## 四、后端代码

### 4.1 应用入口

#### FocusFlowServerApplication.java
**路径**: `FocusFlow_Server/src/main/java/com/focusflow/server/FocusFlowServerApplication.java`

```java
// 待收集
```

### 4.2 控制器层

#### AuthController.java
**路径**: `FocusFlow_Server/src/main/java/com/focusflow/server/controller/AuthController.java`

```java
// 待收集
```

---

## 五、前端代码

### 5.1 主入口

#### main.js
**路径**: `FocusFlow_Front/src/main.js`

```javascript
// 待收集
```

#### App.vue
**路径**: `FocusFlow_Front/src/App.vue`

```vue
// 待收集
```

---

*文档持续更新中...*
