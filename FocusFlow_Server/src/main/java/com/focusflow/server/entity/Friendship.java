package com.focusflow.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * 好友关系实体类
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * 【业务说明】
 * 存储用户间的好友关系，支持社交系统。
 *
 * 【状态流转】
 * status 0 = 申请中（等待对方同意）
 * status 1 = 已同意（成为好友）
 *
 * 【双向关系设计】
 * A 添加 B 为好友时：
 * - 创建两条记录：user_id=A, friend_id=B 和 user_id=B, friend_id=A
 * - 或采用单向设计：只在同意后创建一条记录
 *
 * 本系统采用单向设计：同意好友申请后创建一条记录，查询时双向查询
 */
@Data
@TableName("biz_friendship")
public class Friendship implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 关系ID（主键，自增）
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 发起申请的用户ID
     */
    private Long userId;

    /**
     * 接收申请的用户ID
     */
    private Long friendId;

    /**
     * 状态（0=申请中，1=已同意）
     */
    private Integer status;

    /**
     * 创建时间戳
     */
    private Long createdAt;

    /**
     * 更新时间戳
     */
    private Long updatedAt;

    /**
     * 逻辑删除标记
     */
    @TableLogic
    private Integer deleted;
}
