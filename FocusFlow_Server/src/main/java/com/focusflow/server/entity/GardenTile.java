package com.focusflow.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * 花园地块实体类
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * 【业务说明】
 * 支撑 2.5D 等轴测地图的云端存档，记录哪个坐标种了什么。
 *
 * 【惰性枯萎机制】
 * lastChargeTime：记录最后一次充能时间，客户端据此计算植物状态
 */
@Data
@TableName("biz_garden_tile")
public class GardenTile implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 地块ID（主键，自增）
     */
    @TableId(value = "tile_id", type = IdType.AUTO)
    private Long tileId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 网格X坐标
     */
    private Integer x;

    /**
     * 网格Y坐标
     */
    private Integer y;

    /**
     * 植物ID（荒芜地块为NULL）
     */
    private Integer plantId;

    /**
     * 关联的背包记录ID
     */
    private String bagRecordId;

    /**
     * 部署时间戳
     */
    private Long deployTime;

    /**
     * 最后一次充能时间戳
     */
    private Long lastChargeTime;

    /**
     * 是否已净化（0=否，1=是）
     */
    private Integer isPurified;

    /**
     * 创建时间戳
     */
    private Long createdAt;

    /**
     * 更新时间戳
     */
    private Long updatedAt;

    /**
     * 逻辑删除标记
     */
    @TableLogic
    private Integer deleted;
}
