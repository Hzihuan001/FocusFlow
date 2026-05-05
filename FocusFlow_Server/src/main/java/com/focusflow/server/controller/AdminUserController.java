package com.focusflow.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.focusflow.server.common.Result;
import com.focusflow.server.common.SecurityUtils;
import com.focusflow.server.entity.User;
import com.focusflow.server.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * 用户管理控制器
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * 【API 列表】
 * GET  /admin/user           - 分页获取用户列表
 * GET  /admin/user/{id}      - 获取用户详情
 * PUT  /admin/user/{id}/flux - 更新用户光流
 * PUT  /admin/user/{id}/ban  - 封禁用户
 * PUT  /admin/user/{id}/unban - 解封用户
 * PUT  /admin/user/batch/ban - 批量封禁
 * PUT  /admin/user/batch/unban - 批量解封
 * PUT  /admin/user/batch/flux - 批量修改光流
 * GET  /admin/user/stats     - 获取用户统计
 */
@Slf4j
@RestController
@RequestMapping("/admin/user")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserMapper userMapper;

    /**
     * 分页获取用户列表
     */
    @GetMapping
    public Result<Map<String, Object>> getUserList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        log.info("获取用户列表: page={}, size={}, keyword={}", page, size, keyword);
        
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w
                .like(User::getAccount, keyword)
                .or()
                .like(User::getNickname, keyword)
            );
        }
        
        wrapper.orderByDesc(User::getCreatedAt);
        
        Page<User> userPage = userMapper.selectPage(new Page<>(page, size), wrapper);
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", userPage.getRecords());
        result.put("total", userPage.getTotal());
        result.put("page", page);
        result.put("size", size);
        
        return Result.success(result);
    }

    /**
     * 获取用户详情
     */
    @GetMapping("/{userId}")
    public Result<User> getUserDetail(@PathVariable Long userId) {
        log.info("获取用户详情: userId={}", userId);
        
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        
        // 清除敏感信息
        user.setPassword(null);
        return Result.success(user);
    }

    /**
     * 新增用户
     */
    @PostMapping
    public Result<Boolean> addUser(@RequestBody Map<String, Object> body) {
        String account = (String) body.get("account");
        String password = (String) body.get("password");
        String nickname = (String) body.get("nickname");
        Integer timeFlux = body.get("timeFlux") != null ? (Integer) body.get("timeFlux") : 0;
        
        log.info("新增用户: account={}, nickname={}", account, nickname);
        
        // 参数校验
        if (account == null || account.isEmpty()) {
            return Result.error("账号不能为空");
        }
        if (password == null || password.isEmpty()) {
            return Result.error("密码不能为空");
        }
        
        // 检查账号是否已存在
        LambdaQueryWrapper<User> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(User::getAccount, account);
        if (userMapper.selectCount(checkWrapper) > 0) {
            return Result.error("账号已存在");
        }
        
        // 创建用户
        User user = new User();
        user.setAccount(account);
        user.setPassword(SecurityUtils.hashPassword(password));
        user.setNickname(nickname != null && !nickname.isEmpty() ? nickname : account);
        user.setTimeFlux(timeFlux);
        user.setAvatarId(1);
        user.setStatus(0);
        user.setStreakDays(0);
        user.setCreatedAt(System.currentTimeMillis());
        user.setUpdatedAt(System.currentTimeMillis());
        
        int rows = userMapper.insert(user);
        return rows > 0 
            ? Result.success("用户创建成功", true)
            : Result.error("用户创建失败");
    }

    /**
     * 更新用户光流
     */
    @PutMapping("/{userId}/flux")
    public Result<Boolean> updateTimeFlux(
            @PathVariable Long userId,
            @RequestParam Integer timeFlux) {
        log.info("更新用户光流: userId={}, timeFlux={}", userId, timeFlux);
        
        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(User::getUserId, userId)
               .set(User::getTimeFlux, Math.max(0, timeFlux))
               .set(User::getUpdatedAt, System.currentTimeMillis());
        
        int rows = userMapper.update(null, wrapper);
        return rows > 0 
            ? Result.success("光流更新成功", true)
            : Result.error("用户不存在");
    }

    /**
     * 封禁用户
     */
    @PutMapping("/{userId}/ban")
    public Result<Boolean> banUser(@PathVariable Long userId) {
        log.info("封禁用户: userId={}", userId);
        
        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(User::getUserId, userId)
               .set(User::getStatus, 1)
               .set(User::getUpdatedAt, System.currentTimeMillis());
        
        int rows = userMapper.update(null, wrapper);
        return rows > 0 
            ? Result.success("用户已封禁", true)
            : Result.error("用户不存在");
    }

    /**
     * 解封用户
     */
    @PutMapping("/{userId}/unban")
    public Result<Boolean> unbanUser(@PathVariable Long userId) {
        log.info("解封用户: userId={}", userId);
        
        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(User::getUserId, userId)
               .set(User::getStatus, 0)
               .set(User::getUpdatedAt, System.currentTimeMillis());
        
        int rows = userMapper.update(null, wrapper);
        return rows > 0 
            ? Result.success("用户已解封", true)
            : Result.error("用户不存在");
    }

    /**
     * 更新用户信息（账号、密码、昵称）
     */
    @PutMapping("/{userId}")
    public Result<Boolean> updateUserInfo(
            @PathVariable Long userId,
            @RequestBody Map<String, String> body) {
        String account = body.get("account");
        String password = body.get("password");
        String nickname = body.get("nickname");
        
        log.info("更新用户信息: userId={}, account={}, nickname={}", userId, account, nickname);
        
        // 检查账号是否已被其他用户占用
        if (account != null && !account.isEmpty()) {
            LambdaQueryWrapper<User> checkWrapper = new LambdaQueryWrapper<>();
            checkWrapper.eq(User::getAccount, account)
                       .ne(User::getUserId, userId);
            if (userMapper.selectCount(checkWrapper) > 0) {
                return Result.error("账号已被占用");
            }
        }
        
        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(User::getUserId, userId);
        
        if (account != null && !account.isEmpty()) {
            wrapper.set(User::getAccount, account);
        }
        if (password != null && !password.isEmpty()) {
            // 使用统一的 SecurityUtils 加密，与登录/注册保持一致
            String hashedPassword = SecurityUtils.hashPassword(password);
            wrapper.set(User::getPassword, hashedPassword);
        }
        if (nickname != null) {
            wrapper.set(User::getNickname, nickname);
        }
        wrapper.set(User::getUpdatedAt, System.currentTimeMillis());
        
        int rows = userMapper.update(null, wrapper);
        return rows > 0 
            ? Result.success("用户信息更新成功", true)
            : Result.error("用户不存在");
    }
    
    /**
     * 获取用户统计数据
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getUserStats() {
        log.info("获取用户统计数据");
        
        // 总用户数
        Long totalUsers = userMapper.selectCount(null);
        
        // 今日新增（简化处理，实际应按日期查询）
        long dayStart = System.currentTimeMillis() - (System.currentTimeMillis() % 86400000);
        LambdaQueryWrapper<User> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.ge(User::getCreatedAt, dayStart);
        Long todayNew = userMapper.selectCount(todayWrapper);
        
        // 活跃用户（最近7天有更新）
        long weekAgo = System.currentTimeMillis() - 7 * 86400000L;
        LambdaQueryWrapper<User> activeWrapper = new LambdaQueryWrapper<>();
        activeWrapper.ge(User::getUpdatedAt, weekAgo);
        Long activeUsers = userMapper.selectCount(activeWrapper);
        
        // 封禁用户
        LambdaQueryWrapper<User> bannedWrapper = new LambdaQueryWrapper<>();
        bannedWrapper.eq(User::getStatus, 1);
        Long bannedUsers = userMapper.selectCount(bannedWrapper);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("todayNew", todayNew);
        stats.put("activeUsers", activeUsers);
        stats.put("bannedUsers", bannedUsers);
        
        return Result.success(stats);
    }
    
    // ════════════════════════════════════════════════════════════════════════
    // 批量操作 API
    // ════════════════════════════════════════════════════════════════════════
    
    /**
     * 批量封禁用户
     */
    @PutMapping("/batch/ban")
    public Result<Map<String, Object>> batchBanUsers(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        java.util.List<Integer> userIds = (java.util.List<Integer>) body.get("userIds");
        
        if (userIds == null || userIds.isEmpty()) {
            return Result.error("用户ID列表不能为空");
        }
        
        log.info("批量封禁用户: {}", userIds);
        
        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(User::getUserId, userIds)
               .set(User::getStatus, 1)
               .set(User::getUpdatedAt, System.currentTimeMillis());
        
        int rows = userMapper.update(null, wrapper);
        
        Map<String, Object> result = new HashMap<>();
        result.put("successCount", rows);
        result.put("totalRequested", userIds.size());
        
        return Result.success("批量封禁成功", result);
    }
    
    /**
     * 批量解封用户
     */
    @PutMapping("/batch/unban")
    public Result<Map<String, Object>> batchUnbanUsers(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        java.util.List<Integer> userIds = (java.util.List<Integer>) body.get("userIds");
        
        if (userIds == null || userIds.isEmpty()) {
            return Result.error("用户ID列表不能为空");
        }
        
        log.info("批量解封用户: {}", userIds);
        
        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(User::getUserId, userIds)
               .set(User::getStatus, 0)
               .set(User::getUpdatedAt, System.currentTimeMillis());
        
        int rows = userMapper.update(null, wrapper);
        
        Map<String, Object> result = new HashMap<>();
        result.put("successCount", rows);
        result.put("totalRequested", userIds.size());
        
        return Result.success("批量解封成功", result);
    }
    
    /**
     * 批量修改用户光流
     */
    @PutMapping("/batch/flux")
    public Result<Map<String, Object>> batchUpdateFlux(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        java.util.List<Integer> userIds = (java.util.List<Integer>) body.get("userIds");
        Integer timeFlux = (Integer) body.get("timeFlux");
        String mode = (String) body.get("mode"); // "set" | "add" | "subtract"
        
        if (userIds == null || userIds.isEmpty()) {
            return Result.error("用户ID列表不能为空");
        }
        if (timeFlux == null) {
            return Result.error("光流数量不能为空");
        }
        
        log.info("批量修改光流: userIds={}, timeFlux={}, mode={}", userIds, timeFlux, mode);
        
        int successCount = 0;
        
        for (Integer userId : userIds) {
            User user = userMapper.selectById(userId);
            if (user != null) {
                int newFlux;
                if ("add".equals(mode)) {
                    newFlux = user.getTimeFlux() + timeFlux;
                } else if ("subtract".equals(mode)) {
                    newFlux = Math.max(0, user.getTimeFlux() - timeFlux);
                } else {
                    newFlux = Math.max(0, timeFlux); // 默认为设置
                }
                
                LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<>();
                wrapper.eq(User::getUserId, userId)
                       .set(User::getTimeFlux, newFlux)
                       .set(User::getUpdatedAt, System.currentTimeMillis());
                
                userMapper.update(null, wrapper);
                successCount++;
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("successCount", successCount);
        result.put("totalRequested", userIds.size());
        
        return Result.success("批量修改光流成功", result);
    }
}
