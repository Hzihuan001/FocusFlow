package com.example.focusflow.ui.screens

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.focusflow.ui.BarData
import com.example.focusflow.ui.MainViewModel
import com.example.focusflow.ui.theme.*
import com.example.focusflow.service.NetworkMonitor
import com.example.focusflow.ui.components.PullRefreshLayout
import com.example.focusflow.ui.components.SimpleRefreshIndicator
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale // 🟢 引入 Locale
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.abs


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: MainViewModel = viewModel(),
    onNavigateToGarden: () -> Unit = {}
) {
    val context = LocalContext.current
    val appColors = LocalAppColors.current
    val networkMonitor = remember { NetworkMonitor.getInstance(context) }
    val networkStatus by networkMonitor.networkStatus.collectAsState()
    
    // 确保监控已启动
    LaunchedEffect(Unit) {
        networkMonitor.startMonitoring()
    }
    
    val dailyStats by viewModel.dailyStats.collectAsState()
    val weeklyStats by viewModel.weeklyStats.collectAsState()
    val totalStats by viewModel.totalStats.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    var showDatePicker by remember { mutableStateOf(false) }

    // 使用中文格式 "yyyy年MM月dd日"
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy年MM月dd日", Locale.CHINA) }
    val displayDateText = if (selectedDate == LocalDate.now()) "今日" else selectedDate.format(dateFormatter)

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                        viewModel.updateSelectedDate(date)
                    }
                    showDatePicker = false
                }) { Text("确定", color = appColors.primary) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消", color = appColors.textSub) }
            },
            colors = DatePickerDefaults.colors(containerColor = appColors.cardBg)
        ) {
            DatePicker(
                state = datePickerState,
                // 🟢 修改：标题强制显示中文提示
                title = {
                    Text(
                        "选择日期",
                        modifier = Modifier.padding(start = 24.dp, top = 24.dp),
                        color = appColors.primary,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = DatePickerDefaults.colors(
                    headlineContentColor = appColors.primary,
                    dayContentColor = appColors.textMain,
                    selectedDayContainerColor = appColors.primary,
                    todayDateBorderColor = appColors.primary,
                    weekdayContentColor = appColors.textSub
                )
            )
        }
    }

    // 下拉刷新包装
    PullRefreshLayout(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refreshFromCloud() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(appColors.bgDeep)
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("数据复盘", color = appColors.textMain, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("让坚持看得见", color = appColors.textMain.copy(alpha = 0.7f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
            Row(
                modifier = Modifier
                    .background(appColors.cardBg, RoundedCornerShape(12.dp))
                    .border(1.dp, appColors.textSub.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                TabButton("日报", selectedTab == 0) { selectedTab = 0 }
                TabButton("本周", selectedTab == 1) { selectedTab = 1 }
                TabButton("总体", selectedTab == 2) { selectedTab = 2 }
            }
        }

        GardenEntryCard(
            isConnected = networkStatus is NetworkMonitor.NetworkStatus.Connected,
            onClick = {
                if (networkStatus is NetworkMonitor.NetworkStatus.Connected) {
                    onNavigateToGarden()
                } else {
                    Toast.makeText(context, "网络不可用，无法进入花园", Toast.LENGTH_SHORT).show()
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Crossfade(targetState = selectedTab, label = "StatsSwitch") { tab ->
            if (tab == 0) {
                Column {
                    StatsCard {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("$displayDateText 专注时长", color = appColors.textMain.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                IconButton(onClick = { showDatePicker = true }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = "选择日期", tint = appColors.primary)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text("${dailyStats.totalHours}", color = appColors.textMain, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                                Text("小时", color = appColors.textMain.copy(alpha = 0.7f), fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 6.dp, start = 4.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("${dailyStats.totalMinutes}", color = appColors.textMain, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                                Text("分钟", color = appColors.textMain.copy(alpha = 0.7f), fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 6.dp, start = 4.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    StatsCard {
                        Column(
                            modifier = Modifier.padding(vertical = 20.dp, horizontal = 10.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 0.dp).padding(bottom = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("标签分布 ($displayDateText)", color = appColors.textMain.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Row(
                                    modifier = Modifier
                                        .clickable { showDatePicker = true }
                                        .background(appColors.cardBg, RoundedCornerShape(8.dp))
                                        .border(1.dp, appColors.textSub.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CalendarMonth, null, tint = appColors.primary, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("选择日期", color = appColors.primary, fontSize = 12.sp)
                                }
                            }
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().height(280.dp).padding(horizontal = 24.dp)) {
                                if (dailyStats.pieData.isNotEmpty()) {
                                    val chartColors = PrintFriendlyColors.getChartColors()
                                    val mappedColors = dailyStats.pieData.indices.map { chartColors[it % chartColors.size] }
                                    AdvancedCyberPieChart(
                                        dataValues = dailyStats.pieData,
                                        labels = dailyStats.legendNames,
                                        colors = mappedColors,
                                        totalHours = "${dailyStats.totalHours}.${(dailyStats.totalMinutes / 60.0 * 10).toInt()}h"
                                    )
                                } else {
                                    Text("$displayDateText 暂无数据", color = appColors.textSub, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            } else if (tab == 1) {
                Column {
                    val weekTotalHours = weeklyStats.sumOf { it.value.toDouble() }.toFloat() / 60f
                    StatsCard {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("本周累计专注", color = appColors.textMain.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(String.format("%.1f", weekTotalHours), color = appColors.primary, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                                Text("小时", color = appColors.textMain.copy(alpha = 0.7f), fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 6.dp, start = 4.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    StatsCard {
                        Column(
                            modifier = Modifier.padding(20.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("近7天趋势", color = appColors.textMain.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.align(Alignment.Start).padding(bottom = 20.dp))
                            Text("点击柱状图查看该日详情", color = appColors.textMain.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Medium, modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp))
                            Box(modifier = Modifier.fillMaxWidth().height(250.dp)) {
                                CyberBarChart(
                                    data = weeklyStats,
                                    onBarClick = { barData ->
                                        barData.date?.let { date ->
                                            viewModel.updateSelectedDate(date)
                                            selectedTab = 0 // 切换到日报tab
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                // 总体统计
                Column {
                    StatsCard {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("累计专注时长", color = appColors.textMain.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text("${totalStats.totalHours}", color = appColors.primary, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                                Text("小时", color = appColors.textMain.copy(alpha = 0.7f), fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 6.dp, start = 4.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("${totalStats.totalMinutes}", color = appColors.primary, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                                Text("分钟", color = appColors.textMain.copy(alpha = 0.7f), fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 6.dp, start = 4.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    StatsCard {
                        Column(
                            modifier = Modifier.padding(vertical = 20.dp, horizontal = 10.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("任务分布（全部）", color = appColors.textMain.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 10.dp))
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().height(280.dp).padding(horizontal = 24.dp)) {
                                if (totalStats.pieData.isNotEmpty()) {
                                    val chartColors = PrintFriendlyColors.getChartColors()
                                    val mappedColors = totalStats.pieData.indices.map { chartColors[it % chartColors.size] }
                                    AdvancedCyberPieChart(
                                        dataValues = totalStats.pieData,
                                        labels = totalStats.legendNames,
                                        colors = mappedColors,
                                        totalHours = "${totalStats.totalHours}.${(totalStats.totalMinutes / 60.0 * 10).toInt()}h"
                                    )
                                } else {
                                    Text("暂无数据", color = appColors.textSub, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

// ================= 组件区 =================

@Composable
fun GardenEntryCard(
    isConnected: Boolean,
    onClick: () -> Unit
) {
    val appColors = LocalAppColors.current
    // 离线时降低透明度
    val contentAlpha = if (isConnected) 1f else 0.5f
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        appColors.primary.copy(alpha = 0.2f),
                        appColors.bgDeep
                    )
                ),
                RoundedCornerShape(24.dp)
            )
            .clickable(enabled = isConnected) { onClick() }
            .padding(20.dp)
    ) {
        Row(
            Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocalFlorist,
                        null,
                        tint = appColors.primary.copy(alpha = contentAlpha)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "我的花园",
                        color = appColors.textMain.copy(alpha = contentAlpha),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    if (isConnected) "查看你的数字植物收藏" else "网络不可用",
                    color = appColors.textSub.copy(alpha = contentAlpha),
                    fontSize = 12.sp
                )
            }
            Box(
                Modifier
                    .size(40.dp)
                    .background(appColors.cardBg.copy(0.3f), RoundedCornerShape(12.dp)),
                Alignment.Center
            ) {
                Icon(
                    if (isConnected) Icons.Default.ArrowForward else Icons.Default.Warning,
                    null,
                    tint = if (isConnected) appColors.primary else appColors.textSub
                )
            }
        }
    }
}

@Composable
fun TabButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    val appColors = LocalAppColors.current
    Box(
        modifier = Modifier
            .background(if (isSelected) appColors.primary else Color.Transparent, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text, 
            color = if (isSelected) {
                if (appColors.isDark) Color.Black else Color.White
            } else {
                appColors.textSub
            }, 
            fontSize = 12.sp, 
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun StatsCard(content: @Composable () -> Unit) {
    val appColors = LocalAppColors.current
    Box(
        modifier = Modifier.fillMaxWidth()
            .background(appColors.cardBg, RoundedCornerShape(24.dp))
            .border(1.dp, appColors.textSub.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
    ) { content() }
}

@Composable
fun CyberBarChart(
    data: List<BarData>,
    onBarClick: (BarData) -> Unit = {}
) {
    val appColors = LocalAppColors.current
    val maxValue = data.maxOfOrNull { it.value } ?: 1f
    val safeMax = if (maxValue == 0f) 1f else maxValue * 1.2f
    // 在 Composable 作用域内读取主题颜色，供 Canvas DrawScope 使用
    val primaryColor = appColors.primary
    val textMainColor = appColors.textMain
    val textSubColor = appColors.textSub

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val barWidth = size.width / (data.size * 1.5f)
            val spacing = size.width / data.size
            val bottomY = size.height - 30.dp.toPx()

            data.forEachIndexed { index, barData ->
                val x = spacing * index + (spacing - barWidth) / 2
                val barHeight = (barData.value / safeMax) * bottomY
                val topY = bottomY - barHeight

                drawRoundRect(
                    color = if (barData.isToday) primaryColor else textSubColor.copy(alpha = 0.3f),
                    topLeft = Offset(x, topY),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(4.dp.toPx())
                )

                if (barData.value > 0) {
                    drawIntoCanvas { canvas ->
                        val paint = Paint().apply {
                            color = textMainColor.toArgb()
                            textSize = 11.sp.toPx()  // 从 10sp 增加到 11sp
                            textAlign = Paint.Align.CENTER
                            typeface = Typeface.DEFAULT_BOLD  // 添加粗体
                        }
                        canvas.nativeCanvas.drawText(String.format("%.0f", barData.value), x + barWidth / 2, topY - 10f, paint)
                    }
                }

                drawIntoCanvas { canvas ->
                    val paint = Paint().apply {
                        color = if(barData.isToday) primaryColor.toArgb() else textMainColor.copy(alpha = 0.7f).toArgb()  // 从 textSubColor 改为 textMainColor
                        textSize = 12.sp.toPx()  // 从 11sp 增加到 12sp
                        textAlign = Paint.Align.CENTER
                        typeface = Typeface.DEFAULT_BOLD  // 添加粗体
                    }
                    canvas.nativeCanvas.drawText(barData.label, x + barWidth / 2, size.height - 5f, paint)
                }
            }
        }
        
        // 点击检测层
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            data.forEach { barData ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onBarClick(barData) }
                )
            }
        }
    }
}

// 定义一个临时数据类，用来存储标签的计算信息
private data class PieLabelData(
    val index: Int,
    val color: Color,
    val label: String,
    val percent: Float,
    val baseAngle: Float, // 原始角度
    var anchorX: Float,   // 圆环边缘的起点 X
    var anchorY: Float,   // 圆环边缘的起点 Y
    var elbowX: Float,    // 拐点 X
    var elbowY: Float,    // 拐点 Y (这个值会被调整)
    var textX: Float,     // 文字最终 X
    var isLeft: Boolean   // 在左侧还是右侧
)

@Composable
fun AdvancedCyberPieChart(
    dataValues: List<Float>,
    labels: List<String>,
    colors: List<Color>,
    totalHours: String,
    modifier: Modifier = Modifier
) {
    val appColors = LocalAppColors.current
    val safeData = if (dataValues.isEmpty()) listOf(1f) else dataValues
    val safeColors = if (dataValues.isEmpty()) listOf(appColors.textSub) else colors
    val safeLabels = if (dataValues.isEmpty()) listOf("暂无") else labels

    Canvas(modifier = modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val centerY = size.height / 2

        // 调整圆环大小 (根据图二效果，圆环可以稍微粗一点)
        val radius = size.minDimension / 6f
        val holeRadius = radius * 0.65f

        // -----------------------------
        // 第一步：画圆弧 & 收集标签数据
        // -----------------------------
        var startAngle = -90f
        val labelList = mutableListOf<PieLabelData>()

        safeData.forEachIndexed { index, percent ->
            val sweepAngle = percent * 360f
            val color = safeColors.getOrElse(index) { appColors.textSub }

            // 画圆弧
            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(centerX - radius, centerY - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = radius - holeRadius)
            )

            // 如果占比太小(例如0)，就不显示标签，避免视觉噪音
            if (percent > 0.001f) {
                val midAngle = startAngle + sweepAngle / 2
                // 将角度标准化到 0-360，方便判断左右
                val normalizedAngle = (midAngle + 360) % 360
                val midRad = Math.toRadians(midAngle.toDouble())

                // 判断左右：90度到270度在左边，其他在右边
                val isLeft = normalizedAngle > 90 && normalizedAngle < 270

                // 1. 起点：圆环外沿稍微出去一点
                val lineStartRadius = radius + 4.dp.toPx()
                val anchorX = (centerX + lineStartRadius * cos(midRad)).toFloat()
                val anchorY = (centerY + lineStartRadius * sin(midRad)).toFloat()

                // 2. 理想的拐点位置 (先算出理想的 Y，后面防碰撞逻辑会修改这个 Y)
                val lineElbowRadius = radius + 25.dp.toPx() // 拐点距离圆心远一点，给线条留出空间
                val idealElbowX = (centerX + lineElbowRadius * cos(midRad)).toFloat()
                val idealElbowY = (centerY + lineElbowRadius * sin(midRad)).toFloat()

                // 3. 文字最终位置 (X轴固定拉开，形成图二那种对齐效果)
                // 这里的 60.dp 是指引线横向延伸的长度
                val textOffset = 50.dp.toPx()
                val textX = if (isLeft) centerX - radius - textOffset else centerX + radius + textOffset

                // 修正拐点 X：让拐点的 X 稍微靠近文字一点，或者保持圆形分布
                // 为了达到图二的效果，我们让拐点 X 保持在圆周上，但 Y 会被调整

                labelList.add(
                    PieLabelData(
                        index = index,
                        color = color,
                        label = safeLabels.getOrElse(index) { "" },
                        percent = percent,
                        baseAngle = midAngle,
                        anchorX = anchorX,
                        anchorY = anchorY,
                        elbowX = idealElbowX, // 初始拐点 X
                        elbowY = idealElbowY, // 初始拐点 Y
                        textX = textX,
                        isLeft = isLeft
                    )
                )
            }
            startAngle += sweepAngle
        }

        // -----------------------------
        // 第二步：防碰撞布局算法 (核心修改)
        // -----------------------------

        // 分离左右列表
        val leftLabels = labelList.filter { it.isLeft }.sortedBy { it.elbowY } // 按 Y 从上到下排序
        val rightLabels = labelList.filter { !it.isLeft }.sortedBy { it.elbowY }

        // 定义标签之间的最小垂直间距 (文字高度 + 间隙)
        val minSpacing = 32.sp.toPx() // 根据字体大小动态调整

        // 调整左侧
        adjustLabelsY(leftLabels, minSpacing)
        // 调整右侧
        adjustLabelsY(rightLabels, minSpacing)

        // -----------------------------
        // 第三步：绘制调整后的标签
        // -----------------------------

        val textPaint = Paint().apply {
            textSize = 13.sp.toPx()  // 从 12sp 增加到 13sp
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.LEFT // 先统一设为左对齐，后面根据左右修正
        }
        val subTextPaint = Paint().apply {
            textSize = 11.sp.toPx()  // 从 10sp 增加到 11sp
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)  // 改为粗体
            color = appColors.textMain.copy(alpha = 0.8f).toArgb()  // 从 textSub 改为 textMain
            textAlign = Paint.Align.LEFT
        }

        val allLabels = leftLabels + rightLabels
        allLabels.forEach { item ->
            val isLeft = item.isLeft

            // 重新计算拐点 X 坐标
            // 简单的做法：拐点 X 可以跟随 Y 的变化在垂直方向微调，或者保持原样
            // 为了美观，我们让拐点的 X 坐标稍微往外推一点，形成折线
            // 这里我们保持 elbowX 不变，只让线条变长或变斜

            // 绘制折线
            val path = Path().apply {
                moveTo(item.anchorX, item.anchorY)
                // 第一段：从圆环到拐点 (Y 已被调整)
                lineTo(item.elbowX, item.elbowY)
                // 第二段：从拐点到文字前
                lineTo(item.textX, item.elbowY)
            }
            drawPath(path = path, color = item.color, style = Stroke(width = 1.5.dp.toPx()))

            // 绘制文字末端的小圆点 (图二风格)
            drawCircle(
                color = item.color,
                radius = 2.dp.toPx(),
                center = Offset(item.textX, item.elbowY)
            )

            // 绘制文字
            drawIntoCanvas { canvas ->
                // 设置对齐方式
                textPaint.textAlign = if (isLeft) Paint.Align.RIGHT else Paint.Align.LEFT
                subTextPaint.textAlign = if (isLeft) Paint.Align.RIGHT else Paint.Align.LEFT
                textPaint.color = item.color.toArgb()

                val padding = 8f
                val finalX = if (isLeft) item.textX - padding else item.textX + padding

                // 百分比 (上方)
                val percentText = if (item.percent < 0.01f) "<1%" else "${(item.percent * 100).toInt()}%"
                canvas.nativeCanvas.drawText(percentText, finalX, item.elbowY - 5f, textPaint)

                // 标签名 (下方)
                canvas.nativeCanvas.drawText(item.label, finalX, item.elbowY + 25f, subTextPaint)
            }
        }

        // -----------------------------
        // 第四步：中心总时长
        // -----------------------------
        drawIntoCanvas { canvas ->
            val centerPaint = Paint().apply {
                color = appColors.textMain.toArgb()
                textSize = 26.sp.toPx()  // 从 24sp 增加到 26sp
                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT_BOLD
            }
            val yPos = centerY - (centerPaint.descent() + centerPaint.ascent()) / 2
            canvas.nativeCanvas.drawText(totalHours, centerX, yPos, centerPaint)
        }
    }
}

/**
 * 核心算法：调整 Y 坐标以避免重叠
 * 逻辑：从上往下遍历，如果当前元素比上一个元素的位置太高（重叠），就把它按下去。
 */
private fun adjustLabelsY(labels: List<PieLabelData>, minSpacing: Float) {
    if (labels.isEmpty()) return

    // 1. 从上往下扫一遍，解决重叠
    for (i in 1 until labels.size) {
        val prev = labels[i - 1]
        val curr = labels[i]

        // 如果当前标签的 Y 坐标 小于 (上一个标签 Y + 间距)，说明挤在一起了
        if (curr.elbowY < prev.elbowY + minSpacing) {
            curr.elbowY = prev.elbowY + minSpacing
        }
    }
}