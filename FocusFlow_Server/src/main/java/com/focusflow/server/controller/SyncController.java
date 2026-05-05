package com.focusflow.server.controller;

import com.focusflow.server.common.Result;
import com.focusflow.server.dto.BatchSyncRequest;
import com.focusflow.server.dto.FocusRecordSyncDTO;
import com.focusflow.server.dto.SyncResultDTO;
import com.focusflow.server.service.FocusRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * 同步控制器
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * 【API 列表】
 * POST /api/sync/focus-records/batch - 批量同步专注记录
 *
 * 【核心功能】
 * 1. 接收 Android 端离线产生的专注记录
 * 2. 验证每条记录的防篡改签名
 * 3. 验签通过后落库并增加光流
 * 4. 返回每条记录的同步结果
 *
 * 【安全机制】
 * 签名算法：SHA-256(recordId || userId || durationMinutes || startTime || SALT)
 * 防止用户通过 SQLite 修改器伪造专注时长
 */
@Slf4j
@RestController
@RequestMapping("/sync")
@RequiredArgsConstructor
public class SyncController {

    private final FocusRecordService focusRecordService;

    /**
     * 批量同步专注记录
     *
     * 【业务流程】
     * 1. 接收 List<FocusRecordSyncDTO> 格式的记录列表
     * 2. 遍历每条记录，验证签名
     * 3. 验签通过：落库 + 增加光流
     * 4. 验签失败：记录失败原因
     * 5. 返回每条记录的同步结果
     *
     * @param request 批量同步请求
     * @return 同步结果列表
     */
    @PostMapping("/focus-records/batch")
    public Result<List<SyncResultDTO>> batchSyncFocusRecords(@Valid @RequestBody BatchSyncRequest request) {
        List<FocusRecordSyncDTO> records = request.getRecords();
        
        log.info("批量同步专注记录: count={}", records != null ? records.size() : 0);
        
        if (records == null || records.isEmpty()) {
            return Result.badRequest("记录列表不能为空");
        }

        List<SyncResultDTO> results = focusRecordService.batchSync(records);
        
        // 统计成功/失败数量
        long successCount = results.stream().filter(SyncResultDTO::getSuccess).count();
        log.info("批量同步完成: 总数={}, 成功={}, 失败={}", 
                results.size(), successCount, results.size() - successCount);

        return Result.success(results);
    }
}