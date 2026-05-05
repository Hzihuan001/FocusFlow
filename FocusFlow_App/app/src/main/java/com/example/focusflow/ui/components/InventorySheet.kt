package com.example.focusflow.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.focusflow.api.BagService
import com.example.focusflow.ui.MainViewModel
import com.example.focusflow.ui.components.PlantImage
import com.example.focusflow.ui.theme.*
import com.example.focusflow.ui.viewmodel.BagViewModel
import kotlin.random.Random

/**
 * InventorySheet —— 全息收纳仓（底部弹窗版）
 * 使用BagScreen的卡片样式
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventorySheet(
    mainViewModel: MainViewModel = viewModel(),
    bagViewModel: BagViewModel = viewModel(),
    plantDict: Map<Int, com.example.focusflow.data.entity.PlantDictEntity> = emptyMap(),
    onDismiss: () -> Unit,
    onPlantSelected: (bagRecordId: String, plantId: Int, plantName: String) -> Unit
) {
    val bagItems by bagViewModel.bagItems.collectAsState()
    val timeFlux by mainViewModel.timeFlux.collectAsState()
    val decodingItemId by bagViewModel.decodingItemId.collectAsState()
    val lastRarity by bagViewModel.lastRarity.collectAsState()
    val openBoxResult by bagViewModel.openBoxResult.collectAsState()
    val errorMsg by bagViewModel.errorMsg.collectAsState()
    
    // 每次打开弹窗时刷新背包数据
    LaunchedEffect(Unit) {
        bagViewModel.loadBagFromCloud()
    }
    
    // 监听开箱结果，同步光流余额
    LaunchedEffect(openBoxResult) {
        openBoxResult?.let { result ->
            mainViewModel.syncTimeFlux(result.newTimeFlux)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0A0F14).copy(alpha = 0.98f),
        scrimColor = Color.Black.copy(alpha = 0.7f),
        tonalElevation = 8.dp,
        dragHandle = { BottomSheetDefaults.DragHandle(color = CyberPrimary.copy(alpha = 0.3f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "收纳仓",
                        color = CyberPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Text("INVENTORY TERMINAL V4.0", color = CyberTextSub, fontSize = 10.sp)
                }

                // 光流余额
                Surface(
                    color = Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, CyberSecondary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("✨", fontSize = 14.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "$timeFlux",
                            color = CyberSecondary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // 分类显示
            val unknownItems = bagItems.filter { it.status == 0 }
            val deployableItems = bagItems.filter { it.status == 1 }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxHeight(0.7f)
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 加密种子
                items(unknownItems.groupBy { it.plantId }.values.toList()) { group ->
                    val firstInstance = group.first()
                    val count = group.size
                    
                    InventoryItemCard(
                        item = firstInstance,
                        count = count,
                        isDecoding = decodingItemId == firstInstance.bagId,
                        canAfford = timeFlux >= 100,
                        onDecode = { bagViewModel.openBox(firstInstance) },
                        onDeploy = {}
                    )
                }
                
                // 待种植
                items(deployableItems.groupBy { it.plantId }.values.toList()) { group ->
                    val firstInstance = group.first()
                    val count = group.size
                    
                    InventoryItemCard(
                        item = firstInstance,
                        count = count,
                        isDecoding = false,
                        canAfford = true,
                        onDecode = {},
                        onDeploy = {
                            onPlantSelected(
                                firstInstance.bagId,
                                firstInstance.plantId,
                                firstInstance.plantName ?: "未知植物"
                            )
                        }
                    )
                }
            }
        }
    }

    // 解析结果弹窗 - 显示开出的植物信息
    if (openBoxResult != null) {
        OpenBoxResultDialog(
            result = openBoxResult!!,
            onDismiss = { bagViewModel.clearRarityEffect() }
        )
    }

    // 错误提示
    if (errorMsg != null) {
        AlertDialog(
            onDismissRequest = { bagViewModel.clearError() },
            containerColor = Color(0xFF1A1F26),
            title = { Text("系统反馈", color = CyberPrimary, fontWeight = FontWeight.Bold) },
            text = { Text(errorMsg!!, color = Color.White.copy(alpha = 0.8f)) },
            confirmButton = {
                Button(
                    onClick = { bagViewModel.clearError() },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary)
                ) {
                    Text("收到", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

/**
 * 统一卡片组件（使用BagScreen样式）
 */
