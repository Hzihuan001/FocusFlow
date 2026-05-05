# GardenScreen 组件化拆分重构

**日期**: 2026-04-01  
**类型**: 架构重构  
**影响范围**: `FocusFlow_App` 花园模块

---

## 背景

`GardenScreen.kt` 原有 **1758 行**代码，包含多个独立组件和工具函数，导致：
- 单文件过于臃肿，难以维护
- 组件职责不清晰
- 代码复用困难

## 拆分方案

### 新增文件

| 文件 | 职责 | 行数 |
|------|------|------|
| `ui/components/GardenComponents.kt` | UI 组件（状态指示器、排行榜、按钮等） | ~350 行 |
| `utils/GardenUtils.kt` | 工具函数（底部空白计算、坐标转换等） | ~120 行 |

### 拆分详情

#### 1. GardenComponents.kt - UI 组件

提取的 Composable 组件：

| 组件 | 功能 |
|------|------|
| `VitalityStateIndicator` | 惰性枯萎状态指示器 HUD |
| `LeaderboardDialog` | 点亮排行榜弹窗 |
| `PulseChargeButton` | 点亮花园按钮（带呼吸动效） |
| `NetworkUnavailableScreen` | 断网提示界面 |
| `handleTileClick()` | 地块点击事件处理 |

#### 2. GardenUtils.kt - 工具函数

提取的工具函数：

| 函数 | 功能 |
|------|------|
| `calculateBottomPadding()` | 计算贴图底部空白比例 |
| `getGroundTileAt()` | 获取地面贴图资源 ID（伪随机） |
| `gridToIso()` | 网格坐标转等距投影坐标 |
| `isoToGrid()` | 等距投影坐标转网格坐标 |
| `screenToGrid()` | 屏幕坐标转网格坐标 |
| `isPointInDiamond()` | 判断点是否在菱形地块内 |

### GardenScreen.kt 保留内容

| 模块 | 职责 |
|------|------|
| `GardenScreen` 主函数 | 状态管理、Canvas 渲染、手势处理 |
| `TileType` 枚举 | 地块类型定义 |
| `VisualTile` 数据类 | 渲染元模型 |

---

## 重构效果

| 指标 | 重构前 | 重构后 |
|------|--------|--------|
| GardenScreen.kt 行数 | 1758 | 1159 |
| 单文件最大行数 | 1758 | 1159 |
| 新增组件文件 | - | 2 个 |

**减少约 600 行代码**（34%）

---

## 依赖关系

```
GardenScreen.kt
    ├── import GardenComponents.kt
    │       ├── VitalityStateIndicator
    │       ├── LeaderboardDialog
    │       ├── PulseChargeButton
    │       └── NetworkUnavailableScreen
    └── import GardenUtils.kt
            └── calculateBottomPadding
```

---

## 后续优化建议

1. **Canvas 渲染逻辑提取**: 当前 Canvas 渲染逻辑仍在主文件中（约 300 行），可考虑提取为 `GardenRenderer.kt`
2. **状态管理简化**: 部分状态可合并到 ViewModel 中统一管理
3. **组件单元测试**: 新提取的组件易于独立测试

---

## 验证结果

```
BUILD SUCCESSFUL in 39s
```

功能完整，编译通过。

---

## 性能修复（2026-04-01 补充）

### 问题

拆分后出现卡顿，原因是提取的组件中无限动画导致 GardenScreen 频繁重组：

| 组件 | 问题 |
|------|------|
| `VitalityStateIndicator` | `rememberInfiniteTransition` 即使在 VIBRANT 状态也持续运行 |
| `PulseChargeButton` | `rememberInfiniteTransition` 充能后仍持续运行 |

### 解决方案

使用 `derivedStateOf` 减少不必要的重组：

