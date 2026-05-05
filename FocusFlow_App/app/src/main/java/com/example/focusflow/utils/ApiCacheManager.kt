package com.example.focusflow.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * API 缓存管理器 - 静态数据本地缓存策略
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * 【设计理念】
 * - 版本控制：通过版本号判断是否需要更新
 * - ETag/签名：通过数据签名判断内容是否变化
 * - 过期策略：设置缓存过期时间，超时自动更新
 *
 * 【适用场景】
 * - 植物图鉴（plant_dict）
 * - 系统配置（sys_config）
 * - 其他静态参考数据
 */
object ApiCacheManager {

    private const val TAG = "ApiCacheManager"
    private const val PREF_NAME = "api_cache"
    private const val KEY_VERSION_PREFIX = "version_"
    private const val KEY_SIGNATURE_PREFIX = "signature_"
    private const val KEY_TIMESTAMP_PREFIX = "timestamp_"
    
    // 缓存过期时间（毫秒）
    const val CACHE_EXPIRY_PLANT_DICT = 24 * 60 * 60 * 1000L  // 24小时
    const val CACHE_EXPIRY_CONFIG = 1 * 60 * 60 * 1000L       // 1小时
    
    private lateinit var prefs: SharedPreferences
    private var isInitialized = false

    /**
     * 初始化缓存管理器
     */
    fun init(context: Context) {
        if (isInitialized) return
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        isInitialized = true
        Log.d(TAG, "API 缓存管理器初始化完成")
    }

    /**
     * 检查是否需要更新数据
     *
     * @param cacheKey 缓存键名
     * @param serverVersion 服务端版本号（可选）
     * @param serverSignature 服务端数据签名（可选）
     * @param expiryMillis 缓存过期时间
     * @return true 表示需要更新
     */
    fun shouldUpdate(
        cacheKey: String,
        serverVersion: Int? = null,
        serverSignature: String? = null,
        expiryMillis: Long = CACHE_EXPIRY_PLANT_DICT
    ): Boolean {
        // 1. 检查是否有缓存时间戳
        val lastTimestamp = prefs.getLong(KEY_TIMESTAMP_PREFIX + cacheKey, 0L)
        if (lastTimestamp == 0L) {
            Log.d(TAG, "[$cacheKey] 无缓存记录，需要更新")
            return true
        }

        // 2. 检查是否过期
        val now = System.currentTimeMillis()
        if (now - lastTimestamp > expiryMillis) {
            Log.d(TAG, "[$cacheKey] 缓存已过期，需要更新")
            return true
        }

        // 3. 检查版本号
        if (serverVersion != null) {
            val cachedVersion = prefs.getInt(KEY_VERSION_PREFIX + cacheKey, -1)
            if (serverVersion > cachedVersion) {
                Log.d(TAG, "[$cacheKey] 版本更新: $cachedVersion -> $serverVersion")
                return true
            }
        }

        // 4. 检查签名
        if (serverSignature != null) {
            val cachedSignature = prefs.getString(KEY_SIGNATURE_PREFIX + cacheKey, "")
            if (serverSignature != cachedSignature) {
                Log.d(TAG, "[$cacheKey] 数据签名变化，需要更新")
                return true
            }
        }

        Log.v(TAG, "[$cacheKey] 缓存有效，无需更新")
        return false
    }

    /**
     * 更新缓存元数据
     *
     * @param cacheKey 缓存键名
     * @param version 版本号
     * @param signature 数据签名
     */
    fun updateCacheMeta(
        cacheKey: String,
        version: Int = 0,
        signature: String = ""
    ) {
        prefs.edit()
            .putLong(KEY_TIMESTAMP_PREFIX + cacheKey, System.currentTimeMillis())
            .putInt(KEY_VERSION_PREFIX + cacheKey, version)
            .putString(KEY_SIGNATURE_PREFIX + cacheKey, signature)
            .apply()
        
        Log.d(TAG, "[$cacheKey] 缓存元数据已更新: version=$version")
    }

    /**
     * 计算数据签名（MD5）
     */
    fun calculateSignature(data: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(data.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * 清除指定缓存
     */
    fun clearCache(cacheKey: String) {
        prefs.edit()
            .remove(KEY_TIMESTAMP_PREFIX + cacheKey)
            .remove(KEY_VERSION_PREFIX + cacheKey)
            .remove(KEY_SIGNATURE_PREFIX + cacheKey)
            .apply()
        Log.d(TAG, "[$cacheKey] 缓存已清除")
    }

    /**
     * 清除所有缓存
     */
    fun clearAllCache() {
        prefs.edit().clear().apply()
        Log.d(TAG, "所有缓存已清除")
    }

    /**
     * 获取缓存时间戳
     */
    fun getCacheTimestamp(cacheKey: String): Long {
        return prefs.getLong(KEY_TIMESTAMP_PREFIX + cacheKey, 0L)
    }

    /**
     * 获取缓存统计
     */
    fun getCacheStats(): Map<String, CacheInfo> {
        val stats = mutableMapOf<String, CacheInfo>()
        val allEntries = prefs.all
        
        allEntries.keys
            .filter { it.startsWith(KEY_TIMESTAMP_PREFIX) }
            .forEach { key ->
                val cacheKey = key.removePrefix(KEY_TIMESTAMP_PREFIX)
                val timestamp = prefs.getLong(key, 0L)
                val version = prefs.getInt(KEY_VERSION_PREFIX + cacheKey, 0)
                val signature = prefs.getString(KEY_SIGNATURE_PREFIX + cacheKey, "")
                
                stats[cacheKey] = CacheInfo(
                    key = cacheKey,
                    timestamp = timestamp,
                    version = version,
                    signature = signature ?: ""
                )
            }
        
        return stats
    }

    /**
     * 缓存信息
     */
    data class CacheInfo(
        val key: String,
        val timestamp: Long,
        val version: Int,
        val signature: String
    ) {
        val ageMillis: Long
            get() = System.currentTimeMillis() - timestamp
        
        val ageFormatted: String
            get() {
                val hours = ageMillis / (60 * 60 * 1000)
                return if (hours > 0) "${hours}小时前" else "刚刚"
            }
        
        override fun toString(): String {
            return "[$key] 版本: $version, 缓存时间: $ageFormatted"
        }
    }
}

/**
 * 扩展函数：安全的缓存更新
 */
suspend inline fun <T> withCache(
    cacheKey: String,
    crossinline shouldUpdate: () -> Boolean,
    crossinline update: suspend () -> T
): T {
    return if (shouldUpdate()) {
        withContext(Dispatchers.IO) {
            update()
        }
    } else {
        // 使用缓存数据（由调用方从本地数据库读取）
        throw CacheValidException("缓存有效，无需更新")
    }
}

/**
 * 缓存有效异常（用于跳过网络请求）
 */
class CacheValidException(message: String) : Exception(message)