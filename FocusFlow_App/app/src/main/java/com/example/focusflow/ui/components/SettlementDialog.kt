package com.example.focusflow.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.focusflow.data.repository.FocusSettlementResult
import com.example.focusflow.ui.theme.*

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * 全息结算面板 (Settlement Dialog) - 优化版
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * 视觉特征：
 * 1. 分层卡片布局 - 信息层次清晰
 * 2. 数字滚动动画 - 奖励金额动态增长
 * 3. 粒子光效背景 - 沉浸式赛博氛围
 * 4. 状态徽章系统 - 直观的成果反馈
 */
@Composable
fun SettlementDialog(
    result: FocusSettlementResult,
    onConfirm: () -> Unit,
    onNavigateToBag: () -> Unit
) {
    // ══ 动画状态 ══
    val infiniteTransition = rememberInfiniteTransition()
    
    // 外发光呼吸动画
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    // 卡片微浮动动画
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    // 数字滚动动画
    var displayReward by remember { mutableStateOf(0) }
    LaunchedEffect(result.rewardAmount) {
        val steps = 20
        val stepDuration = 50L
        repeat(steps) { i ->
            displayReward = (result.rewardAmount * (i + 1) / steps)
            kotlinx.coroutines.delay(stepDuration)
        }
        displayReward = result.rewardAmount
    }

    Dialog(
        onDismissRequest = onConfirm,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.92f),
                            Color(0xFF0A0A1A).copy(alpha = 0.95f),
                            Color.Black.copy(alpha = 0.92f)
                        )
                    )
                )
                .padding(horizontal = 24.dp, vertical = 48.dp),
            contentAlignment = Alignment.Center
        ) {
            // ══ 背景光效层 ══
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = floatOffset.dp)
            ) {
                // 顶部光晕
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = 80.dp)
                        .size(200.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    CyberPrimary.copy(alpha = glowAlpha * 0.3f),
                                    Color.Transparent
                                )
                            ),
                            CircleShape
                        )
                )
            }
            
            // ══ 核心内容面板 ══
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ─── 顶部状态标签 ───
                Surface(
                    color = CyberPrimary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(50),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberPrimary.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Timer,
                            contentDescription = null,
                            tint = CyberPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${result.focusMinutes} 分钟专注完成",
                            color = CyberPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // ─── 核心奖励卡片 ───
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .border(
                            2.dp,
                            Brush.linearGradient(
                                colors = listOf(
                                    CyberPrimary.copy(alpha = glowAlpha),
                                    CyberPurple.copy(alpha = glowAlpha),
                                    CyberPrimary.copy(alpha = glowAlpha)
                                )
                            ),
                            RoundedCornerShape(20.dp)
                        )
                        .shadow(
                            elevation = 24.dp,
                            shape = RoundedCornerShape(20.dp),
                            spotColor = CyberPrimary.copy(alpha = 0.3f),
                            ambientColor = CyberPrimary.copy(alpha = 0.1f)
                        ),
                    color = CyberCardBg
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 标题
                        Text(
                            text = "FOCUS CYCLE COMPLETE",
                            color = CyberPrimary.copy(alpha = 0.8f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 4.sp
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // 主奖励数字
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "+$displayReward",
                                color = Color.White,
                                fontSize = 56.sp,
                                fontWeight = FontWeight.Black,
                                style = androidx.compose.ui.text.TextStyle(
                                    brush = Brush.linearGradient(
                                        colors = listOf(CyberPrimary, Color(0xFF66FFB7))
                                    )
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "✨",
                                fontSize = 28.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        
                        Text(
                            text = "光流已注入终端",
                            color = CyberTextSub,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // ─── 成果卡片区域 ───
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 种子掉落卡片
                    SeedDropCard(
                        modifier = Modifier.weight(1f),
                        focusMinutes = result.focusMinutes,
                        isDropped = result.isDropped,
                        droppedPlantName = result.droppedPlantName
                    )
                    
                    // 充能状态卡片
                    GardenChargeCard(
                        modifier = Modifier.weight(1f),
                        purifiedTiles = result.purifiedTiles
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // ─── 操作按钮区 ───
                // 主按钮
                val shimmerX by infiniteTransition.animateFloat(
                    initialValue = -1f,
                    targetValue = 2f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2500, easing = LinearEasing)
                    )
                )
                
                Button(
                    onClick = onConfirm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .border(
                            1.dp,
                            Brush.linearGradient(
                                0.0f to Color.Transparent,
                                shimmerX to Color.White.copy(alpha = 0.3f),
                                shimmerX + 0.3f to Color.Transparent
                            ),
                            RoundedCornerShape(12.dp)
                        ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberPrimary,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "确认并继续",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                
                // 次级按钮
                if (result.isDropped) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = onNavigateToBag,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = CyberSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "前往收纳仓解析种子",
                            color = CyberSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * 种子掉落卡片
 */
@Composable
fun SeedDropCard(
    modifier: Modifier = Modifier,
    focusMinutes: Int,
    isDropped: Boolean,
    droppedPlantName: String?
) {
    val minMinutes = com.example.focusflow.data.AppConfigManager.getSeedDropMinMinutes()
    
    // 状态判定
    val state = when {
        focusMinutes < minMinutes -> 0  // 时长不足
        !isDropped -> 1                  // 未掉落
        else -> 2                        // 成功掉落
    }
    
    val bgColor: Color
    val borderColor: Color
    val icon: String
    val title: String
    val subtitle: String
    
    when (state) {
        0 -> {
            bgColor = Color.Gray.copy(alpha = 0.08f)
            borderColor = Color.Gray.copy(alpha = 0.3f)
            icon = "🔒"
            title = "时长不足"
            subtitle = "需专注 $minMinutes 分钟"
        }
        1 -> {
            bgColor = CyberSecondary.copy(alpha = 0.08f)
            borderColor = CyberSecondary.copy(alpha = 0.4f)
            icon = "📡"
            title = "探测中"
            subtitle = "未捕获信号"
        }
        else -> {
            bgColor = Color(0xFFD946EF).copy(alpha = 0.1f)
            borderColor = Color(0xFFD946EF).copy(alpha = 0.6f)
            icon = "📦"
            title = "种子获取！"
            subtitle = "前往背包解析查看"
        }
    }
    
    // 掉落时的脉冲动画
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by if (state == 2) {
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.03f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
    } else {
        remember { mutableStateOf(1f) }
    }
    
    Surface(
        modifier = modifier
            .height(90.dp)
            .scale(pulseScale)
            .clip(RoundedCornerShape(14.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(14.dp)),
        color = bgColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = icon, fontSize = 22.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                color = borderColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = CyberTextSub,
                fontSize = 11.sp,
                maxLines = 1
            )
        }
    }
}

/**
 * 花园充能卡片
 */
@Composable
fun GardenChargeCard(
    modifier: Modifier = Modifier,
    purifiedTiles: Int
) {
    val hasCharged = purifiedTiles > 0
    
    val bgColor: Color
    val borderColor: Color
    val icon: String
    val title: String
    val subtitle: String
    
    if (hasCharged) {
        bgColor = Color(0xFF10B981).copy(alpha = 0.1f)
        borderColor = Color(0xFF10B981).copy(alpha = 0.6f)
        icon = "⚡"
        title = "充能完成"
        subtitle = "植物已恢复活力"
    } else {
        bgColor = Color(0xFFF59E0B).copy(alpha = 0.08f)
        borderColor = Color(0xFFF59E0B).copy(alpha = 0.4f)
        icon = "🌱"
        title = "花园待充能"
        subtitle = "专注可激活植物"
    }
    
    Surface(
        modifier = modifier
            .height(90.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(14.dp)),
        color = bgColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = icon, fontSize = 22.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                color = borderColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = CyberTextSub,
                fontSize = 11.sp,
                maxLines = 1
            )
        }
    }
}