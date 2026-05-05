package com.focusflow.server.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 批量同步请求 DTO
 */
@Data
public class BatchSyncRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 待同步的专注记录列表
     */
    private java.util.List<FocusRecordSyncDTO> records;
}
