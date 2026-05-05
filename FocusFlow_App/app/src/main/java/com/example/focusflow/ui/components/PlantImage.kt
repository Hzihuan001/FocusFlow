package com.example.focusflow.ui.components

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import com.example.focusflow.api.RetrofitClient
import com.example.focusflow.ui.theme.CyberPrimary
import com.example.focusflow.ui.theme.CyberTextSub

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * 植物图片组件 - 支持网络图片加载与缓存
 * ═══════════════════════════════════════════════════════════════════════════════
 */

/**
 * 将相对路径转换为完整URL
 */
private fun resolveImageUrl(imageUrl: String?): String? {
    if (imageUrl.isNullOrBlank()) return null
    
    // 已经是完整URL
    if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
        return imageUrl
    }
    
    // 相对路径，拼接baseURL
    val baseUrl = RetrofitClient.GATEWAY_BASE_URL
    // 移除baseURL末尾的斜杠和imageUrl开头的斜杠，避免重复
    val cleanBaseUrl = baseUrl.trimEnd('/')
    val cleanPath = imageUrl.trimStart('/')
    
    return "$cleanBaseUrl/$cleanPath"
}

/**
 * 植物图片组件
 */
@Composable
fun PlantImage(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    tint: Color? = null,
    contentScale: ContentScale = ContentScale.Fit
) {
    val context = LocalContext.current
    val resolvedUrl = resolveImageUrl(imageUrl)
    
    if (resolvedUrl == null) {
        DefaultPlantIcon(
            modifier = modifier.size(size),
            tint = tint ?: CyberPrimary
        )
    } else {
        val request = ImageRequest.Builder(context)
            .data(resolvedUrl)
            .memoryCacheKey(resolvedUrl)
            .diskCacheKey(resolvedUrl)
            .crossfade(true)
            .crossfade(300)
            .build()
        
        Box(
            modifier = modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            var isLoading by remember { mutableStateOf(true) }
            var hasError by remember { mutableStateOf(false) }
            
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(size * 0.4f),
                    color = CyberPrimary.copy(alpha = 0.6f),
                    strokeWidth = 2.dp
                )
            }
            
            if (hasError) {
                DefaultPlantIcon(
                    modifier = Modifier.size(size),
                    tint = tint ?: CyberTextSub
                )
            } else {
                AsyncImage(
                    model = request,
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale,
                    colorFilter = tint?.let { ColorFilter.tint(it) },
                    onState = { state ->
                        when (state) {
                            is AsyncImagePainter.State.Loading -> {
                                isLoading = true
                                hasError = false
                            }
                            is AsyncImagePainter.State.Success -> {
                                isLoading = false
                                hasError = false
                            }
                            is AsyncImagePainter.State.Error -> {
                                isLoading = false
                                hasError = true
                                Log.w("PlantImage", "图片加载失败: $resolvedUrl, error: ${state.result.throwable}")
                            }
                            else -> {
                                isLoading = false
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun DefaultPlantIcon(
    modifier: Modifier,
    tint: Color
) {
    Icon(
        imageVector = Icons.Rounded.Eco,
        contentDescription = "植物",
        modifier = modifier,
        tint = tint
    )
}

@Composable
fun PlantImageSmall(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color? = null
) {
    PlantImage(
        imageUrl = imageUrl,
        contentDescription = contentDescription,
        modifier = modifier,
        size = 32.dp,
        tint = tint
    )
}

@Composable
fun PlantImageMedium(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color? = null
) {
    PlantImage(
        imageUrl = imageUrl,
        contentDescription = contentDescription,
        modifier = modifier,
        size = 64.dp,
        tint = tint
    )
}

@Composable
fun PlantImageLarge(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color? = null
) {
    PlantImage(
        imageUrl = imageUrl,
        contentDescription = contentDescription,
        modifier = modifier,
        size = 128.dp,
        tint = tint
    )
}
