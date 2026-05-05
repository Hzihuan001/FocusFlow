package com.focusflow.server.controller;

import com.focusflow.server.common.Result;
import com.focusflow.server.dto.AuthResponse;
import com.focusflow.server.dto.LoginRequest;
import com.focusflow.server.dto.RegisterRequest;
import com.focusflow.server.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * 认证控制器
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * 【API 列表】
 * POST /api/auth/register - 用户注册
 * POST /api/auth/login    - 用户登录
 *
 * 【安全设计】
 * 1. 密码使用 SHA-256 加盐哈希，绝不存储明文
 * 2. 登录失败统一返回"账号或密码错误"，防止账号枚举攻击
 * 3. 参数校验使用 Jakarta Validation
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /**
     * 用户注册
     *
     * 【业务流程】
     * 1. 参数校验（账号格式、密码长度）
     * 2. 检查账号是否已存在
     * 3. 密码加盐哈希后存储
     * 4. 返回用户信息
     *
     * @param request 注册请求
     * @return 认证响应
     */
    @PostMapping("/register")
    public Result<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("用户注册请求: account={}", request.getAccount());
        
        try {
            AuthResponse response = userService.register(request);
            log.info("用户注册成功: userId={}, account={}", response.getUserId(), request.getAccount());
            return Result.success("注册成功", response);
        } catch (RuntimeException e) {
            log.warn("用户注册失败: account={}, reason={}", request.getAccount(), e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 用户登录
     *
     * 【安全设计】
     * 登录失败统一返回"账号或密码错误"，不区分账号不存在和密码错误。
     * 这样可以防止攻击者通过错误信息枚举系统中存在的账号。
     *
     * @param request 登录请求
     * @return 认证响应
     */
    @PostMapping("/login")
    public Result<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("用户登录请求: account={}", request.getAccount());
        
        try {
            AuthResponse response = userService.login(request);
            log.info("用户登录成功: userId={}, account={}", response.getUserId(), request.getAccount());
            return Result.success("登录成功", response);
        } catch (RuntimeException e) {
            log.warn("用户登录失败: account={}, reason={}", request.getAccount(), e.getMessage());
            return Result.error(e.getMessage());
        }
    }
}