package com.example.focusflow.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.PixelFormat
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Space
import android.widget.TextView
import androidx.compose.ui.graphics.toArgb
import com.example.focusflow.MainActivity
import com.example.focusflow.data.AppDatabase
import com.example.focusflow.data.ChatMessageEntity
import com.example.focusflow.data.ChatSessionEntity
import com.example.focusflow.ui.widget.CapsuleHatchView
import com.example.focusflow.utils.UsageStatsWatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * 悬浮窗锁屏服务 - 基因孵化舱版本
 * 
 * 采用盲盒充能流概念，使用纯原生Canvas绘制引擎
 * 
 * ## 防逃逸机制
 * - 集成 UsageStatsWatcher 高频轮询引擎
 * - 检测到逃逸时自动拉回应用
 * - 生命周期安全管理，防止内存泄漏和后台耗电
 */
class LockOverlayService : Service() {

    /**
     * 主题颜色数据类
     */
    private data class ThemeColors(
        val bgDeep: Int,
        val primary: Int,
        val textMain: Int,
        val textSub: Int,
        val cardBg: Int
    )

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var isOverlayShowing = false
    
    // 主题颜色（缓存以供各个函数使用）
    private var themeColors: ThemeColors? = null
    
    private var taskName = ""
    private var timeLeftSeconds = 0L          // 当前阶段剩余时间
    private var totalTimeLeftSeconds = 0L     // 总剩余时间
    private var totalTimeSeconds = 0L         // 初始总时长
    private var isBreakPhase = false          // 当前是否休息阶段
    private val handler = Handler(Looper.getMainLooper())
    
    // 主视图
    private var capsuleHatchView: CapsuleHatchView? = null
    private var taskNameText: TextView? = null
    private var statusText: TextView? = null
    private var dateTimeText: TextView? = null  // 系统日期时间
    
    // AI 对话相关
    private var mainLayout: View? = null
    private var chatOverlay: View? = null
    private var chatMessageContainer: LinearLayout? = null
    private var chatInput: EditText? = null
    private var chatMessages = mutableListOf<Pair<Boolean, String>>()
    private var currentAiResponse = StringBuilder()
    private var sseClient: OkHttpClient? = null
    private var sseEventSource: EventSource? = null
    
    // 会话管理相关
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var database: AppDatabase? = null
    private var chatSessionDao: com.example.focusflow.data.ChatSessionDao? = null
    private var chatDao: com.example.focusflow.data.ChatDao? = null
    private var currentSessionId: Long? = null
    private var sessionsList: List<ChatSessionEntity> = emptyList()
    private var sessionListPanel: FrameLayout? = null
    private var currentSessionTitleView: TextView? = null
    
    // 确认弹窗
    private var confirmDialog: FrameLayout? = null

    // 防逃逸暗哨 - 高频轮询引擎
    private var usageStatsWatcher: UsageStatsWatcher? = null
    private var escapeCount = 0  // 逃逸次数统计（用于日志分析）
    
    // 主题变化监听
    private var themeChangeReceiver: android.content.BroadcastReceiver? = null
    
    // 专注完成广播接收器
    private var focusCompleteReceiver: android.content.BroadcastReceiver? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        // 初始化数据库
        database = AppDatabase.getDatabase(this)
        chatSessionDao = database?.chatSessionDao()
        chatDao = database?.chatDao()
        
        // 初始化防逃逸暗哨
        usageStatsWatcher = UsageStatsWatcher(this)
        android.util.Log.d("LockOverlayService", "防逃逸暗哨已初始化")
        
        // 注册专注完成广播接收器
        registerFocusCompleteReceiver()
        
