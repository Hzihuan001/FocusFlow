package com.example.focusflow.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.graphicsLayer
import com.example.focusflow.ui.theme.CyberPrimary
import com.example.focusflow.ui.theme.LocalAppColors
import kotlinx.coroutines.delay

/**
 * 赛博滚动数字动画组件
 *
 * 【视觉特效】
 * 1. 数字滚动：新旧数值之间平滑过渡
 * 2. 霓虹光晕：外发光效果增强科技感
 * 3. 扫描线：CRT 显示器风格的水平扫描线
 *
 * 【使用场景】
 * - 个人中心顶部光流余额显示
 * - 专注完成后的奖励数字跳动
 */
@Composable
fun CyberScrollingNumber(
    value: Int,
    modifier: Modifier = Modifier,
    label: String = "光流余额",
    prefix: String = "✨ ",
    isConnected: Boolean? = true // null=检查中, true=已连接, false=断开
) {
    val appColors = LocalAppColors.current
    
    // ───────────── 动画状态 ─────────────

    // 目标值和当前显示值
    var displayValue by remember { mutableStateOf(value) }
    var previousValue by remember { mutableStateOf(value) }

    // 滚动动画进度 (0f -> 1f)
    val scrollProgress = remember { Animatable(1f) }

    // 扫描线位置
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val scanLinePosition by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanLine"
    )

    // 呼吸光晕强度
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    // ───────────── 数值变化检测 ─────────────

    LaunchedEffect(value) {
        if (value != displayValue) {
            previousValue = displayValue

            // 启动滚动动画
            scrollProgress.snapTo(0f)
            scrollProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 800,
                    easing = FastOutSlowInEasing
                )
            )

            displayValue = value
        }
    }

    // ───────────── 渲染组件 ─────────────

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        appColors.cardBg.copy(alpha = if (appColors.isDark) 0.9f else 1f),
                        appColors.cardBg.copy(alpha = if (appColors.isDark) 0.95f else 1f)
                    )
                )
            )
            .then(
                // 扫描线效果
                Modifier.drawBehind {
                    val lineHeight = 1.dp.toPx()
                    val y = size.height * scanLinePosition
                    drawLine(
                        color = appColors.primary.copy(alpha = 0.15f),
                        start = androidx.compose.ui.geometry.Offset(0f, y),
                        end = androidx.compose.ui.geometry.Offset(size.width, y),
                        strokeWidth = lineHeight
                    )
                }
            )
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // 左侧标签
            Column {
                Text(
                    text = label,
                    color = appColors.textSub,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "TIME FLUX",
                    color = appColors.primary.copy(alpha = 0.5f),
                    fontSize = 9.sp,
                    letterSpacing = 2.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // 右侧数值（带滚动动画）
            Box(
                contentAlignment = Alignment.CenterEnd
            ) {
                // 🟢 [OFFLINE] 离线时显示 "失去连接"
                if (isConnected == false) {
                    CyberGlowText(
                        text = "失去连接",
                        glowAlpha = 0.5f,
                        textColor = appColors.textSub
                    )
                } else {
                    // 使用淡入淡出效果
                    androidx.compose.animation.AnimatedVisibility(
                        visible = scrollProgress.value >= 0.5f,
                        enter = androidx.compose.animation.fadeIn(tween(400)),
                        exit = androidx.compose.animation.fadeOut(tween(400))
                    ) {
                        CyberGlowText(
                            text = "$prefix$displayValue",
                            glowAlpha = glowAlpha,
                            textColor = appColors.primary
                        )
                    }

                    // 旧值（淡出）
                    if (scrollProgress.value < 0.5f && previousValue != displayValue) {
                        CyberGlowText(
                            text = "$prefix$previousValue",
                            glowAlpha = glowAlpha * (1f - scrollProgress.value * 2),
                            textColor = appColors.primary,
                            modifier = Modifier.graphicsLayer { alpha = 1f - scrollProgress.value * 2 }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 带霓虹光晕的文字组件
 */
@Composable
private fun CyberGlowText(
    text: String,
    glowAlpha: Float,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        color = textColor,
        fontSize = 26.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 2.sp,
        textAlign = TextAlign.End,
        style = androidx.compose.ui.text.TextStyle(
            shadow = androidx.compose.ui.graphics.Shadow(
                color = textColor.copy(alpha = glowAlpha),
                blurRadius = 12f,
                offset = androidx.compose.ui.geometry.Offset(0f, 0f)
            )
        ),
        modifier = modifier
    )
}

/**
 * 同步状态指示器组件
 *
 * 【功能】
 * - 显示当前同步状态（同步中/成功/失败）
 * - 显示待同步记录数
 * - 点击触发手动同步
 */
@Composable
fun SyncStatusIndicator(
    isSyncing: Boolean,
    pendingCount: Int,
    lastSyncTime: Long?,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusText = when {
        isSyncing -> "同步中..."
        pendingCount > 0 -> "$pendingCount 条待同步"
        lastSyncTime != null -> {
            val seconds = (System.currentTimeMillis() - lastSyncTime) / 1000
            when {
                seconds < 60 -> "刚刚同步"
                seconds < 3600 -> "${seconds / 60}分钟前"
                else -> "${seconds / 3600}小时前"
            }
        }
        else -> "未同步"
    }

    val statusColor = when {
        isSyncing -> CyberPrimary
        pendingCount > 0 -> Color(0xFFFF9500) // 橙色警告
        else -> Color.Gray
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.3f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        // 同步图标（带旋转动画）
        if (isSyncing) {
            val rotation by rememberInfiniteTransition(label = "rotation")
                .animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "rotation"
                )

            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier
                    .size(14.dp)
                    .graphicsLayer { rotationZ = rotation }
            )
        } else {
            Icon(
                imageVector = Icons.Default.CloudDone,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(14.dp)
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = statusText,
            color = statusColor,
            fontSize = 11.sp,
            letterSpacing = 0.5.sp
        )
    }
}
