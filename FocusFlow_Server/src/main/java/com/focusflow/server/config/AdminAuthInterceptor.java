package com.focusflow.server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focusflow.server.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * 后台管理员权限拦截器
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * 【功能说明】
 * 拦截所有 /admin/** 路径的请求，验证管理员是否已登录。
 * 未登录返回 401 状态码，前端应跳转到登录页。
 */
@Slf4j
@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    private static final String SESSION_ADMIN_KEY = "ADMIN_USER";
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // OPTIONS 请求直接放行（CORS 预检）
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        
        HttpSession session = request.getSession(false);
        
        // 检查是否已登录
        if (session == null || session.getAttribute(SESSION_ADMIN_KEY) == null) {
            log.warn("未授权访问: {} {}", request.getMethod(), request.getRequestURI());
            
            // 返回 401 JSON 响应
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            
            Result<Object> result = Result.error(401, "未登录或会话已过期");
            response.getWriter().write(objectMapper.writeValueAsString(result));
            
            return false;
        }
        
        return true;
    }
}
