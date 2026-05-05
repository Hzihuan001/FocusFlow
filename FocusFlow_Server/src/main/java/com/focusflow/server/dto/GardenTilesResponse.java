package com.focusflow.server.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 花园地块列表响应（包含用户活力状态）
 */
@Data
public class GardenTilesResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 地块列表
     */
    private List<GardenTileResponse> tiles;

    /**
     * 用户最后专注时间（毫秒时间戳）
     */
    private Long lastFocusTime;

    /**
     * 创建响应
     */
    public static GardenTilesResponse of(List<GardenTileResponse> tiles, Long lastFocusTime) {
        GardenTilesResponse response = new GardenTilesResponse();
        response.setTiles(tiles);
        response.setLastFocusTime(lastFocusTime);
        return response;
    }
}
