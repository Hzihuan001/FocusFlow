package com.focusflow.server.service;

import com.focusflow.server.dto.PlantDictResponse;
import com.focusflow.server.entity.PlantDict;

import java.util.List;

/**
 * 植物图鉴服务接口
 */
public interface PlantDictService {

    /**
     * 获取所有上架植物
     *
     * @return 植物列表
     */
    List<PlantDictResponse> getAllActivePlants();

    /**
     * 根据 ID 查询植物
     *
     * @param plantId 植物ID
     * @return 植物实体
     */
    PlantDict getById(Integer plantId);
}
