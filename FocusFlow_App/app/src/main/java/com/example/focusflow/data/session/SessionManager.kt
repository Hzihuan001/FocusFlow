package com.example.focusflow.data.session

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

/**
 * SessionManager —— 设备指纹静默登录会话管理
 *
 * 【重构说明】
 * 移除 EncryptedSharedPreferences，改用普通 DataStore 存储。
 * 若需要加密存储，可在后续版本集成 Jetpack Security Crypto 库。
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_session")

class SessionManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "SessionManager"
        
        // DataStore Keys
        private val KEY_USER_ID  = longPreferencesKey("user_id")
        private val KEY_NICKNAME = stringPreferencesKey("nickname")
        private val KEY_DEVICE_UUID = stringPreferencesKey("device_uuid")
        private val KEY_AUTH_TOKEN = stringPreferencesKey("auth_token")
        private val KEY_API_KEY = stringPreferencesKey("api_key")
        private val KEY_PITY_COUNTER = doublePreferencesKey("pity_counter")

        // 哈希盐值
        private const val DEVICE_UUID_SALT = "FocusFlow_2025_Cyber_Salt_v1"

        @Volatile private var INSTANCE: SessionManager? = null

        fun getInstance(context: Context): SessionManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: SessionManager(context.applicationContext).also { INSTANCE = it }
            }
    }

    // ───────────── 设备指纹生成 ─────────────

    @SuppressLint("HardwareIds")
    suspend fun getOrCreateDeviceUuid(): String {
        val cached: String? = context.dataStore.data.map { it[KEY_DEVICE_UUID] }.firstOrNull()
        if (!cached.isNullOrBlank()) return cached

        val androidId = try {
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            ) ?: "unknown_device"
        } catch (e: Exception) {
            Log.w(TAG, "获取 ANDROID_ID 失败", e)
            "unknown_device"
        }

        val raw = "$androidId:$DEVICE_UUID_SALT"
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
        val uuid = digest.joinToString("") { "%02x".format(it) }.take(64)

        context.dataStore.edit { it[KEY_DEVICE_UUID] = uuid }
        return uuid
    }

    // ───────────── 会话管理 ─────────────

    suspend fun saveSession(userId: Long, token: String, nickname: String) {
        Log.d(TAG, "保存会话: userId=$userId, nickname=$nickname")
        context.dataStore.edit { prefs ->
            prefs[KEY_USER_ID] = userId
            prefs[KEY_NICKNAME] = nickname
            prefs[KEY_AUTH_TOKEN] = token
        }
        // 同时保存到 SharedPreferences 供 Service 同步读取
        context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
            .edit()
            .putLong("user_id", userId)
            .apply()
    }

    /**
     * 同步获取用户ID（供 Service 使用）
     */
    fun getUserIdSync(): Long {
        return context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
            .getLong("user_id", 0L)
    }

    suspend fun saveApiKey(apiKey: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_API_KEY] = apiKey
        }
    }

    suspend fun getApiKey(): String? {
        return context.dataStore.data.map { it[KEY_API_KEY] }.firstOrNull()
    }

    suspend fun getToken(): String? {
        return context.dataStore.data.map { it[KEY_AUTH_TOKEN] }.firstOrNull()
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }

    // ───────────── Flow 订阅 ─────────────

    val userIdFlow: Flow<Long?> = context.dataStore.data.map { it[KEY_USER_ID] }
    val nicknameFlow: Flow<String?> = context.dataStore.data.map { it[KEY_NICKNAME] }
    val tokenFlow: Flow<String?> = context.dataStore.data.map { it[KEY_AUTH_TOKEN] }

    // ───────────── 一次性读取 ─────────────

    suspend fun requireUserId(): Long {
        return context.dataStore.data.map { it[KEY_USER_ID] }.first()
            ?: throw IllegalStateException("用户未登录")
    }

    suspend fun isLoggedIn(): Boolean {
        val userId = context.dataStore.data.map { it[KEY_USER_ID] }.firstOrNull()
        return userId != null
    }

    // ───────────── Pity 保底管理 ─────────────

    val pityFlow: Flow<Double> = context.dataStore.data.map { it[KEY_PITY_COUNTER] ?: 0.0 }

    suspend fun updatePity(delta: Double) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_PITY_COUNTER] ?: 0.0
            prefs[KEY_PITY_COUNTER] = current + delta
        }
    }

    suspend fun resetPity() {
        context.dataStore.edit { prefs ->
            prefs[KEY_PITY_COUNTER] = 0.0
        }
    }
}