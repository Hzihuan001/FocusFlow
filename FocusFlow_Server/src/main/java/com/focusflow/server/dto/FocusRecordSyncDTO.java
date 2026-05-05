package com.focusflow.server.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 专注记录同步请求 DTO
 *
 * 【安全机制】
 * signature 字段用于防篡改验证，算法：
 * SHA-256(recordId || userId || durationMinutes || startTime || APP_SECRET_SALT)
 */
@Data
public class FocusRecordSyncDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 记录ID（UUID，由Android端生成）
     */
    @NotBlank(message = "记录ID不能为空")
    private String recordId;

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 专注时长（分钟）
     */
    @NotNull(message = "专注时长不能为空")
    @Min(value = 1, message = "专注时长必须大于0")
    private Integer durationMinutes;

    /**
     * 开始时间戳（毫秒）
     */
    @NotNull(message = "开始时间不能为空")
    private Long startTime;

    /**
     * 防篡改签名
     */
    @NotBlank(message = "签名不能为空")
    private String signature;
}
