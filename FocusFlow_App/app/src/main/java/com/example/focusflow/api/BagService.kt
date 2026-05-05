package com.example.focusflow.api

import retrofit2.Response
import retrofit2.http.*

/**
 * BagService —— 背包 API 服务接口
 *
 * 【API 列表】
 * GET  /bag/list           - 获取背包列表
 * POST /bag/add            - 添加背包物品
 * POST /bag/open/{bagId}   - 开箱（解析种子）
 * PUT  /bag/status/{bagId} - 更新物品状态
 */
interface BagService {

    /**
     * 获取背包列表
     */
    @GET("bag/list")
    suspend fun getBagList(
        @Header("X-User-Id") userId: Long
    ): Response<BagListResponse>

    /**
     * 添加背包物品
     */
    @POST("bag/add")
    suspend fun addBagItem(
        @Header("X-User-Id") userId: Long,
        @Body request: AddBagItemRequest
    ): Response<AddBagItemResponse>

    /**
     * 开箱（解析种子）
     */
    @POST("bag/open/{bagId}")
    suspend fun openBox(
        @Header("X-User-Id") userId: Long,
        @Path("bagId") bagId: String
    ): Response<OpenBoxResponse>

    /**
     * 更新物品状态
     */
    @PUT("bag/status/{bagId}")
    suspend fun updateStatus(
        @Header("X-User-Id") userId: Long,
        @Path("bagId") bagId: String,
        @Body request: UpdateStatusRequest
    ): Response<BooleanResponse>

    // ═══════════════════════════════════════════════════════════════════════════
    // DTO 数据类
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * 背包物品 DTO
     */
    data class BagItemDto(
        val bagId: String,
        val userId: Long,
        val plantId: Int,
        val plantName: String?,
        val rarity: Int?,
        val imageUrl: String?,
        val status: Int,
        val obtainedAt: Long
    )

    /**
     * 背包列表响应
     */
    data class BagListResponse(
        val code: Int,
        val message: String?,
        val data: List<BagItemDto>
    )

    /**
     * 添加背包物品请求
     */
    data class AddBagItemRequest(
        val plantId: Int,
        val status: Int = 0
    )

    /**
     * 添加背包物品响应
     */
    data class AddBagItemResponse(
        val code: Int,
        val message: String?,
        val data: BagIdData?
    ) {
        val isSuccess: Boolean get() = code == 200
    }

    data class BagIdData(
        val bagId: String
    )

    /**
     * 开箱响应
     */
    data class OpenBoxResponse(
        val code: Int,
        val message: String?,
        val data: OpenBoxData?
    ) {
        val isSuccess: Boolean get() = code == 200
    }

    data class OpenBoxData(
        val bagId: String,
        val plantId: Int,
        val plantName: String?,
        val imageUrl: String?,
        val rarity: Int?,
        val timeFlux: Int?  // 用户剩余光流
    )

    /**
     * 更新状态请求
     */
    data class UpdateStatusRequest(
        val status: Int
    )

    /**
     * 布尔响应
     */
    data class BooleanResponse(
        val code: Int,
        val message: String?,
        val data: Boolean?
    ) {
        val isSuccess: Boolean get() = code == 200
    }
}
