package com.focusflow.server.service;

import com.focusflow.server.dto.GardenTileResponse;
import com.focusflow.server.dto.GardenTilesResponse;
import com.focusflow.server.dto.LeaderboardEntry;
import com.focusflow.server.dto.PlantRequest;

import java.util.List;

/**
 * 花园地块服务接口
 */
public interface GardenTileService {

    /**
     * 获取用户所有花园地块（包含活力状态）
     *
     * @param userId 用户ID
     * @return 地块列表和活力状态
     */
    GardenTilesResponse getUserTilesWithVitality(Long userId);

    /**
     * 获取用户所有花园地块
     *
     * @param userId 用户ID
     * @return 地块列表
     */
    List<GardenTileResponse> getUserTiles(Long userId);

    /**
     * 种植植物
     *
     * @param userId  用户ID
     * @param request 种植请求
     * @return 种植结果
     */
    GardenTileResponse plant(Long userId, PlantRequest request);

    /**
     * 充能（更新最后充能时间）
     *
     * @param userId 用户ID
     * @param tileId 地块ID
     * @return 是否成功
     */
    boolean charge(Long userId, Long tileId);

    /**
     * 收获植物（删除地块上的植物）
     *
     * @param userId 用户ID
     * @param tileId 地块ID
     * @return 是否成功
     */
    boolean harvest(Long userId, Long tileId);

    /**
     * 专注完成后净化花园
     * 根据专注时长，在花园边缘扩展新的净化区域
     *
     * @param userId          用户ID
     * @param durationMinutes 专注时长（分钟）
     * @return 新净化的地块数量
     */
    int purifyOnFocus(Long userId, int durationMinutes);

    /**
     * 获取点亮地块排行榜
     * 按用户点亮（已净化）地块数量排名
     *
     * @param limit 返回条数（默认20）
     * @return 排行榜列表
     */
    /**
     * 获取好友点亮排行榜
     * 
     * @param userId 当前用户ID（用于查询好友列表）
     * @param limit 返回条数
     * @return 好友排行榜列表（包含当前用户）
     */
    List<LeaderboardEntry> getFriendLeaderboard(Long userId, int limit);
}
