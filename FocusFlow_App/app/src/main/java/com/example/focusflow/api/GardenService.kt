package com.example.focusflow.api

import retrofit2.Response
import retrofit2.http.*

// ═══════════════════════════════════════════════════════════════════════════════
// 植物图鉴相关 DTO
// ═══════════════════════════════════════════════════════════════════════════════

data class PlantDto(
    val id: Int,
    val name: String,
    val description: String,
    val color: String,
    val purifyRange: Int,
    val imageUrl: String? = null,
    val dropWeight: Int,
    val resourceCode: String,
    val width: Int,
    val rarity: Int,
    val scale: Int = 100  // 显示缩放比例（百分比）：100=原大小, 50=缩小一半, 200=放大两倍
)

data class GardenResponse(
    val code: Int,
    val message: String?,
    val data: List<PlantDto>
)

// ═══════════════════════════════════════════════════════════════════════════════
// 花园地块相关 DTO
// ═══════════════════════════════════════════════════════════════════════════════

data class GardenTileDto(
    val tileId: Long,
    val x: Int,
    val y: Int,
    val plantId: Int?,
    val plantName: String?,
    val plantColor: String?,
    val imageUrl: String? = null,  // 植物图片URL
    val bagRecordId: String?,
    val deployTime: Long?,
    val lastChargeTime: Long?,
    val isPurified: Boolean,
    // 🔧 [WIDTH×WIDTH] 植物占用区域（width×width 正方形）
    val width: Int? = null
)

data class GardenTileListResponse(
    val code: Int,
    val message: String?,
    val data: GardenTilesData?
)

data class GardenTilesData(
    val tiles: List<GardenTileDto>,
    val lastFocusTime: Long
)

data class GardenTileResponse(
    val code: Int,
    val message: String?,
    val data: GardenTileDto?
)

data class PlantRequest(
    val x: Int,
    val y: Int,
    val plantId: Int,
    val bagRecordId: String? = null
)

data class BooleanResponse(
    val code: Int,
    val message: String?,
    val data: Boolean?
)

data class PurifyResponse(
    val code: Int,
    val message: String?,
    val data: Int?
)

// ═══════════════════════════════════════════════════════════════════════════════
// 排行榜相关 DTO
// ═══════════════════════════════════════════════════════════════════════════════

data class LeaderboardEntryDto(
    val userId: Long,
    val nickname: String?,
    val account: String?,
    val avatarUrl: String?,
    val purifiedCount: Int,
    val rank: Int
)

data class LeaderboardResponse(
    val code: Int,
    val message: String?,
    val data: List<LeaderboardEntryDto>
)

// ═══════════════════════════════════════════════════════════════════════════════
// GardenService 接口
// ═══════════════════════════════════════════════════════════════════════════════

interface GardenService {

    /**
     * 获取植物图鉴列表
     */
    @GET("plants")
    suspend fun getPlants(): GardenResponse

    /**
     * 获取用户花园地块
     */
    @GET("garden/tiles")
    suspend fun getGardenTiles(
        @Header("X-User-Id") userId: Long
    ): GardenTileListResponse

    /**
     * 种植植物
     */
    @POST("garden/plant")
    suspend fun plant(
        @Header("X-User-Id") userId: Long,
        @Body request: PlantRequest
    ): GardenTileResponse

    /**
     * 充能
     */
    @POST("garden/charge/{tileId}")
    suspend fun charge(
        @Header("X-User-Id") userId: Long,
        @Path("tileId") tileId: Long
    ): BooleanResponse

    /**
     * 收获
     */
    @POST("garden/harvest/{tileId}")
    suspend fun harvest(
        @Header("X-User-Id") userId: Long,
        @Path("tileId") tileId: Long
    ): BooleanResponse

    /**
     * 专注完成后净化花园
     */
    @POST("garden/purify")
    suspend fun purifyOnFocus(
        @Header("X-User-Id") userId: Long,
        @Query("durationMinutes") durationMinutes: Int
    ): PurifyResponse

    /**
     * 获取好友点亮排行榜
     * 仅显示当前用户及其好友的排名
     * 
     * @param userId 当前用户ID
     * @param limit 返回条数
     */
    @GET("garden/leaderboard")
    suspend fun getLeaderboard(
        @Header("X-User-Id") userId: Long,
        @Query("limit") limit: Int = 20
    ): LeaderboardResponse
}
