package com.example.focusflow.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.focusflow.data.ChatSessionEntity
import com.example.focusflow.ui.theme.CyberCardBg
import com.example.focusflow.ui.theme.CyberPrimary
import com.example.focusflow.ui.theme.CyberTextSub
import com.example.focusflow.ui.viewmodel.ChatMessage
import com.example.focusflow.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * 简单的 Markdown 清理函数
 * 保留空格和换行，只移除 Markdown 标记
 */
private fun cleanMarkdown(text: String): String {
    return text
        // 移除粗斜体 ***text***
        .replace(Regex("""\*\*\*(.+?)\*\*\*"""), "$1")
        // 移除粗体 **text**
        .replace(Regex("""\*\*(.+?)\*\*"""), "$1")
        // 移除斜体 *text* 或 _text_
        .replace(Regex("""\*(.+?)\*"""), "$1")
        .replace(Regex("""_(.+?)_"""), "$1")
        // 移除行内代码 `code`
        .replace(Regex("""`(.+?)`"""), "$1")
        // 移除代码块 ```code```
        .replace(Regex("""```[\s\S]*?```"""), "")
        // 移除标题标记 # ## ### 等（保留标题内容）
        .replace(Regex("""#{1,6}\s"""), "")
        // 移除列表标记 - 或 * 或 1.
        .replace(Regex("""^[\s]*[-*+]\s""", RegexOption.MULTILINE), "")
        .replace(Regex("""^[\s]*\d+\.\s""", RegexOption.MULTILINE), "")
        // 规范化多个连续空格为单个（但保留换行）
        .replace(Regex("""[^\S\n]{2,}"""), " ")
        // 规范化多个连续换行
        .replace(Regex("""\n{3,}"""), "\n\n")
}

private fun formatTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "刚刚"
        diff < 3600_000 -> "${diff / 60_000}分钟前"
        diff < 86400_000 -> "${diff / 3600_000}小时前"
        diff < 604800_000 -> SimpleDateFormat("E", Locale.CHINESE).format(Date(timestamp))
        else -> SimpleDateFormat("MM/dd", Locale.CHINESE).format(Date(timestamp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatBottomSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    viewModel: ChatViewModel = viewModel()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSessionList by remember { mutableStateOf(false) }

    if (isOpen) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = CyberCardBg,
            scrimColor = Color.Black.copy(alpha = 0.5f),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                ChatContent(
                    onDismiss = onDismiss,
                    onToggleSessions = { showSessionList = !showSessionList },
                    viewModel = viewModel
                )
                
                // 会话列表侧边栏
                if (showSessionList) {
                    SessionListPanel(
                        viewModel = viewModel,
                        onClose = { showSessionList = false },
                        modifier = Modifier.align(Alignment.CenterStart)
                    )
                }
            }
        }
    }
}

@Composable
fun SessionListPanel(
    viewModel: ChatViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sessions by viewModel.sessions.collectAsState()
    val currentSessionId by viewModel.currentSessionId.collectAsState()
    val scope = rememberCoroutineScope()
    
    Column(
        modifier = modifier
            .width(280.dp)
            .fillMaxHeight()
            .background(Color(0xFF1A1A2E))
    ) {
        // 标题栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "历史会话",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "关闭", tint = Color.White)
            }
        }
        
        Divider(color = Color.Gray.copy(alpha = 0.2f))
        
        // 新建会话按钮
        Button(
            onClick = {
                scope.launch {
                    viewModel.createSession()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("新建会话", color = Color.Black)
        }
        
        // 会话列表
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(
                items = sessions,
                key = { it.sessionId }
            ) { session ->
                SessionItem(
                    session = session,
                    isSelected = session.sessionId == currentSessionId,
                    onSelect = {
                        viewModel.selectSession(session.sessionId)
                        onClose()
                    },
                    onDelete = {
                        scope.launch {
                            viewModel.deleteSession(session.sessionId)
                        }
                    },
                    onPin = {
                        scope.launch {
                            viewModel.togglePinSession(session.sessionId)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun SessionItem(
    session: ChatSessionEntity,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    onPin: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable(onClick = onSelect),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) CyberPrimary.copy(alpha = 0.2f) else Color(0xFF252540)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 置顶图标
            if (session.isPinned) {
                Icon(
                    Icons.Default.PushPin,
                    contentDescription = "置顶",
                    tint = CyberPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    session.title,
                    color = if (isSelected) CyberPrimary else Color.White,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        formatTime(session.updatedAt),
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                    if (session.messageCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "${session.messageCount}条消息",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            
            // 更多操作
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "更多",
                        tint = Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
                
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(if (session.isPinned) "取消置顶" else "置顶") },
                        onClick = {
                            onPin()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.PushPin,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("删除", color = Color.Red) },
                        onClick = {
                            onDelete()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = Color.Red,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ChatContent(
    onDismiss: () -> Unit,
    onToggleSessions: () -> Unit,
    viewModel: ChatViewModel
) {
    var inputValue by remember { mutableStateOf("") }
    val messages by viewModel.messages.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val currentSessionId by viewModel.currentSessionId.collectAsState()
    val isTyping = messages.isNotEmpty() && messages.last().isTyping
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    fun sendMessage() {
        if (inputValue.isBlank() || isTyping) return
        val userText = inputValue
        inputValue = ""
        viewModel.sendMessage(userText)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.9f)
            .imePadding()
            .navigationBarsPadding()
    ) {
        // Top Bar
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 会话切换按钮
            IconButton(onClick = onToggleSessions) {
                Icon(Icons.Default.Menu, contentDescription = "会话列表", tint = CyberPrimary)
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(CyberPrimary, CircleShape))
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI 专注助手", color = Color.White, fontWeight = FontWeight.Bold)
            }
            
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = CyberTextSub)
            }
        }
        
        // 当前会话信息
        if (currentSessionId != null && sessions.isNotEmpty()) {
            val currentSession = sessions.find { it.sessionId == currentSessionId }
            if (currentSession != null) {
                Text(
                    currentSession.title,
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }
        
        Divider(color = Color.Gray.copy(alpha = 0.2f), modifier = Modifier.padding(top = 8.dp))

        // 消息列表（reverseLayout 让最新消息在底部）
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            reverseLayout = true
        ) {
            if (messages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "开始新对话吧！",
                            color = Color.Gray,
                            fontSize = 16.sp
                        )
                    }
                }
            } else {
                // 反向遍历消息列表，让最新消息显示在底部
                items(
                    items = messages.reversed(),
                    key = { msg -> msg.id }
                ) { msg ->
                    ChatBubble(message = msg)
                }
            }
        }

        // 输入框
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputValue,
                onValueChange = { inputValue = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("输入...", color = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberPrimary,
                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = { sendMessage() },
                enabled = !isTyping && inputValue.isNotBlank(),
                modifier = Modifier.size(50.dp).background(if (isTyping) Color.Gray else CyberPrimary, CircleShape)
            ) {
                if (isTyping) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black)
                } else {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.Black)
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val displayText = if (!message.isUser) cleanMarkdown(message.text) else message.text
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(
                    color = if (message.isUser) CyberPrimary else Color(0xFF2A2A2A),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(12.dp)
        ) {
            Text(
                text = displayText,
                color = if (message.isUser) Color.Black else Color.White,
                fontSize = 14.sp
            )
        }
    }
}
