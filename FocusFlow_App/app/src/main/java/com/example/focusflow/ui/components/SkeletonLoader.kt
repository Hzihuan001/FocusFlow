package com.example.focusflow.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.focusflow.ui.theme.MonumentMint

/**
 * ═══════════════════════════════════════════════════════════════
 * 骨架屏加载组件
 * ═══════════════════════════════════════════════════════════════
 *
 * 用于列表加载时显示占位动画，提升用户感知速度
 * 支持自定义形状、尺寸和颜色
 */

/**
 * 骨架屏基础组件
 *
 * @param modifier 修饰符
 * @param shape 形状
 * @param width 宽度
 * @param height 高度
 */
@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    width: Dp = 100.dp,
    height: Dp = 16.dp,
    shape: Shape = RoundedCornerShape(8.dp)
) {
    // 闪烁动画
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    // 骨架屏颜色
    val skeletonColor = Color(0xFF2A3A3A)
    val shimmerColor = Color(0xFF3A4A4A)

    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(skeletonColor, shimmerColor, skeletonColor),
                    start = Offset(shimmerOffset - 300, 0f),
                    end = Offset(shimmerOffset, 0f)
                )
            )
    )
}

/**
 * 圆形骨架屏（用于头像）
 */
@Composable
fun SkeletonCircle(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    SkeletonBox(
        modifier = modifier,
        width = size,
        height = size,
        shape = CircleShape
    )
}

/**
 * 列表项骨架屏
 * 用于好友列表、访客日志等列表加载
 */
@Composable
fun SkeletonListItem(
    modifier: Modifier = Modifier,
    showAvatar: Boolean = true,
    showSubtitle: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 头像骨架
        if (showAvatar) {
            SkeletonCircle(size = 48.dp)
            Spacer(modifier = Modifier.width(12.dp))
        }

        // 文本骨架
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            // 标题骨架
            SkeletonBox(
                width = 120.dp,
                height = 16.dp
            )
            
            if (showSubtitle) {
                Spacer(modifier = Modifier.height(8.dp))
                // 副标题骨架
                SkeletonBox(
                    width = 80.dp,
                    height = 12.dp
                )
            }
        }

        // 右侧图标骨架
        SkeletonBox(
            width = 60.dp,
            height = 28.dp,
            shape = RoundedCornerShape(14.dp)
        )
    }
}

/**
 * 植物卡片骨架屏
 * 用于花园种植面板
 */
@Composable
fun SkeletonPlantCard(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(100.dp)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 植物图片骨架
        SkeletonBox(
            width = 80.dp,
            height = 80.dp,
            shape = RoundedCornerShape(12.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 名称骨架
        SkeletonBox(
            width = 60.dp,
            height = 14.dp
        )
    }
}

/**
 * 排行榜条目骨架屏
 */
@Composable
fun SkeletonLeaderboardItem(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 排名骨架
        SkeletonBox(
            width = 36.dp,
            height = 24.dp
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // 用户信息骨架
        Column(modifier = Modifier.weight(1f)) {
            SkeletonBox(width = 100.dp, height = 16.dp)
            Spacer(modifier = Modifier.height(4.dp))
            SkeletonBox(width = 60.dp, height = 12.dp)
        }
        
        // 数量骨架
        SkeletonBox(
            width = 50.dp,
            height = 20.dp
        )
    }
}

/**
 * 花园地块骨架屏
 */
@Composable
fun SkeletonGardenTile(
    modifier: Modifier = Modifier
) {
    SkeletonBox(
        modifier = modifier,
        width = 80.dp,
        height = 60.dp,
        shape = RoundedCornerShape(8.dp)
    )
}

/**
 * 数据统计卡片骨架屏
 */
@Composable
fun SkeletonStatsCard(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // 标题骨架
        SkeletonBox(width = 100.dp, height = 14.dp)
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // 数值骨架
        SkeletonBox(width = 80.dp, height = 32.dp)
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 副文本骨架
        SkeletonBox(width = 140.dp, height = 12.dp)
    }
}

/**
 * 骨架屏列表
 * 快速生成多个骨架屏条目
 *
 * @param count 条目数量
 * @param itemContent 单个条目的骨架屏内容
 */
@Composable
fun SkeletonList(
    count: Int = 5,
    itemContent: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(count) {
            itemContent()
        }
    }
}