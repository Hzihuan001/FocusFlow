package com.example.focusflow.utils

import android.content.Context
import android.util.Log
import coil.ImageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * 植物图片加载器 - 预加载与缓存策略
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * 【核心功能】
 * 1. 内存缓存：Coil 自动管理，默认 1/4 可用内存
 * 2. 磁盘缓存：Coil 自动管理，持久化存储
 * 3. 预加载：启动时预加载所有植物图片
 * 4. 网络图片：支持服务端动态图片URL
 *
 * 【缓存策略】
 * - 内存缓存：Coil 自动 LRU 管理
 * - 磁盘缓存：持久化图片，离线可用
 * - 预加载：APP启动时预加载所有上架植物图片
 */
object PlantImageLoader {

    private const val TAG = "PlantImageLoader"

    private lateinit var imageLoader: ImageLoader
    private var isInitialized = false
    private val preloadedUrls = mutableSetOf<String>()

    /**
     * 初始化图片加载器
     * 应在 Application.onCreate() 中调用
     */
    fun init(context: Context) {
        if (isInitialized) return

        // 创建 Coil ImageLoader，使用默认缓存配置
        imageLoader = ImageLoader.Builder(context)
            .crossfade(true)
            .crossfade(300)
            .build()

        isInitialized = true
        Log.d(TAG, "植物图片加载器初始化完成")
    }

    /**
     * 预加载植物图片
     * 在后台线程执行，不阻塞主线程
     *
     * @param context 上下文
     * @param imageUrls 需要预加载的图片URL列表
     */
    suspend fun preloadImages(context: Context, imageUrls: List<String>) {
        if (!isInitialized) {
            init(context)
        }

        withContext(Dispatchers.IO) {
            Log.d(TAG, "开始预加载 ${imageUrls.size} 张植物图片...")

            imageUrls.filter { it.isNotEmpty() && !preloadedUrls.contains(it) }
                .forEach { url ->
                    try {
                        val request = ImageRequest.Builder(context)
                            .data(url)
                            .memoryCacheKey(url)
                            .diskCacheKey(url)
                            .build()

                        imageLoader.execute(request)
                        preloadedUrls.add(url)
                        Log.v(TAG, "预加载成功: $url")
                    } catch (e: Exception) {
                        Log.w(TAG, "预加载失败: $url, 原因: ${e.message}")
                    }
                }

            Log.d(TAG, "预加载完成，已缓存 ${preloadedUrls.size} 张图片")
        }
    }

    /**
     * 获取 ImageLoader 实例
     * 供 AsyncImage 组件使用
     */
    fun getImageLoader(context: Context): ImageLoader {
        if (!isInitialized) {
            init(context)
        }
        return imageLoader
    }

    /**
     * 检查图片是否已缓存
     */
    fun isCached(url: String): Boolean {
        return preloadedUrls.contains(url)
    }

    /**
     * 获取缓存统计信息
     */
    fun getCacheStats(): CacheStats {
        return CacheStats(
            preloadedCount = preloadedUrls.size
        )
    }

    /**
     * 清除缓存记录
     */
    fun clearCache() {
        preloadedUrls.clear()
        Log.d(TAG, "已清除预加载记录")
    }

    /**
     * 缓存统计信息
     */
    data class CacheStats(
        val preloadedCount: Int
    ) {
        override fun toString(): String {
            return "已预加载图片: $preloadedCount 张"
        }
    }
}