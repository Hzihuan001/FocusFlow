package com.focusflow.server.controller;

import com.focusflow.server.common.Result;
import com.focusflow.server.dto.GardenTileResponse;
import com.focusflow.server.dto.GardenTilesResponse;
import com.focusflow.server.dto.LeaderboardEntry;
import com.focusflow.server.dto.PlantRequest;
import com.focusflow.server.service.GardenTileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * 花园控制器
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * 【API 列表】
 * GET  /garden/tiles/{userId}       - 获取用户花园地块（含活力状态）
 * POST /garden/plant                - 种植植物
 * POST /garden/charge/{tileId}      - 充能
 * POST /garden/harvest/{tileId}     - 收获
 */
@Slf4j
@RestController
@RequestMapping("/garden")
@RequiredArgsConstructor
public class GardenController {

    private final GardenTileService gardenTileService;

    /**
     * 获取用户花园地块（含活力状态）
     *
     * @param userId 用户ID（从请求头获取）
     * @return 地块列表和活力状态
     */
    @GetMapping("/tiles")
    public Result<GardenTilesResponse> getUserTiles(
            @RequestHeader("X-User-Id") Long userId) {
        log.info("获取花园地块: userId={}", userId);

        GardenTilesResponse response = gardenTileService.getUserTilesWithVitality(userId);
        return Result.success(response);
    }

    /**
     * 种植植物
     *
     * @param userId  用户ID（从请求头获取）
     * @param request 种植请求
     * @return 种植结果
     */
    @PostMapping("/plant")
    public Result<GardenTileResponse> plant(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody PlantRequest request) {
        log.info("种植植物: userId={}, x={}, y={}, plantId={}",
                userId, request.getX(), request.getY(), request.getPlantId());

        try {
            GardenTileResponse response = gardenTileService.plant(userId, request);
            return Result.success("种植成功", response);
        } catch (RuntimeException e) {
            log.warn("种植失败: userId={}, reason={}", userId, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 充能（更新最后充能时间）
     *
     * @param userId 用户ID（从请求头获取）
     * @param tileId 地块ID
     * @return 操作结果
     */
    @PostMapping("/charge/{tileId}")
    public Result<Boolean> charge(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long tileId) {
        log.info("充能: userId={}, tileId={}", userId, tileId);

        try {
            boolean success = gardenTileService.charge(userId, tileId);
            return Result.success("充能成功", success);
        } catch (RuntimeException e) {
            log.warn("充能失败: userId={}, tileId={}, reason={}", userId, tileId, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 收获植物
     *
     * @param userId 用户ID（从请求头获取）
     * @param tileId 地块ID
     * @return 操作结果
     */
    @PostMapping("/harvest/{tileId}")
    public Result<Boolean> harvest(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long tileId) {
        log.info("收获植物: userId={}, tileId={}", userId, tileId);

        try {
            boolean success = gardenTileService.harvest(userId, tileId);
            return Result.success("收获成功", success);
        } catch (RuntimeException e) {
            log.warn("收获失败: userId={}, tileId={}, reason={}", userId, tileId, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 专注完成后给花园充能
     *
     * @param userId          用户ID（从请求头获取）
     * @param durationMinutes 专注时长（分钟）
     * @return 充能的地块数量
     */
    @PostMapping("/purify")
    public Result<Integer> purifyOnFocus(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam int durationMinutes) {
        log.info("专注充能: userId={}, durationMinutes={}", userId, durationMinutes);

        int charged = gardenTileService.purifyOnFocus(userId, durationMinutes);
        String message = charged > 0 
                ? String.format("充能成功！%d格地块已恢复活力", charged) 
                : "没有可充能的地块";
        return Result.success(message, charged);
    }

    /**
     * 获取好友点亮排行榜
     * 仅显示当前用户及其好友的排名，不涉及全服用户
     *
     * @param userId 当前用户ID（从请求头获取）
     * @param limit 返回条数（默认20，最大100）
     * @return 好友排行榜列表
     */
    @GetMapping("/leaderboard")
    public Result<List<LeaderboardEntry>> getFriendLeaderboard(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "20") int limit) {
        log.info("获取好友排行榜: userId={}, limit={}", userId, limit);
        
        // 限制最大条数
        int safeLimit = Math.min(limit, 100);
        List<LeaderboardEntry> leaderboard = gardenTileService.getFriendLeaderboard(userId, safeLimit);
        return Result.success(leaderboard);
    }
}
