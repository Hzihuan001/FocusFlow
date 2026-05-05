package com.focusflow.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.focusflow.server.common.Result;
import com.focusflow.server.entity.FocusRecord;
import com.focusflow.server.entity.User;
import com.focusflow.server.mapper.FocusRecordMapper;
import com.focusflow.server.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * 后台统计控制器
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * 【API 列表】
 * GET  /admin/stats           - 获取仪表盘统计数据
 * GET  /admin/focus/trend     - 获取专注趋势
 * GET  /admin/focus/modes     - 获取专注模式分布
 * GET  /admin/focus/hourly    - 获取24小时专注分布
 */
@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminStatsController {

    private final UserMapper userMapper;
    private final FocusRecordMapper focusRecordMapper;

    /**
     * 获取仪表盘统计数据（优化版）
     * 使用 SQL 聚合查询代替全表扫描，提升性能
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getDashboardStats() {
        log.info("获取仪表盘统计数据");
        
        Map<String, Object> stats = new LinkedHashMap<>();
        long dayStart = getDayStart();
        long dayEnd = dayStart + 86400000L;
        
        // ═══════════════════════════════════════════════════════════════
        // 一、用户生态（使用 SQL 聚合）
        // ═══════════════════════════════════════════════════════════════
        long totalUsers = userMapper.countTotal();
        long todayNewUsers = userMapper.countBetween(dayStart, dayEnd);
        long todayDAU = focusRecordMapper.countDistinctUsersBetween(dayStart, dayEnd);
        
        stats.put("totalUsers", totalUsers);
        stats.put("todayNewUsers", todayNewUsers);
        stats.put("todayDAU", todayDAU);
        
        // ═══════════════════════════════════════════════════════════════
        // 二、专注大盘（使用 SQL 聚合）
        // ═══════════════════════════════════════════════════════════════
        int totalDurationMinutes = focusRecordMapper.sumTotalDuration();
        int todayDurationMinutes = focusRecordMapper.sumDurationBetween(dayStart, dayEnd);
        long totalFocusRecords = focusRecordMapper.countTotal();
        long todayFocusRecords = focusRecordMapper.countBetween(dayStart, dayEnd);
        
        stats.put("totalDurationMinutes", totalDurationMinutes);
        stats.put("totalDurationHours", totalDurationMinutes / 60);
        stats.put("todayDurationMinutes", todayDurationMinutes);
        stats.put("todayDurationHours", todayDurationMinutes / 60);
        stats.put("totalFocusRecords", totalFocusRecords);
        stats.put("todayFocusRecords", todayFocusRecords);
        
        // ═══════════════════════════════════════════════════════════════
        // 三、经济系统（使用 SQL 聚合）
        // ═══════════════════════════════════════════════════════════════
        // 全服累计产出光流总额（假设每分钟产出1光流）
        int totalProducedTimeFlux = totalDurationMinutes;
        
        // 当前流通光流总量
        int circulatingTimeFlux = userMapper.sumTimeFlux();
        
        stats.put("totalProducedTimeFlux", totalProducedTimeFlux);
        stats.put("circulatingTimeFlux", circulatingTimeFlux);
        
        return Result.success(stats);
    }

    /**
     * 聚合接口：一次性获取所有仪表盘数据
     * 减少前端并发请求数，提升页面加载性能
     */
    @GetMapping("/stats/all")
    public Result<Map<String, Object>> getDashboardAll(
            @RequestParam(defaultValue = "7") int days) {
        log.info("获取仪表盘聚合数据: days={}", days);
        
        Map<String, Object> result = new LinkedHashMap<>();
        
        // 1. 基础统计数据
        long dayStart = getDayStart();
        long dayEnd = dayStart + 86400000L;
        
        long totalUsers = userMapper.countTotal();
        long todayNewUsers = userMapper.countBetween(dayStart, dayEnd);
        long todayDAU = focusRecordMapper.countDistinctUsersBetween(dayStart, dayEnd);
        int totalDurationMinutes = focusRecordMapper.sumTotalDuration();
        int todayDurationMinutes = focusRecordMapper.sumDurationBetween(dayStart, dayEnd);
        long totalFocusRecords = focusRecordMapper.countTotal();
        long todayFocusRecords = focusRecordMapper.countBetween(dayStart, dayEnd);
        int circulatingTimeFlux = userMapper.sumTimeFlux();
        
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("todayNewUsers", todayNewUsers);
        stats.put("todayDAU", todayDAU);
        stats.put("totalDurationMinutes", totalDurationMinutes);
        stats.put("totalDurationHours", totalDurationMinutes / 60);
        stats.put("todayDurationMinutes", todayDurationMinutes);
        stats.put("todayDurationHours", todayDurationMinutes / 60);
        stats.put("totalFocusRecords", totalFocusRecords);
        stats.put("todayFocusRecords", todayFocusRecords);
        stats.put("totalProducedTimeFlux", totalDurationMinutes);
        stats.put("circulatingTimeFlux", circulatingTimeFlux);
        result.put("stats", stats);
        
        // 2. 专注趋势
        List<Map<String, Object>> trend = new ArrayList<>();
        long todayStart = getDayStart();
        for (int i = days - 1; i >= 0; i--) {
            long dStart = todayStart - i * 86400000L;
            long dEnd = dStart + 86400000L;
            int dayDuration = focusRecordMapper.sumDurationBetween(dStart, dEnd);
            long dayCount = focusRecordMapper.countBetween(dStart, dEnd);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", new java.text.SimpleDateFormat("MM-dd").format(new Date(dStart)));
            item.put("duration", dayDuration);
            item.put("hours", dayDuration / 60.0);
            item.put("count", dayCount);
            trend.add(item);
        }
        result.put("focusTrend", trend);
        
        // 3. 专注模式分布
        List<Map<String, Object>> durationDistribution = focusRecordMapper.getDurationDistribution();
        int pomodoroCount = 0, fiftyTwoCount = 0, immersiveCount = 0, customCount = 0;
        for (Map<String, Object> item : durationDistribution) {
            int duration = ((Number) item.get("duration_minutes")).intValue();
            int count = ((Number) item.get("count")).intValue();
            if (duration > 0 && duration % 90 == 0 && duration <= 270) {
                immersiveCount += count;
            } else if (duration > 0 && duration % 25 == 0 && duration <= 150) {
                pomodoroCount += count;
            } else if (duration == 52 || duration == 104) {
                fiftyTwoCount += count;
            } else {
                customCount += count;
            }
        }
        List<Map<String, Object>> modes = new ArrayList<>();
        modes.add(createModeItem("番茄工作法", pomodoroCount, "#00ff88"));
        modes.add(createModeItem("52/17法则", fiftyTwoCount, "#2196f3"));
        modes.add(createModeItem("沉浸周期", immersiveCount, "#ff6b6b"));
        modes.add(createModeItem("自定义模式", customCount, "#ffd700"));
        result.put("focusModes", modes);
        
        // 4. 24小时分布
        int[] hourCounts = new int[24];
        int[] hourDurations = new int[24];
        List<Map<String, Object>> dbDistribution = focusRecordMapper.getHourlyDistribution();
        for (Map<String, Object> item : dbDistribution) {
            int hour = ((Number) item.get("hour")).intValue();
            int count = ((Number) item.get("count")).intValue();
            int duration = ((Number) item.get("duration")).intValue();
            if (hour >= 0 && hour < 24) {
                hourCounts[hour] = count;
                hourDurations[hour] = duration;
            }
        }
        List<Map<String, Object>> hourly = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("hour", i);
            item.put("label", String.format("%02d:00", i));
            item.put("count", hourCounts[i]);
            item.put("duration", hourDurations[i]);
            hourly.add(item);
        }
        result.put("hourlyDistribution", hourly);
        
        // 5. DAU趋势
        java.util.TimeZone tz = java.util.TimeZone.getTimeZone("Asia/Shanghai");
        java.text.SimpleDateFormat displayFormat = new java.text.SimpleDateFormat("MM-dd");
        displayFormat.setTimeZone(tz);
        List<Map<String, Object>> dauTrend = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            long dStart = todayStart - i * 86400000L;
            long dEnd = dStart + 86400000L;
            long dau = focusRecordMapper.countDistinctUsersBetween(dStart, dEnd);
            long newUsers = userMapper.countBetween(dStart, dEnd);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", displayFormat.format(new Date(dStart)));
            item.put("dau", dau);
            item.put("newUsers", newUsers);
            dauTrend.add(item);
        }
        result.put("dauTrend", dauTrend);
        
        return Result.success(result);
    }

    /**
     * 获取专注趋势数据（近N天）- 优化版
     * 使用 SQL 聚合查询，避免内存计算
     */
    @GetMapping("/focus/trend")
    public Result<List<Map<String, Object>>> getFocusTrend(
            @RequestParam(defaultValue = "7") int days) {
        log.info("获取专注趋势: days={}", days);
        
        List<Map<String, Object>> trend = new ArrayList<>();
        long todayStart = getDayStart();
        
        for (int i = days - 1; i >= 0; i--) {
            long dayStart = todayStart - i * 86400000L;
            long dayEnd = dayStart + 86400000L;
            
            // 使用 SQL 聚合查询
            int dayDuration = focusRecordMapper.sumDurationBetween(dayStart, dayEnd);
            long dayCount = focusRecordMapper.countBetween(dayStart, dayEnd);
            
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", new java.text.SimpleDateFormat("MM-dd").format(new Date(dayStart)));
            item.put("duration", dayDuration);
            item.put("hours", dayDuration / 60.0);
            item.put("count", dayCount);
            trend.add(item);
        }
        
        return Result.success(trend);
    }
    
    /**
     * 获取专注模式分布 - 优化版
     * 根据时长推断模式：
     * - 25分钟的倍数 → 番茄工作法
     * - 52分钟的倍数 → 52/17法则
     * - 90分钟的倍数 → 沉浸周期
     * - 其他 → 自定义模式
     */
    @GetMapping("/focus/modes")
    public Result<List<Map<String, Object>>> getFocusModeDistribution() {
        log.info("获取专注模式分布");
        
        // 使用 SQL 聚合获取时长分布
        List<Map<String, Object>> durationDistribution = focusRecordMapper.getDurationDistribution();
        
        int pomodoroCount = 0;    // 番茄工作法 (25分钟倍数)
        int fiftyTwoCount = 0;    // 52/17法则 (52或104分钟)
        int immersiveCount = 0;   // 沉浸周期 (90分钟倍数)
        int customCount = 0;      // 自定义
        
        for (Map<String, Object> item : durationDistribution) {
            int duration = ((Number) item.get("duration_minutes")).intValue();
            int count = ((Number) item.get("count")).intValue();
            
            if (duration > 0 && duration % 90 == 0 && duration <= 270) {
                // 沉浸周期：90/180/270分钟 (优先匹配)
                immersiveCount += count;
            } else if (duration > 0 && duration % 25 == 0 && duration <= 150) {
                // 番茄工作法：25/50/75/100/125/150分钟
                pomodoroCount += count;
            } else if (duration == 52 || duration == 104) {
                // 52/17法则
                fiftyTwoCount += count;
            } else {
                customCount += count;
            }
        }
        
        List<Map<String, Object>> distribution = new ArrayList<>();
        distribution.add(createModeItem("番茄工作法", pomodoroCount, "#00ff88"));
        distribution.add(createModeItem("52/17法则", fiftyTwoCount, "#2196f3"));
        distribution.add(createModeItem("沉浸周期", immersiveCount, "#ff6b6b"));
        distribution.add(createModeItem("自定义模式", customCount, "#ffd700"));
        
        return Result.success(distribution);
    }
    
    private Map<String, Object> createModeItem(String name, int count, String color) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", name);
        item.put("value", count);
        item.put("color", color);
        return item;
    }
    
    /**
     * 获取24小时专注分布（热力图数据）- 优化版
     * 使用 SQL 聚合查询
     */
    @GetMapping("/focus/hourly")
    public Result<List<Map<String, Object>>> getHourlyDistribution() {
        log.info("获取24小时专注分布");
        
        // 初始化24小时数据
        int[] hourCounts = new int[24];
        int[] hourDurations = new int[24];
        
        // 使用 SQL 聚合获取每小时的分布
        List<Map<String, Object>> dbDistribution = focusRecordMapper.getHourlyDistribution();
        for (Map<String, Object> item : dbDistribution) {
            int hour = ((Number) item.get("hour")).intValue();
            int count = ((Number) item.get("count")).intValue();
            int duration = ((Number) item.get("duration")).intValue();
            if (hour >= 0 && hour < 24) {
                hourCounts[hour] = count;
                hourDurations[hour] = duration;
            }
        }
        
        List<Map<String, Object>> distribution = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("hour", i);
            item.put("label", String.format("%02d:00", i));
            item.put("count", hourCounts[i]);
            item.put("duration", hourDurations[i]);
            distribution.add(item);
        }
        
        return Result.success(distribution);
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
    
    // ═══════════════════════════════════════════════════════════════════════════
    // 数据统计增强：DAU趋势 & 留存分析
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * 获取DAU趋势（近N天每日活跃用户数）- 优化版
     * DAU定义：当日有专注记录的用户数
     */
    @GetMapping("/dau/trend")
    public Result<List<Map<String, Object>>> getDauTrend(
            @RequestParam(defaultValue = "7") int days) {
        log.info("获取DAU趋势: days={}", days);
        
        List<Map<String, Object>> trend = new ArrayList<>();
        
        // 使用东八区时区
        java.util.TimeZone tz = java.util.TimeZone.getTimeZone("Asia/Shanghai");
        java.text.SimpleDateFormat displayFormat = new java.text.SimpleDateFormat("MM-dd");
        displayFormat.setTimeZone(tz);
        
        // 获取今天开始时间（东八区）
        Calendar cal = Calendar.getInstance(tz);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long todayStart = cal.getTimeInMillis();
        
        for (int i = days - 1; i >= 0; i--) {
            long dayStart = todayStart - i * 86400000L;
            long dayEnd = dayStart + 86400000L;
            
            // 使用 SQL 聚合查询 DAU
            long dau = focusRecordMapper.countDistinctUsersBetween(dayStart, dayEnd);
            
            // 使用 SQL 聚合查询新增用户
            long newUsers = userMapper.countBetween(dayStart, dayEnd);
            
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", displayFormat.format(new Date(dayStart)));
            item.put("dau", dau);
            item.put("newUsers", newUsers);
            trend.add(item);
        }
        
        return Result.success(trend);
    }
    
    /**
     * 获取留存分析数据
     * 计算指定日期注册的用户在后续N日的留存情况
     */
    @GetMapping("/retention")
    public Result<Map<String, Object>> getRetentionAnalysis(
            @RequestParam(defaultValue = "7") int days) {
        log.info("获取留存分析: days={}", days);
        
        Map<String, Object> result = new LinkedHashMap<>();
        
        try {
            // 使用东八区时区
            java.util.TimeZone tz = java.util.TimeZone.getTimeZone("Asia/Shanghai");
            java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd");
            dateFormat.setTimeZone(tz);
            java.text.SimpleDateFormat displayFormat = new java.text.SimpleDateFormat("MM-dd");
            displayFormat.setTimeZone(tz);
            
            // 获取今天开始时间（东八区）
            Calendar cal = Calendar.getInstance(tz);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            long todayStart = cal.getTimeInMillis();
            
            log.info("今天开始时间: {}, 日期: {}", todayStart, dateFormat.format(new Date(todayStart)));
            
            // 获取所有用户的注册时间和首次活跃信息
            List<User> allUsers = userMapper.selectList(null);
            List<FocusRecord> allRecords = focusRecordMapper.selectList(null);
            
            log.info("总用户数: {}, 总专注记录数: {}", allUsers.size(), allRecords.size());
            
            // 按用户ID分组专注记录 - 使用 startTime 作为活跃时间
            Map<Long, Set<String>> userActiveDates = new HashMap<>();
            
            for (FocusRecord record : allRecords) {
                Long userId = record.getUserId();
                // 使用 startTime 作为活跃时间（专注开始时间）
                if (userId != null && record.getStartTime() != null) {
                    String dateStr = dateFormat.format(new Date(record.getStartTime()));
                    userActiveDates.computeIfAbsent(userId, k -> new HashSet<>()).add(dateStr);
                }
            }
            
            log.info("有活跃记录的用户数: {}, 活跃日期: {}", userActiveDates.size(), userActiveDates);
            
            // 计算每日留存
            List<Map<String, Object>> retentionData = new ArrayList<>();
            
            for (int i = days - 1; i >= 0; i--) {
                long dayStart = todayStart - i * 86400000L;
                long dayEnd = dayStart + 86400000L;
                String dayStr = displayFormat.format(new Date(dayStart));
                String baseDate = dateFormat.format(new Date(dayStart));
                
                // 获取当天注册的用户
                final long fs = dayStart;
                final long fe = dayEnd;
                List<User> dayNewUsers = allUsers.stream()
                    .filter(u -> u.getCreatedAt() != null 
                        && u.getCreatedAt() >= fs 
                        && u.getCreatedAt() < fe)
                    .collect(Collectors.toList());
                
                int cohortSize = dayNewUsers.size();
                log.info("日期: {}, 注册用户数: {}", dayStr, cohortSize);
                
                if (cohortSize == 0) {
                    // 即使没有新用户也添加一条记录（显示0）
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("date", dayStr);
                    item.put("newUsers", 0);
                    item.put("day1Retention", 0);
                    item.put("day3Retention", 0);
                    item.put("day7Retention", 0);
                    retentionData.add(item);
                    continue;
                }
                
                // 计算次日、3日、7日留存
                int day1Retention = 0, day3Retention = 0, day7Retention = 0;
                
                for (User user : dayNewUsers) {
                    Set<String> activeDates = userActiveDates.getOrDefault(user.getUserId(), Collections.emptySet());
                    
                    // 次日留存
                    String day1Date = addDays(baseDate, 1);
                    if (activeDates.contains(day1Date)) day1Retention++;
                    
                    // 3日留存
                    String day3Date = addDays(baseDate, 3);
                    if (activeDates.contains(day3Date)) day3Retention++;
                    
                    // 7日留存
                    String day7Date = addDays(baseDate, 7);
                    if (activeDates.contains(day7Date)) day7Retention++;
                }
                
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("date", dayStr);
                item.put("newUsers", cohortSize);
                item.put("day1Retention", (int) Math.round(day1Retention * 100.0 / cohortSize));
                item.put("day3Retention", (int) Math.round(day3Retention * 100.0 / cohortSize));
                item.put("day7Retention", (int) Math.round(day7Retention * 100.0 / cohortSize));
                retentionData.add(item);
                
                log.info("日期: {}, 新用户: {}, 次日留存: {}/{}, 3日留存: {}/{}, 7日留存: {}/{}", 
                    dayStr, cohortSize, day1Retention, cohortSize, day3Retention, cohortSize, day7Retention, cohortSize);
            }
            
            // 计算平均留存率（过滤掉新用户为0的天数）
            List<Map<String, Object>> validData = retentionData.stream()
                .filter(m -> (Integer) m.get("newUsers") > 0)
                .collect(Collectors.toList());
            
            double avgDay1 = validData.isEmpty() ? 0 : validData.stream()
                .mapToInt(m -> (Integer) m.get("day1Retention")).average().orElse(0);
            double avgDay3 = validData.isEmpty() ? 0 : validData.stream()
                .mapToInt(m -> (Integer) m.get("day3Retention")).average().orElse(0);
            double avgDay7 = validData.isEmpty() ? 0 : validData.stream()
                .mapToInt(m -> (Integer) m.get("day7Retention")).average().orElse(0);
            
            result.put("data", retentionData);
            result.put("avgDay1Retention", Math.round(avgDay1 * 10) / 10.0);
            result.put("avgDay3Retention", Math.round(avgDay3 * 10) / 10.0);
            result.put("avgDay7Retention", Math.round(avgDay7 * 10) / 10.0);
            
            log.info("留存分析结果: 数据条数={}, 平均次日留存={}%", retentionData.size(), Math.round(avgDay1 * 10) / 10.0);
            
        } catch (Exception e) {
            log.error("获取留存分析失败", e);
            result.put("data", new ArrayList<>());
            result.put("avgDay1Retention", 0.0);
            result.put("avgDay3Retention", 0.0);
            result.put("avgDay7Retention", 0.0);
        }
        
        return Result.success(result);
    }
    
    /**
     * 日期加减
     */
    private String addDays(String dateStr, int days) {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
            Date date = sdf.parse(dateStr);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            cal.add(Calendar.DAY_OF_MONTH, days);
            return sdf.format(cal.getTime());
        } catch (Exception e) {
            return dateStr;
        }
    }
}