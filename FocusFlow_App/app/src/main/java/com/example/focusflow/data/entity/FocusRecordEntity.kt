package com.example.focusflow.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 专注记录实体
 * 表名: app_focus_record
 * 存储用户的专注会话记录
 *
 * 【设计亮点】
 * recordId 使用 UUID 字符串，由客户端生成，确保：
 * 1. 离线场景下可直接落库，无需等待服务器返回自增ID
 * 2. 端云主键一致，同步时无冲突
 * 3. UUID 全局唯一，防止重放攻击
 */
@Entity(tableName = "app_focus_record")
data class FocusRecordEntity(
    @PrimaryKey
    val recordId: String,        // UUID 字符串 (对应云端 CHAR(36))
    val userId: Long,            // 关联用户 ID (BIGINT)
    val taskName: String,        // 任务名称 (如 "复习数据结构")
    val durationMinutes: Int,    // 专注时长 (分钟)
    val startTime: Long,         // 开始时间戳
    val syncStatus: Int = 0,     // 同步状态 (0=未同步, 1=已同步)
    val signature: String = "",  // 防篡改签名 (SHA-256)
    val rewardSettled: Int = 0   // 奖励结算状态 (0=未结算, 1=已结算) - 离线优先架构核心字段
)