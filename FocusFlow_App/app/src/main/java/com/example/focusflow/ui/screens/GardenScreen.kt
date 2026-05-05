package com.example.focusflow.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Inventory
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.focusflow.R
import com.example.focusflow.ui.components.InventorySheet
import com.example.focusflow.ui.components.SkeletonList
import com.example.focusflow.ui.components.SkeletonLeaderboardItem
import com.example.focusflow.ui.components.VitalityStateIndicator
import com.example.focusflow.ui.components.LeaderboardDialog
import com.example.focusflow.ui.components.PulseChargeButton
import com.example.focusflow.ui.components.NetworkUnavailableScreen
import com.example.focusflow.utils.calculateBottomPadding
import com.example.focusflow.ui.theme.*
import com.example.focusflow.ui.viewmodel.GardenViewModel
import com.example.focusflow.ui.viewmodel.GardenVitalityState
import com.example.focusflow.ui.viewmodel.GardenVitality
import com.example.focusflow.ui.viewmodel.SocialViewModel
import com.example.focusflow.ui.viewmodel.Friend
import com.example.focusflow.service.NetworkMonitor
import com.example.focusflow.utils.PlantBitmapLoader
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import kotlin.math.abs
import androidx.compose.ui.geometry.Offset

/**
 * 花园界面
 * 
 * 功能：
 * 1. 渲染植物（根据 width/height 正确绘制）
 * 2. 显示可种植区域（迷雾探索算法）
 * 3. 点击交互：
 *    - 点击植物 -> 显示详情/收获
 *    - 点击可种植区 -> 打开背包选种子
 *    - 点击迷雾区 -> 提示需要解锁
 */
