package com.example.focusflow.ui.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

// ═══════════════════════════════════════════════════════════════
// 主题颜色集合
// ═══════════════════════════════════════════════════════════════

data class AppColors(
    // 主色调
    val primary: Color,
    val primaryDim: Color,
    // 背景
    val bgDeep: Color,
    val cardBg: Color,
    // 文字
    val textMain: Color,
    val textSub: Color,
    // 状态栏图标是否为深色（浅色主题时为 true）
    val isLightStatusBar: Boolean,
    // 主题标识
    val isDark: Boolean
)

// ── 深色主题（赛博朋克，原版）──
val DarkAppColors = AppColors(
    primary          = Color(0xFF00FF9D),
    primaryDim       = Color(0x2600FF9D),
    bgDeep           = Color(0xFF050807),
    cardBg           = Color(0xFF0F1412),
    textMain         = Color(0xFFFFFFFF),
    textSub          = Color(0xFF6E8578),
    isLightStatusBar = false,
    isDark           = true
)

// ── 浅色主题（优化版：蓝色主题，更有活力）──
val LightAppColors = AppColors(
    primary          = Color(0xFF2196F3),   // 蓝色主色调（Material Design Blue）
    primaryDim       = Color(0x1A2196F3),   // 10% 透明度蓝色
    bgDeep           = Color(0xFFFAFAFA),   // 极浅灰背景（更柔和）
    cardBg           = Color(0xFFFFFFFF),   // 纯白卡片
    textMain         = Color(0xFF1A1A1A),   // 深灰主文字（比纯黑柔和）
    textSub          = Color(0xFF757575),   // 中灰副文字
    isLightStatusBar = true,
    isDark           = false
)

// ═══════════════════════════════════════════════════════════════
// CompositionLocal：全局注入当前主题颜色
// ═══════════════════════════════════════════════════════════════

val LocalAppColors = compositionLocalOf { DarkAppColors }

// ═══════════════════════════════════════════════════════════════
// ThemeManager：持久化主题选择（升级为 DataStore）
// ═══════════════════════════════════════════════════════════════

// DataStore 扩展属性
private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")

object PreferencesKeys {
    val THEME_MODE = intPreferencesKey("theme_mode")
}

object ThemeManager {
    private const val PREFS_NAME = "app_theme"
    private const val KEY_IS_LIGHT = "is_light_theme"

    private lateinit var dataStore: DataStore<Preferences>
    
    /** 主题切换回调，由 MainActivity 注册 */
    var onThemeChange: ((ThemeMode) -> Unit)? = null
    
    /**
     * 初始化 DataStore
     * 必须在使用其他方法前调用
     */
    fun initialize(context: Context) {
        dataStore = context.themeDataStore
    }
    
    /**
     * 主题模式响应式流
     * 订阅此 Flow 可实时获取主题模式变化
     */
    val themeModeFlow: Flow<ThemeMode>
        get() {
            require(::dataStore.isInitialized) { "ThemeManager must be initialized before use" }
            return dataStore.data.map { preferences ->
                val ordinal = preferences[PreferencesKeys.THEME_MODE] ?: 0
                ThemeMode.fromOrdinal(ordinal)
            }
        }
    
    /**
     * 读取当前主题模式（同步，用于初始化）
     */
    suspend fun getThemeMode(): ThemeMode {
        require(::dataStore.isInitialized) { "ThemeManager must be initialized before use" }
        return try {
            val preferences = dataStore.data.first()
            val ordinal = preferences[PreferencesKeys.THEME_MODE] ?: 0
            ThemeMode.fromOrdinal(ordinal)
        } catch (e: Exception) {
            android.util.Log.e("ThemeManager", "Failed to read theme mode", e)
            ThemeMode.FOLLOW_SYSTEM
        }
    }
    
    /**
     * 设置主题模式（异步持久化）
     */
    suspend fun setThemeMode(mode: ThemeMode, context: Context? = null) {
        require(::dataStore.isInitialized) { "ThemeManager must be initialized before use" }
        try {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.THEME_MODE] = mode.ordinal
            }
            // 在主线程触发回调
            withContext(Dispatchers.Main) {
                onThemeChange?.invoke(mode)
                
                // 发送主题变化广播（通知 LockOverlayService）
                context?.let { ctx ->
                    val intent = android.content.Intent("com.example.focusflow.action.THEME_CHANGED")
                    ctx.sendBroadcast(intent)
                    android.util.Log.d("ThemeManager", "主题变化广播已发送")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ThemeManager", "Failed to save theme mode", e)
            // 重试一次
            try {
                dataStore.edit { preferences ->
                    preferences[PreferencesKeys.THEME_MODE] = mode.ordinal
                }
            } catch (retryException: Exception) {
                android.util.Log.e("ThemeManager", "Retry failed to save theme mode", retryException)
            }
        }
    }
    
