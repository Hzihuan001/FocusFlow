package com.example.focusflow.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.focusflow.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.min

class FocusService : Service() {

    // --- 状态定义 ---
    sealed class FocusState {
        object IDLE : FocusState()
        object RUNNING : FocusState()
        // 用秒为单位传递，避免分钟整数截断导致的错误
        data class FINISHED(val cycleFocusSeconds: Long, val taskName: String) : FocusState()
    }

    // 新增：专注阶段
    enum class FocusPhase {
        FOCUS, // 专注中
        BREAK  // 休息中
    }

    // --- 对外暴露的数据 ---
    private val _timeLeft = MutableStateFlow(0L) // 当前小轮剩余时间
    val timeLeft: StateFlow<Long> = _timeLeft.asStateFlow()

    private val _focusState = MutableStateFlow<FocusState>(FocusState.IDLE)
    val focusState: StateFlow<FocusState> = _focusState.asStateFlow()

    // 新增：对外暴露当前阶段（是专注还是休息？）
    private val _currentPhase = MutableStateFlow(FocusPhase.FOCUS)
    val currentPhase: StateFlow<FocusPhase> = _currentPhase.asStateFlow()

    // 新增：总剩余时间（用于判断大循环是否结束）
    private val _totalTimeLeft = MutableStateFlow(0L)
    val totalTimeLeft: StateFlow<Long> = _totalTimeLeft.asStateFlow()

    // --- 内部变量 ---
    private val binder = LocalBinder()
    private var timerJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // 参数记录
    var cycleFocusSeconds = 0L  // 每小轮专注时长（配置值，如25分 = 1500秒）
    var cycleBreakSeconds = 0L  // 每小轮休息时长
    private var totalTargetSeconds = 0L // 用户实际设定的总时长（如1分 = 60秒）★关键修复★
    private var currentTaskName = ""

    inner class LocalBinder : Binder() {
        fun getService(): FocusService = this@FocusService
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        // Android 8.0+ 要求服务启动后 5 秒内必须调用 startForeground
        startForegroundImmediate()
    }
    
