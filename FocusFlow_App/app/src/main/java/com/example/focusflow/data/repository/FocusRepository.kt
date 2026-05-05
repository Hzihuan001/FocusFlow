package com.example.focusflow.data.repository

import android.util.Log
import com.example.focusflow.data.dao.FocusRecordDao
import com.example.focusflow.data.dao.UserDao
import com.example.focusflow.data.entity.FocusRecordEntity
import com.example.focusflow.data.entity.UserEntity
import com.example.focusflow.utils.SecurityUtils
import com.example.focusflow.data.session.SessionManager
import com.example.focusflow.api.RetrofitClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import kotlin.random.Random

/**
 * 网络不可达异常
 */
class NetworkException(message: String) : Exception(message)

/**
 * 结算结果包装类
 */
data class FocusSettlementResult(
    val record: FocusRecordEntity,
    val rewardAmount: Int,
    val isDropped: Boolean,
    val focusMinutes: Int,
    val droppedPlantName: String? = null,
    val droppedPlantId: Int? = null,
    val purifiedTiles: Int = 0,
    val isOffline: Boolean = false,
    val recordIds: List<String> = emptyList()
)

/**
 * 批量结算结果
 */
data class BatchSettlementResult(
    val totalRewardFlux: Int,
    val totalRecords: Int,
    val totalDurationMinutes: Int,
    val droppedSeeds: List<DroppedSeed>,
    val recordIds: List<String>
)

data class DroppedSeed(
    val plantId: Int,
    val plantName: String
)

/**
 * 专注记录 Repository（精简版）
 * 
 * 【本地只存专注记录，其他数据云端化】
 * - 专注记录：本地 Room（离线优先）
 * - 用户/光流/种子：云端 API
 */
