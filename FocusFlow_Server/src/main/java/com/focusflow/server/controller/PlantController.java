package com.focusflow.server.controller;

import com.focusflow.server.common.Result;
import com.focusflow.server.dto.PlantDictResponse;
import com.focusflow.server.service.PlantDictService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * 植物图鉴控制器
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * 【API 列表】
 * GET /api/plants - 查询所有上架植物
 *
 * 【设计理念】
 * 植物图鉴由后台管理员配置，App 启动时拉取，实现新植物上线免发版。
 */
@Slf4j
@RestController
@RequestMapping("/plants")
@RequiredArgsConstructor
public class PlantController {

    private final PlantDictService plantDictService;

    /**
     * 查询所有上架植物
     *
     * @return 植物列表
     */
    @GetMapping
    public Result<List<PlantDictResponse>> getAllPlants() {
        log.info("查询植物图鉴列表");
        
        List<PlantDictResponse> plants = plantDictService.getAllActivePlants();
        return Result.success(plants);
    }
}