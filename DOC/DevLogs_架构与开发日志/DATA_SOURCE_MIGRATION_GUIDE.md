# 数据源迁移指南

## 概述

本文档说明如何将 StatsScreen 和 MineScreen 从旧的数据源迁移到新的 Repository 数据源。

## MainViewModel 新增的数据源

### 1. 用户数据（已完成）✅

```kotlin
// 当前用户（实时观察）
val currentUser: StateFlow<UserEntity?>

// 用户昵称（从 currentUser 派生）
val nickname: StateFlow<String>

// Time Flux 余额（从 currentUser 派生）
val timeFlux: StateFlow<Int>
```

**用途：** MineScreen 显示用户信息和光流余额

### 2. 统计数据（新增）✅

```kotlin
// 今日记录（从 FocusRepository 获取）
val todayRecordsNew: StateFlow<List<FocusRecordEntity>>

// 本周记录（从 FocusRepository 获取）
val weekRecordsNew: StateFlow<List<FocusRecordEntity>>

// 今日总专注时长（分钟）
val todayTotalMinutes: StateFlow<Int>

// 本周总专注时长（分钟）
val weekTotalMinutes: StateFlow<Int>
```

**用途：** StatsScreen 显示统计数据

### 3. 旧数据源（保留用于兼容）

```kotlin
// 旧表的所有记录（用于饼图和柱状图）
val allRecords: StateFlow<List<FocusRecord>>

// 日报统计（基于旧表）
val dailyStats: StateFlow<StatsSummary>

// 周报统计（基于旧表）
val weeklyStats: StateFlow<List<BarData>>
```

**说明：** 保留用于现有的图表功能，后续可以迁移

## MineScreen 迁移指南

### 当前实现（需要更新）

```kotlin
@Composable
fun MineScreen(viewModel: MainViewModel) {
    // ❌ 旧方式：可能使用硬编码或本地状态
    var coins by remember { mutableStateOf(1000) }
    var nickname by remember { mutableStateOf("用户") }
}
```

### 新实现（推荐）✅

```kotlin
@Composable
fun MineScreen(viewModel: MainViewModel) {
    // ✅ 新方式：实时观察 Repository 数据
    val currentUser by viewModel.currentUser.collectAsState()
    val nickname by viewModel.nickname.collectAsState()
    val timeFlux by viewModel.timeFlux.collectAsState()
    
    Column {
        // 显示用户信息
        Text("昵称: $nickname")
        
        // 显示光流余额（实时更新）
        Row {
            Icon(Icons.Default.Star, contentDescription = "Time Flux")
            Text("$timeFlux")
        }
        
        // 显示用户 ID（可选）
        currentUser?.let { user ->
            Text("ID: ${user.userId.take(8)}...", 
                 style = MaterialTheme.typography.bodySmall)
        }
    }
}
```

### 关键点

1. **实时更新：** `timeFlux` 会在完成专注任务后自动更新
2. **响应式：** 使用 `collectAsState()` 自动订阅数据变化
3. **类型安全：** 直接使用 `UserEntity` 类型

## StatsScreen 迁移指南

### 方案 A：使用新的统计数据（推荐）

```kotlin
@Composable
fun StatsScreen(viewModel: MainViewModel) {
    // ✅ 使用新的 Repository 数据源
    val todayMinutes by viewModel.todayTotalMinutes.collectAsState()
    val weekMinutes by viewModel.weekTotalMinutes.collectAsState()
    val todayRecords by viewModel.todayRecordsNew.collectAsState()
    val weekRecords by viewModel.weekRecordsNew.collectAsState()
    
    Column {
        // 今日统计卡片
        StatCard(
            title = "今日专注",
            value = "${todayMinutes / 60}h ${todayMinutes % 60}m",
            subtitle = "${todayRecords.size} 次专注"
        )
        
        // 本周统计卡片
        StatCard(
            title = "本周专注",
            value = "${weekMinutes / 60}h ${weekMinutes % 60}m",
            subtitle = "${weekRecords.size} 次专注"
        )
        
        // 简单的列表展示
        LazyColumn {
            items(todayRecords) { record ->
                RecordItem(
                    taskName = record.taskName,
                    duration = record.durationMinutes,
                    time = record.startTime
                )
            }
        }
    }
}
```

### 方案 B：继续使用旧数据源（临时方案）

```kotlin
@Composable
fun StatsScreen(viewModel: MainViewModel) {
    // 继续使用现有的数据源
    val dailyStats by viewModel.dailyStats.collectAsState()
    val weeklyStats by viewModel.weeklyStats.collectAsState()
    
    // 现有的图表代码保持不变
    PieChart(dailyStats)
    BarChart(weeklyStats)
}
```

**说明：** 旧数据源仍然可用，可以逐步迁移

### 方案 C：混合使用（过渡方案）