        // 注册主题变化监听
        registerThemeChangeReceiver()
    }
    
    /**
     * 注册主题变化监听
     */
    private fun registerThemeChangeReceiver() {
        val filter = android.content.IntentFilter(ACTION_THEME_CHANGED)
        themeChangeReceiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
                android.util.Log.d("LockOverlayService", "收到主题变化广播，重建悬浮窗")
                
                // 如果悬浮窗正在显示，重新创建视图
                if (isOverlayShowing) {
                    recreateOverlayView()
                }
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(themeChangeReceiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(themeChangeReceiver, filter)
        }
        
        android.util.Log.d("LockOverlayService", "主题变化监听已注册")
    }
    
    /**
     * 重新创建悬浮窗视图（保持状态）
     */
    private fun recreateOverlayView() {
        try {
            // 移除旧视图
            overlayView?.let { view ->
                windowManager?.removeView(view)
            }
            
            // 创建新视图
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.LEFT
            }
            
            val container = createCapsuleOverlayView()
            windowManager?.addView(container, params)
            overlayView = container
            
            // 更新时间显示
            updateTimeDisplay()
            
            android.util.Log.d("LockOverlayService", "悬浮窗视图已重建")
        } catch (e: Exception) {
            android.util.Log.e("LockOverlayService", "重建悬浮窗失败", e)
        }
    }
    
    /**
     * 注册专注完成广播接收器
     * 处理悬浮窗隐藏和返回应用
     */
    private fun registerFocusCompleteReceiver() {
        val filter = android.content.IntentFilter(ACTION_FOCUS_COMPLETE)
        focusCompleteReceiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
                android.util.Log.d("LockOverlayService", "收到专注完成广播")
                
                // 1. 隐藏悬浮窗
                hideOverlay()
                
                // 2. 返回应用（复用已存在的 Activity）
                val launchIntent = Intent(this@LockOverlayService, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                startActivity(launchIntent)
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(focusCompleteReceiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(focusCompleteReceiver, filter)
        }
        
        android.util.Log.d("LockOverlayService", "专注完成广播接收器已注册")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> {
                taskName = intent.getStringExtra(EXTRA_TASK_NAME) ?: "专注中"
                timeLeftSeconds = intent.getLongExtra(EXTRA_TIME_LEFT, 0L)
                totalTimeLeftSeconds = intent.getLongExtra(EXTRA_TOTAL_TIME_LEFT, timeLeftSeconds)
                totalTimeSeconds = totalTimeLeftSeconds  // 记录初始总时长
                isBreakPhase = intent.getBooleanExtra(EXTRA_IS_BREAK, false)
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                    android.util.Log.e("LockOverlayService", "没有悬浮窗权限")
                    return START_STICKY
                }
                
                showOverlay()
            }
            ACTION_HIDE -> {
                hideOverlay()
            }
            ACTION_UPDATE -> {
                // 接收来自 FocusService 的每秒更新
                timeLeftSeconds = intent.getLongExtra(EXTRA_TIME_LEFT, timeLeftSeconds)
                totalTimeLeftSeconds = intent.getLongExtra(EXTRA_TOTAL_TIME_LEFT, totalTimeLeftSeconds)
                isBreakPhase = intent.getBooleanExtra(EXTRA_IS_BREAK, false)
                updateTimeDisplay()
            }
        }
        return START_STICKY
    }

    @SuppressLint("ClickableViewAccessibility", "InlinedApi")
    private fun showOverlay() {
        if (isOverlayShowing || windowManager == null) return
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.LEFT
        }
        val container = createCapsuleOverlayView()
        try {
            windowManager?.addView(container, params)
            overlayView = container
            isOverlayShowing = true
            
            // 计时由 FocusService 通过 startService 控制，不再启动独立计时器
            // 初始化显示当前时间
            updateTimeDisplay()
            
            // 启动防逃逸暗哨监控
            startEscapeWatch()
            
            android.util.Log.d("LockOverlayService", "悬浮窗已显示: timeLeft=$timeLeftSeconds, total=$totalTimeLeftSeconds")
        } catch (e: Exception) {
            android.util.Log.e("LockOverlayService", "显示悬浮窗失败", e)
        }
    }
    
    /**
     * 启动防逃逸监控
     */
    private fun startEscapeWatch() {
        usageStatsWatcher?.let { watcher ->
            if (com.example.focusflow.utils.FocusLockHelper.hasUsageStatsPermission(this)) {
                watcher.startWatching {
                    // 检测到逃逸，在主线程执行拉回操作
                    handler.post {
                        escapeCount++
                        android.util.Log.w("LockOverlayService", "检测到第 $escapeCount 次逃逸尝试，正在拉回...")
                        watcher.pullAppToFront()
                    }
                }
                android.util.Log.i("LockOverlayService", "防逃逸暗哨已启动")
            } else {
                android.util.Log.w("LockOverlayService", "无使用情况访问权限，防逃逸监控未启动")
            }
        }
    }
    
    /**
     * 停止防逃逸监控
     */
    private fun stopEscapeWatch() {
        usageStatsWatcher?.stopWatching()
        android.util.Log.i("LockOverlayService", "防逃逸暗哨已停止，本次专注共检测到 $escapeCount 次逃逸尝试")
        escapeCount = 0
    }
    
    /**
     * 创建基因孵化舱主题界面
     */
    @SuppressLint("InlinedApi")
    private fun createCapsuleOverlayView(): View {
        // 获取当前主题颜色并缓存
        val appColors = com.example.focusflow.ui.theme.ThemeManager.getCurrentColors(this)
        
        // 调试日志：输出当前主题颜色
        android.util.Log.d("LockOverlayService", "创建悬浮窗 - 主题颜色: bgDeep=${appColors.bgDeep}, primary=${appColors.primary}, isDark=${appColors.isDark}")
        
        themeColors = ThemeColors(
            bgDeep = appColors.bgDeep.toArgb(),
            primary = appColors.primary.toArgb(),
            textMain = appColors.textMain.toArgb(),
            textSub = appColors.textSub.toArgb(),
            cardBg = appColors.cardBg.toArgb()
        )
        
        val cyberBg = themeColors!!.bgDeep
        val cyberPrimary = themeColors!!.primary
        val cyberPurple = Color.parseColor("#B026FF")  // 保留紫色作为固定强调色
        val cyberTextSub = themeColors!!.textSub
        val cyberTextMain = themeColors!!.textMain
        val cyberCardBg = themeColors!!.cardBg
        
        android.util.Log.d("LockOverlayService", "背景色 Int 值: $cyberBg (hex: ${Integer.toHexString(cyberBg)})")
        
        // 初始化 SSE 客户端
        if (sseClient == null) {
            sseClient = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build()
        }
        
        return FrameLayout(this).apply {
            setBackgroundColor(cyberBg)
            
            // 隐藏系统UI（全屏沉浸）
            systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
            
            // ========== 主布局（孵化舱界面） ==========
            mainLayout = LinearLayout(this@LockOverlayService).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dpToPx(24), dpToPx(48), dpToPx(24), dpToPx(24))
                
                // ========== 顶部状态区 ==========
                addView(createTopStatusSection(cyberPrimary, cyberPurple, cyberTextSub, cyberTextMain))
                
                // ========== 孵化舱核心视图 ==========
                capsuleHatchView = CapsuleHatchView(this@LockOverlayService).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1f
                    )
                    // 设置主题颜色
                    setThemeColors(cyberBg, cyberPrimary, appColors.isDark)
                    // 设置初始总时长用于进度计算
                    if (totalTimeSeconds > 0) {
                        setInitialTotal(totalTimeSeconds)
                    }
                }
                addView(capsuleHatchView)
                
                // ========== 操作按钮区 ==========
                addView(createActionButtonSection(cyberPrimary, cyberPurple, cyberCardBg))
            }
            
            addView(mainLayout, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ))
            
            // ========== 确认弹窗（初始隐藏） ==========
            addView(createConfirmDialog(cyberBg, cyberPrimary, cyberTextSub, cyberTextMain), FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ))
            
            // ========== AI 对话覆盖层（初始隐藏） ==========
            chatOverlay = createChatOverlay(cyberBg, cyberPrimary, cyberTextSub, cyberTextMain, cyberCardBg)
            addView(chatOverlay, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ))
        }
    }
    
    /**
     * 创建顶部状态区域
     */
    private fun createTopStatusSection(cyberPrimary: Int, cyberPurple: Int, cyberTextSub: Int, cyberTextMain: Int): LinearLayout {
        
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            
            // 系统日期时间
            dateTimeText = TextView(this@LockOverlayService).apply {
                text = ""  // 初始为空，由 updateTimeDisplay 更新
                setTextColor(cyberTextSub)
                textSize = 13f
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                gravity = Gravity.CENTER
                letterSpacing = 0.1f
            }
            addView(dateTimeText, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ))
            
            // 状态文字
            statusText = TextView(this@LockOverlayService).apply {
                text = "◈ 专注充能中 ◈"
                setTextColor(cyberPrimary)
                textSize = 14f
                letterSpacing = 0.2f
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                gravity = Gravity.CENTER
            }
            addView(statusText, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dpToPx(8) })
            
            // 任务名
            taskNameText = TextView(this@LockOverlayService).apply {
                text = taskName
                setTextColor(cyberTextMain)
                textSize = 20f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
            }
            addView(taskNameText, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dpToPx(8) })
        }
    }
    
    /**
     * 创建操作按钮区域
     */
    private fun createActionButtonSection(cyberPrimary: Int, cyberPurple: Int, cyberCardBg: Int): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dpToPx(24) }
            
            // AI 对话按钮
            addView(createCyberButton("AI助手", cyberPrimary, cyberCardBg) {
                showChatOverlay()
            })
            
            addView(Space(this@LockOverlayService).apply {
                layoutParams = LinearLayout.LayoutParams(dpToPx(32), 1)
            })
            
            // 放弃按钮（保留红色作为警告色）
            addView(createCyberButton("⛔ 放弃", Color.parseColor("#FF6B6B"), cyberCardBg) {
                showConfirmDialog()
            })
        }
    }
    
    /**
     * 创建赛博风格按钮
     */
    private fun createCyberButton(text: String, accentColor: Int, bgColor: Int, onClick: () -> Unit): FrameLayout {
        return FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            
            // 背景
            background = GradientDrawable().apply {
                setColor(bgColor)
                setCornerRadius(dpToPx(24).toFloat())
                setStroke(dpToPx(1), accentColor)
            }
            
            // 文字
            addView(TextView(this@LockOverlayService).apply {
                this.text = text
                setTextColor(accentColor)
                textSize = 14f
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                setPadding(dpToPx(20), dpToPx(12), dpToPx(20), dpToPx(12))
            }, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            ))
            
            // 点击效果
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }
    }
    
    /**
     * 创建确认弹窗
     */
    private fun createConfirmDialog(cyberBg: Int, cyberPrimary: Int, cyberTextSub: Int, cyberTextMain: Int): FrameLayout {
        val cyberDanger = Color.parseColor("#FF6B6B")  // 保留红色作为警告色
        
        return FrameLayout(this).apply {
            confirmDialog = this
            visibility = View.GONE
            setBackgroundColor(Color.parseColor("#CC000000"))  // 保留半透明遮罩
            
            val cardView = FrameLayout(this@LockOverlayService).apply {
                setBackgroundDrawable(GradientDrawable().apply {
                    setColor(cyberBg)
                    setCornerRadius(dpToPx(16).toFloat())
                    setStroke(dpToPx(2), cyberDanger)
                })
                setPadding(dpToPx(24), dpToPx(32), dpToPx(24), dpToPx(24))
                
                val contentLayout = LinearLayout(this@LockOverlayService).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    
                    // 标题
                    addView(TextView(this@LockOverlayService).apply {
                        text = "⚠️ 确认放弃？"
                        setTextColor(cyberTextMain)
                        textSize = 22f
                        typeface = Typeface.DEFAULT_BOLD
                        gravity = Gravity.CENTER
                    })
                    
                    // 提示信息
                    addView(TextView(this@LockOverlayService).apply {
                        text = "放弃后本次专注将不会获得任何奖励"
                        setTextColor(cyberTextSub)
                        textSize = 14f
                        gravity = Gravity.CENTER
                    }, LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { topMargin = dpToPx(12) })
                    
                    // 按钮区域
                    addView(LinearLayout(this@LockOverlayService).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER
                        
                        val cyberDanger = Color.parseColor("#FF6B6B")  // 保留红色作为警告色
                        
                        // 继续专注按钮
                        addView(TextView(this@LockOverlayService).apply {
                            text = "继续专注"
                            setTextColor(cyberPrimary)
                            textSize = 16f
                            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                            setPadding(dpToPx(20), dpToPx(12), dpToPx(20), dpToPx(12))
                            setOnClickListener { hideConfirmDialog() }
                        }, LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { rightMargin = dpToPx(12) })
                        
                        // 确认放弃按钮
                        addView(TextView(this@LockOverlayService).apply {
                            text = "确认放弃"
                            setTextColor(cyberDanger)
                            textSize = 16f
                            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                            setPadding(dpToPx(20), dpToPx(12), dpToPx(20), dpToPx(12))
                            setOnClickListener {
                                hideConfirmDialog()
                                performAbandon()
                            }
                        })
                    }, LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { topMargin = dpToPx(24) })
                }
                
                addView(contentLayout, FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
                ))
            }
            
            addView(cardView, FrameLayout.LayoutParams(
                dpToPx(340),
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            ))
        }
    }
    
    private fun showConfirmDialog() {
        confirmDialog?.visibility = View.VISIBLE
    }
    
    private fun hideConfirmDialog() {
        confirmDialog?.visibility = View.GONE
    }
    
    // ========== AI 对话相关 ==========
    
    @SuppressLint("InlinedApi")
    private fun createChatOverlay(cyberBg: Int, cyberPrimary: Int, cyberTextSub: Int, cyberTextMain: Int, cyberCardBg: Int): FrameLayout {
        return FrameLayout(this).apply {
            visibility = View.GONE
            setBackgroundColor(cyberBg)
            
            // 隐藏系统UI
            systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
            
            // 主聊天容器
            val mainContainer = LinearLayout(this@LockOverlayService).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dpToPx(16), dpToPx(24), dpToPx(16), dpToPx(16))
                
                // 标题栏
                addView(LinearLayout(this@LockOverlayService).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    
                    // 会话切换按钮
                    addView(TextView(this@LockOverlayService).apply {
                        text = "☰"
                        setTextColor(cyberPrimary)
                        textSize = 22f
                        setPadding(dpToPx(8), dpToPx(8), dpToPx(12), dpToPx(8))
                        setOnClickListener { toggleSessionList() }
                    })
                    
                    // 当前会话标题
                    currentSessionTitleView = TextView(this@LockOverlayService).apply {
                        text = "AI专注助手"
                        setTextColor(cyberTextMain)
                        textSize = 18f
                        typeface = Typeface.DEFAULT_BOLD
                    }
                    addView(currentSessionTitleView)
                    
                    addView(Space(this@LockOverlayService).apply {
                        layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
                    })
                    
                    // 关闭按钮
                    addView(TextView(this@LockOverlayService).apply {
                        text = "✕"
                        setTextColor(cyberPrimary)
                        textSize = 24f
                        setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))
                        setOnClickListener { hideChatOverlay() }
                    })
                }, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ))
                
                // 消息列表
                val scrollView = ScrollView(this@LockOverlayService).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1f
                    )
                    
                    chatMessageContainer = LinearLayout(this@LockOverlayService).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(0, dpToPx(16), 0, dpToPx(16))
                    }
                    addView(chatMessageContainer)
                }
                addView(scrollView)
                
                // 输入区域
                addView(LinearLayout(this@LockOverlayService).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { topMargin = dpToPx(16) }
                    
                    // 输入框
                    chatInput = EditText(this@LockOverlayService).apply {
                        hint = "输入消息..."
                        setHintTextColor(cyberTextSub)
                        setTextColor(cyberTextMain)
                        textSize = 16f
                        background = GradientDrawable().apply {
                            setColor(cyberCardBg)
                            setCornerRadius(dpToPx(24).toFloat())
                        }
                        setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12))
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                        setOnEditorActionListener { _, actionId, _ ->
                            if (actionId == EditorInfo.IME_ACTION_SEND) {
                                sendChatMessage()
                                true
                            } else false
                        }
                    }
                    addView(chatInput)
                    
                    // 发送按钮
                    addView(TextView(this@LockOverlayService).apply {
                        text = "发送"
                        setTextColor(cyberPrimary)
                        textSize = 14f
                        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                        setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12))
                        setOnClickListener { sendChatMessage() }
                    })
                })
            }
            
            // 计算屏幕高度的 90%（与首页 ChatBottomSheet 协同）
            val displayMetrics = resources.displayMetrics
            val screenHeight = displayMetrics.heightPixels
            val chatHeight = (screenHeight * 0.9f).toInt()
            
            // 底部对齐，模拟 BottomSheet 效果
            addView(mainContainer, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                chatHeight,
                Gravity.BOTTOM
            ))
            
            // 会话列表面板（初始隐藏）
            sessionListPanel = createSessionListPanel(cyberBg, cyberPrimary, cyberTextMain, cyberCardBg)
            addView(sessionListPanel, FrameLayout.LayoutParams(
                dpToPx(280),
                FrameLayout.LayoutParams.MATCH_PARENT
            ))
            sessionListPanel?.visibility = View.GONE
            
            // 初始化加载会话
            loadSessions()
        }
    }
    
    // 创建会话列表面板
    private fun createSessionListPanel(cyberBg: Int, cyberPrimary: Int, cyberTextMain: Int, cyberCardBg: Int): FrameLayout {
        return FrameLayout(this).apply {
            setBackgroundColor(cyberBg)
            
            val container = LinearLayout(this@LockOverlayService).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dpToPx(16), dpToPx(24), dpToPx(16), dpToPx(16))
                
                // 标题栏
                addView(LinearLayout(this@LockOverlayService).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    
                    addView(TextView(this@LockOverlayService).apply {
                        text = "会话列表"
                        setTextColor(cyberTextMain)
                        textSize = 18f
                        typeface = Typeface.DEFAULT_BOLD
                    })
                    
                    addView(Space(this@LockOverlayService).apply {
                        layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
                    })
                    
                    // 关闭按钮
                    addView(TextView(this@LockOverlayService).apply {
                        text = "✕"
                        setTextColor(cyberPrimary)
                        textSize = 20f
                        setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
                        setOnClickListener { hideSessionList() }
                    })
                }, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ))
                
                // 新建会话按钮
                addView(TextView(this@LockOverlayService).apply {
                    text = "+ 新建会话"
                    setTextColor(cyberTextMain)
                    textSize = 14f
                    gravity = Gravity.CENTER
                    setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12))
                    background = GradientDrawable().apply {
                        setColor(cyberPrimary)
                        setCornerRadius(dpToPx(8).toFloat())
                    }
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { topMargin = dpToPx(16); bottomMargin = dpToPx(16) }
                    setOnClickListener { 
                        createNewSession()
                        hideSessionList()
                    }
                })
                
                // 会话列表（使用 ScrollView 包裹）
                addView(ScrollView(this@LockOverlayService).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1f
                    )
                    
                    // 会话列表容器（动态添加）
                    addView(LinearLayout(this@LockOverlayService).apply {
                        orientation = LinearLayout.VERTICAL
                        id = android.R.id.list
                    })
                })
            }
            
            addView(container, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ))
        }
    }
    
    // 切换会话列表显示
    private fun toggleSessionList() {
        if (sessionListPanel?.visibility == View.VISIBLE) {
            hideSessionList()
        } else {
            showSessionList()
        }
    }
    
    private fun showSessionList() {
        loadSessions()
        sessionListPanel?.visibility = View.VISIBLE
    }
    
    private fun hideSessionList() {
        sessionListPanel?.visibility = View.GONE
    }
    
    // 加载会话列表
    private fun loadSessions() {
        serviceScope.launch {
            try {
                sessionsList = withContext(Dispatchers.IO) {
                    chatSessionDao?.getAllSessionsOnce() ?: emptyList()
                }
                updateSessionListUI()
            } catch (e: Exception) {
                android.util.Log.e("LockOverlayService", "加载会话失败: ${e.message}")
            }
        }
    }
    
    // 更新会话列表 UI
    private fun updateSessionListUI() {
        val listContainer = sessionListPanel?.findViewById<LinearLayout>(android.R.id.list) ?: return
        listContainer.removeAllViews()
        
        for (session in sessionsList) {
            val isSelected = session.sessionId == currentSessionId
            listContainer.addView(createSessionItem(session, isSelected))
        }
    }
    
    // 创建会话项
    private fun createSessionItem(session: ChatSessionEntity, isSelected: Boolean): View {
        val colors = themeColors ?: return LinearLayout(this)  // 安全检查
        val cyberPrimary = colors.primary
        val cyberTextMain = colors.textMain
        val cyberTextSub = colors.textSub
        val cyberCardBg = colors.cardBg
        
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
            background = GradientDrawable().apply {
                // 选中状态使用 primary 颜色的半透明版本，未选中使用 cardBg
                setColor(if (isSelected) {
                    // 创建半透明的 primary 背景
                    val alpha = 0x33  // 20% 透明度
                    (alpha shl 24) or (cyberPrimary and 0x00FFFFFF)
                } else {
                    cyberCardBg
                })
                setCornerRadius(dpToPx(8).toFloat())
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dpToPx(8) }
            
            setOnClickListener { 
                selectSession(session.sessionId)
                hideSessionList()
            }
            
            // 标题
            addView(TextView(this@LockOverlayService).apply {
                text = session.title
                setTextColor(if (isSelected) cyberPrimary else cyberTextMain)
                textSize = 14f
                maxLines = 1
            })
            
            // 时间和消息数
            addView(TextView(this@LockOverlayService).apply {
                val timeStr = formatTime(session.updatedAt)
                text = "$timeStr · ${session.messageCount}条消息"
                setTextColor(cyberTextSub)
                textSize = 12f
            })
        }
    }
    
    private fun formatTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        return when {
            diff < 60000 -> "刚刚"
            diff < 3600000 -> "${diff / 60000}分钟前"
            diff < 86400000 -> "${diff / 3600000}小时前"
            else -> "${diff / 86400000}天前"
        }
    }
    
    // 创建新会话
    private fun createNewSession() {
        serviceScope.launch {
            try {
                val session = ChatSessionEntity(
                    title = "新对话",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                val sessionId = withContext(Dispatchers.IO) {
                    chatSessionDao?.insertSession(session) ?: 0L
                }
                currentSessionId = sessionId
                chatMessages.clear()
                chatMessageContainer?.removeAllViews()
                addChatMessage(false, "你好！我是你的专注助手，有什么可以帮助你的吗？")
                currentSessionTitleView?.text = "新对话"
                loadSessions()
            } catch (e: Exception) {
                android.util.Log.e("LockOverlayService", "创建会话失败: ${e.message}")
            }
        }
    }
    
    // 选择会话
    private fun selectSession(sessionId: Long) {
        if (currentSessionId == sessionId) return
        
        serviceScope.launch {
            currentSessionId = sessionId
            loadMessagesForSession(sessionId)
            
            // 更新标题
            val session = sessionsList.find { it.sessionId == sessionId }
            currentSessionTitleView?.text = session?.title ?: "AI 助手"
        }
    }
    
    // 加载会话消息
    private suspend fun loadMessagesForSession(sessionId: Long) {
        try {
            val messages = withContext(Dispatchers.IO) {
                chatDao?.getMessagesBySessionOnce(sessionId) ?: emptyList()
            }
            
            chatMessages.clear()
            chatMessageContainer?.removeAllViews()
            
            for (msg in messages) {
                addChatMessage(msg.isUser, msg.content)
            }
            
            if (messages.isEmpty()) {
                addChatMessage(false, "你好！我是你的专注助手，有什么可以帮助你的吗？")
            }
        } catch (e: Exception) {
            android.util.Log.e("LockOverlayService", "加载消息失败: ${e.message}")
        }
    }
    
    private fun showChatOverlay() {
        mainLayout?.visibility = View.GONE
        chatOverlay?.visibility = View.VISIBLE
    }
    
    private fun hideChatOverlay() {
        chatOverlay?.visibility = View.GONE
        mainLayout?.visibility = View.VISIBLE
    }
    
    private fun addChatMessage(isUser: Boolean, message: String) {
        chatMessages.add(Pair(isUser, message))
        
        val colors = themeColors ?: return  // 安全检查
        
        chatMessageContainer?.addView(TextView(this).apply {
            text = message
            // 用户消息：深色文字 + primary 背景；AI消息：textMain 文字 + cardBg 背景
            setTextColor(if (isUser) colors.bgDeep else colors.textMain)
            textSize = 14f
            setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10))
            background = GradientDrawable().apply {
                setColor(if (isUser) colors.primary else colors.cardBg)
                setCornerRadius(dpToPx(12).toFloat())
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = if (isUser) Gravity.END else Gravity.START
                topMargin = dpToPx(8)
                if (isUser) {
                    leftMargin = dpToPx(48)
                } else {
                    rightMargin = dpToPx(48)
                }
            }
        })
        
        // 滚动到底部
        handler.postDelayed({
            (chatMessageContainer?.parent as? ScrollView)?.fullScroll(View.FOCUS_DOWN)
        }, 100)
    }
    
    private fun updateLastAiMessage(message: String) {
        if (chatMessages.isNotEmpty() && !chatMessages.last().first) {
            chatMessages[chatMessages.size - 1] = Pair(false, message)
            chatMessageContainer?.getChildAt(chatMessageContainer!!.childCount - 1)?.let {
                (it as? TextView)?.text = message
            }
            // 滚动到底部（流式更新时保持可见）
            handler.post {
                (chatMessageContainer?.parent as? ScrollView)?.fullScroll(View.FOCUS_DOWN)
            }
        }
    }
    
    private fun sendChatMessage() {
        val message = chatInput?.text?.toString()?.trim() ?: ""
        if (message.isEmpty()) return
        
        // 清空输入框
        chatInput?.setText("")
        
        // 确保有当前会话
        ensureCurrentSession()
        
        // 添加用户消息
        addChatMessage(true, message)
        
        // 保存用户消息到数据库
        saveMessageToDatabase(true, message)
        
        // 添加 AI 占位消息
        addChatMessage(false, "...")
        
        // 发送 SSE 请求
        sendSseRequest(message)
    }
    
    // 确保当前有会话
    private fun ensureCurrentSession() {
        if (currentSessionId == null) {
            serviceScope.launch {
                createNewSession()
            }
        }
    }
    
    // 保存消息到数据库
    private fun saveMessageToDatabase(isUser: Boolean, content: String) {
        val sessionId = currentSessionId ?: return
        serviceScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    chatDao?.insertMessage(ChatMessageEntity(
                        sessionId = sessionId,
                        content = content,
                        isUser = isUser
                    ))
                    
                    // 更新会话信息
                    val count = chatDao?.getMessageCount(sessionId) ?: 0
                    val preview = if (content.length > 50) content.take(50) + "..." else content
                    chatSessionDao?.updateMessageInfo(sessionId, count, preview)
                    
                    // 如果是第一条消息，更新标题
                    if (count <= 1 && isUser) {
                        val title = if (content.length > 20) content.take(20) + "..." else content
                        chatSessionDao?.updateTitle(sessionId, title)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("LockOverlayService", "保存消息失败: ${e.message}")
            }
        }
    }
    
    private fun sendSseRequest(userMessage: String) {
        // 取消之前的请求
        sseEventSource?.cancel()
        currentAiResponse.clear()
        
        // 构建历史上下文（取最近6条消息），使用 Gson 正确序列化（转义控制字符）
        val gson = com.google.gson.Gson()
        val contextMessages = chatMessages.takeLast(6).map { (isUser, content) ->
            mapOf("role" to if (isUser) "user" else "assistant", "content" to content)
        }
        val messagesJson = gson.toJson(contextMessages)
        
        // 通过后端代理调用 AI（安全：API Key 在服务端）
        val userId = getUserId()
        
        val request = Request.Builder()
            .url("${com.example.focusflow.api.RetrofitClient.GATEWAY_BASE_URL}ai/chat/stream")
            .post(messagesJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .header("Accept", "text/event-stream")
            .header("X-User-Id", userId.toString())
            .build()
        
        val factory = EventSources.createFactory(sseClient!!)
        sseEventSource = factory.newEventSource(request, object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                if (data == "[DONE]") return
                try {
                    // 兼容两种格式：纯文本或 JSON
                    val content = if (data.startsWith("{")) {
                        JSONObject(data).optString("content", "")
                    } else {
                        data
                    }
                    if (content.isNotEmpty()) {
                        handler.post {
                            currentAiResponse.append(content)
                            updateLastAiMessage(currentAiResponse.toString())
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("LockOverlayService", "SSE解析错误: ${e.message}")
                }
            }
            
            override fun onClosed(eventSource: EventSource) {
                android.util.Log.d("LockOverlayService", "SSE连接关闭")
                // 保存 AI 消息到数据库（排除错误消息）
                val finalResponse = currentAiResponse.toString()
                if (finalResponse.isNotBlank() && !finalResponse.startsWith("⚠️") && !finalResponse.startsWith("网络")) {
                    saveMessageToDatabase(false, finalResponse)
                }
            }
            
            override fun onFailure(eventSource: EventSource, t: Throwable?, response: okhttp3.Response?) {
                android.util.Log.e("LockOverlayService", "SSE失败: ${t?.message}")
                handler.post {
                    if (currentAiResponse.isEmpty()) {
                        updateLastAiMessage("网络错误，请稍后重试")
                    } else {
                        // 保存已收到的部分消息
                        saveMessageToDatabase(false, currentAiResponse.toString())
                    }
                }
            }
        })
    }
    
    private fun getUserId(): Long {
        return com.example.focusflow.data.session.SessionManager.getInstance(this).getUserIdSync()
    }
    
    // ========== 时间更新 ==========
    
    private fun updateTimeDisplay() {
        // 更新孵化舱视图：当前阶段倒计时（timeLeftSeconds）和总剩余时间（totalTimeLeftSeconds）
        capsuleHatchView?.setTime(timeLeftSeconds, totalTimeLeftSeconds)
        
        // 更新任务名（添加阶段提示）
        val phaseText = if (isBreakPhase) "休息中" else "专注中"
        taskNameText?.text = "$phaseText: $taskName"
        
        // 更新系统日期时间
        updateSystemDateTime()
    }
    
    /**
     * 更新系统日期时间显示
     * 格式：2026年4月14日 周一 15:30
     */
    private fun updateSystemDateTime() {
        val now = java.util.Calendar.getInstance()
        val year = now.get(java.util.Calendar.YEAR)
        val month = now.get(java.util.Calendar.MONTH) + 1
        val day = now.get(java.util.Calendar.DAY_OF_MONTH)
        val dayOfWeek = when (now.get(java.util.Calendar.DAY_OF_WEEK)) {
            java.util.Calendar.SUNDAY -> "周日"
            java.util.Calendar.MONDAY -> "周一"
            java.util.Calendar.TUESDAY -> "周二"
            java.util.Calendar.WEDNESDAY -> "周三"
            java.util.Calendar.THURSDAY -> "周四"
            java.util.Calendar.FRIDAY -> "周五"
            java.util.Calendar.SATURDAY -> "周六"
            else -> ""
        }
        val hour = now.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = now.get(java.util.Calendar.MINUTE)
        
        val timeStr = String.format("%d年%d月%d日 %s %02d:%02d", year, month, day, dayOfWeek, hour, minute)
        dateTimeText?.text = timeStr
    }
    
    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
    
    private fun onFocusComplete() {
        android.util.Log.d("LockOverlayService", "专注完成: taskName=$taskName, totalTime=$totalTimeSeconds")
        
        // 发送专注完成广播（携带任务名和时长）
        // 广播接收器会处理隐藏悬浮窗和返回应用
        val broadcastIntent = Intent(ACTION_FOCUS_COMPLETE).apply {
            putExtra(EXTRA_TASK_NAME, taskName)
            putExtra(EXTRA_DURATION_SECONDS, totalTimeSeconds)
            setPackage(packageName)
        }
        sendBroadcast(broadcastIntent)
    }
    
    private fun performAbandon() {
        android.util.Log.d("LockOverlayService", "用户放弃专注")
        hideOverlay()
        
        // 发送放弃广播
        val broadcastIntent = Intent(ACTION_ABANDON_FOCUS).apply {
            setPackage(packageName)
        }
        sendBroadcast(broadcastIntent)
        
        // 返回应用（复用已存在的Activity，避免重启）
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
    }

    private fun hideOverlay() {
        // 停止防逃逸监控
        stopEscapeWatch()
        
        if (!isOverlayShowing) return
        
        try {
            overlayView?.let {
                windowManager?.removeViewImmediate(it)
            }
            overlayView = null
            isOverlayShowing = false
            android.util.Log.d("LockOverlayService", "悬浮窗已隐藏")
        } catch (e: Exception) {
            android.util.Log.e("LockOverlayService", "隐藏悬浮窗失败", e)
        }
    }

    override fun onDestroy() {
        hideOverlay()
        sseEventSource?.cancel()
        
        // 取消注册广播接收器
        try {
            focusCompleteReceiver?.let {
                unregisterReceiver(it)
                focusCompleteReceiver = null
            }
            themeChangeReceiver?.let {
                unregisterReceiver(it)
                themeChangeReceiver = null
            }
        } catch (e: Exception) {
            android.util.Log.e("LockOverlayService", "取消注册广播接收器失败: ${e.message}")
        }
        
        // 确保防逃逸监控彻底停止
        usageStatsWatcher?.stopWatching()
        usageStatsWatcher = null
        
        super.onDestroy()
    }

    companion object {
        const val ACTION_SHOW = "com.example.focusflow.action.SHOW_OVERLAY"
        const val ACTION_HIDE = "com.example.focusflow.action.HIDE_OVERLAY"
        const val ACTION_UPDATE = "com.example.focusflow.action.UPDATE_OVERLAY"
        const val ACTION_ABANDON_FOCUS = "com.example.focusflow.action.ABANDON_FOCUS"
        const val ACTION_FOCUS_COMPLETE = "com.example.focusflow.action.FOCUS_COMPLETE"
        const val ACTION_THEME_CHANGED = "com.example.focusflow.action.THEME_CHANGED"
        
        const val EXTRA_TASK_NAME = "task_name"
        const val EXTRA_TIME_LEFT = "time_left"
        const val EXTRA_TOTAL_TIME_LEFT = "total_time_left"
        const val EXTRA_IS_BREAK = "is_break"
        const val EXTRA_DURATION_SECONDS = "duration_seconds"
        
        fun show(context: android.content.Context, taskName: String, phaseTime: Long = 0, totalTime: Long = 0, isBreak: Boolean = false) {
            val intent = Intent(context, LockOverlayService::class.java).apply {
                action = ACTION_SHOW
                putExtra(EXTRA_TASK_NAME, taskName)
                putExtra(EXTRA_TIME_LEFT, phaseTime)
                putExtra(EXTRA_TOTAL_TIME_LEFT, totalTime)
                putExtra(EXTRA_IS_BREAK, isBreak)
            }
            context.startService(intent)
        }
        
        fun hide(context: android.content.Context) {
            val intent = Intent(context, LockOverlayService::class.java).apply {
                action = ACTION_HIDE
            }
            context.startService(intent)
        }
        
        fun update(context: android.content.Context, timeLeft: Long) {
            val intent = Intent(context, LockOverlayService::class.java).apply {
                action = ACTION_UPDATE
                putExtra(EXTRA_TIME_LEFT, timeLeft)
            }
            context.startService(intent)
        }
    }
}
