package com.focusflow.server.service;

import com.focusflow.server.dto.AuthResponse;
import com.focusflow.server.dto.ChangePasswordRequest;
import com.focusflow.server.dto.LoginRequest;
import com.focusflow.server.dto.RegisterRequest;
import com.focusflow.server.dto.UpdateProfileRequest;
import com.focusflow.server.entity.User;

/**
 * 用户服务接口
 *
 * 【核心功能】
 * - 用户注册：账号唯一性校验 + 密码加盐哈希
 * - 用户登录：账号查询 + 密码验证
 */
public interface UserService {

    /**
     * 用户注册
     *
     * @param request 注册请求
     * @return 认证响应
     * @throws RuntimeException 账号已存在时抛出异常
     */
    AuthResponse register(RegisterRequest request);

    /**
     * 用户登录
     *
     * @param request 登录请求
     * @return 认证响应
     * @throws RuntimeException 账号不存在或密码错误时抛出异常
     */
    AuthResponse login(LoginRequest request);

    /**
     * 根据 ID 查询用户
     *
     * @param userId 用户ID
     * @return 用户实体
     */
    User getById(Long userId);

    /**
     * 增加用户光流
     *
     * @param userId 用户ID
     * @param amount 增加数量
     * @return 是否成功
     */
    boolean addTimeFlux(Long userId, Integer amount);

    /**
     * 扣减用户光流
     *
     * @param userId 用户ID
     * @param amount 扣减数量
     * @return 是否成功（余额不足返回 false）
     */
    boolean deductTimeFlux(Long userId, Integer amount);

    /**
     * 检查账号是否存在
     *
     * @param account 账号
     * @return 是否存在
     */
    boolean existsByAccount(String account);

    /**
     * 根据账号搜索用户（用于添加好友）
     *
     * @param account 用户账号
     * @return 用户信息
     * @throws RuntimeException 用户不存在时抛出异常
     */
    AuthResponse searchByAccount(String account);

    /**
     * 更新用户资料（昵称、头像）
     *
     * @param userId  用户ID
     * @param request 更新请求
     * @return 更新后的用户信息
     */
    AuthResponse updateProfile(Long userId, UpdateProfileRequest request);

    /**
     * 修改密码
     *
     * @param userId  用户ID
     * @param request 修改密码请求
     * @return 是否成功
     * @throws RuntimeException 原密码错误时抛出异常
     */
    boolean changePassword(Long userId, ChangePasswordRequest request);
}