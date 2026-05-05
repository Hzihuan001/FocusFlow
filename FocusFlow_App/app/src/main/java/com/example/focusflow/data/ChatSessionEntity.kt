package com.example.focusflow.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * AI 聊天会话实体
 * 
 * 用于管理多个独立的对话会话，类似 ChatGPT 的会话列表
 */
@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val sessionId: Long = 0,
    
    // 会话标题（自动生成或用户自定义）
    val title: String,
    
    // 创建时间
    val createdAt: Long = System.currentTimeMillis(),
    
    // 最后更新时间（用于排序）
    val updatedAt: Long = System.currentTimeMillis(),
    
    // 消息数量
    val messageCount: Int = 0,
    
    // 是否置顶
    val isPinned: Boolean = false,
    
    // 最后一条消息预览（列表显示用，截取前50字符）
    val lastMessage: String? = null,
    
    // 系统提示词（自定义 AI 角色，如"你是一个专业的编程助手"）
    val systemPrompt: String? = null,
    
    // 用户 ID（多账号支持，null 表示当前登录用户）
    val userId: Long? = null,
    
    // 软删除标记（支持恢复误删）
    val isDeleted: Boolean = false
)