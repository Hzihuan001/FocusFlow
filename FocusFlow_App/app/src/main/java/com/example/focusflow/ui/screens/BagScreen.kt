package com.example.focusflow.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.focusflow.api.BagService
import com.example.focusflow.ui.MainViewModel
import com.example.focusflow.ui.components.PlantImage
import com.example.focusflow.ui.components.SkeletonBox
import com.example.focusflow.ui.components.SkeletonPlantCard
import com.example.focusflow.ui.components.PullRefreshLayout
import com.example.focusflow.ui.theme.*
import com.example.focusflow.ui.viewmodel.BagViewModel
import com.example.focusflow.ui.viewmodel.GardenViewModel
import com.example.focusflow.ui.viewmodel.OpenBoxResult

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * BagScreen —— 我的收纳仓
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * 【功能特性】
 * 1. 同类植物合并显示数量
 * 2. 解析确认弹窗
 * 3. 植物图片展示
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun BagScreen(
    mainViewModel: MainViewModel = viewModel(),
    bagViewModel: BagViewModel = viewModel(),
    gardenViewModel: GardenViewModel = viewModel(),
    onBack: () -> Unit
) {
    val appColors = LocalAppColors.current
    val bagItems by bagViewModel.bagItems.collectAsState()
    val isLoading by bagViewModel.isLoading.collectAsState()
    val timeFlux by mainViewModel.timeFlux.collectAsState()
    
    // 每次进入页面时刷新背包数据
    LaunchedEffect(Unit) {
        bagViewModel.loadBagFromCloud()
    }
    
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    
    // 解析确认弹窗状态
    var showParseDialog by remember { mutableStateOf<BagService.BagItemDto?>(null) }
    
    // 定义Tab配置
    val tabs = listOf(
        TabConfig("未知种子", Icons.Rounded.Science, CyberPurple),
        TabConfig("待种植", Icons.Rounded.Eco, CyberPrimary),
        TabConfig("净化中", Icons.Rounded.Psychology, CyberSecondary)
    )
    
    // 动态背景动画
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val bgOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        appColors.bgDeep,
                        appColors.bgDeep.copy(alpha = 0.8f),
                        appColors.bgDeep.copy(alpha = 0.9f + bgOffset * 0.1f)
                    ),
                    start = androidx.compose.ui.geometry.Offset(0f, bgOffset * 1000),
                    end = androidx.compose.ui.geometry.Offset(1000f, 2000f - bgOffset * 1000)
                )
            )
    ) {
        // 背景装饰光晕
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = (-100).dp, y = (-50).dp)
                .blur(80.dp)
                .background(CyberPrimary.copy(alpha = 0.08f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(250.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 80.dp, y = 80.dp)
                .blur(60.dp)
                .background(CyberPurple.copy(alpha = 0.06f), CircleShape)
        )

        // 下拉刷新包装
        PullRefreshLayout(
            isRefreshing = isLoading,
            onRefresh = { bagViewModel.loadBagFromCloud() }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 顶部栏
                TopBar(
                    timeFlux = timeFlux,
                    appColors = appColors,
                    onBack = onBack
                )

                // Tab 栏
                CyberTabRow(
                    tabs = tabs,
                    selectedIndex = selectedTabIndex,
                    appColors = appColors,
                    onTabSelected = { selectedTabIndex = it }
                )

                // 内容区 - 按植物分组
                val groupedItems = remember(bagItems, selectedTabIndex) {
                    bagItems
                        .filter { it.status == selectedTabIndex }
                        .groupBy { it.plantId }
                        .map { (plantId, items) ->
                            GroupedItem(
                                plantId = plantId,
                                items = items,
                                count = items.size
                            )
                        }
                }

                AnimatedContent(
                targetState = selectedTabIndex,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith 
                    fadeOut(animationSpec = tween(150))
                },
                label = "content"
            ) { index ->
                if (isLoading && groupedItems.isEmpty()) {
                    // 骨架屏加载状态
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(6) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // 图片骨架
                                SkeletonBox(
                                    width = 160.dp,
                                    height = 100.dp,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(Modifier.height(10.dp))
                                // 名称骨架
                                SkeletonBox(width = 80.dp, height = 14.dp)
                                Spacer(Modifier.height(8.dp))
                                // 按钮骨架
                                SkeletonBox(
                                    width = 140.dp,
                                    height = 36.dp,
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }
                } else if (groupedItems.isEmpty()) {
                    EmptyState(tabIndex = index)
                } else {
                    GroupedItemsGrid(
                        groupedItems = groupedItems,
                        tabIndex = selectedTabIndex,
                        appColors = appColors,
                        onParse = { item -> showParseDialog = item },
                        onDeploy = { item ->
                            gardenViewModel.enterPlacementMode(
                                bagRecordId = item.bagId,
                                plantId = item.plantId,
                                plantName = item.plantName ?: "未知植物"
                            )
                            onBack()
                        }
                    )
                }
            }
            }
        }
    }

    // 解析确认弹窗
    showParseDialog?.let { item ->
        ParseConfirmDialog(
            item = item,
            currentFlux = timeFlux,
            appColors = appColors,
            onConfirm = {
                bagViewModel.openBox(item)
                showParseDialog = null
            },
            onDismiss = { showParseDialog = null }
        )
    }
    
    // 解析结果弹窗
    val openBoxResult by bagViewModel.openBoxResult.collectAsState()
    openBoxResult?.let { result ->
        OpenBoxResultDialog(
            result = result,
            appColors = appColors,
            onDismiss = { bagViewModel.clearRarityEffect() }
        )
    }
}

