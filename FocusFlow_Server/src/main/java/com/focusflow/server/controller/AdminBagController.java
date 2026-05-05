package com.focusflow.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.focusflow.server.common.Result;
import com.focusflow.server.entity.PlantDict;
import com.focusflow.server.entity.User;
import com.focusflow.server.entity.UserBag;
import com.focusflow.server.mapper.PlantDictMapper;
import com.focusflow.server.mapper.UserBagMapper;
import com.focusflow.server.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * 后台背包管理控制器
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * 【API 列表】
 * GET  /admin/bag/list       - 分页获取背包列表
 * GET  /admin/bag/user/{id}  - 获取指定用户的背包
 * POST /admin/bag/add        - 为用户添加道具
 * DELETE /admin/bag/{bagId}  - 删除背包物品
 * POST /admin/bag/batch      - 批量发放道具
 */
@Slf4j
@RestController
@RequestMapping("/admin/bag")
@RequiredArgsConstructor
public class AdminBagController {

    private final UserBagMapper userBagMapper;
    private final PlantDictMapper plantDictMapper;
    private final UserMapper userMapper;

    /**
     * 分页获取所有背包物品
     */
    @GetMapping("/list")
    public Result<Map<String, Object>> getBagList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer status) {
        log.info("获取背包列表: page={}, size={}, userId={}, status={}", page, size, userId, status);
        
        LambdaQueryWrapper<UserBag> wrapper = new LambdaQueryWrapper<>();
        
        if (userId != null) {
            wrapper.eq(UserBag::getUserId, userId);
        }
        if (status != null) {
            wrapper.eq(UserBag::getStatus, status);
        }
        
        wrapper.orderByDesc(UserBag::getCreatedAt);
        
        Page<UserBag> bagPage = userBagMapper.selectPage(new Page<>(page, size), wrapper);
        
        // 获取植物信息
        Map<Integer, PlantDict> plantMap = new HashMap<>();
        List<PlantDict> plants = plantDictMapper.selectList(null);
        for (PlantDict plant : plants) {
            plantMap.put(plant.getPlantId(), plant);
        }
        
        // 组装返回数据
        List<Map<String, Object>> list = new ArrayList<>();
        for (UserBag bag : bagPage.getRecords()) {
            Map<String, Object> item = new HashMap<>();
            item.put("bagId", bag.getBagId());
            item.put("userId", bag.getUserId());
            item.put("plantId", bag.getPlantId());
            item.put("status", bag.getStatus());
            item.put("obtainedAt", bag.getObtainedAt());
            item.put("createdAt", bag.getCreatedAt());
            
            PlantDict plant = plantMap.get(bag.getPlantId());
            if (plant != null) {
                item.put("plantName", plant.getPlantName());
                item.put("imageUrl", plant.getImageUrl());
                item.put("rarity", calculateRarity(plant.getDropWeight()));
            }
            
            list.add(item);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", bagPage.getTotal());
        result.put("page", page);
        result.put("size", size);
        
        return Result.success(result);
    }

    /**
     * 获取指定用户的背包
     */
    @GetMapping("/user/{userId}")
    public Result<List<Map<String, Object>>> getUserBag(@PathVariable Long userId) {
        log.info("获取用户背包: userId={}", userId);
        
        LambdaQueryWrapper<UserBag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserBag::getUserId, userId)
               .orderByDesc(UserBag::getCreatedAt);
        
        List<UserBag> bagList = userBagMapper.selectList(wrapper);
        
        // 获取植物信息
        Map<Integer, PlantDict> plantMap = new HashMap<>();
        List<PlantDict> plants = plantDictMapper.selectList(null);
        for (PlantDict plant : plants) {
            plantMap.put(plant.getPlantId(), plant);
        }
        
        // 组装返回数据
        List<Map<String, Object>> list = new ArrayList<>();
        for (UserBag bag : bagList) {
            Map<String, Object> item = new HashMap<>();
            item.put("bagId", bag.getBagId());
            item.put("plantId", bag.getPlantId());
            item.put("status", bag.getStatus());
            item.put("obtainedAt", bag.getObtainedAt());
            item.put("createdAt", bag.getCreatedAt());
            
            PlantDict plant = plantMap.get(bag.getPlantId());
            if (plant != null) {
                item.put("plantName", plant.getPlantName());
                item.put("imageUrl", plant.getImageUrl());
                item.put("rarity", calculateRarity(plant.getDropWeight()));
            }
            
            list.add(item);
        }
        
        return Result.success(list);
    }

    /**
     * 为用户添加道具
     * 
     * @param body.userId 用户ID
     * @param body.plantId 植物ID（null 或 0 表示盲盒种子）
     * @param body.count 数量
     */
    @PostMapping("/add")
    public Result<Boolean> addItem(@RequestBody Map<String, Object> body) {
        Long userId = body.get("userId") != null ? Long.valueOf(body.get("userId").toString()) : null;
        Integer plantId = body.get("plantId") != null ? Integer.valueOf(body.get("plantId").toString()) : null;
        Integer count = body.get("count") != null ? Integer.valueOf(body.get("count").toString()) : 1;
        
        if (userId == null) {
            return Result.error("请选择用户");
        }
        
        log.info("为用户添加道具: userId={}, plantId={}, count={}", userId, plantId, count);
        
        // 添加道具
        for (int i = 0; i < count; i++) {
            UserBag bag = new UserBag();
            bag.setBagId(UUID.randomUUID().toString());
            bag.setUserId(userId);
            bag.setObtainedAt(System.currentTimeMillis());
            bag.setCreatedAt(System.currentTimeMillis());
            
            if (plantId == null || plantId == 0) {
                // 盲盒种子：plantId=0, status=0（需要解析）
                bag.setPlantId(0);
                bag.setStatus(0);
            } else {
                // 指定植物：直接设置 plantId，status=1（已解析，可直接使用）
                // 验证植物存在
                PlantDict plant = plantDictMapper.selectById(plantId);
                if (plant == null) {
                    return Result.error("植物不存在: " + plantId);
                }
                bag.setPlantId(plantId);
                bag.setStatus(1); // 已解析，可直接使用
            }
            
            userBagMapper.insert(bag);
        }
        
        return Result.success("添加成功", true);
    }

    /**
     * 删除背包物品
     */
    @DeleteMapping("/{bagId}")
    public Result<Boolean> deleteItem(@PathVariable String bagId) {
        log.info("删除背包物品: bagId={}", bagId);
        
        int rows = userBagMapper.deleteById(bagId);
        return rows > 0 
            ? Result.success("删除成功", true)
            : Result.error("删除失败");
    }
    
    /**
     * 批量删除背包物品
     */
    @DeleteMapping("/batch")
    public Result<Map<String, Object>> batchDelete(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> bagIds = (List<String>) body.get("bagIds");
        
        if (bagIds == null || bagIds.isEmpty()) {
            return Result.error("背包ID列表不能为空");
        }
        
        log.info("批量删除背包物品: {}", bagIds);
        
        int successCount = 0;
        for (String bagId : bagIds) {
            int rows = userBagMapper.deleteById(bagId);
            if (rows > 0) {
                successCount++;
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("successCount", successCount);
        result.put("totalRequested", bagIds.size());
        
        return Result.success("批量删除成功", result);
    }

    /**
     * 批量发放道具
     */
    @PostMapping("/batch")
    public Result<Map<String, Object>> batchAdd(@RequestBody Map<String, Object> body) {
        List<Long> userIds = new ArrayList<>();
        Object userIdsObj = body.get("userIds");
        if (userIdsObj instanceof List) {
            for (Object id : (List<?>) userIdsObj) {
                userIds.add(Long.valueOf(id.toString()));
            }
        }
        
        Integer plantId = body.get("plantId") != null ? Integer.valueOf(body.get("plantId").toString()) : null;
        Integer count = body.get("count") != null ? Integer.valueOf(body.get("count").toString()) : 1;
        
        if (userIds.isEmpty() || plantId == null) {
            return Result.error("参数不完整");
        }
        
        log.info("批量发放道具: userIds={}, plantId={}, count={}", userIds, plantId, count);
        
        // 验证植物存在
        PlantDict plant = plantDictMapper.selectById(plantId);
        if (plant == null) {
            return Result.error("植物不存在");
        }
        
        // 批量添加
        int successCount = 0;
        for (Long userId : userIds) {
            for (int i = 0; i < count; i++) {
                UserBag bag = new UserBag();
                bag.setBagId(UUID.randomUUID().toString());
                bag.setUserId(userId);
                bag.setPlantId(plantId);
                bag.setStatus(0);
                bag.setObtainedAt(System.currentTimeMillis());
                bag.setCreatedAt(System.currentTimeMillis());
                userBagMapper.insert(bag);
                successCount++;
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("totalItems", successCount);
        result.put("userCount", userIds.size());
        
        return Result.success("批量发放成功", result);
    }

    /**
     * 发放光流
     * 
     * @param body.userIds 用户ID列表
     * @param body.amount 光流数量（正数为增加，负数为减少）
     */
    @PostMapping("/flux")
    public Result<Map<String, Object>> addFlux(@RequestBody Map<String, Object> body) {
        List<Long> userIds = new ArrayList<>();
        Object userIdsObj = body.get("userIds");
        if (userIdsObj instanceof List) {
            for (Object id : (List<?>) userIdsObj) {
                userIds.add(Long.valueOf(id.toString()));
            }
        }
        
        Integer amount = body.get("amount") != null ? Integer.valueOf(body.get("amount").toString()) : null;
        
        if (userIds.isEmpty()) {
            return Result.error("请选择用户");
        }
        if (amount == null || amount == 0) {
            return Result.error("请输入有效的光流数量");
        }
        
        log.info("发放光流: userIds={}, amount={}", userIds, amount);
        
        int successCount = 0;
        List<String> failedUsers = new ArrayList<>();
        
        for (Long userId : userIds) {
            User user = userMapper.selectById(userId);
            if (user == null) {
                failedUsers.add("用户" + userId + "不存在");
                continue;
            }
            
            int newFlux = (user.getTimeFlux() != null ? user.getTimeFlux() : 0) + amount;
            
            // 如果是扣减，检查余额是否足够
            if (amount < 0 && newFlux < 0) {
                failedUsers.add(user.getNickname() + "余额不足");
                continue;
            }
            
            user.setTimeFlux(Math.max(0, newFlux));
            user.setUpdatedAt(System.currentTimeMillis());
            userMapper.updateById(user);
            successCount++;
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("successCount", successCount);
        result.put("userCount", userIds.size());
        if (!failedUsers.isEmpty()) {
            result.put("failedUsers", failedUsers);
        }
        
        String msg = amount > 0 
            ? String.format("成功为 %d 位用户发放 %d 光流", successCount, amount)
            : String.format("成功从 %d 位用户扣除 %d 光流", successCount, Math.abs(amount));
        
        return Result.success(msg, result);
    }

    /**
     * 获取背包统计
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getBagStats() {
        Long totalItems = userBagMapper.selectCount(null);
        
        // 按状态统计
        Map<Integer, Long> statusCount = new HashMap<>();
        List<UserBag> allBags = userBagMapper.selectList(null);
        for (UserBag bag : allBags) {
            statusCount.merge(bag.getStatus(), 1L, Long::sum);
        }
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalItems", totalItems);
        stats.put("seedCount", statusCount.getOrDefault(0, 0L));
        stats.put("plantCount", statusCount.getOrDefault(1, 0L));
        
        return Result.success(stats);
    }

    /**
     * 计算稀有度
     */
    private int calculateRarity(Integer dropWeight) {
        if (dropWeight == null) return 0;
        if (dropWeight >= 50) return 0;      // N
        if (dropWeight >= 20) return 1;      // R
        if (dropWeight >= 5) return 2;       // SR
        return 3;                             // SSR
    }
}
