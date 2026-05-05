package com.focusflow.server.controller;

import com.focusflow.server.common.Result;
import com.focusflow.server.dto.LeaveMessageRequest;
import com.focusflow.server.dto.VisitLogResponse;
import com.focusflow.server.service.VisitLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * 互访日志控制器
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * 【API 列表】
 * POST /api/visit/charge/{hostId}  - 为好友花园充能
 * POST /api/visit/message          - 在好友花园留言
 * GET  /api/visit/logs/{hostId}    - 获取花园访客日志
 * GET  /api/visit/unread           - 获取未读通知数量
 * POST /api/visit/read-all         - 标记所有通知为已读
 *
 * 【认证说明】
 * userId 通过请求头 X-User-Id 获取
 */
@Slf4j
@RestController
@RequestMapping("/visit")
@RequiredArgsConstructor
public class VisitController {

    private final VisitLogService visitLogService;

    /** 默认日志数量限制 */
    private static final int DEFAULT_LOG_LIMIT = 20;

    /**
     * 为好友花园充能
     *
     * 【业务逻辑】
     * 1. 检查是否为好友关系
     * 2. 扣除访客光流（5点）
     * 3. 记录充能日志
     *
     * @param userId 当前用户ID
     * @param hostId 花园主人ID
     * @return 操作结果
     */
    @PostMapping("/charge/{hostId}")
    public Result<Void> chargeForFriend(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long hostId) {
        log.info("为好友花园充能: visitorId={}, hostId={}", userId, hostId);

        try {
            visitLogService.chargeForFriend(userId, hostId);
            return Result.success("充能成功，消耗5光流", null);
        } catch (RuntimeException e) {
            log.warn("充能失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 在好友花园留言
     *
     * @param userId 当前用户ID
     * @param request 留言请求
     * @return 操作结果
     */
    @PostMapping("/message")
    public Result<Void> leaveMessage(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody LeaveMessageRequest request) {
        log.info("在好友花园留言: visitorId={}, hostId={}", userId, request.getHostId());

        try {
            visitLogService.leaveMessage(userId, request);
            return Result.success("留言成功", null);
        } catch (RuntimeException e) {
            log.warn("留言失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取花园访客日志
     *
     * @param hostId 花园主人ID
     * @param limit 数量限制（可选，默认20）
     * @return 日志列表
     */
    @GetMapping("/logs/{hostId}")
    public Result<List<VisitLogResponse>> getVisitLogs(
            @PathVariable Long hostId,
            @RequestParam(required = false, defaultValue = "20") int limit) {
        log.info("获取花园访客日志: hostId={}, limit={}", hostId, limit);

        List<VisitLogResponse> logs = visitLogService.getGardenVisitLogs(hostId, Math.min(limit, 50));
        return Result.success(logs);
    }

    /**
     * 获取未读通知数量
     *
     * @param userId 当前用户ID
     * @return 未读数量
     */
    @GetMapping("/unread")
    public Result<Integer> getUnreadCount(@RequestHeader("X-User-Id") Long userId) {
        int count = visitLogService.getUnreadCount(userId);
        return Result.success(count);
    }

    /**
     * 标记所有通知为已读
     *
     * @param userId 当前用户ID
     * @return 操作结果
     */
    @PostMapping("/read-all")
    public Result<Void> markAllAsRead(@RequestHeader("X-User-Id") Long userId) {
        log.info("标记所有通知为已读: userId={}", userId);

        visitLogService.markAllAsRead(userId);
        return Result.success("已全部标记为已读", null);
    }

    /**
     * 检查今日是否已为某好友点亮花园
     *
     * @param userId 当前用户ID
     * @param hostId 花园主人ID
     * @return 今日是否已点亮
     */
    @GetMapping("/has-charged-today/{hostId}")
    public Result<Boolean> hasChargedToday(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long hostId) {
        log.info("检查今日是否已点亮: visitorId={}, hostId={}", userId, hostId);

        boolean hasCharged = visitLogService.hasChargedToday(userId, hostId);
        return Result.success(hasCharged);
    }
}