@Composable
fun GardenScreen(
    onBack: () -> Unit = {},
    onNavigateToBag: () -> Unit = {},
    onNavigateToStats: () -> Unit = {},
    onNavigateToSocial: () -> Unit = {},
    guestMode: Boolean = false,
    viewModel: GardenViewModel = viewModel(),
    socialViewModel: SocialViewModel = viewModel()
) {
    val context = LocalContext.current
    val appColors = LocalAppColors.current

    // ═══════════════════════════════════════════════════════════════
    // 网络状态检查 - 断网时显示警告横幅而非完全阻止
    // ═══════════════════════════════════════════════════════════════
    val networkStatus by viewModel.networkStatus.collectAsState()
    val isConnected = networkStatus is com.example.focusflow.service.NetworkMonitor.NetworkStatus.Connected
    val isChecking = networkStatus is com.example.focusflow.service.NetworkMonitor.NetworkStatus.Checking

    // 加载状态
    val isLoading by viewModel.isLoading.collectAsState()

    // 离线重试状态
    var isRetrying by remember { mutableStateOf(false) }

    // 2.5D 参数 (纪念碑谷美学：大体块)
    val tileWidth = 280f       // 增大尺寸
    val tileHeight = tileWidth / 2f
    val blockHeight = 28f      // 削减厚度，回归精致全息感

    // ═══════════════════════════════════════════════════════════════
    // 🟢 [DEFERRED RENDERING] 延迟渲染状态
    // 
    // 解决首帧卡顿：页面进入时立即渲染骨架，延迟 300ms 后触发完整渲染
    // 状态流转：false (骨架) → true (完整渲染 + 全息扫描动画)
    // ═══════════════════════════════════════════════════════════════
    val isGridReady by viewModel.isGridReady.collectAsState()
    
    // 全息扫描线动画进度 (0.0 ~ 1.0)
    var scanProgress by remember { mutableStateOf(0f) }
    
    // 🟢 [DEFERRED RENDERING] 延迟触发完整渲染
    // 等待页面切换动画完成后再组装完整数据
    LaunchedEffect(Unit) {
        // 等待 300ms，让页面切换动画完成
        kotlinx.coroutines.delay(300)
        // 触发完整渲染
        viewModel.setGridReady(true)
    }
    
    // 🟢 [HOLOGRAPHIC SCAN] 全息扫描线动画
    // 当 isGridReady 变为 true 时，启动扫描动画
    LaunchedEffect(isGridReady) {
        if (isGridReady) {
            // 动画：扫描线从 0 到 1（800ms）
            val durationMs = 800
            val startTime = System.currentTimeMillis()
            while (scanProgress < 1f) {
                val elapsed = System.currentTimeMillis() - startTime
                scanProgress = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
                kotlinx.coroutines.delay(16) // ~60fps
            }
        }
    }

    // 观察 ViewModel 状态
    val operationMessage by viewModel.operationMessage.collectAsState()

    // 种植状态流 (Holographic Plant State)
    val placementMode by viewModel.placementMode.collectAsState()
    val gardenTiles by viewModel.gardenTiles.collectAsState()
    val plantableTiles by viewModel.plantableTiles.collectAsState()
    
    // 兼容旧代码的状态
    val plants by viewModel.plants.collectAsState()
    val plantDict by viewModel.plantDict.collectAsState()
    val purifiedTiles by viewModel.purifiedTiles.collectAsState()
    val pendingPlantPosition by viewModel.pendingPlantPosition.collectAsState()
    val plantedItems by viewModel.plantedItems.collectAsState()

    // 社交状态监听
    val visitingFriend by socialViewModel.visitingFriend.collectAsState()
    val isPulseLoading by socialViewModel.isPulseLoading.collectAsState()
    val hasChargedToday by socialViewModel.hasChargedToday.collectAsState()
    val allPlantsGlowing by socialViewModel.allPlantsGlowing.collectAsState()
    val pulseMessage by socialViewModel.pulseMessage.collectAsState()
    
    // 当前用户ID（用于排行榜高亮）
    val currentUserId by viewModel.currentUserId.collectAsState()

    // ═══════════════════════════════════════════════════════════════
    // 访客模式：加载好友花园数据
    // ═══════════════════════════════════════════════════════════════
    LaunchedEffect(guestMode, visitingFriend) {
        if (guestMode && visitingFriend != null) {
            Log.d("GardenScreen", "访客模式：加载好友花园, friendId=${visitingFriend?.userId}")
            viewModel.loadFriendGarden(visitingFriend!!.userId)
        } else if (!guestMode) {
            // 非访客模式：加载自己的花园
            Log.d("GardenScreen", "正常模式：加载自己的花园")
            viewModel.loadGardenFromCloud()
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 惰性枯萎状态机 (Lazy Wither State Machine)
    // ═══════════════════════════════════════════════════════════════
    val gardenVitality by viewModel.gardenVitality.collectAsState()
    val vitalityState = gardenVitality.state

    // ══ 纪念碑谷：全域环境光动画状态 (逻辑反转：从暗淡到原图) ══
    val pulseFactor by animateFloatAsState(
        targetValue = if (!guestMode || hasChargedToday) 1.0f else 0.4f,
        animationSpec = tween(1500, easing = LinearEasing),
        label = "pulseFactor"
    )

    // ═══════════════════════════════════════════════════════════════
    // 惰性枯萎状态机：动态滤镜矩阵生成器
    // ═══════════════════════════════════════════════════════════════
    //
    // 【技术原理】
    // ColorMatrix 是 4x5 矩阵，作用于 RGBA 向量：
    // | R' |   | a00 a01 a02 a03 a04 |   | R |
    // | G' | = | a10 a11 a12 a13 a14 | × | G |
    // | B' |   | a20 a21 a22 a23 a24 |   | B |
    // | A' |   | a30 a31 a32 a33 a34 |   | A |
    //                                 | 1 |
    //
    // 【灰度转换】
    // 灰度 = 0.299R + 0.587G + 0.114B (ITU-R BT.601 标准)
    // 矩阵实现：将 RGB 通道映射为相同的灰度值
    //
    // 【饱和度调节】
    // 通过插值在单位矩阵(饱和度=1)和灰度矩阵(饱和度=0)之间
    //
    val saturationFactor by animateFloatAsState(
        targetValue = vitalityState.saturationMultiplier,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "saturationAnimation"
    )

    val brightnessFactor by animateFloatAsState(
        targetValue = vitalityState.brightnessMultiplier,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "brightnessAnimation"
    )

    val glowIntensity by animateFloatAsState(
        targetValue = vitalityState.glowIntensity,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "glowAnimation"
    )

    // 综合颜色滤镜：饱和度 + 亮度 + 脉冲因子
    val vitalityColorFilter = remember(saturationFactor, brightnessFactor, pulseFactor) {
        // 灰度转换基础矩阵 (ITU-R BT.601)
        val grayScale = floatArrayOf(
            0.299f, 0.587f, 0.114f, 0f, 0f,
            0.299f, 0.587f, 0.114f, 0f, 0f,
            0.299f, 0.587f, 0.114f, 0f, 0f,
            0f,     0f,     0f,     1f, 0f
        )

        // 单位矩阵 (保持原色)
        val identity = floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )

        // 在灰度和原色之间插值（saturationFactor = 1 时为原色，= 0 时为灰度）
        val saturationMatrix = FloatArray(20) { i ->
            identity[i] * saturationFactor + grayScale[i] * (1f - saturationFactor)
        }

        // 应用亮度因子和脉冲因子
        val finalMatrix = FloatArray(20) { i ->
            // 对角线元素 (亮度调节)
            if (i % 5 == i / 5 && i < 15) {
                saturationMatrix[i] * brightnessFactor * pulseFactor
            } else {
                saturationMatrix[i]
            }
        }

        ColorFilter.colorMatrix(androidx.compose.ui.graphics.ColorMatrix(finalMatrix))
    }

    // 种植位置确认弹窗状态
    var deployConfirmPosition by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    // ✒️ 注入信标弹窗状态 (客态模式留言)
    var showBeaconDialog by remember { mutableStateOf(false) }
    var beaconText by remember { mutableStateOf("") }

    // 🟢 [DYNAMIC OASIS] 坐标拓扑计算 (互斥集合生成)
    // 注意：这个变量只在首次渲染时计算，点击处理中不使用它

    // ════════════════════════════════════════════════════════════════════════
    // 🔧 [MULTI-TILE] 构建植物实例到中心坐标的映射
    // 使用 deployTime + plantId 作为实例标识
    // 找到每个实例所有地块的几何中心作为渲染位置
    // ════════════════════════════════════════════════════════════════════════
    val mainTileMap = remember(gardenTiles) {
        val instanceTiles = mutableMapOf<String, MutableList<Pair<Int, Int>>>()
        gardenTiles.filter { it.plantId != null && it.deployTime != null }.forEach { tile ->
            val instanceKey = "${tile.deployTime}_${tile.plantId}"
            instanceTiles.getOrPut(instanceKey) { mutableListOf() }.add(tile.x to tile.y)
        }
        
        // 计算每个实例的几何中心（取所有坐标的平均值）
        val map = mutableMapOf<String, Pair<Int, Int>>()
        instanceTiles.forEach { (key, coords) ->
            if (coords.isNotEmpty()) {
                val avgX = coords.map { it.first }.average().toInt()
                val avgY = coords.map { it.second }.average().toInt()
                val center = coords.minByOrNull { 
                    kotlin.math.abs(it.first - avgX) + kotlin.math.abs(it.second - avgY) 
                } ?: coords.first()
                map[key] = center
            }
        }
        map
    }

    // 🟢 [UNIFIED RENDER] 统一深度可视化队列 (实现互斥渲染与 Z-Index 排序)
    // 🔧 [MULTI-TILE] 计算多格植物的主坐标，避免重复渲染
    val renderingQueue = remember(gardenTiles, plants, plantableTiles, mainTileMap) {
        val queue = mutableListOf<VisualTile>()
        
        // 1. 实装资产优先 - 区分植物占领区和照亮区
        gardenTiles.forEach { tile -> 
            val instanceKey = if (tile.plantId != null && tile.deployTime != null) {
                "${tile.deployTime}_${tile.plantId}"
            } else {
                null
            }
            val isMainTile = if (instanceKey != null) {
                mainTileMap[instanceKey] == (tile.x to tile.y)
            } else {
                true
            }
            
            // 🔧 [TILE TYPE] 区分地块类型
            val tileType = when {
                tile.plantId != null -> TileType.PLANTED      // 植物占领区
                tile.isPurified -> TileType.ILLUMINATED       // 植物照亮区
                else -> TileType.EMPTY                         // 未净化区域
            }
            
            queue.add(VisualTile(
                tile.x, tile.y, 
                tileType = tileType, 
                plantId = tile.plantId ?: 0, 
                tileId = tile.tileId,
                isMainTile = isMainTile
            ))
        }
        
        // 2. Legacy 植物兼容
        plants.forEach { p ->
            if (queue.none { it.x == p.x && it.y == p.y }) {
                queue.add(VisualTile(p.x, p.y, tileType = TileType.PLANTED, plantId = p.plantId, instanceId = p.instanceId))
            }
        }
        
        // 3. 开拓边缘补漏
        plantableTiles.forEach { (x, y) ->
            if (queue.none { it.x == x && it.y == y }) {
                queue.add(VisualTile(x, y, tileType = TileType.EMPTY))
            }
        }
        
        // 4. Z-排序：从后往前绘制 (纪念碑谷核心画家算法)
        queue.sortedBy { it.x + it.y }
    }

    // 显示操作消息
    LaunchedEffect(operationMessage) {
        operationMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearOperationMessage()
        }
    }

    // 选中的植物
    var selectedPlant by remember { mutableStateOf<String?>(null) }
    
    // 背包弹窗状态
    var showInventorySheet by remember { mutableStateOf(false) }
    
    // ═══════════════════════════════════════════════════════════════
    // 图片懒加载策略：使用 LRU 缓存 + 按需加载
    // ═══════════════════════════════════════════════════════════════
    val plantBottomPadding = remember {
        // 预计算底部空白比例（轻量级，仅计算不加载图片）
        mutableMapOf<String, Float>()
    }
    
    // 🟢 [PERF] 预加载当前花园需要的植物图片（从后端服务器加载）
    LaunchedEffect(plantDict, gardenTiles) {
        Log.d("GardenScreen", "预加载检查: plantDict=${plantDict.size}, gardenTiles=${gardenTiles.size}")
        
        val plantIds = gardenTiles.mapNotNull { it.plantId }.distinct()
        Log.d("GardenScreen", "花园中的植物ID: $plantIds")
        
        // 🟢 [CLOUD IMAGE] 从后端获取图片 URL
        val imageUrls = gardenTiles
            .mapNotNull { it.plantId }
            .mapNotNull { plantId ->
                val plant = plantDict[plantId]
                Log.v("GardenScreen", "植物ID=$plantId -> imageUrl=${plant?.imageUrl}")
                plant?.imageUrl
            }
            .filter { !it.isNullOrBlank() }
            .distinct()
        
        Log.d("GardenScreen", "需要预加载的图片URL: ${imageUrls.size}张")
        
        if (imageUrls.isNotEmpty()) {
            Log.d("GardenScreen", "预加载 ${imageUrls.size} 张植物图片")
            PlantBitmapLoader.preload(imageUrls, context)
            
            // 🟢 [PERF] 预计算底部空白比例，避免绘制时计算
            imageUrls.forEach { url ->
                if (!plantBottomPadding.containsKey(url)) {
                    PlantBitmapLoader.loadBitmap(url, context)?.let { bitmap ->
                        plantBottomPadding[url] = calculateBottomPadding(bitmap)
                    }
                }
            }
        } else {
            Log.w("GardenScreen", "没有需要预加载的植物图片")
        }
    }
    
    // 懒加载植物图片（从后端 URL 加载）
    fun getPlantBitmap(imageUrl: String?): ImageBitmap? {
        return PlantBitmapLoader.load(imageUrl, context)
    }

    // 视野控制
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var scale by remember { mutableStateOf(1f) }

    // 背景渐变：根据主题动态调整
    val backgroundBrush = if (appColors.isDark) {
        // 深色主题：纪念碑谷水之阁经典背景渐变
        Brush.verticalGradient(
            colors = listOf(WaterPavilionDeep, WaterPavilionDark)
        )
    } else {
        // 浅色主题：柔和的浅色渐变
        Brush.verticalGradient(
            colors = listOf(
                appColors.bgDeep,
                appColors.cardBg
            )
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(backgroundBrush)) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(visitingFriend) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        offsetX += pan.x
                        offsetY += pan.y
                        scale *= zoom
                    }
                }
                .pointerInput(visitingFriend) {
                    detectTapGestures { tapOffset ->
                        Log.d("GardenScreen", "检测到点击: tapOffset=$tapOffset, guestMode=$guestMode")
                        if (guestMode) return@detectTapGestures
                        
                        // 🟢 核心修复：对齐 RenderScope 的变换逻辑
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        
                        // 1. 逆向计算：从屏幕坐标转换回 Canvas 本地绝对坐标
                        val canvasX = (tapOffset.x - centerX - offsetX) / scale
                        val canvasY = (tapOffset.y - centerY - offsetY) / scale
                        
                        // 2. 逆向投影：从 Canvas 本地坐标转换回网格(row, col)
                        // Isometric 矩阵逆矩阵：
                        // gridX = (x / (tw/2) + y / (th/2)) / 2
                        // gridY = (y / (th/2) - x / (tw/2)) / 2
                        val rx = canvasX / (tileWidth / 2f)
                        val ry = canvasY / (tileHeight / 2f)
                        
                        val gridCol = (rx + ry) / 2f
                        val gridRow = (ry - rx) / 2f
                        
                        val col = kotlin.math.round(gridCol).toInt()
                        val row = kotlin.math.round(gridRow).toInt()

                        // 3. 碰撞区过滤：确保点击点在菱形内（距离菱心阈值判定）
                        val isoX = (col - row) * (tileWidth / 2f)
                        val isoY = (col + row) * (tileHeight / 2f)
                        val dx = abs(canvasX - isoX)
                        val dy = abs(canvasY - isoY)
                        
                        // 判定是否在菱形内：dx/(tw/2) + dy/(th/2) <= 1
                        if (dx / (tileWidth / 2f) + dy / (tileHeight / 2f) <= 1.1f) {
                            Log.d("GardenScreen", "点击地块: ($col, $row), plantId: ${gardenTiles.find { it.x == col && it.y == row }?.plantId}")
                            
                            if (placementMode != null) {
                                // 仅允许在 plantableTiles 区域种植
                                if (plantableTiles.contains(col to row)) {
                                    deployConfirmPosition = Pair(col, row)
                                } else {
                                    val tileAt = gardenTiles.find { it.x == col && it.y == row }
                                    if (tileAt?.plantId == null) {
                                        Toast.makeText(context, "此处超出生命矩阵辐射范围，无法实装！", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                return@detectTapGestures
                            }

                            // ══ 直接查找点击位置的地块 ══
                            val tileAt = gardenTiles.find { it.x == col && it.y == row }
                            
                            if (tileAt != null && tileAt.plantId != null) {
                                // ══ 点击植物占领区，显示植物信息卡片 ══
                                if (tileAt.deployTime != null) {
                                    // 多格植物：找主地块
                                    val instanceKey = "${tileAt.deployTime}_${tileAt.plantId}"
                                    val mainTileCoord = mainTileMap[instanceKey]
                                    if (mainTileCoord != null) {
                                        val mainTile = gardenTiles.find { 
                                            it.x == mainTileCoord.first && it.y == mainTileCoord.second 
                                        }
                                        if (mainTile != null) {
                                            selectedPlant = mainTile.tileId.toString()
                                        }
                                    } else {
                                        selectedPlant = tileAt.tileId.toString()
                                    }
                                } else {
                                    // 单格植物或旧数据
                                    selectedPlant = tileAt.tileId.toString()
                                }
                            } else if (plantableTiles.contains(col to row)) {
                                // 🟢 [ACTION] 点击可种植区域，打开背包
                                showInventorySheet = true
                            }
                        }
                    }
                }
        ) {
            val centerX = size.width / 2
            val centerY = size.height / 2

            // ═══════════════════════════════════════════════════════════════════════════════
            // 🟢 [VIEWPORT CULLING] 视口剔除算法
            // 
            // 原理：根据屏幕尺寸、偏移量、缩放比例，计算当前可见的网格坐标范围
            // 只渲染可见区域内的地块，大幅减少 drawPath 和 drawImage 调用次数
            // 
            // 数学推导：
            // 1. 屏幕四个角在变换后的 Canvas 本地坐标系中的位置
            // 2. 将屏幕角点逆变换为网格坐标
            // 3. 得到可见网格的边界框 (minCol, maxCol, minRow, maxRow)
            // 
            // 等距投影坐标变换：
            //   isoX = (col - row) * (tileWidth / 2)
            //   isoY = (col + row) * (tileHeight / 2)
            // 
            // 逆变换：
            //   col = (isoX / (tw/2) + isoY / (th/2)) / 2
            //   row = (isoY / (th/2) - isoX / (tw/2)) / 2
            // ═══════════════════════════════════════════════════════════════════════════════
            
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
                val col = ((canvasX / tw2) + (canvasY / th2)) / 2f
                val row = ((canvasY / th2) - (canvasX / tw2)) / 2f
                Pair(col, row)
            }
            
            // 计算可见网格边界（向外扩展 2 格作为缓冲，避免边缘裁切）
            val buffer = 3
            val minCol = (gridCoords.minOf { it.first } - buffer).toInt()
            val maxCol = (gridCoords.maxOf { it.first } + buffer).toInt()
            val minRow = (gridCoords.minOf { it.second } - buffer).toInt()
            val maxRow = (gridCoords.maxOf { it.second } + buffer).toInt()

            withTransform({
                translate(left = centerX + offsetX, top = centerY + offsetY)
                scale(scale, scale, pivot = Offset.Zero)
            }) {
                // ═══════════════════════════════════════════════════════════════════════════════
                // 🟢 [DEFERRED RENDERING] 延迟渲染 - 骨架状态
                // 
                // 当 isGridReady = false 时，只绘制轻量的赛博网格线骨架
                // 避免首帧卡顿，用户立即看到页面响应
                // ═══════════════════════════════════════════════════════════════════════════════
                if (!isGridReady) {
                    // 骨架状态：绘制赛博网格线（极简风格）
                    val skeletonPath = Path()
                    val skeletonColor = appColors.primary.copy(alpha = 0.15f)
                    
                    renderingQueue.forEach { tile ->
                        if (tile.x < minCol || tile.x > maxCol || tile.y < minRow || tile.y > maxRow) {
                            return@forEach
                        }
                        val col = tile.x
                        val row = tile.y
                        val isoX = (col - row) * (tileWidth / 2f)
                        val isoY = (col + row) * (tileHeight / 2f)
                        
                        // 只绘制线框菱形
                        skeletonPath.reset()
                        skeletonPath.moveTo(isoX, isoY - tileHeight / 2)
                        skeletonPath.lineTo(isoX + tileWidth / 2, isoY)
                        skeletonPath.lineTo(isoX, isoY + tileHeight / 2)
                        skeletonPath.lineTo(isoX - tileWidth / 2, isoY)
                        skeletonPath.close()
                        drawPath(skeletonPath, color = skeletonColor, style = Stroke(1f))
                    }
                    
                    // 绘制中央脉冲点（赛博朋克风格）
                    val pulseAlpha = (kotlin.math.sin(System.currentTimeMillis() / 200.0) * 0.3 + 0.5).toFloat()
                    drawCircle(
                        color = appColors.primary.copy(alpha = pulseAlpha),
                        radius = 8f,
                        center = Offset.Zero
                    )
                    
                    return@withTransform  // 骨架渲染完成，跳过后续完整渲染
                }
                
                // ═══════════════════════════════════════════════════════════════════════════════
                // 🟢 [HOLOGRAPHIC SCAN] 全息扫描线动画
                // 
                // 扫描线从屏幕顶部向下扫过，被扫过的区域才渲染真实植物
                // 扫描进度 scanProgress: 0.0 (顶部) → 1.0 (底部)
                // ═══════════════════════════════════════════════════════════════════════════════
                
                // 计算扫描线在 Canvas 本地坐标系中的 Y 位置
                // 需要考虑视口范围
                val viewportTopY = -halfH / scale
                val viewportBottomY = halfH / scale
                val scanY = viewportTopY + (viewportBottomY - viewportTopY) * scanProgress
                
                // ══ 🟢 [OPTIMIZED RENDER] 优化渲染 - 复用Path对象 ══
                // 使用单个Path对象避免每帧创建大量对象
                
                val reusablePath = Path()
                
                // ══ 第一轮：渲染所有底座（使用复用Path + 视口剔除） ══
                renderingQueue.forEach { tile ->
                    // 🟢 [VIEWPORT CULLING] 剔除屏幕外的地块
                    if (tile.x < minCol || tile.x > maxCol || tile.y < minRow || tile.y > maxRow) {
                        return@forEach  // 跳过不可见地块
                    }
                    val col = tile.x
                    val row = tile.y
                    val isoX = (col - row) * (tileWidth / 2f)
                    val isoY = (col + row) * (tileHeight / 2f)

                    when (tile.tileType) {
                        // ══ 植物占领区 ══
                        TileType.PLANTED -> {
                            // 顶面
                            reusablePath.reset()
                            reusablePath.moveTo(isoX, isoY - tileHeight / 2)
                            reusablePath.lineTo(isoX + tileWidth / 2, isoY)
                            reusablePath.lineTo(isoX, isoY + tileHeight / 2)
                            reusablePath.lineTo(isoX - tileWidth / 2, isoY)
                            reusablePath.close()
                            drawPath(reusablePath, color = MonumentMint.copy(alpha = 0.85f))

                            // 左侧面
                            reusablePath.reset()
                            reusablePath.moveTo(isoX - tileWidth / 2, isoY)
                            reusablePath.lineTo(isoX, isoY + tileHeight / 2)
                            reusablePath.lineTo(isoX, isoY + tileHeight / 2 + blockHeight)
                            reusablePath.lineTo(isoX - tileWidth / 2, isoY + blockHeight)
                            reusablePath.close()
                            drawPath(reusablePath, color = MonumentDeepMint)

                            // 右侧面
                            reusablePath.reset()
                            reusablePath.moveTo(isoX, isoY + tileHeight / 2)
                            reusablePath.lineTo(isoX + tileWidth / 2, isoY)
                            reusablePath.lineTo(isoX + tileWidth / 2, isoY + blockHeight)
                            reusablePath.lineTo(isoX, isoY + tileHeight / 2 + blockHeight)
                            reusablePath.close()
                            drawPath(reusablePath, color = MonumentDeepMint.copy(alpha = 0.8f))
                        }
                        
                        // ══ 植物照亮区 ══
                        TileType.ILLUMINATED -> {
                            val illuminateHeight = 8f

                            // 顶面
                            reusablePath.reset()
                            reusablePath.moveTo(isoX, isoY - tileHeight / 2)
                            reusablePath.lineTo(isoX + tileWidth / 2, isoY)
                            reusablePath.lineTo(isoX, isoY + tileHeight / 2)
                            reusablePath.lineTo(isoX - tileWidth / 2, isoY)
                            reusablePath.close()
                            drawPath(reusablePath, color = appColors.primary.copy(alpha = 0.4f))

                            // 侧面
                            reusablePath.reset()
                            reusablePath.moveTo(isoX - tileWidth / 2, isoY)
                            reusablePath.lineTo(isoX, isoY + tileHeight / 2)
                            reusablePath.lineTo(isoX + tileWidth / 2, isoY)
                            reusablePath.lineTo(isoX + tileWidth / 2, isoY + illuminateHeight)
                            reusablePath.lineTo(isoX, isoY + tileHeight / 2 + illuminateHeight)
                            reusablePath.lineTo(isoX - tileWidth / 2, isoY + illuminateHeight)
                            reusablePath.close()
                            drawPath(reusablePath, color = appColors.primary.copy(alpha = 0.3f))
                        }
                        
                        // ══ 未净化区域 ══
                        // 🟢 [PERF] 简化渲染，移除昂贵的渐变圆形，复用Path
                        TileType.EMPTY -> {
                            val pioneerHeight = 16f
                            val holographicTop = appColors.textSub.copy(alpha = 0.12f)
                            val holographicSide = appColors.textSub.copy(alpha = 0.06f)

                            // 顶面
                            reusablePath.reset()
                            reusablePath.moveTo(isoX, isoY - tileHeight / 2)
                            reusablePath.lineTo(isoX + tileWidth / 2, isoY)
                            reusablePath.lineTo(isoX, isoY + tileHeight / 2)
                            reusablePath.lineTo(isoX - tileWidth / 2, isoY)
                            reusablePath.close()
                            drawPath(reusablePath, color = holographicTop)

                            // 侧面
                            reusablePath.reset()
                            reusablePath.moveTo(isoX - tileWidth / 2, isoY)
                            reusablePath.lineTo(isoX, isoY + tileHeight / 2)
                            reusablePath.lineTo(isoX + tileWidth / 2, isoY)
                            reusablePath.lineTo(isoX + tileWidth / 2, isoY + pioneerHeight)
                            reusablePath.lineTo(isoX, isoY + tileHeight / 2 + pioneerHeight)
                            reusablePath.lineTo(isoX - tileWidth / 2, isoY + pioneerHeight)
                            reusablePath.close()
                            drawPath(reusablePath, color = holographicSide)
                        }
                    }
                }
                
                // ══ 第二轮：渲染所有植物（视口剔除 + 扫描线渐显） ══
                renderingQueue.filter { 
                    it.isMainTile && 
                    it.tileType == TileType.PLANTED &&
                    it.x >= minCol && it.x <= maxCol && 
                    it.y >= minRow && it.y <= maxRow  // 🟢 [VIEWPORT CULLING]
                }.forEach { tile ->
                    val plantInfo = plantDict[tile.plantId] ?: return@forEach
                    // 🟢 [CLOUD IMAGE] 从后端 URL 加载植物图片
                    val plantBitmap = getPlantBitmap(plantInfo.imageUrl) ?: return@forEach
                    
                    val col = tile.x
                    val row = tile.y
                    // 🔧 [CENTER RENDER] 点击地块就是中心，无需额外偏移
                    val isoX = (col - row) * (tileWidth / 2f)
                    val isoY = (col + row) * (tileHeight / 2f)
                    
                    // 🟢 [HOLOGRAPHIC SCAN] 扫描线渐显效果
                    // 只有被扫描线扫过的区域才渲染真实植物
                    if (isoY > scanY) {
                        // 未扫过区域：绘制半透明线框植物占位符
                        val placeholderPath = Path()
                        placeholderPath.moveTo(isoX, isoY - tileHeight / 2)
                        placeholderPath.lineTo(isoX + tileWidth / 2, isoY)
                        placeholderPath.lineTo(isoX, isoY + tileHeight / 2)
                        placeholderPath.lineTo(isoX - tileWidth / 2, isoY)
                        placeholderPath.close()
                        drawPath(
                            placeholderPath, 
                            color = appColors.primary.copy(alpha = 0.1f), 
                            style = Stroke(1f)
                        )
                        return@forEach  // 跳过真实植物渲染
                    }
                    
                    // 🟢 [DYNAMIC ASPECT RATIO] 计算原始宽高比
                    val bitmapWidth = plantBitmap.width.toFloat()
                    val bitmapHeight = plantBitmap.height.toFloat()
                    val aspectRatio = bitmapHeight / bitmapWidth
                    
                    // 🟢 [SCALE FIELD] 使用服务端 scale 字段控制植物显示大小
                    // 新格式：scale=100 → 1.0x (原大小), scale=50 → 0.5x, scale=200 → 2.0x
                    // 兼容旧格式：scale=10 → 1.0x, scale=20 → 2.0x, scale=5 → 0.5x
                    val rawScale = plantInfo.scale ?: 100
                    val sizeMultiplier = when {
                        rawScale >= 50 -> rawScale / 100f  // 新格式：百分比
                        rawScale == 10 -> 1.0f              // 旧格式兼容
                        rawScale == 20 -> 2.0f              // 旧格式兼容
                        rawScale == 5 -> 0.5f               // 旧格式兼容
                        rawScale == 1 -> 1.0f               // 旧格式兼容
                        rawScale <= 0 -> 1.0f               // 异常值保护
                        else -> rawScale / 10f               // 其他旧格式值
                    }
                    
                    // 🟢 [RELATIVE SCALING] 以地块宽度为基准计算最终尺寸
                    val baseWidth = tileWidth * 1.0f
                    val targetWidth = baseWidth * sizeMultiplier
                    val targetHeight = targetWidth * aspectRatio
                    
                    // 🟢 [BOTTOM ANCHOR] 植物底部贴在菱形下三分之一处
                    val bottomPadding = plantBottomPadding.getOrPut(plantInfo.imageUrl ?: plantInfo.resourceCode) {
                        // 使用 PlantBitmapLoader 获取原始 Bitmap 进行底部空白计算
                        PlantBitmapLoader.loadBitmap(plantInfo.imageUrl, context)
                            ?.let { calculateBottomPadding(it) } ?: 0f
                    }
                    val paddingOffset = targetHeight * bottomPadding
                    val drawX = isoX - (targetWidth / 2f)
                    val drawY = isoY + tileHeight / 6 - targetHeight + paddingOffset
                    
                    drawImage(
                        image = plantBitmap,
                        dstOffset = IntOffset(drawX.toInt(), drawY.toInt()),
                        dstSize = IntSize(targetWidth.toInt(), targetHeight.toInt()),
                        colorFilter = vitalityColorFilter
                    )
                }
                
                // ═══════════════════════════════════════════════════════════════════════════════
                // 🟢 [HOLOGRAPHIC SCAN] 全息扫描线绘制
                // 
                // 在前景绘制一条水平发光扫描线，从上到下扫过整个视口
                // ═══════════════════════════════════════════════════════════════════════════════
                if (scanProgress < 1f) {
                    // 扫描线发光效果
                    val scanLineBrush = Brush.verticalGradient(
                        colors = listOf(
                            appColors.primary.copy(alpha = 0f),
                            appColors.primary.copy(alpha = 0.8f),
                            appColors.primary.copy(alpha = 0f)
                        ),
                        startY = scanY - 20f,
                        endY = scanY + 20f
                    )
                    
                    // 绘制扫描线（在视口范围内）
                    val scanLineWidth = (halfW * 2) / scale
                    drawRect(
                        brush = scanLineBrush,
                        topLeft = Offset(-scanLineWidth / 2, scanY - 20f),
                        size = androidx.compose.ui.geometry.Size(scanLineWidth, 40f)
                    )
                    
                    // 扫描线中心高亮点
                    drawCircle(
                        color = appColors.primary.copy(alpha = 0.9f),
                        radius = 4f,
                        center = Offset(0f, scanY)
                    )
                }
            }
        }

        // ══ HUD 布局：玻璃拟态 Dock ══
        Box(modifier = Modifier.fillMaxSize()) {

            // ═══════════════════════════════════════════════════════════════
            // 离线警告横幅 - 不再完全阻止，显示警告并可重试
            // ═══════════════════════════════════════════════════════════════
            AnimatedVisibility(
                visible = !isConnected && !isChecking,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp)
            ) {
                Surface(
                    color = Color(0xFFFF9500).copy(alpha = 0.95f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(horizontal = 16.dp).shadow(8.dp, RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "离线模式 - 部分功能受限",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(12.dp))
                        TextButton(
                            onClick = {
                                isRetrying = true
                                viewModel.retryConnection()
                            },
                            enabled = !isRetrying,
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                        ) {
                            if (isRetrying) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("重试", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // ═══════════════════════════════════════════════════════════════
            // 惰性枯萎状态机：状态指示器 HUD
            // ═══════════════════════════════════════════════════════════════
            if (!guestMode) {
                VitalityStateIndicator(
                    vitalityState = vitalityState,
                    hoursSinceLastFocus = gardenVitality.hoursSinceLastFocus,
                    hasFocusRecord = gardenVitality.hasFocusRecord,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = if (isConnected) 60.dp else 80.dp, end = 16.dp)
                )
            }
            
            // ═══════════════════════════════════════════════════════════════
            // 排行榜入口按钮
            // ═══════════════════════════════════════════════════════════════
            var showLeaderboard by remember { mutableStateOf(false) }
            val leaderboard by viewModel.leaderboard.collectAsState()
            val isLoadingLeaderboard by viewModel.isLoadingLeaderboard.collectAsState()
            
            if (!guestMode) {
                // 排行榜按钮
                Surface(
                    onClick = {
                        showLeaderboard = true
                        viewModel.loadLeaderboard()
                    },
                    color = appColors.cardBg,  // 使用主题卡片背景色
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, appColors.primary.copy(alpha = 0.3f)),  // 使用主题主色调边框
                    shadowElevation = if (appColors.isDark) 0.dp else 2.dp,  // 浅色主题添加阴影
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = if (isConnected) 60.dp else 80.dp, start = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = "🏆", fontSize = 16.sp)
                        Text(
                            text = "排行榜",
                            color = appColors.primary,  // 使用主题主色调
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                // 排行榜弹窗
                if (showLeaderboard) {
                    LeaderboardDialog(
                        entries = leaderboard,
                        isLoading = isLoadingLeaderboard,
                        currentUserId = currentUserId,
                        onDismiss = { showLeaderboard = false }
                    )
                }
            }
            
            // 顶部种植提示 (指示器)
            AnimatedVisibility(
                visible = placementMode != null,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 80.dp)
            ) {
                Surface(
                    color = appColors.primary.copy(alpha = 0.95f),  // 使用主题主色调
                    shape = RoundedCornerShape(24.dp),
                    shadowElevation = 8.dp,  // 添加阴影
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Warning, 
                            contentDescription = null, 
                            tint = if (appColors.isDark) Color.Black else Color.White,  // 根据主题调整图标颜色
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "📡 坐标校验中：请点击目标地块种植 ${placementMode?.plantName}",
                            color = if (appColors.isDark) Color.Black else Color.White,  // 根据主题调整文字颜色
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            "取消", 
                            color = Color.Red, 
                            fontSize = 12.sp, 
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.clickable { viewModel.cancelPlacement() }
                        )
                    }
                }
            }

            // 底部 GlassDock (返回, 背包, 社交) —— 仅在主视角显示
            AnimatedVisibility(
                visible = !guestMode,
                enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp)
            ) {
                Surface(
                    color = appColors.cardBg.copy(alpha = 0.95f),  // 使用主题卡片背景色
                    shape = RoundedCornerShape(40.dp),
                    border = BorderStroke(1.5.dp, appColors.primary.copy(alpha = 0.3f)),  // 使用主题主色调边框
                    shadowElevation = if (appColors.isDark) 0.dp else 4.dp,  // 浅色主题添加阴影
                    modifier = Modifier
                        .height(80.dp)
                        .padding(horizontal = 8.dp)
                        .graphicsLayer {
                            // 这里模拟毛玻璃特效（如果系统级别不支持 RenderEffect，则表现为高质量半透明）
                            clip = true
                            shape = RoundedCornerShape(40.dp)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(32.dp)
                    ) {
                        // 返回图标 (量子跃迁)
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.Rounded.ArrowBack, 
                                "Back", 
                                tint = appColors.primary,  // 使用主题主色调
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        
                        // 背包图标 (收纳仓)
                        IconButton(onClick = { showInventorySheet = true }) {
                            Icon(
                                Icons.Rounded.Inventory, 
                                "Bag", 
                                tint = appColors.primary,  // 使用主题主色调
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        // 量子雷达图标 (📡 跃迁) - 严格主态可见
                        IconButton(onClick = onNavigateToSocial) {
                            Icon(
                                Icons.Rounded.People, 
                                "Social", 
                                tint = appColors.primary,  // 使用主题主色调
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }

        // 植物信息气泡
        selectedPlant?.let { id ->
            val tilePlant = gardenTiles.find { it.tileId.toString() == id }
            val plantId = tilePlant?.plantId
            val plantInfo = plantId?.let { plantDict[it] }

            if (plantInfo != null) {
                // 简洁的植物信息气泡
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(bottom = 100.dp)
                        .clickable { selectedPlant = null }
                ) {
                    Surface(
                        color = WaterPavilionDeep.copy(alpha = 0.95f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MonumentMint.copy(alpha = 0.5f)),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = plantInfo.plantName,
                                color = MonumentMint,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = plantInfo.description,
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(24.dp)
                            ) {
                                // 占地面积
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${plantInfo.width}×${plantInfo.width}",
                                        color = MonumentMint,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "占地面积",
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 11.sp
                                    )
                                }
                                // 照亮范围
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${plantInfo.purifyRange}格",
                                        color = appColors.primary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "照亮范围",
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "点击任意位置关闭",
                                color = Color.White.copy(alpha = 0.3f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // 访客提示 Header
        if (guestMode) {
            Surface(
                color = WaterPavilionDeep.copy(alpha = 0.8f), 
                shape = RoundedCornerShape(24.dp), 
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), 
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Rounded.ArrowBack, "Exit", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "🌐 访客模式: 正在观测好友花园",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 种植确认
        deployConfirmPosition?.let { pos ->
            val x = pos.first
            val y = pos.second
            AlertDialog(
                onDismissRequest = { deployConfirmPosition = null },
                containerColor = WaterPavilionDeep.copy(alpha = 0.98f),
                title = { Text("📡 植物种植确认", color = MonumentMint, fontWeight = FontWeight.Bold) },
                text = { Text("确定要在坐标 ($x, $y) 种植 [${placementMode?.plantName}] 吗？\n种植后该区域将永久受控。", color = Color.White.copy(alpha = 0.8f)) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.confirmPlacement(x, y)
                        deployConfirmPosition = null
                    }) {
                        Text("立刻种植", color = MonumentMint, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deployConfirmPosition = null }) {
                        Text("再想想", color = Color.White.copy(alpha = 0.4f))
                    }
                },
                shape = RoundedCornerShape(24.dp)
            )
        }

        // 移除原有的右下角收纳仓按钮，已迁移至中央 Dock

        // 移除原有的左下角社交按钮，已迁移至左上角

        // ══ 正下方：操作区 (仅客态可见) —— 主按钮 + 注入信标次级按钮 ══
        AnimatedVisibility(
            visible = guestMode,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // 主按钮：释放全域量子脉冲
                                    PulseChargeButton(
                                        isLoading = isPulseLoading,
                                        isCharged = hasChargedToday,
                                        onClick = { socialViewModel.unleashPulse() }
                                    )
                // 次级按钮：✒️ 注入信标 (留言)
                OutlinedButton(
                    onClick = {
                        beaconText = ""
                        showBeaconDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.82f)
                        .height(48.dp),
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(1.5.dp, appColors.primary.copy(alpha = 0.4f)),  // 使用主题主色调边框
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = appColors.cardBg.copy(alpha = 0.95f)  // 使用主题卡片背景色
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("✒️", fontSize = 16.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "注入信标",
                            color = appColors.primary,  // 使用主题主色调
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }

        // 脉冲提示消息
        if (pulseMessage != null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Surface(
                    color = appColors.cardBg,  // 使用主题卡片背景色
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 4.dp,  // 添加阴影
                    border = BorderStroke(1.5.dp, appColors.primary.copy(alpha = 0.3f)),  // 添加边框
                    modifier = Modifier.padding(horizontal = 32.dp)
                ) {
                    Text(
                        text = pulseMessage!!,
                        color = appColors.textMain,  // 使用主题主文字色
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium  // 增加字重
                    )
                }
            }
        }
    }

    // --- 全息收纳仓 (Inventory Sheet) ---
    if (showInventorySheet) {
        InventorySheet(
            plantDict = plantDict,
            onDismiss = { showInventorySheet = false },
            onPlantSelected = { bagRecordId, plantId, plantName ->
                viewModel.enterPlacementMode(bagRecordId.toString(), plantId, plantName)
                showInventorySheet = false
            }
        )
    }

    // --- ✒️ 注入信标 Dialog (客态留言) ---
    if (showBeaconDialog) {
        val visitingFriendState by socialViewModel.visitingFriend.collectAsState()
        AlertDialog(
            onDismissRequest = { showBeaconDialog = false },
            containerColor = appColors.cardBg,  // 使用主题卡片背景色
            shape = RoundedCornerShape(24.dp),
            title = {
                Column {
                    Text(
                        "✒️ 注入信标",
                        color = appColors.primary,  // 使用主题主色调
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        "INJECT BEACON",
                        color = appColors.textSub,  // 使用主题副文字色
                        fontSize = 9.sp,
                        letterSpacing = 2.sp
                    )
                }
            },
            text = {
                Column {
                    Text(
                        "在 ${visitingFriendState?.nickname ?: "好友"} 的花园留言：",
                        color = appColors.textSub,  // 使用主题副文字色
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    // 自定义输入框 (玻璃拟态)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = appColors.primary.copy(alpha = 0.08f),  // 使用主题主色调的淡色背景
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                1.dp,
                                appColors.primary.copy(alpha = 0.3f),  // 使用主题主色调的边框
                                RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        if (beaconText.isEmpty()) {
                            Text(
                                "在这里留下你的数字烙印...",
                                color = appColors.textSub.copy(alpha = 0.5f),  // 使用主题副文字色
                                fontSize = 13.sp
                            )
                        }
                        BasicTextField(
                            value = beaconText,
                            onValueChange = { if (it.length <= 100) beaconText = it },
                            textStyle = TextStyle(
                                color = appColors.textMain,  // 使用主题主文字色
                                fontSize = 14.sp
                            ),
                            cursorBrush = SolidColor(appColors.primary),  // 使用主题主色调
                            maxLines = 4,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Text(
                        "${beaconText.length}/100",
                        color = appColors.textSub,  // 使用主题副文字色
                        fontSize = 10.sp,
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 4.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (beaconText.isNotBlank()) {
                            socialViewModel.sendBeacon(
                                hostId = visitingFriendState?.userId ?: 0L,
                                content = beaconText.trim(),
                                onSuccess = {
                                    Toast.makeText(context, "留言完成", Toast.LENGTH_SHORT).show()
                                },
                                onError = { msg ->
                                    Toast.makeText(context, "❌ $msg", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                        showBeaconDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = appColors.primary.copy(alpha = 0.12f)  // 使用主题主色调
                    ),
                    border = BorderStroke(1.dp, appColors.primary.copy(alpha = 0.4f)),  // 使用主题主色调
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("留言 ✒️", color = appColors.primary, fontWeight = FontWeight.Bold)  // 使用主题主色调
                }
            },
            dismissButton = {
                TextButton(onClick = { showBeaconDialog = false }) {
                    Text("取消", color = appColors.textSub)  // 使用主题副文字色
                }
            }
        )
    }
}

/**
 * 地块类型枚举
 */
private enum class TileType {
    PLANTED,       // 植物占领区 - 植物实际占用的地块
    ILLUMINATED,   // 植物照亮区 - 被净化但没有植物的地块
    EMPTY          // 未净化区域 - 可开拓的边缘
}

/**
 * 视觉地块渲染元模型 (Internal Visual Model)
 */
private data class VisualTile(
    val x: Int,
    val y: Int,
    val tileType: TileType,
    val plantId: Int = 0,
    val tileId: Long? = null,
    val instanceId: String? = null,
    // 🔧 [MULTI-TILE] 是否是植物的主坐标（用于多格植物去重渲染）
    val isMainTile: Boolean = true
)
