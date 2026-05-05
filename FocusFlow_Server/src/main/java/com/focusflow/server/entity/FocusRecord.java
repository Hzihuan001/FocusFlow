package com.focusflow.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * 专注记录实体类
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * 【核心设计】
 * recordId 使用 UUID 字符串，由 Android 端生成，确保端云主键一致。
 * 这是实现离线优先架构的关键——断网时 Android 端可直接生成唯一 ID，
 * 无需等待服务器返回自增 ID。
 */
@Data
@TableName("biz_focus_record")
public class FocusRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 专注记录ID（UUID，由Android端生成）
     * 
     * 【设计亮点】
     * 传统方案：使用自增 ID，需要先请求服务器获取 ID
     * 本方案：UUID 本地生成，断网也能落库，网络恢复后直接同步
     */
    @TableId(value = "record_id", type = IdType.INPUT)
    private String recordId;

    /**
     * 用户ID（关联biz_user）
     */
    private Long userId;

    /**
     * 任务名称（如"写代码"）
     */
    private String taskName;

    /**
     * 专注时长（分钟）
     */
    private Integer durationMinutes;

    /**
     * 专注开始时间戳（毫秒）
     */
    private Long startTime;

    /**
     * 防篡改签名（SHA-256）
     * 
     * 【安全机制】
     * 签名算法：SHA-256(recordId || userId || durationMinutes || startTime || SALT)
     * 服务端重新计算签名进行验证，防止用户伪造时长
     */
    private String signature;

    /**
     * 同步到云端的时间戳（毫秒）
     */
    private Long syncTime;

    /**
     * 记录创建时间戳
     */
    private Long createdAt;

    /**
     * 逻辑删除标记
     */
    @TableLogic
    private Integer deleted;
}
