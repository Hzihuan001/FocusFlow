package com.focusflow.server.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 植物图鉴响应 DTO
 */
@Data
public class PlantDictResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 植物ID
     */
    private Integer id;

    /**
     * 植物名称
     */
    private String name;

    /**
     * 植物描述
     */
    private String description;

    /**
     * 主色调
     */
    private String color;

    /**
     * 净化范围
     */
    private Integer purifyRange;

    /**
     * 植物图片URL
     */
    private String imageUrl;

    /**
     * 掉落权重
     */
    private Integer dropWeight;

    /**
     * 资源代码
     */
    private String resourceCode;

    /**
     * 植物占用区域：1=1×1, 2=2×2, 3=3×3
     */
    private Integer width;

    /**
     * 显示缩放比例（百分比）
     * 100=原大小, 50=缩小一半, 200=放大两倍
     */
    private Integer scale;

    /**
     * 稀有度（由 drop_weight 推算）
     */
    private Integer rarity;

    /**
     * 根据 drop_weight 推算稀有度
     * dropWeight范围1-1000，值越大越容易掉落，稀有度越低
     */
    private static int calculateRarity(int dropWeight) {
        if (dropWeight >= 301) return 0;  // N (普通) - 70%
        if (dropWeight >= 101) return 1;  // R (稀有) - 20%
        if (dropWeight >= 11) return 2;   // SR (史诗) - 9%
        return 3;                         // SSR (传说) - 1%
    }

    /**
     * 从 Entity 转换
     */
    public static PlantDictResponse fromEntity(com.focusflow.server.entity.PlantDict entity) {
        PlantDictResponse response = new PlantDictResponse();
        response.setId(entity.getPlantId());
        response.setName(entity.getPlantName());
        response.setDescription(entity.getDescription());
        response.setColor(entity.getColorHex());
        response.setPurifyRange(entity.getPurifyRange());
        response.setImageUrl(entity.getImageUrl());
        response.setDropWeight(entity.getDropWeight());
        response.setResourceCode(entity.getResourceCode());
        response.setWidth(entity.getWidth());
        response.setScale(entity.getScale() != null ? entity.getScale() : 100);
        // 根据 drop_weight 推算稀有度
        response.setRarity(calculateRarity(entity.getDropWeight()));
        return response;
    }
}
