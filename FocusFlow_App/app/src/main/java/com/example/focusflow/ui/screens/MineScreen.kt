package com.example.focusflow.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PhoneIphone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.focusflow.api.UserPreferences
import com.example.focusflow.ui.MainViewModel
import com.example.focusflow.ui.theme.*
import com.example.focusflow.ui.viewmodel.ProfileViewModel
import com.example.focusflow.ui.components.AvatarPickerBottomSheet
import com.example.focusflow.ui.components.MockTotems
import com.example.focusflow.ui.components.CyberScrollingNumber
import com.example.focusflow.ui.components.SyncStatusIndicator
import com.example.focusflow.ui.components.SkeletonBox
import com.example.focusflow.ui.components.SkeletonCircle
import com.example.focusflow.ui.components.PullRefreshLayout
import com.example.focusflow.service.TimeFluxSyncService
import com.example.focusflow.utils.SoundManager
import com.example.focusflow.utils.FocusLockHelper
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

// 定义我的页面内部的子路由
enum class MineSubScreen {
    Main,
    UserProfile,
    GeneralSettings
}

@Composable
fun MineScreen(
    viewModel: MainViewModel = viewModel(),
    profileViewModel: ProfileViewModel = viewModel()
) {
    val appColors = LocalAppColors.current
    var currentSubScreen by remember { mutableStateOf(MineSubScreen.Main) }

    // ═══════════════════════════════════════════════════════════════
    // 每次进入个人终端时，从云端刷新用户数据
    // ═══════════════════════════════════════════════════════════════
    LaunchedEffect(Unit) {
        profileViewModel.refreshFromCloud()
    }

    BackHandler(enabled = currentSubScreen != MineSubScreen.Main) {
        currentSubScreen = MineSubScreen.Main
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appColors.bgDeep)
            .statusBarsPadding()
    ) {
        when (currentSubScreen) {
            MineSubScreen.Main -> MineMainContent(
                viewModel = viewModel,
                profileViewModel = profileViewModel,
                onNavigateToProfile = { currentSubScreen = MineSubScreen.UserProfile },
                onNavigateToSettings = { currentSubScreen = MineSubScreen.GeneralSettings }
            )
            MineSubScreen.UserProfile -> UserProfileContent(
                viewModel = viewModel,
                profileViewModel = profileViewModel,
                onBack = { currentSubScreen = MineSubScreen.Main }
            )
            MineSubScreen.GeneralSettings -> GeneralSettingsContent(
                profileViewModel = profileViewModel,
                onBack = { currentSubScreen = MineSubScreen.Main }
            )
        }
    }
}

