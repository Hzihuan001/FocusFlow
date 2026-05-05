package com.focusflow.server.controller;

import com.focusflow.server.common.Result;
import com.focusflow.server.dto.AuthResponse;
import com.focusflow.server.dto.ChangePasswordRequest;
import com.focusflow.server.dto.UpdateProfileRequest;
import com.focusflow.server.entity.User;
import com.focusflow.server.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 */
@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 查询用户信息
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    @GetMapping("/{userId}")
    public Result<AuthResponse> getUserInfo(@PathVariable Long userId) {
        log.info("查询用户信息: userId={}", userId);
        
        User user = userService.getById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }

        return Result.success(AuthResponse.fromEntity(user));
    }

    /**
     * 根据账号搜索用户（用于添加好友）
     *
     * @param account 用户账号
     * @return 用户信息
     */
    @GetMapping("/search")
    public Result<AuthResponse> searchUser(@RequestParam String account) {
        log.info("搜索用户: account={}", account);
        try {
            AuthResponse response = userService.searchByAccount(account);
            return Result.success(response);
        } catch (RuntimeException e) {
            log.warn("搜索用户失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新用户资料（昵称、头像）
     *
     * @param userId  用户ID（Header传递）
     * @param request 更新请求
     * @return 更新后的用户信息
     */
    @PutMapping("/profile")
    public Result<AuthResponse> updateProfile(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody UpdateProfileRequest request) {
        log.info("更新用户资料: userId={}, nickname={}, avatarId={}", 
                userId, request.getNickname(), request.getAvatarId());

        try {
            AuthResponse response = userService.updateProfile(userId, request);
            return Result.success("更新成功", response);
        } catch (RuntimeException e) {
            log.warn("更新用户资料失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 修改密码
     *
     * @param userId  用户ID（Header传递）
     * @param request 修改密码请求
     * @return 是否成功
     */
    @PutMapping("/password")
    public Result<Boolean> changePassword(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody ChangePasswordRequest request) {
        log.info("修改密码: userId={}", userId);

        try {
            boolean success = userService.changePassword(userId, request);
            return Result.success("密码修改成功", success);
        } catch (RuntimeException e) {
            log.warn("修改密码失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }
}
