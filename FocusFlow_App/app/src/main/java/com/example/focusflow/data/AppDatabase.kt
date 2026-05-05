package com.example.focusflow.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.focusflow.data.dao.*
import com.example.focusflow.data.entity.*

/**
 * FocusFlow 应用数据库（精简版）
 *
 * 本地数据库仅保留核心离线功能所需的表：
 * 1. FocusRecordEntity - 专注记录（离线优先架构核心）
 * 2. ChatMessageEntity - AI对话历史（会话管理）
 * 3. ChatSessionEntity - AI会话列表
 * 4. UserEntity - 用户信息（本地缓存）
 * 5. PlantDictEntity - 植物图鉴（本地缓存，云端同步）
 *
 * 其他数据通过API实时获取。
 */
@Database(
    entities = [
        // AI 聊天会话
        ChatSessionEntity::class,
        // AI 聊天消息
        ChatMessageEntity::class,
        // 专注记录（离线优先架构核心）
        FocusRecordEntity::class,
        // 用户信息（本地缓存）
        UserEntity::class,
        // 植物图鉴（本地缓存，云端同步）
        PlantDictEntity::class
    ],
    version = 15,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    // AI 聊天会话 DAO
    abstract fun chatSessionDao(): ChatSessionDao
    
    // AI 聊天消息 DAO
    abstract fun chatDao(): ChatDao
    
    // 专注记录 DAO
    abstract fun focusRecordDao(): FocusRecordDao
    
    // 用户 DAO
    abstract fun userDao(): UserDao
    
    // 植物图鉴 DAO
    abstract fun plantDictDao(): PlantDictDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "focus_flow_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}