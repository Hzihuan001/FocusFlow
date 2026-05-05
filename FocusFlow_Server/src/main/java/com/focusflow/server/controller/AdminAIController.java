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
 * AI大模型配置控制器
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * 【API 列表】
 * GET  /admin/ai/config   - 获取AI配置（API配置、Prompt、熔断开关）
 * PUT  /admin/ai/config   - 更新API配置
 * PUT  /admin/ai/prompt   - 更新系统Prompt
 * PUT  /admin/ai/switch   - 切换熔断开关
 */
@Slf4j
@RestController
@RequestMapping("/admin/ai")
@RequiredArgsConstructor
public class AdminAIController {

    private final SystemConfigService systemConfigService;

    // 配置键
    private static final String KEY_PROMPT = "ai.system.prompt";
    private static final String KEY_KILL_SWITCH = "ai.kill.switch";
    private static final String KEY_API_PROVIDER = "ai.api.provider";
    private static final String KEY_API_KEY = "ai.api.key";
    private static final String KEY_API_ENDPOINT = "ai.api.endpoint";
    private static final String KEY_MODEL_NAME = "ai.model.name";
    private static final String KEY_MAX_TOKENS = "ai.max.tokens";
    private static final String KEY_TEMPERATURE = "ai.temperature";
    private static final String KEY_TIMEOUT = "ai.timeout";

    /**
     * 获取AI配置
     */
    @GetMapping("/config")
    public Result<Map<String, Object>> getAIConfig() {
        log.info("获取AI配置");
        
        Map<String, Object> config = new HashMap<>();
        
        // Prompt配置
        config.put("systemPrompt", systemConfigService.getConfigValue(KEY_PROMPT, 
            "你是一个友善的学习助手，帮助用户解答学习问题，鼓励他们保持专注。"));
        
        // 熔断开关
        config.put("killSwitch", "true".equalsIgnoreCase(
            systemConfigService.getConfigValue(KEY_KILL_SWITCH, "false")));
        
        // API配置
        config.put("apiProvider", systemConfigService.getConfigValue(KEY_API_PROVIDER, "zhipu"));
        config.put("apiKey", systemConfigService.getConfigValue(KEY_API_KEY, ""));
        config.put("apiEndpoint", systemConfigService.getConfigValue(KEY_API_ENDPOINT, ""));
        config.put("modelName", systemConfigService.getConfigValue(KEY_MODEL_NAME, "glm-4-flash"));
        config.put("maxTokens", Integer.parseInt(systemConfigService.getConfigValue(KEY_MAX_TOKENS, "2048")));
        config.put("temperature", Double.parseDouble(systemConfigService.getConfigValue(KEY_TEMPERATURE, "0.7")));
        config.put("timeout", Integer.parseInt(systemConfigService.getConfigValue(KEY_TIMEOUT, "30")));
        
        return Result.success(config);
    }

    /**
     * 更新API配置
     */
    @PutMapping("/config")
    public Result<Boolean> updateApiConfig(@RequestBody Map<String, Object> body) {
        log.info("更新AI API配置");
        
        try {
            if (body.get("apiProvider") != null) {
                systemConfigService.updateConfig(KEY_API_PROVIDER, body.get("apiProvider").toString());
            }
            if (body.get("apiKey") != null) {
                systemConfigService.updateConfig(KEY_API_KEY, body.get("apiKey").toString());
            }
            if (body.get("apiEndpoint") != null) {
                systemConfigService.updateConfig(KEY_API_ENDPOINT, body.get("apiEndpoint").toString());
            }
            if (body.get("modelName") != null) {
                systemConfigService.updateConfig(KEY_MODEL_NAME, body.get("modelName").toString());
            }
            if (body.get("maxTokens") != null) {
                systemConfigService.updateConfig(KEY_MAX_TOKENS, body.get("maxTokens").toString());
            }
            if (body.get("temperature") != null) {
                systemConfigService.updateConfig(KEY_TEMPERATURE, body.get("temperature").toString());
            }
            if (body.get("timeout") != null) {
                systemConfigService.updateConfig(KEY_TIMEOUT, body.get("timeout").toString());
            }
            
            return Result.success("API配置更新成功", true);
        } catch (Exception e) {
            log.error("更新API配置失败", e);
            return Result.error("更新失败: " + e.getMessage());
        }
    }

    /**
     * 更新系统Prompt
     */
    @PutMapping("/prompt")
    public Result<Boolean> updatePrompt(@RequestBody Map<String, String> body) {
        String prompt = body.get("prompt");
        if (prompt == null || prompt.trim().isEmpty()) {
            return Result.error("Prompt不能为空");
        }
        
        log.info("更新AI系统Prompt: length={}", prompt.length());
        
        boolean success = systemConfigService.updateConfig(KEY_PROMPT, prompt);
        return success 
            ? Result.success("Prompt更新成功", true)
            : Result.error("Prompt更新失败");
    }

    /**
     * 切换熔断开关
     */
    @PutMapping("/switch")
    public Result<Boolean> toggleKillSwitch(@RequestBody Map<String, Boolean> body) {
        Boolean enabled = body.get("enabled");
        if (enabled == null) {
            return Result.error("缺少enabled参数");
        }
        
        log.info("切换AI熔断开关: enabled={}", enabled);
        
        boolean success = systemConfigService.updateConfig(KEY_KILL_SWITCH, enabled.toString());
        
        String msg = enabled ? "AI功能已关闭" : "AI功能已开启";
        return success 
            ? Result.success(msg, true)
            : Result.error("开关切换失败");
    }
}