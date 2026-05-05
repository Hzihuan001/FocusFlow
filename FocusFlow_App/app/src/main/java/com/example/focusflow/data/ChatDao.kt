package com.example.focusflow.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    
    /**
     * 获取指定会话的所有消息
     */
    @Query("SELECT * FROM chat_history WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesBySession(sessionId: Long): Flow<List<ChatMessageEntity>>

    /**
     * 一次性获取指定会话的所有消息（非 Flow）
     */
    @Query("SELECT * FROM chat_history WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getMessagesBySessionOnce(sessionId: Long): List<ChatMessageEntity>

    /**
     * 获取所有消息（兼容旧代码）
     */
    @Query("SELECT * FROM chat_history ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessageEntity>>

    /**
     * 一次性获取所有消息（非 Flow，用于上下文构建）
     */
    @Query("SELECT * FROM chat_history ORDER BY timestamp ASC")
    suspend fun getAllMessagesOnce(): List<ChatMessageEntity>

    /**
     * 插入消息
     */
    @Insert
    suspend fun insertMessage(message: ChatMessageEntity)

    /**
     * 删除指定会话的所有消息
     */
    @Query("DELETE FROM chat_history WHERE sessionId = :sessionId")
    suspend fun clearSession(sessionId: Long)

    /**
     * 删除所有消息
     */
    @Query("DELETE FROM chat_history")
    suspend fun clearHistory()
    
    /**
     * 获取指定会话的消息数量
     */
    @Query("SELECT COUNT(*) FROM chat_history WHERE sessionId = :sessionId")
    suspend fun getMessageCount(sessionId: Long): Int
    
    /**
     * 获取会话的第一条用户消息（用于生成标题）
     */
    @Query("SELECT content FROM chat_history WHERE sessionId = :sessionId AND isUser = 1 ORDER BY timestamp ASC LIMIT 1")
    suspend fun getFirstUserMessage(sessionId: Long): String?
}