    /**
     * 从旧版 SharedPreferences 迁移数据
     * 仅在首次运行时调用一次
     */
    suspend fun migrateFromSharedPreferences(context: Context) {
        require(::dataStore.isInitialized) { "ThemeManager must be initialized before use" }
        
        // 检查是否已迁移
        val currentMode = getThemeMode()
        val preferences = dataStore.data.first()
        val hasMigrated = preferences[PreferencesKeys.THEME_MODE] != null
        
        if (hasMigrated) {
            return // 已迁移，跳过
        }
        
        // 读取旧的 SharedPreferences
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!sharedPrefs.contains(KEY_IS_LIGHT)) {
            // 旧数据不存在，使用默认值
            setThemeMode(ThemeMode.FOLLOW_SYSTEM)
            return
        }
        
        val isLight = sharedPrefs.getBoolean(KEY_IS_LIGHT, false)
        val migratedMode = if (isLight) ThemeMode.FORCE_LIGHT else ThemeMode.FORCE_DARK
        
        // 保存到 DataStore
        setThemeMode(migratedMode)
        
        // 删除旧数据
        sharedPrefs.edit().remove(KEY_IS_LIGHT).apply()
        
        android.util.Log.d("ThemeManager", "Migrated theme from SharedPreferences: $migratedMode")
    }
    
    /**
     * 同步获取当前主题颜色（用于 Service 等非 Compose 环境）
     * 
     * 此方法会阻塞调用线程读取 DataStore，不应在主线程调用
     * 如果读取失败，返回默认的深色主题
     * 
     * @param context Android Context
     * @return 当前主题的 AppColors 实例
     */
    fun getCurrentColors(context: Context): AppColors {
        require(::dataStore.isInitialized) { "ThemeManager must be initialized before use" }
        
        return try {
            // 使用 runBlocking 同步读取（仅用于 Service）
            val preferences = kotlinx.coroutines.runBlocking {
                dataStore.data.first()
            }
            val ordinal = preferences[PreferencesKeys.THEME_MODE] ?: 0
            val themeMode = ThemeMode.fromOrdinal(ordinal)
            
            // 根据主题模式返回对应颜色
            when (themeMode) {
                ThemeMode.FOLLOW_SYSTEM -> {
                    // 检查系统是否为深色模式
                    val isSystemDark = (context.resources.configuration.uiMode and 
                        android.content.res.Configuration.UI_MODE_NIGHT_MASK) == 
                        android.content.res.Configuration.UI_MODE_NIGHT_YES
                    if (isSystemDark) DarkAppColors else LightAppColors
                }
                ThemeMode.FORCE_DARK -> DarkAppColors
                ThemeMode.FORCE_LIGHT -> LightAppColors
            }
        } catch (e: Exception) {
            android.util.Log.e("ThemeManager", "Failed to get current colors, using dark theme", e)
            DarkAppColors
        }
    }
    
    /**
     * 获取指定颜色类型的 Android Color Int（用于传统 View 系统）
     * 
     * @param context Android Context
     * @param colorType 颜色类型: "primary", "bgDeep", "cardBg", "textMain", "textSub"
     * @return Android Color Int (0xAARRGGBB)
     */
    fun getColorInt(context: Context, colorType: String): Int {
        val appColors = getCurrentColors(context)
        val color = when (colorType) {
            "primary" -> appColors.primary
            "bgDeep" -> appColors.bgDeep
            "cardBg" -> appColors.cardBg
            "textMain" -> appColors.textMain
            "textSub" -> appColors.textSub
            else -> {
                android.util.Log.w("ThemeManager", "Unknown color type: $colorType, using primary")
                appColors.primary
            }
        }
        
        // 将 Compose Color 转换为 Android Color Int
        return android.graphics.Color.argb(
            (color.alpha * 255).toInt(),
            (color.red * 255).toInt(),
            (color.green * 255).toInt(),
            (color.blue * 255).toInt()
        )
    }

    // ── 兼容旧代码的方法（已废弃，保留用于过渡期）──
    @Deprecated("Use themeModeFlow or getThemeMode() instead")
    fun isLightTheme(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_IS_LIGHT, false)

    @Deprecated("Use setThemeMode() instead")
    fun setLightTheme(context: Context, isLight: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_IS_LIGHT, isLight)
            .apply()
    }
}

// ═══════════════════════════════════════════════════════════════
// AppThemeProvider：包裹整个 App，提供主题颜色（支持三模式）
// ═══════════════════════════════════════════════════════════════

@Composable
fun AppThemeProvider(
    themeMode: ThemeMode,
    content: @Composable () -> Unit
) {
    // 计算有效主题
    val colors = when (themeMode) {
        ThemeMode.FOLLOW_SYSTEM -> {
            val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
            if (isSystemDark) DarkAppColors else LightAppColors
        }
        ThemeMode.FORCE_DARK -> DarkAppColors
        ThemeMode.FORCE_LIGHT -> LightAppColors
    }
    
    CompositionLocalProvider(LocalAppColors provides colors) {
        content()
    }
}
