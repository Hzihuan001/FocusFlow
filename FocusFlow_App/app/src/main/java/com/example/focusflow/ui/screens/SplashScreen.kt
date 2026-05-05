package com.example.focusflow.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.focusflow.R
import com.example.focusflow.ui.theme.CyberPrimary
import com.example.focusflow.ui.theme.CyberTertiary
import com.example.focusflow.ui.theme.LocalAppColors
import kotlinx.coroutines.delay

/**
 * 启动界面
 * 
 * 显示 Logo + FocusFlow 字样，配合渐入动画效果
 */
@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    val appColors = LocalAppColors.current
    
    // 动画状态
    var startAnimation by remember { mutableStateOf(false) }
    
    // Logo 缩放动画
    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.6f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    // Logo 透明度动画
    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 600, easing = LinearEasing),
        label = "alpha"
    )
    
    // 文字透明度动画（延迟出现）
    val textAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = 800,
            delayMillis = 400,
            easing = LinearEasing
        ),
        label = "textAlpha"
    )
    
    // 启动动画
    LaunchedEffect(Unit) {
        startAnimation = true
        // 延迟后跳转
        delay(1800)
        onSplashFinished()
    }
    
    // 背景
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appColors.bgDeep),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo 图片 - 使用 Coil 加载 mipmap 中的 foreground 图片
            val context = androidx.compose.ui.platform.LocalContext.current
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(R.mipmap.focus_flow_logo_foreground)
                    .build(),
                contentDescription = "FocusFlow Logo",
                modifier = Modifier
                    .size(120.dp)
                    .scale(scaleAnim)
                    .alpha(alphaAnim)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // FocusFlow 文字（霓虹绿渐变效果）
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = appColors.primary)) {
                        append("Focus")
                    }
                    withStyle(SpanStyle(color = if (appColors.isDark) CyberTertiary else appColors.textSub)) {
                        append("Flow")
                    }
                },
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.alpha(textAlpha),
                letterSpacing = 2.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 副标题
            Text(
                text = "注意力管理系统",
                fontSize = 14.sp,
                color = appColors.textSub,
                modifier = Modifier.alpha(textAlpha),
                letterSpacing = 4.sp
            )
        }
        
        // 底部版本信息
        Text(
            text = "v1.0.0",
            fontSize = 12.sp,
            color = appColors.textSub.copy(alpha = 0.5f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
                .alpha(textAlpha)
        )
    }
}
