package com.example.focusflow.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.focusflow.api.RetrofitClient
import com.example.focusflow.api.SocialService
import com.example.focusflow.data.session.SessionManager
import com.example.focusflow.utils.SoundManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

private const val TAG = "SocialViewModel"

/**
 * 社交好友数据模型
 */
data class Friend(
    val userId: Long,
    val nickname: String,
    val account: String = "",
    val avatarEmoji: String = "👤"
)

/**
 * 花园访客日志 (对应云端 biz_visit_log 表)
 *
 * actionType: 1 = 量子充能 (⚡ 释放脉冲)，2 = 信标留言 (💬 注入信标)
 * isRead: 标识用户是否已阅读此条通知
 */
data class VisitLog(
    val logId: Long,
    val visitorId: Long,
    val visitorNickname: String,
    val visitorAvatar: String,
    val actionType: Int,       // 1=充能 / 2=留言
    val content: String,
    val createTime: String,
    val isRead: Boolean = false
)

/**
 * 频段申请请求 (对应云端 biz_friendship 表的待审核状态)
 */
data class FriendRequest(
    val requestId: Long,
    val fromUserId: Long,
    val fromNickname: String,
    val fromAvatar: String,
    val requestTime: String
)

/**
 * 社交中枢 ViewModel
 *
 * 核心功能：
 * 1. 管理好友列表（云端同步）
 * 2. 控制"客态访问模式"全局状态
 * 3. 量子脉冲充能逻辑
 * 4. 管理访客日志 & 频段申请的消息状态
 * 5. 向好友花园注入信标留言
 */
class SocialViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager.getInstance(application)
    private val socialService: SocialService = RetrofitClient.socialService

    // ══ 好友列表 ══
    private val _friends = MutableStateFlow<List<Friend>>(emptyList())
    val friends: StateFlow<List<Friend>> = _friends.asStateFlow()

    private val _isLoadingFriends = MutableStateFlow(false)
    val isLoadingFriends: StateFlow<Boolean> = _isLoadingFriends.asStateFlow()

    // ══ 搜索用户 ══
    private val _searchResult = MutableStateFlow<Friend?>(null)
    val searchResult: StateFlow<Friend?> = _searchResult.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError.asStateFlow()

    // ══ 访客日志 (biz_visit_log 云端数据) ══
    private val _visitLogs = MutableStateFlow<List<VisitLog>>(emptyList())
    val visitLogs: StateFlow<List<VisitLog>> = _visitLogs.asStateFlow()

    private val _isLoadingLogs = MutableStateFlow(false)
    val isLoadingLogs: StateFlow<Boolean> = _isLoadingLogs.asStateFlow()

    // ══ 未读消息红点信号 ══
    val hasUnreadMessages: StateFlow<Boolean> = _visitLogs.map { logs ->
        logs.any { !it.isRead }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // ══ 频段申请接收器 (biz_friendship 待审) ══
    private val _friendRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val friendRequests: StateFlow<List<FriendRequest>> = _friendRequests.asStateFlow()

    private val _isLoadingRequests = MutableStateFlow(false)
    val isLoadingRequests: StateFlow<Boolean> = _isLoadingRequests.asStateFlow()

    // ══ 访问状态：如果不为 null，则处于"访客模式" ══
    private val _visitingFriend = MutableStateFlow<Friend?>(null)
    val visitingFriend: StateFlow<Friend?> = _visitingFriend.asStateFlow()

    // ══ 量子脉冲充能状态（今日是否已点亮，次日重置） ══
    private val _isPulseLoading = MutableStateFlow(false)
    val isPulseLoading: StateFlow<Boolean> = _isPulseLoading.asStateFlow()

    private val _hasChargedToday = MutableStateFlow(false)
    val hasChargedToday: StateFlow<Boolean> = _hasChargedToday.asStateFlow()

    // 全局植物发光信号
    private val _allPlantsGlowing = MutableStateFlow(false)
    val allPlantsGlowing: StateFlow<Boolean> = _allPlantsGlowing.asStateFlow()

    private val _pulseMessage = MutableStateFlow<String?>(null)
    val pulseMessage: StateFlow<String?> = _pulseMessage.asStateFlow()

    // ══ 错误消息 ══
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // ══ 操作反馈消息（用于 Snackbar 提示）══
    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    // ══ 下拉刷新状态 ══
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    private fun showActionMessage(message: String) {
        _actionMessage.value = message
    }

    // ══ 初始化：加载数据 ══
    init {
        loadAllData()
    }

    /**
     * 加载所有社交数据
     */
    fun loadAllData() {
        loadFriendList()
        loadFriendRequests()
        loadVisitLogs()
    }

    /**
     * 下拉刷新所有数据
     */
    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            loadAllData()
            // 等待所有加载完成
            kotlinx.coroutines.delay(300)
            _isRefreshing.value = false
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 搜索用户
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * 根据账号搜索用户
     */
    fun searchUser(account: String) {
        if (account.isBlank()) {
            _searchError.value = "请输入账号"
            return
        }

        viewModelScope.launch {
            _isSearching.value = true
            _searchError.value = null
            _searchResult.value = null

            try {
                val response = socialService.searchUser(account.trim())
                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    val data = response.body()?.data
                    if (data != null) {
                        _searchResult.value = Friend(
                            userId = data.userId,
                            nickname = data.nickname,
                            avatarEmoji = getAvatarEmoji(data.avatarId)
                        )
                        Log.d(TAG, "搜索用户成功: ${data.nickname}")
                    } else {
                        _searchError.value = "用户不存在"
                    }
                } else {
                    _searchError.value = response.body()?.message ?: "搜索失败"
                }
            } catch (e: Exception) {
                _searchError.value = e.message ?: "网络错误"
                Log.e(TAG, "搜索用户异常: ${e.message}")
            } finally {
                _isSearching.value = false
            }
        }
    }

    /**
     * 清除搜索结果
     */
    fun clearSearch() {
        _searchResult.value = null
        _searchError.value = null
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 好友系统
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * 加载好友列表
     */
    fun loadFriendList() {
        viewModelScope.launch {
            val userId = sessionManager.userIdFlow.firstOrNull() ?: return@launch
            _isLoadingFriends.value = true
            try {
                val response = socialService.getFriendList(userId)
                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    val data = response.body()?.data ?: emptyList()
                    _friends.value = data.map {
                        Friend(
                            userId = it.userId,
                            nickname = it.nickname,
                            account = it.account,
                            avatarEmoji = getAvatarEmoji(it.avatarId)
                        )
                    }
                    Log.d(TAG, "好友列表加载成功: ${data.size} 人")
                } else {
                    Log.e(TAG, "好友列表加载失败: ${response.body()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "好友列表加载异常: ${e.message}")
            } finally {
                _isLoadingFriends.value = false
            }
        }
    }

    /**
     * 发送好友申请
     */
    fun sendFriendRequest(friendId: Long, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            val userId = sessionManager.userIdFlow.firstOrNull() ?: return@launch
            try {
                val response = socialService.addFriend(userId, SocialService.AddFriendRequest(friendId))
                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    Log.d(TAG, "好友申请发送成功: friendId=$friendId")
                    showActionMessage("好友申请已发送")
                    onSuccess()
                } else {
                    val msg = response.body()?.message ?: "发送失败"
                    Log.e(TAG, "好友申请发送失败: $msg")
                    showActionMessage(msg)
                    onError(msg)
                }
            } catch (e: Exception) {
                Log.e(TAG, "好友申请发送异常: ${e.message}")
                showActionMessage("网络错误: ${e.message}")
                onError(e.message ?: "网络错误")
            }
        }
    }

    /**
     * 同意好友申请
     */
    fun acceptFriendRequest(requestId: Long) {
        viewModelScope.launch {
            val userId = sessionManager.userIdFlow.firstOrNull() ?: return@launch
            try {
                val response = socialService.acceptFriend(userId, requestId)
                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    Log.d(TAG, "好友申请已同意: requestId=$requestId")
                    _friendRequests.value = _friendRequests.value.filter { it.requestId != requestId }
                    showActionMessage("已同意好友申请")
                    // 刷新好友列表
                    loadFriendList()
                } else {
                    val msg = response.body()?.message ?: "操作失败"
                    Log.e(TAG, "同意好友申请失败: $msg")
                    showActionMessage(msg)
                }
            } catch (e: Exception) {
                Log.e(TAG, "同意好友申请异常: ${e.message}")
                showActionMessage("网络错误: ${e.message}")
            }
        }
    }

    /**
     * 拒绝好友申请
     */
    fun rejectFriendRequest(requestId: Long) {
        viewModelScope.launch {
            val userId = sessionManager.userIdFlow.firstOrNull() ?: return@launch
            try {
                val response = socialService.rejectFriend(userId, requestId)
                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    Log.d(TAG, "好友申请已拒绝: requestId=$requestId")
                    _friendRequests.value = _friendRequests.value.filter { it.requestId != requestId }
                    showActionMessage("已拒绝好友申请")
                } else {
                    val msg = response.body()?.message ?: "操作失败"
                    Log.e(TAG, "拒绝好友申请失败: $msg")
                    showActionMessage(msg)
                }
            } catch (e: Exception) {
                Log.e(TAG, "拒绝好友申请异常: ${e.message}")
                showActionMessage("网络错误: ${e.message}")
            }
        }
    }

    /**
     * 加载待处理的好友申请
     */
    fun loadFriendRequests() {
        viewModelScope.launch {
            val userId = sessionManager.userIdFlow.firstOrNull() ?: return@launch
            _isLoadingRequests.value = true
            try {
                val response = socialService.getPendingRequests(userId)
                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    val data = response.body()?.data ?: emptyList()
                    _friendRequests.value = data.map {
                        FriendRequest(
                            requestId = it.friendshipId ?: it.createdAt, // 优先使用 friendshipId
                            fromUserId = it.userId,
                            fromNickname = it.nickname,
                            fromAvatar = getAvatarEmoji(it.avatarId),
                            requestTime = formatTime(it.createdAt)
                        )
                    }
                    Log.d(TAG, "好友申请列表加载成功: ${data.size} 条")
                } else {
                    Log.e(TAG, "好友申请列表加载失败: ${response.body()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "好友申请列表加载异常: ${e.message}")
            } finally {
                _isLoadingRequests.value = false
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 互访系统
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * 加载访客日志
     */
    fun loadVisitLogs() {
        viewModelScope.launch {
            val userId = sessionManager.userIdFlow.firstOrNull() ?: return@launch
            _isLoadingLogs.value = true
            try {
                val response = socialService.getVisitLogs(userId, 50)
                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    val data = response.body()?.data ?: emptyList()
                    _visitLogs.value = data.map {
                        VisitLog(
                            logId = it.logId,
                            visitorId = it.visitorId,
                            visitorNickname = it.visitorNickname,
                            visitorAvatar = getAvatarEmoji(it.visitorAvatarId),
                            actionType = it.actionType,
                            content = it.content ?: if (it.actionType == 1) "释放了全域量子脉冲为你助力" else "",
                            createTime = formatTime(it.createdAt),
                            isRead = it.isRead == 1
                        )
                    }
                    Log.d(TAG, "访客日志加载成功: ${data.size} 条")
                } else {
                    Log.e(TAG, "访客日志加载失败: ${response.body()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "访客日志加载异常: ${e.message}")
            } finally {
                _isLoadingLogs.value = false
            }
        }
    }

    /**
     * 将所有访客日志标记为已读
     */
    fun markAllRead() {
        viewModelScope.launch {
            val userId = sessionManager.userIdFlow.firstOrNull() ?: return@launch
            try {
                val response = socialService.markAllAsRead(userId)
                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    Log.d(TAG, "访客日志已全部标记已读")
                    _visitLogs.value = _visitLogs.value.map { it.copy(isRead = true) }
                } else {
                    Log.e(TAG, "标记已读失败: ${response.body()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "标记已读异常: ${e.message}")
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 花园访问
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * 发起跃迁访问好友花园
     */
    fun visitFriend(friend: Friend) {
        _visitingFriend.value = friend
        _hasChargedToday.value = false
        _allPlantsGlowing.value = false
        _pulseMessage.value = null
        
        // 检查今日是否已点亮该好友花园
        checkHasChargedToday(friend.userId)
    }

    /**
     * 检查今日是否已为该好友点亮花园
     */
    private fun checkHasChargedToday(hostId: Long) {
        viewModelScope.launch {
            val userId = sessionManager.userIdFlow.firstOrNull() ?: return@launch
            try {
                val response = socialService.hasChargedToday(userId, hostId)
                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    _hasChargedToday.value = response.body()?.data ?: false
                    Log.d(TAG, "检查今日点亮状态: hostId=$hostId, hasCharged=${_hasChargedToday.value}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "检查点亮状态异常: ${e.message}")
            }
        }
    }

    /**
     * 退出访问模式
     */
    fun exitVisit() {
        _visitingFriend.value = null
        _hasChargedToday.value = false
        _allPlantsGlowing.value = false
        _isPulseLoading.value = false
        _pulseMessage.value = null
    }

    /**
     * 释放全域量子脉冲（为好友花园点亮）
     */
    fun unleashPulse() {
        if (_hasChargedToday.value || _isPulseLoading.value) return

        val friend = _visitingFriend.value ?: return

        viewModelScope.launch {
            val userId = sessionManager.userIdFlow.firstOrNull() ?: return@launch
            _isPulseLoading.value = true
            _pulseMessage.value = null

            try {
                // 调用后端充能接口
                val response = socialService.chargeForFriend(userId, friend.userId)
                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    delay(500) // 短暂延迟增强体验
                    _isPulseLoading.value = false
                    _hasChargedToday.value = true
                    _allPlantsGlowing.value = true
                    SoundManager.playCharge() // 🎵 充能音效
                    _pulseMessage.value = "⚡ 花园已点亮！赛博植物焕发新生！"
                    Log.d(TAG, "点亮成功: hostId=${friend.userId}")
                } else {
                    _isPulseLoading.value = false
                    val msg = response.body()?.message ?: "点亮失败"
                    _pulseMessage.value = "❌ $msg"
                    Log.e(TAG, "点亮失败: $msg")
                }
            } catch (e: Exception) {
                _isPulseLoading.value = false
                _pulseMessage.value = "❌ 网络错误，请稍后重试"
                Log.e(TAG, "点亮异常: ${e.message}")
            }

            // 3秒后清除消息
            delay(3000)
            _pulseMessage.value = null
        }
    }

    /**
     * 向当前访问的好友花园注入信标留言
     *
     * @param hostId 目标好友的 userId
     * @param content 留言内容
     */
    fun sendBeacon(hostId: Long, content: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            val userId = sessionManager.userIdFlow.firstOrNull() ?: return@launch
            try {
                val response = socialService.leaveMessage(userId, SocialService.LeaveMessageRequest(hostId, content))
                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    Log.d(TAG, "留言发送成功: hostId=$hostId")
                    onSuccess()
                } else {
                    val msg = response.body()?.message ?: "留言失败"
                    Log.e(TAG, "留言发送失败: $msg")
                    onError(msg)
                }
            } catch (e: Exception) {
                Log.e(TAG, "留言发送异常: ${e.message}")
                onError(e.message ?: "网络错误")
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 工具方法
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * 根据 avatarId 获取对应的 emoji（与个人终端头像一致）
     */
    private fun getAvatarEmoji(avatarId: Int): String {
        val totems = listOf(
            "💠", "🔺", "🔯", "🌀", "💎", "⚛️", "🧿", "🌠", "🪐", "🛸", "🔮", "🏮"
        )
        return if (avatarId in 1..12) totems[avatarId - 1] else "💠"
    }

    /**
     * 格式化时间戳为可读字符串
     */
    private fun formatTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60_000 -> "刚刚"
            diff < 3600_000 -> "${diff / 60_000} 分钟前"
            diff < 86400_000 -> "${diff / 3600_000} 小时前"
            diff < 604800_000 -> "${diff / 86400_000} 天前"
            else -> {
                val sdf = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.CHINA)
                sdf.format(java.util.Date(timestamp))
            }
        }
    }
}