package com.example.focusflow

import android.app.Application
import com.example.focusflow.api.RetrofitClient
import com.example.focusflow.api.UserPreferences
import com.example.focusflow.data.AppConfigManager
import com.example.focusflow.data.AppDatabase
import com.example.focusflow.data.repository.BatchSettlementResult
import com.example.focusflow.data.repository.FocusRepository
import com.example.focusflow.data.repository.SyncRepository
import com.example.focusflow.data.session.SessionManager
import com.example.focusflow.service.TimeFluxSyncService
import com.example.focusflow.utils.PlantImageLoader
import com.example.focusflow.utils.PlantBitmapLoader
import com.example.focusflow.utils.ApiCacheManager
import com.example.focusflow.utils.SoundManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * FocusFlow 应用程序入口
 *
 * 【职责】
 * 1. 初始化全局单例组件（数据库、SessionManager）
 * 2. 启动网络状态监听（端云同步基础设施）
 * 3. 加载服务端配置
 * 4. 初始化图片加载器
 * 5. 设置同步后奖励结算回调
 */
class MyApplication : Application() {

    companion object {
        @Volatile
        private var INSTANCE: MyApplication? = null

        fun getInstance(): MyApplication =
            INSTANCE ?: throw IllegalStateException("Application not initialized")
            
        // 全局结算回调（用于同步后通知 UI 显示结算弹窗）
        @Volatile
        var onBatchSettlement: ((BatchSettlementResult) -> Unit)? = null
    }

    // 全局依赖实例
    lateinit var sessionManager: SessionManager
        private set

    lateinit var syncRepository: SyncRepository
        private set
        
    lateinit var focusRepository: FocusRepository
        private set

    // 应用级协程作用域
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this

        // 初始化会话管理器
        sessionManager = SessionManager.getInstance(this)

        // 初始化数据库
        val database = AppDatabase.getDatabase(this)

        // 初始化 FocusRepository
        focusRepository = FocusRepository(
            focusRecordDao = database.focusRecordDao(),
            userDao = database.userDao(),
            sessionManager = sessionManager
        )

        // 初始化同步仓库并启动网络监听
        syncRepository = SyncRepository(
            context = this,
            focusRecordDao = database.focusRecordDao(),
            sessionManager = sessionManager
        )
        
        // 设置 FocusRepository 引用到 SyncRepository（用于结算种子）
        syncRepository.setFocusRepository(focusRepository)
        
        syncRepository.startNetworkMonitoring()

        // 初始化图片加载器
        PlantImageLoader.init(this)
        PlantBitmapLoader.init(this)
        
        // 初始化API缓存管理器
        ApiCacheManager.init(this)
        
        // 初始化音效管理器
        SoundManager.init(this)

        // 加载服务端配置
        loadConfigs()
        
        // 设置同步仓库引用到 TimeFluxSyncService（联动同步）
        val timeFluxSyncService = TimeFluxSyncService.getInstance(this)
        timeFluxSyncService.setSyncRepository(syncRepository)
        
        // 🟢 应用启动时立即启动光流同步服务
        timeFluxSyncService.startSync()
        android.util.Log.d("MyApplication", "光流同步服务已启动")
        
        // 设置同步完成监听器：同步成功后更新待同步数
        syncRepository.setOnSyncCompleteListener {
            applicationScope.launch {
                timeFluxSyncService.updatePendingSyncCount()
            }
        }
        
        // 设置奖励结算监听器：同步成功后触发结算弹窗
        syncRepository.setOnRewardSettlementListener { result ->
            android.util.Log.d("MyApplication", "奖励结算回调触发: ${result.totalRecords} 条记录")
            // 通知 UI 显示结算弹窗
            onBatchSettlement?.invoke(result)
        }
    }

    /**
     * 从服务端加载配置
     */
    private fun loadConfigs() {
        applicationScope.launch {
            try {
                val success = AppConfigManager.loadConfigs(RetrofitClient.configService)
                android.util.Log.d("MyApplication", if (success) "配置加载成功" else "配置加载失败")
                
                // 加载音效设置
                SoundManager.loadSettings(this@MyApplication)
            } catch (e: Exception) {
                android.util.Log.e("MyApplication", "配置加载异常", e)
            }
        }
    }
}
