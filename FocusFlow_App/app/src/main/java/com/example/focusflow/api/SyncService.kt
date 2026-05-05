package com.example.focusflow.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * 同步服务API
 * 
 * 用于将本地离线产生的专注记录同步到云端
 */
interface SyncService {
    
    /**
     * 批量同步专注记录
     * 
     * @param request 批量同步请求
     */
    @POST("sync/focus-records/batch")
    suspend fun batchSyncFocusRecords(@Body request: BatchSyncRequest): Response<BatchSyncResponse>
}

/**
 * 批量同步请求
 */
data class BatchSyncRequest(
    val records: List<FocusRecordSyncDTO>
)

/**
 * 单条专注记录同步DTO
 */
data class FocusRecordSyncDTO(
    val recordId: String,
    val userId: Long,
    val taskName: String,
    val durationMinutes: Int,
    val startTime: Long,
    val signature: String
)

/**
 * 批量同步响应
 */
data class BatchSyncResponse(
    val code: Int,
    val message: String?,
    val data: List<SyncResultDTO>?
)

/**
 * 单条记录同步结果
 */
data class SyncResultDTO(
    val recordId: String,
    val success: Boolean,
    val message: String?
)
