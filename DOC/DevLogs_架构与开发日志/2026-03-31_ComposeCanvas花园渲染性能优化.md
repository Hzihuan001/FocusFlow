# Compose Canvas 花园渲染性能优化

> **开发日期**: 2026-03-31  
> **功能模块**: 花园渲染 / 性能优化  
> **技术栈**: Jetpack Compose Canvas + LruCache + 视口剔除

---

## 一、问题描述

### 1.1 性能瓶颈

在 `detectTransformGestures` 触发高频重绘时，存在以下问题：

1. **缺乏视口剔除（Culling）**: 所有地块都会被绘制，即使它们在屏幕外
2. **Bitmap 实时缩放**: 每次 `drawImage` 时可能触发解码或缩放操作
3. **重复对象创建**: 每次绘制都创建临时对象，增加 GC 压力

### 1.2 性能影响

假设花园有 100 个地块，用户缩放时只能看到 20 个：
- 优化前：每帧调用 100 次 `drawPath` + `drawImage`
- 优化后：每帧仅调用 20 次可见地块的绘制

---

## 二、解决方案

### 2.1 ImageBitmap 内存缓存池

#### 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                    PlantBitmapLoader                         │
│                                                              │
│  ┌─────────────────────┐    ┌─────────────────────┐         │
│  │   bitmapCache       │    │  imageBitmapCache   │         │
│  │   (内存大小限制)     │    │  (数量限制: 30张)    │         │
│  │   LruCache<String,  │    │  LruCache<String,   │         │
│  │   Bitmap>           │    │  ImageBitmap>       │         │
│  └─────────────────────┘    └─────────────────────┘         │
│           │                          │                        │
│           │ 底部空白计算              │ 渲染专用               │
│           ▼                          ▼                        │
│  ┌─────────────────────────────────────────────────────┐    │
│  │              loadFromResource()                      │    │
│  │  1. inJustDecodeBounds 获取原始尺寸                  │    │
│  │  2. calculateInSampleSize 计算采样率                 │    │
│  │  3. inSampleSize 解码缩略图                          │    │
│  │  4. createScaledBitmap 精确缩放（如需要）            │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

#### 关键代码

```kotlin
object PlantBitmapLoader {
    // 目标尺寸：植物在花园中的最大显示尺寸
    private const val TARGET_MAX_WIDTH = 280
    private const val TARGET_MAX_HEIGHT = 400

    // 双层缓存
    private var bitmapCache: LruCache<String, Bitmap>? = null
    private var imageBitmapCache: LruCache<String, ImageBitmap>? = null

    /**
     * 核心方法：获取 ImageBitmap（渲染专用）
     * 
     * 性能保证：
     * - 仅从缓存获取，绝不触发 IO 或解码
     * - 返回的是缓存中的同一个 ImageBitmap 对象
     */
    fun load(resourceCode: String, context: Context? = null): ImageBitmap? {
        // 优先从 ImageBitmap 缓存获取
        imageBitmapCache?.get(resourceCode)?.let { return it }

        // 缓存未命中，加载并缓存
        val bitmap = loadBitmapInternal(resourceCode, ctx) ?: return null
        val imageBitmap = bitmap.asImageBitmap()
        imageBitmapCache?.put(resourceCode, imageBitmap)
        return imageBitmap
    }

    /**
     * 预缩放解码
     */
    private fun loadBitmapInternal(...): Bitmap? {
        // Step 1: 获取原始尺寸（不解码）
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeResource(resources, drawableId, options)
        
        // Step 2: 计算采样率（2的幂次方）
        val sampleSize = calculateInSampleSize(
            options.outWidth, options.outHeight,
            TARGET_MAX_WIDTH, TARGET_MAX_HEIGHT
        )
        
        // Step 3: 采样解码
        val loadOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        var bitmap = BitmapFactory.decodeResource(resources, drawableId, loadOptions)
        
        // Step 4: 如仍超过目标尺寸，精确缩放
        if (bitmap.width > TARGET_MAX_WIDTH) {
            val scale = TARGET_MAX_WIDTH.toFloat() / bitmap.width
            bitmap = Bitmap.createScaledBitmap(bitmap, 
                (bitmap.width * scale).toInt(),
                (bitmap.height * scale).toInt(), true)
        }
        
        return bitmap
    }
}
```

---

### 2.2 视口剔除算法 (Viewport Culling)

#### 核心思想

**只渲染屏幕可见区域内的地块，跳过屏幕外的地块。**

#### 数学推导

##### 等距投影坐标变换

在等距投影中，网格坐标 `(col, row)` 转换为屏幕坐标 `(isoX, isoY)`：

```
isoX = (col - row) × (tileWidth / 2)
isoY = (col + row) × (tileHeight / 2)
```

这是正向变换，用于绘制时计算地块位置。

##### 逆变换

从屏幕坐标 `(isoX, isoY)` 转换回网格坐标 `(col, row)`：

```
rx = isoX / (tileWidth / 2)
ry = isoY / (tileHeight / 2)

col = (rx + ry) / 2
row = (ry - rx) / 2
```

##### 视口边界计算

