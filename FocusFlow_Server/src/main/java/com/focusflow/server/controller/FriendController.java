package com.focusflow.server.controller;

import com.focusflow.server.common.Result;
import com.focusflow.server.dto.AddFriendRequest;
import com.focusflow.server.dto.FriendInfoResponse;
import com.focusflow.server.service.FriendshipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * 好友控制器
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * 【API 列表】
 * POST /api/friend/add           - 发送好友申请
 * POST /api/friend/accept/{id}   - 同意好友申请
 * POST /api/friend/reject/{id}   - 拒绝好友申请
 * GET  /api/friend/list          - 获取好友列表
 * GET  /api/friend/requests      - 获取待处理的好友申请
 *
 * 【认证说明】
 * userId 通过请求头或 JWT Token 获取，此处简化为请求参数
 */
@Slf4j
@RestController
@RequestMapping("/friend")
@RequiredArgsConstructor
public class FriendController {

    private final FriendshipService friendshipService;

    /**
     * 发送好友申请
     *
     * @param userId 当前用户ID（请求头）
     * @param request 申请请求
     * @return 操作结果
     */
    @PostMapping("/add")
    public Result<Void> addFriend(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody AddFriendRequest request) {
        log.info("发送好友申请: userId={}, friendId={}", userId, request.getFriendId());

        try {
            friendshipService.sendFriendRequest(userId, request);
            return Result.success("好友申请已发送", null);
        } catch (RuntimeException e) {
            log.warn("发送好友申请失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 同意好友申请
     *
     * @param userId 当前用户ID
     * @param friendshipId 关系记录ID
     * @return 操作结果
     */
    @PostMapping("/accept/{friendshipId}")
    public Result<Void> acceptFriend(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long friendshipId) {
        log.info("同意好友申请: userId={}, friendshipId={}", userId, friendshipId);

        try {
            friendshipService.acceptFriendRequest(userId, friendshipId);
            return Result.success("已成为好友", null);
        } catch (RuntimeException e) {
            log.warn("同意好友申请失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 拒绝好友申请
     *
     * @param userId 当前用户ID
     * @param friendshipId 关系记录ID
     * @return 操作结果
     */
    @PostMapping("/reject/{friendshipId}")
    public Result<Void> rejectFriend(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long friendshipId) {
        log.info("拒绝好友申请: userId={}, friendshipId={}", userId, friendshipId);

        try {
            friendshipService.rejectFriendRequest(userId, friendshipId);
            return Result.success("已拒绝申请", null);
        } catch (RuntimeException e) {
            log.warn("拒绝好友申请失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取好友列表
     *
     * @param userId 当前用户ID
     * @return 好友列表
     */
    @GetMapping("/list")
    public Result<List<FriendInfoResponse>> getFriendList(
            @RequestHeader("X-User-Id") Long userId) {
        log.info("获取好友列表: userId={}", userId);

        List<FriendInfoResponse> friends = friendshipService.getFriendList(userId);
        return Result.success(friends);
    }

    /**
     * 获取待处理的好友申请
     *
     * @param userId 当前用户ID
     * @return 申请列表
     */
    @GetMapping("/requests")
    public Result<List<FriendInfoResponse>> getPendingRequests(
            @RequestHeader("X-User-Id") Long userId) {
        log.info("获取待处理的好友申请: userId={}", userId);

        List<FriendInfoResponse> requests = friendshipService.getPendingRequests(userId);
        return Result.success(requests);
    }
}
