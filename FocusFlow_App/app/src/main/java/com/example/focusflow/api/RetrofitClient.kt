package com.example.focusflow.api

import com.example.focusflow.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    // ═══════════════════════════════════════════════════════════════════════════
    // 【安全加固】API Key 已移至服务端
    // 客户端不再存储敏感密钥，所有 AI 请求通过后端代理转发
    // ═══════════════════════════════════════════════════════════════════════════
    
    // ═══════════════════════════════════════════════════════════════════════════
    // 后端服务地址配置（通过 BuildConfig 管理）
    // ═══════════════════════════════════════════════════════════════════════════
    // Debug 环境：使用局域网 IP 或 10.0.2.2（模拟器访问宿主机）
    // Release 环境：使用正式服务器地址
    //
    // 【修改地址】
    // 修改 app/build.gradle.kts 中的 buildConfigField("String", "API_BASE_URL", ...)
    // ═══════════════════════════════════════════════════════════════════════════
    val GATEWAY_BASE_URL: String = BuildConfig.API_BASE_URL

    /**
     * 快速健康检查服务（2秒超时）
     * 专注结算前快速验证后端可达性
     */
    val quickHealthService: AuthService by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .build()
            
        Retrofit.Builder()
            .baseUrl(GATEWAY_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthService::class.java)
    }

    /**
     * AI 助手服务（通过后端代理，安全）
     * 
     * 【安全设计】
     * API Key 存储在服务端，客户端通过此代理接口调用 AI
     */
    val zhipuService: ZhipuService by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(GATEWAY_BASE_URL)  // 使用后端代理地址
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ZhipuService::class.java)
    }

    val gatewayService: GardenService by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
            
        Retrofit.Builder()
            .baseUrl(GATEWAY_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GardenService::class.java)
    }

    /**
     * 用户鉴权服务
     */
    val authService: AuthService by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
            
        Retrofit.Builder()
            .baseUrl(GATEWAY_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthService::class.java)
    }

    /**
     * 社交系统服务
     */
    val socialService: SocialService by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
            
        Retrofit.Builder()
            .baseUrl(GATEWAY_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SocialService::class.java)
    }

    /**
     * 背包系统服务
     */
    val bagService: BagService by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
            
        Retrofit.Builder()
            .baseUrl(GATEWAY_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BagService::class.java)
    }

    /**
     * 系统配置服务
     * 
     * 【API 列表】
     * GET  /config         - 获取所有配置
     * GET  /config/{key}   - 获取单个配置
     */
    val configService: ConfigService by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
            
        Retrofit.Builder()
            .baseUrl(GATEWAY_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ConfigService::class.java)
    }
    
    /**
     * 专注记录云端服务
     * 
     * 【API 列表】
     * GET  /focus/records/{userId}  - 获取用户专注记录
     * GET  /focus/stats/{userId}    - 获取用户专注统计
     */
    val focusService: FocusService by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
            
        Retrofit.Builder()
            .baseUrl(GATEWAY_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FocusService::class.java)
    }
    
    /**
     * 同步服务
     * 
     * 【API 列表】
     * POST /sync/focus-records/batch - 批量同步专注记录
     */
    val syncService: SyncService by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
            
        Retrofit.Builder()
            .baseUrl(GATEWAY_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SyncService::class.java)
    }
}