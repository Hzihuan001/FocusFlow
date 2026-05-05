package com.example.focusflow.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.focusflow.api.ChatMessage as ApiChatMessage
import com.example.focusflow.data.AppDatabase
import com.example.focusflow.data.ChatMessageEntity
import com.example.focusflow.data.ChatSessionEntity
import com.example.focusflow.data.session.SessionManager
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val isTyping: Boolean = false
)

data class ChatSession(
    val sessionId: Long,
    val title: String,
    val updatedAt: Long,
    val isPinned: Boolean = false
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val chatDao = database.chatDao()
    private val sessionDao = database.chatSessionDao()
    private val gson = Gson()

    // ==================== 会话管理 ====================
    
    // 会话列表
    val sessions: StateFlow<List<ChatSessionEntity>> = sessionDao.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // 当前会话 ID
    private val _currentSessionId = MutableStateFlow<Long?>(null)
    val currentSessionId: StateFlow<Long?> = _currentSessionId.asStateFlow()

    // ==================== 消息管理 ====================
    
    // 当前会话的消息
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    
    // 消息订阅 Job（用于取消之前的订阅）
    private var messagesJob: Job? = null

    // ==================== AI 流式响应 ====================
    
    private val sseClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val factory: EventSource.Factory = EventSources.createFactory(sseClient)
    private var currentEventSource: EventSource? = null
    private val responseBuilder = StringBuilder()
    private var currentAiMessageId: String? = null

    // ==================== 会话操作 ====================
    
    /**
     * 创建新会话
     */
    suspend fun createSession(): Long {
        val session = ChatSessionEntity(
            title = "新对话",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val sessionId = sessionDao.insertSession(session)
        
        // 先取消旧的订阅，防止旧消息干扰
        messagesJob?.cancel()
        
        // 设置当前会话 ID
        _currentSessionId.value = sessionId
        
        // 清空消息（确保新会话是空的）
        _messages.value = emptyList()
        
        // 订阅新会话的消息
        subscribeToMessages(sessionId)
        
        return sessionId
    }
    
    /**
     * 切换会话
     */
    fun selectSession(sessionId: Long) {
        // 允许切换到同一个会话（用于刷新）
        
        // 取消之前的订阅
        messagesJob?.cancel()
        currentEventSource?.cancel()
        
        // 清空当前消息（防止显示旧数据）
        _messages.value = emptyList()
        
        // 设置新会话 ID
        _currentSessionId.value = sessionId
        
        // 订阅新会话的消息
        subscribeToMessages(sessionId)
    }
    
    /**
     * 订阅指定会话的消息
     */
    private fun subscribeToMessages(sessionId: Long) {
        // 取消之前的订阅（双重保险）
        messagesJob?.cancel()
        
        // 创建新订阅
        messagesJob = viewModelScope.launch {
            chatDao.getMessagesBySession(sessionId)
                .catch { e -> Log.e("ChatViewModel", "读取消息失败", e) }
                .collect { entities ->
                    // 只有当当前会话 ID 匹配时才更新消息
                    if (_currentSessionId.value == sessionId) {
                        // 检查是否正在输入（避免覆盖打字机效果）
                        val lastMsg = _messages.value.lastOrNull()
                        if (lastMsg?.isTyping == true) {
                            // 正在输入中，不更新
                        } else {
                            _messages.value = entities.map { entity ->
                                ChatMessage(
                                    id = entity.id.toString(),
                                    text = entity.content,
                                    isUser = entity.isUser,
                                    isTyping = false
                                )
                            }
                        }
                    }
                }
        }
    }
    
    /**
     * 删除会话
     */
    suspend fun deleteSession(sessionId: Long) {
        sessionDao.deleteSession(ChatSessionEntity(sessionId = sessionId, title = ""))
        if (_currentSessionId.value == sessionId) {
            messagesJob?.cancel()
            val nextSession = sessions.value.firstOrNull { it.sessionId != sessionId }
            if (nextSession != null) {
                _currentSessionId.value = nextSession.sessionId
                subscribeToMessages(nextSession.sessionId)
            } else {
                _currentSessionId.value = null
                _messages.value = emptyList()
            }
        }
    }
    
    /**
     * 切换置顶
     */
    suspend fun togglePinSession(sessionId: Long) {
        sessionDao.togglePin(sessionId)
    }
    
    /**
     * 重命名会话
     */
    suspend fun renameSession(sessionId: Long, newTitle: String) {
        sessionDao.updateTitle(sessionId, newTitle)
    }

    /**
     * 获取或创建当前会话
     */
    private suspend fun getOrCreateCurrentSession(): Long {
        val currentId = _currentSessionId.value
        if (currentId != null) return currentId
        
        // 检查是否有现有会话
        val existingSessions = sessions.value
        if (existingSessions.isNotEmpty()) {
            val sessionId = existingSessions.first().sessionId
            _currentSessionId.value = sessionId
            subscribeToMessages(sessionId)
            return sessionId
        }
        
        // 创建新会话
        return createSession()
    }

    // ==================== 消息操作 ====================
    
    fun sendMessage(content: String) {
        if (content.isBlank()) return

        viewModelScope.launch {
            val sessionId = getOrCreateCurrentSession()

            // 1. 立即添加用户消息到 UI
            val userMsgId = UUID.randomUUID().toString()
            _messages.value = _messages.value + ChatMessage(
                id = userMsgId,
                text = content,
                isUser = true,
                isTyping = false
            )

            // 2. 后台保存用户消息
            chatDao.insertMessage(ChatMessageEntity(
                sessionId = sessionId,
                content = content,
                isUser = true
            ))

            // 3. 更新会话标题（如果是第一条消息）
            val msgCount = chatDao.getMessageCount(sessionId)
            if (msgCount <= 1) {
                val title = if (content.length > 20) content.take(20) + "..." else content
                sessionDao.updateTitle(sessionId, title)
            }
            // 更新消息数量和最后消息预览
            val preview = if (content.length > 50) content.take(50) + "..." else content
            sessionDao.updateMessageInfo(sessionId, msgCount + 1, preview)

            // 4. 准备上下文
            val currentContext = _messages.value
                .takeLast(6)
                .filter { !it.isTyping }
                .map { uiMsg ->
                    ApiChatMessage(
                        role = if (uiMsg.isUser) "user" else "assistant",
                        content = uiMsg.text
                    )
                }.toMutableList()
            currentContext.add(ApiChatMessage(role = "user", content = content))
            val jsonBody = gson.toJson(currentContext)

            // 5. 添加 AI 占位消息
            currentAiMessageId = UUID.randomUUID().toString()
            _messages.value = _messages.value + ChatMessage(
                id = currentAiMessageId!!,
                text = "",
                isUser = false,
                isTyping = true
            )
            responseBuilder.clear()

            // 6. 发起 SSE 请求
            launchSseRequest(sessionId, jsonBody)
        }
    }

    private suspend fun launchSseRequest(sessionId: Long, jsonBody: String) {
        val sessionManager = SessionManager.getInstance(getApplication())
        val userId = try {
            sessionManager.requireUserId()
        } catch (e: Exception) {
            Log.e("ChatViewModel", "获取用户ID失败", e)
            updateAiMessage("请先登录", false)
            return
        }

        val request = Request.Builder()
            .url("${com.example.focusflow.api.RetrofitClient.GATEWAY_BASE_URL}ai/chat/stream")
            .post(jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .header("Accept", "text/event-stream")
            .header("X-User-Id", userId.toString())
            .build()

        currentEventSource = factory.newEventSource(request, object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: Response) {
                Log.d("ChatViewModel", "SSE 连接建立")
            }

            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                if (data == "[DONE]") return
                try {
                    // 智谱 AI 返回格式: {"choices":[{"delta":{"content":"xxx"}}]}
                    val chunk = if (data.startsWith("{")) {
                        val json = JSONObject(data)
                        val choices = json.optJSONArray("choices")
                        if (choices != null && choices.length() > 0) {
                            val delta = choices.getJSONObject(0).optJSONObject("delta")
                            delta?.optString("content", "") ?: ""
                        } else {
                            ""
                        }
                    } else {
                        data
                    }
                    if (chunk.isNotEmpty()) {
                        // 调试：记录空格情况
                        Log.d("ChatViewModel", "SSE chunk: '$chunk' (hasSpace=${chunk.contains(" ")})")
                        responseBuilder.append(chunk)
                        viewModelScope.launch(Dispatchers.Main) {
                            updateAiMessage(responseBuilder.toString(), true)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ChatViewModel", "解析失败: $data", e)
                }
            }

            override fun onClosed(eventSource: EventSource) {
                Log.d("ChatViewModel", "SSE 关闭")
                val finalText = responseBuilder.toString()
                viewModelScope.launch(Dispatchers.Main) {
                    updateAiMessage(finalText, false)
                }
                saveAiMessage(sessionId, finalText)
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                Log.e("ChatViewModel", "SSE 失败: ${t?.message}")
                val finalText = responseBuilder.toString()
                viewModelScope.launch(Dispatchers.Main) {
                    if (finalText.isBlank()) {
                        updateAiMessage("网络异常，请重试", false)
                    } else {
                        updateAiMessage(finalText, false)
                    }
                }
                if (finalText.isNotBlank()) {
                    saveAiMessage(sessionId, finalText)
                }
            }
        })
    }

    private fun updateAiMessage(text: String, isTyping: Boolean) {
        val targetId = currentAiMessageId ?: return
        _messages.value = _messages.value.map { msg ->
            if (msg.id == targetId) {
                msg.copy(text = text, isTyping = isTyping)
            } else {
                msg
            }
        }
    }

    private fun saveAiMessage(sessionId: Long, text: String) {
        // 不保存错误消息，避免污染历史上下文导致后续解析失败
        if (text.isNotBlank() && !text.startsWith("⚠️") && !text.startsWith("网络")) {
            viewModelScope.launch(Dispatchers.IO) {
                chatDao.insertMessage(ChatMessageEntity(
                    sessionId = sessionId,
                    content = text,
                    isUser = false
                ))
                val count = chatDao.getMessageCount(sessionId)
                // 更新消息数量和最后消息预览（截取前50字符）
                val preview = if (text.length > 50) text.take(50) + "..." else text
                sessionDao.updateMessageInfo(sessionId, count, preview)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        currentEventSource?.cancel()
        messagesJob?.cancel()
    }
}
