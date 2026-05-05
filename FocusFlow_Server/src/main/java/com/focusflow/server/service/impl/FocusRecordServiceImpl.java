package com.focusflow.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.focusflow.server.common.SecurityUtils;
import com.focusflow.server.dto.FocusRecordSyncDTO;
import com.focusflow.server.dto.SyncResultDTO;
import com.focusflow.server.entity.FocusRecord;
import com.focusflow.server.entity.SystemConfig;
import com.focusflow.server.entity.User;
import com.focusflow.server.mapper.FocusRecordMapper;
import com.focusflow.server.mapper.UserMapper;
import com.focusflow.server.service.FocusRecordService;
import com.focusflow.server.service.SystemConfigService;
import com.focusflow.server.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * 专注记录服务实现类
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * 【核心业务流程】
 * 1. 遍历待同步记录
 * 2. 验证签名（防篡改）
 * 3. 验签通过：落库 + 增加用户光流
 * 4. 返回每条记录的同步结果
 *
 * 【防篡改验签设计】
 * 签名算法：SHA-256(recordId || userId || durationMinutes || startTime || SALT)
 * 服务端重新计算签名并与提交的签名比对，防止：
 * - 用户通过 SQLite 修改器伪造时长
 * - 中间人攻击篡改网络包
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FocusRecordServiceImpl implements FocusRecordService {

    private final FocusRecordMapper focusRecordMapper;
    private final UserMapper userMapper;
    private final UserService userService;
    private final SystemConfigService systemConfigService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<SyncResultDTO> batchSync(List<FocusRecordSyncDTO> records) {
        List<SyncResultDTO> results = new ArrayList<>();
        long currentTime = System.currentTimeMillis();

        for (FocusRecordSyncDTO dto : records) {
            try {
                SyncResultDTO result = processSingleRecord(dto, currentTime);
                results.add(result);
            } catch (Exception e) {
                log.error("同步记录异常: recordId={}, error={}", dto.getRecordId(), e.getMessage());
                results.add(SyncResultDTO.fail(dto.getRecordId(), "服务器异常: " + e.getMessage()));
            }
        }

        log.info("批量同步完成: 总数={}, 成功={}", 
                records.size(), 
                results.stream().filter(SyncResultDTO::getSuccess).count());

        return results;
    }

    /**
     * 处理单条同步记录
     *
     * 【验签流程】
     * 1. 检查用户是否存在
     * 2. 检查记录是否已存在（防重放）
     * 3. 验证签名（防篡改）
     * 4. 落库并增加光流
     */
    private SyncResultDTO processSingleRecord(FocusRecordSyncDTO dto, long currentTime) {
        String recordId = dto.getRecordId();
        Long userId = dto.getUserId();

        // ════════════════════════════════════════════════════════════════════════
        // Step 1: 检查用户是否存在
        // ════════════════════════════════════════════════════════════════════════
        User user = userMapper.selectById(userId);
        if (user == null) {
            log.warn("用户不存在: userId={}", userId);
            return SyncResultDTO.fail(recordId, "用户不存在");
        }

        // ════════════════════════════════════════════════════════════════════════
        // Step 2: 检查记录是否已存在（防重放攻击）
        // ════════════════════════════════════════════════════════════════════════
        if (existsByRecordId(recordId)) {
            log.warn("记录已存在，跳过同步: recordId={}", recordId);
            return SyncResultDTO.fail(recordId, "记录已存在，请勿重复同步");
        }

        // ════════════════════════════════════════════════════════════════════════
        // Step 3: 验证签名（防篡改核心）
        // ════════════════════════════════════════════════════════════════════════
        boolean signatureValid = SecurityUtils.verifySignature(
                recordId,
                userId,
                dto.getDurationMinutes(),
                dto.getStartTime(),
                dto.getSignature()
        );

        if (!signatureValid) {
            log.warn("签名验证失败: recordId={}, userId={}, duration={}", 
                    recordId, userId, dto.getDurationMinutes());
            return SyncResultDTO.fail(recordId, "签名验证失败，数据可能被篡改");
        }

        // ════════════════════════════════════════════════════════════════════════
        // Step 4: 落库 + 增加光流 + 连续天数奖励
        // ════════════════════════════════════════════════════════════════════════
        FocusRecord record = new FocusRecord();
        record.setRecordId(recordId);
        record.setUserId(userId);
        record.setTaskName(dto.getTaskName() != null ? dto.getTaskName() : "未命名任务");
        record.setDurationMinutes(dto.getDurationMinutes());
        record.setStartTime(dto.getStartTime());
        record.setSignature(dto.getSignature());
        // 创建时间 = 专注开始时间 + 时长（专注结束时间）
        long createdAtTime = dto.getStartTime() + dto.getDurationMinutes() * 60L * 1000L;
        record.setCreatedAt(createdAtTime);
        // 同步时间 = 当前服务器时间
        record.setSyncTime(currentTime);

        focusRecordMapper.insert(record);

        // 从系统配置读取每分钟光流奖励
        int rewardPerMinute = systemConfigService.getIntConfig(SystemConfig.FOCUS_REWARD_PER_MINUTE, 1);
        int baseReward = dto.getDurationMinutes() * rewardPerMinute;
        int streakBonus = 0;

        // 计算连续专注天数奖励
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String lastFocusDate = user.getLastFocusDate();
        Integer streakDays = user.getStreakDays() != null ? user.getStreakDays() : 0;
        long focusStartTime = dto.getStartTime();

        if (!today.equals(lastFocusDate)) {
            // 今天首次专注
            String yesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
            
            if (yesterday.equals(lastFocusDate)) {
                // 连续专注：天数+1
                streakDays = streakDays + 1;
            } else {
                // 中断了，重新开始
                streakDays = 1;
            }

            // 更新用户连续天数和最后专注时间
            userMapper.update(null, new LambdaUpdateWrapper<User>()
                    .eq(User::getUserId, userId)
                    .set(User::getStreakDays, streakDays)
                    .set(User::getLastFocusDate, today)
                    .set(User::getLastFocusTime, focusStartTime)
                    .set(User::getUpdatedAt, currentTime));

            // 连续天数奖励
            streakBonus = calculateStreakBonus(streakDays);
            if (streakBonus > 0) {
                log.info("连续专注奖励: userId={}, streakDays={}, bonus={}", userId, streakDays, streakBonus);
            }
        } else {
            // 同一天多次专注，只更新最后专注时间
            userMapper.update(null, new LambdaUpdateWrapper<User>()
                    .eq(User::getUserId, userId)
                    .set(User::getLastFocusTime, focusStartTime)
                    .set(User::getUpdatedAt, currentTime));
        }

        // 总光流奖励
        int totalReward = baseReward + streakBonus;
        userService.addTimeFlux(userId, totalReward);

        log.info("记录同步成功: recordId={}, userId={}, duration={}, baseReward={}, streakBonus={}", 
                recordId, userId, dto.getDurationMinutes(), baseReward, streakBonus);

        return SyncResultDTO.success(recordId, totalReward);
    }

    /**
     * 计算连续专注天数奖励
     * 从系统配置读取奖励值，支持后台动态调整
     */
    private int calculateStreakBonus(int streakDays) {
        // 从配置读取各阶梯奖励值
        if (streakDays >= 30) {
            return systemConfigService.getIntConfig(SystemConfig.STREAK_BONUS_30, 100);
        }
        if (streakDays >= 21) {
            return systemConfigService.getIntConfig(SystemConfig.STREAK_BONUS_21, 50);
        }
        if (streakDays >= 14) {
            return systemConfigService.getIntConfig(SystemConfig.STREAK_BONUS_14, 30);
        }
        if (streakDays >= 7) {
            return systemConfigService.getIntConfig(SystemConfig.STREAK_BONUS_7, 20);
        }
        if (streakDays >= 5) {
            return systemConfigService.getIntConfig(SystemConfig.STREAK_BONUS_5, 10);
        }
        if (streakDays >= 3) {
            return systemConfigService.getIntConfig(SystemConfig.STREAK_BONUS_3, 5);
        }
        return 0;
    }

    @Override
    public boolean existsByRecordId(String recordId) {
        Long count = focusRecordMapper.selectCount(
                new LambdaQueryWrapper<FocusRecord>()
                        .eq(FocusRecord::getRecordId, recordId)
        );
        return count != null && count > 0;
    }
}
