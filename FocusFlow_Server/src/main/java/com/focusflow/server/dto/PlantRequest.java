package com.focusflow.server.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 种植请求 DTO
 */
@Data
public class PlantRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * X坐标
     */
    @NotNull(message = "X坐标不能为空")
    private Integer x;

    /**
     * Y坐标
     */
    @NotNull(message = "Y坐标不能为空")
    private Integer y;

    /**
     * 植物ID
     */
    @NotNull(message = "植物ID不能为空")
    private Integer plantId;

    /**
     * 背包记录ID
     */
    private String bagRecordId;
}
