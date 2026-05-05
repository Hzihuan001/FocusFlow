package com.example.focusflow.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.focusflow.data.entity.PlantDictEntity
import kotlinx.coroutines.flow.Flow

/**
 * 植物字典数据访问对象
 */
@Dao
interface PlantDictDao {
    
    /**
     * 插入植物字典条目
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlant(plant: PlantDictEntity)
    
    /**
     * 批量插入植物字典条目
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlants(plants: List<PlantDictEntity>)
    
    /**
     * 获取所有植物字典条目
     */
    @Query("SELECT * FROM app_plant_dict ORDER BY plantId ASC")
    suspend fun getAllPlants(): List<PlantDictEntity>
    
    /**
     * 获取所有植物字典条目 (Flow 版本)
     */
    @Query("SELECT * FROM app_plant_dict ORDER BY plantId ASC")
    fun getAllPlantsFlow(): Flow<List<PlantDictEntity>>
    
    /**
     * 根据植物 ID 获取植物信息
     */
    @Query("SELECT * FROM app_plant_dict WHERE plantId = :plantId")
    suspend fun getPlantById(plantId: Int): PlantDictEntity?
    
    /**
     * 根据植物 ID 获取植物信息 (Flow 版本)
     */
    @Query("SELECT * FROM app_plant_dict WHERE plantId = :plantId")
    fun getPlantByIdFlow(plantId: Int): Flow<PlantDictEntity?>
    
    /**
     * 根据资源代码获取植物
     */
    @Query("SELECT * FROM app_plant_dict WHERE resourceCode = :resourceCode")
    suspend fun getPlantByResourceCode(resourceCode: String): PlantDictEntity?
    
    /**
     * 根据植物名称搜索
     */
    @Query("SELECT * FROM app_plant_dict WHERE plantName LIKE '%' || :keyword || '%'")
    suspend fun searchPlantsByName(keyword: String): List<PlantDictEntity>
    
    /**
     * 获取植物总数
     */
    @Query("SELECT COUNT(*) FROM app_plant_dict")
    suspend fun getPlantCount(): Int
    
    /**
     * 根据稀有度获取所有植物
     */
    @Query("SELECT * FROM app_plant_dict WHERE rarity = :rarity")
    suspend fun getPlantsByRarity(rarity: Int): List<PlantDictEntity>

    /**
     * 删除所有植物字典条目 (用于测试或重置)
     */
    @Query("DELETE FROM app_plant_dict")
    suspend fun deleteAllPlants()
}
