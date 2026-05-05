package com.focusflow.server.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 花园地块响应 DTO
 */
@Data
public class GardenTileResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 地块ID
     */
    private Long tileId;

    /**
     * X坐标
     */
    private Integer x;

    /**
     * Y坐标
     */
    private Integer y;

    /**
     * 植物ID
     */
    private Integer plantId;

    /**
     * 植物名称
     */
    private String plantName;

    /**
     * 植物颜色
     */
    private String plantColor;

    /**
     * 植物图片URL
     */
    private String imageUrl;

    /**
     * 背包记录ID
     */
    private String bagRecordId;

    /**
     * 部署时间
     */
    private Long deployTime;

    /**
     * 最后充能时间
     */
    private Long lastChargeTime;

    /**
     * 是否已净化
     */
    private Boolean isPurified;

    /**
     * 植物占用区域（width×width 正方形）
     */
    private Integer width;

    /**
     * 从 Entity 转换
     */
    public static GardenTileResponse fromEntity(com.focusflow.server.entity.GardenTile entity) {
        GardenTileResponse response = new GardenTileResponse();
        response.setTileId(entity.getTileId());
        response.setX(entity.getX());
        response.setY(entity.getY());
        response.setPlantId(entity.getPlantId());
        response.setBagRecordId(entity.getBagRecordId());
        response.setDeployTime(entity.getDeployTime());
        response.setLastChargeTime(entity.getLastChargeTime());
        response.setIsPurified(entity.getIsPurified() != null && entity.getIsPurified() == 1);
        return response;
    }
}
