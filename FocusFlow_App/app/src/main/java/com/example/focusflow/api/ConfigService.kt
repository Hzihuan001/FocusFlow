package com.example.focusflow.api

import retrofit2.http.GET
import retrofit2.http.Path

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * 系统配置 API
 * ═══════════════════════════════════════════════════════════════════════════════
 */

data class ConfigResponse(
    val code: Int,
    val message: String?,
    val data: Map<String, String>
)

data class SingleConfigResponse(
    val code: Int,
    val message: String?,
    val data: String?
)

interface ConfigService {

    /**
     * 获取所有配置（Map格式）
     */
    @GET("config")
    suspend fun getAllConfigs(): ConfigResponse

    /**
     * 获取单个配置值
     */
    @GET("config/{key}")
    suspend fun getConfigValue(@Path("key") key: String): SingleConfigResponse
}
