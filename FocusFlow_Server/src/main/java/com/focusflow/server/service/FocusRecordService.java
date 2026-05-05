package com.focusflow.server.service;

import com.focusflow.server.dto.FocusRecordSyncDTO;
import com.focusflow.server.dto.SyncResultDTO;

import java.util.List;

/**
 * 专注记录服务接口
 */
public interface FocusRecordService {

    /**
     * 批量同步专注记录
     *
     * 【核心业务流程】
     * 1. 遍历每条记录
     * 2. 验证签名（防篡改）
     * 3. 验签通过：落库 + 增加用户光流
     * 4. 返回每条记录的同步结果
     *
     * @param records 待同步记录列表
     * @return 同步结果列表
     */
    List<SyncResultDTO> batchSync(List<FocusRecordSyncDTO> records);

    /**
     * 检查记录是否已存在（防重放）
     *
     * @param recordId 记录ID
     * @return 是否存在
     */
    boolean existsByRecordId(String recordId);
}
