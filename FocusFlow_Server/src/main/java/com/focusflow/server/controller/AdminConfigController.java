package com.focusflow.server.controller;

import com.focusflow.server.common.Result;
import com.focusflow.server.dto.ConfigUpdateRequest;
import com.focusflow.server.entity.SystemConfig;
import com.focusflow.server.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * 系统配置管理控制器
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * 【API 列表】
 * GET  /admin/config           - 获取所有配置
 * GET  /admin/config/grouped   - 获取分组配置
 * PUT  /admin/config           - 更新单个配置
 * PUT  /admin/config/batch     - 批量更新配置
 * POST /admin/config           - 新增配置
 * DELETE /admin/config/{id}    - 删除配置
 */
@Slf4j
@RestController
@RequestMapping("/admin/config")
@RequiredArgsConstructor
public class AdminConfigController {

    private final SystemConfigService systemConfigService;

    /**
     * 获取所有配置
     */
    @GetMapping
    public Result<List<SystemConfig>> getAllConfigs() {
        log.info("获取所有系统配置");
        List<SystemConfig> configs = systemConfigService.getAllConfigs();
        return Result.success(configs);
    }

    /**
     * 获取分组配置
     */
    @GetMapping("/grouped")
    public Result<Map<String, List<SystemConfig>>> getConfigsByGroup() {
        log.info("获取分组系统配置");
        Map<String, List<SystemConfig>> configs = systemConfigService.getConfigsByGroup();
        return Result.success(configs);
    }

    /**
     * 更新单个配置
     */
    @PutMapping
    public Result<Boolean> updateConfig(@RequestBody ConfigUpdateRequest request) {
        log.info("更新配置: key={}, value={}", request.getConfigKey(), request.getConfigValue());
        
        boolean success = systemConfigService.updateConfig(
            request.getConfigKey(), 
            request.getConfigValue()
        );
        
        return success 
            ? Result.success("配置更新成功", true)
            : Result.error("配置项不存在");
    }

    /**
     * 批量更新配置
     */
    @PutMapping("/batch")
    public Result<Boolean> batchUpdateConfig(@RequestBody List<ConfigUpdateRequest> requests) {
        log.info("批量更新配置: count={}", requests.size());
        
        Map<String, String> configMap = requests.stream()
            .collect(Collectors.toMap(
                ConfigUpdateRequest::getConfigKey,
                ConfigUpdateRequest::getConfigValue,
                (v1, v2) -> v2  // 重复key取后者
            ));
        
        boolean success = systemConfigService.batchUpdateConfig(configMap);
        return success 
            ? Result.success("批量更新成功", true)
            : Result.error("部分配置更新失败");
    }

    /**
     * 新增配置
     */
    @PostMapping
    public Result<Boolean> addConfig(@RequestBody SystemConfig config) {
        log.info("新增配置: key={}", config.getConfigKey());
        
        boolean success = systemConfigService.addConfig(config);
        return success 
            ? Result.success("配置新增成功", true)
            : Result.error("配置键已存在");
    }

    /**
     * 删除配置
     */
    @DeleteMapping("/{configId}")
    public Result<Boolean> deleteConfig(@PathVariable Long configId) {
        log.info("删除配置: configId={}", configId);
        
        boolean success = systemConfigService.deleteConfig(configId);
        return success 
            ? Result.success("配置删除成功", true)
            : Result.error("配置项不存在");
    }
}
