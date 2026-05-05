package com.focusflow.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.focusflow.server.dto.LeaveMessageRequest;
import com.focusflow.server.dto.VisitLogResponse;
import com.focusflow.server.entity.User;
import com.focusflow.server.entity.VisitLog;
import com.focusflow.server.mapper.UserMapper;
import com.focusflow.server.mapper.VisitLogMapper;
import com.focusflow.server.service.FriendshipService;
import com.focusflow.server.service.VisitLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * 互访日志服务实现类
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * 【业务逻辑】
 * 1. 充能：只有好友才能充能，每次充能消耗访客的光流
 * 2. 留言：只有好友才能留言
 * 3. 通知：主人可查看访客日志，标记已读
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VisitLogServiceImpl implements VisitLogService {

    private final VisitLogMapper visitLogMapper;
    private final UserMapper userMapper;
    private final FriendshipService friendshipService;

    /** 充能消耗的光流数量 */
    private static final int CHARGE_COST_FLUX = 5;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean chargeForFriend(Long visitorId, Long hostId) {
        // 不能给自己充能
        if (visitorId.equals(hostId)) {
            throw new RuntimeException("不能给自己充能");
        }

        // 检查是否为好友关系
        if (!friendshipService.isFriend(visitorId, hostId)) {
            throw new RuntimeException("只能为好友充能");
        }

        // 检查访客光流是否足够
        User visitor = userMapper.selectById(visitorId);
        if (visitor == null) {
            throw new RuntimeException("访客不存在");
        }

        if (visitor.getTimeFlux() < CHARGE_COST_FLUX) {
            throw new RuntimeException("光流不足，无法充能");
        }

        // 扣除访客光流
        visitor.setTimeFlux(visitor.getTimeFlux() - CHARGE_COST_FLUX);
        userMapper.updateById(visitor);

        // 更新被充能用户的最后专注时间（点亮花园）
        long currentTime = System.currentTimeMillis();
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getUserId, hostId)
                .set(User::getLastFocusTime, currentTime));

        // 记录充能日志
        VisitLog visitLog = new VisitLog();
        visitLog.setVisitorId(visitorId);
        visitLog.setHostId(hostId);
        visitLog.setActionType(VisitLog.ACTION_CHARGE);
        visitLog.setIsRead(0);
        visitLog.setCreatedAt(currentTime);

        visitLogMapper.insert(visitLog);

        log.info("好友花园充能成功: visitorId={}, hostId={}", visitorId, hostId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean leaveMessage(Long visitorId, LeaveMessageRequest request) {
        Long hostId = request.getHostId();

        // 不能给自己留言
        if (visitorId.equals(hostId)) {
            throw new RuntimeException("不能给自己留言");
        }

        // 检查是否为好友关系
        if (!friendshipService.isFriend(visitorId, hostId)) {
            throw new RuntimeException("只能给好友留言");
        }

        // 记录留言日志
        VisitLog visitLog = new VisitLog();
        visitLog.setVisitorId(visitorId);
        visitLog.setHostId(hostId);
        visitLog.setActionType(VisitLog.ACTION_MESSAGE);
        visitLog.setContent(request.getContent());
        visitLog.setIsRead(0);
        visitLog.setCreatedAt(System.currentTimeMillis());

        visitLogMapper.insert(visitLog);

        log.info("好友花园留言成功: visitorId={}, hostId={}", visitorId, hostId);
        return true;
    }

    @Override
    public List<VisitLogResponse> getGardenVisitLogs(Long hostId, int limit) {
        List<VisitLogResponse> responses = new ArrayList<>();

        LambdaQueryWrapper<VisitLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VisitLog::getHostId, hostId)
               .orderByDesc(VisitLog::getCreatedAt)
               .last("LIMIT " + limit);

        List<VisitLog> logs = visitLogMapper.selectList(wrapper);

        for (VisitLog log : logs) {
            User visitor = userMapper.selectById(log.getVisitorId());
            if (visitor != null) {
                VisitLogResponse response = new VisitLogResponse();
                response.setLogId(log.getLogId());
                response.setVisitorId(log.getVisitorId());
                response.setVisitorNickname(visitor.getNickname());
                response.setVisitorAvatarId(visitor.getAvatarId());
                response.setActionType(log.getActionType());
                response.setContent(log.getContent());
                response.setIsRead(log.getIsRead());
                response.setCreatedAt(log.getCreatedAt());

                responses.add(response);
            }
        }

        return responses;
    }

    @Override
    public int getUnreadCount(Long userId) {
        LambdaQueryWrapper<VisitLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VisitLog::getHostId, userId).eq(VisitLog::getIsRead, 0);

        Long count = visitLogMapper.selectCount(wrapper);
        return count != null ? count.intValue() : 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markAllAsRead(Long userId) {
        LambdaUpdateWrapper<VisitLog> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(VisitLog::getHostId, userId)
               .eq(VisitLog::getIsRead, 0)
               .set(VisitLog::getIsRead, 1);

        visitLogMapper.update(null, wrapper);

        log.info("标记所有通知为已读: userId={}", userId);
        return true;
    }

    @Override
    public boolean hasChargedToday(Long visitorId, Long hostId) {
        // 计算今天的起始时间戳（毫秒）
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDateTime startOfDay = today.atStartOfDay();
        long todayStartMillis = startOfDay.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();

        // 查询今日是否已有充能记录
        LambdaQueryWrapper<VisitLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VisitLog::getVisitorId, visitorId)
               .eq(VisitLog::getHostId, hostId)
               .eq(VisitLog::getActionType, VisitLog.ACTION_CHARGE)
               .ge(VisitLog::getCreatedAt, todayStartMillis);

        Long count = visitLogMapper.selectCount(wrapper);
        return count != null && count > 0;
    }
}
