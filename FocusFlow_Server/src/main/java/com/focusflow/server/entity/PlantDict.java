package com.focusflow.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * 植物图鉴实体类
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * 【业务说明】
 * 由后台管理员动态维护的"数值策划表"。
 * App 启动时拉取此表，实现新植物上线免发版。
 */
@Data
@TableName("biz_plant_dict")
public class PlantDict implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 植物ID（主键，自增）
     */
    @TableId(value = "plant_id", type = IdType.AUTO)
    private Integer plantId;

    /**
     * 植物名称
     */
    private String plantName;

    /**
     * 植物背景故事/描述
     */
    private String description;

    /**
     * 盲盒掉落权重
     * 值越大越容易掉落，稀有植物设为极低值
     */
    private Integer dropWeight;

    /**
     * Android端UI贴图资源名
     * 如 plant_neon_tree
     */
    private String resourceCode;

    /**
     * 植物图片URL
     * 后台上传的植物贴图
     */
    private String imageUrl;

    /**
     * 植物主色调（十六进制）
     * 如 #00FF00
     */
    private String colorHex;

    /**
     * 净化辐射半径（格数）
     * 植物种下后，该坐标及其周围一定范围内的地块会被净化
     */
    private Integer purifyRange;

    /**
     * 植物占用区域
     * 1 = 1×1 格, 2 = 2×2 格, 3 = 3×3 格
     */
    private Integer width;

    /**
     * 植物显示缩放比例（百分比）
     * 100 = 原大小，50 = 缩小一半，200 = 放大两倍
     * 用于控制植物图片在花园中的显示大小，与占地(width)分离
     */
    private Integer scale;

    /**
     * 状态（0=下架，1=上架）
     */
    private Integer status;

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
