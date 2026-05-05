package com.focusflow.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 植物创建/更新请求
 */
@Data
public class PlantDictRequest {
    
    @NotBlank(message = "植物名称不能为空")
    private String plantName;
    
    private String description;
    
    @NotNull(message = "掉落权重不能为空")
    private Integer dropWeight;
    
    private String resourceCode;
    
    private String colorHex;
    
    @NotNull(message = "净化范围不能为空")
    private Integer purifyRange;
    
    @NotNull(message = "占用面积不能为空")
    private Integer width;
    
    /**
     * 植物显示缩放比例
     * 10 = 原大小，20 = 放大一倍，5 = 缩小一倍
     */
    private Integer scale;
    
    private Integer status = 1;
    
    /** 图片URL（上传后返回） */
    private String imageUrl;
}
