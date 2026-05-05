package com.example.focusflow.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.example.focusflow.data.dao.FocusRecordDao
import com.example.focusflow.data.dao.PlantDictDao
import com.example.focusflow.data.dao.UserDao
import com.example.focusflow.data.entity.FocusRecordEntity
import com.example.focusflow.data.session.SessionManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient

/**
 * 端云同步调度仓库 —— 离线优先架构核心组件
 *
 * 【核心功能】
 * 1. 离线专注记录保存到本地
 * 2. 网络恢复时自动同步到云端
 * 3. 同步成功后触发奖励结算（种子掉落）
 *
 * 【数据流架构】
 * ```
 * [专注完成] 
 *     ↓ 
 * [本地落库 syncStatus=0, rewardSettled=0] 
 *     ↓ 
 * [网络监听 Flow] ──→ 网络恢复
 *     ↓
 * [拉取未同步记录] 
 *     ↓ 
 * [批量Push云端] 
 *     ↓ 
 * [验签成功 → syncStatus=1]
 *     ↓
 * [触发奖励结算 → rewardSettled=1]
 *     ↓
 * [通知UI显示结算弹窗]
 * ```
 */
class SyncRepository(
    private val context: Context,
    private val focusRecordDao: FocusRecordDao,
    private val sessionManager: SessionManager
) {
    companion object {
        private const val TAG = "SyncRepository"
        private const val SYNC_BATCH_SIZE = 20
        private const val RETRY_DELAY_MS = 5000L
        private const val MAX_RETRY_COUNT = 3
    }

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ───────────── 网络状态监听 ─────────────

    private val _networkAvailable = MutableSharedFlow<Boolean>(
        replay = 1,
        extraBufferCapacity = 1
    )
    val networkAvailable: Flow<Boolean> = _networkAvailable
        .debounce(1000)

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    fun startNetworkMonitoring() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) 
            as ConnectivityManager

        val isCurrentlyConnected = isNetworkConnected(connectivityManager)
        _networkAvailable.tryEmit(isCurrentlyConnected)

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d(TAG, "网络已连接: $network")
                _networkAvailable.tryEmit(true)
                triggerSync()
            }

            override fun onLost(network: Network) {
                Log.d(TAG, "网络已断开: $network")
                _networkAvailable.tryEmit(false)
            }

            override fun onUnavailable() {
                Log.d(TAG, "网络不可用")
                _networkAvailable.tryEmit(false)
            }
        }

        connectivityManager.registerNetworkCallback(request, networkCallback!!)
        Log.i(TAG, "网络状态监听已启动")
    }

    fun stopNetworkMonitoring() {
        networkCallback?.let {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) 
                as ConnectivityManager
            connectivityManager.unregisterNetworkCallback(it)
        }
        networkCallback = null
        Log.i(TAG, "网络状态监听已停止")
    }

    private fun isNetworkConnected(connectivityManager: ConnectivityManager): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    // ───────────── 同步调度核心逻辑 ─────────────

    private var syncJob: Job? = null

    fun triggerManualSync() {
        Log.i(TAG, "用户触发手动同步")
        triggerSync()
    }

    private fun triggerSync() {
        syncJob?.cancel()

        syncJob = syncScope.launch {
            try {
                performSync()
            } catch (e: CancellationException) {
                Log.d(TAG, "同步任务被取消")
            } catch (e: Exception) {
                Log.e(TAG, "同步任务异常: ${e.message}", e)
            }
        }
    }

    // ───────────── 结算回调 ─────────────

    /**
     * 同步完成回调（用于通知 TimeFluxSyncService 更新待同步数）
     */
    private var onSyncCompleteListener: (() -> Unit)? = null

    fun setOnSyncCompleteListener(listener: () -> Unit) {
        onSyncCompleteListener = listener
    }
    
    /**
     * 奖励结算回调（用于通知 UI 显示结算弹窗）
     */
    private var onRewardSettlementListener: ((BatchSettlementResult) -> Unit)? = null
    
    fun setOnRewardSettlementListener(listener: (BatchSettlementResult) -> Unit) {
        onRewardSettlementListener = listener
    }

    // ───────────── FocusRepository 引用 ─────────────
    
    private var focusRepository: FocusRepository? = null
    
    fun setFocusRepository(repository: FocusRepository) {
        focusRepository = repository
    }

    // ───────────── 同步核心流程 ─────────────
    
    // 累计光流奖励（同步过程中累加）
    private var accumulatedRewardFlux = 0

    private suspend fun performSync() {
        Log.i(TAG, "====== 开始执行同步流程 ======")
        
        // 重置累计光流
        accumulatedRewardFlux = 0

        val userId = try {
            sessionManager.requireUserId()
        } catch (e: Exception) {
            Log.w(TAG, "用户未登录，跳过同步")
            return
        }

        val pendingRecords = focusRecordDao.getPendingSyncRecords(userId)
        if (pendingRecords.isEmpty()) {
            Log.i(TAG, "无待同步记录，同步完成")
            return
        }

        Log.i(TAG, "发现 ${pendingRecords.size} 条待同步记录")

        pendingRecords.chunked(SYNC_BATCH_SIZE).forEach { batch ->
            syncBatch(batch)
        }

        // 同步完成后触发奖励结算
        triggerRewardSettlement(userId)

        onSyncCompleteListener?.invoke()

        Log.i(TAG, "====== 同步流程执行完毕 ======")
    }

    private suspend fun syncBatch(records: List<FocusRecordEntity>) {
        Log.d(TAG, "同步批次: ${records.size} 条记录")

        val syncRecords = records.map { record ->
            SyncRecord(
                recordId = record.recordId,
                userId = record.userId,
                taskName = record.taskName,
                durationMinutes = record.durationMinutes,
                startTime = record.startTime,
                signature = record.signature
            )
        }
        
        val request = BatchSyncRequest(records = syncRecords)

        try {
            val response = syncApiService.batchSyncFocusRecords(request)

            if (response.isSuccessful && response.body()?.code == 200) {
                val syncResults = response.body()?.data ?: emptyList()

                syncResults.forEach { result ->
                    if (result.success) {
                        focusRecordDao.updateSyncStatus(result.recordId, 1)
                        // 累加光流奖励
                        accumulatedRewardFlux += result.rewardFlux ?: 0
                        Log.d(TAG, "记录 ${result.recordId} 同步成功, 光流奖励: ${result.rewardFlux}")
                    } else {
                        Log.w(TAG, "记录 ${result.recordId} 同步失败: ${result.message}")
                    }
                }
            } else {
                Log.e(TAG, "批量同步请求失败: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "批量同步网络异常: ${e.message}", e)
        }
    }
    
    /**
     * 触发奖励结算
     * 【同步成功后调用】结算种子掉落并通知UI
     */
    private suspend fun triggerRewardSettlement(userId: Long) {
        Log.i(TAG, "====== 开始触发奖励结算 ======")
        
        val repo = focusRepository
        if (repo == null) {
            Log.w(TAG, "FocusRepository 未设置，跳过结算")
            return
        }
        
        // 获取已同步但未结算的记录
        val unsettledRecords = focusRecordDao.getUnsettledRecords(userId)
        if (unsettledRecords.isEmpty()) {
            Log.i(TAG, "无待结算记录")
            return
        }
        
        Log.i(TAG, "发现 ${unsettledRecords.size} 条待结算记录，开始结算...")
        
        // 执行批量结算（种子掉落）
        val seedResult = repo.settlePendingRecords(userId)
        
        // 构建完整的结算结果（包含光流）
        val result = seedResult.copy(
            totalRewardFlux = accumulatedRewardFlux
        )
        
        if (result.totalRecords > 0 || result.droppedSeeds.isNotEmpty() || result.totalRewardFlux > 0) {
            // 通知UI显示结算弹窗
            onRewardSettlementListener?.invoke(result)
            Log.i(TAG, "结算结果已通知UI: ${result.totalRecords} 条记录, ${result.droppedSeeds.size} 颗种子, ${result.totalRewardFlux} 光流")
        }
    }

    // ───────────── Retrofit API 服务 ─────────────

    interface SyncApiService {
        @POST("sync/focus-records/batch")
        suspend fun batchSyncFocusRecords(@Body request: BatchSyncRequest): retrofit2.Response<SyncResponse>
    }

    data class BatchSyncRequest(
        val records: List<SyncRecord>
    )

    data class SyncRecord(
        val recordId: String,
        val userId: Long,
        val taskName: String,
        val durationMinutes: Int,
        val startTime: Long,
        val signature: String
    )

    data class SyncResponse(
        val code: Int,
        val message: String?,
        val data: List<SyncResult>?
    )

    data class SyncResult(
        val recordId: String,
        val success: Boolean,
        val message: String = "",
        val rewardFlux: Int? = null  // 后端返回的光流奖励
    )

    private val syncApiService: SyncApiService by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(com.example.focusflow.api.RetrofitClient.GATEWAY_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SyncApiService::class.java)
    }

    // ───────────── 状态查询接口 ─────────────

    fun getPendingSyncCountFlow(userId: Long): Flow<Int> = flow {
        while (true) {
            val count = focusRecordDao.getPendingSyncRecords(userId).size
            emit(count)
            delay(5000)
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getSyncSummary(): SyncSummary {
        return try {
            val userId = sessionManager.requireUserId()
            val pendingCount = focusRecordDao.getPendingSyncRecords(userId).size

            SyncSummary(
                pendingCount = pendingCount,
                isNetworkAvailable = _networkAvailable.replayCache.firstOrNull() ?: false,
                lastSyncTime = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            SyncSummary(pendingCount = 0, isNetworkAvailable = false, lastSyncTime = 0)
        }
    }

    data class SyncSummary(
        val pendingCount: Int,
        val isNetworkAvailable: Boolean,
        val lastSyncTime: Long
    )
}