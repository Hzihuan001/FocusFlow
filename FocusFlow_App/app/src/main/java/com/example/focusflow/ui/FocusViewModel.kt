package com.example.focusflow.ui

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.focusflow.api.ChatMessage
import com.example.focusflow.api.ChatRequest
import com.example.focusflow.api.ChatResponseChunk
import com.example.focusflow.api.GardenService
import com.example.focusflow.api.RetrofitClient
import com.example.focusflow.api.ZhipuService
import com.example.focusflow.data.AppDatabase
import com.example.focusflow.data.ChatMessageEntity
import com.example.focusflow.data.entity.UserEntity
import com.example.focusflow.data.repository.BatchSettlementResult
import com.example.focusflow.data.repository.FocusRepository
import com.example.focusflow.data.repository.FocusSettlementResult
import com.example.focusflow.data.session.SessionManager
import com.example.focusflow.service.FocusStateManager
import com.example.focusflow.service.TimeFluxSyncService
import com.example.focusflow.utils.SoundManager
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.roundToInt


data class FocusUiMessage(val role: String, val content: String)

/**
 * 离线专注结果
 */
data class OfflineFocusResult(
    val taskName: String,
    val durationMinutes: Int
)

class FocusViewModel(application: Application) : AndroidViewModel(application) {

    // --- 状态流：专注引擎桥接 ---
    val isFocusing: StateFlow<Boolean> = FocusStateManager.isFocusing
    
    // --- 状态流：AI 聊天与隐私隔离 ---
    private val db = AppDatabase.getDatabase(application)
    private val chatDao = db.chatDao()
    
    private val focusRepository = FocusRepository(
        db.focusRecordDao(),
        db.userDao(),
        SessionManager.getInstance(application)
    )

    private val sessionManager = SessionManager.getInstance(application)

    /**
     * 光流余额从 SessionManager 缓存获取
     */
    private val _timeFluxBalance = MutableStateFlow(0)
    val timeFluxBalance: StateFlow<Int> = _timeFluxBalance.asStateFlow()

    // ═══════════════════════════════════════════════════════════════
    // 🟢 [PULL TO REFRESH] 下拉刷新状态
    // ═══════════════════════════════════════════════════════════════
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    /**
     * 下拉刷新：重新检测连接 + 同步光流
     */
    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                // 触发 TimeFluxSyncService 同步
                TimeFluxSyncService.getInstance(getApplication()).triggerManualSync()
                // 等待同步完成（最多 3 秒）
                kotlinx.coroutines.delay(500)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    init {
        // 监听用户变化更新光流
        viewModelScope.launch {
            sessionManager.userIdFlow.collect { userId ->
                if (userId != null) {
                    val user = focusRepository.getUserById(userId)
                    _timeFluxBalance.value = user?.timeFlux ?: 0
                } else {
                    _timeFluxBalance.value = 0
                }
            }
        }
    }
        
    // --- 状态流：在线结算弹窗 ---
    private val _showSummaryDialog = MutableStateFlow(false)
    val showSummaryDialog: StateFlow<Boolean> = _showSummaryDialog.asStateFlow()
    
    private val _lastFocusReward = MutableStateFlow(0)
    val lastFocusReward: StateFlow<Int> = _lastFocusReward.asStateFlow()

    // 单次专注结算结果（在线模式使用）
    private val _settlementResult = MutableStateFlow<FocusSettlementResult?>(null)
    val settlementResult: StateFlow<FocusSettlementResult?> = _settlementResult.asStateFlow()
    
    // --- 状态流：离线模式弹窗 ---
    private val _showOfflineDialog = MutableStateFlow(false)
    val showOfflineDialog: StateFlow<Boolean> = _showOfflineDialog.asStateFlow()
    
    private val _offlineResult = MutableStateFlow<OfflineFocusResult?>(null)
    val offlineResult: StateFlow<OfflineFocusResult?> = _offlineResult.asStateFlow()
    
    // --- 状态流：批量结算弹窗 ---
    private val _batchSettlementResult = MutableStateFlow<BatchSettlementResult?>(null)
    val batchSettlementResult: StateFlow<BatchSettlementResult?> = _batchSettlementResult.asStateFlow()
    
    private val _showBatchSettlementDialog = MutableStateFlow(false)
    val showBatchSettlementDialog: StateFlow<Boolean> = _showBatchSettlementDialog.asStateFlow()

    val chatHistory = chatDao.getAllMessages().map { list ->
        list.map { FocusUiMessage(role = if (it.isUser) "user" else "assistant", content = it.content) }
    }

    private val _streamingText = MutableStateFlow("")
    val streamingText: StateFlow<String> = _streamingText.asStateFlow()

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://open.bigmodel.cn/api/paas/v4/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ZhipuService::class.java)

    private val gardenService: GardenService = RetrofitClient.gatewayService

