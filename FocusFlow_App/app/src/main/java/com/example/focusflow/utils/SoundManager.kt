package com.example.focusflow.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.media.MediaPlayer
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.ConcurrentHashMap

/**
 * 音效管理器
 * 
 * 【功能】
 * 1. 管理应用内所有音效播放
 * 2. 支持音效开关控制
 * 3. 支持音量调节
 * 4. 预加载音效避免延迟
 * 
 * 【音效类型】
 * - UI 点击音效
 * - 专注完成音效
 * - 种植音效
 * - 充能音效
 * - 获得奖励音效
 * - 成就解锁音效
 * 
 * 【使用说明】
 * 1. 在 res/raw/ 目录放置音效文件（OGG 格式推荐）
 * 2. 文件命名：sound_xxx.ogg
 * 3. SoundManager 会自动扫描并加载
 */
object SoundManager {

    private val Context.soundPrefs: DataStore<Preferences> by preferencesDataStore(name = "sound_settings")

    private val KEY_SOUND_ENABLED = booleanPreferencesKey("sound_enabled")

    // ───────────── 音效文件名 ─────────────

    private val SOUND_FILES = mapOf(
        "click" to "sound_click",
        "focus_complete" to "sound_focus_complete",
        "plant" to "sound_plant",
        "charge" to "sound_charge",
        "reward" to "sound_reward",
        "achievement" to "sound_achievement",
        "error" to "sound_error",
        "harvest" to "sound_harvest"
    )

    // ───────────── 音效池 ─────────────

    private var soundPool: SoundPool? = null
    private val soundIds = ConcurrentHashMap<String, Int>() // name -> soundId
    private var isInitialized = false
    private var context: Context? = null

    // ───────────── 状态 ─────────────

    private var isSoundEnabled = true
    
    // 防止重复播放的时间戳记录
    private val lastPlayTime = ConcurrentHashMap<String, Long>()
    private const val MIN_PLAY_INTERVAL = 300L // 最小播放间隔（毫秒）

    /**
     * 初始化音效管理器
     * 应在 Application.onCreate() 中调用
     */
    fun init(ctx: Context) {
        if (isInitialized) return
        
        context = ctx.applicationContext
        
        // 创建 SoundPool
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()

        // 预加载音效
        preloadSounds()

        isInitialized = true
    }

    /**
     * 预加载所有音效
     */
    private fun preloadSounds() {
        val ctx = context ?: return

        SOUND_FILES.forEach { (key, fileName) ->
            try {
                val resId = ctx.resources.getIdentifier(fileName, "raw", ctx.packageName)
                if (resId != 0) {
                    val soundId = soundPool?.load(ctx, resId, 1)
                    if (soundId != null && soundId != 0) {
                        soundIds[key] = soundId
                        android.util.Log.d("SoundManager", "✅ 音效加载成功: $fileName -> soundId=$soundId")
                    }
                } else {
                    android.util.Log.w("SoundManager", "❌ 音效文件不存在: $fileName")
                }
            } catch (e: Exception) {
                android.util.Log.e("SoundManager", "❌ 音效加载失败: $fileName, 错误: ${e.message}")
            }
        }
        
        android.util.Log.d("SoundManager", "音效预加载完成，共加载 ${soundIds.size} 个音效")
    }

    // ───────────── 设置管理 ─────────────

    /**
     * 获取音效开关状态
     */
    fun isSoundEnabledFlow(ctx: Context): Flow<Boolean> {
        return ctx.soundPrefs.data.map { it[KEY_SOUND_ENABLED] ?: true }
    }

    /**
     * 设置音效开关
     */
    suspend fun setSoundEnabled(ctx: Context, enabled: Boolean) {
        isSoundEnabled = enabled
        ctx.soundPrefs.edit { it[KEY_SOUND_ENABLED] = enabled }
    }

    /**
     * 快速获取音效开关（非协程）
     */
    fun isSoundEnabled(): Boolean = isSoundEnabled

    /**
     * 从存储加载设置
     * 优先使用 UserPreferences（与其他设置保持一致）
     */
    suspend fun loadSettings(ctx: Context) {
        // 优先从 UserPreferences 读取（同步方式）
        isSoundEnabled = com.example.focusflow.api.UserPreferences.isSoundEnabled(ctx)
        
        // 同时保存到 DataStore（备用）
        ctx.soundPrefs.edit { it[KEY_SOUND_ENABLED] = isSoundEnabled }
    }

    // ───────────── 音效播放 ─────────────

    /**
     * 播放点击音效
     */
    fun playClick() {
        playSound("click")
    }

    /**
     * 播放专注完成音效
     */
    fun playFocusComplete() {
        playSound("focus_complete")
    }

    /**
     * 播放种植音效
     */
    fun playPlant() {
        playSound("plant")
    }

    /**
     * 播放充能音效
     */
    fun playCharge() {
        playSound("charge")
    }

    /**
     * 播放获得奖励音效
     */
    fun playReward() {
        playSound("reward")
    }

    /**
     * 播放成就解锁音效
     */
    fun playAchievement() {
        playSound("achievement")
    }

    /**
     * 播放错误音效
     */
    fun playError() {
        playSound("error")
    }

    /**
     * 播放收获音效
     */
    fun playHarvest() {
        playSound("harvest")
    }

    /**
     * 播放指定音效
     * @param name 音效名称
     * @param volume 音量 (0.0 - 1.0)
     */
    fun playSound(name: String, volume: Float = 1.0f) {
        android.util.Log.d("SoundManager", "尝试播放音效: $name, 启用=$isSoundEnabled, 初始化=$isInitialized")
        
        if (!isSoundEnabled || !isInitialized) {
            android.util.Log.w("SoundManager", "音效未启用或未初始化")
            return
        }

        val soundId = soundIds[name]
        if (soundId == null) {
            android.util.Log.w("SoundManager", "❌ 音效未找到: $name (可用音效: ${soundIds.keys})")
            return
        }

        // 防止短时间内重复播放
        val now = System.currentTimeMillis()
        val lastTime = lastPlayTime[name] ?: 0L
        if (now - lastTime < MIN_PLAY_INTERVAL) {
            android.util.Log.d("SoundManager", "音效播放过快，跳过: $name")
            return
        }
        lastPlayTime[name] = now

        val result = soundPool?.play(
            soundId,
            volume,  // 左声道音量
            volume,  // 右声道音量
            1,       // 优先级
            0,       // 循环次数（0=不循环）
            1.0f     // 播放速率
        )
        
        android.util.Log.d("SoundManager", "🎵 播放音效: $name, soundId=$soundId, result=$result")
    }

    /**
     * 播放长音效（使用 MediaPlayer）
     * 用于专注完成等较长音效
     */
    fun playLongSound(name: String) {
        if (!isSoundEnabled) return
        
        val ctx = context ?: return
        val fileName = SOUND_FILES[name] ?: return

        try {
            val resId = ctx.resources.getIdentifier(fileName, "raw", ctx.packageName)
            if (resId != 0) {
                val mediaPlayer = MediaPlayer.create(ctx, resId)
                mediaPlayer?.setOnCompletionListener { it.release() }
                mediaPlayer?.start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ───────────── 生命周期 ─────────────

    /**
     * 释放资源
     */
    fun release() {
        soundPool?.release()
        soundPool = null
        soundIds.clear()
        isInitialized = false
    }

    /**
     * 暂停所有音效
     */
    fun pauseAll() {
        soundPool?.autoPause()
    }

    /**
     * 恢复所有音效
     */
    fun resumeAll() {
        soundPool?.autoResume()
    }
}
