package com.focusflow.server.controller;

import com.focusflow.server.common.Result;
import com.focusflow.server.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * 系统配置控制器（APP端）
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * 【API 列表】
 * GET  /config     - 获取所有配置（Map格式）
 */
@Slf4j
@RestController
@RequestMapping("/config")
@RequiredArgsConstructor
public class ConfigController {

    private final SystemConfigService systemConfigService;

    /**
     * 获取所有配置（供APP端使用）
     * 返回 Map<configKey, configValue> 格式，方便APP端直接使用
     */
    @GetMapping
    public Result<Map<String, String>> getAllConfigs() {
        log.info("APP端获取系统配置");
        
        Map<String, String> configs = new HashMap<>();
        
        // 连续专注奖励
        configs.put("streak.bonus.3", systemConfigService.getConfigValue("streak.bonus.3", "10"));
        configs.put("streak.bonus.5", systemConfigService.getConfigValue("streak.bonus.5", "25"));
        configs.put("streak.bonus.7", systemConfigService.getConfigValue("streak.bonus.7", "50"));
        configs.put("streak.bonus.14", systemConfigService.getConfigValue("streak.bonus.14", "100"));
        configs.put("streak.bonus.21", systemConfigService.getConfigValue("streak.bonus.21", "200"));
        configs.put("streak.bonus.30", systemConfigService.getConfigValue("streak.bonus.30", "500"));
        
        // 专注奖励
        configs.put("focus.reward.per.minute", systemConfigService.getConfigValue("focus.reward.per.minute", "1"));
        
        // 种子配置
        String dropRate = systemConfigService.getConfigValue("focus.drop.base.rate", "0.5");
        configs.put("focus.drop.min.minutes", systemConfigService.getConfigValue("focus.drop.min.minutes", "15"));
        configs.put("focus.drop.base.rate", dropRate);
        log.info("种子掉落概率配置: focus.drop.base.rate = {}", dropRate);
        
        // 充能配置
        configs.put("charge.cost", systemConfigService.getConfigValue("charge.cost", "50"));
        
        return Result.success(configs);
    }
    
    /**
     * 获取单个配置值
     */
    @GetMapping("/{key}")
    public Result<String> getConfigValue(@PathVariable String key) {
        String value = systemConfigService.getConfigValue(key);
        if (value == null) {
            return Result.error("配置项不存在");
        }
        return Result.success(value);
    }
}
