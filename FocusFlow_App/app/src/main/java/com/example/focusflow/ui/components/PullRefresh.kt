package com.example.focusflow.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.focusflow.ui.theme.CyberPrimary
import com.example.focusflow.ui.theme.WaterPavilionDeep

/**
 * ═══════════════════════════════════════════════════════════════
 * 统一下拉刷新组件
 * ═══════════════════════════════════════════════════════════════
 *
 * 提供统一的下拉刷新体验，支持：
 * - 自定义刷新阈值
 * - 进度指示动画
 * - 旋转加载动画
 */

/**
 * 下拉刷新状态
 */
data class PullRefreshState(
    val isRefreshing: Boolean = false,
    val refreshOffset: Float = 0f,
    val threshold: Float = 150f
) {
    val progress: Float
        get() = (refreshOffset / threshold).coerceIn(0f, 1f)
    
    val shouldRefresh: Boolean
        get() = refreshOffset >= threshold && !isRefreshing
}

/**
 * 创建下拉刷新连接
 */
@Composable
fun rememberPullRefreshState(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    threshold: Float = 150f
): Pair<PullRefreshState, NestedScrollConnection> {
    var refreshOffset by remember { mutableStateOf(0f) }
    
    val state = remember(isRefreshing, refreshOffset, threshold) {
        PullRefreshState(
            isRefreshing = isRefreshing,
            refreshOffset = refreshOffset,
            threshold = threshold
        )
    }
    
    // 检测是否触发刷新
    LaunchedEffect(state.shouldRefresh) {
        if (state.shouldRefresh) {
            onRefresh()
            refreshOffset = 0f
        }
    }
    
    // 刷新完成后重置
    LaunchedEffect(isRefreshing) {
        if (!isRefreshing) {
            refreshOffset = 0f
        }
    }
    
    val connection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // 下拉时累计偏移（available.y > 0 表示手指向下滑动）
                if (available.y > 0 && !isRefreshing) {
                    val newOffset = (refreshOffset + available.y * 0.5f).coerceAtMost(threshold * 1.5f)
                    refreshOffset = newOffset
                    // 消费下拉偏移，避免内容跟随移动
                    return Offset(0f, available.y * 0.5f)
                }
                // 上滑时重置偏移
                if (available.y < 0 && refreshOffset > 0) {
                    refreshOffset = 0f
                }
                return Offset.Zero
            }
            
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                return Offset.Zero
            }
        }
    }
    
    return Pair(state, connection)
}

/**
 * 下拉刷新指示器
 * 显示刷新动画和状态
 */
@Composable
fun PullRefreshIndicator(
    isRefreshing: Boolean,
    modifier: Modifier = Modifier,
    progressValue: Float = 0f
) {
    // 旋转动画
    val infiniteTransition = rememberInfiniteTransition(label = "refreshRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            modifier = Modifier
                .size(40.dp)
                .rotate(if (isRefreshing) rotation else progressValue * 180f),
            shape = CircleShape,
            color = WaterPavilionDeep,
            shadowElevation = 4.dp
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        color = CyberPrimary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(24.dp)
                    )
                } else if (progressValue > 0) {
                    // 显示进度圆环
                    val animatedProgress = animateFloatAsState(
                        targetValue = progressValue,
                        animationSpec = tween(300),
                        label = "progress"
                    )
                    CircularProgressIndicator(
                        progress = animatedProgress.value,
                        color = CyberPrimary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

/**
 * 简化版下拉刷新指示器
 * 仅显示刷新中的加载动画
 */
@Composable
fun SimpleRefreshIndicator(
    isRefreshing: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isRefreshing,
        enter = slideInVertically { -it },
        exit = slideOutVertically { -it },
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            WaterPavilionDeep.copy(alpha = 0.8f),
                            Color.Transparent
                        )
                    )
                )
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    color = CyberPrimary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "正在刷新...",
                    color = CyberPrimary,
                    fontSize = 14.sp
                )
            }
        }
    }
}

/**
 * 下拉刷新包装组件
 * 简化使用，只需包装内容区域
 *
 * @param isRefreshing 是否正在刷新
 * @param onRefresh 刷新回调
 * @param modifier 修饰符
 * @param content 内容
 */
@Composable
fun PullRefreshLayout(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    threshold: Float = 150f,
    content: @Composable () -> Unit
) {
    val (state, connection) = rememberPullRefreshState(isRefreshing, onRefresh, threshold)
    
    Box(modifier = modifier.nestedScroll(connection)) {
        // 内容
        content()
        
        // 刷新指示器
        if (state.refreshOffset > 0 || isRefreshing) {
            PullRefreshIndicator(
                isRefreshing = isRefreshing,
                progressValue = state.progress,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}
