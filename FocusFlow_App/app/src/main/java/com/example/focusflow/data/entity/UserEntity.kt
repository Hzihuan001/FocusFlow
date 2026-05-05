package com.example.focusflow.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 用户实体
 * 表名: app_user
 * 存储用户基本信息和核心游戏资产 Time Flux
 */
@Entity(tableName = "biz_user")
data class UserEntity(
    @PrimaryKey
    val id: Long,                    // 主键 ID
    val account: String,             // 账号
    val password: String,            // 密码 (加密存储)
    val nickname: String,            // 昵称
    val avatarId: Int = 1,           // 头像 ID (1=默认)
    val timeFlux: Int = 0,           // 光流资产
    val status: Int = 0              // 状态 (0=正常)
)
