package com.focusflow.server.controller;

import com.focusflow.server.common.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * 健康检查控制器
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * 【用途】
 * 1. Android 端心跳检测，判断服务端是否可达
 * 2. 负载均衡健康检查
 * 3. 监控系统探活
 */
@Slf4j
@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
public class HealthController {

    /**
     * 健康检查接口
     *
     * @return 服务状态信息
     */
    @GetMapping
    public Result<Map<String, Object>> health() {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "UP");
        data.put("service", "focusflow-server");
        data.put("timestamp", LocalDateTime.now().toString());

        return Result.success("服务正常", data);
    }
}
