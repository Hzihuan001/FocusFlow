package com.example.focusflow.api

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Streaming

/**
 * AI 助手服务接口（后端代理版）
 * 
 * 【安全设计】
 * 客户端不再直接调用智谱 API，而是通过后端代理转发
 * API Key 存储在服务端，客户端无需知道
 */
interface ZhipuService {
    /**
     * 流式对话（SSE）
     * 
     * @param messages JSON 格式的消息列表，如 [{"role":"user","content":"你好"}]
     * @param model 模型名称（可选，默认 glm-4-flash）
     * @param userId 用户ID（通过 Header 传递）
     */
    @Headers("Content-Type: application/json")
    @POST("ai/chat/stream")
    @Streaming
    fun streamChat(
        @Body messages: String,
        @Query("model") model: String? = null,
        @Header("X-User-Id") userId: Long? = null
    ): Call<ResponseBody>

    /**
     * 非流式对话
     */
    @Headers("Content-Type: application/json")
    @POST("ai/chat")
    fun chat(
        @Body messages: String,
        @Query("model") model: String? = null
    ): Call<ResponseBody>
}
