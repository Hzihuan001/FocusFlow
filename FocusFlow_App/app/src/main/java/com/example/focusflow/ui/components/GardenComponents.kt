package com.example.focusflow.ui.components

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.example.focusflow.R
import com.example.focusflow.api.LeaderboardEntryDto
import com.example.focusflow.ui.theme.*
import com.example.focusflow.ui.viewmodel.GardenVitalityState
import com.example.focusflow.ui.viewmodel.GardenViewModel

/**
 * ═══════════════════════════════════════════════════════════════
 * 惰性枯萎状态指示器 HUD
 * ═══════════════════════════════════════════════════════════════
 *
 * 功能：
 * - 显示当前花园的活力状态（VIBRANT/WARNING/WITHERED）
 * - 距离上次专注时间
 * - 状态切换动画效果
 *
 * 【性能优化】
 * - 使用条件渲染，仅在 WARNING/WITHERED 状态时运行动画
 * - VIBRANT 状态完全无动画开销
 */
@Composable
fun VitalityStateIndicator(
    vitalityState: GardenVitalityState,
    hoursSinceLastFocus: Float,
    hasFocusRecord: Boolean,
    modifier: Modifier = Modifier
) {
    // 状态颜色配置
    val stateColor = when (vitalityState) {
        GardenVitalityState.VIBRANT -> MonumentMint
        GardenVitalityState.WARNING -> Color(0xFFFFB74D) // 橙黄
        GardenVitalityState.WITHERED -> Color(0xFF8D6E63) // 灰棕
    }

    // 状态图标
    val stateIcon = when (vitalityState) {
        GardenVitalityState.VIBRANT -> "🌱"
        GardenVitalityState.WARNING -> "⚠️"
        GardenVitalityState.WITHERED -> "🥀"
    }

    // 颜色过渡动画（低成本，保留）
    val animatedColor by animateColorAsState(
        targetValue = stateColor,
        animationSpec = tween(500),
        label = "stateColorAnimation"
    )

    // 🟢 [PERF] 条件渲染：仅非 VIBRANT 状态时运行脉冲动画
    val shouldPulse = vitalityState != GardenVitalityState.VIBRANT
    
    if (shouldPulse) {
        // 脉冲版本 - 带动画
        PulsingVitalityCard(
            animatedColor = animatedColor,
            stateIcon = stateIcon,
            vitalityState = vitalityState,
            hoursSinceLastFocus = hoursSinceLastFocus,
            hasFocusRecord = hasFocusRecord,
            modifier = modifier
        )
    } else {
        // 静态版本 - 无动画
        StaticVitalityCard(
            animatedColor = animatedColor,
            stateIcon = stateIcon,
            vitalityState = vitalityState,
            hoursSinceLastFocus = hoursSinceLastFocus,
            hasFocusRecord = hasFocusRecord,
            modifier = modifier
        )
    }
}

/**
 * 静态活力状态卡片（VIBRANT 状态使用，无动画开销）
 */
@Composable
private fun StaticVitalityCard(
    animatedColor: Color,
    stateIcon: String,
    vitalityState: GardenVitalityState,
    hoursSinceLastFocus: Float,
    hasFocusRecord: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = animatedColor.copy(alpha = 0.15f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, animatedColor.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        VitalityContent(
            stateIcon = stateIcon,
            vitalityState = vitalityState,
            hoursSinceLastFocus = hoursSinceLastFocus,
            hasFocusRecord = hasFocusRecord,
            animatedColor = animatedColor
        )
    }
}

/**
 * 脉冲活力状态卡片（WARNING/WITHERED 状态使用）
 */
@Composable
private fun PulsingVitalityCard(
    animatedColor: Color,
    stateIcon: String,
    vitalityState: GardenVitalityState,
    hoursSinceLastFocus: Float,
    hasFocusRecord: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "alertPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Surface(
        color = animatedColor.copy(alpha = 0.15f * pulseAlpha),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, animatedColor.copy(alpha = 0.5f * pulseAlpha)),
        modifier = modifier
    ) {
        VitalityContent(
            stateIcon = stateIcon,
            vitalityState = vitalityState,
            hoursSinceLastFocus = hoursSinceLastFocus,
            hasFocusRecord = hasFocusRecord,
            animatedColor = animatedColor
        )
    }
}

/**
 * 活力状态内容（共用组件）
 */
