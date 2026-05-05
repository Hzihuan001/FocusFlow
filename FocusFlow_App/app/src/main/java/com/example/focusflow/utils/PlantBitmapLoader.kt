package com.example.focusflow.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import com.example.focusflow.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * 植物贴图缓存池 - 网络图片加载 + LRU 缓存
 * 
 * 【重构说明】
 * 植物图片统一从后端服务器加载，不再使用 app res 本地资源。
 * 后端图片存储位置: FocusFlow_Server/uploads/plants/
 * 
 * 【性能优化要点】
 * 1. 双层缓存：Bitmap 层（用于底部空白计算）+ ImageBitmap 层（用于渲染）
 * 2. Coil 自动管理内存和磁盘缓存
 * 3. 预加载机制：启动时加载所有植物图片到缓存
 * ═══════════════════════════════════════════════════════════════════════════════
 */
object PlantBitmapLoader {

    private const val TAG = "PlantBitmapLoader"
    
    // 目标尺寸：植物在花园中的最大显示尺寸
    private const val TARGET_MAX_WIDTH = 280
    private const val TARGET_MAX_HEIGHT = 400

    // ═══════════════════════════════════════════════════════════════════════════
    // 双层缓存：Bitmap 层（用于底部空白计算）+ ImageBitmap 层（用于渲染）
    // ═══════════════════════════════════════════════════════════════════════════
    private var bitmapCache: LruCache<String, Bitmap>? = null      // 原始 Bitmap 缓存
    private var imageBitmapCache: LruCache<String, ImageBitmap>? = null  // ImageBitmap 缓存（渲染用）
    
    @Volatile
    private var isInitialized = false
    private val initLock = Any()
    private var appContext: Context? = null
    private var coilImageLoader: ImageLoader? = null

    /**
     * 初始化双层 LRU 缓存（线程安全）
     */
    fun init(context: Context) {
        if (isInitialized) return
        
        synchronized(initLock) {
            if (isInitialized) return
            
            appContext = context.applicationContext

            // 计算可用内存，分配 1/16 作为图片缓存
            val maxMemory = Runtime.getRuntime().maxMemory() / 1024
            val cacheSize = (maxMemory / 16).toInt()

            // Bitmap 缓存（用于底部空白计算等）
            bitmapCache = object : LruCache<String, Bitmap>(cacheSize) {
                override fun sizeOf(key: String, value: Bitmap): Int {
                    return value.byteCount / 1024
                }
            }

            // ImageBitmap 缓存（渲染专用，计数型）
            imageBitmapCache = LruCache<String, ImageBitmap>(30)  // 最多缓存 30 张

            // 初始化 Coil ImageLoader
            coilImageLoader = ImageLoader.Builder(context)
                .crossfade(true)
                .crossfade(300)
                .build()

            isInitialized = true
            Log.d(TAG, "植物贴图缓存池初始化完成，内存缓存: ${cacheSize}KB，ImageBitmap缓存: 30张")
        }
    }

    /**
     * ═══════════════════════════════════════════════════════════════════════════
     * 核心方法：从网络 URL 加载 ImageBitmap（渲染专用）
     * ═══════════════════════════════════════════════════════════════════════════
     */
    fun load(imageUrl: String?, context: Context? = null): ImageBitmap? {
        if (imageUrl.isNullOrBlank()) return null
        
        // 确保 URL 是完整的
        val fullUrl = resolveImageUrl(imageUrl)
        
        // 确保初始化
        val ctx = context ?: appContext
        if (!isInitialized && ctx != null) {
            init(ctx)
        }
        
        // 优先从 ImageBitmap 缓存获取
        imageBitmapCache?.get(fullUrl)?.let { 
            return it  // 直接返回缓存的对象，无需转换
        }

        // 尝试从 Bitmap 缓存转换
        bitmapCache?.get(fullUrl)?.let { bitmap ->
            val imageBitmap = bitmap.asImageBitmap()
            imageBitmapCache?.put(fullUrl, imageBitmap)
            return imageBitmap
        }
        
        Log.w(TAG, "ImageBitmap 缓存未命中: $fullUrl，请先预加载")
        return null
    }

