package com.example.focusflow.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.focusflow.data.repository.BatchSettlementResult
import com.example.focusflow.data.repository.DroppedSeed
import com.example.focusflow.ui.theme.*

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * 批量结算面板 (Batch Settlement Dialog)
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * 【使用场景】
 * 离线专注后联网同步，统一结算奖励时显示
 * 
 * 【显示内容】
 * 1. 同步的专注记录数量
 * 2. 总专注时长
 * 3. 掉落的种子列表
 * 4. 光流奖励（已由后端结算）
 */
@Composable
fun BatchSettlementDialog(
    result: BatchSettlementResult,
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
                            imageVector = Icons.Rounded.CloudSync,
                            contentDescription = null,
                            tint = CyberPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "数据同步完成",
                            color = CyberPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // ─── 核心统计卡片 ───
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
                            text = "OFFLINE SYNC COMPLETE",
                            color = CyberPrimary.copy(alpha = 0.8f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 4.sp
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 统计数据行
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // 记录数量
                            StatItem(
                                value = "${result.totalRecords}",
                                label = "条记录",
                                icon = "📝"
                            )
                            
                            // 分隔线
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(48.dp)
                                    .background(CyberPrimary.copy(alpha = 0.2f))
                            )
                            
                            // 总时长
                            StatItem(
                                value = "${result.totalDurationMinutes}",
                                label = "分钟",
                                icon = "⏱️"
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // 光流奖励显示
                        if (result.totalRewardFlux > 0) {
                            Text(
                                text = "+${result.totalRewardFlux} 光流已注入",
                                color = CyberPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                // ─── 种子掉落区域 ───
                if (result.droppedSeeds.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                1.5.dp,
                                Color(0xFFD946EF).copy(alpha = 0.6f),
                                RoundedCornerShape(16.dp)
                            ),
                        color = Color(0xFFD946EF).copy(alpha = 0.08f)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "📦", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "获得 ${result.droppedSeeds.size} 颗种子",
                                    color = Color(0xFFD946EF),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "前往背包解析查看",
                                color = Color(0xFFD946EF).copy(alpha = 0.7f),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // ─── 操作按钮区 ───
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
                        text = "确认",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                
                // 次级按钮
                if (result.droppedSeeds.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = onNavigateToBag,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Inventory2,
                            contentDescription = null,
                            tint = CyberSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "前往收纳仓查看",
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
 * 统计项组件
 */
@Composable
private fun StatItem(
    value: String,
    label: String,
    icon: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = icon, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            text = label,
            color = CyberTextSub,
            fontSize = 12.sp
        )
    }
}
