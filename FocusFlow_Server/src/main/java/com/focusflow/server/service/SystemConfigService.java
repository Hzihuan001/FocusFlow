package com.focusflow.server.service;

import com.focusflow.server.entity.SystemConfig;

import java.util.List;
import java.util.Map;

/**
 * 系统配置服务接口
 */
public interface SystemConfigService {

    /**
     * 获取所有配置
     */
    List<SystemConfig> getAllConfigs();

    /**
     * 按分组获取配置
     */
    Map<String, List<SystemConfig>> getConfigsByGroup();

    /**
     * 根据键获取配置值
     */
    String getConfigValue(String key);

    /**
     * 根据键获取配置值（带默认值）
     */
    String getConfigValue(String key, String defaultValue);

    /**
     * 根据键获取整数配置值
     */
    int getIntConfig(String key, int defaultValue);

    /**
     * 更新配置
     */
    boolean updateConfig(String key, String value);

    /**
     * 批量更新配置
     */
    boolean batchUpdateConfig(Map<String, String> configs);

    /**
     * 新增配置
     */
    boolean addConfig(SystemConfig config);

    /**
     * 删除配置
     */
    boolean deleteConfig(Long configId);
}
