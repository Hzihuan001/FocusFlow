package com.example.focusflow.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.focusflow.ui.components.ChatBottomSheet
import com.example.focusflow.ui.components.PullRefreshLayout
import com.example.focusflow.ui.theme.*
import kotlin.math.absoluteValue

// 🟢 FocusMode is defined HERE (Source of Truth)
data class FocusMode(
    val id: String,
    val name: String,
    val desc: String,
    val detailedDesc: String,
    val defaultFocusMinutes: Int,
    val defaultBreakMinutes: Int,
    val icon: ImageVector,
    val tag: String
)

// Data List
val focusModes = listOf(
    FocusMode(
        id = "custom",
        name = "自定义模式",
        desc = "完全由你掌控节奏",
        detailedDesc = "适合有独特工作习惯的用户。你可以自由设置总时长、每次专注的时长以及休息时长。",
        defaultFocusMinutes = 25,
        defaultBreakMinutes = 5,
        icon = Icons.Default.Settings,
        tag = "自由"
    ),
    FocusMode(
        id = "pomodoro",
        name = "番茄工作法",
        desc = "25分钟专注 + 5分钟休息",
        detailedDesc = "最经典的时间管理方法。通过短时间的冲刺和高频的休息，保持大脑的高效运转，防止疲劳。",
        defaultFocusMinutes = 25,
        defaultBreakMinutes = 5,
        icon = Icons.Default.Eco,
        tag = "经典"
    ),
    FocusMode(
        id = "5217",
        name = "52/17 法则",
        desc = "52分钟工作 + 17分钟休息",
        detailedDesc = "一项针对最高效员工的研究发现，52分钟的深度工作配合17分钟的完全抽离，能带来最高的产出效率。",
        defaultFocusMinutes = 52,
        defaultBreakMinutes = 17,
        icon = Icons.Default.Bolt,
        tag = "高效"
    ),
    FocusMode(
        id = "flow",
        name = "沉浸周期",
        desc = "90分钟深度工作",
        detailedDesc = "基于人体的“超日节律”。适合需要长时间连续思考的深度任务，如编程、写作或逻辑推演。",
        defaultFocusMinutes = 90,
        defaultBreakMinutes = 15,
        icon = Icons.Default.Waves,
        tag = "深度"
    )
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenSetup: (FocusMode) -> Unit,
    focusViewModel: com.example.focusflow.ui.FocusViewModel = viewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val appColors = LocalAppColors.current
    val pagerState = rememberPagerState(pageCount = { focusModes.size })
    var showDetailDialog by remember { mutableStateOf<FocusMode?>(null) }
    var showAiChat by remember { mutableStateOf(false) }
    
    // ───────────── 光流同步服务（实时响应）─────────────
    val syncService = remember { com.example.focusflow.service.TimeFluxSyncService.getInstance(context) }
    val syncTimeFlux by syncService.timeFlux.collectAsState()
    val localTimeFlux by focusViewModel.timeFluxBalance.collectAsState()
    val syncStatus by syncService.syncStatus.collectAsState()
    // 优先使用同步服务的光流值（实时更新），兜底使用本地数据库值
    val timeFlux = syncTimeFlux ?: localTimeFlux
    // 🟢 [OFFLINE] 判断是否失去连接（同步服务出错）
    val isConnected = syncStatus !is com.example.focusflow.service.TimeFluxSyncService.SyncStatus.Error

    // 🟢 [PULL TO REFRESH] 下拉刷新状态
    val isRefreshing by focusViewModel.isRefreshing.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            text = "Focus Flow",
                            color = appColors.primary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "选择你的专注流派",
                            color = appColors.textSub,
                            fontSize = 12.sp
                        )
                    }
                },
                actions = {
                    // 资产看板
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(appColors.cardBg)
                            .border(1.dp, appColors.primary, RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("✨", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        // 🟢 [OFFLINE] 离线时显示 "-"
                        Text(
                            text = if (isConnected) timeFlux.toString() else "-",
                            color = appColors.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // AI Assistant Button
                    IconButton(
                        onClick = { showAiChat = true },
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(40.dp)
                            .background(appColors.cardBg, CircleShape)
                            .border(1.dp, appColors.primary.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.SmartToy,
                            contentDescription = "AI Assistant",
                            tint = appColors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        // 🟢 [PULL TO REFRESH] 下拉刷新容器
        PullRefreshLayout(
            isRefreshing = isRefreshing,
            onRefresh = { focusViewModel.refresh() }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(appColors.bgDeep)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(20.dp))

            // Carousel
            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = 48.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                val mode = focusModes[page]
                val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                val scale = lerp(0.85f, 1f, 1f - pageOffset.absoluteValue.coerceIn(0f, 1f))
                val alpha = lerp(0.5f, 1f, 1f - pageOffset.absoluteValue.coerceIn(0f, 1f))

                Card(
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                        }
                        .fillMaxWidth()
                        .fillMaxHeight(0.9f)
                        .clickable { showDetailDialog = mode },
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = appColors.cardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, appColors.primary.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .background(appColors.bgDeep, CircleShape)
                                .border(2.dp, appColors.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(mode.icon, contentDescription = null, tint = appColors.primary, modifier = Modifier.size(48.dp))
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Text(mode.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = appColors.textMain)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(mode.desc, fontSize = 14.sp, color = appColors.textSub, textAlign = TextAlign.Center)

                        Spacer(modifier = Modifier.height(24.dp))

                        Surface(color = appColors.primary.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp)) {
                            Text(mode.tag, color = appColors.primary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Text("点击查看详情说明 >", fontSize = 12.sp, color = appColors.textSub)
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Bottom Button
            val currentMode = focusModes[pagerState.currentPage]

            Button(
                onClick = { onOpenSetup(currentMode) },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = appColors.primary, 
                    contentColor = if (appColors.isDark) Color.Black else Color.White
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("开始专注 (START)", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            // Bottom Spacing for Navigation Bar
            Spacer(modifier = Modifier.height(100.dp))
        }

        // Detail Dialog
        if (showDetailDialog != null) {
            val mode = showDetailDialog!!
            AlertDialog(
                onDismissRequest = { showDetailDialog = null },
                containerColor = appColors.cardBg,
                title = { Text(mode.name, color = appColors.primary) },
                text = {
                    Column {
                        Text(mode.detailedDesc, color = appColors.textMain)
                        Spacer(modifier = Modifier.height(16.dp))
                        if (mode.id != "custom") {
                            Text("配置：专注 ${mode.defaultFocusMinutes}分钟 / 休息 ${mode.defaultBreakMinutes}分钟", color = appColors.textSub, fontSize = 12.sp)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDetailDialog = null }) {
                        Text("关闭", color = appColors.primary)
                    }
                }
            )
        }

            // AI Chat
            ChatBottomSheet(
                isOpen = showAiChat,
                onDismiss = { showAiChat = false }
            )
            }
        }
    }
}