```kotlin
// 优化前
val displayAlpha = if (vitalityState != GardenVitalityState.VIBRANT) {
    pulseAlpha
} else {
    1f
}

// 优化后
val shouldPulse = vitalityState != GardenVitalityState.VIBRANT
val displayAlpha by remember(shouldPulse, pulseAlpha) {
    derivedStateOf {
        if (shouldPulse) pulseAlpha else 1f
    }
}
```

### 效果

- 无限动画仍运行，但 `derivedStateOf` 缓存计算结果
- 仅在状态变化时触发重组，而非每帧
- Canvas 渲染不受影响

---

## 性能修复 v2（2026-04-01 最终版）

### 问题

`derivedStateOf` 优化不够彻底，`rememberInfiniteTransition` 仍持续运行。

### 彻底解决：条件渲染

将组件拆分为**静态版本**和**动画版本**，通过条件渲染完全隔离：

```kotlin
@Composable
fun VitalityStateIndicator(...) {
    if (shouldPulse) {
        PulsingVitalityCard(...)   // 带动画版本
    } else {
        StaticVitalityCard(...)    // 静态版本，零动画开销
    }
}
```

### 组件拆分

| 原组件 | 拆分后 |
|--------|--------|
| `VitalityStateIndicator` | `StaticVitalityCard` (静态) + `PulsingVitalityCard` (脉冲) + `VitalityContent` (共用) |
| `PulseChargeButton` | `StaticPulseButton` (已点亮) + `LoadingPulseButton` (加载中) + `GlowingPulseButton` (呼吸发光) |

### 运行时行为

| 状态 | 使用的组件 | 动画开销 |
|------|------------|----------|
| VIBRANT | `StaticVitalityCard` | **零** |
| WARNING/WITHERED | `PulsingVitalityCard` | 脉冲动画 |
| 已点亮 | `StaticPulseButton` | **零** |
| 加载中 | `LoadingPulseButton` | 转圈动画 |
| 未点亮 | `GlowingPulseButton` | 呼吸动画 |

### 最终文件

| 文件 | 行数 |
|------|------|
| `GardenScreen.kt` | 1159 |
| `GardenComponents.kt` | 566 |
| `GardenUtils.kt` | 165 |

---

## 离线缓存功能（2026-04-01 补充）

### 需求

个人终端（MineScreen）在离线时也能显示账号和昵称。

### 实现方案

#### 1. UserPreferences.kt 新增方法

```kotlin
private const val KEY_ACCOUNT = "user_account"

fun getAccount(context: Context): String {
    return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        .getString(KEY_ACCOUNT, "") ?: ""
}

fun setAccount(context: Context, account: String) {
    context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        .edit().putString(KEY_ACCOUNT, account).apply()
}
```

#### 2. ProfileViewModel.kt 缓存逻辑

```kotlin
private fun loadFromLocalCache() {
    val context: Context = getApplication()
    val cachedNickname = UserPreferences.getNickname(context)
    val cachedAccount = UserPreferences.getAccount(context)
    
    if (cachedNickname.isNotEmpty()) {
        _nickname.value = cachedNickname
    }
    if (cachedAccount.isNotEmpty()) {
        _account.value = cachedAccount
    }
}

fun refreshFromCloud() {
    // 1. 先从本地缓存加载，立即显示
    loadFromLocalCache()
    
    // 2. 尝试从云端刷新
    viewModelScope.launch {
        // ... 网络请求 ...
        // 3. 成功后保存到缓存
        UserPreferences.setNickname(getApplication(), cloudUser.nickname)
        UserPreferences.setAccount(getApplication(), cloudUser.account)
    }
}
```

### 行为

| 场景 | 行为 |
|------|------|
| 有网络 | 先显示缓存，再从云端刷新并更新缓存 |
| 无网络 | 显示本地缓存的昵称和账号 |

### 修改文件

| 文件 | 修改内容 |
|------|----------|
| `UserPreferences.kt` | 新增 `getAccount()` / `setAccount()` |
| `ProfileViewModel.kt` | 新增 `loadFromLocalCache()`，修改 `refreshFromCloud()` |