// 1. 个人中心主页
@Composable
fun MineMainContent(
    viewModel: MainViewModel,
    profileViewModel: ProfileViewModel,
    onNavigateToProfile: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val appColors = LocalAppColors.current
    
    // ═══════════════════════════════════════════════════════════════
    // 用户数据（从云端加载，不使用 Room）
    // ═══════════════════════════════════════════════════════════════
    val userId by profileViewModel.userId.collectAsState()
    val account by profileViewModel.account.collectAsState()
    val nickname by profileViewModel.nickname.collectAsState()
    val avatarId by profileViewModel.avatarId.collectAsState()
    val timeFlux by profileViewModel.timeFlux.collectAsState()
    val streakDays by profileViewModel.streakDays.collectAsState()
    val isLoading by profileViewModel.isLoading.collectAsState()
    val isConnected by profileViewModel.isConnected.collectAsState() // 🟢 [OFFLINE] 云端连接状态

    // 🟢 [PULL TO REFRESH] 下拉刷新状态
    val isRefreshing by profileViewModel.isRefreshing.collectAsState()

    val selectedAvatarId by profileViewModel.temporarySelectedAvatarId.collectAsState()
    var showEditDialog by remember { mutableStateOf(false) }
    var showAvatarPicker by remember { mutableStateOf(false) }

    // ───────────── 光流同步服务 ─────────────
    val syncService = remember { TimeFluxSyncService.getInstance(context) }
    val syncTimeFlux by syncService.timeFlux.collectAsState()
    val syncStatus by syncService.syncStatus.collectAsState()
    val pendingCount by syncService.pendingSyncCount.collectAsState()
    val lastSyncTime by syncService.lastSyncTime.collectAsState()

    // 启动同步服务
    LaunchedEffect(Unit) {
        syncService.startSync()
    }

    // 🟢 [PULL TO REFRESH] 下拉刷新容器
    PullRefreshLayout(
        isRefreshing = isRefreshing,
        onRefresh = { profileViewModel.refreshFromCloud() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
        // 标题
        Column {
            Text("个人终端", color = appColors.primary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("PERSONAL TERMINAL", color = appColors.textSub, fontSize = 12.sp, letterSpacing = 2.sp)
        }

        Spacer(modifier = Modifier.height(30.dp))

        // --- 赛博玻璃拟态身份卡 (Glass ID Card) ---
        if (userId != null) {
            GlassIdentityCard(
                nickname = nickname,
                account = account,
                avatarId = avatarId,
                onEditClick = { 
                    profileViewModel.updateTemporaryAvatar(avatarId)
                    showAvatarPicker = true 
                }
            )
        } else if (isLoading) {
            // 骨架屏加载状态
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 头像骨架
                SkeletonCircle(size = 72.dp)
                Spacer(modifier = Modifier.width(16.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    // 昵称骨架
                    SkeletonBox(width = 100.dp, height = 20.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    // 账号骨架
                    SkeletonBox(width = 80.dp, height = 14.dp)
                }
            }
        } else {
            // 未登录状态 - 显示登录按钮
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                    .clickable { 
                        // 跳转到登录页面（通过退出 session 触发 AuthScreen 显示）
                        profileViewModel.logout()
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("请先登录", color = CyberTextSub, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("点击此处登录", color = CyberPrimary, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- 光流余额赛博滚动数字组件 ---
        CyberScrollingNumber(
            value = syncTimeFlux ?: timeFlux,
            modifier = Modifier.fillMaxWidth(),
            isConnected = isConnected // 🟢 [OFFLINE] 传递连接状态
        )

        Spacer(modifier = Modifier.height(12.dp))

        // --- 同步状态指示器 ---
        SyncStatusIndicator(
            isSyncing = syncStatus is TimeFluxSyncService.SyncStatus.Syncing,
            pendingCount = pendingCount,
            lastSyncTime = lastSyncTime,
            onRefresh = { syncService.triggerManualSync() },
            modifier = Modifier.fillMaxWidth()
        )

        // --- 连续专注天数 ---
        if (streakDays > 0) {
            Spacer(modifier = Modifier.height(12.dp))
            val streakColor = Color(0xFFEAB308) // 保留金黄色用于连续专注
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                streakColor.copy(alpha = 0.15f),
                                Color(0xFFF97316).copy(alpha = 0.08f)
                            )
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(1.dp, streakColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🔥", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "连续专注",
                    color = appColors.textMain.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "$streakDays",
                    color = streakColor,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "天",
                    color = streakColor.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    when {
                        streakDays >= 30 -> "月度冠军！"
                        streakDays >= 7 -> "周度冠军！"
                        streakDays >= 3 -> "坚持中！"
                        else -> "加油！"
                    },
                    color = streakColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // --- 功能菜单 ---
        Text("系统功能 SYSTEM", color = appColors.textSub, fontSize = 14.sp, modifier = Modifier.padding(bottom = 12.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(appColors.cardBg, RoundedCornerShape(16.dp))
                .border(1.dp, appColors.textSub.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                .padding(vertical = 8.dp)
        ) {
            // 通用设置
            SettingArrowItem(
                icon = Icons.Default.Settings,
                title = "通用设置",
                onClick = { onNavigateToSettings() }
            )

            HorizontalDivider(color = appColors.textSub.copy(alpha = 0.2f), thickness = 1.dp)

            SettingArrowItem(
                icon = Icons.Default.Info,
                title = "关于 Focus Flow",
                onClick = { }
            )
        }

        // 底部留白
        Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // 2. 量子图腾选择器 (BottomSheet)
    if (showAvatarPicker && userId != null) {
        AvatarPickerBottomSheet(
            selectedId = selectedAvatarId,
            onAvatarSelect = { profileViewModel.updateTemporaryAvatar(it) },
            onConfirm = {
                profileViewModel.saveAvatar()
                showAvatarPicker = false
            },
            onEditNickname = {
                showEditDialog = true
            },
            onDismiss = { showAvatarPicker = false }
        )
    }

    // 1. 覆写数字标识弹窗 (Nickname Dialog)
    if (showEditDialog && userId != null) {
        OverrideIdentityDialog(
            currentNickname = nickname,
            onDismiss = { showEditDialog = false },
            onConfirm = { newName ->
                profileViewModel.updateNickname(newName)
                showEditDialog = false
            }
        )
    }
}

/**
 * 赛博玻璃拟态身份卡
 */
@Composable
fun GlassIdentityCard(
    nickname: String,
    account: String,
    avatarId: Int,
    onEditClick: () -> Unit
) {
    val appColors = LocalAppColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        appColors.cardBg.copy(alpha = if (appColors.isDark) 0.8f else 1f),
                        appColors.cardBg.copy(alpha = if (appColors.isDark) 0.5f else 0.95f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .border(
                width = 2.dp,
                color = appColors.primary.copy(alpha = 0.4f),
                shape = RoundedCornerShape(24.dp)
            )
            .clip(RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：带发光边框的圆形头像 (赛博几何图形)
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(appColors.bgDeep.copy(alpha = 0.6f), CircleShape)
                    .border(2.dp, appColors.primary, CircleShape)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                // 根据 avatarId 渲染不同几何图形
                val totem = MockTotems.find { it.id == avatarId } ?: MockTotems.first()
                Text(
                    text = totem.emoji,
                    fontSize = 38.sp
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            // 右侧：身份详情
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = nickname,
                    color = appColors.textMain,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "ACCOUNT: $account",
                    color = appColors.textSub,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Light,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // 编辑按钮
        IconButton(
            onClick = onEditClick,
            modifier = Modifier.align(Alignment.TopEnd).offset(x = 12.dp, y = (-12).dp)
        ) {
            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = appColors.primary.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
        }
    }
}

/**
 * 覆写数字标识弹窗
 */
@Composable
fun OverrideIdentityDialog(
    currentNickname: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val appColors = LocalAppColors.current
    var text by remember { mutableStateOf(currentNickname) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = appColors.cardBg,
        shape = RoundedCornerShape(20.dp),
        title = {
            Column {
                Text("覆写数字标识", color = appColors.primary, fontWeight = FontWeight.Bold)
                Text("OVERWRITE IDENTIFIER", color = appColors.textSub, fontSize = 10.sp)
            }
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { if (it.length <= 16) text = it },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = appColors.primary,
                    unfocusedBorderColor = appColors.textSub,
                    focusedTextColor = appColors.textMain
                ),
                singleLine = true,
                placeholder = { Text("输入新代号", color = appColors.textSub) }
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(text) },
                colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary)
            ) {
                Text("确认覆写", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Color.Gray)
            }
        }
    )
}

// UserProfileContent 和 GeneralSettingsContent 保持不变
@Composable
fun UserProfileContent(
    viewModel: MainViewModel,
    profileViewModel: ProfileViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val avatarId by profileViewModel.avatarId.collectAsState()
    val nickname by profileViewModel.nickname.collectAsState()
    var showEditDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        SimpleTopBar(title = "个人信息", onBack = onBack)

        Column(modifier = Modifier.padding(24.dp)) {
            ProfileItemRow(title = "头像", onClick = { }) {
                val totem = MockTotems.find { it.id == avatarId } ?: MockTotems.first()
                Text(totem.emoji, fontSize = 24.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
            ProfileItemRow(title = "昵称", onClick = { showEditDialog = true }) {
                Text(nickname, color = Color.White, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
            ProfileItemRow(title = "手机号码", onClick = { }) {
                Text("138****8888", color = CyberTextSub, fontSize = 16.sp)
            }
        }
    }

    if (showEditDialog) {
        var tempName by remember { mutableStateOf(nickname) }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = CyberCardBg,
            title = { Text("修改昵称", color = CyberPrimary) },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberPrimary,
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    profileViewModel.updateNickname(tempName)
                    showEditDialog = false
                }) { Text("保存", color = CyberPrimary) }
            }
        )
    }
}

@Composable
fun GeneralSettingsContent(
    profileViewModel: ProfileViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var soundEnabled by remember { mutableStateOf(UserPreferences.isSoundEnabled(context)) }
    var vibrationEnabled by remember { mutableStateOf(UserPreferences.isVibrationEnabled(context)) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    val operationMessage by profileViewModel.operationMessage.collectAsState()
    
    // 锁屏增强状态
    var lockStatus by remember { mutableStateOf(FocusLockHelper.getLockEnhancementStatus(context)) }
    var showLockEnhanceDialog by remember { mutableStateOf(false) }

    // 每次进入页面时刷新状态
    LaunchedEffect(Unit) {
        lockStatus = FocusLockHelper.getLockEnhancementStatus(context)
        soundEnabled = UserPreferences.isSoundEnabled(context)
        vibrationEnabled = UserPreferences.isVibrationEnabled(context)
    }

    // 显示操作消息
    LaunchedEffect(operationMessage) {
        operationMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            profileViewModel.clearOperationMessage()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SimpleTopBar(title = "通用设置", onBack = onBack)

        Column(
            modifier = Modifier
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("偏好设置", color = CyberTextSub, fontSize = 14.sp, modifier = Modifier.padding(bottom = 12.dp))
            
            // 主题设置（导航到专门的主题设置页）
            SettingArrowItem(
                icon = Icons.Default.Brightness6,
                title = "主题设置",
                subtitle = "跟随系统 / 深色 / 浅色"
            ) {
                // TODO: 导航到 ThemeSettingsScreen
                // 暂时保留旧的切换逻辑作为临时方案
                val currentMode = runBlocking { ThemeManager.getThemeMode() }
                val nextMode = when (currentMode) {
                    ThemeMode.FOLLOW_SYSTEM -> ThemeMode.FORCE_DARK
                    ThemeMode.FORCE_DARK -> ThemeMode.FORCE_LIGHT
                    ThemeMode.FORCE_LIGHT -> ThemeMode.FOLLOW_SYSTEM
                }
                scope.launch {
                    ThemeManager.setThemeMode(nextMode, context)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 音效开关
            SettingSwitchItem(
                icon = Icons.Default.VolumeUp,
                title = "音效反馈",
                checked = soundEnabled,
                onCheckedChange = {
                    soundEnabled = it
                    UserPreferences.setSoundEnabled(context, it)
                    // 同步更新 SoundManager
                    scope.launch {
                        SoundManager.setSoundEnabled(context, it)
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 震动开关
            SettingSwitchItem(
                icon = Icons.Default.Vibration,
                title = "震动反馈",
                checked = vibrationEnabled,
                onCheckedChange = {
                    vibrationEnabled = it
                    UserPreferences.setVibrationEnabled(context, it)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text("专注锁屏", color = CyberTextSub, fontSize = 14.sp, modifier = Modifier.padding(bottom = 12.dp))
            
            // 锁屏增强设置项
            SettingArrowItem(
                icon = Icons.Default.Security,
                title = "锁屏增强保护",
                subtitle = lockStatus.statusText
            ) {
                showLockEnhanceDialog = true
            }
            
            // 提示文字
            if (!lockStatus.isFullyProtected) {
                Text(
                    text = "💡 开启后，专注时将无法切出应用",
                    color = CyberTextSub,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("安全设置", color = CyberTextSub, fontSize = 14.sp, modifier = Modifier.padding(bottom = 12.dp))
            SettingArrowItem(
                icon = Icons.Default.Lock,
                title = "修改密码"
            ) {
                showChangePasswordDialog = true
            }

            Spacer(modifier = Modifier.height(40.dp))

            // 退出终端按钮
            OutlinedButton(
                onClick = { profileViewModel.logout() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f))
            ) {
                Text("退出终端 (LOGOUT)", fontWeight = FontWeight.Bold)
            }
        }
    }

    // 修改密码弹窗
    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showChangePasswordDialog = false },
            onConfirm = { oldPassword, newPassword ->
                profileViewModel.changePassword(oldPassword, newPassword) {
                    showChangePasswordDialog = false
                }
            }
        )
    }
    
    // 锁屏增强引导弹窗
    if (showLockEnhanceDialog) {
        LockEnhanceDialog(
            currentStatus = lockStatus,
            onOpenOverlay = {
                FocusLockHelper.openOverlaySettings(context)
            },
            onOpenUsageStats = {
                FocusLockHelper.openUsageStatsSettings(context)
            },
            onDismiss = {
                showLockEnhanceDialog = false
                // 返回时刷新状态
                lockStatus = FocusLockHelper.getLockEnhancementStatus(context)
            }
        )
    }
}

// 辅助组件
@Composable
fun SimpleTopBar(title: String, onBack: () -> Unit) {
    val appColors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = appColors.textMain) }
        Text(title, color = appColors.textMain, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ProfileItemRow(title: String, onClick: () -> Unit, content: @Composable () -> Unit) {
    val appColors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.background(appColors.cardBg, RoundedCornerShape(12.dp)).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, color = appColors.textMain, fontSize = 16.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            content()
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.ChevronRight, null, tint = appColors.textSub)
        }
    }
}

@Composable
fun SettingSwitchItem(icon: ImageVector, title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val appColors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = appColors.textSub, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, color = appColors.textMain, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = appColors.primary, 
                checkedTrackColor = appColors.primary.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
fun SettingArrowItem(
    icon: ImageVector, 
    title: String, 
    subtitle: String? = null,
    onClick: () -> Unit
) {
    val appColors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(appColors.cardBg, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = appColors.textSub)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = appColors.textMain, fontSize = 16.sp)
            if (subtitle != null) {
                Text(subtitle, color = appColors.textSub, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = appColors.textSub)
    }
}

/**
 * 修改密码弹窗
 */
@Composable
fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (oldPassword: String, newPassword: String) -> Unit
) {
    val appColors = LocalAppColors.current
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showOldPassword by remember { mutableStateOf(false) }
    var showNewPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = appColors.cardBg,
        title = {
            Text(
                "修改密码",
                color = appColors.primary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 原密码
                OutlinedTextField(
                    value = oldPassword,
                    onValueChange = { oldPassword = it },
                    label = { Text("原密码", color = CyberTextSub) },
                    singleLine = true,
                    visualTransformation = if (showOldPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(onClick = { showOldPassword = !showOldPassword }) {
                            Text(
                                if (showOldPassword) "隐藏" else "显示",
                                color = CyberPrimary,
                                fontSize = 12.sp
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CyberPrimary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // 新密码
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("新密码", color = CyberTextSub) },
                    singleLine = true,
                    visualTransformation = if (showNewPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(onClick = { showNewPassword = !showNewPassword }) {
                            Text(
                                if (showNewPassword) "隐藏" else "显示",
                                color = CyberPrimary,
                                fontSize = 12.sp
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CyberPrimary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // 确认新密码
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("确认新密码", color = CyberTextSub) },
                    singleLine = true,
                    visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                            Text(
                                if (showConfirmPassword) "隐藏" else "显示",
                                color = CyberPrimary,
                                fontSize = 12.sp
                            )
                        }
                    },
                    isError = confirmPassword.isNotEmpty() && confirmPassword != newPassword,
                    supportingText = {
                        if (confirmPassword.isNotEmpty() && confirmPassword != newPassword) {
                            Text("两次密码不一致", color = Color.Red)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CyberPrimary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        errorBorderColor = Color.Red
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    "密码长度需在6-32位之间",
                    color = CyberTextSub,
                    fontSize = 12.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (oldPassword.length < 6 || newPassword.length < 6) {
                        return@Button
                    }
                    if (newPassword != confirmPassword) {
                        return@Button
                    }
                    onConfirm(oldPassword, newPassword)
                },
                enabled = oldPassword.length >= 6 &&
                        newPassword.length >= 6 &&
                        newPassword == confirmPassword,
                colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary)
            ) {
                Text("确认修改", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = CyberTextSub)
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

/**
 * 锁屏增强引导弹窗
 */
@Composable
fun LockEnhanceDialog(
    currentStatus: com.example.focusflow.utils.LockEnhancementStatus,
    onOpenOverlay: () -> Unit,
    onOpenUsageStats: () -> Unit,
    onDismiss: () -> Unit
) {
    val appColors = LocalAppColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = appColors.cardBg,
        title = {
            Text("🛡️ 锁屏增强保护", color = appColors.textMain, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    text = "开启以下权限后，专注时将无法切出应用，实现真正的防分心保护。",
                    color = appColors.textSub,
                    fontSize = 14.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 悬浮窗权限状态
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(appColors.bgDeep, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Icon(
                        imageVector = if (currentStatus.overlayEnabled) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = null,
                        tint = if (currentStatus.overlayEnabled) appColors.primary else Color(0xFFFF4B4B),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "悬浮窗权限",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (currentStatus.overlayEnabled) "已开启" else "用于显示锁屏覆盖层",
                            color = if (currentStatus.overlayEnabled) CyberPrimary else Color.Red,
                            fontSize = 12.sp
                        )
                    }
                    if (!currentStatus.overlayEnabled) {
                        TextButton(onClick = onOpenOverlay) {
                            Text("去开启", color = appColors.primary, fontSize = 12.sp)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 使用情况访问权限状态
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(appColors.bgDeep, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Icon(
                        imageVector = if (currentStatus.usageStatsEnabled) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = null,
                        tint = if (currentStatus.usageStatsEnabled) appColors.primary else Color(0xFFFF4B4B),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "使用情况访问权限",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (currentStatus.usageStatsEnabled) "已开启" else "用于检测前台应用",
                            color = if (currentStatus.usageStatsEnabled) CyberPrimary else Color.Red,
                            fontSize = 12.sp
                        )
                    }
                    if (!currentStatus.usageStatsEnabled) {
                        TextButton(onClick = onOpenUsageStats) {
                            Text("去开启", color = CyberPrimary, fontSize = 12.sp)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 设置提示
                if (!currentStatus.isFullyProtected) {
                    Text(
                        text = "💡 设置提示：",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• 悬浮窗：找到 FocusFlow → 开启「显示在其他应用上层」\n• 使用情况：找到 FocusFlow → 开启「允许查看使用情况」",
                        color = CyberTextSub,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary)
            ) {
                Text(if (currentStatus.isFullyProtected) "完成" else "稍后设置", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭", color = CyberTextSub)
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}