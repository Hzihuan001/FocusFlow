package com.example.focusflow.data.repository

import com.example.focusflow.data.dao.UserDao
import com.example.focusflow.data.entity.UserEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * 用户 Repository
 * 封装用户相关的业务逻辑
 */
class UserRepository(
    private val userDao: UserDao
) {
    
    /**
     * 获取当前用户
     */
    suspend fun getCurrentUser(): UserEntity? {
        return userDao.getCurrentUser()
    }
    
    /**
     * 获取当前用户 (Flow)
     */
    fun getCurrentUserFlow(): Flow<UserEntity?> {
        return userDao.getCurrentUserFlow()
    }

    /**
     * 根据 ID 获取用户 (Flow)
     */
    fun getUserFlow(userId: Long): Flow<UserEntity?> {
        return userDao.getUserFlow(userId)
    }

    /**
     * 根据 ID 获取用户
     */
    suspend fun getUserById(userId: Long): UserEntity? {
        return userDao.getUserById(userId)
    }
    
    /**
     * 创建或更新用户
     */
    suspend fun saveUser(user: UserEntity) {
        userDao.insertUser(user)
    }
    
    suspend fun initializeDefaultUser(): UserEntity {
        val existingUser = userDao.getCurrentUser()
        if (existingUser != null) {
            return existingUser
        }
        
        // 创建默认本地访客用户 (ID 设为 1L)
        val defaultUser = UserEntity(
            id = 1L,
            account = "Guest_01",
            password = "pwd",
            nickname = "专注者",
            avatarId = 1,
            timeFlux = 0,
            status = 0
        )
        
        userDao.insertUser(defaultUser)
        return defaultUser
    }
    
    /**
     * 更新用户昵称
     */
    suspend fun updateNickname(userId: Long, nickname: String) {
        userDao.updateNickname(userId, nickname)
    }
    
    /**
     * 更新用户头像 ID
     */
    suspend fun updateAvatar(userId: Long, avatarId: Int) {
        userDao.updateAvatar(userId, avatarId)
    }
    
    /**
     * 获取用户的 Time Flux 余额
     */
    suspend fun getTimeFlux(userId: Long): Int {
        return userDao.getTimeFlux(userId) ?: 0
    }
    
    /**
     * 增加 Time Flux
     */
    suspend fun addTimeFlux(userId: Long, amount: Int) {
        userDao.updateTimeFlux(userId, amount)
    }
    
    /**
     * 扣除 Time Flux
     * @return true 如果扣除成功，false 如果余额不足
     */
    suspend fun deductTimeFlux(userId: Long, amount: Int): Boolean {
        val currentBalance = userDao.getTimeFlux(userId) ?: 0
        return if (currentBalance >= amount) {
            userDao.updateTimeFlux(userId, -amount)
            true
        } else {
            false
        }
    }
    
    /**
     * 设置 Time Flux 为指定值（用于同步云端余额）
     */
    suspend fun setTimeFlux(userId: Long, amount: Int) {
        userDao.setTimeFlux(userId, amount)
    }
}