    /**
     * 获取原始 Bitmap（用于底部空白计算等）
     */
    fun loadBitmap(imageUrl: String?, context: Context? = null): Bitmap? {
        if (imageUrl.isNullOrBlank()) return null
        
        val fullUrl = resolveImageUrl(imageUrl)
        
        val ctx = context ?: appContext
        if (!isInitialized && ctx != null) {
            init(ctx)
        }

        // 从 Bitmap 缓存获取
        return bitmapCache?.get(fullUrl)
    }

    /**
     * ═══════════════════════════════════════════════════════════════════════════
     * 预加载植物图片（从网络 URL）
     * 
     * @param imageUrls 需要预加载的图片 URL 列表
     * @param context 上下文
     ═══════════════════════════════════════════════════════════════════════════
     */
    suspend fun preload(imageUrls: List<String>, context: Context) {
        if (!isInitialized) {
            init(context)
        }
        
        val urls = imageUrls.filter { it.isNotEmpty() }.map { resolveImageUrl(it) }.distinct()
        
        if (urls.isEmpty()) {
            Log.d(TAG, "没有需要预加载的植物图片")
            return
        }
        
        Log.d(TAG, "开始预加载 ${urls.size} 张植物图片...")
        
        var loaded = 0
        urls.forEach { url ->
            try {
                val bitmap = loadBitmapFromNetwork(url, context)
                if (bitmap != null) {
                    // 缓存 Bitmap
                    bitmapCache?.put(url, bitmap)
                    loaded++
                    Log.v(TAG, "预加载成功: $url")
                }
            } catch (e: Exception) {
                Log.w(TAG, "预加载失败: $url, 原因: ${e.message}")
            }
        }
        
        Log.d(TAG, "预加载完成: $loaded/${urls.size}，Bitmap缓存: ${bitmapCache?.size() ?: 0}张")
    }

    /**
     * 从网络加载 Bitmap
     * 注意：使用 ARGB_8888 配置，避免 HARDWARE bitmap 导致 getPixel() 崩溃
     */
    private suspend fun loadBitmapFromNetwork(url: String, context: Context): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val loader = coilImageLoader ?: run {
                    Log.e(TAG, "ImageLoader 未初始化")
                    return@withContext null
                }
                
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .memoryCacheKey(url)
                    .diskCacheKey(url)
                    // 🔧 强制使用 ARGB_8888 配置，避免 HARDWARE bitmap
                    .bitmapConfig(Bitmap.Config.ARGB_8888)
                    .build()

                val result = loader.execute(request)
                val bitmap = (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                
                // 双重保险：如果是 HARDWARE bitmap，复制为软件 bitmap
                if (bitmap != null && bitmap.config == Bitmap.Config.HARDWARE) {
                    Log.d(TAG, "检测到 HARDWARE bitmap，转换为 ARGB_8888")
                    bitmap.copy(Bitmap.Config.ARGB_8888, false)
                } else {
                    bitmap
                }
            } catch (e: Exception) {
                Log.e(TAG, "网络加载图片失败: $url", e)
                null
            }
        }
    }

    /**
     * 将相对路径转换为完整 URL
     */
    private fun resolveImageUrl(imageUrl: String): String {
        if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
            return imageUrl
        }
        
        // 相对路径，拼接 baseURL
        val baseUrl = RetrofitClient.GATEWAY_BASE_URL
        val cleanBaseUrl = baseUrl.trimEnd('/')
        var cleanPath = imageUrl.trimStart('/')
        
        // 处理路径重复：如果 baseUrl 以 /api 结尾，且 path 以 api/ 开头，去除重复
        if (cleanBaseUrl.endsWith("/api") && cleanPath.startsWith("api/")) {
            cleanPath = cleanPath.removePrefix("api/")
        }
        
        return "$cleanBaseUrl/$cleanPath"
    }

    /**
     * 清除缓存
     */
    fun clearCache() {
        bitmapCache?.evictAll()
        imageBitmapCache?.evictAll()
        Log.d(TAG, "已清除所有缓存")
    }
}