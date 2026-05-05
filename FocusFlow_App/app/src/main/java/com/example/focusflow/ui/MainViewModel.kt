package com.example.focusflow.ui

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.focusflow.api.AuthService
import com.example.focusflow.api.RetrofitClient
import com.example.focusflow.data.AppDatabase
import com.example.focusflow.data.entity.FocusRecordEntity
import com.example.focusflow.data.entity.UserEntity
import com.example.focusflow.data.repository.FocusRepository
import com.example.focusflow.data.repository.UserRepository
import com.example.focusflow.data.repository.BagRepository
import com.example.focusflow.api.FocusService
import com.example.focusflow.service.TimeFluxSyncService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

// ================= 数据类定义区域 =================

// 1. 柱状图数据模型
data class BarData(
    val label: String,      // 如 "周一" 或 "12/30"
    val value: Float,       // 专注时长
    val isToday: Boolean,   // 是否高亮
    val date: LocalDate? = null  // 对应日期，用于点击跳转
)

// 2. 饼图统计摘要
data class StatsSummary(
    val totalMinutes: Int = 0,      // 显示用：剩余分钟数
    val totalHours: Int = 0,        // 显示用：小时数
    val totalMinutesRaw: Int = 0,   // 完整总分钟数
    val recordCount: Int = 0,       // 记录数量
    val pieData: List<Float> = emptyList(),   // 饼图百分比
    val legendNames: List<String> = emptyList(), // 标签名
    val pieColors: List<Color> = emptyList()     // 颜色
)

// 3. 用户等级模型
data class UserLevel(
    val level: Int, 
    val title: String, 
    val currentExp: Int, 
    val nextLevelExp: Int
)

// 4. 颜色常量
val ChartColors = listOf(
    Color(0xFF00E5FF), Color(0xFFFF2975), Color(0xFFF2CE1B),
    Color(0xFF8C52FF), Color(0xFF3DDC84), Color(0xFFFF8C00)
)

// ================= ViewModel 实现 =================