@Composable
private fun InventoryItemCard(
    item: BagService.BagItemDto,
    count: Int = 1,
    isDecoding: Boolean,
    canAfford: Boolean,
    onDecode: () -> Unit,
    onDeploy: () -> Unit
) {
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

    // 解码动画
    val transition = rememberInfiniteTransition()
    val shakeOffset by transition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(50, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    var showConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .graphicsLayer {
                if (isDecoding) {
                    translationX = shakeOffset
                    translationY = Random.nextFloat() * 2f
                }
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { if (isLocked && !isDecoding) showConfirm = true },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1923)),
        border = BorderStroke(1.dp, if (isDecoding) CyberPrimary else themeColor.copy(alpha = 0.3f)),
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
                        .height(90.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    themeColor.copy(alpha = 0.15f),
                                    Color(0xFF0A0A12)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLocked) {
                        if (isDecoding) {
                            // 解码动画
                            val glitchChars = "01#%&*@$"
                            var glitchText by remember { mutableStateOf("") }
                            LaunchedEffect(Unit) {
                                while(true) {
                                    glitchText = (1..6).map { glitchChars[Random.nextInt(glitchChars.length)] }.joinToString("")
                                    kotlinx.coroutines.delay(80)
                                }
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("⚡", fontSize = 36.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = glitchText,
                                    color = CyberPrimary,
                                    fontSize = 12.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                                LinearProgressIndicator(
                                    modifier = Modifier.padding(top = 8.dp).width(60.dp).height(2.dp),
                                    color = CyberPrimary,
                                    trackColor = Color.Transparent
                                )
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = CyberPurple,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "???",
                                    color = CyberPurple.copy(alpha = 0.7f),
                                    fontSize = 12.sp,
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
                        }
                    } else {
                        // 显示植物图片
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
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
                                    modifier = Modifier.size(48.dp)
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

                Spacer(Modifier.height(8.dp))

                // 名称
                Text(
                    text = if (isLocked) "未知种子" else (item.plantName ?: "未知植物"),
                    color = Color.White,
                    fontSize = 13.sp,
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
                                .padding(top = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(text = "解析", color = CyberPurple, fontSize = 11.sp)
                            Icon(
                                Icons.Default.KeyboardArrowRight,
                                contentDescription = null,
                                tint = CyberPurple,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    1 -> {
                        Button(
                            onClick = onDeploy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                                .height(32.dp),
                            shape = RoundedCornerShape(8.dp),
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
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("种植", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
            
            // 数量徽章
            if (count > 1) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = themeColor
                ) {
                    Text(
                        text = "×$count",
                        color = Color.Black,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }

    // 解析确认弹窗
    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            containerColor = Color(0xFF1A1F26),
            title = { Text("培育种子", color = CyberPurple, fontWeight = FontWeight.Bold) },
            text = { 
                Text("灌溉该种子将消耗 100 ✨ 光流。是否执行？", color = Color.White.copy(alpha = 0.8f))
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirm = false
                        if (canAfford) onDecode()
                    },
                    enabled = canAfford,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberPurple)
                ) {
                    Text("灌溉", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) {
                    Text("取消", color = CyberTextSub)
                }
            }
        )
    }
}

/**
 * 稀有度耀斑弹窗
 */
@Composable
fun RarityFlareDialog(
    rarity: Int,
    onDismiss: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val rarityColor = when (rarity) {
        3 -> Color(0xFFEAB308) // SSR (Gold)
        2 -> Color(0xFFA855F7) // SR
        1 -> Color(0xFF3B82F6) // R
        else -> Color(0xFF22C55E) // N
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0D1117),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = rarityColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("接入实装列表", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp)
                    .scale(glowScale),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when (rarity) {
                        3 -> "🌟 绝密资产解码成功！"
                        else -> "DECODING COMPLETE"
                    },
                    color = rarityColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(32.dp))

                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .shadow(
                            24.dp,
                            shape = RoundedCornerShape(20.dp),
                            spotColor = rarityColor,
                            ambientColor = rarityColor
                        )
                        .background(rarityColor.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                        .border(2.dp, rarityColor, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🌿", fontSize = 64.sp)
                }

                Spacer(Modifier.height(32.dp))

                Text(
                    text = when (rarity) {
                        3 -> "级别: [ LEGENDARY SSR ]"
                        2 -> "级别: [ EPIC SR ]"
                        1 -> "级别: [ RARE R ]"
                        else -> "级别: [ NORMAL N ]"
                    },
                    color = rarityColor.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    letterSpacing = 2.sp
                )
            }
        }
    )
}

/**
 * 开箱结果弹窗 - 显示完整的植物信息
 */
@Composable
private fun OpenBoxResultDialog(
    result: com.example.focusflow.ui.viewmodel.OpenBoxResult,
    onDismiss: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val rarityColor = when (result.rarity) {
        3 -> Color(0xFFEAB308) // SSR 金色
        2 -> Color(0xFFA855F7) // SR 紫色
        1 -> Color(0xFF3B82F6) // R 蓝色
        else -> Color(0xFF22C55E) // N 绿色
    }

    val rarityText = when (result.rarity) {
        3 -> "SSR 传说"
        2 -> "SR 史诗"
        1 -> "R 稀有"
        else -> "N 普通"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0D1117),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(horizontal = 24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = rarityColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("太棒了！", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
                    .scale(glowScale),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 稀有度徽章
                Surface(
                    color = rarityColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, rarityColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = rarityText,
                        color = rarityColor,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                Spacer(Modifier.height(20.dp))

                // 植物图片
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .shadow(
                            20.dp,
                            shape = RoundedCornerShape(20.dp),
                            spotColor = rarityColor,
                            ambientColor = rarityColor
                        )
                        .background(rarityColor.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                        .border(2.dp, rarityColor, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!result.imageUrl.isNullOrBlank()) {
                        PlantImage(
                            imageUrl = result.imageUrl,
                            contentDescription = result.plantName,
                            modifier = Modifier.fillMaxSize(),
                            size = 100.dp
                        )
                    } else {
                        Text(text = "🌿", fontSize = 48.sp)
                    }
                }

                Spacer(Modifier.height(20.dp))

                // 植物名称
                Text(
                    text = result.plantName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(16.dp))

                // 剩余光流
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("✨", fontSize = 16.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "剩余光流: ${result.newTimeFlux}",
                        color = CyberSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        }
    )
}