```kotlin
@Composable
fun StatsScreen(viewModel: MainViewModel) {
    // 新数据：用于顶部统计卡片
    val todayMinutes by viewModel.todayTotalMinutes.collectAsState()
    val weekMinutes by viewModel.weekTotalMinutes.collectAsState()
    val timeFlux by viewModel.timeFlux.collectAsState()
    
    // 旧数据：用于图表（因为有 tag 字段）
    val dailyStats by viewModel.dailyStats.collectAsState()
    val weeklyStats by viewModel.weeklyStats.collectAsState()
    
    Column {
        // 顶部：使用新数据
        Row {
            StatCard("今日", "${todayMinutes}分钟")
            StatCard("本周", "${weekMinutes}分钟")
            StatCard("光流", "$timeFlux")
        }
        
        // 图表：使用旧数据（因为需要 tag 分类）
        PieChart(dailyStats)
        BarChart(weeklyStats)
    }
}
```

## 数据流对比

### 旧数据流（已废弃）

```
FocusDao.getAllRecords()
  ↓
MainViewModel.allRecords
  ↓
MainViewModel.dailyStats / weeklyStats
  ↓
StatsScreen
```

### 新数据流（推荐）✅

```
FocusRepository.getTodayRecordsFlow(userId)
  ↓
MainViewModel.todayRecordsNew
  ↓
StatsScreen

UserRepository.getCurrentUserFlow()
  ↓
MainViewModel.currentUser
  ↓
MainViewModel.timeFlux (派生)
  ↓
MineScreen
```

## 迁移步骤

### 第 1 步：更新 MineScreen ✅

1. 移除硬编码的 coins 和 nickname
2. 使用 `viewModel.timeFlux.collectAsState()`
3. 使用 `viewModel.nickname.collectAsState()`
4. 测试光流余额是否实时更新

### 第 2 步：更新 StatsScreen（简单统计）✅

1. 使用 `viewModel.todayTotalMinutes.collectAsState()`
2. 使用 `viewModel.weekTotalMinutes.collectAsState()`
3. 显示简单的统计卡片
4. 测试数据是否正确

### 第 3 步：迁移图表（可选）

1. 由于新表没有 `tag` 字段，图表功能需要重新设计
2. 可以继续使用旧表的数据（`dailyStats`, `weeklyStats`）
3. 或者在新表中添加 `tag` 字段

## 示例代码

### MineScreen 完整示例

```kotlin
@Composable
fun MineScreen(
    viewModel: MainViewModel,
    onNavigateToSettings: () -> Unit = {}
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val nickname by viewModel.nickname.collectAsState()
    val timeFlux by viewModel.timeFlux.collectAsState()
    val userLevel by viewModel.userLevel.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 用户头像和信息
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 头像
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4CAF50))
            ) {
                Text(
                    text = nickname.take(1),
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = nickname,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = userLevel.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 光流余额卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF2A2A3E)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Time Flux",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    Text(
                        text = timeFlux.toString(),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFEB3B)
                    )
                }
                
                Icon(
                    Icons.Default.Star,
                    contentDescription = "Time Flux",
                    tint = Color(0xFFFFEB3B),
                    modifier = Modifier.size(48.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 用户 ID（可选）
        currentUser?.let { user ->
            Text(
                text = "用户 ID: ${user.userId.take(8)}...",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}
```

### StatsScreen 简单示例

```kotlin
@Composable
fun StatsScreenSimple(viewModel: MainViewModel) {
    val todayMinutes by viewModel.todayTotalMinutes.collectAsState()
    val weekMinutes by viewModel.weekTotalMinutes.collectAsState()
    val timeFlux by viewModel.timeFlux.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "统计数据",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 统计卡片
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                title = "今日",
                value = "${todayMinutes / 60}h ${todayMinutes % 60}m",
                color = Color(0xFF4CAF50)
            )
            
            StatCard(
                modifier = Modifier.weight(1f),
                title = "本周",
                value = "${weekMinutes / 60}h ${weekMinutes % 60}m",
                color = Color(0xFF2196F3)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 光流余额
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF2A2A3E)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Time Flux 余额", color = Color.White)
                Text(
                    text = timeFlux.toString(),
                    color = Color(0xFFFFEB3B),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    color: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = color
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}
```

## 总结

### 已完成 ✅

1. **MainViewModel 数据源对齐**
   - 添加 `todayRecordsNew`, `weekRecordsNew`
   - 添加 `todayTotalMinutes`, `weekTotalMinutes`
   - `currentUser`, `nickname`, `timeFlux` 已就绪

2. **数据流设计**
   - 所有数据从 Repository 获取
   - 使用 Flow 实现响应式更新
   - 支持实时观察用户状态

### 待完成 📋

1. **更新 MineScreen**
   - 使用 `viewModel.timeFlux` 显示光流余额
   - 使用 `viewModel.nickname` 显示昵称
   - 使用 `viewModel.currentUser` 显示用户信息

2. **更新 StatsScreen**
   - 使用 `viewModel.todayTotalMinutes` 显示今日统计
   - 使用 `viewModel.weekTotalMinutes` 显示本周统计
   - 可选：继续使用旧数据源的图表功能

### 注意事项 ⚠️

1. **新表没有 tag 字段**：如果需要按标签分类的图表，需要继续使用旧表或在新表中添加 tag 字段
2. **数据同步**：新旧表同时写入，确保兼容性
3. **性能优化**：使用 `collectAsState()` 自动管理订阅生命周期

现在可以开始更新 UI 层了！
