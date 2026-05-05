package com.example.focusflow.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.focusflow.ui.theme.LocalAppColors
import com.example.focusflow.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.flow.collectLatest

/**
 * AuthScreen —— 赛博朋克风格登录/注册大门
 *
 * 视觉设计亮点：
 *   ① 呼吸光晕：drawBehind 在输入框后绘制脉动的霓虹 Glow，无需图片资源
 *   ② 双模式切换：登录/注册通过 isRegisterMode 状态机无缝切换，AnimatedVisibility 驱动
 *   ③ 扫描线动画：无限循环的水平扫描线模拟 CRT 显示器效果
 */
@Composable
fun AuthScreen(
    onLoginSuccess: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    val isLoading by authViewModel.isLoading.collectAsState()
    val errorMsg  by authViewModel.errorMsg.collectAsState()
    
    // 使用主题颜色
    val appColors = LocalAppColors.current

    // 收集一次性导航事件
    LaunchedEffect(Unit) {
        authViewModel.navigationEvent.collectLatest {
            onLoginSuccess()
        }
    }

    var username     by remember { mutableStateOf("") }
    var password     by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isRegisterMode by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    // ══ 扫描线动画（无限循环，从上到下）══
    val infiniteTransition = rememberInfiniteTransition(label = "scan_line")
    val scanY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scan_y"
    )
    // 呼吸光晕强度
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    val cyberGreen = if (appColors.isDark) Color(0xFF00FFAA) else Color(0xFF00875A)
    val neonPurple = if (appColors.isDark) Color(0xFFBB00FF) else Color(0xFF666666)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appColors.bgDeep)
            // 扫描线：在整个屏幕上绘制一条横向光带
            .drawBehind {
                val lineY = size.height * scanY
                drawLine(
                    color = appColors.primary.copy(alpha = 0.12f),
                    start = Offset(0f, lineY),
                    end = Offset(size.width, lineY),
                    strokeWidth = 2.dp.toPx()
                )
                // 网格背景（极低透明度）
                val gridSpacing = 48.dp.toPx()
                var x = 0f
                while (x < size.width) {
                    drawLine(appColors.primary.copy(alpha = 0.03f), Offset(x, 0f), Offset(x, size.height), 1f)
                    x += gridSpacing
                }
                var y = 0f
                while (y < size.height) {
                    drawLine(appColors.primary.copy(alpha = 0.03f), Offset(0f, y), Offset(size.width, y), 1f)
                    y += gridSpacing
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // ══ 标题区 ══
            Text(
                text = "FOCUS",
                fontSize = 52.sp,
                fontWeight = FontWeight.Black,
                style = TextStyle(
                    brush = Brush.horizontalGradient(listOf(appColors.primary, neonPurple)),
                    shadow = Shadow(appColors.primary, blurRadius = 24f)
                )
            )
            Text(
                text = "F L O W",
                fontSize = 16.sp,
                letterSpacing = 12.sp,
                color = appColors.primary.copy(alpha = 0.7f),
                fontWeight = FontWeight.Light
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (isRegisterMode) "// 初始化" else "",
                fontSize = 12.sp,
                color = cyberGreen.copy(alpha = 0.6f),
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )

            Spacer(Modifier.height(40.dp))

            // ══ 输入面板（带霓虹 Glow）══
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = appColors.cardBg,
                shape = RoundedCornerShape(4.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Brush.horizontalGradient(listOf(appColors.primary.copy(alpha = glowAlpha), neonPurple.copy(alpha = glowAlpha * 0.6f)))
                )
            ) {
                Column(modifier = Modifier.padding(24.dp)) {

                    // ── Username 输入框 ──
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it; authViewModel.clearError() },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("用户名", color = appColors.primary.copy(alpha = 0.7f)) },
                        placeholder = { Text("输入你的用户名...", color = appColors.textSub, fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Person, null, tint = appColors.primary.copy(alpha = 0.8f))
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        textStyle = TextStyle(color = appColors.textMain, fontSize = 15.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = appColors.primary,
                            unfocusedBorderColor = appColors.primary.copy(alpha = 0.3f),
                            cursorColor = appColors.primary,
                            focusedLabelColor = appColors.primary,
                        )
                    )

                    Spacer(Modifier.height(16.dp))

                    // ── Password 输入框 ──
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; authViewModel.clearError() },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("密钥序列", color = appColors.primary.copy(alpha = 0.7f)) },
                        placeholder = { Text("输入你的密钥...", color = appColors.textSub, fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, null, tint = appColors.primary.copy(alpha = 0.8f))
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = appColors.primary.copy(alpha = 0.6f)
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = if (isRegisterMode) ImeAction.Next else ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) },
                            onDone = {
                                focusManager.clearFocus()
                                if (isRegisterMode) {
                                    authViewModel.register(username, password, confirmPassword)
                                } else {
                                    authViewModel.login(username, password)
                                }
                            }
                        ),
                        textStyle = TextStyle(color = appColors.textMain, fontSize = 15.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = appColors.primary,
                            unfocusedBorderColor = appColors.primary.copy(alpha = 0.3f),
                            cursorColor = appColors.primary,
                            focusedLabelColor = appColors.primary,
                        )
                    )

                    // ── 确认密码（仅注册模式显示）──
                    AnimatedVisibility(visible = isRegisterMode, enter = fadeIn(), exit = fadeOut()) {
                        Column {
                            Spacer(Modifier.height(16.dp))
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it; authViewModel.clearError() },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("确认密钥", color = appColors.primary.copy(alpha = 0.7f)) },
                                placeholder = { Text("再次输入密钥...", color = appColors.textSub, fontSize = 13.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.Lock, null, tint = appColors.primary.copy(alpha = 0.8f))
                                },
                                trailingIcon = {
                                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                        Icon(
                                            if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = null,
                                            tint = appColors.primary.copy(alpha = 0.6f))
                                    }
                                },
                                singleLine = true,
                                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = {
                                    focusManager.clearFocus()
                                    authViewModel.register(username, password, confirmPassword)
                                }),
                                textStyle = TextStyle(color = appColors.textMain, fontSize = 15.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = appColors.primary,
                                    unfocusedBorderColor = appColors.primary.copy(alpha = 0.3f),
                                    cursorColor = appColors.primary,
                                    focusedLabelColor = appColors.primary,
                                )
                            )
                        }
                    }

                    // ── 错误信息 ──
                    AnimatedVisibility(visible = errorMsg != null, enter = fadeIn(), exit = fadeOut()) {
                        errorMsg?.let {
                            Text(
                                text = "⚠ $it",
                                color = if (appColors.isDark) Color(0xFFFF6B6B) else Color(0xFFDC2626),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ══ 接入矩阵按钮 ══
            Button(
                onClick = {
                    focusManager.clearFocus()
                    if (isRegisterMode) {
                        authViewModel.register(username, password, confirmPassword)
                    } else {
                        authViewModel.login(username, password)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isLoading,
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = appColors.primary,
                    contentColor = if (appColors.isDark) Color.Black else Color.White,
                    disabledContainerColor = appColors.primary.copy(alpha = 0.3f)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = if (appColors.isDark) Color.Black else Color.White,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("连接矩阵中...", fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                } else {
                    Text(
                        text = if (isRegisterMode) "▶ 初始化身份" else "▶ 接入矩阵 ENTER",
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ══ 切换模式按钮 ══
            TextButton(onClick = {
                isRegisterMode = !isRegisterMode
                authViewModel.clearError()
            }) {
                Text(
                    text = if (isRegisterMode) "登录" else "注册",
                    color = appColors.primary.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
            }

            Spacer(Modifier.height(32.dp))

            // ══ 底部签名 ══
            Text(
                text = "CYBER WASTELAND v2.5 // NEURAL LINK STABLE",
                fontSize = 9.sp,
                color = appColors.textSub.copy(alpha = 0.3f),
                textAlign = TextAlign.Center,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
