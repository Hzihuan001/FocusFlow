package com.example.focusflow.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * AI 聊天会话 DAO
 */
@Dao
interface ChatSessionDao {
    
    /**
     * 获取所有未删除的会话（按置顶和更新时间降序）
     */
    @Query("SELECT * FROM chat_sessions WHERE isDeleted = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllSessions(): Flow<List<ChatSessionEntity>>
    
    /**
     * 一次性获取所有未删除的会话（非 Flow）
     */
    @Query("SELECT * FROM chat_sessions WHERE isDeleted = 0 ORDER BY isPinned DESC, updatedAt DESC")
    suspend fun getAllSessionsOnce(): List<ChatSessionEntity>
    
    /**
     * 获取指定用户的会话列表
     */
    @Query("SELECT * FROM chat_sessions WHERE isDeleted = 0 AND (userId IS NULL OR userId = :userId) ORDER BY isPinned DESC, updatedAt DESC")
    fun getSessionsByUser(userId: Long): Flow<List<ChatSessionEntity>>
    
    /**
     * 根据 ID 获取会话
     */
    @Query("SELECT * FROM chat_sessions WHERE sessionId = :sessionId")
    suspend fun getSessionById(sessionId: Long): ChatSessionEntity?
    
    /**
     * 创建新会话
     */
    @Insert
    suspend fun insertSession(session: ChatSessionEntity): Long
    
    /**
     * 更新会话
     */
    @Update
    suspend fun updateSession(session: ChatSessionEntity)
    
    /**
     * 删除会话（软删除，可恢复）
     */
    @Delete
    suspend fun deleteSession(session: ChatSessionEntity)
    
    /**
     * 软删除会话
     */
    @Query("UPDATE chat_sessions SET isDeleted = 1, updatedAt = :updatedAt WHERE sessionId = :sessionId")
    suspend fun softDeleteSession(sessionId: Long, updatedAt: Long = System.currentTimeMillis())
    
    /**
     * 恢复已删除的会话
     */
    @Query("UPDATE chat_sessions SET isDeleted = 0, updatedAt = :updatedAt WHERE sessionId = :sessionId")
    suspend fun restoreSession(sessionId: Long, updatedAt: Long = System.currentTimeMillis())
    
    /**
     * 获取已删除的会话（回收站）
     */
    @Query("SELECT * FROM chat_sessions WHERE isDeleted = 1 ORDER BY updatedAt DESC")
    fun getDeletedSessions(): Flow<List<ChatSessionEntity>>
    
    /**
     * 永久删除已软删除的会话
     */
    @Query("DELETE FROM chat_sessions WHERE isDeleted = 1 AND sessionId = :sessionId")
    suspend fun permanentDelete(sessionId: Long)
    
    /**
     * 清空回收站
     */
    @Query("DELETE FROM chat_sessions WHERE isDeleted = 1")
    suspend fun emptyTrash()
    
    /**
     * 更新会话标题
     */
    @Query("UPDATE chat_sessions SET title = :title, updatedAt = :updatedAt WHERE sessionId = :sessionId")
    suspend fun updateTitle(sessionId: Long, title: String, updatedAt: Long = System.currentTimeMillis())
    
    /**
     * 更新会话消息数量和最后消息预览
     */
    @Query("UPDATE chat_sessions SET messageCount = :count, lastMessage = :lastMessage, updatedAt = :updatedAt WHERE sessionId = :sessionId")
    suspend fun updateMessageInfo(sessionId: Long, count: Int, lastMessage: String?, updatedAt: Long = System.currentTimeMillis())
    
    /**
     * 更新会话消息数量
     */
    @Query("UPDATE chat_sessions SET messageCount = :count, updatedAt = :updatedAt WHERE sessionId = :sessionId")
    suspend fun updateMessageCount(sessionId: Long, count: Int, updatedAt: Long = System.currentTimeMillis())
    
    /**
     * 切换置顶状态
     */
    @Query("UPDATE chat_sessions SET isPinned = NOT isPinned, updatedAt = :updatedAt WHERE sessionId = :sessionId")
    suspend fun togglePin(sessionId: Long, updatedAt: Long = System.currentTimeMillis())
    
    /**
     * 更新系统提示词
     */
    @Query("UPDATE chat_sessions SET systemPrompt = :systemPrompt, updatedAt = :updatedAt WHERE sessionId = :sessionId")
    suspend fun updateSystemPrompt(sessionId: Long, systemPrompt: String?, updatedAt: Long = System.currentTimeMillis())
    
    /**
     * 获取会话数量
     */
    @Query("SELECT COUNT(*) FROM chat_sessions WHERE isDeleted = 0")
    suspend fun getSessionCount(): Int
}