    private fun startForegroundImmediate() {
        // 创建一个简单的初始通知，避免依赖 buildNotification
        val notification = NotificationCompat.Builder(this, "focus_channel")
            .setContentTitle("FocusFlow")
            .setContentText("服务运行中")
            .setSmallIcon(R.mipmap.focus_flow_logo)
            .setOngoing(true)
            .build()
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(1, notification)
            }
        } catch (e: Exception) {
            android.util.Log.e("FocusService", "startForeground failed", e)
        }
    }

    /**
     * 启动高级专注模式
     * @param totalMinutes 总时长（例如 240分钟）
     * @param focusMinutes 小轮专注时长（例如 25分钟）
     * @param breakMinutes 小轮休息时长（例如 5分钟）
     * @param taskName 任务名
     */
    fun startFocus(totalMinutes: Int, focusMinutes: Int, breakMinutes: Int, taskName: String) {
        android.util.Log.d("FocusService", "startFocus: $taskName, ${totalMinutes}min")
        
        currentTaskName = taskName
        cycleFocusSeconds = focusMinutes * 60L
        cycleBreakSeconds = breakMinutes * 60L

        // 记录用户真实设定的总时长
        totalTargetSeconds = totalMinutes * 60L
        _totalTimeLeft.value = totalTargetSeconds

        // 初始阶段：专注
        _currentPhase.value = FocusPhase.FOCUS
        _timeLeft.value = min(cycleFocusSeconds, _totalTimeLeft.value)

        _focusState.value = FocusState.RUNNING
        
        // 设置专注状态和参数
        android.util.Log.d("FocusService", "设置专注状态为 true")
        FocusStateManager.setFocusParams(taskName, totalMinutes, focusMinutes, breakMinutes, "")
        FocusStateManager.setFocusingState(true)
        android.util.Log.d("FocusService", "专注状态已设置: ${FocusStateManager.isFocusing.value}")

        // 启动悬浮窗锁屏 - 传递当前阶段时间（_timeLeft）和总剩余时间（_totalTimeLeft）
        LockOverlayService.show(this, taskName, _timeLeft.value, _totalTimeLeft.value, false)

        startForegroundService()
        startTimer()
    }

    // 为了兼容旧代码，保留旧的重载方法 (默认为单次专注)
    fun startFocus(minutes: Int, taskName: String) {
        startFocus(minutes, minutes, 0, taskName) // break设为0即可
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (_totalTimeLeft.value > 0 && _timeLeft.value > 0) {
                delay(1000L)

                // 两个时间都减少
                _timeLeft.value--
                _totalTimeLeft.value--
                
                // 实时更新悬浮窗锁屏显示
                updateOverlayDisplay()

                // 更新通知栏
                if (_timeLeft.value % 60 == 0L) updateNotification()

                // --- 阶段切换逻辑 ---
                if (_timeLeft.value <= 0) {
                    // 当前小轮结束，但总时间还没完
                    if (_totalTimeLeft.value > 0) {
                        switchPhase()
                    }
                }
            }

            // 总时间结束
            finishFocus()
        }
    }
    
    /**
     * 更新悬浮窗锁屏显示
     */
    private fun updateOverlayDisplay() {
        val updateIntent = Intent(this, LockOverlayService::class.java).apply {
            action = LockOverlayService.ACTION_UPDATE
            putExtra(LockOverlayService.EXTRA_TIME_LEFT, _timeLeft.value)
            putExtra(LockOverlayService.EXTRA_TOTAL_TIME_LEFT, _totalTimeLeft.value)
            putExtra(LockOverlayService.EXTRA_IS_BREAK, _currentPhase.value == FocusPhase.BREAK)
        }
        startService(updateIntent)
    }

    private fun switchPhase() {
        // 震动 + 提示音
        triggerAlert()

        if (_currentPhase.value == FocusPhase.FOCUS) {
            // 切换到 -> 休息
            _currentPhase.value = FocusPhase.BREAK
            // 设置休息时间 (但不超过总剩余时间)
            _timeLeft.value = min(cycleBreakSeconds, _totalTimeLeft.value)
            FocusStateManager.setFocusingState(false)
            updateNotification("☕ 该休息了！")
        } else {
            // 切换到 -> 专注
            _currentPhase.value = FocusPhase.FOCUS
            // 设置专注时间
            _timeLeft.value = min(cycleFocusSeconds, _totalTimeLeft.value)
            FocusStateManager.setFocusingState(true)
            updateNotification("🔥 开始下一轮专注")
        }
    }

    private fun finishFocus() {
        _timeLeft.value = 0
        _totalTimeLeft.value = 0

        // 奖励基于用户设定的总目标时长
        _focusState.value = FocusState.FINISHED(
            cycleFocusSeconds = totalTargetSeconds,
            taskName = currentTaskName
        )

        FocusStateManager.setFocusingState(false)
        
        // 发送专注完成广播，LockOverlayService 会先拉回用户再隐藏悬浮窗
        val completeIntent = Intent(LockOverlayService.ACTION_FOCUS_COMPLETE).apply {
            setPackage(packageName)
            putExtra(LockOverlayService.EXTRA_TASK_NAME, currentTaskName)
            putExtra(LockOverlayService.EXTRA_DURATION_SECONDS, totalTargetSeconds)
        }
        sendBroadcast(completeIntent)
        
        // 不直接调用 hide()，让 LockOverlayService 处理拉回和隐藏
        triggerAlert()
    }

    /**
     * 用户主动放弃专注——只停止计时，绝不发出 FINISHED 状态
     * 与 finishFocus() 的根本区别：不触发结算弹窗，不计算奖励
     */
    fun abandonFocus() {
        timerJob?.cancel()
        timerJob = null
        _focusState.value = FocusState.IDLE   // 直接回 IDLE，跳过 FINISHED
        _timeLeft.value = 0
        _totalTimeLeft.value = 0
        FocusStateManager.setFocusingState(false)
        
        // 停止悬浮窗锁屏
        LockOverlayService.hide(this)
        
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // --- 震动与声音提醒 ---
    private fun triggerAlert() {
        // 1. 震动（检查震动开关）
        val vibrationEnabled = com.example.focusflow.api.UserPreferences.isVibrationEnabled(this)
        
        if (vibrationEnabled) {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                manager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (vibrator.hasVibrator()) {
                // 震动两下：嗡~ 嗡~
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 200, 500), -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0, 500, 200, 500), -1)
                }
            }
        }

        // 2. 播放完成音效（由 SoundManager 统一管理）
        // 注意：音效会在 FocusViewModel 中播放，这里不再重复播放
    }

    // --- 基础服务方法 (通知栏等) ---

    fun stopFocus() {
        timerJob?.cancel()
        _focusState.value = FocusState.IDLE
        FocusStateManager.setFocusingState(false)
        
        // 停止悬浮窗锁屏
        LockOverlayService.hide(this)
        
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startForegroundService() {
        // 更新通知内容为专注状态
        updateNotification("专注开始")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("focus_channel", "专注服务", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(statusText: String): Notification {
        val mins = _timeLeft.value / 60
        val phaseText = if (_currentPhase.value == FocusPhase.FOCUS) "专注中" else "休息中"

        return NotificationCompat.Builder(this, "focus_channel")
            .setContentTitle("$phaseText: $currentTaskName")
            .setContentText("$statusText (剩余 ${mins}分钟)")
            .setSmallIcon(R.mipmap.focus_flow_logo) // 使用新 logo
            .setOngoing(true)
            .setOnlyAlertOnce(true) // 防止每次更新都叮咚响
            .build()
    }

    private fun updateNotification(customText: String? = null) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(1, buildNotification(customText ?: ""))
    }

    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
        serviceScope.cancel()
    }
}