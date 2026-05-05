package com.focusflow.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * 用户实体类
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * 【MyBatis-Plus 注解说明】
 * @TableName: 指定表名
 * @TableId: 主键字段，type = IdType.AUTO 表示自增
 * @TableLogic: 逻辑删除字段
 *
 * 【安全设计】
 * password 字段存储的是 SHA-256 哈希后的密码，绝非明文！
 * 哈希算法：SHA-256(password + PASSWORD_SALT)
 */
@Data
@TableName("biz_user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID（主键，自增）
     */
    @TableId(value = "user_id", type = IdType.AUTO)
    private Long userId;

    /**
     * 登录账号（唯一）
     * 用户注册时设置，支持字母、数字、下划线
     */
    private String account;

    /**
     * 密码（SHA-256 哈希值）
     *
     * 【安全说明】
     * 数据库中绝不存储明文密码！
     * 存储格式：SHA-256(原始密码 + PASSWORD_SALT)
     * 即使数据库泄露，攻击者也无法反推原始密码
     */
    private String password;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 预设头像ID
     */
    private Integer avatarId;

    /**
     * 光流余额（核心货币资产）
     * 必须以云端为准，防止本地作弊
     */
    private Integer timeFlux;

    /**
     * 连续专注天数
     * 每日首次专注时+1，中断则归零
     */
    private Integer streakDays;

    /**
     * 最后专注日期（YYYY-MM-DD格式）
     * 用于计算连续天数
     */
    private String lastFocusDate;

    /**
     * 最后专注时间戳（毫秒）
     * 用于判断花园点亮状态
     * - 用户专注完成时更新
     * - 好友充能时更新
     */
    private Long lastFocusTime;

    /**
     * 账号状态（0=正常，1=封禁）
     */
    private Integer status;

    /**
     * 注册时间戳（毫秒）
     */
    private Long createdAt;

    /**
     * 最后更新时间戳（毫秒）
     */
    private Long updatedAt;

    /**
     * 逻辑删除标记（0=未删除，1=已删除）
     */
    @TableLogic
    private Integer deleted;
}