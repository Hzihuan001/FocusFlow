package com.example.focusflow.data.repository

import android.util.Log
import com.example.focusflow.api.BagService
import com.example.focusflow.api.RetrofitClient
import com.example.focusflow.data.dao.UserDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 背包 Repository (云端版)
 * 
 * 【架构说明】
 * 背包数据完全云端化，本地不再存储。
 * 所有操作直接调用后端 API。
 * 
 * 【API 列表】
 * - GET  /bag/list         获取背包列表
 * - POST /bag/add          添加背包物品
 * - POST /bag/open/{bagId} 开箱（解析种子）
 * - PUT  /bag/status/{bagId} 更新物品状态
 */
class BagRepository(
    private val userDao: UserDao
) {
    companion object {
        private const val TAG = "BagRepository"
    }
    
    private val bagService: BagService = RetrofitClient.bagService
    
    // 背包数据缓存 (用于UI响应式更新)
    private val _bagItemsFlow = MutableStateFlow<List<BagService.BagItemDto>>(emptyList())
    val bagItemsFlow: Flow<List<BagService.BagItemDto>> = _bagItemsFlow.asStateFlow()
    
    /**
     * 从云端获取背包列表
     */
    suspend fun loadBagFromCloud(userId: Long): List<BagService.BagItemDto> {
        return try {
            val response = bagService.getBagList(userId)
            if (response.isSuccessful && response.body()?.code == 200) {
                val items = response.body()?.data ?: emptyList()
                _bagItemsFlow.value = items
                Log.d(TAG, "从云端获取背包成功: ${items.size} 个物品")
                items
            } else {
                Log.w(TAG, "获取背包失败: ${response.body()?.message}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取背包异常: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * 获取当前光流余额
     */
    suspend fun getTimeFlux(userId: Long): Int {
        return userDao.getTimeFlux(userId) ?: 0
    }
    
    /**
     * 更新本地用户光流 (用于UI即时更新)
     */
    suspend fun updateTimeFluxLocal(userId: Long, delta: Int) {
        userDao.updateTimeFlux(userId, delta)
    }

    /**
     * 开箱（解析种子）- 云端版
     * 
     * 业务流程：
     * 1. 调用后端 API /bag/open/{bagId}
     * 2. 后端扣除光流、执行随机抽取、更新背包状态
     * 3. 返回解析结果
     * 
     * @param userId 用户ID
     * @param bagId 背包记录ID (UUID String)
     * @return Result<OpenBoxResult> 解析结果
     */
    suspend fun openBox(userId: Long, bagId: String): Result<OpenBoxResult> {
        return try {
            val response = bagService.openBox(userId, bagId)
            if (response.isSuccessful && response.body()?.isSuccess == true) {
                val data = response.body()?.data
                if (data != null) {
                    // 更新本地用户光流余额
                    data.timeFlux?.let { flux ->
                        // 直接设置光流为服务器返回的最新值
                        userDao.setTimeFlux(userId, flux)
                    }
                    
                    Log.d(TAG, "开箱成功: plantId=${data.plantId}, plantName=${data.plantName}")
                    Result.success(OpenBoxResult(
                        bagId = data.bagId,
                        plantId = data.plantId,
                        plantName = data.plantName ?: "未知植物",
                        rarity = data.rarity ?: 0,
                        remainingTimeFlux = data.timeFlux ?: 0
                    ))
                } else {
                    Result.failure(Exception("开箱响应数据为空"))
                }
            } else {
                val errorMsg = response.body()?.message ?: "开箱失败"
                Log.w(TAG, "开箱失败: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "开箱异常: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * 更新背包物品状态 - 云端版
     * 
     * @param userId 用户ID
     * @param bagId 背包记录ID
     * @param status 新状态 (0=未解析, 1=已解析, 2=已使用)
     */
    suspend fun updateStatus(userId: Long, bagId: String, status: Int): Result<Boolean> {
        return try {
            val response = bagService.updateStatus(userId, bagId, BagService.UpdateStatusRequest(status))
            if (response.isSuccessful && response.body()?.isSuccess == true) {
                Log.d(TAG, "更新背包状态成功: bagId=$bagId, status=$status")
                Result.success(true)
            } else {
                val errorMsg = response.body()?.message ?: "更新失败"
                Log.w(TAG, "更新背包状态失败: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "更新背包状态异常: ${e.message}")
            Result.failure(e)
        }
    }
}

/**
 * 开箱结果
 */
data class OpenBoxResult(
    val bagId: String,
    val plantId: Int,
    val plantName: String,
    val rarity: Int,
    val remainingTimeFlux: Int
)