    fun sendChatMessage(userText: String) {
        if (userText.isBlank() || _isStreaming.value) return

        viewModelScope.launch {
            val userMsg = ChatMessageEntity(content = userText, isUser = true)
            chatDao.insertMessage(userMsg)

            // 获取历史上下文（取最近6条消息）
            val historyMessages = chatDao.getAllMessagesOnce().takeLast(6).map { entity ->
                ChatMessage(
                    role = if (entity.isUser) "user" else "assistant",
                    content = entity.content
                )
            }.toMutableList()
            
            // 添加系统提示
            historyMessages.add(0, ChatMessage(
                role = "system", 
                content = "你是一个网络赛博朋克废土风格的专注助手，用词偏向AI系统和电子风格。用户正在专注。"
            ))
            
            // 【安全加固】通过后端代理调用 AI，API Key 存储在服务端
            val gson = Gson()
            val messagesJson = gson.toJson(historyMessages)
            
            // 获取当前用户ID
            val userId = sessionManager.userIdFlow.firstOrNull()

            _isStreaming.value = true
            _streamingText.value = ""

            withContext(Dispatchers.IO) {
                try {
                    val response = retrofit.streamChat(messagesJson, model = "glm-4-flash", userId = userId).execute()
                    if (response.isSuccessful && response.body() != null) {
                        val inputStream = response.body()!!.byteStream()
                        val reader = BufferedReader(InputStreamReader(inputStream))
                        var line: String?

                        val completeAiResponse = StringBuilder()

                        while (reader.readLine().also { line = it } != null) {
                            if (line!!.startsWith("data: ")) {
                                val jsonStr = line!!.substring(6)
                                if (jsonStr == "[DONE]") break

                                try {
                                    val chunk = gson.fromJson(jsonStr, ChatResponseChunk::class.java)
                                    val delta = chunk?.choices?.firstOrNull()?.delta?.content
                                    if (delta != null) {
                                        completeAiResponse.append(delta)
                                        withContext(Dispatchers.Main) {
                                            _streamingText.value = completeAiResponse.toString()
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                        
                        val aiMsg = ChatMessageEntity(
                            content = completeAiResponse.toString(), 
                            isUser = false
                        )
                        chatDao.insertMessage(aiMsg)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    withContext(Dispatchers.Main) {
                        _isStreaming.value = false
                        _streamingText.value = ""
                    }
                }
            }
        }
    }
    
    fun clearChat() {
        viewModelScope.launch { chatDao.clearHistory() }
    }
    
    /**
     * 捕获 FocusService 的倒计时结束事件
     * 【离线优先架构核心】
     * 优化：先保存本地记录，再检测网络，快速响应用户
     */
    fun handleFocusFinish(cycleFocusSeconds: Long, taskName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val durationMinutes = (cycleFocusSeconds / 60.0).roundToInt().coerceAtLeast(1)
                val userId = sessionManager.requireUserId()

                // ========== 检测网络和后端可用性 ==========
                val isOnline = isNetworkAvailable()
                Log.d("FocusDebug", "网络和后端检测: isOnline=$isOnline")

                if (!isOnline) {
                    // 离线模式：直接保存本地记录并显示离线弹窗
                    Log.d("FocusDebug", "离线模式，快速显示离线弹窗")
                    val record = focusRepository.saveFocusRecord(
                        userId = userId,
                        taskName = taskName,
                        durationMinutes = durationMinutes
                    )
                    // 更新待同步数（立即刷新MineScreen）
                    try {
                        TimeFluxSyncService.getInstance(getApplication()).updatePendingSyncCount()
                    } catch (e: Exception) {
                        Log.e("FocusDebug", "更新待同步数失败: ${e.message}")
                    }
                    withContext(Dispatchers.Main) {
                        _offlineResult.value = OfflineFocusResult(taskName, durationMinutes)
                        _showOfflineDialog.value = true
                    }
                    return@launch
                }

                // ========== 在线模式：刷新配置并结算 ==========
                try {
                    com.example.focusflow.data.AppConfigManager.reload(
                        com.example.focusflow.api.RetrofitClient.configService
                    )
                } catch (e: Exception) {
                    Log.w("FocusViewModel", "配置刷新失败: ${e.message}")
                }
                
                val rewardPerMinute = com.example.focusflow.data.AppConfigManager.getFocusRewardPerMinute()
                val expectedReward = durationMinutes * rewardPerMinute

                Log.d("FocusDebug", "专注完成: durationMinutes=$durationMinutes, rewardPerMinute=$rewardPerMinute, expectedReward=$expectedReward")

                // ========== 先保存本地记录（快速完成）==========
                val record = focusRepository.saveFocusRecord(
                    userId = userId,
                    taskName = taskName,
                    durationMinutes = durationMinutes
                )
                Log.d("FocusDebug", "本地记录已保存: recordId=${record.recordId}")

                // ========== 在线结算（立即显示结果，后台同步）==========
                Log.d("FocusDebug", "在线模式，立即结算...")
                
                // 更新本地光流（用户立即看到结果）
                focusRepository.updateLocalTimeFlux(userId, expectedReward)
                
                // 种子掉落判定
                var isDropped = false
                try {
                    isDropped = focusRepository.tryDropSeed(userId, durationMinutes)
                } catch (e: Exception) {
                    Log.e("FocusDebug", "种子掉落判定失败: ${e.message}")
                }

                // 调用专注充能API（允许失败）
                var purifiedTiles = 0
                try {
                    val purifyResponse = gardenService.purifyOnFocus(userId, durationMinutes)
                    if (purifyResponse.code == 200 && purifyResponse.data != null) {
                        purifiedTiles = purifyResponse.data
                        Log.d("FocusDebug", "专注充能完成: $purifiedTiles 格")
                    }
                } catch (e: Exception) {
                    Log.e("FocusDebug", "专注充能失败: ${e.message}")
                }

                // 标记为已结算（本地）
                focusRepository.markRecordSettled(record.recordId)
                
                // 更新待同步数（立即刷新MineScreen）
                try {
                    TimeFluxSyncService.getInstance(getApplication()).updatePendingSyncCount()
                } catch (e: Exception) {
                    Log.e("FocusDebug", "更新待同步数失败: ${e.message}")
                }

                // 显示在线结算弹窗
                withContext(Dispatchers.Main) {
                    SoundManager.playFocusComplete() // 🎵 专注完成音效
                    _lastFocusReward.value = expectedReward
                    // 立即更新光流显示（三处同步）
                    val newFlux = _timeFluxBalance.value + expectedReward
                    _timeFluxBalance.value = newFlux
                    // 同步到 TimeFluxSyncService（首页和我的页面会实时更新）
                    TimeFluxSyncService.getInstance(getApplication()).updateTimeFluxDirectly(newFlux)
                    _settlementResult.value = FocusSettlementResult(
                        record = record,
                        rewardAmount = expectedReward,
                        isDropped = isDropped,
                        focusMinutes = durationMinutes,
                        purifiedTiles = purifiedTiles
                    )
                    _showSummaryDialog.value = true
                    Log.d("FocusDebug", "在线结算完成！掉落: $isDropped, 净化: $purifiedTiles")
                }
                
                // 不立即触发同步，避免服务器旧值覆盖本地新值
                // 专注记录会在下次定时同步或应用启动时自动同步
            } catch (e: Exception) {
                Log.e("FocusDebug", "专注结算失败: ${e.message}", e)
            }
        }
    }
    
    /**
     * 处理同步后的批量结算
     * 【由 SyncRepository 同步成功后调用】
     */
    fun handleBatchSettlement(result: BatchSettlementResult) {
        viewModelScope.launch(Dispatchers.Main) {
            Log.d("FocusDebug", "批量结算完成: ${result.totalRecords} 条记录, ${result.droppedSeeds.size} 颗种子")
            
            _batchSettlementResult.value = result
            _showBatchSettlementDialog.value = true
        }
    }
    
    /**
     * 关闭在线结算弹窗
     */
    fun dismissSummaryDialog() {
        _showSummaryDialog.value = false
        _lastFocusReward.value = 0
        _settlementResult.value = null
    }
    
    /**
     * 关闭离线模式弹窗
     */
    fun dismissOfflineDialog() {
        _showOfflineDialog.value = false
        _offlineResult.value = null
    }
    
    /**
     * 关闭批量结算弹窗
     */
    fun dismissBatchSettlementDialog() {
        _showBatchSettlementDialog.value = false
        _batchSettlementResult.value = null
    }
    
    /**
     * 快速检测系统网络连接（仅检查系统状态，无HTTP请求）
     * 用于离线模式下快速判断，避免等待HTTP超时
     */
    private fun isSystemNetworkConnected(): Boolean {
        val connectivityManager = getApplication<Application>()
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    
    /**
     * 检查网络是否可用
     * 直接通过健康检查验证后端可达性（2秒快速超时）
     * 
     * 注意：不再依赖 NET_CAPABILITY_VALIDATED，因为它在某些环境下可能误报
     */
    private suspend fun isNetworkAvailable(): Boolean {
        // 直接尝试健康检查（2秒超时）
        return try {
            Log.d("FocusDebug", "开始后端健康检查...")
            val response = RetrofitClient.quickHealthService.healthCheck()
            val isHealthy = response.isSuccessful && response.body()?.isSuccess == true
            Log.d("FocusDebug", "后端健康检查结果: isHealthy=$isHealthy, code=${response.body()?.code}")
            isHealthy
        } catch (e: Exception) {
            Log.e("FocusDebug", "后端健康检查失败: ${e.javaClass.simpleName} - ${e.message}")
            false
        }
    }
}
