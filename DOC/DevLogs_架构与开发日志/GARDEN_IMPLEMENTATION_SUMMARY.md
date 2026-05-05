# FocusFlow 花园系统实现总结

## 已完成的工作

### 1. MainViewModel 重构 ✅

完全重写了 MainViewModel，使用 Repository 层：

**依赖注入：**
```kotlin
private val userRepository = UserRepository(database.userDao())
private val focusRepository = FocusRepository(...)
private val gardenRepository = GardenRepository(...)
```

**状态管理：**
```kotlin
// 观察当前用户（用于 MineScreen 显示金币和昵称）
val currentUser: StateFlow<UserEntity?> = userRepository.getCurrentUserFlow()

// 派生状态
val nickname: StateFlow<String> = currentUser.map { it?.nickname ?: "Focus Runner" }
val timeFlux: StateFlow<Int> = currentUser.map { it?.timeFlux ?: 0 }
```

**核心方法 saveFocusRecord：**
```kotlin
fun saveFocusRecord(taskName: String, minutes: Int, tag: String) {
    // 1. 完成专注会话（Repository 内部已封装：插入记录 + 增加 timeFlux）
    focusRepository.completeFocusSession(userId, taskName, minutes, startTime)
    
    // 2. 更新花园中所有植物的生长值
    gardenRepository.updatePlantGrowth(userId, minutes)
    
    // 3. 更新用户等级
    // 4. 保持向后兼容（插入旧表）
}
```

### 2. GardenScreen 交互逻辑 ✅

完全重写了 GardenScreen，添加完整的交互功能：

**渲染层：**
- ✅ 监听 `viewModel.plants` - 植物列表
- ✅ 监听 `viewModel.visibleTiles` - 可种植区域（迷雾探索算法）
- ✅ 根据 `plant.width/height` 正确绘制植物图片
- ✅ 根据生长阶段选择图片（plant_stage_1 到 plant_stage_4）
- ✅ 高亮可种植区域（半透明绿色方块）

**交互层：**
- ✅ 点击事件处理（触摸坐标 → 世界坐标 → 网格坐标）
- ✅ **情况 A - 点击植物**：
  - 检测点击是否在植物占用区域内（考虑 width/height）
  - 显示植物详情弹窗
  - 如果成熟可以收获
  
- ✅ **情况 B - 点击空地**：
  - 可种植区 → 打开背包选种子
  - 迷雾区 → 提示 "需要先点亮周围的土地"

**植物详情弹窗：**
```kotlin
@Composable
fun PlantDetailDialog(
    plant: GardenEntity,
    plantInfo: PlantDictEntity,
    onDismiss: () -> Unit,
    onHarvest: () -> Unit
)
```
- 显示植物名称、状态、生长进度、位置、尺寸
- 成熟植物显示"收获"按钮

### 3. BagBottomSheet 背包弹窗 ✅

创建了完整的背包界面组件：

**功能：**
- ✅ 列出 `app_user_bag` 中的所有种子
- ✅ 显示种子数量和植物信息
- ✅ 点击种子触发 `onSeedSelected(plantId)` 回调
- ✅ 集成到 GardenScreen 完成种植流程

**UI 特性：**
- Material 3 ModalBottomSheet
- 赛博朋克风格配色
- 显示植物名称、描述、尺寸、生长时间
- 空状态提示
- 种子数量徽章

**种植流程：**
```
1. 用户点击可种植区域
   ↓
2. viewModel.setPendingPlantPosition(x, y)
   ↓
3. 打开 BagBottomSheet
   ↓
4. 用户选择种子
   ↓
5. onSeedSelected(plantId) 触发
   ↓
6. viewModel.plantSeed(x, y, plantId)
   ↓
7. Repository 执行：
   - 检查背包种子
   - 碰撞检测
   - 事务：扣减种子 + 插入花园记录
   ↓
8. 刷新花园数据
   ↓
9. 显示"种植成功"消息
```

### 4. GardenViewModel 增强 ✅

添加了背包相关的状态和方法：

**新增状态：**
```kotlin
// 背包物品
val bagItems: StateFlow<List<UserBagEntity>>

// 待种植的坐标
val pendingPlantPosition: StateFlow<Pair<Int, Int>?>
```

**新增方法：**
```kotlin
// 设置待种植位置（打开背包前）
fun setPendingPlantPosition(x: Int, y: Int)

// 清除待种植位置
fun clearPendingPlantPosition()
```

**数据加载：**
- 在 `loadGardenData()` 中同时加载背包物品
- 自动刷新背包数据

## 完整的种植流程

### 用户操作流程：
1. **完成专注任务** → 获得 Time Flux 和种子
2. **打开花园界面** → 看到绿色高亮的可种植区域
3. **点击可种植区域** → 打开背包弹窗
4. **选择种子** → 自动种植到点击的位置
5. **等待植物成长** → 完成更多专注任务加速生长
6. **植物成熟** → 点击植物收获，获得 Time Flux 奖励

