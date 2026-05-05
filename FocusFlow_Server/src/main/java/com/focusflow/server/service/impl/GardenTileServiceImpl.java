package com.focusflow.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.focusflow.server.dto.GardenTileResponse;
import com.focusflow.server.dto.GardenTilesResponse;
import com.focusflow.server.dto.LeaderboardEntry;
import com.focusflow.server.dto.PlantRequest;
import com.focusflow.server.entity.GardenTile;
import com.focusflow.server.entity.PlantDict;
import com.focusflow.server.entity.SystemConfig;
import com.focusflow.server.entity.User;
import com.focusflow.server.entity.UserBag;
import com.focusflow.server.entity.Friendship;
import com.focusflow.server.mapper.GardenTileMapper;
import com.focusflow.server.mapper.PlantDictMapper;
import com.focusflow.server.mapper.UserBagMapper;
import com.focusflow.server.mapper.UserMapper;
import com.focusflow.server.mapper.FriendshipMapper;
import com.focusflow.server.service.GardenTileService;
import com.focusflow.server.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 花园地块服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GardenTileServiceImpl implements GardenTileService {

    private final GardenTileMapper gardenTileMapper;
    private final PlantDictMapper plantDictMapper;
    private final UserBagMapper userBagMapper;
    private final SystemConfigService systemConfigService;
    private final com.focusflow.server.service.UserService userService;
    private final UserMapper userMapper;
    private final FriendshipMapper friendshipMapper;

    @Override
    public GardenTilesResponse getUserTilesWithVitality(Long userId) {
        // 获取地块列表
        List<GardenTileResponse> tiles = getUserTiles(userId);
        
        // 获取用户最后专注时间
        Long lastFocusTime = getLastFocusTime(userId);
        
        return GardenTilesResponse.of(tiles, lastFocusTime);
    }

    @Override
    public List<GardenTileResponse> getUserTiles(Long userId) {
        // 查询用户所有地块
        LambdaQueryWrapper<GardenTile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GardenTile::getUserId, userId)
               .orderByAsc(GardenTile::getY)
               .orderByAsc(GardenTile::getX);

        List<GardenTile> tiles = gardenTileMapper.selectList(wrapper);

        // 获取植物字典
        List<PlantDict> plants = plantDictMapper.selectList(null);
        Map<Integer, PlantDict> plantMap = plants.stream()
                .collect(Collectors.toMap(PlantDict::getPlantId, p -> p));

        // 转换为响应DTO
        return tiles.stream().map(tile -> {
            GardenTileResponse response = GardenTileResponse.fromEntity(tile);
            if (tile.getPlantId() != null) {
                PlantDict plant = plantMap.get(tile.getPlantId());
                if (plant != null) {
                    response.setPlantName(plant.getPlantName());
                    response.setPlantColor(plant.getColorHex());
                    response.setImageUrl(plant.getImageUrl());
                    response.setWidth(plant.getWidth());
                }
            }
            return response;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GardenTileResponse plant(Long userId, PlantRequest request) {
        // ════════════════════════════════════════════════════════════════════════
        // 🔧 [MULTI-TILE] 获取植物尺寸，支持多格占用
        // ════════════════════════════════════════════════════════════════════════
        PlantDict plant = plantDictMapper.selectById(request.getPlantId());
        if (plant == null) {
            throw new RuntimeException("植物不存在");
        }
        
        // 🔧 [WIDTH×WIDTH] width 表示 n×n 正方形区域（只有奇数值：1/3/5）
        // 点击坐标作为中心，向四周扩展
        int plantSize = plant.getWidth() != null ? plant.getWidth() : 1;
        int centerX = request.getX();
        int centerY = request.getY();
        int radius = (plantSize - 1) / 2;  // 向四周扩展的半径
        
        log.info("种植请求: userId={}, plantId={}, size={}x{}, 中心=({},{})", 
                userId, request.getPlantId(), plantSize, plantSize, centerX, centerY);

        // ════════════════════════════════════════════════════════════════════════
        // 1. 检查所有占用地块是否可用（碰撞检测）
        // ════════════════════════════════════════════════════════════════════════
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                int checkX = centerX + dx;
                int checkY = centerY + dy;
                
                LambdaQueryWrapper<GardenTile> existWrapper = new LambdaQueryWrapper<>();
                existWrapper.eq(GardenTile::getUserId, userId)
                           .eq(GardenTile::getX, checkX)
                           .eq(GardenTile::getY, checkY);
                
                GardenTile existingTile = gardenTileMapper.selectOne(existWrapper);
                if (existingTile != null && existingTile.getPlantId() != null) {
                    throw new RuntimeException(String.format(
                        "坐标(%d,%d)已被占用，无法种植", checkX, checkY));
                }
            }
        }

        // ════════════════════════════════════════════════════════════════════════
        // 2. 验证背包物品
        // ════════════════════════════════════════════════════════════════════════
        String bagRecordId = request.getBagRecordId();
        if (bagRecordId != null && !bagRecordId.isEmpty()) {
            UserBag bagItem = userBagMapper.selectById(bagRecordId);
            if (bagItem == null || !bagItem.getUserId().equals(userId)) {
                throw new RuntimeException("背包物品不存在或无权限");
            }
            if (bagItem.getStatus() != 1) {
                throw new RuntimeException("该物品不是成熟植物，无法种植");
            }
            if (!bagItem.getPlantId().equals(request.getPlantId())) {
                throw new RuntimeException("背包物品与要种植的植物不匹配");
            }
            
            // 更新背包物品状态为"已种植"
            bagItem.setStatus(2);
            userBagMapper.updateById(bagItem);
            log.info("背包物品状态更新为已种植: bagId={}", bagRecordId);
        }

        // ════════════════════════════════════════════════════════════════════════
        // 3. 在所有占用地块上创建/更新记录（从中心向外扩展）
        // ════════════════════════════════════════════════════════════════════════
        GardenTile mainTile = null;
        long currentTime = System.currentTimeMillis();
        int tilesCreated = 0;
        int tilesUpdated = 0;
        
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                int tileX = centerX + dx;
                int tileY = centerY + dy;
                
                LambdaQueryWrapper<GardenTile> existWrapper = new LambdaQueryWrapper<>();
                existWrapper.eq(GardenTile::getUserId, userId)
                           .eq(GardenTile::getX, tileX)
                           .eq(GardenTile::getY, tileY);
                
                GardenTile existingTile = gardenTileMapper.selectOne(existWrapper);
                GardenTile tile = existingTile != null ? existingTile : new GardenTile();
                
                tile.setUserId(userId);
                tile.setX(tileX);
                tile.setY(tileY);
                tile.setPlantId(request.getPlantId());
                tile.setBagRecordId(bagRecordId);
                tile.setDeployTime(currentTime);
                tile.setLastChargeTime(currentTime);
                tile.setIsPurified(1);
                tile.setUpdatedAt(currentTime);
                
                if (existingTile != null) {
                    tile.setCreatedAt(existingTile.getCreatedAt());
                    gardenTileMapper.updateById(tile);
                    tilesUpdated++;
                } else {
                    tile.setCreatedAt(currentTime);
                    gardenTileMapper.insert(tile);
                    tilesCreated++;
                }
                
                // 记录主地块（中心坐标）
                if (dx == 0 && dy == 0) {
                    mainTile = tile;
                }
            }
        }
        
        log.info("多格种植完成: userId={}, 占用{}格, 新建={}, 更新={}", 
                userId, plantSize * plantSize, tilesCreated, tilesUpdated);

        // ════════════════════════════════════════════════════════════════════════
        // 4. 根据 purify_range 解锁周围地块
        // ════════════════════════════════════════════════════════════════════════
        int purifyRange = plant.getPurifyRange() != null ? plant.getPurifyRange() : 1;
        int newTilesPurified = purifyTilesAround(userId, centerX, centerY, purifyRange);
        
        log.info("净化解锁: userId={}, 中心=({},{}, 范围={}, 新地块={}", 
                userId, centerX, centerY, purifyRange, newTilesPurified);

        // 构建响应（使用主地块）
        GardenTileResponse response = GardenTileResponse.fromEntity(mainTile);
        response.setPlantName(plant.getPlantName());
        response.setPlantColor(plant.getColorHex());
        // 添加尺寸信息供前端使用（width×width 正方形）
        response.setWidth(plantSize);

        log.info("种植成功: userId={}, x={}, y={}, plantId={}, size={}x{}", 
                userId, centerX, centerY, request.getPlantId(), plantSize, plantSize);
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean charge(Long userId, Long tileId) {
        // 检查地块是否存在且属于该用户
        GardenTile tile = gardenTileMapper.selectById(tileId);
        if (tile == null || !tile.getUserId().equals(userId)) {
            throw new RuntimeException("地块不存在或无权限");
        }

        // 从配置获取充能消耗光流
        int chargeCost = systemConfigService.getIntConfig(SystemConfig.CHARGE_COST, 50);
        
        // 扣减用户光流
        boolean deducted = userService.deductTimeFlux(userId, chargeCost);
        if (!deducted) {
            throw new RuntimeException("光流不足，需要 " + chargeCost + " 光流");
        }

        // 更新所有地块的最后充能时间（一键点亮所有植物）
        long currentTime = System.currentTimeMillis();
        LambdaUpdateWrapper<GardenTile> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(GardenTile::getUserId, userId)
                     .set(GardenTile::getLastChargeTime, currentTime)
                     .set(GardenTile::getUpdatedAt, currentTime);
        
        gardenTileMapper.update(null, updateWrapper);
        
        log.info("充能成功: userId={}, tileId={}, 充能所有地块, 消耗光流={}", userId, tileId, chargeCost);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean harvest(Long userId, Long tileId) {
        // 检查地块是否存在且属于该用户
        GardenTile tile = gardenTileMapper.selectById(tileId);
        if (tile == null || !tile.getUserId().equals(userId)) {
            throw new RuntimeException("地块不存在或无权限");
        }

        if (tile.getPlantId() == null) {
            throw new RuntimeException("该地块没有植物可收获");
        }

        // 清除植物信息
        LambdaUpdateWrapper<GardenTile> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(GardenTile::getTileId, tileId)
               .set(GardenTile::getPlantId, null)
               .set(GardenTile::getBagRecordId, null)
               .set(GardenTile::getDeployTime, null)
               .set(GardenTile::getUpdatedAt, System.currentTimeMillis());

        gardenTileMapper.update(null, wrapper);
        log.info("收获成功: userId={}, tileId={}", userId, tileId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int purifyOnFocus(Long userId, int durationMinutes) {
        // ════════════════════════════════════════════════════════════════════════
        // 🎮 专注充能机制
        // ════════════════════════════════════════════════════════════════════════
        // 当天完成专注后，给整个花园充能（更新所有地块的 lastChargeTime）
        // 效果等同于好友充能，让所有植物恢复活力
        // ════════════════════════════════════════════════════════════════════════
        
        log.info("专注充能: userId={}, durationMinutes={}", userId, durationMinutes);
        
        // 查询用户所有地块
        LambdaQueryWrapper<GardenTile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GardenTile::getUserId, userId);
        List<GardenTile> tiles = gardenTileMapper.selectList(wrapper);
        
        if (tiles.isEmpty()) {
            log.info("用户没有花园地块: userId={}", userId);
            return 0;
        }
        
        // 更新所有地块的最后充能时间
        long currentTime = System.currentTimeMillis();
        LambdaUpdateWrapper<GardenTile> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(GardenTile::getUserId, userId)
                     .set(GardenTile::getLastChargeTime, currentTime)
                     .set(GardenTile::getUpdatedAt, currentTime);
        
        int updated = gardenTileMapper.update(null, updateWrapper);
        
        log.info("专注充能完成: userId={}, 充能地块数={}", userId, updated);
        return updated;
    }
    
    /**
     * 净化指定坐标周围的地块
     * 
     * @param userId 用户ID
     * @param centerX 中心X坐标
     * @param centerY 中心Y坐标
     * @param range 净化范围半径
     * @return 新创建的地块数量
     */
    private int purifyTilesAround(Long userId, int centerX, int centerY, int range) {
        int created = 0;
        long currentTime = System.currentTimeMillis();
        
        // 遍历正方形范围内的所有坐标
        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -range; dy <= range; dy++) {
                // 跳过中心点（已种植）
                if (dx == 0 && dy == 0) continue;
                
                // 曼哈顿距离过滤：只净化菱形区域
                if (Math.abs(dx) + Math.abs(dy) > range) continue;
                
                int x = centerX + dx;
                int y = centerY + dy;
                
                // 检查地块是否已存在
                LambdaQueryWrapper<GardenTile> existWrapper = new LambdaQueryWrapper<>();
                existWrapper.eq(GardenTile::getUserId, userId)
                           .eq(GardenTile::getX, x)
                           .eq(GardenTile::getY, y);
                
                GardenTile existingTile = gardenTileMapper.selectOne(existWrapper);
                
                if (existingTile == null) {
                    // 创建新净化地块
                    GardenTile newTile = new GardenTile();
                    newTile.setUserId(userId);
                    newTile.setX(x);
                    newTile.setY(y);
                    newTile.setPlantId(null);
                    newTile.setBagRecordId(null);
                    newTile.setIsPurified(1);
                    newTile.setCreatedAt(currentTime);
                    newTile.setUpdatedAt(currentTime);
                    gardenTileMapper.insert(newTile);
                    created++;
                } else if (existingTile.getIsPurified() == null || existingTile.getIsPurified() == 0) {
                    // 更新现有地块为已净化
                    existingTile.setIsPurified(1);
                    existingTile.setUpdatedAt(currentTime);
                    gardenTileMapper.updateById(existingTile);
                }
            }
        }
        
        return created;
    }

    /**
     * 获取用户最后专注时间（从用户表获取，统一管理花园点亮状态）
     */
    private Long getLastFocusTime(Long userId) {
        User user = userMapper.selectById(userId);
        if (user != null && user.getLastFocusTime() != null) {
            return user.getLastFocusTime();
        }
        return 0L;
    }

    @Override
    public List<LeaderboardEntry> getFriendLeaderboard(Long userId, int limit) {
        // ════════════════════════════════════════════════════════════════════════
        // 🏆 好友点亮排行榜：仅显示当前用户及其好友的排名
        // ════════════════════════════════════════════════════════════════════════
        
        // 1. 获取用户的所有好友ID（双向查询）
        LambdaQueryWrapper<Friendship> friendWrapper = new LambdaQueryWrapper<>();
        friendWrapper.and(w -> w
            .eq(Friendship::getUserId, userId)
            .or()
            .eq(Friendship::getFriendId, userId)
        ).eq(Friendship::getStatus, 1);  // 已同意的好友
        
        List<Friendship> friendships = friendshipMapper.selectList(friendWrapper);
        
        // 2. 提取好友ID列表（包含自己）
        java.util.Set<Long> friendIds = new java.util.HashSet<>();
        friendIds.add(userId);  // 包含当前用户自己
        
        for (Friendship f : friendships) {
            if (f.getUserId().equals(userId)) {
                friendIds.add(f.getFriendId());
            } else {
                friendIds.add(f.getUserId());
            }
        }
        
        log.info("好友排行榜: userId={}, 好友数={}", userId, friendIds.size() - 1);
        
        // 3. 查询这些好友的净化地块
        if (friendIds.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        
        LambdaQueryWrapper<GardenTile> tileWrapper = new LambdaQueryWrapper<>();
        tileWrapper.eq(GardenTile::getIsPurified, 1)
                   .in(GardenTile::getUserId, friendIds);
        List<GardenTile> purifiedTiles = gardenTileMapper.selectList(tileWrapper);
        
        // 4. 按用户分组统计
        Map<Long, Long> userCountMap = purifiedTiles.stream()
            .collect(Collectors.groupingBy(GardenTile::getUserId, Collectors.counting()));
        
        // 确保所有好友都在统计中（即使点亮数为0）
        for (Long friendId : friendIds) {
            userCountMap.putIfAbsent(friendId, 0L);
        }
        
        // 5. 按数量降序排序
        List<Map.Entry<Long, Long>> sortedEntries = userCountMap.entrySet().stream()
            .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
            .limit(limit)
            .collect(Collectors.toList());
        
        // 6. 构建排行榜条目
        List<LeaderboardEntry> leaderboard = new java.util.ArrayList<>();
        int rank = 1;
        
        for (Map.Entry<Long, Long> entry : sortedEntries) {
            Long uid = entry.getKey();
            int count = entry.getValue().intValue();
            
            // 获取用户信息
            User user = userMapper.selectById(uid);
            if (user != null) {
                leaderboard.add(LeaderboardEntry.builder()
                    .userId(uid)
                    .nickname(user.getNickname())
                    .account(user.getAccount())
                    .avatarUrl(user.getAvatarId() != null ? "avatar_${user.getAvatarId()}" : null)
                    .purifiedCount(count)
                    .rank(rank++)
                    .build());
            }
        }
        
        log.info("好友排行榜查询完成: userId={}, 返回{}条记录", userId, leaderboard.size());
        return leaderboard;
    }
}