class FocusRepository(
    private val focusRecordDao: FocusRecordDao,
    private val userDao: UserDao,
    private val sessionManager: SessionManager
) {
    
    companion object {
        private const val TAG = "FocusRepository"
    }
    
    /**
     * 获取指定用户的响应式 Flow
     */
    fun getUserFlow(userId: Long): Flow<UserEntity?> = userDao.getUserFlow(userId)
    
    /**
     * 获取指定用户
     */
    suspend fun getUserById(userId: Long): UserEntity? = userDao.getUserById(userId)
    
    // ========== 核心方法：保存专注记录 ==========
    
    suspend fun saveFocusRecord(
        userId: Long,
        taskName: String,
        durationMinutes: Int,
        startTime: Long = System.currentTimeMillis()
    ): FocusRecordEntity {
        val recordId = UUID.randomUUID().toString()
        val signature = SecurityUtils.generateFocusSignature(
            recordId = recordId,
            userId = userId,
            durationMinutes = durationMinutes,
            startTime = startTime
        )
        
        val record = FocusRecordEntity(
            recordId = recordId,
            userId = userId,
            taskName = taskName,
            durationMinutes = durationMinutes,
            startTime = startTime,
            syncStatus = 0,
            signature = signature,
            rewardSettled = 0
        )
        
        focusRecordDao.insertRecord(record)
        Log.d(TAG, "专注记录已保存: recordId=$recordId, duration=$durationMinutes 分钟")
        
        return record
    }
    
    // ========== 种子掉落 ==========
    
    suspend fun settleFocusReward(record: FocusRecordEntity): DroppedSeed? {
        Log.d(TAG, "开始结算奖励: recordId=${record.recordId}")
        
        try {
            com.example.focusflow.data.AppConfigManager.reload(
                com.example.focusflow.api.RetrofitClient.configService
            )
        } catch (e: Exception) {
            Log.w(TAG, "配置刷新失败: ${e.message}")
        }
        
        val minMinutes = com.example.focusflow.data.AppConfigManager.getSeedDropMinMinutes()
        val baseRate = com.example.focusflow.data.AppConfigManager.getSeedDropBaseRate()
        
        if (record.durationMinutes < minMinutes) {
            focusRecordDao.updateRewardSettled(record.recordId, 1)
            return null
        }
        
        val currentPity = sessionManager.pityFlow.firstOrNull() ?: 0.0
        val finalChance = baseRate + currentPity
        val randomRoll = Random.nextDouble()
        
        var droppedSeed: DroppedSeed? = null
        
        if (randomRoll < finalChance) {
            try {
                val response = RetrofitClient.bagService.addBagItem(
                    record.userId,
                    com.example.focusflow.api.BagService.AddBagItemRequest(
                        plantId = 0,
                        status = 0
                    )
                )
                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    droppedSeed = DroppedSeed(plantId = 0, plantName = "未解析种子")
                    Log.d(TAG, "种子掉落成功")
                }
            } catch (e: Exception) {
                Log.e(TAG, "种子同步异常: ${e.message}")
            }
            sessionManager.resetPity()
        } else {
            sessionManager.updatePity(0.05)
        }
        
        focusRecordDao.updateRewardSettled(record.recordId, 1)
        return droppedSeed
    }
    
    suspend fun settlePendingRecords(userId: Long): BatchSettlementResult {
        val unsettledRecords = focusRecordDao.getUnsettledRecords(userId)
        
        if (unsettledRecords.isEmpty()) {
            return BatchSettlementResult(0, 0, 0, emptyList(), emptyList())
        }
        
        var totalDuration = 0
        val droppedSeeds = mutableListOf<DroppedSeed>()
        val settledRecordIds = mutableListOf<String>()
        
        for (record in unsettledRecords) {
            totalDuration += record.durationMinutes
            val seed = settleFocusReward(record)
            if (seed != null) droppedSeeds.add(seed)
            settledRecordIds.add(record.recordId)
        }
        
        return BatchSettlementResult(0, unsettledRecords.size, totalDuration, droppedSeeds, settledRecordIds)
    }
    
    // ========== 在线模式 ==========
    
    @Throws(NetworkException::class)
    suspend fun saveAndCalculateReward(
        userId: Long,
        taskName: String,
        durationMinutes: Int,
        rewardAmount: Int,
        startTime: Long = System.currentTimeMillis()
    ): FocusSettlementResult {
        if (!checkNetworkAvailable()) {
            throw NetworkException("网络不可达")
        }
        
        try {
            com.example.focusflow.data.AppConfigManager.reload(
                com.example.focusflow.api.RetrofitClient.configService
            )
        } catch (e: Exception) {
            Log.w(TAG, "配置刷新失败: ${e.message}")
        }
        
        val record = saveFocusRecord(userId, taskName, durationMinutes, startTime)
        
        val minMinutes = com.example.focusflow.data.AppConfigManager.getSeedDropMinMinutes()
        val baseRate = com.example.focusflow.data.AppConfigManager.getSeedDropBaseRate()
        
        var isDropped = false
        
        if (durationMinutes >= minMinutes) {
            val currentPity = sessionManager.pityFlow.firstOrNull() ?: 0.0
            val finalChance = baseRate + currentPity
            val randomRoll = Random.nextDouble()
            
            if (randomRoll < finalChance) {
                try {
                    val response = RetrofitClient.bagService.addBagItem(
                        userId,
                        com.example.focusflow.api.BagService.AddBagItemRequest(plantId = 0, status = 0)
                    )
                    if (response.isSuccessful && response.body()?.isSuccess == true) {
                        isDropped = true
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "种子添加失败: ${e.message}")
                }
                sessionManager.resetPity()
            } else {
                sessionManager.updatePity(0.05)
            }
        }
        
        focusRecordDao.updateRewardSettled(record.recordId, 1)
        
        return FocusSettlementResult(record, rewardAmount, isDropped, durationMinutes)
    }
    
    private suspend fun checkNetworkAvailable(): Boolean {
        return withTimeoutOrNull(2000L) {
            try {
                val response = RetrofitClient.authService.healthCheck()
                response.isSuccessful && response.body()?.isSuccess == true
            } catch (e: Exception) {
                false
            }
        } ?: false
    }
    
    // ========== 辅助方法 ==========
    
    suspend fun updateLocalTimeFlux(userId: Long, amount: Int) {
        userDao.updateTimeFlux(userId, amount)
    }
    
    suspend fun markRecordSettled(recordId: String) {
        focusRecordDao.updateRewardSettled(recordId, 1)
    }
    
    @Throws(NetworkException::class)
    suspend fun tryDropSeed(userId: Long, durationMinutes: Int): Boolean {
        if (!checkNetworkAvailable()) {
            throw NetworkException("网络不可达")
        }
        
        try {
            com.example.focusflow.data.AppConfigManager.reload(
                com.example.focusflow.api.RetrofitClient.configService
            )
        } catch (e: Exception) {
            Log.w(TAG, "配置刷新失败: ${e.message}")
        }
        
        val minMinutes = com.example.focusflow.data.AppConfigManager.getSeedDropMinMinutes()
        val baseRate = com.example.focusflow.data.AppConfigManager.getSeedDropBaseRate()
        
        if (durationMinutes < minMinutes) return false
        
        val currentPity = sessionManager.pityFlow.firstOrNull() ?: 0.0
        val finalChance = baseRate + currentPity
        val randomRoll = Random.nextDouble()
        
        return if (randomRoll < finalChance) {
            val response = RetrofitClient.bagService.addBagItem(
                userId,
                com.example.focusflow.api.BagService.AddBagItemRequest(plantId = 0, status = 0)
            )
            if (response.isSuccessful && response.body()?.isSuccess == true) {
                sessionManager.resetPity()
                true
            } else false
        } else {
            sessionManager.updatePity(0.05)
            false
        }
    }
    
    // ========== 查询方法 ==========
    
    suspend fun getAllRecords(userId: Long) = focusRecordDao.getAllRecords(userId)
    fun getAllRecordsFlow(userId: Long) = focusRecordDao.getAllRecordsFlow(userId)
    
    suspend fun getTodayRecords(userId: Long): List<FocusRecordEntity> {
        return focusRecordDao.getTodayRecords(userId, getStartOfDayTimestamp())
    }
    
    fun getTodayRecordsFlow(userId: Long) = focusRecordDao.getTodayRecordsFlow(userId, getStartOfDayTimestamp())
    
    suspend fun getWeekRecords(userId: Long): List<FocusRecordEntity> {
        return focusRecordDao.getWeekRecords(userId, getStartOfWeekTimestamp())
    }
    
    fun getWeekRecordsFlow(userId: Long) = focusRecordDao.getWeekRecordsFlow(userId, getStartOfWeekTimestamp())
    
    suspend fun getTodayTotalMinutes(userId: Long) = focusRecordDao.getTodayTotalMinutes(userId, getStartOfDayTimestamp())
    fun getTodayTotalMinutesFlow(userId: Long) = focusRecordDao.getTodayTotalMinutesFlow(userId, getStartOfDayTimestamp())
    
    suspend fun getWeekTotalMinutes(userId: Long) = focusRecordDao.getWeekTotalMinutes(userId, getStartOfWeekTimestamp())
    fun getWeekTotalMinutesFlow(userId: Long) = focusRecordDao.getWeekTotalMinutesFlow(userId, getStartOfWeekTimestamp())
    
    suspend fun getRecordsBetween(userId: Long, startTime: Long, endTime: Long) = 
        focusRecordDao.getRecordsBetween(userId, startTime, endTime)
    
    suspend fun getPendingSyncRecords(userId: Long) = focusRecordDao.getPendingSyncRecords(userId)
    suspend fun updateSyncStatus(recordId: String, status: Int) = focusRecordDao.updateSyncStatus(recordId, status)
    
    private fun getStartOfDayTimestamp(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    private fun getStartOfWeekTimestamp(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.firstDayOfWeek = java.util.Calendar.MONDAY
        calendar.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}