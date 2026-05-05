package com.focusflow.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.focusflow.server.common.Result;
import com.focusflow.server.entity.FocusRecord;
import com.focusflow.server.mapper.FocusRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * 专注记录管理控制器
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * 【API 列表】
 * GET  /admin/focus/records   - 分页获取专注记录列表
 * GET  /admin/focus/stats     - 获取专注统计信息
 */
@Slf4j
@RestController
@RequestMapping("/admin/focus")
@RequiredArgsConstructor
public class AdminFocusController {

    private final FocusRecordMapper focusRecordMapper;

    /**
     * 分页获取专注记录列表（支持高级筛选）
     */
    @GetMapping("/records")
    public Result<Map<String, Object>> getFocusRecordList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long startDate,
            @RequestParam(required = false) Long endDate,
            @RequestParam(required = false) Integer minDuration,
            @RequestParam(required = false) Integer maxDuration,
            @RequestParam(required = false) String taskName) {
        log.info("获取专注记录列表: page={}, size={}, userId={}, startDate={}, endDate={}, minDuration={}, maxDuration={}, taskName={}", 
                 page, size, userId, startDate, endDate, minDuration, maxDuration, taskName);
        
        LambdaQueryWrapper<FocusRecord> wrapper = new LambdaQueryWrapper<>();
        
        // 用户ID过滤
        if (userId != null) {
            wrapper.eq(FocusRecord::getUserId, userId);
        }
        
        // 日期范围过滤
        if (startDate != null) {
            wrapper.ge(FocusRecord::getStartTime, startDate);
        }
        if (endDate != null) {
            wrapper.le(FocusRecord::getStartTime, endDate);
        }
        
        // 时长范围过滤
        if (minDuration != null) {
            wrapper.ge(FocusRecord::getDurationMinutes, minDuration);
        }
        if (maxDuration != null) {
            wrapper.le(FocusRecord::getDurationMinutes, maxDuration);
        }
        
        // 任务名称模糊搜索
        if (taskName != null && !taskName.trim().isEmpty()) {
            wrapper.like(FocusRecord::getTaskName, taskName.trim());
        }
        
        // 按创建时间倒序
        wrapper.orderByDesc(FocusRecord::getCreatedAt);
        
        Page<FocusRecord> recordPage = focusRecordMapper.selectPage(new Page<>(page, size), wrapper);
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", recordPage.getRecords());
        result.put("total", recordPage.getTotal());
        result.put("page", page);
        result.put("size", size);
        
        return Result.success(result);
    }

    /**
     * 获取专注统计信息
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getFocusStats() {
        log.info("获取专注统计信息");
        
        List<FocusRecord> allRecords = focusRecordMapper.selectList(null);
        
        // 总记录数
        int totalRecords = allRecords.size();
        
        // 总时长（分钟）
        int totalDuration = allRecords.stream()
            .mapToInt(r -> r.getDurationMinutes() != null ? r.getDurationMinutes() : 0)
            .sum();
        
        // 今日统计
        long dayStart = getDayStart();
        LambdaQueryWrapper<FocusRecord> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.ge(FocusRecord::getCreatedAt, dayStart);
        List<FocusRecord> todayRecords = focusRecordMapper.selectList(todayWrapper);
        
        int todayRecordsCount = todayRecords.size();
        int todayDuration = todayRecords.stream()
            .mapToInt(r -> r.getDurationMinutes() != null ? r.getDurationMinutes() : 0)
            .sum();
        
        // 今日活跃用户数
        long todayActiveUsers = todayRecords.stream()
            .map(FocusRecord::getUserId)
            .distinct()
            .count();
        
        // 平均专注时长
        double avgDuration = totalRecords > 0 ? (double) totalDuration / totalRecords : 0;
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRecords", totalRecords);
        stats.put("totalDuration", totalDuration);
        stats.put("todayRecords", todayRecordsCount);
        stats.put("todayDuration", todayDuration);
        stats.put("todayActiveUsers", todayActiveUsers);
        stats.put("avgDuration", Math.round(avgDuration * 10) / 10.0);
        
        return Result.success(stats);
    }

    /**
     * 获取当天开始时间戳
     */
    private long getDayStart() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }
    
    /**
     * 删除单条专注记录
     */
    @DeleteMapping("/{recordId}")
    public Result<Boolean> deleteRecord(@PathVariable Long recordId) {
        log.info("删除专注记录: recordId={}", recordId);
        
        int rows = focusRecordMapper.deleteById(recordId);
        return rows > 0 
            ? Result.success("删除成功", true)
            : Result.error("记录不存在");
    }
    
    /**
     * 批量删除专注记录
     */
    @DeleteMapping("/batch")
    public Result<Map<String, Object>> batchDelete(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Long> recordIds = (List<Long>) body.get("recordIds");
        
        if (recordIds == null || recordIds.isEmpty()) {
            return Result.error("记录ID列表不能为空");
        }
        
        log.info("批量删除专注记录: {}", recordIds);
        
        int successCount = 0;
        for (Long recordId : recordIds) {
            int rows = focusRecordMapper.deleteById(recordId);
            if (rows > 0) {
                successCount++;
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("successCount", successCount);
        result.put("totalRequested", recordIds.size());
        
        return Result.success("批量删除成功", result);
    }
}