/**
 * 分组后的物品
 */
private data class GroupedItem(
    val plantId: Int,
    val items: List<BagService.BagItemDto>,
    val count: Int
) {
    // 取第一个item作为代表
    val firstItem: BagService.BagItemDto get() = items.first()
    val bagId: String get() = firstItem.bagId
    val plantName: String? get() = firstItem.plantName
    val rarity: Int? get() = firstItem.rarity
    val imageUrl: String? get() = firstItem.imageUrl
    val status: Int get() = firstItem.status
}

/**
 * 解析确认弹窗
 */
@Composable
private fun ParseConfirmDialog(
    item: BagService.BagItemDto,
    currentFlux: Int,
    appColors: AppColors,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val cost = 100
    val canAfford = currentFlux >= cost
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = appColors.cardBg,
            border = BorderStroke(1.dp, CyberPurple.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 图标
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    CyberPurple.copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = CyberPurple,
                        modifier = Modifier.size(36.dp)
                    )
                }
                
                Spacer(Modifier.height(16.dp))
                
                Text(
                    text = "培育种子",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(Modifier.height(8.dp))
                
                Text(
                    text = "当前种子处于未知状态\n消耗 $cost ✨ 光流可培育出随机植物",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
                
                Spacer(Modifier.height(16.dp))
                
                // 余额显示
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (canAfford) appColors.cardBg.copy(alpha = 0.6f) else appColors.cardBg.copy(alpha = 0.4f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "当前余额:",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 13.sp
                        )
                        Text(
                            text = "✨ $currentFlux",
                            color = if (canAfford) CyberPrimary else Color(0xFFFF6B6B),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(Modifier.height(20.dp))
                
                // 按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 取消按钮
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White.copy(alpha = 0.7f)
                        )
                    ) {
                        Text("取消")
                    }
                    
                    // 确认按钮
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = canAfford,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberPurple,
                            disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                        )
                    ) {
                        Text(
                            text = if (canAfford) "开始灌溉" else "光流不足",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * 顶部栏
 */
@Composable
private fun TopBar(
    timeFlux: Int,
    appColors: AppColors,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(44.dp)
                .background(Color.White.copy(alpha = 0.05f), CircleShape)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                tint = CyberPrimary
            )
        }

        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
            Text(
                text = "收纳仓",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Text(
                text = "QUANTUM STORAGE",
                color = CyberPrimary.copy(alpha = 0.6f),
                fontSize = 10.sp,
                letterSpacing = 3.sp
            )
        }

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = appColors.cardBg,
            border = BorderStroke(1.dp, CyberPrimary.copy(alpha = 0.3f)),
            modifier = Modifier.padding(end = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(text = "✨", fontSize = 16.sp)
                Text(
                    text = timeFlux.toString(),
                    color = CyberPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Tab配置
 */
private data class TabConfig(
    val title: String,
    val icon: ImageVector,
    val color: Color
)

/**
 * Tab栏
 */
@Composable
private fun CyberTabRow(
    tabs: List<TabConfig>,
    selectedIndex: Int,
    appColors: AppColors,
    onTabSelected: (Int) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = appColors.cardBg,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier
                .padding(4.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = index == selectedIndex
                
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0.98f,
                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                    label = "scale"
                )

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .scale(scale)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onTabSelected(index) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) tab.color.copy(alpha = 0.15f) else Color.Transparent,
                    border = if (isSelected) BorderStroke(1.dp, tab.color.copy(alpha = 0.5f)) else null
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = null,
                            tint = if (isSelected) tab.color else Color.Gray,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = tab.title,
                            color = if (isSelected) tab.color else Color.Gray,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
    
    Spacer(Modifier.height(12.dp))
}

/**
 * 分组物品网格
 */
@Composable
private fun GroupedItemsGrid(
    groupedItems: List<GroupedItem>,
    tabIndex: Int,
    appColors: AppColors,
    onParse: (BagService.BagItemDto) -> Unit,
    onDeploy: (BagService.BagItemDto) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(groupedItems, key = { it.plantId }) { group ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(300)) + scaleIn(tween(300)),
                exit = fadeOut(tween(150))
            ) {
                GroupedItemCard(
                    group = group,
                    tabIndex = tabIndex,
                    appColors = appColors,
                    onParse = { onParse(group.firstItem) },
                    onDeploy = { onDeploy(group.firstItem) }
                )
            }
        }
    }
}

