package com.example.focusflow.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.focusflow.ui.theme.CyberPrimary
import com.example.focusflow.ui.theme.WaterPavilionDeep
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ═══════════════════════════════════════════════════════════════
 * 全局错误处理组件
 * ═══════════════════════════════════════════════════════════════
 *
 * 用于统一处理网络异常、Token过期、服务器错误等情况
 */

/**
 * 错误类型枚举
 */
enum class ErrorType {
    NETWORK_ERROR,      // 网络连接失败
    SERVER_ERROR,       // 服务器错误 (5xx)
    AUTH_ERROR,         // 认证失败 (401/403)
    NOT_FOUND,          // 资源不存在 (404)
    RATE_LIMIT,         // 请求过于频繁 (429)
    UNKNOWN             // 未知错误
}

/**
 * 错误信息数据类
 */
data class AppError(
    val type: ErrorType,
    val message: String,
    val throwable: Throwable? = null
) {
    companion object {
        fun network(message: String = "网络连接失败，请检查网络设置") = 
            AppError(ErrorType.NETWORK_ERROR, message)
        
        fun server(message: String = "服务器开小差了，请稍后重试") = 
            AppError(ErrorType.SERVER_ERROR, message)
        
        fun auth(message: String = "登录已过期，请重新登录") = 
            AppError(ErrorType.AUTH_ERROR, message)
        
        fun notFound(message: String = "请求的资源不存在") = 
            AppError(ErrorType.NOT_FOUND, message)
        
        fun rateLimit(message: String = "请求过于频繁，请稍后再试") = 
            AppError(ErrorType.RATE_LIMIT, message)
        
        fun unknown(message: String = "发生未知错误") = 
            AppError(ErrorType.UNKNOWN, message)
        
        /**
         * 从异常创建错误对象
         */
        fun fromThrowable(throwable: Throwable): AppError {
            val message = throwable.message ?: "发生未知错误"
            return when {
                message.contains("network", ignoreCase = true) ||
                message.contains("connection", ignoreCase = true) ||
                message.contains("timeout", ignoreCase = true) -> network(message)
                
                message.contains("401") || message.contains("403") -> auth()
                message.contains("404") -> notFound()
                message.contains("429") -> rateLimit()
                message.contains("500") || message.contains("502") || 
                message.contains("503") -> server()
                
                else -> unknown(message)
            }
        }
    }
}

/**
 * 全局错误状态管理器
 */
object GlobalErrorHandler {
    private val _currentError = MutableStateFlow<AppError?>(null)
    val currentError: StateFlow<AppError?> = _currentError.asStateFlow()
    
    /**
     * 显示错误
     */
    fun showError(error: AppError) {
        _currentError.value = error
    }
    
    /**
     * 显示错误（从异常）
     */
    fun showError(throwable: Throwable) {
        showError(AppError.fromThrowable(throwable))
    }
    
    /**
     * 清除错误
     */
    fun clearError() {
        _currentError.value = null
    }
}

/**
 * 错误提示横幅组件
 * 在页面顶部显示错误信息
 */
@Composable
fun ErrorBanner(
    error: AppError,
    onDismiss: () -> Unit = {},
    onRetry: (() -> Unit)? = null
) {
    val backgroundColor = when (error.type) {
        ErrorType.NETWORK_ERROR -> Color(0xFFFF9500)  // 橙色
        ErrorType.AUTH_ERROR -> Color(0xFFFF3B30)      // 红色
        ErrorType.SERVER_ERROR -> Color(0xFFFF9500)    // 橙色
        ErrorType.RATE_LIMIT -> Color(0xFFFFCC00)      // 黄色
        else -> Color(0xFF8E8E93)                      // 灰色
    }
    
    Surface(
        color = backgroundColor.copy(alpha = 0.95f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = error.message,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            
            if (onRetry != null) {
                TextButton(
                    onClick = onRetry,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Text("重试", fontWeight = FontWeight.Bold)
                }
            }
            
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color.White.copy(alpha = 0.8f)
                )
            ) {
                Text("关闭")
            }
        }
    }
}

/**
 * 错误对话框组件
 * 用于显示严重错误需要用户确认的情况
 */
@Composable
fun ErrorDialog(
    error: AppError,
    onDismiss: () -> Unit,
    onRetry: (() -> Unit)? = null
) {
    val context = LocalContext.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = WaterPavilionDeep,
        shape = RoundedCornerShape(20.dp),
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = when (error.type) {
                    ErrorType.AUTH_ERROR -> Color(0xFFFF3B30)
                    else -> Color(0xFFFF9500)
                },
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = when (error.type) {
                    ErrorType.NETWORK_ERROR -> "网络连接失败"
                    ErrorType.AUTH_ERROR -> "认证已过期"
                    ErrorType.SERVER_ERROR -> "服务器错误"
                    ErrorType.RATE_LIMIT -> "请求过于频繁"
                    else -> "发生错误"
                },
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                text = error.message,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            if (onRetry != null) {
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("重试", color = WaterPavilionDeep, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭", color = Color.White.copy(alpha = 0.6f))
            }
        }
    )
}

/**
 * 全屏错误页面
 * 用于页面加载失败时显示
 */
@Composable
fun ErrorPage(
    error: AppError,
    onRetry: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WaterPavilionDeep),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 错误图标
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = when (error.type) {
                    ErrorType.NETWORK_ERROR -> Color(0xFFFF9500)
                    ErrorType.AUTH_ERROR -> Color(0xFFFF3B30)
                    else -> Color(0xFF8E8E93)
                },
                modifier = Modifier.size(80.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 错误标题
            Text(
                text = when (error.type) {
                    ErrorType.NETWORK_ERROR -> "网络不可用"
                    ErrorType.AUTH_ERROR -> "登录已过期"
                    ErrorType.SERVER_ERROR -> "服务器错误"
                    else -> "加载失败"
                },
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 错误描述
            Text(
                text = error.message,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 操作按钮
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (onBack != null) {
                    OutlinedButton(
                        onClick = onBack,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, Color.White.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("返回")
                    }
                }
                
                if (onRetry != null) {
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("重试", color = WaterPavilionDeep, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * 全局错误监听器
 * 在 Composable 中监听全局错误并显示 Toast
 */
@Composable
fun rememberGlobalErrorToast() {
    val context = LocalContext.current
    val error by GlobalErrorHandler.currentError.collectAsState()
    
    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it.message, Toast.LENGTH_LONG).show()
            GlobalErrorHandler.clearError()
        }
    }
}

/**
 * 扩展函数：安全的 API 调用
 * 自动处理异常并上报到全局错误处理器
 */
suspend inline fun <T> safeApiCall(
    crossinline block: suspend () -> T
): Result<T> {
    return try {
        Result.success(block())
    } catch (e: Exception) {
        GlobalErrorHandler.showError(e)
        Result.failure(e)
    }
}
