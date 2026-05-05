package com.example.focusflow.service

import android.content.Context
import android.util.Log
import com.example.focusflow.api.AuthService
import com.example.focusflow.api.RetrofitClient
import com.example.focusflow.data.AppDatabase
import com.example.focusflow.data.repository.SyncRepository
import com.example.focusflow.data.session.SessionManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.firstOrNull

/**
 * TimeFlux 同步服务 —— 光流余额云端同步引擎
 *
 * 【核心功能】
 * 1. 定期从服务端拉取最新的 time_flux（光流余额）
 * 2. 更新本地 Room 数据库
 * 3. 通过 Flow 驱动 UI 实时更新
 * 4. 同步专注记录（调用 SyncRepository）
 *
 * 【设计理念】
 * - 离线优先：本地数据作为主数据源，云端同步仅作为校准
 * - 增量同步：仅同步变化的余额，减少网络开销
 * - 响应式更新：UI 订阅 Flow，无需手动刷新
 *
 * 【轮询策略】
 * - 默认间隔：30秒
 * - 用户在前台时：启用轮询
 * - 用户在后台时：暂停轮询（省电）
 */
class TimeFluxSyncService private constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "TimeFluxSyncService"
        private const val SYNC_INTERVAL_MS = 30_000L // 30秒

        @Volatile
        private var INSTANCE: TimeFluxSyncService? = null

        fun getInstance(context: Context): TimeFluxSyncService =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: TimeFluxSyncService(context.applicationContext).also { INSTANCE = it }
            }
    }

    // ───────────── 依赖组件 ─────────────

    private val sessionManager = SessionManager.getInstance(context)
    private val userDao = AppDatabase.getDatabase(context).userDao()
    private val authService: AuthService = RetrofitClient.authService
    
    // 同步仓库引用（用于同步专注记录）
    private var syncRepository: SyncRepository? = null

    // ───────────── 状态流 ─────────────

    /** 光流余额（从服务端同步的最新值） */
    private val _timeFlux = MutableStateFlow<Int?>(null)
    val timeFlux: StateFlow<Int?> = _timeFlux.asStateFlow()

    /** 同步状态 */
    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    /** 待同步记录数 */
    private val _pendingSyncCount = MutableStateFlow(0)
    val pendingSyncCount: StateFlow<Int> = _pendingSyncCount.asStateFlow()
    
    /** 最后同步时间 */
    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime: StateFlow<Long?> = _lastSyncTime.asStateFlow()

    sealed class SyncStatus {
        object Idle : SyncStatus()
        object Syncing : SyncStatus()
        data class Success(val timeFlux: Int) : SyncStatus()
        data class Error(val message: String) : SyncStatus()
    }

    // ───────────── 协程管理 ─────────────

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var syncJob: Job? = null

    /**
     * 设置同步仓库引用
     * 在 MyApplication 中调用，用于联动同步专注记录
     */
    fun setSyncRepository(repository: SyncRepository) {
        syncRepository = repository
    }

    // ───────────── 公共 API ─────────────

    /**
     * 启动定期同步
     *
     * 【触发时机】
     * - 应用进入前台（MainActivity onResume）
     * - 用户手动刷新
     */
    fun startSync() {
        if (syncJob?.isActive == true) return

        syncJob = serviceScope.launch {
            while (true) {
                try {
                    syncTimeFlux()
                } catch (e: CancellationException) {
                    Log.d(TAG, "同步任务被取消")
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "同步异常: ${e.message}")
                    _syncStatus.value = SyncStatus.Error(e.message ?: "未知错误")
                }

                delay(SYNC_INTERVAL_MS)
            }
        }

        Log.i(TAG, "光流同步服务已启动")
    }

    /**
     * 停止定期同步
     *
     * 【触发时机】
     * - 应用进入后台（MainActivity onPause）
     */
    fun stopSync() {
        syncJob?.cancel()
        syncJob = null
        _syncStatus.value = SyncStatus.Idle
        Log.i(TAG, "光流同步服务已停止")
    }

    /**
     * 手动触发同步（用户下拉刷新）
     */
    fun triggerManualSync() {
        serviceScope.launch {
            syncTimeFlux()
        }
    }
    
    /**
     * 直接更新光流值（用于结算后立即更新UI）
     * 不触发网络请求，仅更新本地状态
     */
    fun updateTimeFluxDirectly(newValue: Int) {
        _timeFlux.value = newValue
        Log.d(TAG, "光流值直接更新: $newValue")
    }

    // ───────────── 内部实现 ─────────────

    /**
     * 从服务端同步光流余额
     */
    private suspend fun syncTimeFlux() {
        _syncStatus.value = SyncStatus.Syncing

        try {
            // 1. 获取当前用户 ID
            val userId = sessionManager.userIdFlow.firstOrNull()
            if (userId == null) {
                Log.w(TAG, "用户未登录，跳过同步")
                _syncStatus.value = SyncStatus.Idle
                return
            }

            // 2. 调用服务端 API
            val response = authService.getUserInfo(userId)

            if (response.isSuccessful && response.body()?.isSuccess == true) {
                val userInfo = response.body()?.data
                if (userInfo != null) {
                    // 3. 更新本地数据库
                    userDao.setTimeFlux(userId, userInfo.timeFlux)

                    // 4. 更新状态流
                    _timeFlux.value = userInfo.timeFlux
                    _syncStatus.value = SyncStatus.Success(userInfo.timeFlux)
                    _lastSyncTime.value = System.currentTimeMillis()

                    Log.d(TAG, "光流同步成功: userId=$userId, timeFlux=${userInfo.timeFlux}")
                }
            } else {
                val errorMsg = response.body()?.message ?: "服务器错误 (${response.code()})"
                _syncStatus.value = SyncStatus.Error(errorMsg)
                Log.w(TAG, "光流同步失败: $errorMsg")
            }
            
            // 5. 同步专注记录（调用 SyncRepository）
            syncRepository?.triggerManualSync()

        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.Error(e.message ?: "网络错误")
            Log.e(TAG, "光流同步异常: ${e.message}", e)
        }

        // 6. 更新待同步记录数
        updatePendingSyncCount()
    }

    /**
     * 更新待同步记录数（供外部调用）
     */
    suspend fun updatePendingSyncCount() {
        try {
            val userId = sessionManager.userIdFlow.firstOrNull()
            if (userId != null) {
                val count = AppDatabase.getDatabase(context)
                    .focusRecordDao()
                    .getPendingSyncCount(userId)
                _pendingSyncCount.value = count
                Log.d(TAG, "待同步记录数: $count")
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取待同步记录数失败: ${e.message}")
        }
    }
}