/**
 * 分组物品卡片
 */
@Composable
private fun GroupedItemCard(
    group: GroupedItem,
    tabIndex: Int,
    appColors: AppColors,
    onParse: () -> Unit,
    onDeploy: () -> Unit
) {
    val item = group.firstItem
    val status = item.status
    val isLocked = status == 0
    
    // 稀有度颜色
    val rarityColor = when(item.rarity ?: 0) {
        1 -> CyberSecondary
        2 -> Color(0xFFFFD700)
        3 -> Color(0xFFFF69B4)
        else -> Color.Gray
    }
    
    val themeColor = if (isLocked) CyberPurple else CyberPrimary

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "press"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { if (isLocked) onParse() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = appColors.cardBg),
        border = BorderStroke(1.dp, themeColor.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 图片区域
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    themeColor.copy(alpha = 0.15f),
                                    appColors.bgDeep
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLocked) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.zIndex(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = CyberPurple,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "???",
                                color = CyberPurple.copy(alpha = 0.7f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 4.sp
                            )
                        }
                        
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp),
                            shape = RoundedCornerShape(6.dp),
                            color = CyberPurple.copy(alpha = 0.3f)
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "加密",
                                tint = Color.White,
                                modifier = Modifier.padding(4.dp).size(14.dp)
                            )
                        }
                    } else {
                        // 显示植物图片
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            // 如果有图片URL则显示，否则显示默认图标
                            if (!item.imageUrl.isNullOrBlank()) {
                                PlantImage(
                                    imageUrl = item.imageUrl,
                                    contentDescription = item.plantName,
                                    modifier = Modifier.fillMaxSize().padding(8.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Eco,
                                    contentDescription = item.plantName,
                                    tint = CyberPrimary,
                                    modifier = Modifier.size(56.dp)
                                )
                            }
                        }
                        
                        // 稀有度徽章
                        val rarity = item.rarity ?: 0
                        if (rarity > 0) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp),
                                shape = RoundedCornerShape(6.dp),
                                color = rarityColor.copy(alpha = 0.3f)
                            ) {
                                Text(
                                    text = when(rarity) {
                                        1 -> "R"
                                        2 -> "SR"
                                        3 -> "SSR"
                                        else -> ""
                                    },
                                    color = rarityColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // 名称
                Text(
                    text = if (isLocked) "未知种子" else (item.plantName ?: "未知植物"),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                // 操作按钮
                when (status) {
                    0 -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(text = "解析", color = CyberPurple, fontSize = 12.sp)
                            Icon(
                                Icons.Default.KeyboardArrowRight,
                                contentDescription = null,
                                tint = CyberPurple,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    1 -> {
                        Button(
                            onClick = onDeploy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .height(36.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberPrimary.copy(alpha = 0.2f),
                                contentColor = CyberPrimary
                            ),
                            border = BorderStroke(1.dp, CyberPrimary.copy(alpha = 0.5f)),
                            elevation = ButtonDefaults.buttonElevation(0.dp)
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("种植", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    2 -> {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = CyberSecondary.copy(alpha = 0.1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                val pulseScale by rememberInfiniteTransition(label = "pulse").animateFloat(
                                    initialValue = 0.8f,
                                    targetValue = 1.2f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(800, easing = EaseInOut),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "pulse"
                                )
                                
                                Box(
                                    modifier = Modifier
                                        .size(8.dp * pulseScale)
                                        .background(CyberSecondary, CircleShape)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(text = "净化中...", color = CyberSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            
            // 数量徽章（大于1时显示）
            if (group.count > 1) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = themeColor
                ) {
                    Text(
                        text = "×${group.count}",
                        color = Color.Black,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * 空状态
 */
@Composable
private fun EmptyState(tabIndex: Int) {
    val (icon, text, color) = when(tabIndex) {
        0 -> Triple(Icons.Rounded.Science, "暂无加密种子\n完成专注可获得种子", CyberPurple)
        1 -> Triple(Icons.Rounded.Eco, "暂无待种植植物\n解析种子后可种植", CyberPrimary)
        else -> Triple(Icons.Rounded.Psychology, "暂无净化中的植物", CyberSecondary)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                color.copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color.copy(alpha = 0.4f),
                    modifier = Modifier.size(48.dp)
                )
            }

            Text(
                text = text,
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        }
    }
}

/**
 * 解析结果弹窗
 */
@Composable
private fun OpenBoxResultDialog(
    result: OpenBoxResult,
    appColors: AppColors,
    onDismiss: () -> Unit
) {
    // 稀有度配置
    val (rarityName, rarityColor, rarityBg) = when (result.rarity) {
        0 -> Triple("N · 普通", Color(0xFF9E9E9E), Color(0xFF424242))
        1 -> Triple("R · 稀有", Color(0xFF2196F3), Color(0xFF0D47A1))
        2 -> Triple("SR · 史诗", Color(0xFF9C27B0), Color(0xFF4A148C))
        3 -> Triple("SSR · 传说", Color(0xFFFFD700), Color(0xFFFF6F00))
        else -> Triple("未知", Color.Gray, Color.Gray)
    }
    
    // 脉冲动画
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = appColors.cardBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 稀有度徽章
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = rarityBg
                ) {
                    Text(
                        text = rarityName,
                        color = rarityColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // 植物图标（带光晕效果）
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(glowScale)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    rarityColor.copy(alpha = 0.3f),
                                    rarityColor.copy(alpha = 0.1f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Eco,
                        contentDescription = null,
                        tint = rarityColor,
                        modifier = Modifier.size(64.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // 植物名称
                Text(
                    text = "解析成功！",
                    color = CyberPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = result.plantName,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 光流余额
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "剩余光流：",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )
                    Text(
                        text = "${result.newTimeFlux} ✨",
                        color = CyberPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 确认按钮
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = rarityColor
                    )
                ) {
                    Text(
                        text = "太棒了！",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
