package com.example.focusflow

import android.Manifest
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.focusflow.service.FocusService
import com.example.focusflow.service.LockOverlayService
import com.example.focusflow.data.session.SessionManager
import kotlinx.coroutines.flow.first
import com.example.focusflow.data.repository.BatchSettlementResult
import com.example.focusflow.ui.MainViewModel
import com.example.focusflow.ui.screens.AuthScreen
import com.example.focusflow.ui.components.InputBottomSheet
import com.example.focusflow.ui.components.BatchSettlementDialog
import com.example.focusflow.ui.screens.*
import com.example.focusflow.ui.theme.*
import com.example.focusflow.ui.viewmodel.SocialViewModel
import com.example.focusflow.ui.components.rememberGlobalErrorToast
import java.util.Locale

// 待执行的专注参数
data class PendingFocusParams(
    val taskName: String,
    val totalMins: Int,
    val focusMins: Int,
    val breakMins: Int,
    val tag: String
)

// 登录状态密封类
sealed class AuthState {
    object Loading : AuthState()
    object NotLoggedIn : AuthState()
    data class LoggedIn(val userId: Long) : AuthState()
}

class MainActivity : ComponentActivity() {

    private var focusService: FocusService? = null
    private var isBound = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, "请开启通知权限以显示倒计时", Toast.LENGTH_SHORT).show()
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as FocusService.LocalBinder
            focusService = binder.getService()
            isBound = true
        }
        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
        }
    }
    
    // 专注完成状态（用于通知 Compose 层）
    private val _focusCompleteEvent = mutableStateOf<Pair<String, Long>?>(null)
    val focusCompleteEvent: MutableState<Pair<String, Long>?> = _focusCompleteEvent
    
    // 广播接收器：处理悬浮窗事件
    private val overlayEventReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                LockOverlayService.ACTION_ABANDON_FOCUS -> {
                    android.util.Log.d("MainActivity", "收到放弃专注广播")
                    focusService?.abandonFocus()
                }
                LockOverlayService.ACTION_FOCUS_COMPLETE -> {
                    val taskName = intent.getStringExtra(LockOverlayService.EXTRA_TASK_NAME) ?: "专注"
                    val durationSeconds = intent.getLongExtra(LockOverlayService.EXTRA_DURATION_SECONDS, 0L)
                    android.util.Log.d("MainActivity", "收到专注完成广播: $taskName, ${durationSeconds}秒")
                    // 通知 Compose 层显示结算对话框
                    _focusCompleteEvent.value = taskName to durationSeconds
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🟢 核心修改：强制设置语言为中文
        val locale = Locale.CHINA
        Locale.setDefault(locale)
        val config = Configuration()
        config.setLocale(locale)
        baseContext.resources.updateConfiguration(config, baseContext.resources.displayMetrics)

        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        Intent(this, FocusService::class.java).also { intent ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            bindService(intent, connection, BIND_AUTO_CREATE)
        }
        
        // 注册广播接收器
        val filter = IntentFilter().apply {
            addAction(LockOverlayService.ACTION_ABANDON_FOCUS)
            addAction(LockOverlayService.ACTION_FOCUS_COMPLETE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(overlayEventReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(overlayEventReceiver, filter)
        }

        // 初始化 ThemeManager
        ThemeManager.initialize(this)
        
        // 启动协程进行数据迁移（仅首次运行）
        lifecycleScope.launch {
            ThemeManager.migrateFromSharedPreferences(this@MainActivity)
        }
        
        setContent {
            // 订阅主题模式 Flow
            val themeMode by ThemeManager.themeModeFlow
                .collectAsState(initial = ThemeMode.FOLLOW_SYSTEM)
            
            // 注册主题切换回调（用于日志记录）
            LaunchedEffect(Unit) {
                ThemeManager.onThemeChange = { mode ->
                    android.util.Log.d("MainActivity", "Theme changed to: $mode")
                }
            }

            AppThemeProvider(themeMode = themeMode) {
                FocusFlowTheme {
                    // 启动页状态
                    var showSplash by remember { mutableStateOf(true) }
                    
                    if (showSplash) {
                        SplashScreen(onSplashFinished = { showSplash = false })
                    } else {
                        MainScreenHolder(
                            getService = { focusService },
                            activity = this
                        )
                    }
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // 不再需要处理无障碍服务弹回
    }
    
    override fun onResume() {
        super.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(overlayEventReceiver)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "注销广播接收器失败", e)
        }
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }
}

@Composable
fun MainScreenHolder(
    getService: () -> FocusService?,
    activity: MainActivity
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val viewModel: MainViewModel = viewModel()
    val socialViewModel: SocialViewModel = viewModel()

    // ══ 全局错误处理 ══
    rememberGlobalErrorToast()

    // ══ 路由守卫：SessionManager 驱动的登录墙 ══
    val sessionManager = remember { SessionManager.getInstance(activity) }
    
    // 使用 mutableStateOf 跟踪登录状态加载状态
    var authState by remember { mutableStateOf<AuthState>(AuthState.Loading) }
    
    // 持续监听 SessionManager，logout 后自动跳回登录页
    LaunchedEffect(Unit) {
        sessionManager.userIdFlow.collect { userId ->
            authState = if (userId != null) AuthState.LoggedIn(userId) else AuthState.NotLoggedIn
        }
    }
    
    // 根据登录状态显示不同界面
    when (authState) {
        AuthState.Loading -> {
            // 加载中，显示空白或加载指示器（不显示登录页）
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CyberBgDeep)
            )
            return
        }
        AuthState.NotLoggedIn -> {
            val coroutineScope = rememberCoroutineScope()
            AuthScreen(onLoginSuccess = { 
                // 登录成功后立即加载用户ID并更新状态
                coroutineScope.launch {
                    val userId = sessionManager.userIdFlow.first()
                    if (userId != null) {
                        authState = AuthState.LoggedIn(userId)
                    }
                }
            })
            return
        }
        is AuthState.LoggedIn -> {
            // 已登录，继续显示主界面
        }
    }

    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedFocusMode by remember { mutableStateOf(focusModes[0]) }
    
    // ══ 专注结算相关 ══
    val focusViewModel: com.example.focusflow.ui.FocusViewModel = viewModel()
    val showSummaryDialog by focusViewModel.showSummaryDialog.collectAsState()
    val settlementResult by focusViewModel.settlementResult.collectAsState()
    
    // ══ 离线专注弹窗 ══
    val showOfflineDialog by focusViewModel.showOfflineDialog.collectAsState()
    val offlineResult by focusViewModel.offlineResult.collectAsState()
    
    // ══ 批量结算弹窗状态 ══
    var batchSettlementResult by remember { mutableStateOf<BatchSettlementResult?>(null) }
    var showBatchSettlementDialog by remember { mutableStateOf(false) }
    
    // ══ 锁屏增强引导状态 ══
    var showLockEnhanceDialog by remember { mutableStateOf(false) }
    var pendingFocusParams by remember { mutableStateOf<PendingFocusParams?>(null) }
    
    // 检测锁屏增强状态
    val lockEnhanceStatus = remember { com.example.focusflow.utils.FocusLockHelper.getLockEnhancementStatus(activity) }
    var currentLockStatus by remember { mutableStateOf(lockEnhanceStatus) }

    // ══ 监听全局批量结算回调 ══
    // 当离线专注同步成功后，MyApplication 会触发此回调
    LaunchedEffect(Unit) {
        MyApplication.onBatchSettlement = { result ->
            android.util.Log.d("MainActivity", "收到批量结算回调: ${result.totalRecords} 条记录")
            batchSettlementResult = result
            showBatchSettlementDialog = true
        }
    }
    
    // ══ 监听悬浮窗专注完成事件 ══
    val focusCompleteEvent by activity.focusCompleteEvent
    LaunchedEffect(focusCompleteEvent) {
        focusCompleteEvent?.let { (taskName, durationSeconds) ->
            android.util.Log.d("MainActivity", "处理专注完成: $taskName, ${durationSeconds}秒")
            // 调用结算流程
            focusViewModel.handleFocusFinish(durationSeconds, taskName)
            // 清除事件
            activity.focusCompleteEvent.value = null
        }
    }
    
    // ══ 监听无障碍服务弹回导航 ══
    // 已移除 - 悬浮窗方案不需要此功能
    
    // ══ 启动时检查专注状态 ══
    // 已移除 - 悬浮窗会自动覆盖，不需要导航
    
    // 清理回调
    DisposableEffect(Unit) {
        onDispose {
            MyApplication.onBatchSettlement = null
        }
    }
    
    val appColors = LocalAppColors.current
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = appColors.bgDeep,
        bottomBar = {
            val isLockScreen = currentRoute?.startsWith("lock") == true
            val isGardenScreen = currentRoute?.startsWith("garden") == true
            val isSocialScreen = currentRoute == "social"
            
            if (!isLockScreen && !isGardenScreen && !isSocialScreen) {
                NavigationBar(
                    containerColor = appColors.cardBg,
                    contentColor = appColors.primary
                ) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, null) },
                        label = { Text("专注") },
                        selected = currentRoute == "home",
                        onClick = { navController.navigate("home") { popUpTo("home") { inclusive = true } } },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = appColors.primary,
                            unselectedIconColor = appColors.textSub,
                            selectedTextColor = appColors.primary,
                            unselectedTextColor = appColors.textSub
                        )
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.BarChart, null) },
                        label = { Text("数据") },
                        selected = currentRoute == "stats",
                        onClick = { navController.navigate("stats") { popUpTo("home") } },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = appColors.primary,
                            unselectedIconColor = appColors.textSub,
                            selectedTextColor = appColors.primary,
                            unselectedTextColor = appColors.textSub
                        )
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Person, null) },
                        label = { Text("我的") },
                        selected = currentRoute == "mine",
                        onClick = { navController.navigate("mine") { popUpTo("home") } },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = appColors.primary,
                            unselectedIconColor = appColors.textSub,
                            selectedTextColor = appColors.primary,
                            unselectedTextColor = appColors.textSub
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavHost(navController = navController, startDestination = "home") {

                composable("home") {
                    HomeScreen(onOpenSetup = {
                        selectedFocusMode = it
                        showBottomSheet = true
                    })
                }

                composable("stats") {
                    StatsScreen(
                        viewModel = viewModel,
                        onNavigateToGarden = { navController.navigate("garden") }
                    )
                }

                composable("bag") {
                    BagScreen(
                        mainViewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable("social") {
                    SocialScreen(
                        onBack = { navController.popBackStack() },
                        onNavigateToFriendGarden = { friend ->
                            socialViewModel.visitFriend(friend)
                            navController.navigate("garden?guestMode=true")
                        },
                        socialViewModel = socialViewModel
                    )
                }

                composable("mine") {
                    MineScreen(
                        viewModel = viewModel
                    )
                }

                composable(
                    route = "garden?guestMode={guestMode}",
                    arguments = listOf(
                        navArgument("guestMode") { 
                            type = NavType.BoolType
                            defaultValue = false 
                        }
                    )
                ) { backStackEntry ->
                    val guestMode = backStackEntry.arguments?.getBoolean("guestMode") ?: false
                    GardenScreen(
                        guestMode = guestMode,
                        onBack = { 
                            if (guestMode) {
                                socialViewModel.exitVisit()
                            }
                            navController.popBackStack() 
                        },
                        onNavigateToBag = { navController.navigate("bag") },
                        onNavigateToStats = { navController.navigate("stats") { popUpTo("home") } },
                        onNavigateToSocial = { navController.navigate("social") },
                        socialViewModel = socialViewModel
                    )
                }
            }

            if (showBottomSheet) {
                InputBottomSheet(
                    isOpen = true,
                    initialMode = selectedFocusMode,
                    allModes = focusModes,
                    onDismiss = { showBottomSheet = false },
                    onStartFocus = { taskName, totalMins, focusMins, breakMins, tag ->
                        showBottomSheet = false
                        
                        // 检测双权限（悬浮窗 + 使用情况访问）
                        val currentStatus = com.example.focusflow.utils.FocusLockHelper.getLockEnhancementStatus(activity)
                        
                        if (!currentStatus.isFullyProtected) {
                            // 未完全开启，显示引导对话框
                            pendingFocusParams = PendingFocusParams(taskName, totalMins, focusMins, breakMins, tag)
                            showLockEnhanceDialog = true
                        } else {
                            // 已开启悬浮窗权限，直接开始专注（悬浮窗会显示）
                            try {
                                val service = getService()
                                if (service != null) {
                                    android.util.Log.d("MainActivity", "开始专注: $taskName, $totalMins min")
                                    service.startFocus(totalMins, focusMins, breakMins, taskName)
                                    // 悬浮窗会自动显示，不需要导航到应用内锁屏
                                } else {
                                    Toast.makeText(activity, "服务未就绪，请稍后重试", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("MainActivity", "开始专注失败", e)
                                Toast.makeText(activity, "启动失败: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
            
            // ══ 锁屏增强引导弹窗 ══
            if (showLockEnhanceDialog && pendingFocusParams != null) {
                LockEnhanceGuideDialog(
                    onSkip = {
                        // 双权限已开启或用户点击已完成，开始专注
                        showLockEnhanceDialog = false
                        val params = pendingFocusParams
                        pendingFocusParams = null
                        try {
                            val service = getService()
                            if (service != null && params != null) {
                                android.util.Log.d("MainActivity", "权限已就绪，开始专注: ${params.taskName}")
                                service.startFocus(params.totalMins, params.focusMins, params.breakMins, params.taskName)
                                // 悬浮窗会自动显示，不需要导航到应用内锁屏
                            } else {
                                Toast.makeText(activity, "服务未就绪，请稍后重试", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("MainActivity", "开始专注失败", e)
                            Toast.makeText(activity, "启动失败: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onOpenSettings = {
                        // 打开设置页面（保留 pendingFocusParams，用户返回后可以继续）
                        showLockEnhanceDialog = false
                        com.example.focusflow.utils.FocusLockHelper.openOverlaySettings(activity)
                    }
                )
            }
            
            // ══ 单次专注结算弹窗 ══
            if (showSummaryDialog && settlementResult != null) {
                com.example.focusflow.ui.components.SettlementDialog(
                    result = settlementResult!!,
                    onConfirm = {
                        focusViewModel.dismissSummaryDialog()
                    },
                    onNavigateToBag = {
                        focusViewModel.dismissSummaryDialog()
                        navController.navigate("bag") {
                            popUpTo("home")
                        }
                    }
                )
            }
            
            // ══ 离线专注弹窗 ══
            if (showOfflineDialog && offlineResult != null) {
                com.example.focusflow.ui.components.OfflineFocusDialog(
                    taskName = offlineResult!!.taskName,
                    durationMinutes = offlineResult!!.durationMinutes,
                    onConfirm = {
                        focusViewModel.dismissOfflineDialog()
                    }
                )
            }
            
            // ══ 批量结算弹窗 ══
            if (showBatchSettlementDialog && batchSettlementResult != null) {
                BatchSettlementDialog(
                    result = batchSettlementResult!!,
                    onConfirm = {
                        showBatchSettlementDialog = false
                        batchSettlementResult = null
                    },
                    onNavigateToBag = {
                        showBatchSettlementDialog = false
                        batchSettlementResult = null
                        navController.navigate("bag") {
                            popUpTo("home")
                        }
                    }
                )
            }
            
        }
    }
}

/**
 * 锁屏增强引导弹窗 - 双权限引导（悬浮窗 + 使用情况访问）
 */
@Composable
fun LockEnhanceGuideDialog(
    onSkip: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = context as? androidx.lifecycle.LifecycleOwner
    
    // 实时检测双权限状态
    var overlayEnabled by remember { mutableStateOf(com.example.focusflow.utils.FocusLockHelper.canDrawOverlays(context)) }
    var usageStatsEnabled by remember { mutableStateOf(com.example.focusflow.utils.FocusLockHelper.hasUsageStatsPermission(context)) }
    
    // 刷新权限状态的函数
    fun refreshPermissions() {
        overlayEnabled = com.example.focusflow.utils.FocusLockHelper.canDrawOverlays(context)
        usageStatsEnabled = com.example.focusflow.utils.FocusLockHelper.hasUsageStatsPermission(context)
        android.util.Log.d("LockEnhanceGuide", "权限刷新: overlay=$overlayEnabled, usageStats=$usageStatsEnabled")
    }
    
    // 监听生命周期，在 onResume 时刷新权限状态
    if (lifecycleOwner != null) {
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    refreshPermissions()
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }
    }
    
    // 双权限都已开启时自动关闭弹窗并继续
    LaunchedEffect(overlayEnabled, usageStatsEnabled) {
        if (overlayEnabled && usageStatsEnabled) {
            android.util.Log.d("LockEnhanceGuide", "双权限已开启，自动继续")
            onSkip()
        }
    }
    LaunchedEffect(overlayEnabled, usageStatsEnabled) {
        if (overlayEnabled && usageStatsEnabled) {
            android.util.Log.d("LockEnhanceGuide", "双权限已开启，自动继续")
            onSkip()
        }
    }
    
    AlertDialog(
        onDismissRequest = onSkip,
        containerColor = LocalAppColors.current.cardBg,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = "🛡️ 开启防逃逸保护",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "开启以下权限，确保专注时不被干扰：",
                    color = CyberTextSub,
                    fontSize = 14.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 权限1：悬浮窗权限
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = if (overlayEnabled) 
                                LocalAppColors.current.primary.copy(alpha = 0.15f) 
                            else 
                                LocalAppColors.current.cardBg.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (overlayEnabled) "✅" else "❌",
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "悬浮窗权限",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "用于显示全屏锁屏覆盖层",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                    if (!overlayEnabled) {
                        TextButton(onClick = {
                            com.example.focusflow.utils.FocusLockHelper.openOverlaySettings(context)
                        }) {
                            Text("开启", color = CyberPrimary, fontSize = 12.sp)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 权限2：使用情况访问权限
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = if (usageStatsEnabled) 
                                LocalAppColors.current.primary.copy(alpha = 0.15f) 
                            else 
                                LocalAppColors.current.cardBg.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (usageStatsEnabled) "✅" else "❌",
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "使用情况访问权限",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "用于检测并阻止逃逸行为",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                    if (!usageStatsEnabled) {
                        TextButton(onClick = {
                            com.example.focusflow.utils.FocusLockHelper.openUsageStatsSettings(context)
                        }) {
                            Text("开启", color = CyberPrimary, fontSize = 12.sp)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 使用情况访问权限的特殊说明
                if (!usageStatsEnabled) {
                    Column(
                        modifier = Modifier
                            .background(
                                color = LocalAppColors.current.bgDeep,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "📌 开启步骤：",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "1. 点击上方「开启」进入系统设置\n" +
                                  "2. 找到 FocusFlow 并开启权限\n" +
                                  "3. 返回应用即可生效",
                            color = CyberPrimary,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "💡 提示：开启所有权限可获得最佳防逃逸保护",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // 刷新权限状态
                    overlayEnabled = com.example.focusflow.utils.FocusLockHelper.canDrawOverlays(context)
                    usageStatsEnabled = com.example.focusflow.utils.FocusLockHelper.hasUsageStatsPermission(context)
                    
                    if (overlayEnabled && usageStatsEnabled) {
                        // 双权限已开启，关闭对话框继续
                        onSkip()
                    } else {
                        // 未完全开启，打开对应设置
                        if (!overlayEnabled) {
                            com.example.focusflow.utils.FocusLockHelper.openOverlaySettings(context)
                        } else {
                            com.example.focusflow.utils.FocusLockHelper.openUsageStatsSettings(context)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (overlayEnabled && usageStatsEnabled) 
                        CyberPrimary 
                    else 
                        LocalAppColors.current.textSub.copy(alpha = 0.3f)
                )
            ) {
                Text(
                    if (overlayEnabled && usageStatsEnabled) "已完成" else "去开启",
                    color = if (overlayEnabled && usageStatsEnabled) Color.Black else Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onSkip) {
                Text("暂时跳过", color = CyberTextSub)
            }
        }
    )
}
