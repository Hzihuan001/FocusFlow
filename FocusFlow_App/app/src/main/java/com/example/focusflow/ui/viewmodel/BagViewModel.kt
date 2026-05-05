package com.example.focusflow.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.focusflow.api.BagService
import com.example.focusflow.api.RetrofitClient
import com.example.focusflow.data.session.SessionManager
import com.example.focusflow.service.NetworkMonitor
import com.example.focusflow.service.TimeFluxSyncService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 开箱结果数据类
 */
data class OpenBoxResult(
    val plantId: Int,
    val plantName: String,
    val imageUrl: String?,
    val rarity: Int,
    val newTimeFlux: Int
)

/**
 * BagViewModel —— 背包系统控制中枢（纯云端版本）
 * 
 * 【架构变更】
 * 本地不再存储背包数据，所有操作直接访问云端 API。
 * 
 * 核心职责：
 * 1. 响应式分发当前用户的背包资产列表
 * 2. 调度"光流解析"事务逻辑
 * 3. 统计各频段资产状态情况
 */
class BagViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "BagViewModel"
    }

    private val sessionManager = SessionManager.getInstance(application)
    private val bagService: BagService = RetrofitClient.bagService
    private val networkMonitor = NetworkMonitor.getInstance(application)

    // 用户 ID
    private val _currentUserId = MutableStateFlow<Long?>(null)
    val currentUserId: StateFlow<Long?> = _currentUserId.asStateFlow()

    // 网络状态
    val networkStatus: StateFlow<NetworkMonitor.NetworkStatus> = networkMonitor.networkStatus

    // UI 状态
    private val _errorMsg = MutableStateFlow<String?>(null)
    val errorMsg: StateFlow<String?> = _errorMsg.asStateFlow()

    private val _isParsing = MutableStateFlow(false)
    val isParsing: StateFlow<Boolean> = _isParsing.asStateFlow()

    // 解码状态机：追踪当前正在震动的项
    private val _decodingItemId = MutableStateFlow<String?>(null)
    val decodingItemId: StateFlow<String?> = _decodingItemId.asStateFlow()

    // 稀有度反馈：存储最新开箱结果用于耀斑展示
    private val _lastRarity = MutableStateFlow<Int?>(null)
    val lastRarity: StateFlow<Int?> = _lastRarity.asStateFlow()
    
    // 开箱结果：包含更新后的光流余额
    private val _openBoxResult = MutableStateFlow<OpenBoxResult?>(null)
    val openBoxResult: StateFlow<OpenBoxResult?> = _openBoxResult.asStateFlow()

    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 背包物品列表（从云端加载）
    private val _bagItems = MutableStateFlow<List<BagService.BagItemDto>>(emptyList())
    val bagItems: StateFlow<List<BagService.BagItemDto>> = _bagItems.asStateFlow()

    init {
        // 启动网络监听
        networkMonitor.startMonitoring()
        
        viewModelScope.launch {
            _currentUserId.value = sessionManager.userIdFlow.firstOrNull()
            loadBagFromCloud()
        }
    }

    /**
     * 从云端加载背包数据
     */
    fun loadBagFromCloud() {
        viewModelScope.launch {
            val userId = _currentUserId.value ?: return@launch
            
            // 🔧 [CLOUD-ONLY] 纯云端模式：断网时提示用户
            if (!networkMonitor.isConnected) {
                _errorMsg.value = "网络不可用，无法加载背包"
                Log.w(TAG, "loadBagFromCloud: 网络不可用")
                return@launch
            }
            
            _isLoading.value = true
            try {
                Log.d(TAG, "从云端加载背包数据: userId=$userId")
                val response = bagService.getBagList(userId)
                
                if (response.isSuccessful && response.body()?.code == 200) {
                    val items = response.body()?.data ?: emptyList()
                    _bagItems.value = items
                    Log.d(TAG, "加载背包数据成功: ${items.size} 条记录")
                } else {
                    _errorMsg.value = response.body()?.message ?: "加载失败"
                }
            } catch (e: Exception) {
                Log.e(TAG, "加载背包数据失败", e)
                _errorMsg.value = "网络错误: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 执行开箱 (Gacha) - 全息状态机版
     * 1. 启动 1.5s 乱码解码动效 (decodingItemId)
     * 2. 调用云端 API 进行开箱
     * 3. 捕捉稀有度结果并触发 UI 耀斑
     */
    fun openBox(item: BagService.BagItemDto) {
        viewModelScope.launch {
            val userId = _currentUserId.value ?: return@launch
            
            // 🔧 [CLOUD-ONLY] 纯云端模式：断网时提示用户
            if (!networkMonitor.isConnected) {
                _errorMsg.value = "网络不可用，无法解析种子"
                Log.w(TAG, "openBox: 网络不可用")
                return@launch
            }
            
            // 校验是否已经在处理中
            if (_decodingItemId.value != null) return@launch

            _errorMsg.value = null
            _decodingItemId.value = item.bagId
            _isParsing.value = true

            // 1.5s 沉浸式解码仪式感 (震动/乱码时间)
            delay(1500)

            try {
                val response = bagService.openBox(userId, item.bagId)
                
                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    val data = response.body()?.data
                    if (data != null) {
                        // 更新稀有度和开箱结果
                        _lastRarity.value = data.rarity ?: 0
                        _openBoxResult.value = OpenBoxResult(
                            plantId = data.plantId,
                            plantName = data.plantName ?: "未知植物",
                            imageUrl = data.imageUrl,
                            rarity = data.rarity ?: 0,
                            newTimeFlux = data.timeFlux ?: 0
                        )
                        // 同步光流到 TimeFluxSyncService（首页和我的页面会实时更新）
                        data.timeFlux?.let { flux ->
                            TimeFluxSyncService.getInstance(getApplication()).updateTimeFluxDirectly(flux)
                        }
                        Log.d(TAG, "开箱成功: plantId=${data.plantId}, rarity=${data.rarity}, timeFlux=${data.timeFlux}")
                    }
                    // 刷新背包列表
                    loadBagFromCloud()
                } else {
                    _errorMsg.value = response.body()?.message ?: "培育失败，系统频段异常"
                }
            } catch (e: Exception) {
                Log.e(TAG, "开箱失败", e)
                _errorMsg.value = "网络错误: ${e.message}"
            } finally {
                _decodingItemId.value = null
                _isParsing.value = false
            }
        }
    }
    
    /**
     * 更新物品状态（用于种植时标记为已使用）
     */
    fun updateStatus(bagId: String, status: Int) {
        viewModelScope.launch {
            val userId = _currentUserId.value ?: return@launch
            
            // 🔧 [CLOUD-ONLY] 纯云端模式：断网时提示用户
            if (!networkMonitor.isConnected) {
                _errorMsg.value = "网络不可用，无法更新状态"
                Log.w(TAG, "updateStatus: 网络不可用")
                return@launch
            }
            
            try {
                val response = bagService.updateStatus(userId, bagId, BagService.UpdateStatusRequest(status))
                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    Log.d(TAG, "更新物品状态成功: bagId=$bagId, status=$status")
                    loadBagFromCloud()
                }
            } catch (e: Exception) {
                Log.e(TAG, "更新物品状态失败", e)
            }
        }
    }

    /**
     * 清除稀有度耀斑状态
     */
    fun clearRarityEffect() {
        _lastRarity.value = null
        _openBoxResult.value = null
    }

    fun clearError() { 
        _errorMsg.value = null 
    }
}