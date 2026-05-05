package com.focusflow.server.service;

import com.focusflow.server.dto.BagItemResponse;
import com.focusflow.server.entity.UserBag;
import com.focusflow.server.entity.PlantDict;
import com.focusflow.server.entity.User;

import java.util.List;

/**
 * 用户背包服务接口
 */
public interface UserBagService {

    /**
     * 获取用户背包列表
     *
     * @param userId 用户ID
     * @return 背包物品列表
     */
    List<BagItemResponse> getBagItems(Long userId);

    /**
     * 添加背包物品
     *
     * @param userId  用户ID
     * @param plantId 植物ID
     * @param status  状态
     * @return 背包记录ID
     */
    String addBagItem(Long userId, Integer plantId, Integer status);

    /**
     * 解析种子（开箱）
     * 将 status=0 的未解析种子随机分配植物
     *
     * @param userId 用户ID
     * @param bagId  背包记录ID
     * @return 解析后的植物ID
     */
    Integer openBox(Long userId, String bagId);

    /**
     * 更新背包物品状态
     *
     * @param userId 用户ID
     * @param bagId  背包记录ID
     * @param status 新状态
     * @return 是否成功
     */
    boolean updateStatus(Long userId, String bagId, Integer status);

    /**
     * 获取用户背包实体
     *
     * @param bagId 背包记录ID
     * @return 背包实体
     */
    UserBag getById(String bagId);
    
    /**
     * 获取植物字典
     *
     * @param plantId 植物ID
     * @return 植物字典实体
     */
    PlantDict getPlantDictById(Integer plantId);
    
    /**
     * 获取用户实体
     *
     * @param userId 用户ID
     * @return 用户实体
     */
    User getUserById(Long userId);
}
