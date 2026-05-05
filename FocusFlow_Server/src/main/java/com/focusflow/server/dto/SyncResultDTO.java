package com.focusflow.server.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 单条记录同步结果 DTO
 */
@Data
public class SyncResultDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 记录ID
     */
    private String recordId;

    /**
     * 是否同步成功
     */
    private Boolean success;

    /**
     * 失败原因（success=false时有值）
     */
    private String message;

    /**
     * 获得的光流奖励（success=true时有值）
     */
    private Integer rewardFlux;

    public static SyncResultDTO success(String recordId, Integer rewardFlux) {
        SyncResultDTO dto = new SyncResultDTO();
        dto.setRecordId(recordId);
        dto.setSuccess(true);
        dto.setRewardFlux(rewardFlux);
        dto.setMessage("同步成功");
        return dto;
    }

    public static SyncResultDTO fail(String recordId, String message) {
        SyncResultDTO dto = new SyncResultDTO();
        dto.setRecordId(recordId);
        dto.setSuccess(false);
        dto.setMessage(message);
        return dto;
    }
}
