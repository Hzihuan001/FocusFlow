package com.example.focusflow.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 专注记录云端服务
 * 
 * 用于从云端拉取用户专注记录数据
 */
interface FocusService {
    
    /**
     * 获取用户专注记录
     * 
     * @param userId 用户ID
     * @param limit 返回记录数量限制
     */
    @GET("focus/records/{userId}")
    suspend fun getUserFocusRecords(
        @Path("userId") userId: Long,
        @Query("limit") limit: Int = 100
    ): Response<FocusRecordsResponse>
    
    /**
     * 获取用户专注统计
     */
    @GET("focus/stats/{userId}")
    suspend fun getUserFocusStats(
        @Path("userId") userId: Long
    ): Response<FocusStatsResponse>
}

/**
 * 专注记录列表响应
 */
data class FocusRecordsResponse(
    val code: Int,
    val message: String?,
    val data: List<CloudFocusRecord>?
)

/**
 * 云端专注记录数据类
 */
data class CloudFocusRecord(
    val recordId: String,
    val userId: Long,
    val taskName: String?,
    val durationMinutes: Int?,
    val startTime: Long?,
    val signature: String?,
    val syncTime: Long?,
    val createdAt: Long?
)

/**
 * 专注统计响应
 */
data class FocusStatsResponse(
    val code: Int,
    val message: String?,
    val data: FocusStats?
)

/**
 * 专注统计数据类
 */
data class FocusStats(
    val totalRecords: Int,
    val totalMinutes: Int,
    val todayRecords: Int,
    val todayMinutes: Int
)
