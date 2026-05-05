package com.example.focusflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.focusflow.ui.theme.*
import com.example.focusflow.ui.viewmodel.VisitLog

/**
 * 花园访客日志 BottomSheet
 *
 * 设计原则：
 * - 未读条目右侧显示 NeonRed 霓虹圆点
 * - 底部「全部已读」按钮触发 markAllRead()
 * 技术创新点：通过 .map { it.copy(isRead = true) } 实现不可变数据流更新，
 * 完全符合 Kotlin StateFlow 响应式编程范式，避免并发数据竞争。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitLogBottomSheet(
    visitLogs: List<VisitLog>,
    onDismiss: () -> Unit,
    onMarkAllRead: () -> Unit
) {
    val appColors = LocalAppColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = appColors.cardBg,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            // 自定义拖拽条 - 霓虹点缀
            Box(
                Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .width(40.dp)
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(appColors.primary.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            // ══ 标题区 ══
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "💬 花园访客日志",
                        color = appColors.primary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "OASIS VISIT LOG",
                        color = appColors.textSub,
                        fontSize = 10.sp,
                        letterSpacing = 2.sp
                    )
                }
                // 未读数量徽章
                val unreadCount = visitLogs.count { !it.isRead }
                if (unreadCount > 0) {
                    Surface(
                        color = NeonRed.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonRed.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "$unreadCount 条未读",
                            color = NeonRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // ══ 日志列表 ══
            if (visitLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🌌", fontSize = 40.sp)
                        Spacer(Modifier.height(12.dp))
                        Text("尚无访客踪迹", color = appColors.textSub, fontSize = 14.sp)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                ) {
                    items(visitLogs) { log ->
                        VisitLogItem(log = log)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ══ 底部操作按钮 ══
            val hasUnread = visitLogs.any { !it.isRead }
            Button(
                onClick = {
                    onMarkAllRead()
                    onDismiss()
                },
                enabled = hasUnread,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = appColors.primary.copy(alpha = 0.12f),
                    disabledContainerColor = appColors.textSub.copy(alpha = 0.05f)
                ),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (hasUnread) appColors.primary.copy(alpha = 0.4f) else appColors.textSub.copy(alpha = 0.2f)
                )
            ) {
                Text(
                    text = if (hasUnread) "✓ 全部标为已读" else "— 全部已阅 —",
                    color = if (hasUnread) appColors.primary else appColors.textSub,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

/**
 * 单条访客日志卡片
 */
@Composable
private fun VisitLogItem(log: VisitLog) {
    val appColors = LocalAppColors.current
    
    // 根据 actionType 决定视觉风格
    val accentColor = if (log.actionType == 1) appColors.primary else CyberSecondary
    val typeIcon = if (log.actionType == 1) "⚡" else "💬"
    val typeLabel = if (log.actionType == 1) "量子充能" else "信标留言"

    Surface(
        color = if (log.isRead) appColors.textSub.copy(alpha = 0.05f) else accentColor.copy(alpha = 0.08f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (log.isRead) appColors.textSub.copy(alpha = 0.15f) else accentColor.copy(alpha = 0.25f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 类型图标圆圈
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.12f))
                    .border(1.dp, accentColor.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(log.visitorAvatar, fontSize = 20.sp)
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        log.visitorNickname,
                        color = appColors.textMain,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.width(6.dp))
                    // 类型角标
                    Surface(
                        color = accentColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            "$typeIcon $typeLabel",
                            color = accentColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    log.content,
                    color = appColors.textMain,  // 增强可见性：使用主文字色
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,  // 增加字重
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    log.createTime,
                    color = appColors.textSub,  // 增强可见性：移除 alpha
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // 未读红点（保留固定的警告红色）
            if (!log.isRead) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(NeonRed)
                )
            }
        }
    }
}
