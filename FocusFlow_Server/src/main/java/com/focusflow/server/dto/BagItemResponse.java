package com.focusflow.server.dto;

import com.focusflow.server.entity.UserBag;
import lombok.Data;

/**
 * 背包物品响应 DTO
 */
@Data
public class BagItemResponse {

    /**
     * 背包记录ID
     */
    private String bagId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 植物ID
     */
    private Integer plantId;

    /**
     * 植物名称
     */
    private String plantName;

    /**
     * 植物稀有度
     */
    private Integer rarity;

    /**
     * 植物图片URL
     */
    private String imageUrl;

    /**
     * 状态 (0=未解析, 1=成熟, 2=已种植)
     */
    private Integer status;

    /**
     * 获取时间
     */
    private Long obtainedAt;

    /**
     * 从实体转换
     */
    public static BagItemResponse fromEntity(UserBag bag, String plantName, Integer rarity, String imageUrl) {
        BagItemResponse response = new BagItemResponse();
        response.setBagId(bag.getBagId());
        response.setUserId(bag.getUserId());
        response.setPlantId(bag.getPlantId());
        response.setPlantName(plantName);
        response.setRarity(rarity);
        response.setImageUrl(imageUrl);
        response.setStatus(bag.getStatus());
        response.setObtainedAt(bag.getObtainedAt());
        return response;
    }
}
