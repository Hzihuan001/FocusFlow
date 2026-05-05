package com.example.focusflow.service

import android.content.Context
import android.util.Log
import com.example.focusflow.api.RetrofitClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * 网络监控服务 - 端云连接状态检测
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * 【核心功能】
 * 1. 心跳检测：定期向服务端发送健康检查请求
 * 2. 状态通知：通过 StateFlow 向 UI 层推送网络状态变化
 * 3. 断网降级：触发离线模式，禁用云端依赖功能
 *
 * 【使用场景】
 * - 花园入口：检查网络状态，断网时禁用
 * - 专注同步：网络恢复后自动触发同步
 * - UI 提示：显示网络状态指示器
 *
 * @author FocusFlow Team
 * @since 2026-03
 */
class NetworkMonitor private constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "NetworkMonitor"
        private const val CHECK_INTERVAL_MS = 30_000L // 30秒心跳
        private const val TIMEOUT_MS = 5_000L // 5秒超时

        @Volatile
        private var INSTANCE: NetworkMonitor? = null

        fun getInstance(context: Context): NetworkMonitor =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: NetworkMonitor(context.applicationContext).also { INSTANCE = it }
            }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 网络状态定义
    // ═══════════════════════════════════════════════════════════════════════════

    sealed class NetworkStatus {
        /** 已连接云端 */
        object Connected : NetworkStatus()
        
        /** 断网（无法连接服务端） */
        object Disconnected : NetworkStatus()
        
        /** 检测中 */
        object Checking : NetworkStatus()
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 状态流
    // ═══════════════════════════════════════════════════════════════════════════

    private val _networkStatus = MutableStateFlow<NetworkStatus>(NetworkStatus.Checking)
    val networkStatus: StateFlow<NetworkStatus> = _networkStatus.asStateFlow()

    /** 是否已连接云端 */
    val isConnected: Boolean
        get() = _networkStatus.value is NetworkStatus.Connected

    // ═══════════════════════════════════════════════════════════════════════════
    // 协程管理
    // ═══════════════════════════════════════════════════════════════════════════

    private val monitorScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var monitorJob: Job? = null

    // ═══════════════════════════════════════════════════════════════════════════
    // 公共 API
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * 启动网络监控
     * 
     * 【触发时机】
     * - Application.onCreate()
     * - MainActivity.onCreate()
     */
    fun startMonitoring() {
        if (monitorJob?.isActive == true) return

        monitorJob = monitorScope.launch {
            while (true) {
                checkServerConnection()
                delay(CHECK_INTERVAL_MS)
            }
        }

        Log.i(TAG, "网络监控已启动")
    }

    /**
     * 停止网络监控
     */
    fun stopMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
        _networkStatus.value = NetworkStatus.Checking
        Log.i(TAG, "网络监控已停止")
    }

    /**
     * 手动触发连接检查
     * 
     * @return 是否已连接
     */
    suspend fun checkConnection(): Boolean {
        return checkServerConnection()
    }

    /**
     * 等待网络连接
     * 
     * 【使用场景】
     * 用户点击花园入口时，如果断网则等待连接
     * 
     * @param timeoutMs 超时时间（毫秒）
     * @return 是否在超时前连接成功
     */
    suspend fun waitForConnection(timeoutMs: Long = 10_000L): Boolean {
        if (isConnected) return true

        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (checkServerConnection()) return true
            delay(1000)
        }
        return false
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 内部实现
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * 检查与服务端的连接状态
     * 
     * 【检测方式】
     * 发送 GET /api/health 请求，判断服务端是否可达
     * 
     * @return 是否已连接
     */
    private suspend fun checkServerConnection(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                _networkStatus.value = NetworkStatus.Checking

                // 使用 OkHttpClient 发送健康检查请求
                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(TIMEOUT_MS / 1000, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(TIMEOUT_MS / 1000, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                val request = okhttp3.Request.Builder()
                    .url("${com.example.focusflow.api.RetrofitClient.GATEWAY_BASE_URL}health")
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                val isSuccess = response.isSuccessful

                _networkStatus.value = if (isSuccess) {
                    NetworkStatus.Connected
                } else {
                    NetworkStatus.Disconnected
                }

                if (isSuccess) {
                    Log.d(TAG, "服务端连接正常")
                } else {
                    Log.w(TAG, "服务端响应异常: ${response.code}")
                }

                isSuccess
            } catch (e: Exception) {
                _networkStatus.value = NetworkStatus.Disconnected
                Log.w(TAG, "服务端连接失败: ${e.message}")
                false
            }
        }
    }
}