@Composable
private fun VitalityContent(
    stateIcon: String,
    vitalityState: GardenVitalityState,
    hoursSinceLastFocus: Float,
    hasFocusRecord: Boolean,
    animatedColor: Color
) {
    val appColors = LocalAppColors.current
    
    Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stateIcon,
            fontSize = 16.sp
        )
        Column {
            Text(
                text = vitalityState.displayName,
                color = animatedColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            if (hasFocusRecord) {
                Text(
                    text = String.format("%.1f小时前", hoursSinceLastFocus),
                    color = appColors.textSub,  // 使用主题副文字色
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium  // 增加字重
                )
            } else {
                Text(
                    text = "暂无专注记录",
                    color = appColors.textSub,  // 使用主题副文字色
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium  // 增加字重
                )
            }
        }
    }
}

/**
 * ═══════════════════════════════════════════════════════════════
 * 排行榜弹窗组件
 * ═══════════════════════════════════════════════════════════════
 */
@Composable
fun LeaderboardDialog(
    entries: List<LeaderboardEntryDto>,
    isLoading: Boolean,
    currentUserId: Long?,
    onDismiss: () -> Unit
) {
    // 排名颜色配置
    val rankColors = listOf(
        Color(0xFFFFD700), // 金色 - 第1名
        Color(0xFFC0C0C0), // 银色 - 第2名
        Color(0xFFCD7F32), // 铜色 - 第3名
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = WaterPavilionDeep.copy(alpha = 0.98f),
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🏆", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "点亮排行榜",
                    color = MonumentMint,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            if (isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(5) {
                        SkeletonLeaderboardItem()
                    }
                }
            } else if (entries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无排行数据",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(entries) { entry ->
                        val isCurrentUser = entry.userId == currentUserId
                        val rankColor = when {
                            entry.rank == 1 -> rankColors[0]
                            entry.rank == 2 -> rankColors[1]
                            entry.rank == 3 -> rankColors[2]
                            else -> Color.White.copy(alpha = 0.6f)
                        }
                        
                        Surface(
                            color = if (isCurrentUser) 
                                MonumentMint.copy(alpha = 0.2f) 
                            else 
                                Color.White.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(12.dp),
                            border = if (isCurrentUser) 
                                BorderStroke(1.dp, MonumentMint.copy(alpha = 0.5f)) 
                            else 
                                null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 排名
                                Text(
                                    text = if (entry.rank <= 3) {
                                        when (entry.rank) {
                                            1 -> "🥇"
                                            2 -> "🥈"
                                            3 -> "🥉"
                                            else -> "${entry.rank}"
                                        }
                                    } else {
                                        "${entry.rank}"
                                    },
                                    color = rankColor,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(36.dp)
                                )
                                
                                // 用户信息
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = entry.nickname ?: entry.account ?: "未知用户",
                                        color = if (isCurrentUser) MonumentMint else Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (entry.account != null) {
                                        Text(
                                            text = "@${entry.account}",
                                            color = Color.White.copy(alpha = 0.4f),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                
                                // 点亮数量
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = "${entry.purifiedCount}",
                                        color = if (isCurrentUser) MonumentMint else Color(0xFF4DD0E1),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "格",
                                        color = Color.White.copy(alpha = 0.4f),
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "关闭",
                    color = MonumentMint,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

/**
 * 点亮花园按钮
 *
 * 设计要点：
 * - 实色背景 (CyberBgDeep)：杜绝透明叠层造成的视觉穿透
 * - CyberPrimary 霓虹外发光（shadow + border 双层）
 * - 点亮后文案变为"今日已点亮"，按钮置灰
 *
 * 【性能优化】
 * - 使用条件渲染：已点亮状态完全无动画开销
 * - 仅未点亮时运行呼吸动画
 */
@Composable
fun PulseChargeButton(
    isLoading: Boolean,
    isCharged: Boolean,
    onClick: () -> Unit
) {
    // 🟢 [PERF] 条件渲染：已点亮时使用静态版本
    if (isCharged) {
        StaticPulseButton(
            isCharged = true,
            onClick = onClick
        )
    } else if (isLoading) {
        LoadingPulseButton()
    } else {
        GlowingPulseButton(
            isCharged = false,
            onClick = onClick
        )
    }
}

/**
 * 静态点亮按钮（已点亮状态，无动画）
 */
@Composable
private fun StaticPulseButton(
    isCharged: Boolean,
    onClick: () -> Unit
) {
    val glowColor = Color.White.copy(alpha = 0.2f)

    Box(
        modifier = Modifier
            .fillMaxWidth(0.82f)
            .height(68.dp)
            .shadow(elevation = 16.dp, shape = RoundedCornerShape(34.dp), ambientColor = glowColor, spotColor = glowColor)
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(34.dp), ambientColor = glowColor, spotColor = glowColor),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = onClick,
            enabled = false,
            modifier = Modifier.fillMaxSize(),
            colors = ButtonDefaults.buttonColors(
                containerColor = WaterPavilionDeep,
                disabledContainerColor = WaterPavilionDeep
            ),
            shape = RoundedCornerShape(34.dp)
        ) {
            Text(
                text = "今日已点亮",
                color = Color.White.copy(alpha = 0.3f),
                fontWeight = FontWeight.Light,
                fontSize = 18.sp,
                letterSpacing = 1.sp
            )
        }
    }
}

/**
 * 加载中按钮
 */
@Composable
private fun LoadingPulseButton() {
    val glowColor = MonumentMint

    Box(
        modifier = Modifier
            .fillMaxWidth(0.82f)
            .height(68.dp)
            .shadow(elevation = 16.dp, shape = RoundedCornerShape(34.dp), ambientColor = glowColor, spotColor = glowColor)
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(34.dp), ambientColor = glowColor, spotColor = glowColor),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = {},
            enabled = false,
            modifier = Modifier.fillMaxSize(),
            colors = ButtonDefaults.buttonColors(
                containerColor = WaterPavilionDeep,
                disabledContainerColor = WaterPavilionDeep
            ),
            shape = RoundedCornerShape(34.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = MonumentMint,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "正在点亮...",
                    color = MonumentMint,
                    fontWeight = FontWeight.Light,
                    fontSize = 16.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

/**
 * 呼吸发光按钮（未点亮状态，带动画）
 */
@Composable
private fun GlowingPulseButton(
    isCharged: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulseGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "borderGlow"
    )

    val glowColor = MonumentMint

    Box(
        modifier = Modifier
            .fillMaxWidth(0.82f)
            .height(68.dp)
            .shadow(elevation = 16.dp, shape = RoundedCornerShape(34.dp), ambientColor = glowColor, spotColor = glowColor)
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(34.dp), ambientColor = glowColor, spotColor = glowColor),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = onClick,
            enabled = true,
            modifier = Modifier.fillMaxSize(),
            colors = ButtonDefaults.buttonColors(
                containerColor = WaterPavilionDeep,
                disabledContainerColor = WaterPavilionDeep
            ),
            shape = RoundedCornerShape(34.dp)
        ) {
            Text(
                text = "点亮花园",
                color = MonumentMint.copy(alpha = glowAlpha),
                fontWeight = FontWeight.Light,
                fontSize = 18.sp,
                letterSpacing = 1.sp
            )
        }
    }
}

/**
 * ═══════════════════════════════════════════════════════════════
 * 断网提示界面
 * ═══════════════════════════════════════════════════════════════
 */
@Composable
fun NetworkUnavailableScreen(
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(Color(0xFF0F0F19), Color.Black))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 断网图标
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFFF9500),
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "网络不可用",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "花园功能需要连接服务器",
                color = Color.Gray,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "请检查网络连接后重试",
                color = Color.Gray,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 提示：专注功能仍可使用
            Text(
                text = "💡 专注功能在离线时仍可使用",
                color = CyberPrimary,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 返回按钮
            OutlinedButton(
                onClick = onBack,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberPrimary),
                border = BorderStroke(1.dp, CyberPrimary)
            ) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("返回")
            }
        }
    }
}

