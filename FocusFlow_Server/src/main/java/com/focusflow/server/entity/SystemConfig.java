package com.focusflow.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * 系统配置实体类
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * 【业务说明】
 * 存储系统运行时配置参数，支持后台管理平台动态调整。
 * 采用 key-value 结构，方便扩展新配置项。
 */
@Data
@TableName("sys_config")
public class SystemConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 配置ID（主键，自增）
     */
    @TableId(value = "config_id", type = IdType.AUTO)
    private Long configId;

    /**
     * 配置键（唯一）
     */
    private String configKey;

    /**
     * 配置值
     */
    private String configValue;

    /**
     * 配置描述
     */
    private String description;

    /**
     * 配置分组（用于前台分组展示）
     */
    private String configGroup;

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

    // ════════════════════════════════════════════════════════════════════════
    // 配置键常量
    // ════════════════════════════════════════════════════════════════════════

    /** 连续专注奖励 - 3天 */
    public static final String STREAK_BONUS_3 = "streak.bonus.3";
    /** 连续专注奖励 - 5天 */
    public static final String STREAK_BONUS_5 = "streak.bonus.5";
    /** 连续专注奖励 - 7天 */
    public static final String STREAK_BONUS_7 = "streak.bonus.7";
    /** 连续专注奖励 - 14天 */
    public static final String STREAK_BONUS_14 = "streak.bonus.14";
    /** 连续专注奖励 - 21天 */
    public static final String STREAK_BONUS_21 = "streak.bonus.21";
    /** 连续专注奖励 - 30天 */
    public static final String STREAK_BONUS_30 = "streak.bonus.30";

    /** 专注 - 每分钟奖励光流 */
    public static final String FOCUS_REWARD_PER_MINUTE = "focus.reward.per.minute";

    /** 种子 - 获得种子最低专注时长 */
    public static final String FOCUS_DROP_MIN_MINUTES = "focus.drop.min.minutes";
    /** 种子 - 获得种子基础概率 */
    public static final String FOCUS_DROP_BASE_RATE = "focus.drop.base.rate";

    /** 充能 - 消耗光流（一键点亮所有植物） */
    public static final String CHARGE_COST = "charge.cost";
}