### 技术流程：
```
用户点击可种植区域
  ↓
GardenScreen.handleTileClick()
  ↓
viewModel.setPendingPlantPosition(x, y)
  ↓
showBagSheet = true
  ↓
BagBottomSheet 显示
  ↓
用户选择种子
  ↓
onSeedSelected(plantId)
  ↓
viewModel.plantSeed(x, y, plantId)
  ↓
GardenRepository.plantSeed()
  ↓
事务执行：
  1. 检查背包种子数量
  2. 获取植物 width/height
  3. 碰撞检测（考虑其他植物的锚点+尺寸）
  4. 扣减背包种子
  5. 插入花园记录（syncStatus=1）
  ↓
loadGardenData() 刷新
  ↓
显示"种植成功"消息
```

## 关键算法

### 1. 迷雾探索算法
```kotlin
// 获取所有成熟植物 (status=1)
val maturePlants = gardenRepository.getMaturePlants(userId)

// 初始可见区域（原点周围）
for (x in -1..1) {
    for (y in -1..1) {
        visibleSet.add(Pair(x, y))
    }
}

// 为每个成熟植物添加周围一圈的可见区域
for (plant in maturePlants) {
    val plantInfo = plantDict[plant.plantId]
    if (plantInfo != null) {
        // 计算植物占用区域
        val plantMinX = plant.x
        val plantMaxX = plant.x + plantInfo.width - 1
        val plantMinY = plant.y
        val plantMaxY = plant.y + plantInfo.height - 1
        
        // 添加周围一圈
        for (x in (plantMinX - 1)..(plantMaxX + 1)) {
            for (y in (plantMinY - 1)..(plantMaxY + 1)) {
                visibleSet.add(Pair(x, y))
            }
        }
    }
}
```

### 2. 碰撞检测算法
```kotlin
// 计算新植物占用区域
val newPlantMinX = x
val newPlantMaxX = x + width - 1
val newPlantMinY = y
val newPlantMaxY = y + height - 1

// 检查每个现有植物
for (existingPlant in existingPlants) {
    val existingPlantDict = plantDictDao.getPlantById(existingPlant.plantId)
    
    // 计算现有植物占用区域
    val existingMinX = existingPlant.x
    val existingMaxX = existingPlant.x + existingPlantDict.width - 1
    val existingMinY = existingPlant.y
    val existingMaxY = existingPlant.y + existingPlantDict.height - 1
    
    // 检查两个矩形是否重叠
    val isOverlapping = !(
        newPlantMaxX < existingMinX ||  // 新植物在左边
        newPlantMinX > existingMaxX ||  // 新植物在右边
        newPlantMaxY < existingMinY ||  // 新植物在上边
        newPlantMinY > existingMaxY     // 新植物在下边
    )
    
    if (isOverlapping) {
        return CollisionResult(isValid = false, message = "该位置已被占用")
    }
}
```

### 3. 点击检测算法
```kotlin
// 触摸坐标 → 世界坐标
val worldX = (tapOffset.x - centerX - offsetX) / scale
val worldY = (tapOffset.y - centerY - offsetY) / scale

// 世界坐标 → 网格坐标（逆等轴测变换）
val gridX = ((worldX / (tileWidth / 2f)) + (worldY / (tileHeight / 2f))) / 2
val gridY = ((worldY / (tileHeight / 2f)) - (worldX / (tileWidth / 2f))) / 2

val x = gridX.toInt()
val y = gridY.toInt()

// 检查是否点击了植物（考虑 width/height）
val clickedPlant = plants.find { plant ->
    val plantInfo = viewModel.getPlantRenderInfo(plant)
    if (plantInfo != null) {
        x >= plant.x && x < plant.x + plantInfo.width &&
        y >= plant.y && y < plant.y + plantInfo.height
    } else {
        false
    }
}
```

## 文件清单

### 新增文件：
1. `app/src/main/java/com/example/focusflow/ui/components/BagBottomSheet.kt` - 背包弹窗组件
2. `GARDEN_IMPLEMENTATION_SUMMARY.md` - 本文档

### 更新文件：
1. `app/src/main/java/com/example/focusflow/ui/MainViewModel.kt` - 完全重写
2. `app/src/main/java/com/example/focusflow/ui/screens/GardenScreen.kt` - 完全重写
3. `app/src/main/java/com/example/focusflow/ui/viewmodel/GardenViewModel.kt` - 添加背包支持

## 下一步工作

### 必需：
1. **初始化植物字典数据** - 在应用启动时预加载 PlantDictEntity
2. **初始化用户背包** - 给新用户一些初始种子
3. **测试完整流程** - 从专注到种植到收获

### 可选：
1. **商店系统** - 使用 Time Flux 购买种子
2. **植物动画** - 添加生长动画和收获特效
3. **成就系统** - 解锁特殊植物
4. **社交功能** - 访问好友花园

## 总结

✅ 完成了 MainViewModel 重构（使用 Repository 层）
✅ 完成了 GardenScreen 交互逻辑（渲染 + 点击 + 弹窗）
✅ 完成了 BagBottomSheet 背包弹窗
✅ 完成了完整的种植流程（点击 → 选种子 → 种植 → 刷新）

所有核心功能已实现，可以开始测试和优化！
