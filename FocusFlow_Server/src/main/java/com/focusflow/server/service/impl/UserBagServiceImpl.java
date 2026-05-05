package com.focusflow.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.focusflow.server.dto.BagItemResponse;
import com.focusflow.server.entity.PlantDict;
import com.focusflow.server.entity.UserBag;
import com.focusflow.server.mapper.PlantDictMapper;
import com.focusflow.server.mapper.UserBagMapper;
import com.focusflow.server.service.UserBagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 用户背包服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserBagServiceImpl implements UserBagService {

    private final UserBagMapper userBagMapper;
    private final PlantDictMapper plantDictMapper;
    private final com.focusflow.server.service.UserService userService;
    
    /**
     * 解析种子消耗的光流数量
     */
    private static final int OPEN_BOX_COST = 100;
    
    /**
     * 根据掉落权重推算稀有度
     * 
     * 稀有度分级：
     * - N (0):  drop_weight >= 30 → 高概率普通
     * - R (1):  drop_weight >= 15 → 中等概率稀有
     * - SR (2): drop_weight >= 5  → 低概率史诗
     * - SSR (3): drop_weight < 5  → 极低概率传说
     */
    private int calculateRarityByWeight(int dropWeight) {
        if (dropWeight >= 30) return 0;  // N
        if (dropWeight >= 15) return 1;  // R
        if (dropWeight >= 5) return 2;   // SR
        return 3;                        // SSR
    }

    @Override
    public List<BagItemResponse> getBagItems(Long userId) {
        // 获取用户背包物品
        List<UserBag> bags = userBagMapper.selectByUserId(userId);
        
        // 获取植物字典
        List<PlantDict> plants = plantDictMapper.selectList(null);
        Map<Integer, PlantDict> plantMap = plants.stream()
                .collect(Collectors.toMap(PlantDict::getPlantId, p -> p));
        
        // 转换为响应 DTO
        List<BagItemResponse> result = new ArrayList<>();
        for (UserBag bag : bags) {
            // 🔧 [BLIND-BOX] plantId=0 表示未解析的种子
            if (bag.getPlantId() == null || bag.getPlantId() == 0) {
                result.add(BagItemResponse.fromEntity(bag, "未解析种子", null, null));
            } else {
                PlantDict plant = plantMap.get(bag.getPlantId());
                String plantName = plant != null ? plant.getPlantName() : "未知植物";
                // 🔧 [RARITY] 根据 drop_weight 推算稀有度
                Integer rarity = plant != null ? calculateRarityByWeight(plant.getDropWeight()) : 0;
                String imageUrl = plant != null ? plant.getImageUrl() : null;
                result.add(BagItemResponse.fromEntity(bag, plantName, rarity, imageUrl));
            }
        }
        
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String addBagItem(Long userId, Integer plantId, Integer status) {
        String bagId = UUID.randomUUID().toString();
        long currentTime = System.currentTimeMillis();
        
        UserBag bag = new UserBag();
        bag.setBagId(bagId);
        bag.setUserId(userId);
        bag.setPlantId(plantId);
        bag.setStatus(status);
        bag.setObtainedAt(currentTime);
        bag.setCreatedAt(currentTime);
        bag.setDeleted(0);
        
        userBagMapper.insert(bag);
        
        log.info("添加背包物品: bagId={}, userId={}, plantId={}, status={}", bagId, userId, plantId, status);
        
        return bagId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer openBox(Long userId, String bagId) {
        // 查询背包物品
        UserBag bag = userBagMapper.selectById(bagId);
        if (bag == null || !bag.getUserId().equals(userId)) {
            throw new RuntimeException("背包物品不存在");
        }
        
        if (bag.getStatus() != 0) {
            throw new RuntimeException("该物品已解析，无法重复操作");
        }
        
        // 🔧 [DEDUCT TIMEFLUX] 扣减光流（在开箱前）
        boolean deducted = userService.deductTimeFlux(userId, OPEN_BOX_COST);
        if (!deducted) {
            throw new RuntimeException("光流不足，需要 " + OPEN_BOX_COST + " 光流");
        }
        
        Integer resultPlantId;
        
        // 🔧 [FIX] 如果 plantId 已有值（非0），说明是指定植物种子，直接更新状态
        if (bag.getPlantId() != null && bag.getPlantId() != 0) {
            resultPlantId = bag.getPlantId();
            log.info("指定植物种子解析: bagId={}, userId={}, plantId={}", bagId, userId, resultPlantId);
        } else {
            // 盲盒种子：随机分配植物
            List<PlantDict> plants = plantDictMapper.selectList(null);
            if (plants.isEmpty()) {
                throw new RuntimeException("植物字典为空，无法解析");
            }
            
            // 加权随机选择植物
            int totalWeight = plants.stream().mapToInt(PlantDict::getDropWeight).sum();
            int random = ThreadLocalRandom.current().nextInt(totalWeight);
            int cumulative = 0;
            PlantDict selectedPlant = plants.get(0);
            
            for (PlantDict plant : plants) {
                cumulative += plant.getDropWeight();
                if (random < cumulative) {
                    selectedPlant = plant;
                    break;
                }
            }
            
            resultPlantId = selectedPlant.getPlantId();
            bag.setPlantId(resultPlantId);
            
            // 🔧 [RARITY] 根据 drop_weight 推算稀有度
            int rarity = calculateRarityByWeight(selectedPlant.getDropWeight());
            
            log.info("盲盒种子解析: bagId={}, userId={}, plantId={}, plantName={}, dropWeight={}, rarity={}", 
                    bagId, userId, selectedPlant.getPlantId(), 
                    selectedPlant.getPlantName(), selectedPlant.getDropWeight(), rarity);
        }
        
        // 更新背包物品状态
        bag.setStatus(1); // 已解析
        userBagMapper.updateById(bag);
        
        return resultPlantId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(Long userId, String bagId, Integer status) {
        int updated = userBagMapper.update(null,
                new LambdaUpdateWrapper<UserBag>()
                        .eq(UserBag::getBagId, bagId)
                        .eq(UserBag::getUserId, userId)
                        .set(UserBag::getStatus, status)
        );
        
        if (updated > 0) {
            log.info("更新背包物品状态: bagId={}, userId={}, status={}", bagId, userId, status);
        }
        
        return updated > 0;
    }

    @Override
    public UserBag getById(String bagId) {
        return userBagMapper.selectById(bagId);
    }
    
    @Override
    public com.focusflow.server.entity.PlantDict getPlantDictById(Integer plantId) {
        return plantDictMapper.selectById(plantId);
    }
    
    @Override
    public com.focusflow.server.entity.User getUserById(Long userId) {
        return userService.getById(userId);
    }
}
