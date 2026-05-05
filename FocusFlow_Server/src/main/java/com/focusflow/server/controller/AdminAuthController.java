package com.focusflow.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.focusflow.server.common.Result;
import com.focusflow.server.entity.SysAdmin;
import com.focusflow.server.entity.SysAdminLoginLog;
import com.focusflow.server.mapper.SysAdminMapper;
import com.focusflow.server.mapper.SysAdminLoginLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * 后台管理员认证控制器
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * 【API 列表】
 * POST /admin/auth/login   - 管理员登录
 * POST /admin/auth/logout  - 退出登录
 * GET  /admin/auth/info    - 获取当前登录管理员信息
 * PUT  /admin/auth/password - 修改密码
 */
@Slf4j
@RestController
@RequestMapping("/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final SysAdminMapper sysAdminMapper;
    private final SysAdminLoginLogMapper loginLogMapper;

    private static final String SESSION_ADMIN_KEY = "ADMIN_USER";
    private static final String PASSWORD_SALT = "FocusFlow_Admin_2024";

    /**
     * 管理员登录
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(
            @RequestBody Map<String, String> body,
            HttpServletRequest request,
            HttpSession session) {
        
        String username = body.get("username");
        String password = body.get("password");
        String captcha = body.get("captcha");
        
        log.info("管理员登录尝试: username={}", username);
        
        // 验证码校验
        if (!CaptchaController.verifyCaptcha(session, captcha)) {
            return Result.error("验证码错误或已过期");
        }
        
        // 参数校验
        if (username == null || username.isEmpty()) {
            return Result.error("用户名不能为空");
        }
        if (password == null || password.isEmpty()) {
            return Result.error("密码不能为空");
        }
        
        // 查询管理员
        LambdaQueryWrapper<SysAdmin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysAdmin::getUsername, username);
        SysAdmin admin = sysAdminMapper.selectOne(wrapper);
        
        // 获取登录信息
        String loginIp = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        
        // 账号不存在
        if (admin == null) {
            saveLoginLog(null, username, loginIp, userAgent, 1); // 1=账号不存在
            return Result.error("用户名或密码错误");
        }
        
        // 检查账号状态
        if (admin.getStatus() != 0) {
            saveLoginLog(admin.getAdminId(), username, loginIp, userAgent, 2); // 2=账号禁用
            return Result.error("账号已被禁用");
        }
        
        // 验证密码
        String hashedPassword = hashPassword(password);
        if (!hashedPassword.equals(admin.getPassword())) {
            saveLoginLog(admin.getAdminId(), username, loginIp, userAgent, 3); // 3=密码错误
            return Result.error("用户名或密码错误");
        }
        
        // 登录成功，更新最后登录信息
        LambdaUpdateWrapper<SysAdmin> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SysAdmin::getAdminId, admin.getAdminId())
                     .set(SysAdmin::getLastLoginAt, System.currentTimeMillis())
                     .set(SysAdmin::getLastLoginIp, loginIp)
                     .set(SysAdmin::getUpdatedAt, System.currentTimeMillis());
        sysAdminMapper.update(null, updateWrapper);
        
        // 记录成功日志
        saveLoginLog(admin.getAdminId(), username, loginIp, userAgent, 0); // 0=成功
        
        // 存入Session
        session.setAttribute(SESSION_ADMIN_KEY, admin);
        
        log.info("管理员登录成功: adminId={}, username={}", admin.getAdminId(), username);
        
        // 返回用户信息（不含密码）
        Map<String, Object> result = new HashMap<>();
        result.put("adminId", admin.getAdminId());
        result.put("username", admin.getUsername());
        result.put("nickname", admin.getNickname());
        result.put("role", admin.getRole());
        
        return Result.success("登录成功", result);
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public Result<Boolean> logout(HttpSession session) {
        SysAdmin admin = (SysAdmin) session.getAttribute(SESSION_ADMIN_KEY);
        if (admin != null) {
            log.info("管理员退出登录: adminId={}, username={}", admin.getAdminId(), admin.getUsername());
        }
        session.invalidate();
        return Result.success("退出成功", true);
    }

    /**
     * 获取当前登录管理员信息
     */
    @GetMapping("/info")
    public Result<Map<String, Object>> getAdminInfo(HttpSession session) {
        SysAdmin admin = (SysAdmin) session.getAttribute(SESSION_ADMIN_KEY);
        
        if (admin == null) {
            return Result.error("未登录");
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("adminId", admin.getAdminId());
        result.put("username", admin.getUsername());
        result.put("nickname", admin.getNickname());
        result.put("role", admin.getRole());
        result.put("lastLoginAt", admin.getLastLoginAt());
        
        return Result.success(result);
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    public Result<Boolean> changePassword(
            @RequestBody Map<String, String> body,
            HttpSession session) {
        
        SysAdmin admin = (SysAdmin) session.getAttribute(SESSION_ADMIN_KEY);
        if (admin == null) {
            return Result.error("未登录");
        }
        
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        
        if (oldPassword == null || newPassword == null || newPassword.length() < 6) {
            return Result.error("参数不完整或新密码长度不足");
        }
        
        // 验证旧密码
        if (!hashPassword(oldPassword).equals(admin.getPassword())) {
            return Result.error("原密码错误");
        }
        
        // 更新密码
        LambdaUpdateWrapper<SysAdmin> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysAdmin::getAdminId, admin.getAdminId())
               .set(SysAdmin::getPassword, hashPassword(newPassword))
               .set(SysAdmin::getUpdatedAt, System.currentTimeMillis());
        
        int rows = sysAdminMapper.update(null, wrapper);
        if (rows > 0) {
            // 更新Session中的管理员信息
            admin.setPassword(hashPassword(newPassword));
            session.setAttribute(SESSION_ADMIN_KEY, admin);
            return Result.success("密码修改成功", true);
        }
        
        return Result.error("密码修改失败");
    }

    /**
     * 密码哈希
     */
    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String salted = password + PASSWORD_SALT;
            byte[] hash = md.digest(salted.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("密码哈希失败", e);
        }
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个代理时取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 保存登录日志
     */
    private void saveLoginLog(Long adminId, String username, String loginIp, String userAgent, int result) {
        try {
            SysAdminLoginLog log = new SysAdminLoginLog();
            log.setAdminId(adminId != null ? adminId : 0L);
            log.setUsername(username);
            log.setLoginIp(loginIp);
            log.setUserAgent(userAgent != null && userAgent.length() > 255 ? userAgent.substring(0, 255) : userAgent);
            log.setLoginResult(result);
            log.setLoginAt(System.currentTimeMillis());
            loginLogMapper.insert(log);
        } catch (Exception e) {
            AdminAuthController.log.error("保存登录日志失败", e);
        }
    }
}