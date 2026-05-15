package com.focusflow.server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * AI 大模型代理服务 - 数据库配置版
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * 【重构说明】
 * API Key、模型名称、端点等配置从数据库 sys_config 表读取，
 * 支持后管端动态配置，无需重启服务。
 *
 * 【配置项】
 * - ai.api.key        : API 密钥
 * - ai.api.endpoint   : API 端点（可选，默认智谱）
 * - ai.model.name     : 模型名称
 * - ai.system.prompt  : 系统 Prompt
 * - ai.kill.switch    : 熔断开关
 * - ai.max.tokens     : 最大 Token 数
 * - ai.temperature    : 温度参数
 * - ai.timeout        : 超时时间
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ZhipuProxyService {

    private final SystemConfigService systemConfigService;
    
    // 非流式调用专用 RestTemplate，设置连接/读取超时，防止无限挂起
    private final RestTemplate restTemplate = buildRestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    // 配置键常量
    private static final String KEY_API_KEY = "ai.api.key";
    private static final String KEY_API_ENDPOINT = "ai.api.endpoint";
    private static final String KEY_MODEL_NAME = "ai.model.name";
    private static final String KEY_SYSTEM_PROMPT = "ai.system.prompt";
    private static final String KEY_KILL_SWITCH = "ai.kill.switch";
    private static final String KEY_MAX_TOKENS = "ai.max.tokens";
    private static final String KEY_TEMPERATURE = "ai.temperature";
    private static final String KEY_TIMEOUT = "ai.timeout";

    // 默认值
    private static final String DEFAULT_ENDPOINT = "https://open.bigmodel.cn/api/paas/v4/";
    private static final String DEFAULT_MODEL = "glm-4-flash";
    private static final String DEFAULT_PROMPT = "你是一个友善的学习助手，帮助用户解答学习问题，鼓励他们保持专注。";
    private static final int DEFAULT_MAX_TOKENS = 2048;
    private static final double DEFAULT_TEMPERATURE = 0.7;
    private static final int DEFAULT_TIMEOUT = 60;

    /**
     * 构建带超时配置的 RestTemplate（非流式调用专用）
     * 连接超时 30s，读取超时与数据库配置的 ai.timeout 一致（默认 60s）
     */
    private static RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30_000);  // 30 秒连接超时
        factory.setReadTimeout(60_000);     // 60 秒读取超时（兜底默认值）
        return new RestTemplate(factory);
    }

    /**
     * 获取 API Key
     */
    private String getApiKey() {
        return systemConfigService.getConfigValue(KEY_API_KEY, "");
    }

    /**
     * 获取 API 端点
     */
    private String getApiEndpoint() {
        String endpoint = systemConfigService.getConfigValue(KEY_API_ENDPOINT, "");
        return endpoint.isEmpty() ? DEFAULT_ENDPOINT : endpoint;
    }

    /**
     * 获取模型名称
     */
    private String getModelName() {
        return systemConfigService.getConfigValue(KEY_MODEL_NAME, DEFAULT_MODEL);
    }

    /**
     * 获取系统 Prompt
     */
    private String getSystemPrompt() {
        return systemConfigService.getConfigValue(KEY_SYSTEM_PROMPT, DEFAULT_PROMPT);
    }

    /**
     * 检查熔断开关
     */
    private boolean isKillSwitchOn() {
        return "true".equalsIgnoreCase(systemConfigService.getConfigValue(KEY_KILL_SWITCH, "false"));
    }

    /**
     * 获取最大 Token 数
     */
    private int getMaxTokens() {
        return systemConfigService.getIntConfig(KEY_MAX_TOKENS, DEFAULT_MAX_TOKENS);
    }

    /**
     * 获取温度参数
     */
    private double getTemperature() {
        String temp = systemConfigService.getConfigValue(KEY_TEMPERATURE, String.valueOf(DEFAULT_TEMPERATURE));
        try {
            return Double.parseDouble(temp);
        } catch (NumberFormatException e) {
            return DEFAULT_TEMPERATURE;
        }
    }

    /**
     * 获取超时时间（秒）
     */
    private int getTimeout() {
        return systemConfigService.getIntConfig(KEY_TIMEOUT, DEFAULT_TIMEOUT);
    }

    /**
     * 流式对话代理（SSE）
     * 
     * @param messages 消息列表（JSON 数组）
     * @param model 模型名称（可选，覆盖数据库配置）
     * @param userId 用户ID（用于日志记录）
     * @return SSE Emitter
     */
    public SseEmitter streamChat(String messages, String model, Long userId) {
        SseEmitter emitter = new SseEmitter(getTimeout() * 1000L);

        // 检查熔断开关
        if (isKillSwitchOn()) {
            log.warn("AI 服务已熔断 - 用户ID: {}", userId);
            executorService.execute(() -> {
                try {
                    emitter.send(SseEmitter.event().data("⚠️ AI 服务正在维护中，请稍后再试"));
                    emitter.complete();
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
            });
            return emitter;
        }

        // 检查 API Key
        String apiKey = getApiKey();
        if (apiKey.isEmpty()) {
            log.error("API Key 未配置 - 用户ID: {}", userId);
            executorService.execute(() -> {
                try {
                    emitter.send(SseEmitter.event().data("⚠️ AI 服务未配置，请联系管理员"));
                    emitter.complete();
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
            });
            return emitter;
        }

        String apiEndpoint = getApiEndpoint();
        String modelName = model != null ? model : getModelName();
        String systemPrompt = getSystemPrompt();

        executorService.execute(() -> {
            try {
                // 构建请求体
                ObjectNode requestBody = objectMapper.createObjectNode();
                requestBody.put("model", modelName);
                requestBody.put("stream", true);
                requestBody.put("max_tokens", getMaxTokens());
                requestBody.put("temperature", getTemperature());

                // 解析用户消息并注入系统 Prompt
                ArrayNode messagesArray = objectMapper.createArrayNode();
                
                // 添加系统 Prompt
                ObjectNode systemMessage = objectMapper.createObjectNode();
                systemMessage.put("role", "system");
                systemMessage.put("content", systemPrompt);
                messagesArray.add(systemMessage);
                
                // 添加用户消息（安全解析，过滤无效消息）
                try {
                    JsonNode userMessages = objectMapper.readTree(messages);
                    if (userMessages.isArray()) {
                        for (JsonNode msg : userMessages) {
                            // 验证消息格式，跳过包含控制字符的无效消息
                            if (msg.has("role") && msg.has("content")) {
                                String content = msg.path("content").asText("");
                                // 过滤掉错误消息和包含异常提示的消息
                                if (!content.startsWith("⚠️") && !content.contains("AI 服务异常")) {
                                    messagesArray.add(msg);
                                }
                            }
                        }
                    }
                } catch (Exception parseEx) {
                    log.warn("解析用户消息失败，使用空上下文: {}", parseEx.getMessage());
                    // 解析失败时只使用系统 prompt
                }
                requestBody.set("messages", messagesArray);

                URL url = new URL(apiEndpoint + "chat/completions");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Authorization", "Bearer " + apiKey);
                connection.setDoOutput(true);
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(getTimeout() * 1000);

                // 发送请求
                connection.getOutputStream().write(requestBody.toString().getBytes(StandardCharsets.UTF_8));
                connection.getOutputStream().flush();

                // 读取流式响应
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    StringBuilder fullResponse = new StringBuilder();
                    
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data: ")) {
                            String data = line.substring(6);
                            if ("[DONE]".equals(data)) {
                                emitter.send(SseEmitter.event().data("[DONE]"));
                                break;
                            }
                            try {
                                JsonNode jsonNode = objectMapper.readTree(data);
                                JsonNode choices = jsonNode.path("choices");
                                if (choices.isArray() && choices.size() > 0) {
                                    JsonNode delta = choices.get(0).path("delta");
                                    String content = delta.path("content").asText("");
                                    if (!content.isEmpty()) {
                                        fullResponse.append(content);
                                        emitter.send(SseEmitter.event().data(content));
                                    }
                                }
                            } catch (Exception e) {
                                log.debug("解析 SSE 数据异常: {}", e.getMessage());
                            }
                        }
                    }
                    
                    log.info("AI对话完成 - 用户ID: {}, 模型: {}, 响应长度: {}", userId, modelName, fullResponse.length());
                }

                emitter.complete();
            } catch (Exception e) {
                log.error("AI代理服务异常 - 用户ID: {}, 错误: {}", userId, e.getMessage());
                try {
                    // 转义异常消息中的控制字符，避免 JSON 解析错误
                    String safeMessage = e.getMessage()
                        .replace("\n", "\\n")
                        .replace("\r", "\\r")
                        .replace("\t", "\\t")
                        .replace("\"", "\\\"");
                    emitter.send(SseEmitter.event().data("⚠️ AI 服务异常: " + safeMessage));
                    emitter.complete();
                } catch (Exception ex) {
                    emitter.completeWithError(e);
                }
            }
        });

        emitter.onCompletion(() -> log.debug("SSE 连接关闭 - 用户ID: {}", userId));
        emitter.onTimeout(() -> log.warn("SSE 连接超时 - 用户ID: {}", userId));

        return emitter;
    }

    /**
     * 非流式对话代理
     */
    public String chat(String messages, String model) {
        // 检查熔断开关
        if (isKillSwitchOn()) {
            throw new RuntimeException("AI 服务已熔断");
        }

        String apiKey = getApiKey();
        if (apiKey.isEmpty()) {
            throw new RuntimeException("API Key 未配置");
        }

        String apiEndpoint = getApiEndpoint();
        String modelName = model != null ? model : getModelName();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", modelName);
            requestBody.put("stream", false);
            requestBody.put("max_tokens", getMaxTokens());
            requestBody.put("temperature", getTemperature());

            // 解析用户消息并注入系统 Prompt
            ArrayNode messagesArray = objectMapper.createArrayNode();
            
            ObjectNode systemMessage = objectMapper.createObjectNode();
            systemMessage.put("role", "system");
            systemMessage.put("content", getSystemPrompt());
            messagesArray.add(systemMessage);
            
            JsonNode userMessages = objectMapper.readTree(messages);
            if (userMessages.isArray()) {
                for (JsonNode msg : userMessages) {
                    messagesArray.add(msg);
                }
            }
            requestBody.set("messages", messagesArray);

            HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    apiEndpoint + "chat/completions",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            return response.getBody();
        } catch (Exception e) {
            log.error("AI代理服务异常: {}", e.getMessage());
            throw new RuntimeException("AI服务调用失败: " + e.getMessage());
        }
    }
}