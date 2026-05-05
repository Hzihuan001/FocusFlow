package com.example.focusflow.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * AI 聊天消息实体
 * 
 * @param sessionId 关联的会话 ID（0 表示旧数据，归属于默认会话）
 */
@Entity(
    tableName = "chat_history",
    foreignKeys = [
        ForeignKey(
            entity = ChatSessionEntity::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    
    // 会话 ID
    val sessionId: Long = 0,
    
    // 消息内容
    val content: String,
    
    // 是否为用户消息
    val isUser: Boolean,
    
    // 时间戳
    val timestamp: Long = System.currentTimeMillis(),
    
    // Token 数量（用于上下文长度控制，可选）
    val tokenCount: Int = 0
)