/**
 * 主 ViewModel
 * 
 * 重构说明：
 * - 使用 Repository 层而不是直接操作 DAO
 * - 依赖注入 FocusRepository, UserRepository
 * - 状态管理：currentUser 观察 userRepository，用于 MineScreen 显示金币和昵称
 * - saveFocusRecord 调用 focusRepository.saveAndCalculateReward()，内部已封装事务处理
 * 
 * 🔧 [CLOUD-ONLY] 花园和背包采用纯云端模式，不依赖本地 Repository
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    companion object {
        private const val TAG = "MainViewModel"
    }
    
    // ========== 依赖注入 ==========
    
    private val database = AppDatabase.getDatabase(application)
    private val sessionManager = com.example.focusflow.data.session.SessionManager.getInstance(application)
    
    // 🔧 [CLOUD-ONLY] 云端 API 服务
    private val authService: AuthService = RetrofitClient.authService
    
    // Repository 层（业务逻辑中枢）
    private val userRepository = UserRepository(database.userDao())
    private val focusRepository = FocusRepository(
        database.focusRecordDao(),
        database.userDao(),
        sessionManager
    )
    private val bagRepository = BagRepository(
        database.userDao()
    )

    /**
     * 当前用户（响应式切换）
     * 核心逻辑：监听 SessionManager 的 userIdFlow，当 ID 变化时，自动从数据库捞取对应实体。
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentUser: StateFlow<UserEntity?> = sessionManager.userIdFlow
        .flatMapLatest { id ->
            if (id != null) {
                userRepository.getUserFlow(id)
            } else {
                flowOf(null)
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    /**
     * 用户昵称（双重保险：DB 优先，Session 兜底）
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val nickname: StateFlow<String> = combine(currentUser, sessionManager.nicknameFlow) { user, sessionName ->
        user?.nickname ?: sessionName ?: "Focus Runner"
    }.stateIn(viewModelScope, SharingStarted.Lazily, "Focus Runner")
    
    /**
     * Time Flux 余额（从云端实时获取）
     * 🔧 [CLOUD-ONLY] 不再从本地数据库获取，防止本地篡改
     */
    private val _timeFlux = MutableStateFlow(0)
    val timeFlux: StateFlow<Int> = _timeFlux.asStateFlow()
    
    /**
     * 用户等级（模拟数据，可以后续从 UserEntity 扩展）
     */
    private val _userLevel = MutableStateFlow(UserLevel(1, "初级专注者", 0, 100))
    val userLevel: StateFlow<UserLevel> = _userLevel.asStateFlow()
    
    /**
     * 下拉刷新状态
     */
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // ========== 统计数据 ==========
    
    /**
     * 新表的今日记录（从 FocusRepository 获取）
     * 用于实时统计今日专注时长
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val todayRecordsNew: StateFlow<List<com.example.focusflow.data.entity.FocusRecordEntity>> = 
        currentUser.flatMapLatest { user ->
            if (user != null) {
                focusRepository.getTodayRecordsFlow(user.id)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    /**
     * 新表的本周记录（从 FocusRepository 获取）
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val weekRecordsNew: StateFlow<List<com.example.focusflow.data.entity.FocusRecordEntity>> = 
        currentUser.flatMapLatest { user ->
            if (user != null) {
                focusRepository.getWeekRecordsFlow(user.id)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    /**
     * 今日总专注时长（分钟）- 从新表获取
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val totalFocusMinutesToday: StateFlow<Int> = currentUser.flatMapLatest { user ->
            if (user != null) {
                focusRepository.getTodayTotalMinutesFlow(user.id)
            } else {
                flowOf(0)
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, 0)
    
    /**
     * 本周总专注时长（分钟）- 从新表获取
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val weekTotalMinutes: StateFlow<Int> = 
        currentUser.flatMapLatest { user ->
            if (user != null) {
                focusRepository.getWeekTotalMinutesFlow(user.id)
            } else {
                flowOf(0)
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, 0)
    
    /**
     * 所有专注记录（从新表 app_focus_record 获取）
     * 🔧 [SYNC] 统一从 FocusRecordDao 读取，与保存逻辑一致
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val allRecordsNew: StateFlow<List<FocusRecordEntity>> = currentUser.flatMapLatest { user ->
            if (user != null) {
                focusRepository.getAllRecordsFlow(user.id)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    /**
     * 当前选中的日期（默认为今天）
     */
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()
    
    /**
     * 日报统计（响应 selectedDate 的变化）
     */
    val dailyStats: StateFlow<StatsSummary> = combine(allRecordsNew, _selectedDate) { records, date ->
        calculateStatsByTagNew(records, targetDate = date)
    }.stateIn(viewModelScope, SharingStarted.Lazily, StatsSummary())
    
    /**
     * 周报统计（柱状图）
     */
    val weeklyStats: StateFlow<List<BarData>> = allRecordsNew.map { records ->
        calculateWeeklyBarDataNew(records)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    /**
     * 今日专注时长（StatsScreen 顶部卡片）
     */
    val todayStats: StateFlow<StatsSummary> = allRecordsNew.map { records ->
        calculateStatsByTagNew(records, targetDate = LocalDate.now())
    }.stateIn(viewModelScope, SharingStarted.Lazily, StatsSummary())
    
    /**
     * 总体统计（所有记录）
     */
    val totalStats: StateFlow<StatsSummary> = allRecordsNew.map { records ->
        calculateTotalStatsNew(records)
    }.stateIn(viewModelScope, SharingStarted.Lazily, StatsSummary())

    // ========== 初始化 ==========
    
    init {
        // 🔧 [AUTH] 监听用户登录状态，登录成功后自动触发同步
        viewModelScope.launch {
            sessionManager.userIdFlow.collect { userId ->
                if (userId != null) {
                    Log.d(TAG, "用户已登录: userId=$userId，开始同步数据")
                    ensureUserExists()
                    refreshFromCloud()
                } else {
                    Log.d(TAG, "用户未登录，等待登录...")
                }
            }
        }
    }
    
    /**
     * ═══════════════════════════════════════════════════════════════
     * 从云端刷新用户数据
     * 🔧 [SYNC] 先上传本地待同步记录，再拉取云端数据
     * 🔧 [AUTH] 未登录时不执行，由 MainActivity 登录守卫跳转登录页
     * ═══════════════════════════════════════════════════════════════
     */
    fun refreshFromCloud() {
        viewModelScope.launch {
            // 🔧 [AUTH] 未登录时不执行，由 MainActivity 登录守卫处理
            val userId = sessionManager.userIdFlow.firstOrNull()
            if (userId == null) {
                Log.d(TAG, "refreshFromCloud: 用户未登录，跳过同步")
                return@launch
            }
            Log.d(TAG, "refreshFromCloud: userId=$userId")
            
            _isRefreshing.value = true
            try {
                // 🔧 [SYNC-UPLOAD] 先上传本地待同步的记录到云端
                pushLocalRecordsToCloud(userId)
                
                Log.d(TAG, "从云端刷新用户数据: userId=$userId")
                val response = authService.getUserInfo(userId)
                
                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    val cloudUser = response.body()?.data
                    if (cloudUser != null) {
                        _timeFlux.value = cloudUser.timeFlux
                        // 同步到 TimeFluxSyncService（首页和我的页面会实时更新）
                        TimeFluxSyncService.getInstance(getApplication()).updateTimeFluxDirectly(cloudUser.timeFlux)
                        Log.d(TAG, "云端数据刷新成功: timeFlux=${cloudUser.timeFlux}")
                    }
                } else {
                    Log.w(TAG, "从云端获取用户数据失败: ${response.body()?.message}")
                }
                
                // 🔧 [SYNC-DOWNLOAD] 拉取云端专注记录到本地
                pullFocusRecordsFromCloud(userId)
            } catch (e: Exception) {
                Log.e(TAG, "从云端刷新用户数据失败", e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }
    
    /**
     * ═══════════════════════════════════════════════════════════════
     * 上传本地待同步的专注记录到云端
     * 🔧 [LOCAL-FIRST] 本地优先，联网后自动同步
     * ═══════════════════════════════════════════════════════════════
     */
    private suspend fun pushLocalRecordsToCloud(userId: Long) {
        try {
            // 获取本地待同步的记录（syncStatus = 0）
            val pendingRecords = database.focusRecordDao().getPendingSyncRecords(userId)
            
            if (pendingRecords.isEmpty()) {
                Log.d(TAG, "没有待同步的本地记录")
                return
            }
            
            Log.d(TAG, "发现 ${pendingRecords.size} 条待同步记录，准备上传到云端")
            
            // 转换为同步DTO
            val syncDTOs = pendingRecords.map { record ->
                com.example.focusflow.api.FocusRecordSyncDTO(
                    recordId = record.recordId,
                    userId = record.userId,
                    taskName = record.taskName,
                    durationMinutes = record.durationMinutes,
                    startTime = record.startTime,
                    signature = record.signature
                )
            }
            
            val request = com.example.focusflow.api.BatchSyncRequest(records = syncDTOs)
            val response = RetrofitClient.syncService.batchSyncFocusRecords(request)
            
            if (response.isSuccessful && response.body()?.code == 200) {
                val results = response.body()?.data ?: emptyList()
                Log.d(TAG, "批量同步完成: ${results.size} 条记录")
                
                // 更新已成功同步的记录状态
                results.filter { it.success }.forEach { result ->
                    database.focusRecordDao().updateSyncStatus(result.recordId, 1)
                }
                
                val failCount = results.count { !it.success }
                if (failCount > 0) {
                    Log.w(TAG, "$failCount 条记录同步失败")
                }
            } else {
                Log.w(TAG, "批量同步失败: ${response.body()?.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "上传本地记录到云端失败", e)
        }
    }
    
    /**
     * ═══════════════════════════════════════════════════════════════
     * 从云端拉取专注记录到本地数据库
     * 同时写入新表（用于云同步）和旧表（用于统计显示）
     * ═══════════════════════════════════════════════════════════════
     */
    private suspend fun pullFocusRecordsFromCloud(userId: Long) {
        try {
            Log.d(TAG, "从云端拉取专注记录: userId=$userId")
            val response = RetrofitClient.focusService.getUserFocusRecords(userId, limit = 500)
            
            if (response.isSuccessful && response.body()?.code == 200) {
                val cloudRecords = response.body()?.data ?: emptyList()
                Log.d(TAG, "云端返回 ${cloudRecords.size} 条专注记录")
                
                // 获取本地已有的recordId (新表)
                val localRecordIds = database.focusRecordDao().getAllRecords(userId)
                    .map { it.recordId }.toSet()
                
                // 筛选出本地没有的记录
                val newRecords = cloudRecords.filter { it.recordId !in localRecordIds }
                
                if (newRecords.isNotEmpty()) {
                    Log.d(TAG, "发现 ${newRecords.size} 条新记录需要同步到本地")
                    
                    // 1. 写入新表（用于云同步）
                    // 注意：云端历史记录已结算，设置 rewardSettled = 1 避免重复掉落种子
                    val entities = newRecords.map { cloudRecord ->
                        FocusRecordEntity(
                            recordId = cloudRecord.recordId,
                            userId = cloudRecord.userId,
                            taskName = cloudRecord.taskName ?: "专注",
                            durationMinutes = cloudRecord.durationMinutes ?: 0,
                            startTime = cloudRecord.startTime ?: System.currentTimeMillis(),
                            signature = cloudRecord.signature ?: "",
                            syncStatus = 1, // 已同步
                            rewardSettled = 1 // 已结算，避免重复掉落种子
                        )
                    }
                    database.focusRecordDao().insertRecords(entities)
                    
                    Log.d(TAG, "云端专注记录同步完成")
                } else {
                    Log.d(TAG, "本地已是最新，无需同步")
                }
            } else {
                Log.w(TAG, "从云端获取专注记录失败: ${response.body()?.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "从云端拉取专注记录失败", e)
        }
    }
    
    /**
     * 确保用户存在
     * 
     * 设计意图：
     * - 检查 userRepository.currentUser
     * - 如果为空（首次安装），自动创建默认用户
     * - 生成 UUID，昵称"专注者"
     * - 确保后续所有外键关联不会报错
     * 🔧 [AUTH] 未登录时不执行，由 MainActivity 登录守卫处理
     */
    private suspend fun ensureUserExists() {
        try {
            // 🔧 [AUTH] 未登录时不执行
            val currentId = sessionManager.userIdFlow.firstOrNull()
            if (currentId == null) {
                Log.d(TAG, "ensureUserExists: 用户未登录，跳过")
                return
            }
            
            val userInDb = userRepository.getUserById(currentId)
            if (userInDb == null) {
                // 如果数据库里还没这号人，创建一个（同步从 Session 获取的昵称）
                val nickname = sessionManager.nicknameFlow.firstOrNull() ?: "专注者"
                val newUser = UserEntity(
                    id = currentId,
                    account = "User_$currentId",
                    password = "pwd",
                    nickname = nickname,
                    avatarId = 1,
                    timeFlux = 0,
                    status = 0
                )
                userRepository.saveUser(newUser)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ========== 核心方法 ==========
    
    /**
     * 保存专注记录（重构版本 - Critical）
     * 
     * 设计意图：
     * - 调用 focusRepository.saveAndCalculateReward()
     * - 确保用户付出的专注时间一定能转化为货币
     * 
     * 业务逻辑（由 FocusRepository 封装）：
     * 1. 插入 FocusRecordEntity 到新表
     * 2. 增加用户的 timeFlux (光流) - 公式: durationMinutes * rewardPerMinute
     * 3. 所有操作在事务中完成
     * 
     * 🔧 [CLOUD-ONLY] 花园模块采用纯云端模式，植物生长逻辑由后端处理
     * 
     * @param taskName 任务名称
     * @param minutes 专注时长（分钟）
     * @param tag 标签（如 "番茄工作法"）
     */
    fun saveFocusRecord(taskName: String, minutes: Int, tag: String) {
        viewModelScope.launch {
            try {
                // 🟢 [CONFIG RELOAD] 刷新配置，确保所有配置实时生效
                try {
                    com.example.focusflow.data.AppConfigManager.reload(
                        com.example.focusflow.api.RetrofitClient.configService
                    )
                } catch (e: Exception) {
                    android.util.Log.w("MainViewModel", "配置刷新失败: ${e.message}")
                }
                
                // 获取当前用户
                val user = currentUser.value
                if (user == null) {
                    // 如果没有用户，先确保用户存在
                    ensureUserExists()
                    // 重新获取用户
                    val newUser = userRepository.getCurrentUser()
                    if (newUser == null) {
                        // 仍然失败，记录错误
                        android.util.Log.e("MainViewModel", "无法创建或获取用户")
                        return@launch
                    }
                }
                
                val userId = user?.id ?: userRepository.getCurrentUser()?.id
                if (userId == null) {
                    android.util.Log.e("MainViewModel", "用户 ID 为空")
                    return@launch
                }
                
                // 1. 完成专注会话（Repository 内部已封装：插入记录 + 增加 timeFlux）
                // 响应指令：奖励 = 分钟数 * 每分钟光流奖励（从配置获取）
                val rewardPerMinute = com.example.focusflow.data.AppConfigManager.getFocusRewardPerMinute()
                android.util.Log.d("MainViewModel", "每分钟光流奖励: $rewardPerMinute")
                val rewardAmount = minutes * rewardPerMinute
                focusRepository.saveAndCalculateReward(
                    userId = userId,
                    taskName = taskName,
                    durationMinutes = minutes,
                    rewardAmount = rewardAmount,
                    startTime = System.currentTimeMillis()
                )
                
                // 🔧 [CLOUD-ONLY] 花园模块采用纯云端模式，植物生长由后端处理
                // 不再调用本地 gardenRepository.updatePlantGrowth()
                
                // 2. 更新用户等级（模拟经验值增长）
                _userLevel.value = _userLevel.value.copy(
                    currentExp = _userLevel.value.currentExp + minutes
                )
                
            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.e("MainViewModel", "保存专注记录失败", e)
                // 专注记录已保存到本地 Room 数据库，后续会自动同步
            }
        }
    }
    
    /**
     * 更新选中的日期
     */
    fun updateSelectedDate(date: LocalDate) {
        _selectedDate.value = date
    }
    
    /**
     * 加载用户数据
     * 从 Repository 加载用户信息
     */
    fun loadUserData(context: Context) {
        viewModelScope.launch {
            try {
                // currentUser 已经通过 Flow 自动更新，无需手动加载
                // 这里保留方法签名以保持兼容性
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 更新用户昵称
     */
    fun updateNickname(context: Context, newName: String) {
        viewModelScope.launch {
            try {
                val user = currentUser.value
                if (user != null) {
                    userRepository.updateNickname(user.id, newName)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 同步光流余额（从云端同步后更新本地缓存）
     * 
     * @param newTimeFlux 云端返回的最新光流余额
     */
    fun syncTimeFlux(newTimeFlux: Int) {
        // 🔧 [CLOUD-ONLY] 直接更新 StateFlow，数据由云端返回
        _timeFlux.value = newTimeFlux
    }
    

    // ========== 新表统计函数 (FocusRecordEntity) ==========
    
    /**
     * 按任务名称计算统计数据（新表版本）
     * 
     * @param records 专注记录列表 (FocusRecordEntity)
     * @param targetDate 目标日期
     * @return 统计摘要（总时长、饼图数据等）
     */
    private fun calculateStatsByTagNew(
        records: List<FocusRecordEntity>,
        targetDate: LocalDate
    ): StatsSummary {
        // 过滤出目标日期的记录
        val filteredRecords = records.filter {
            val date = java.time.Instant.ofEpochMilli(it.startTime)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            date == targetDate
        }

        if (filteredRecords.isEmpty()) return StatsSummary()

        // 计算总时长
        val totalMins = filteredRecords.sumOf { it.durationMinutes }
        val totalHrs = totalMins / 60

        // 按任务名称分组并排序（新表没有 tag 字段）
        val groupedByTask = filteredRecords
            .groupBy { it.taskName }
            .mapValues { entry -> entry.value.sumOf { it.durationMinutes } }
            .toList()
            .sortedByDescending { it.second }

        // 生成饼图数据
        val percentages = mutableListOf<Float>()
        val labels = mutableListOf<String>()
        val colors = mutableListOf<Color>()

        groupedByTask.forEachIndexed { index, (taskName, minutes) ->
            percentages.add(minutes.toFloat() / totalMins.toFloat())
            labels.add(taskName)
            colors.add(ChartColors[index % ChartColors.size])
        }

        return StatsSummary(
            totalMinutes = totalMins % 60,
            totalHours = totalHrs,
            pieData = percentages,
            legendNames = labels,
            pieColors = colors
        )
    }
    
    /**
     * 计算总体统计（新表版本）
     * 
     * @param records 专注记录列表 (FocusRecordEntity)
     * @return 统计摘要（总时长、饼图数据等）
     */
    private fun calculateTotalStatsNew(records: List<FocusRecordEntity>): StatsSummary {
        if (records.isEmpty()) return StatsSummary()

        // 计算总时长
        val totalMins = records.sumOf { it.durationMinutes }
        val totalHrs = totalMins / 60

        // 按任务名称分组并排序
        val groupedByTask = records
            .groupBy { it.taskName }
            .mapValues { entry -> entry.value.sumOf { it.durationMinutes } }
            .toList()
            .sortedByDescending { it.second }

        // 生成饼图数据
        val percentages = mutableListOf<Float>()
        val labels = mutableListOf<String>()
        val colors = mutableListOf<Color>()

        groupedByTask.forEachIndexed { index, (taskName, minutes) ->
            percentages.add(minutes.toFloat() / totalMins.toFloat())
            labels.add(taskName)
            colors.add(ChartColors[index % ChartColors.size])
        }

        return StatsSummary(
            totalMinutes = totalMins % 60,
            totalHours = totalHrs,
            totalMinutesRaw = totalMins,
            recordCount = records.size,
            pieData = percentages,
            legendNames = labels,
            pieColors = colors
        )
    }
    
    /**
     * 计算周报柱状图数据（新表版本）
     * 
     * @param records 专注记录列表 (FocusRecordEntity)
     * @return 最近 7 天的柱状图数据
     */
    private fun calculateWeeklyBarDataNew(records: List<FocusRecordEntity>): List<BarData> {
        val today = LocalDate.now()
        val result = mutableListOf<BarData>()
        
        for (i in 6 downTo 0) {
            val date = today.minusDays(i.toLong())
            
            // 计算该日期的总专注时长
            val mins = records.filter {
                val rDate = java.time.Instant.ofEpochMilli(it.startTime)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                rDate == date
            }.sumOf { it.durationMinutes }.toFloat()

            val label = "${date.monthValue}/${date.dayOfMonth}"
            result.add(BarData(label, mins, date == today, date))
        }
        
        return result
    }
}