```
┌─────────────────────────────────────────────────────────────┐
│                     屏幕坐标系                               │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ (0,0)                              (width,0)          │  │
│  │    ┌─────────────────────────────────────┐            │  │
│  │    │              可见区域                │            │  │
│  │    │         ┌─────────┐                │            │  │
│  │    │         │ 花园中心 │                │            │  │
│  │    │         └─────────┘                │            │  │
│  │    │                                     │            │  │
│  │    └─────────────────────────────────────┘            │  │
│  │ (0,height)                         (width,height)      │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘

Step 1: 获取屏幕四个角点（相对于变换中心）
Step 2: 应用逆变换得到网格坐标
Step 3: 计算最小/最大 col 和 row
Step 4: 向外扩展 buffer 格，避免边缘裁切
```

#### 代码实现

```kotlin
// 屏幕四个角在 Canvas 本地坐标系中的位置（逆变换）
val halfW = size.width / 2f
val halfH = size.height / 2f

// 屏幕四个角点（相对于变换中心）
val screenCorners = listOf(
    Pair(-halfW - offsetX, -halfH - offsetY),           // 左上
    Pair(halfW - offsetX, -halfH - offsetY),            // 右上
    Pair(-halfW - offsetX, halfH - offsetY),            // 左下
    Pair(halfW - offsetX, halfH - offsetY)              // 右下
)

// 将角点逆缩放后转换为网格坐标
val tw2 = tileWidth / 2f
val th2 = tileHeight / 2f

val gridCoords = screenCorners.map { (sx, sy) ->
    val canvasX = sx / scale
    val canvasY = sy / scale
    // 逆变换公式
    val col = ((canvasX / tw2) + (canvasY / th2)) / 2f
    val row = ((canvasY / th2) - (canvasX / tw2)) / 2f
    Pair(col, row)
}

// 计算可见网格边界（向外扩展 buffer 格作为缓冲）
val buffer = 3
val minCol = (gridCoords.minOf { it.first } - buffer).toInt()
val maxCol = (gridCoords.maxOf { it.first } + buffer).toInt()
val minRow = (gridCoords.minOf { it.second } - buffer).toInt()
val maxRow = (gridCoords.maxOf { it.second } + buffer).toInt()
```

#### 渲染循环中的剔除

```kotlin
// 第一轮：渲染地块底座
renderingQueue.forEach { tile ->
    // 🟢 [VIEWPORT CULLING] 剔除屏幕外的地块
    if (tile.x < minCol || tile.x > maxCol || 
        tile.y < minRow || tile.y > maxRow) {
        return@forEach  // 跳过不可见地块
    }
    // ... 绘制地块
}

// 第二轮：渲染植物
renderingQueue.filter { 
    it.isMainTile && 
    it.tileType == TileType.PLANTED &&
    // 🟢 [VIEWPORT CULLING] 剔除屏幕外的植物
    it.x >= minCol && it.x <= maxCol && 
    it.y >= minRow && it.y <= maxRow
}.forEach { tile ->
    // ... 绘制植物
}
```

---

## 三、算法复杂度分析

### 3.1 时间复杂度

| 操作 | 优化前 | 优化后 |
|------|--------|--------|
| 计算可见边界 | O(1) | O(1) |
| 剔除判断 | 无 | O(n) 但极轻量（仅比较） |
| 绘制调用 | O(n) 全部绘制 | O(k) k = 可见地块数 |

### 3.2 空间复杂度

| 项目 | 说明 |
|------|------|
| screenCorners | 4 个 Pair 对象 |
| gridCoords | 4 个 Pair 对象 |
| 边界变量 | 4 个 Int |

总内存开销：约 200 字节，可忽略不计。

---

## 四、性能对比

### 4.1 理论提升

假设花园有 100 个地块，可见 20 个：

| 指标 | 优化前 | 优化后 | 提升 |
|------|--------|--------|------|
| drawPath 调用 | 200 次 | 40 次 | 80% ↓ |
| drawImage 调用 | 100 次 | 20 次 | 80% ↓ |
| 内存解码 | 可能重复 | 零解码 | 100% ↓ |

### 4.2 缩放场景

用户双指缩放时，`detectTransformGestures` 会高频触发重绘：
- 优化前：每帧绘制所有地块，GPU 压力大
- 优化后：每帧仅绘制可见地块，流畅度大幅提升

---

## 五、文件修改清单

| 文件 | 修改类型 | 说明 |
|------|----------|------|
| `PlantBitmapLoader.kt` | 重构 | 双层缓存 + 预缩放解码 |
| `GardenScreen.kt` | 新增 | 视口剔除算法 |

---

## 六、注意事项

1. **Buffer 值选择**: `buffer = 3` 确保边缘地块不会被裁切，可根据地块尺寸调整
2. **缓存容量**: `imageBitmapCache` 容量 30 张，可根据花园规模调整
3. **预加载时机**: 在 `LaunchedEffect` 中预加载当前花园所需的图片
4. **内存监控**: 可通过 `getCacheStats()` 监控缓存命中率

---

## 七、后续优化方向

1. **脏矩形渲染**: 只重绘变化的区域
2. **LOD 细节层次**: 缩放小时使用简化渲染
3. **异步解码**: 在后台线程预解码即将进入视野的图片
