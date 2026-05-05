package com.focusflow.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.focusflow.server.common.Result;
import com.focusflow.server.entity.FocusRecord;
import com.focusflow.server.mapper.FocusRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * 专注记录API控制器（APP端）
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * 【API 列表】
 * GET  /api/focus/records/{userId}  - 获取用户专注记录
 * GET  /api/focus/stats/{userId}    - 获取用户专注统计
 */
@Slf4j
@RestController
@RequestMapping("/focus")
@RequiredArgsConstructor
public class FocusRecordController {

    private final FocusRecordMapper focusRecordMapper;

    /**
     * 获取用户专注记录
     * 
     * @param userId 用户ID
     * @param limit 返回记录数量限制（默认100）
     */
    @GetMapping("/records/{userId}")
    public Result<List<FocusRecord>> getUserFocusRecords(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "100") int limit) {
        log.info("获取用户专注记录: userId={}, limit={}", userId, limit);
        
        LambdaQueryWrapper<FocusRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FocusRecord::getUserId, userId)
               .orderByDesc(FocusRecord::getStartTime)
               .last("LIMIT " + limit);
        
        List<FocusRecord> records = focusRecordMapper.selectList(wrapper);
        return Result.success(records);
    }

    /**
     * 获取用户专注统计
     */
    @GetMapping("/stats/{userId}")
    public Result<Map<String, Object>> getUserFocusStats(@PathVariable Long userId) {
        log.info("获取用户专注统计: userId={}", userId);
        
        LambdaQueryWrapper<FocusRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FocusRecord::getUserId, userId);
        
        List<FocusRecord> records = focusRecordMapper.selectList(wrapper);
        
        int totalRecords = records.size();
        int totalMinutes = records.stream()
            .mapToInt(r -> r.getDurationMinutes() != null ? r.getDurationMinutes() : 0)
            .sum();
        
        // 计算今日专注
        long dayStart = getDayStart();
        LambdaQueryWrapper<FocusRecord> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.eq(FocusRecord::getUserId, userId)
                    .ge(FocusRecord::getStartTime, dayStart);
        List<FocusRecord> todayRecords = focusRecordMapper.selectList(todayWrapper);
        
        int todayMinutes = todayRecords.stream()
            .mapToInt(r -> r.getDurationMinutes() != null ? r.getDurationMinutes() : 0)
            .sum();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRecords", totalRecords);
        stats.put("totalMinutes", totalMinutes);
        stats.put("todayRecords", todayRecords.size());
        stats.put("todayMinutes", todayMinutes);
        
        return Result.success(stats);
    }

    /**
     * 获取当天开始时间戳
     */
    private long getDayStart() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }
}
