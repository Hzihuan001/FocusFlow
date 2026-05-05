package com.focusflow.server.dto;

import lombok.Data;

/**
 * 配置更新请求
 */
@Data
public class ConfigUpdateRequest {
    private String configKey;
    private String configValue;
}
