package com.example.focusflow.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.focusflow.data.entity.FocusRecordEntity
import kotlinx.coroutines.flow.Flow

/**
 * 专注记录数据访问对象
 */
@Dao
interface FocusRecordDao {
    
    /**
     * 插入专注记录
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: FocusRecordEntity)
    
    /**
     * 批量插入专注记录
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<FocusRecordEntity>)
    
    @Query("SELECT * FROM app_focus_record WHERE userId = :userId ORDER BY startTime DESC")
    suspend fun getAllRecords(userId: Long): List<FocusRecordEntity>
    
    @Query("SELECT * FROM app_focus_record WHERE userId = :userId ORDER BY startTime DESC")
    fun getAllRecordsFlow(userId: Long): Flow<List<FocusRecordEntity>>
    
    @Query("""
        SELECT * FROM app_focus_record 
        WHERE userId = :userId AND startTime >= :startOfDay 
        ORDER BY startTime DESC
    """)
    suspend fun getTodayRecords(userId: Long, startOfDay: Long): List<FocusRecordEntity>
    
    @Query("""
        SELECT * FROM app_focus_record 
        WHERE userId = :userId AND startTime >= :startOfDay 
        ORDER BY startTime DESC
    """)
    fun getTodayRecordsFlow(userId: Long, startOfDay: Long): Flow<List<FocusRecordEntity>>
    
    @Query("""
        SELECT * FROM app_focus_record 
        WHERE userId = :userId AND startTime >= :startOfWeek 
        ORDER BY startTime DESC
    """)
    suspend fun getWeekRecords(userId: Long, startOfWeek: Long): List<FocusRecordEntity>
    
    @Query("""
        SELECT * FROM app_focus_record 
        WHERE userId = :userId AND startTime >= :startOfWeek 
        ORDER BY startTime DESC
    """)
    fun getWeekRecordsFlow(userId: Long, startOfWeek: Long): Flow<List<FocusRecordEntity>>
    
    @Query("""
        SELECT * FROM app_focus_record 
        WHERE userId = :userId 
        AND startTime >= :startTime 
        AND startTime <= :endTime 
        ORDER BY startTime DESC
    """)
    suspend fun getRecordsBetween(
        userId: Long,
        startTime: Long,
        endTime: Long
    ): List<FocusRecordEntity>
    
    @Query("""
        SELECT * FROM app_focus_record 
        WHERE userId = :userId 
        AND startTime >= :startTime 
        AND startTime <= :endTime 
        ORDER BY startTime DESC
    """)
    fun getRecordsBetweenFlow(
        userId: Long,
        startTime: Long,
        endTime: Long
    ): Flow<List<FocusRecordEntity>>
    
    @Query("""
        SELECT COALESCE(SUM(durationMinutes), 0) 
        FROM app_focus_record 
        WHERE userId = :userId AND startTime >= :startOfDay
    """)
    suspend fun getTodayTotalMinutes(userId: Long, startOfDay: Long): Int
    
    @Query("""
        SELECT COALESCE(SUM(durationMinutes), 0) 
        FROM app_focus_record 
        WHERE userId = :userId AND startTime >= :startOfDay
    """)
    fun getTodayTotalMinutesFlow(userId: Long, startOfDay: Long): Flow<Int>
    
    @Query("""
        SELECT COALESCE(SUM(durationMinutes), 0) 
        FROM app_focus_record 
        WHERE userId = :userId AND startTime >= :startOfWeek
    """)
    suspend fun getWeekTotalMinutes(userId: Long, startOfWeek: Long): Int
    
    @Query("""
        SELECT COALESCE(SUM(durationMinutes), 0) 
        FROM app_focus_record 
        WHERE userId = :userId AND startTime >= :startOfWeek
    """)
    fun getWeekTotalMinutesFlow(userId: Long, startOfWeek: Long): Flow<Int>
    
    @Query("SELECT * FROM app_focus_record WHERE userId = :userId AND syncStatus = 0")
    suspend fun getPendingSyncRecords(userId: Long): List<FocusRecordEntity>
    
    @Query("UPDATE app_focus_record SET syncStatus = :status WHERE recordId = :recordId")
    suspend fun updateSyncStatus(recordId: String, status: Int)
    
    /**
     * 批量更新同步状态
     * 用于端云同步成功后批量标记
     */
    @Query("UPDATE app_focus_record SET syncStatus = :status WHERE recordId IN (:recordIds)")
    suspend fun updateSyncStatusBatch(recordIds: List<String>, status: Int)
    
    /**
     * 获取待同步记录数量
     */
    @Query("SELECT COUNT(*) FROM app_focus_record WHERE userId = :userId AND syncStatus = 0")
    suspend fun getPendingSyncCount(userId: Long): Int
    
    @Query("SELECT * FROM app_focus_record WHERE recordId = :recordId")
    suspend fun getRecordById(recordId: String): FocusRecordEntity?
    
    @Query("DELETE FROM app_focus_record WHERE userId = :userId")
    suspend fun deleteAllRecords(userId: Long)
    
    // ========== 奖励结算状态管理（离线优先架构核心） ==========
    
    /**
     * 获取已同步但未结算奖励的记录
     * 用于同步成功后延迟结算种子和光流
     */
    @Query("SELECT * FROM app_focus_record WHERE userId = :userId AND syncStatus = 1 AND rewardSettled = 0")
    suspend fun getUnsettledRecords(userId: Long): List<FocusRecordEntity>
    
    /**
     * 更新奖励结算状态
     */
    @Query("UPDATE app_focus_record SET rewardSettled = :settled WHERE recordId = :recordId")
    suspend fun updateRewardSettled(recordId: String, settled: Int)
    
    /**
     * 批量更新奖励结算状态
     */
    @Query("UPDATE app_focus_record SET rewardSettled = :settled WHERE recordId IN (:recordIds)")
    suspend fun updateRewardSettledBatch(recordIds: List<String>, settled: Int)
}
