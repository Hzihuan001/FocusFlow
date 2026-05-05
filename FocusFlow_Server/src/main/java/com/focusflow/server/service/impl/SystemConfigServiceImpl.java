package com.focusflow.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.focusflow.server.entity.SystemConfig;
import com.focusflow.server.mapper.SystemConfigMapper;
import com.focusflow.server.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统配置服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemConfigServiceImpl implements SystemConfigService {

    private final SystemConfigMapper systemConfigMapper;

    @Override
    public List<SystemConfig> getAllConfigs() {
        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SystemConfig::getConfigGroup)
               .orderByAsc(SystemConfig::getConfigKey);
        return systemConfigMapper.selectList(wrapper);
    }

    @Override
    public Map<String, List<SystemConfig>> getConfigsByGroup() {
        List<SystemConfig> configs = getAllConfigs();
        return configs.stream().collect(Collectors.groupingBy(
            c -> c.getConfigGroup() != null ? c.getConfigGroup() : "其他"
        ));
    }

    @Override
    public String getConfigValue(String key) {
        return getConfigValue(key, null);
    }

    @Override
    public String getConfigValue(String key, String defaultValue) {
        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfig::getConfigKey, key);
        SystemConfig config = systemConfigMapper.selectOne(wrapper);
        return config != null ? config.getConfigValue() : defaultValue;
    }

    @Override
    public int getIntConfig(String key, int defaultValue) {
        String value = getConfigValue(key);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warn("配置值不是有效整数: key={}, value={}", key, value);
            return defaultValue;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateConfig(String key, String value) {
        LambdaUpdateWrapper<SystemConfig> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SystemConfig::getConfigKey, key)
               .set(SystemConfig::getConfigValue, value)
               .set(SystemConfig::getUpdatedAt, System.currentTimeMillis());
        
        int rows = systemConfigMapper.update(null, wrapper);
        if (rows > 0) {
            log.info("配置更新成功: key={}, value={}", key, value);
        } else {
            log.warn("配置更新失败，配置项不存在: key={}", key);
        }
        return rows > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchUpdateConfig(Map<String, String> configs) {
        int successCount = 0;
        for (Map.Entry<String, String> entry : configs.entrySet()) {
            if (updateConfig(entry.getKey(), entry.getValue())) {
                successCount++;
            }
        }
        log.info("批量更新配置完成: 总数={}, 成功={}", configs.size(), successCount);
        return successCount == configs.size();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addConfig(SystemConfig config) {
        // 检查键是否已存在
        LambdaQueryWrapper<SystemConfig> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(SystemConfig::getConfigKey, config.getConfigKey());
        if (systemConfigMapper.selectCount(existWrapper) > 0) {
            log.warn("配置键已存在: key={}", config.getConfigKey());
            return false;
        }

        long currentTime = System.currentTimeMillis();
        config.setCreatedAt(currentTime);
        config.setUpdatedAt(currentTime);
        
        int rows = systemConfigMapper.insert(config);
        log.info("新增配置: key={}, value={}", config.getConfigKey(), config.getConfigValue());
        return rows > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteConfig(Long configId) {
        int rows = systemConfigMapper.deleteById(configId);
        log.info("删除配置: configId={}", configId);
        return rows > 0;
    }
}
