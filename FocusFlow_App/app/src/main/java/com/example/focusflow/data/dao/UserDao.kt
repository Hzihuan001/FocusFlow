package com.example.focusflow.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.focusflow.data.entity.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * 用户数据访问对象
 */
@Dao
interface UserDao {
    
    /**
     * 插入或更新用户
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)
    
    /**
     * 更新用户
     */
    @Update
    suspend fun update(user: UserEntity)
    
    /**
     * 获取当前用户 (假设单用户场景，取第一条记录)
     */
    @Query("SELECT * FROM biz_user LIMIT 1")
    suspend fun getCurrentUser(): UserEntity?
    
    /**
     * 获取当前用户 (Flow 版本，用于响应式 UI)
     */
    @Query("SELECT * FROM biz_user LIMIT 1")
    fun getCurrentUserFlow(): Flow<UserEntity?>

    /**
     * 根据 id 获取用户 (Flow 版本)
     */
    @Query("SELECT * FROM biz_user WHERE id = :userId")
    fun getUserFlow(userId: Long): Flow<UserEntity?>
    
    @Query("SELECT * FROM biz_user WHERE id = :userId")
    suspend fun getUserById(userId: Long): UserEntity?
    
    @Query("UPDATE biz_user SET timeFlux = timeFlux + :amount WHERE id = :userId")
    suspend fun updateTimeFlux(userId: Long, amount: Int)
    
    @Query("UPDATE biz_user SET timeFlux = :newAmount WHERE id = :userId")
    suspend fun setTimeFlux(userId: Long, newAmount: Int)
    
    @Query("SELECT timeFlux FROM biz_user WHERE id = :userId")
    suspend fun getTimeFlux(userId: Long): Int?
    
    @Query("UPDATE biz_user SET nickname = :nickname WHERE id = :userId")
    suspend fun updateNickname(userId: Long, nickname: String)
    
    @Query("UPDATE biz_user SET avatarId = :avatarId WHERE id = :userId")
    suspend fun updateAvatar(userId: Long, avatarId: Int)
    
    /**
     * 删除所有用户 (用于测试或重置)
     */
    @Query("DELETE FROM biz_user")
    suspend fun deleteAllUsers()
}
