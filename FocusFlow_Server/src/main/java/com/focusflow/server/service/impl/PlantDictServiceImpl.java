package com.focusflow.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.focusflow.server.dto.PlantDictResponse;
import com.focusflow.server.entity.PlantDict;
import com.focusflow.server.mapper.PlantDictMapper;
import com.focusflow.server.service.PlantDictService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 植物图鉴服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlantDictServiceImpl implements PlantDictService {

    private final PlantDictMapper plantDictMapper;

    @Override
    public List<PlantDictResponse> getAllActivePlants() {
        // 查询所有上架植物（status=1）
        // 按 dropWeight 降序排序（权重高=稀有度低=N级在前）
        List<PlantDict> plants = plantDictMapper.selectList(
                new LambdaQueryWrapper<PlantDict>()
                        .eq(PlantDict::getStatus, 1)
                        .orderByDesc(PlantDict::getDropWeight)
        );

        // Entity -> DTO 转换
        return plants.stream()
                .map(PlantDictResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public PlantDict getById(Integer plantId) {
        return plantDictMapper.selectById(plantId);
    }
}
