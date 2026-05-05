package com.example.focusflow.data

import com.example.focusflow.api.ConfigService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * APP 配置管理器
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * 【职责】
 * 1. 从服务端获取配置
 * 2. 缓存配置到内存
 * 3. 提供类型安全的配置访问方法
 *
 * 【配置键】
 * - streak.bonus.{n}: 连续专注奖励光流
 * - focus.reward.per.minute: 每分钟专注奖励
 * - focus.drop.min.minutes: 获得种子最低专注时长
 * - focus.drop.base.rate: 获得种子基础概率
 * - charge.cost: 充能消耗光流
 */
object AppConfigManager {

    private val mutex = Mutex()
    private var configCache: Map<String, String> = emptyMap()
    private var isLoaded = false

    // ════════════════════════════════════════════════════════════════════════
    // 配置键常量（与服务端 SystemConfig 保持一致）
    // ════════════════════════════════════════════════════════════════════════

    object Keys {
        const val STREAK_BONUS_3 = "streak.bonus.3"
        const val STREAK_BONUS_5 = "streak.bonus.5"
        const val STREAK_BONUS_7 = "streak.bonus.7"
        const val STREAK_BONUS_14 = "streak.bonus.14"
        const val STREAK_BONUS_21 = "streak.bonus.21"
        const val STREAK_BONUS_30 = "streak.bonus.30"
        const val FOCUS_REWARD_PER_MINUTE = "focus.reward.per.minute"
        const val FOCUS_DROP_MIN_MINUTES = "focus.drop.min.minutes"
        const val FOCUS_DROP_BASE_RATE = "focus.drop.base.rate"
        const val CHARGE_COST = "charge.cost"
    }

    // ════════════════════════════════════════════════════════════════════════
    // 加载配置
    // ════════════════════════════════════════════════════════════════════════

    /**
     * 从服务端加载配置
     * 建议在 Application 启动时调用
     */
    suspend fun loadConfigs(configService: ConfigService): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("AppConfigManager", "正在请求配置API...")
                val response = configService.getAllConfigs()
                android.util.Log.d("AppConfigManager", "配置API响应: code=${response.code}, data=${response.data}")
                
                if (response.code == 200 && response.data != null) {
                    mutex.withLock {
                        configCache = response.data
                        isLoaded = true
                    }
                    android.util.Log.d("AppConfigManager", "配置加载成功，缓存更新: ${configCache.size}项")
                    android.util.Log.d("AppConfigManager", "缓存内容: $configCache")
                    true
                } else {
                    android.util.Log.w("AppConfigManager", "配置API返回非200或data为空")
                    false
                }
            } catch (e: Exception) {
                android.util.Log.e("AppConfigManager", "加载配置失败", e)
                false
            }
        }
    }

    /**
     * 重新加载配置（可用于手动刷新）
     */
    suspend fun reload(configService: ConfigService): Boolean {
        android.util.Log.d("AppConfigManager", "====== 开始刷新配置 ======")
        val success = loadConfigs(configService)
        if (success) {
            android.util.Log.d("AppConfigManager", "====== 配置刷新成功 ======")
            android.util.Log.d("AppConfigManager", "种子掉落概率: ${configCache[Keys.FOCUS_DROP_BASE_RATE]}")
        } else {
            android.util.Log.w("AppConfigManager", "====== 配置刷新失败 ======")
        }
        return success
    }

    // ════════════════════════════════════════════════════════════════════════
    // 配置访问方法
    // ════════════════════════════════════════════════════════════════════════

    /**
     * 获取字符串配置
     */
    fun getString(key: String, defaultValue: String = ""): String {
        return configCache[key] ?: defaultValue
    }

    /**
     * 获取整数配置
     */
    fun getInt(key: String, defaultValue: Int = 0): Int {
        return try {
            configCache[key]?.toInt() ?: defaultValue
        } catch (e: NumberFormatException) {
            defaultValue
        }
    }

    /**
     * 获取浮点配置
     */
    fun getFloat(key: String, defaultValue: Float = 0f): Float {
        return try {
            configCache[key]?.toFloat() ?: defaultValue
        } catch (e: NumberFormatException) {
            defaultValue
        }
    }

    /**
     * 获取双精度配置
     */
    fun getDouble(key: String, defaultValue: Double = 0.0): Double {
        return try {
            configCache[key]?.toDouble() ?: defaultValue
        } catch (e: NumberFormatException) {
            defaultValue
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 便捷访问方法 - 业务配置
    // ════════════════════════════════════════════════════════════════════════

    /** 连续专注N天奖励光流 */
    fun getStreakBonus(days: Int): Int {
        return when (days) {
            3 -> getInt(Keys.STREAK_BONUS_3, 10)
            5 -> getInt(Keys.STREAK_BONUS_5, 25)
            7 -> getInt(Keys.STREAK_BONUS_7, 50)
            14 -> getInt(Keys.STREAK_BONUS_14, 100)
            21 -> getInt(Keys.STREAK_BONUS_21, 200)
            30 -> getInt(Keys.STREAK_BONUS_30, 500)
            else -> 0
        }
    }

    /** 每分钟专注奖励光流 */
    fun getFocusRewardPerMinute(): Int {
        return getInt(Keys.FOCUS_REWARD_PER_MINUTE, 1)
    }

    /** 获得种子最低专注时长（分钟） */
    fun getSeedDropMinMinutes(): Int {
        return getInt(Keys.FOCUS_DROP_MIN_MINUTES, 1)
    }

    /** 获得种子基础概率 */
    fun getSeedDropBaseRate(): Double {
        val rate = getDouble(Keys.FOCUS_DROP_BASE_RATE, 0.1)  // 默认 10%，避免配置未加载时概率过高
        android.util.Log.d("AppConfigManager", "种子掉落概率: $rate (配置加载状态: $isLoaded)")
        return rate
    }

    /** 充能消耗光流 */
    fun getChargeCost(): Int {
        return getInt(Keys.CHARGE_COST, 50)
    }

    /**
     * 是否已加载配置
     */
    fun isConfigLoaded(): Boolean = isLoaded

    /**
     * 获取所有配置（用于调试）
     */
    fun getAllConfigs(): Map<String, String> = configCache.toMap()
}
