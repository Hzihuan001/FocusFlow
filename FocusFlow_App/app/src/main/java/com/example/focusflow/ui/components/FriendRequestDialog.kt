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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.focusflow.ui.theme.*
import com.example.focusflow.ui.viewmodel.FriendRequest

/**
 * 频段申请接收器 Dialog
 *
 * 设计原则：
 * - 深色玻璃拟态对话框（WaterPavilionDeep 背景 + 霓虹边框）
 * - 逐条展示频段申请，每条含【接受 ✓ / 拒绝 ✕】操作按钮对
 * - 接受 → 调用 onAccept 回调，由 ViewModel 从列表移除（Mock）
 * - 拒绝 → 调用 onReject 回调，由 ViewModel 从列表移除（Mock）
 * - 列表为空时展示「频率清净」空态插画
 *
 * 论文亮点：采用回调函数分离展示层与 ViewModel 业务逻辑，
 * 完全符合 MVVM 单向数据流规范；接受/拒绝分两个独立 Lambda，
 * 便于后续对接网络层时精准替换，耦合度极低。
 */
@Composable
fun FriendRequestDialog(
    friendRequests: List<FriendRequest>,
    onDismiss: () -> Unit,
    onAccept: (requestId: Long) -> Unit,
    onReject: (requestId: Long) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF0D151D),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                CyberSecondary.copy(alpha = 0.25f)
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // ══ 标题 ══
                Column(modifier = Modifier.padding(bottom = 20.dp)) {
                    Text(
                        "🔔 频段申请接收器",
                        color = CyberSecondary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "BAND REQUEST RECEIVER",
                        color = CyberTextSub,
                        fontSize = 10.sp,
                        letterSpacing = 2.sp
                    )
                }

                // ══ 申请列表 ══
                if (friendRequests.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📡", fontSize = 36.sp)
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "频率清净，暂无新频段接入申请",
                                color = CyberTextSub,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp)
                    ) {
                        items(friendRequests, key = { it.requestId }) { request ->
                            FriendRequestItem(
                                request = request,
                                onAccept = { onAccept(request.requestId) },
                                onReject = { onReject(request.requestId) }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ══ 关闭按钮 ══
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        "关闭频段 ✕",
                        color = CyberTextSub,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

/**
 * 单条频段申请卡片
 */
@Composable
private fun FriendRequestItem(
    request: FriendRequest,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Surface(
        color = CyberSecondary.copy(alpha = 0.05f),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            CyberSecondary.copy(alpha = 0.15f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 头像
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(CyberSecondary.copy(alpha = 0.10f))
                    .border(1.dp, CyberSecondary.copy(alpha = 0.30f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(request.fromAvatar, fontSize = 22.sp)
            }

            Spacer(Modifier.width(12.dp))

            // 信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    request.fromNickname,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    "ID: FF-${request.fromUserId}  ·  ${request.requestTime}",
                    color = CyberTextSub,
                    fontSize = 11.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }

            Spacer(Modifier.width(8.dp))

            // 操作按钮组
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.End
            ) {
                // 接受按钮
                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberPrimary.copy(alpha = 0.15f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, CyberPrimary.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("✓ 接受", color = CyberPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                // 拒绝按钮
                OutlinedButton(
                    onClick = onReject,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, NeonRed.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = NeonRed.copy(alpha = 0.05f))
                ) {
                    Text("✕ 拒绝", color = NeonRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
