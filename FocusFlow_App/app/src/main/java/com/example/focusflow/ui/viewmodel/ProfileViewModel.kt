package com.example.focusflow.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.focusflow.api.AuthService
import com.example.focusflow.api.RetrofitClient
import com.example.focusflow.api.UserPreferences
import com.example.focusflow.data.session.SessionManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ProfileViewModel —— 用户资料状态管理
 *
 * 【架构说明】
 * 根据设计，本地 Room 不存储用户信息。
 * 用户数据（昵称、头像、光流）完全从云端读取，修改时直接更新云端。
 *
 * 【数据流】
 * 进入页面 → 从云端 GET /user/{userId} → 更新 StateFlow → UI 渲染
 * 修改资料 → PUT /user/profile → 云端更新成功 → 刷新 StateFlow
 */
class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "ProfileViewModel"
    }

    private val sessionManager = SessionManager.getInstance(application)
    private val authService: AuthService = RetrofitClient.authService

    // ═══════════════════════════════════════════════════════════════
    // 用户数据（纯云端，不存 Room）
    // ═══════════════════════════════════════════════════════════════

    /** 用户 ID */
    private val _userId = MutableStateFlow<Long?>(null)
    val userId: StateFlow<Long?> = _userId.asStateFlow()

    /** 用户账号 */
    private val _account = MutableStateFlow("")
    val account: StateFlow<String> = _account.asStateFlow()

    /** 用户昵称 */
    private val _nickname = MutableStateFlow("专注者")
    val nickname: StateFlow<String> = _nickname.asStateFlow()

    /** 用户头像ID */
    private val _avatarId = MutableStateFlow(1)
    val avatarId: StateFlow<Int> = _avatarId.asStateFlow()

    /** 用户光流 */
    private val _timeFlux = MutableStateFlow(0)
    val timeFlux: StateFlow<Int> = _timeFlux.asStateFlow()

    /** 连续专注天数 */
    private val _streakDays = MutableStateFlow(0)
    val streakDays: StateFlow<Int> = _streakDays.asStateFlow()

    /** 🟢 [OFFLINE] 云端连接状态 */
    private val _isConnected = MutableStateFlow<Boolean?>(null) // null=检查中, true=已连接, false=断开
    val isConnected: StateFlow<Boolean?> = _isConnected.asStateFlow()

    /** 加载状态 */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // ═══════════════════════════════════════════════════════════════
    // 🟢 [PULL TO REFRESH] 下拉刷新状态
    // ═══════════════════════════════════════════════════════════════
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    /** 操作消息 */
    private val _operationMessage = MutableStateFlow<String?>(null)
    val operationMessage: StateFlow<String?> = _operationMessage.asStateFlow()

    /** 临时选中的头像 ID (仅在 BottomSheet 交互过程中有效) */
    private val _temporarySelectedAvatarId = MutableStateFlow<Int?>(null)
    val temporarySelectedAvatarId: StateFlow<Int?> = _temporarySelectedAvatarId.asStateFlow()

    init {
        // 初始化时从云端加载用户数据
        refreshFromCloud()
    }

    /**
     * ═══════════════════════════════════════════════════════════════
     * 从云端加载用户数据
     * 🟢 [OFFLINE CACHE] 先从本地缓存读取显示，再从云端刷新
     * 🟢 [OFFLINE] 检测云端连接状态，离线时设置 isConnected = false
     * 🟢 [PULL TO REFRESH] 支持 isRefreshing 状态管理
     * ═══════════════════════════════════════════════════════════════
     */
    fun refreshFromCloud() {
        viewModelScope.launch {
            _isRefreshing.value = true
            
            val sessionUserId = sessionManager.userIdFlow.first()
            
            if (sessionUserId == null) {
                Log.w(TAG, "用户未登录，跳过云端刷新")
                _isLoading.value = false
                _isRefreshing.value = false
                return@launch
            }
            
            // 先设置 userId，确保 UI 不显示"请先登录"
            _userId.value = sessionUserId
            
            // 🟢 [OFFLINE CACHE] 先从本地缓存读取，立即显示
            loadFromLocalCache()
            
            // 🟢 [OFFLINE] 先检查网络连接状态
            val networkAvailable = isNetworkAvailable()
            _isConnected.value = networkAvailable
            
            if (!networkAvailable) {
                Log.w(TAG, "无法连接到云端，使用本地缓存数据")
                _isRefreshing.value = false
                return@launch
            }
            
            _isLoading.value = true
            try {
                Log.d(TAG, "从云端加载用户数据: userId=$sessionUserId")
                val response = authService.getUserInfo(sessionUserId)
                
                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    val cloudUser = response.body()?.data
                    if (cloudUser != null) {
                        _userId.value = cloudUser.userId
                        _account.value = cloudUser.account
                        _nickname.value = cloudUser.nickname
                        _avatarId.value = cloudUser.avatarId
                        _timeFlux.value = cloudUser.timeFlux
                        _streakDays.value = cloudUser.streakDays
                        
                        // 🟢 [OFFLINE CACHE] 保存到本地缓存
                        UserPreferences.setNickname(getApplication(), cloudUser.nickname)
                        UserPreferences.setAccount(getApplication(), cloudUser.account)
                        
                        Log.d(TAG, "云端数据加载成功: nickname=${cloudUser.nickname}, streakDays=${cloudUser.streakDays}")
                    }
                } else {
                    Log.w(TAG, "从云端获取用户数据失败: ${response.body()?.message}")
                    // 离线时已从本地缓存读取，无需额外处理
                }
            } catch (e: Exception) {
                Log.e(TAG, "从云端加载用户数据失败", e)
                _isConnected.value = false
                // 离线时已从本地缓存读取，无需额外处理
            } finally {
                _isLoading.value = false
                _isRefreshing.value = false
            }
        }
    }

    /**
     * 🟢 [OFFLINE CACHE] 从本地缓存加载用户数据
     */
    private fun loadFromLocalCache() {
        val context: android.content.Context = getApplication()
        val cachedNickname = UserPreferences.getNickname(context)
        val cachedAccount = UserPreferences.getAccount(context)
        
        if (cachedNickname.isNotEmpty()) {
            _nickname.value = cachedNickname
            Log.d(TAG, "从本地缓存加载昵称: $cachedNickname")
        }
        if (cachedAccount.isNotEmpty()) {
            _account.value = cachedAccount
            Log.d(TAG, "从本地缓存加载账号: $cachedAccount")
        }
    }

    /**
     * 退出登录
     */
    fun logout() {
        viewModelScope.launch {
            sessionManager.clearSession()
            _userId.value = null
            _account.value = ""
            _nickname.value = "专注者"
            _avatarId.value = 1
            _timeFlux.value = 0
            _streakDays.value = 0
            Log.d(TAG, "用户已退出登录")
        }
    }

    /**
     * 更新临时头像选中值
     */
    fun updateTemporaryAvatar(avatarId: Int) {
        _temporarySelectedAvatarId.value = avatarId
    }

    /**
     * 🟢 [OFFLINE CHECK] 检查网络是否可用
     */
    private suspend fun isNetworkAvailable(): Boolean {
        return try {
            val response = RetrofitClient.quickHealthService.healthCheck()
            response.isSuccessful && response.body()?.isSuccess == true
        } catch (e: Exception) {
            Log.e(TAG, "网络检查失败: ${e.message}")
            false
        }
    }

    /**
     * ═══════════════════════════════════════════════════════════════
     * 确认并保存头像更改（直接更新云端）
     * 🟢 [OFFLINE CHECK] 离线时提示网络不可用
     * ═══════════════════════════════════════════════════════════════
     */
    fun saveAvatar() {
        val userId = _userId.value ?: return
        val avatarId = _temporarySelectedAvatarId.value ?: return
        
        viewModelScope.launch {
            // 🟢 [OFFLINE CHECK] 先检查网络
            if (!isNetworkAvailable()) {
                _operationMessage.value = "网络不可用"
                Log.w(TAG, "保存头像失败: 网络不可用")
                return@launch
            }
            
            _isLoading.value = true
            try {
                val request = AuthService.UpdateProfileRequest(avatarId = avatarId)
                val response = authService.updateProfile(userId, request)
                
                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    _avatarId.value = avatarId
                    Log.d(TAG, "头像云端更新成功: avatarId=$avatarId")
                    _operationMessage.value = "头像更新成功"
                } else {
                    Log.w(TAG, "头像云端更新失败: ${response.body()?.message}")
                    _operationMessage.value = "更新失败: ${response.body()?.message}"
                }
            } catch (e: Exception) {
                Log.e(TAG, "头像更新失败", e)
                _operationMessage.value = "网络错误: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * ═══════════════════════════════════════════════════════════════
     * 更新昵称（直接更新云端）
     * 🟢 [OFFLINE CHECK] 离线时提示网络不可用
     * ═══════════════════════════════════════════════════════════════
     */
    fun updateNickname(newNickname: String) {
        val userId = _userId.value ?: return
        
        viewModelScope.launch {
            // 🟢 [OFFLINE CHECK] 先检查网络
            if (!isNetworkAvailable()) {
                _operationMessage.value = "网络不可用"
                Log.w(TAG, "更新昵称失败: 网络不可用")
                return@launch
            }
            
            _isLoading.value = true
            try {
                val request = AuthService.UpdateProfileRequest(nickname = newNickname)
                val response = authService.updateProfile(userId, request)
                
                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    _nickname.value = newNickname
                    // 🟢 [OFFLINE CACHE] 更新本地缓存
                    UserPreferences.setNickname(getApplication(), newNickname)
                    Log.d(TAG, "昵称云端更新成功: nickname=$newNickname")
                    _operationMessage.value = "昵称更新成功"
                } else {
                    Log.w(TAG, "昵称云端更新失败: ${response.body()?.message}")
                    _operationMessage.value = "更新失败: ${response.body()?.message}"
                }
            } catch (e: Exception) {
                Log.e(TAG, "昵称更新失败", e)
                _operationMessage.value = "网络错误: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 清除操作消息
     */
    fun clearOperationMessage() {
        _operationMessage.value = null
    }

    /**
     * ═══════════════════════════════════════════════════════════════
     * 修改密码
     * ═══════════════════════════════════════════════════════════════
     */
    fun changePassword(oldPassword: String, newPassword: String, onSuccess: () -> Unit = {}) {
        val userId = _userId.value ?: return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val request = AuthService.ChangePasswordRequest(oldPassword, newPassword)
                val response = authService.changePassword(userId, request)

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    Log.d(TAG, "密码修改成功")
                    _operationMessage.value = "密码修改成功"
                    onSuccess()
                } else {
                    Log.w(TAG, "密码修改失败: ${response.body()?.message}")
                    _operationMessage.value = response.body()?.message ?: "密码修改失败"
                }
            } catch (e: Exception) {
                Log.e(TAG, "密码修改失败", e)
                _operationMessage.value = "网络错误: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