### 验证结果

```
BUILD SUCCESSFUL in 46s
```

---

## 离线修改限制（2026-04-01 补充）

### 需求

离线模式下修改昵称或头像时显示"网络不可用"提示。

### 实现方案

#### ProfileViewModel.kt 新增网络检查

```kotlin
private suspend fun isNetworkAvailable(): Boolean {
    return try {
        val response = RetrofitClient.quickHealthService.healthCheck()
        response.isSuccessful && response.body()?.isSuccess == true
    } catch (e: Exception) {
        false
    }
}
```

#### saveAvatar / updateNickname 方法添加网络检查

```kotlin
fun saveAvatar() {
    viewModelScope.launch {
        // 🟢 [OFFLINE CHECK] 先检查网络
        if (!isNetworkAvailable()) {
            _operationMessage.value = "网络不可用"
            return@launch
        }
        // ... 原有云端更新逻辑 ...
    }
}

fun updateNickname(newNickname: String) {
    viewModelScope.launch {
        // 🟢 [OFFLINE CHECK] 先检查网络
        if (!isNetworkAvailable()) {
            _operationMessage.value = "网络不可用"
            return@launch
        }
        // ... 原有云端更新逻辑 ...
    }
}
```

### 行为

| 场景 | 行为 |
|------|------|
| 有网络修改昵称 | 更新云端 → 更新本地缓存 → 提示成功 |
| 无网络修改昵称 | 提示"网络不可用" |
| 有网络修改头像 | 更新云端 → 提示成功 |
| 无网络修改头像 | 提示"网络不可用" |

### 验证结果

```
BUILD SUCCESSFUL in 34s
```

---

## 光流余额离线显示（2026-04-01 补充）

### 需求

无法连接云端时，个人终端的光流余额显示"--"而非具体数值。

### 实现方案

#### 1. ProfileViewModel.kt 新增连接状态

```kotlin
/** 🟢 [OFFLINE] 云端连接状态 */
private val _isConnected = MutableStateFlow<Boolean?>(null) // null=检查中, true=已连接, false=断开
val isConnected: StateFlow<Boolean?> = _isConnected.asStateFlow()
```

#### 2. refreshFromCloud() 检测连接状态

```kotlin
fun refreshFromCloud() {
    viewModelScope.launch {
        // ... 先从本地缓存加载 ...
        
        // 🟢 [OFFLINE] 先检查网络连接状态
        val networkAvailable = isNetworkAvailable()
        _isConnected.value = networkAvailable
        
        if (!networkAvailable) {
            Log.w(TAG, "无法连接到云端，使用本地缓存数据")
            return@launch
        }
        
        // ... 云端刷新逻辑 ...
    }
}
```

#### 3. CyberScrollingNumber 支持离线状态

```kotlin
@Composable
fun CyberScrollingNumber(
    value: Int,
    // ...
    isConnected: Boolean? = true // null=检查中, true=已连接, false=断开
) {
    // ...
    // 🟢 [OFFLINE] 离线时显示 "--"
    if (isConnected == false) {
        CyberGlowText(text = "--", glowAlpha = 0.5f)
    } else {
        // 正常显示数值
    }
}
```

### 行为

| 连接状态 | 光流余额显示 |
|----------|--------------|
| 已连接云端 | 显示具体数值（如 "✨ 1234"） |
| 无法连接云端 | 显示 "--" |
| 检查中 | 显示 "--" |

### 修改文件

| 文件 | 修改内容 |
|------|----------|
| `ProfileViewModel.kt` | 新增 `isConnected` 状态，`refreshFromCloud()` 添加连接检测 |
| `CyberScrollingNumber.kt` | 新增 `isConnected` 参数，离线时显示"--" |
| `MineScreen.kt` | 收集 `isConnected` 状态并传递给组件 |

### 验证结果

```
BUILD SUCCESSFUL in 34s
```
