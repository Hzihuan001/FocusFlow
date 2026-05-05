package com.example.focusflow.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * AuthService —— 用户鉴权 API 服务接口
 *
 * 【API 列表】
 * POST /auth/register - 用户注册
 * POST /auth/login    - 用户登录
 * GET  /user/{userId} - 查询用户信息
 * PUT  /user/profile  - 更新用户资料
 *
 * 【安全设计】
 * - 密码在传输层使用 HTTPS 加密（生产环境必须启用）
 * - 服务端使用 SHA-256 加盐哈希存储
 */
interface AuthService {

    /**
     * 健康检查接口
     * 用于检测后端服务是否可用
     */
    @GET("health")
    suspend fun healthCheck(): Response<HealthCheckResponse>

    /**
     * 用户注册
     *
     * @param request 注册请求（account, password, nickname）
     * @return 认证响应（userId, account, nickname, timeFlux）
     */
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<ApiResponse<AuthResponse>>

    /**
     * 用户登录
     *
     * @param request 登录请求（account, password）
     * @return 认证响应（userId, account, nickname, timeFlux）
     */
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<AuthResponse>>

    /**
     * 查询用户信息
     *
     * @param userId 用户ID
     * @return 用户信息响应
     */
    @GET("user/{userId}")
    suspend fun getUserInfo(@Path("userId") userId: Long): Response<ApiResponse<AuthResponse>>

    /**
     * 更新用户资料（昵称、头像）
     *
     * @param userId  用户ID（Header传递）
     * @param request 更新请求
     * @return 更新后的用户信息
     */
    @PUT("user/profile")
    suspend fun updateProfile(
        @Header("X-User-Id") userId: Long,
        @Body request: UpdateProfileRequest
    ): Response<ApiResponse<AuthResponse>>

    /**
     * 修改密码
     *
     * @param userId  用户ID（Header传递）
     * @param request 修改密码请求
     * @return 是否成功
     */
    @PUT("user/password")
    suspend fun changePassword(
        @Header("X-User-Id") userId: Long,
        @Body request: ChangePasswordRequest
    ): Response<ApiResponse<Boolean>>

    // ═══════════════════════════════════════════════════════════════════════════
    // DTO 数据类
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * 注册请求 DTO
     */
    data class RegisterRequest(
        val account: String,
        val password: String,
        val nickname: String? = null
    )

    /**
     * 登录请求 DTO
     */
    data class LoginRequest(
        val account: String,
        val password: String
    )

    /**
     * 更新用户资料请求 DTO
     */
    data class UpdateProfileRequest(
        val nickname: String? = null,
        val avatarId: Int? = null
    )

    /**
     * 修改密码请求 DTO
     */
    data class ChangePasswordRequest(
        val oldPassword: String,
        val newPassword: String
    )

    /**
     * 认证响应 DTO
     */
    data class AuthResponse(
        val userId: Long,
        val account: String,
        val nickname: String,
        val avatarId: Int = 1,
        val timeFlux: Int = 0,
        val streakDays: Int = 0
    )

    /**
     * 通用 API 响应包装类
     *
     * 【对应后端 Result<T> 结构】
     * {
     *   "code": 200,
     *   "message": "操作成功",
     *   "data": { ... }
     * }
     */
    data class ApiResponse<T>(
        val code: Int,
        val message: String?,
        val data: T?
    ) {
        val isSuccess: Boolean get() = code == 200
    }
    
    /**
     * 健康检查响应 DTO
     */
    data class HealthCheckResponse(
        val code: Int,
        val message: String?,
        val data: HealthData?
    ) {
        val isSuccess: Boolean get() = code == 200
    }
    
    data class HealthData(
        val status: String?,
        val service: String?,
        val timestamp: String?
    )
}
