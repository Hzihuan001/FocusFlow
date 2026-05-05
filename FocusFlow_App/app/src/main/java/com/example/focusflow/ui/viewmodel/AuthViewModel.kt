package com.example.focusflow.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.focusflow.api.AuthService
import com.example.focusflow.api.RetrofitClient
import com.example.focusflow.data.session.SessionManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * AuthViewModel —— 用户鉴权状态管理引擎
 *
 * 【架构设计（论文答辩材料）】
 * 采用 MVI 架构模式：
 *   UI 产生 Intent（Login/Register）→ ViewModel 处理 → 发出 NavigationEvent
 *
 * 【技术亮点】
 *   ① navigationEvent 使用 SharedFlow（而非 StateFlow），保证一次性消费，
 *     不会因 Compose 重组重复触发页面跳转。
 *   ② isLoading 使用 StateFlow，UI 订阅后可自动驱动骨架屏或 Loading 动画。
 *   ③ 登录成功后写入 DataStore（会话凭证），
 *     用户信息通过 ProfileViewModel 从云端实时加载，不存 Room。
 *
 * 【数据流向】
 * ```
 * [用户输入] 
 *     ↓ 
 * [ViewModel 校验] 
 *     ↓ 
 * [Retrofit API 调用] 
 *     ↓ 
 * [SessionManager.saveSession()] → DataStore 持久化（userId, nickname）
 *     ↓
 * [_navigationEvent.emit()]      → UI 导航
 * ```
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    // ───────────── 依赖注入 ─────────────

    private val sessionManager = SessionManager.getInstance(application)
    private val authService: AuthService = RetrofitClient.authService

    // ───────────── 状态流 ─────────────

    /** 加载状态（驱动 Loading 动画和按钮禁用） */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** 错误提示（驱动 Snackbar 或内联错误显示） */
    private val _errorMsg = MutableStateFlow<String?>(null)
    val errorMsg: StateFlow<String?> = _errorMsg.asStateFlow()

    /** 一次性导航事件（SharedFlow：不缓存，只触发一次） */
    private val _navigationEvent = MutableSharedFlow<AuthNavEvent>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    // ───────────── 导航事件定义 ─────────────

    sealed class AuthNavEvent {
        object NavigateToMain : AuthNavEvent()
    }

    // ───────────── 公共 API ─────────────

    /**
     * 用户登录
     *
     * 【业务流程】
     * 1. 前置校验（账号/密码非空）
     * 2. 调用后端 POST /auth/login 接口
     * 3. 成功：写入 SessionManager，触发导航
     * 4. 失败：显示错误信息
     *
     * @param account 账号
     * @param password 密码
     */
    fun login(account: String, password: String) {
        // 1. 前置校验
        if (!validateInput(account, password)) return

        viewModelScope.launch {
            _isLoading.value = true
            _errorMsg.value = null

            try {
                // 2. 调用登录 API
                val response = authService.login(
                    AuthService.LoginRequest(account = account, password = password)
                )

                _isLoading.value = false

                // 3. 处理响应
                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    val authResponse = response.body()?.data
                    if (authResponse != null) {
                        // 写入 SessionManager（会话凭证）
                        // 🔧 [CLOUD-ONLY] 不写入本地数据库，用户数据从云端实时获取
                        sessionManager.saveSession(
                            userId = authResponse.userId,
                            token = "session_${System.currentTimeMillis()}",
                            nickname = authResponse.nickname
                        )

                        // 触发导航事件
                        _navigationEvent.emit(AuthNavEvent.NavigateToMain)
                    } else {
                        _errorMsg.value = "服务器响应异常"
                    }
                } else {
                    _errorMsg.value = response.body()?.message ?: "网络错误，请稍后重试"
                }
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMsg.value = "网络连接失败: ${e.message}"
            }
        }
    }

    /**
     * 用户注册
     *
     * 【业务流程】
     * 1. 前置校验（账号/密码/确认密码）
     * 2. 调用后端 POST /auth/register 接口
     * 3. 成功：自动登录，触发导航
     * 4. 失败：显示错误信息
     *
     * @param account 账号
     * @param password 密码
     * @param confirmPassword 确认密码
     */
    fun register(account: String, password: String, confirmPassword: String) {
        // 1. 前置校验
        if (!validateInput(account, password)) return

        // 2. 确认密码校验
        if (confirmPassword.isBlank()) {
            _errorMsg.value = "请确认密码"
            return
        }
        if (password != confirmPassword) {
            _errorMsg.value = "两次输入的密码不一致"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMsg.value = null

            try {
                // 3. 调用注册 API
                val response = authService.register(
                    AuthService.RegisterRequest(
                        account = account,
                        password = password,
                        nickname = null // 后端自动生成
                    )
                )

                _isLoading.value = false

                // 4. 处理响应
                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    val authResponse = response.body()?.data
                    if (authResponse != null) {
                        // 注册成功，自动登录
                        // 🔧 [CLOUD-ONLY] 不写入本地数据库，用户数据从云端实时获取
                        sessionManager.saveSession(
                            userId = authResponse.userId,
                            token = "session_${System.currentTimeMillis()}",
                            nickname = authResponse.nickname
                        )

                        _navigationEvent.emit(AuthNavEvent.NavigateToMain)
                    } else {
                        _errorMsg.value = "服务器响应异常"
                    }
                } else {
                    _errorMsg.value = response.body()?.message ?: "网络错误，请稍后重试"
                }
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMsg.value = "网络连接失败: ${e.message}"
            }
        }
    }

    // ───────────── 辅助方法 ─────────────

    /**
     * 输入校验
     */
    private fun validateInput(account: String, password: String): Boolean {
        if (account.isBlank()) {
            _errorMsg.value = "请输入账号"
            return false
        }
        if (account.length < 4) {
            _errorMsg.value = "账号长度至少 4 位"
            return false
        }
        if (password.isBlank()) {
            _errorMsg.value = "请输入密码"
            return false
        }
        if (password.length < 6) {
            _errorMsg.value = "密码长度至少 6 位"
            return false
        }
        return true
    }

    /**
     * 清除错误信息
     */
    fun clearError() {
        _errorMsg.value = null
    }
}