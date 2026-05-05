package com.example.focusflow.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.example.focusflow.service.NetworkMonitor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.focusflow.ui.components.FriendRequestDialog
import com.example.focusflow.ui.components.VisitLogBottomSheet
import com.example.focusflow.ui.components.SkeletonList
import com.example.focusflow.ui.components.SkeletonListItem
import com.example.focusflow.ui.components.PullRefreshLayout
import com.example.focusflow.ui.components.SimpleRefreshIndicator
import com.example.focusflow.ui.theme.*
import com.example.focusflow.ui.viewmodel.Friend
import com.example.focusflow.ui.viewmodel.SocialViewModel
import kotlinx.coroutines.launch

/**
 * SocialScreen —— 量子通讯录
 */
@Composable
fun SocialScreen(
    onBack: () -> Unit,
    onNavigateToFriendGarden: (Friend) -> Unit,
    socialViewModel: SocialViewModel = viewModel()
) {
    val context = LocalContext.current
    val appColors = LocalAppColors.current
    val networkMonitor = remember { NetworkMonitor.getInstance(context) }
    val networkStatus by networkMonitor.networkStatus.collectAsState()
    
    // 确保监控已启动
    LaunchedEffect(Unit) {
        networkMonitor.startMonitoring()
    }
    
    // 进入页面时刷新数据（确保好友列表显示）
    LaunchedEffect(Unit) {
        socialViewModel.loadAllData()
    }
    
    val friends by socialViewModel.friends.collectAsState()
    val visitLogs by socialViewModel.visitLogs.collectAsState()
    val friendRequests by socialViewModel.friendRequests.collectAsState()
    val hasUnreadMessages by socialViewModel.hasUnreadMessages.collectAsState()
    val isLoadingFriends by socialViewModel.isLoadingFriends.collectAsState()
    
    // 搜索相关状态
    val searchResult by socialViewModel.searchResult.collectAsState()
    val isSearching by socialViewModel.isSearching.collectAsState()
    val searchError by socialViewModel.searchError.collectAsState()
    
    // 操作反馈消息
    val actionMessage by socialViewModel.actionMessage.collectAsState()
    
    // 下拉刷新状态
    val isRefreshing by socialViewModel.isRefreshing.collectAsState()

    // 本地 UI 状态
    var searchQuery by remember { mutableStateOf("") }
    var isSearchMode by remember { mutableStateOf(false) }
    var addFriendSuccess by remember { mutableStateOf(false) }
    var showVisitLogSheet by remember { mutableStateOf(false) }
    var showFriendRequestDialog by remember { mutableStateOf(false) }
    
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 监听 actionMessage 并显示 Snackbar
    LaunchedEffect(actionMessage) {
        actionMessage?.let { message ->
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Short
                )
            }
            socialViewModel.clearActionMessage()
        }
    }

    // 本地搜索过滤
    val filteredFriends = remember(friends, searchQuery, isSearchMode) {
        if (isSearchMode) emptyList()
        else if (searchQuery.isBlank()) friends
        else friends.filter { it.nickname.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = 80.dp)
            ) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = appColors.cardBg,
                    contentColor = appColors.primary,
                    actionColor = CyberSecondary,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(appColors.bgDeep)
                .statusBarsPadding()
                .padding(paddingValues)
        ) {
            // 统一下拉刷新
            PullRefreshLayout(
                isRefreshing = isRefreshing,
                onRefresh = { socialViewModel.refresh() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, "Back", tint = appColors.textMain)
                    }
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("量子通讯录", color = appColors.primary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("SOCIAL FREQUENCY TERMINAL", color = appColors.textSub, fontSize = 10.sp, letterSpacing = 2.sp)
                    }
                }

                Spacer(Modifier.height(20.dp))

                // 搜索栏（支持搜索好友和添加新好友）
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .background(
                                color = appColors.cardBg.copy(alpha = if (appColors.isDark) 0.5f else 1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = appColors.primary.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.Search,
                                contentDescription = null,
                                tint = appColors.primary.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        "输入账号搜索添加好友",
                                        color = appColors.textSub.copy(alpha = 0.6f),
                                        fontSize = 13.sp
                                    )
                                }
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { 
                                        searchQuery = it
                                        isSearchMode = false
                                        socialViewModel.clearSearch()
                                        addFriendSuccess = false
                                    },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        color = appColors.textMain,
                                        fontSize = 13.sp
                                    ),
                                    cursorBrush = SolidColor(appColors.primary),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(
                                        onSearch = {
                                            if (searchQuery.isNotBlank()) {
                                                isSearchMode = true
                                                socialViewModel.searchUser(searchQuery)
                                                focusManager.clearFocus()
                                            }
                                        }
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                    // 搜索按钮
                    Button(
                        onClick = {
                            if (searchQuery.isNotBlank()) {
                                isSearchMode = true
                                socialViewModel.searchUser(searchQuery)
                                focusManager.clearFocus()
                            }
                        },
                        enabled = searchQuery.isNotBlank() && !isSearching,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = appColors.primary,
                            disabledContainerColor = appColors.cardBg.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = if (appColors.isDark) Color.Black else Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("搜索", color = if (appColors.isDark) Color.Black else Color.White, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // 搜索结果区域
                if (isSearchMode) {
                    when {
                        addFriendSuccess -> {
                            Surface(
                                color = appColors.primary.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, appColors.primary.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("✓", fontSize = 24.sp, color = appColors.primary)
                                    Spacer(Modifier.width(12.dp))
                                    Text("好友申请已发送", color = appColors.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.weight(1f))
                                    TextButton(onClick = {
                                        isSearchMode = false
                                        searchQuery = ""
                                        addFriendSuccess = false
                                        socialViewModel.clearSearch()
                                    }) {
                                        Text("关闭", color = appColors.textSub, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                        searchResult != null -> {
                            Surface(
                                color = appColors.primary.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, appColors.primary.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape)
                                            .background(appColors.primary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(searchResult!!.avatarEmoji, fontSize = 26.sp)
                                    }
                                    Spacer(Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(searchResult!!.nickname, color = appColors.textMain, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                        Text("找到用户", color = appColors.textSub, fontSize = 11.sp)
                                    }
                                    Button(
                                        onClick = {
                                            socialViewModel.sendFriendRequest(
                                                friendId = searchResult!!.userId,
                                                onSuccess = { addFriendSuccess = true },
                                                onError = { /* 已由 ViewModel 处理 */ }
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = appColors.primary),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text("添加好友", color = if (appColors.isDark) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                        searchError != null -> {
                            val errorColor = Color(0xFFFF4B4B) // 保留错误红色
                            Surface(
                                color = errorColor.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, errorColor.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("✕", fontSize = 20.sp, color = errorColor)
                                    Spacer(Modifier.width(12.dp))
                                    Text(searchError ?: "搜索失败", color = errorColor, fontSize = 13.sp)
                                    Spacer(Modifier.weight(1f))
                                    TextButton(onClick = {
                                        isSearchMode = false
                                        searchQuery = ""
                                        socialViewModel.clearSearch()
                                    }) {
                                        Text("关闭", color = appColors.textSub, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // 双入口卡片行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SocialEntryCard(
                        icon = "🔔",
                        title = "频段申请",
                        subtitle = "BAND REQUEST",
                        accentColor = CyberSecondary,
                        showBadge = friendRequests.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                        onClick = { showFriendRequestDialog = true }
                    )
                    SocialEntryCard(
                        icon = "💬",
                        title = "访客日志",
                        subtitle = "VISIT LOG",
                        accentColor = CyberPrimary,
                        showBadge = hasUnreadMessages,
                        modifier = Modifier.weight(1f),
                        onClick = { showVisitLogSheet = true }
                    )
                }

                Spacer(Modifier.height(20.dp))

                // 好友频道标签
                Text(
                    "频道列表 · ${filteredFriends.size} 个节点",
                    color = appColors.textSub,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // 好友列表
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // 骨架屏加载状态
                    if (isLoadingFriends && filteredFriends.isEmpty()) {
                        item {
                            SkeletonList(count = 5) {
                                SkeletonListItem(
                                    showAvatar = true,
                                    showSubtitle = true
                                )
                            }
                        }
                    } else if (filteredFriends.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🔍", fontSize = 36.sp)
                                    Spacer(Modifier.height(10.dp))
                                    Text("未检测到匹配节点频率", color = appColors.textSub, fontSize = 14.sp)
                                    Spacer(Modifier.height(8.dp))
                                    Text("下拉刷新同步数据", color = appColors.textSub.copy(alpha = 0.6f), fontSize = 12.sp)
                                }
                            }
                        }
                    } else {
                        items(filteredFriends) { friend ->
                            SocialFriendCard(
                                friend = friend,
                                onJump = {
                                    if (networkStatus is NetworkMonitor.NetworkStatus.Connected) {
                                        onNavigateToFriendGarden(friend)
                                    } else {
                                        Toast.makeText(context, "网络不可用，无法访问好友花园", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    }

    // 访客日志 BottomSheet
    if (showVisitLogSheet) {
        VisitLogBottomSheet(
            visitLogs = visitLogs,
            onDismiss = { showVisitLogSheet = false },
            onMarkAllRead = { socialViewModel.markAllRead() }
        )
    }

    // 黑段申请 Dialog
    if (showFriendRequestDialog) {
        FriendRequestDialog(
            friendRequests = friendRequests,
            onDismiss = { showFriendRequestDialog = false },
            onAccept = { requestId -> socialViewModel.acceptFriendRequest(requestId) },
            onReject = { requestId -> socialViewModel.rejectFriendRequest(requestId) }
        )
    }
}

/**
 * 社交模块入口玻璃卡片
 */
@Composable
fun SocialEntryCard(
    icon: String,
    title: String,
    subtitle: String,
    accentColor: Color,
    showBadge: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val appColors = LocalAppColors.current
    Box(modifier = modifier) {
        Surface(
            color = appColors.cardBg.copy(alpha = if (appColors.isDark) 0.5f else 1f),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.20f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(icon, fontSize = 28.sp)
                Spacer(Modifier.height(6.dp))
                Text(title, color = accentColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = appColors.textSub, fontSize = 9.sp, letterSpacing = 1.5.sp)
            }
        }

        if (showBadge) {
            val badgeColor = Color(0xFFFF4B4B) // 保留红色徽章
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(badgeColor)
                    .border(1.5.dp, appColors.bgDeep, CircleShape)
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
            )
        }
    }
}

/**
 * 量子雷达好友卡片
 */
@Composable
fun SocialFriendCard(
    friend: Friend,
    onJump: () -> Unit
) {
    val appColors = LocalAppColors.current
    Surface(
        color = appColors.cardBg,  // 移除透明度，使用完整的卡片背景色
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.5.dp,  // 增加边框宽度
            color = appColors.primary.copy(alpha = 0.2f)  // 使用主色调边框，增强卡片轮廓
        ),
        shadowElevation = if (appColors.isDark) 0.dp else 2.dp,  // 浅色主题添加阴影
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 头像
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(appColors.primary.copy(alpha = 0.15f))  // 增加头像背景透明度
                    .border(1.5.dp, appColors.primary.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),  // 增加边框宽度和透明度
                contentAlignment = Alignment.Center
            ) {
                Text(friend.avatarEmoji, fontSize = 28.sp)
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 昵称和账号
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    friend.nickname, 
                    color = appColors.textMain, 
                    fontWeight = FontWeight.Bold,  // 保持粗体
                    fontSize = 18.sp
                )
                Text(
                    "@${friend.account}", 
                    color = appColors.textSub,  // 使用完整的副文字色，不降低透明度
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,  // 增加字重
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }

            // 访问按钮
            Button(
                onClick = onJump,
                colors = ButtonDefaults.buttonColors(containerColor = appColors.primary),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("访问", color = if (appColors.isDark) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.width(4.dp))
                Text("🚀", fontSize = 14.sp)
            }
        }
    }
}