/**
 * 处理格子点击事件
 */
fun handleTileClick(
    x: Int,
    y: Int,
    plants: List<GardenViewModel.LegacyPlant>,
    purifiedTiles: Set<Pair<Int, Int>>,
    viewModel: GardenViewModel,
    context: android.content.Context,
    onPlantClicked: (String) -> Unit,
    onOpenBag: () -> Unit
) {
    // 情况 A：点击了已有的植物
    // 🔧 [CENTER CLICK] 点击地块作为中心，检查点击坐标是否在植物的 width 半径内
    val clickedPlant = plants.find { plant ->
        val plantInfo = viewModel.getPlantRenderInfo(plant)
        if (plantInfo != null) {
            // 植物中心是 plant.x, plant.y
            // width=1 半径=0, width=3 半径=1, width=5 半径=2
            val radius = (plantInfo.width - 1) / 2
            val dx = kotlin.math.abs(x - plant.x)
            val dy = kotlin.math.abs(y - plant.y)
            dx <= radius && dy <= radius
        } else {
            false
        }
    }
    
    if (clickedPlant != null) {
        // 触发植物详情弹窗
        onPlantClicked(clickedPlant.instanceId)
        return
    }
    
    // 情况 B：点击了空地 (净化交互锁 Purified Lock)
    if (purifiedTiles.contains(Pair(x, y))) {
        // 在已净化的领土上，可以种植
        onOpenBag()
    } else {
        // 点击了暗色废土
        Toast.makeText(context, "此区域受废土污染尚未净化，无法种植！", Toast.LENGTH_SHORT).show()
    }
}
