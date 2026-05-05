package com.focusflow.server.controller;

import com.focusflow.server.common.Result;
import com.focusflow.server.service.ZhipuProxyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI 助手代理控制器
 * 
 * 【安全设计】
 * API Key 不暴露给客户端，所有 AI 请求通过后端代理转发
 * 
 * 【API 列表】
 * POST /ai/chat/stream - 流式对话（SSE）
 * POST /ai/chat        - 非流式对话
 */
@Slf4j
@RestController
@RequestMapping("/ai")
public class AiProxyController {

    @Autowired
    private ZhipuProxyService zhipuProxyService;

    /**
     * 流式对话接口（SSE）
     * 
     * @param messages JSON 格式的消息列表
     * @param model 模型名称（可选，默认 glm-4-flash）
     * @param userId 用户ID（从请求头或参数获取）
     * @return SSE 流
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(
            @RequestBody String messages,
            @RequestParam(required = false) String model,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        
        log.info("收到AI流式对话请求 - 用户ID: {}, 模型: {}", userId, model);
        return zhipuProxyService.streamChat(messages, model, userId);
    }

    /**
     * 非流式对话接口
     */
    @PostMapping("/chat")
    public Result<String> chat(
            @RequestBody String messages,
            @RequestParam(required = false) String model) {
        
        log.info("收到AI对话请求 - 模型: {}", model);
        String response = zhipuProxyService.chat(messages, model);
        return Result.success("对话成功", response);
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("AI服务正常", "ok");
    }
}
