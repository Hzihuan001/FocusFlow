package com.example.focusflow.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.focusflow.api.GardenService
import com.example.focusflow.api.GardenTileDto
import com.example.focusflow.api.LeaderboardEntryDto
import com.example.focusflow.api.PlantRequest
import com.example.focusflow.api.RetrofitClient
import com.example.focusflow.data.AppDatabase
import com.example.focusflow.data.entity.PlantDictEntity
import com.example.focusflow.data.session.SessionManager
import com.example.focusflow.service.NetworkMonitor
import com.example.focusflow.utils.ApiCacheManager
import com.example.focusflow.utils.PlantImageLoader
import com.example.focusflow.utils.SoundManager
import com.example.focusflow.service.TimeFluxSyncService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.Color

/**
 * ═══════════════════════════════════════════════════════════════
 * 惰性枯萎状态机 (Lazy Wither State Machine)
 * ═══════════════════════════════════════════════════════════════
 */
enum class GardenVitalityState {
    VIBRANT {
        override val displayName = "活跃"
        override val description = "花园充满生机"
        override val saturationMultiplier = 1.0f
        override val brightnessMultiplier = 1.0f
        override val glowIntensity = 1.0f
    },
    WARNING {
        override val displayName = "警告"
        override val description = "能量正在流失..."
        override val saturationMultiplier = 0.6f
        override val brightnessMultiplier = 0.8f
        override val glowIntensity = 0.5f
    },
    WITHERED {
        override val displayName = "枯萎"
        override val description = "花园已陷入沉寂"
        override val saturationMultiplier = 0.0f
        override val brightnessMultiplier = 0.5f
        override val glowIntensity = 0.0f
    };

    abstract val displayName: String
    abstract val description: String
    abstract val saturationMultiplier: Float
    abstract val brightnessMultiplier: Float
    abstract val glowIntensity: Float

    companion object {
        const val VIBRANT_THRESHOLD_MS = 24 * 60 * 60 * 1000L
        const val WARNING_THRESHOLD_MS = 48 * 60 * 60 * 1000L

        fun fromTimeDiff(lastFocusTimeMs: Long, currentTimeMs: Long = System.currentTimeMillis()): GardenVitalityState {
            val diff = currentTimeMs - lastFocusTimeMs
            return when {
                diff < VIBRANT_THRESHOLD_MS -> VIBRANT
                diff < WARNING_THRESHOLD_MS -> WARNING
                else -> WITHERED
            }
        }
    }
}

data class GardenVitality(
    val state: GardenVitalityState,
    val lastFocusTimeMs: Long,
    val hoursSinceLastFocus: Float,
    val hasFocusRecord: Boolean
) {
    companion object {
        val EMPTY = GardenVitality(
            state = GardenVitalityState.WITHERED,
            lastFocusTimeMs = 0L,
            hoursSinceLastFocus = Float.MAX_VALUE,
            hasFocusRecord = false
        )
    }
}

/**
 * 种植模式状态
 */
data class PlacementState(
    val bagRecordId: String,
    val plantId: Int,
    val plantName: String
)

/**
 * ═══════════════════════════════════════════════════════════════
 * 花园 ViewModel - 纯云端版本
 * ═══════════════════════════════════════════════════════════════
 *
 * 【架构变更】
 * 本地不再存储花园数据，所有操作直接访问云端 API。
 * 断网时花园功能不可用。
 *
 * @author FocusFlow Team
 * @since 2026-03-20
 */
class GardenViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "GardenViewModel"
    }

    // 依赖组件
    private val sessionManager = SessionManager.getInstance(application)
    private val networkMonitor = NetworkMonitor.getInstance(application)
    private val gardenService: GardenService = RetrofitClient.gatewayService
    private val database = AppDatabase.getDatabase(application)
    private val focusRecordDao = database.focusRecordDao()
    private val plantDictDao = database.plantDictDao()

    // 用户 ID
    private val _currentUserId = MutableStateFlow<Long?>(null)
    val currentUserId: StateFlow<Long?> = _currentUserId.asStateFlow()

    // 网络状态
    val networkStatus: StateFlow<NetworkMonitor.NetworkStatus> = networkMonitor.networkStatus

    // 花园地块数据（云端）
    private val _gardenTiles = MutableStateFlow<List<GardenTileDto>>(emptyList())
    val gardenTiles: StateFlow<List<GardenTileDto>> = _gardenTiles.asStateFlow()

    // 可种植区域
    private val _plantableTiles = MutableStateFlow<Set<Pair<Int, Int>>>(setOf(0 to 0))
    val plantableTiles: StateFlow<Set<Pair<Int, Int>>> = _plantableTiles.asStateFlow()

    // 种植模式
    private val _placementMode = MutableStateFlow<PlacementState?>(null)
    val placementMode: StateFlow<PlacementState?> = _placementMode.asStateFlow()

    // 操作消息
    private val _operationMessage = MutableStateFlow<String?>(null)
    val operationMessage: StateFlow<String?> = _operationMessage.asStateFlow()

    // 花园活力状态
    private val _gardenVitality = MutableStateFlow(GardenVitality.EMPTY)
    val gardenVitality: StateFlow<GardenVitality> = _gardenVitality.asStateFlow()

    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // ═══════════════════════════════════════════════════════════════
    // 🟢 [DEFERRED RENDERING] 延迟渲染状态
    // 
    // 用于解决首帧卡顿：页面进入时立即渲染骨架，延迟 300ms 后触发完整渲染
    // 状态流转：false (骨架) → true (完整渲染 + 全息扫描动画)
    // ═══════════════════════════════════════════════════════════════
    private val _isGridReady = MutableStateFlow(false)
    val isGridReady: StateFlow<Boolean> = _isGridReady.asStateFlow()

    // 访客模式标志（正在查看好友花园）
    private val _isGuestMode = MutableStateFlow(false)
    val isGuestMode: StateFlow<Boolean> = _isGuestMode.asStateFlow()
    
    // 加载任务 Job（用于取消之前的加载）
    private var loadJob: kotlinx.coroutines.Job? = null

    // ═══════════════════════════════════════════════════════════════
    // 兼容性属性 (为了兼容 GardenScreen)
    // ═══════════════════════════════════════════════════════════════

    // 植物图鉴字典（从云端加载）
    private val _plantDict = MutableStateFlow<Map<Int, PlantDictEntity>>(emptyMap())
    val plantDict: StateFlow<Map<Int, PlantDictEntity>> = _plantDict.asStateFlow()

    // 兼容 plants 列表 - 从 gardenTiles 转换
    private val _plants = MutableStateFlow<List<LegacyPlant>>(emptyList())
    val plants: StateFlow<List<LegacyPlant>> = _plants.asStateFlow()

    // 已净化区域
    private val _purifiedTiles = MutableStateFlow<Set<Pair<Int, Int>>>(emptySet())
    val purifiedTiles: StateFlow<Set<Pair<Int, Int>>> = _purifiedTiles.asStateFlow()

    // 待种植位置
    private val _pendingPlantPosition = MutableStateFlow<Pair<Int, Int>?>(null)
    val pendingPlantPosition: StateFlow<Pair<Int, Int>?> = _pendingPlantPosition.asStateFlow()

    // 已种植项目（兼容旧代码）
    val plantedItems: StateFlow<List<LegacyPlant>> = _plants.asStateFlow()

    init {
        // 启动网络监控
        networkMonitor.startMonitoring()
        
        // 订阅用户ID变化（登录/登出时自动更新）
        viewModelScope.launch {
            sessionManager.userIdFlow.collect { userId ->
                val previousUserId = _currentUserId.value
                _currentUserId.value = userId
                
                // 用户ID变化时重新加载数据（访客模式下不覆盖）
                // 注意：初始加载由 GardenScreen 的 LaunchedEffect 触发
                if (userId != null && userId != previousUserId && !_isGuestMode.value && previousUserId != null) {
                    Log.d(TAG, "用户ID变化: $previousUserId -> $userId，重新加载花园数据")
                    loadGardenFromCloud()
                }
            }
        }
        // 初始加载由 GardenScreen 的 LaunchedEffect 触发，这里不自动加载
    }

    /**
     * 兼容旧代码的植物数据类
     */
    data class LegacyPlant(
        val x: Int,
        val y: Int,
        val plantId: Int,
        val instanceId: String,
        val status: Int = 0,
        val currentGrowth: Int = 0
    )

    /**
     * 从云端加载花园数据
     */
    fun loadGardenFromCloud() {
        // 取消之前的加载任务
        loadJob?.cancel()
        
        loadJob = viewModelScope.launch {
            val userId = _currentUserId.value
            Log.d(TAG, "loadGardenFromCloud: userId=$userId, isConnected=${networkMonitor.isConnected}")
            
            if (userId == null) {
                Log.w(TAG, "用户ID为空，跳过加载")
                return@launch
            }

            if (!networkMonitor.isConnected) {
                _operationMessage.value = "网络不可用，无法加载花园"
                return@launch
            }

            // 退出访客模式
            _isGuestMode.value = false
            _isLoading.value = true
            try {
                // 加载花园地块
                Log.d(TAG, "正在加载花园地块...")
                val response = gardenService.getGardenTiles(userId)
                Log.d(TAG, "花园地块响应: code=${response.code}, tiles.size=${response.data?.tiles?.size}")
                
                if (response.code == 200 && response.data != null) {
                    val tiles = response.data.tiles
                    val lastFocusTime = response.data.lastFocusTime
                    
                    _gardenTiles.value = tiles
                    updatePlantableTiles(tiles)
                    
                    // 转换为兼容格式
                    _plants.value = tiles.map { tile ->
                        LegacyPlant(
                            x = tile.x,
                            y = tile.y,
                            plantId = tile.plantId ?: 0,
                            instanceId = tile.bagRecordId ?: tile.tileId.toString(),
                            status = if (tile.isPurified) 1 else 0,
                            currentGrowth = 0
                        )
                    }
                    
                    // 更新净化区域
                    _purifiedTiles.value = tiles
                        .filter { it.isPurified }
                        .map { it.x to it.y }
                        .toSet()
                    
                    // 更新花园活力状态（使用服务器返回的最后专注时间）
                    updateGardenVitalityFromServer(lastFocusTime)
                    
                    Log.d(TAG, "加载花园数据成功: ${tiles.size} 条记录, lastFocusTime=$lastFocusTime")
                } else {
                    Log.w(TAG, "加载花园数据失败: code=${response.code}, message=${response.message}")
                    _operationMessage.value = "加载失败: ${response.message}"
                }
                
                // 加载植物图鉴（每次进入花园时强制同步云端数据）
                Log.d(TAG, "准备加载植物图鉴...")
                loadPlantDict(forceRefresh = true)
            } catch (e: Exception) {
                Log.e(TAG, "加载花园数据失败", e)
                _operationMessage.value = "网络错误: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 加载好友的花园数据（访客模式）
     */
    fun loadFriendGarden(friendId: Long) {
        // 取消之前的加载任务
        loadJob?.cancel()
        
        loadJob = viewModelScope.launch {
            if (!networkMonitor.isConnected) {
                _operationMessage.value = "网络不可用，无法加载好友花园"
                return@launch
            }

            // 进入访客模式
            _isGuestMode.value = true
            _isLoading.value = true
            try {
                // 加载好友花园地块
                val response = gardenService.getGardenTiles(friendId)
                if (response.code == 200 && response.data != null) {
                    val tiles = response.data.tiles
                    val lastFocusTime = response.data.lastFocusTime
                    
                    _gardenTiles.value = tiles
                    updatePlantableTiles(tiles)
                    
                    // 转换为兼容格式
                    _plants.value = tiles.map { tile ->
                        LegacyPlant(
                            x = tile.x,
                            y = tile.y,
                            plantId = tile.plantId ?: 0,
                            instanceId = tile.bagRecordId ?: tile.tileId.toString(),
                            status = if (tile.isPurified) 1 else 0,
                            currentGrowth = 0
                        )
                    }
                    
                    // 更新净化区域
                    _purifiedTiles.value = tiles
                        .filter { it.isPurified }
                        .map { it.x to it.y }
                        .toSet()
                    
                    // 更新花园活力状态（使用好友的最后专注时间）
                    updateGardenVitalityFromServer(lastFocusTime)
                    
                    Log.d(TAG, "加载好友花园数据成功: ${tiles.size} 条记录, friendId=$friendId, lastFocusTime=$lastFocusTime")
                } else {
                    _operationMessage.value = "加载失败: ${response.message}"
                }
                
                // 加载植物图鉴（每次进入花园时强制同步云端数据）
                loadPlantDict(forceRefresh = true)
            } catch (e: Exception) {
                Log.e(TAG, "加载好友花园数据失败", e)
                _operationMessage.value = "网络错误: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 从服务器返回的最后专注时间更新花园活力状态
     */
    private fun updateGardenVitalityFromServer(lastFocusTimeMs: Long) {
        val currentTimeMs = System.currentTimeMillis()
        val state = GardenVitalityState.fromTimeDiff(lastFocusTimeMs, currentTimeMs)

        val hoursSinceLastFocus = if (lastFocusTimeMs > 0) {
            (currentTimeMs - lastFocusTimeMs) / (1000f * 60f * 60f)
        } else {
            Float.MAX_VALUE
        }

        _gardenVitality.value = GardenVitality(
            state = state,
            lastFocusTimeMs = lastFocusTimeMs,
            hoursSinceLastFocus = hoursSinceLastFocus,
            hasFocusRecord = lastFocusTimeMs > 0
        )
    }

    /**
     * 加载植物图鉴并同步到本地数据库
     * 
     * 【缓存策略】
     * 1. 优先从本地数据库读取（快速响应）
     * 2. 判断是否需要从网络更新：
     *    - forceRefresh=true 时强制更新（每次进入花园时）
     *    - 本地无数据
     *    - 缓存过期（24小时）
     */
    private suspend fun loadPlantDict(forceRefresh: Boolean = false) {
        val CACHE_KEY = "plant_dict"
        
        Log.d(TAG, "开始加载植物图鉴, forceRefresh=$forceRefresh")
        
        // 1. 优先加载本地缓存（快速响应）
        val localPlants = plantDictDao.getAllPlants()
        Log.d(TAG, "本地植物图鉴数量: ${localPlants.size}, imageUrl数量: ${localPlants.count { !it.imageUrl.isNullOrBlank() }}")
        
        if (localPlants.isNotEmpty()) {
            val dict = localPlants.associateBy { it.plantId }
            _plantDict.value = dict
            Log.d(TAG, "从本地加载植物图鉴: ${dict.size} 种植物")
        }
        
        // 2. 判断是否需要网络更新
        // 🔧 修复：forceRefresh=true 时强制从网络更新（每次进入花园时同步）
        val shouldUpdate = forceRefresh || localPlants.isEmpty() || 
            ApiCacheManager.shouldUpdate(
                cacheKey = CACHE_KEY,
                expiryMillis = 24 * 60 * 60 * 1000L  // 24小时
            )
        
        Log.d(TAG, "shouldUpdate=$shouldUpdate, forceRefresh=$forceRefresh, localPlants.isEmpty=${localPlants.isEmpty()}")
        
        if (!shouldUpdate && localPlants.isNotEmpty()) {
            Log.d(TAG, "植物图鉴缓存有效，跳过网络请求")
            return
        }
        
        // 3. 从网络获取最新数据
        try {
            Log.d(TAG, "正在从网络获取植物图鉴...")
            val response = gardenService.getPlants()
            Log.d(TAG, "植物图鉴网络响应: code=${response.code}, data.size=${response.data?.size}")
            
            if (response.code == 200 && response.data != null) {
                // 转换为实体列表
                val entities = response.data.map { plant ->
                    Log.d(TAG, "植物 ${plant.name} scale=${plant.scale} width=${plant.width}")
                    PlantDictEntity(
                        plantId = plant.id,
                        plantName = plant.name,
                        maxGrowth = 100,
                        resourceCode = plant.resourceCode,
                        description = plant.description,
                        width = plant.width,
                        purifyRange = plant.purifyRange,
                        dropWeight = plant.dropWeight,
                        rarity = plant.rarity,
                        imageUrl = plant.imageUrl,
                        scale = plant.scale
                    )
                }
                
                // 同步到本地数据库（覆盖旧数据）
                plantDictDao.insertPlants(entities)
                
                // 更新内存缓存
                val dict = entities.associateBy { it.plantId }
                _plantDict.value = dict
                Log.d(TAG, "植物图鉴网络同步成功: ${dict.size} 种植物, imageUrl数量: ${entities.count { !it.imageUrl.isNullOrBlank() }}")
                
                // 更新缓存元数据
                ApiCacheManager.updateCacheMeta(CACHE_KEY)
                
                // 预加载植物图片到缓存
                preloadPlantImages(entities)
            } else {
                Log.w(TAG, "植物图鉴网络响应失败: code=${response.code}, message=${response.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "加载植物图鉴失败，使用本地缓存", e)
            // 网络失败时，如果本地有数据则继续使用
            if (localPlants.isEmpty()) {
                _operationMessage.value = "加载植物图鉴失败: ${e.message}"
            }
        }
    }
    
    /**
     * 预加载植物图片到内存和磁盘缓存
     */
    private suspend fun preloadPlantImages(plants: List<PlantDictEntity>) {
        val imageUrls = plants.mapNotNull { it.imageUrl }
        if (imageUrls.isEmpty()) {
            Log.d(TAG, "没有需要预加载的植物图片")
            return
        }
        
        Log.d(TAG, "开始预加载 ${imageUrls.size} 张植物图片...")
        PlantImageLoader.preloadImages(getApplication(), imageUrls)
    }

    /**
     * 更新可种植区域
     * 可种植区域 = 现有空地块(无植物) + 已有植物周围的空白区域
     */
    private fun updatePlantableTiles(tiles: List<GardenTileDto>) {
        // 已有植物的地块
        val planted = tiles.filter { it.plantId != null }.map { it.x to it.y }.toSet()
        // 所有已有地块（包括空地块）
        val existingTiles = tiles.map { it.x to it.y }.toSet()
        
        // 如果没有任何地块，默认只有 (0, 0) 可种植
        if (existingTiles.isEmpty()) {
            _plantableTiles.value = setOf(0 to 0)
            return
        }
        
        // 可种植区域 = 空地块 + 已有植物周围的空白区域
        val plantable = mutableSetOf<Pair<Int, Int>>()
        
        // 1. 添加所有空地块（有记录但无植物）
        tiles.filter { it.plantId == null }.forEach { tile ->
            plantable.add(tile.x to tile.y)
        }
        
        // 2. 添加已有植物周围的空白区域
        planted.forEach { (x, y) ->
            listOf(x + 1 to y, x - 1 to y, x to y + 1, x to y - 1).forEach { pos ->
                // 只添加不在现有地块中的位置
                if (pos !in existingTiles) {
                    plantable.add(pos)
                }
            }
        }
        
        // 如果还没有可种植区域，添加已有地块周围的位置
        if (plantable.isEmpty()) {
            existingTiles.forEach { (x, y) ->
                listOf(x + 1 to y, x - 1 to y, x to y + 1, x to y - 1).forEach { pos ->
                    if (pos !in existingTiles) {
                        plantable.add(pos)
                    }
                }
            }
        }
        
        // 最终确保没有已种植植物的区域
        _plantableTiles.value = plantable - planted
        
        Log.d(TAG, "可种植区域更新: ${_plantableTiles.value.size} 个位置")
    }

    /**
     * 种植植物
     */
    fun plant(x: Int, y: Int, plantId: Int, bagRecordId: String? = null) {
        viewModelScope.launch {
            val userId = _currentUserId.value
            if (userId == null) {
                Log.e(TAG, "种植失败: 用户未登录")
                _operationMessage.value = "用户未登录"
                return@launch
            }

            if (!networkMonitor.isConnected) {
                Log.e(TAG, "种植失败: 网络不可用")
                _operationMessage.value = "网络不可用，无法种植"
                return@launch
            }

            _isLoading.value = true
            Log.d(TAG, "开始种植: userId=$userId, x=$x, y=$y, plantId=$plantId, bagRecordId=$bagRecordId")
            
            try {
                val request = PlantRequest(x, y, plantId, bagRecordId)
                val response = gardenService.plant(userId, request)

                Log.d(TAG, "种植响应: code=${response.code}, message=${response.message}")
                
                if (response.code == 200) {
                    _operationMessage.value = "种植成功！"
                    SoundManager.playPlant() // 🎵 种植音效
                    Log.d(TAG, "种植成功，刷新花园数据")
                    loadGardenFromCloud() // 刷新数据
                    // 同步光流（种植可能消耗光流，需要更新首页和我的页面）
                    TimeFluxSyncService.getInstance(getApplication()).triggerManualSync()
                } else {
                    _operationMessage.value = "种植失败: ${response.message}"
                    Log.w(TAG, "种植失败: ${response.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "种植失败", e)
                _operationMessage.value = "网络错误: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 充能
     */
    fun charge(tileId: Long) {
        viewModelScope.launch {
            val userId = _currentUserId.value ?: return@launch

            if (!networkMonitor.isConnected) {
                _operationMessage.value = "网络不可用"
                return@launch
            }

            try {
                val response = gardenService.charge(userId, tileId)
                if (response.code == 200) {
                    _operationMessage.value = "充能成功！"
                    SoundManager.playCharge() // 🎵 充能音效
                    loadGardenFromCloud()
                } else {
                    _operationMessage.value = "充能失败: ${response.message}"
                }
            } catch (e: Exception) {
                _operationMessage.value = "网络错误: ${e.message}"
            }
        }
    }

    /**
     * 收获
     */
    fun harvest(tileId: Long) {
        viewModelScope.launch {
            val userId = _currentUserId.value ?: return@launch

            if (!networkMonitor.isConnected) {
                _operationMessage.value = "网络不可用"
                return@launch
            }

            try {
                val response = gardenService.harvest(userId, tileId)
                if (response.code == 200) {
                    _operationMessage.value = "收获成功！"
                    loadGardenFromCloud()
                    // 同步光流（收获可能获得光流奖励，需要更新首页和我的页面）
                    TimeFluxSyncService.getInstance(getApplication()).triggerManualSync()
                } else {
                    _operationMessage.value = "收获失败: ${response.message}"
                }
            } catch (e: Exception) {
                _operationMessage.value = "网络错误: ${e.message}"
            }
        }
    }

    /**
     * 计算花园活力状态
     */
    private suspend fun computeGardenVitality(userId: Long) {
        withContext(Dispatchers.IO) {
            try {
                val allRecords = focusRecordDao.getAllRecords(userId)
                val lastRecord = allRecords.maxByOrNull { it.startTime }
                val lastFocusTimeMs = lastRecord?.startTime ?: 0L

                val currentTimeMs = System.currentTimeMillis()
                val state = GardenVitalityState.fromTimeDiff(lastFocusTimeMs, currentTimeMs)

                val hoursSinceLastFocus = if (lastFocusTimeMs > 0) {
                    (currentTimeMs - lastFocusTimeMs) / (1000f * 60f * 60f)
                } else {
                    Float.MAX_VALUE
                }

                _gardenVitality.value = GardenVitality(
                    state = state,
                    lastFocusTimeMs = lastFocusTimeMs,
                    hoursSinceLastFocus = hoursSinceLastFocus,
                    hasFocusRecord = lastRecord != null
                )
            } catch (e: Exception) {
                Log.e(TAG, "计算花园活力状态失败", e)
                _gardenVitality.value = GardenVitality.EMPTY
            }
        }
    }

    /**
     * 刷新花园活力状态
     */
    fun refreshVitality() {
        viewModelScope.launch {
            _currentUserId.value?.let { userId ->
                computeGardenVitality(userId)
            }
        }
    }

    /**
     * 进入种植模式
     */
    fun enterPlacementMode(bagRecordId: String, plantId: Int, plantName: String) {
        _placementMode.value = PlacementState(bagRecordId, plantId, plantName)
        _operationMessage.value = "请点击网格选择种植位置"
    }

    /**
     * 确认种植位置
     */
    fun confirmPlacement(x: Int, y: Int) {
        val state = _placementMode.value ?: return
        plant(x, y, state.plantId, state.bagRecordId)
        _placementMode.value = null
    }

    /**
     * 取消种植模式
     */
    fun cancelPlacement() {
        _placementMode.value = null
        _operationMessage.value = "已取消种植"
    }

    /**
     * 清除操作消息
     */
    fun clearOperationMessage() {
        _operationMessage.value = null
    }

    /**
     * 🟢 [DEFERRED RENDERING] 设置网格就绪状态
     * 
     * 由 UI 层在 LaunchedEffect 中调用，延迟触发完整渲染
     * 解决首帧卡顿问题
     */
    fun setGridReady(ready: Boolean) {
        _isGridReady.value = ready
        Log.d(TAG, "网格渲染状态: $ready")
    }

    /**
     * 重试网络连接
     */
    fun retryConnection() {
        viewModelScope.launch {
            Log.d(TAG, "手动触发网络重试...")
            val connected = networkMonitor.checkConnection()
            if (connected && _currentUserId.value != null) {
                loadGardenFromCloud()
                Log.d(TAG, "网络重试成功，已重新加载花园数据")
            }
        }
    }

    /**
     * 检查是否可以访问花园
     */
    fun canAccessGarden(): Boolean {
        return networkMonitor.isConnected && _currentUserId.value != null
    }

    /**
     * 获取植物渲染信息
     */
    data class PlantRenderInfo(
        val width: Int,
        val resourceCode: String
    )

    fun getPlantRenderInfo(plant: LegacyPlant): PlantRenderInfo? {
        val dict = _plantDict.value[plant.plantId] ?: return null
        return PlantRenderInfo(
            width = dict.width,
            resourceCode = dict.resourceCode
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // 排行榜功能
    // ═══════════════════════════════════════════════════════════════

    // 排行榜数据
    private val _leaderboard = MutableStateFlow<List<LeaderboardEntryDto>>(emptyList())
    val leaderboard: StateFlow<List<LeaderboardEntryDto>> = _leaderboard.asStateFlow()

    // 排行榜加载状态
    private val _isLoadingLeaderboard = MutableStateFlow(false)
    val isLoadingLeaderboard: StateFlow<Boolean> = _isLoadingLeaderboard.asStateFlow()

        /**

         * 加载好友排行榜数据

         */

        fun loadLeaderboard(limit: Int = 20) {

            val userId = _currentUserId.value ?: return

            

            viewModelScope.launch {

                if (!networkMonitor.isConnected) {

                    _operationMessage.value = "网络不可用"

                    return@launch

                }

    

                _isLoadingLeaderboard.value = true

                try {

                    val response = gardenService.getLeaderboard(userId, limit)

                    if (response.code == 200 && response.data != null) {

                        _leaderboard.value = response.data

                        Log.d(TAG, "好友排行榜加载成功: ${response.data.size}条记录")

                    } else {

                        _operationMessage.value = response.message ?: "加载排行榜失败"

                    }

                } catch (e: Exception) {

                    Log.e(TAG, "加载排行榜失败", e)

                    _operationMessage.value = "加载排行榜失败: ${e.message}"

                } finally {

                    _isLoadingLeaderboard.value = false

                }

            }

        }

    }

// Flow 扩展函数
private suspend fun <T> kotlinx.coroutines.flow.Flow<T?>.firstOrNull(): T? {
    var result: T? = null
    collect {
        result = it
        return@collect
    }
    return result
}