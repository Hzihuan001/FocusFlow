package com.example.focusflow.api

import retrofit2.Response
import retrofit2.http.*

/**
 * SocialService —— 社交系统 API 服务接口
 *
 * 【API 列表】
 * GET  /user/search          - 根据账号搜索用户
 * POST /friend/add           - 发送好友申请
 * POST /friend/accept/{id}   - 同意好友申请
 * POST /friend/reject/{id}   - 拒绝好友申请
 * GET  /friend/list          - 获取好友列表
 * GET  /friend/requests      - 获取待处理的好友申请
 * POST /visit/charge/{hostId} - 为好友花园充能
 * POST /visit/message         - 在好友花园留言
 * GET  /visit/logs/{hostId}   - 获取花园访客日志
 * GET  /visit/unread          - 获取未读通知数量
 * POST /visit/read-all        - 标记所有通知为已读
 */
interface SocialService {

    // ═══════════════════════════════════════════════════════════════════════════
    // 用户搜索 API
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * 根据账号搜索用户（用于添加好友）
     */
    @GET("user/search")
    suspend fun searchUser(
        @Query("account") account: String
    ): Response<ApiResponse<FriendResponse>>

    // ═══════════════════════════════════════════════════════════════════════════
    // 好友系统 API
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * 发送好友申请
     */
    @POST("friend/add")
    suspend fun addFriend(
        @Header("X-User-Id") userId: Long,
        @Body request: AddFriendRequest
    ): Response<ApiResponse<Unit>>

    /**
     * 同意好友申请
     */
    @POST("friend/accept/{friendshipId}")
    suspend fun acceptFriend(
        @Header("X-User-Id") userId: Long,
        @Path("friendshipId") friendshipId: Long
    ): Response<ApiResponse<Unit>>

    /**
     * 拒绝好友申请
     */
    @POST("friend/reject/{friendshipId}")
    suspend fun rejectFriend(
        @Header("X-User-Id") userId: Long,
        @Path("friendshipId") friendshipId: Long
    ): Response<ApiResponse<Unit>>

    /**
     * 获取好友列表
     */
    @GET("friend/list")
    suspend fun getFriendList(
        @Header("X-User-Id") userId: Long
    ): Response<ApiResponse<List<FriendResponse>>>

    /**
     * 获取待处理的好友申请
     */
    @GET("friend/requests")
    suspend fun getPendingRequests(
        @Header("X-User-Id") userId: Long
    ): Response<ApiResponse<List<FriendResponse>>>

    // ═══════════════════════════════════════════════════════════════════════════
    // 互访系统 API
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * 为好友花园充能
     */
    @POST("visit/charge/{hostId}")
    suspend fun chargeForFriend(
        @Header("X-User-Id") userId: Long,
        @Path("hostId") hostId: Long
    ): Response<ApiResponse<Unit>>

    /**
     * 在好友花园留言
     */
    @POST("visit/message")
    suspend fun leaveMessage(
        @Header("X-User-Id") userId: Long,
        @Body request: LeaveMessageRequest
    ): Response<ApiResponse<Unit>>

    /**
     * 获取花园访客日志
     */
    @GET("visit/logs/{hostId}")
    suspend fun getVisitLogs(
        @Path("hostId") hostId: Long,
        @Query("limit") limit: Int = 20
    ): Response<ApiResponse<List<VisitLogResponse>>>

    /**
     * 获取未读通知数量
     */
    @GET("visit/unread")
    suspend fun getUnreadCount(
        @Header("X-User-Id") userId: Long
    ): Response<ApiResponse<Int>>

    /**
     * 标记所有通知为已读
     */
    @POST("visit/read-all")
    suspend fun markAllAsRead(
        @Header("X-User-Id") userId: Long
    ): Response<ApiResponse<Unit>>

    /**
     * 检查今日是否已为某好友点亮花园
     */
    @GET("visit/has-charged-today/{hostId}")
    suspend fun hasChargedToday(
        @Header("X-User-Id") userId: Long,
        @Path("hostId") hostId: Long
    ): Response<ApiResponse<Boolean>>

    // ═══════════════════════════════════════════════════════════════════════════
    // DTO 数据类
    // ═══════════════════════════════════════════════════════════════════════════

    data class AddFriendRequest(
        val friendId: Long
    )

    data class LeaveMessageRequest(
        val hostId: Long,
        val content: String
    )

    data class FriendResponse(
        val friendshipId: Long?,
        val userId: Long,
        val account: String,
        val nickname: String,
        val avatarId: Int,
        val timeFlux: Int,
        val status: Int,
        val createdAt: Long
    )

    data class VisitLogResponse(
        val logId: Long,
        val visitorId: Long,
        val visitorNickname: String,
        val visitorAvatarId: Int,
        val actionType: Int,
        val content: String?,
        val isRead: Int,
        val createdAt: Long
    )

    data class ApiResponse<T>(
        val code: Int,
        val message: String?,
        val data: T?
    ) {
        val isSuccess: Boolean get() = code == 200
    }
}