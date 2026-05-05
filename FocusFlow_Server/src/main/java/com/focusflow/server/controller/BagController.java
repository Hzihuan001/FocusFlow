package com.focusflow.server.controller;

import com.focusflow.server.common.Result;
import com.focusflow.server.dto.BagItemResponse;
import com.focusflow.server.entity.UserBag;
import com.focusflow.server.service.UserBagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 背包控制器
 */
@Slf4j
@RestController
@RequestMapping("/bag")
@RequiredArgsConstructor
public class BagController {

    private final UserBagService userBagService;
    
    /**
     * 根据掉落权重推算稀有度
     */
    private int calculateRarityByWeight(int dropWeight) {
        if (dropWeight >= 30) return 0;  // N
        if (dropWeight >= 15) return 1;  // R
        if (dropWeight >= 5) return 2;   // SR
        return 3;                        // SSR
    }

    /**
     * 获取用户背包列表
     *
     * @param userId 用户ID (Header)
     * @return 背包物品列表
     */
    @GetMapping("/list")
    public Result<List<BagItemResponse>> getBagList(
            @RequestHeader("X-User-Id") Long userId
    ) {
        log.info("获取背包列表: userId={}", userId);
        List<BagItemResponse> items = userBagService.getBagItems(userId);
        return Result.success(items);
    }

    /**
     * 添加背包物品
     *
     * @param userId 用户ID (Header)
     * @param body   请求体 {plantId, status}
     * @return 背包记录ID
     * 
     * 【盲盒机制】
     * - plantId = 0: 表示未解析的种子，解析时随机决定植物
     * - plantId > 0: 已确定植物的种子
     */
    @PostMapping("/add")
    public Result<Map<String, String>> addBagItem(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody Map<String, Integer> body
    ) {
        Integer plantId = body.get("plantId");
        Integer status = body.getOrDefault("status", 0);
        
        // 🔧 [BLIND-BOX] 允许 plantId=0 表示未解析的种子
        if (plantId == null) {
            plantId = 0; // 默认为未解析状态
        }
        
        log.info("添加背包物品: userId={}, plantId={}, status={}", userId, plantId, status);
        String bagId = userBagService.addBagItem(userId, plantId, status);
        
        return Result.success("添加成功", Map.of("bagId", bagId));
    }

    /**
     * 开箱（解析种子）
     *
     * @param userId 用户ID (Header)
     * @param bagId  背包记录ID
     * @return 解析结果 {bagId, plantId, plantName, rarity, timeFlux}
     */
    @PostMapping("/open/{bagId}")
    public Result<Map<String, Object>> openBox(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable String bagId
    ) {
        log.info("开箱: userId={}, bagId={}", userId, bagId);
        
        try {
            Integer plantId = userBagService.openBox(userId, bagId);
            
            // 获取背包物品和植物信息
            UserBag bag = userBagService.getById(bagId);
            var plantDict = userBagService.getPlantDictById(plantId);
            
            // 获取用户最新光流余额
            var user = userBagService.getUserById(userId);
            Integer remainingTimeFlux = user != null ? user.getTimeFlux() : 0;
            
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("bagId", bagId);
            result.put("plantId", plantId);
            result.put("plantName", plantDict != null ? plantDict.getPlantName() : "未知植物");
            result.put("imageUrl", plantDict != null ? plantDict.getImageUrl() : null);
            // 🔧 [RARITY] 根据 drop_weight 推算稀有度
            result.put("rarity", plantDict != null ? calculateRarityByWeight(plantDict.getDropWeight()) : 0);
            result.put("timeFlux", remainingTimeFlux);
            
            return Result.success("解析成功", result);
        } catch (RuntimeException e) {
            log.warn("开箱失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新背包物品状态
     *
     * @param userId 用户ID (Header)
     * @param bagId  背包记录ID
     * @param body   请求体 {status}
     * @return 操作结果
     */
    @PutMapping("/status/{bagId}")
    public Result<Boolean> updateStatus(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable String bagId,
            @RequestBody Map<String, Integer> body
    ) {
        Integer status = body.get("status");
        if (status == null) {
            return Result.error("状态不能为空");
        }
        
        log.info("更新背包状态: userId={}, bagId={}, status={}", userId, bagId, status);
        boolean success = userBagService.updateStatus(userId, bagId, status);
        
        if (success) {
            return Result.success("更新成功", true);
        } else {
            return Result.error("更新失败，物品不存在或无权限");
        }
    }
}
