package com.focusflow.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.focusflow.server.common.SecurityUtils;
import com.focusflow.server.dto.AuthResponse;
import com.focusflow.server.dto.ChangePasswordRequest;
import com.focusflow.server.dto.LoginRequest;
import com.focusflow.server.dto.RegisterRequest;
import com.focusflow.server.dto.UpdateProfileRequest;
import com.focusflow.server.entity.User;
import com.focusflow.server.mapper.UserMapper;
import com.focusflow.server.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * 用户服务实现类
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * 【安全设计要点】
 * 1. 密码绝不明文存储，使用 SHA-256 加盐哈希
 * 2. 注册时校验账号唯一性
 * 3. 登录时使用常量时间比较，防时序攻击
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthResponse register(RegisterRequest request) {
        String account = request.getAccount();
        long currentTime = System.currentTimeMillis();

        // ════════════════════════════════════════════════════════════════════════
        // Step 1: 检查账号是否已存在
        // ════════════════════════════════════════════════════════════════════════
        if (existsByAccount(account)) {
            log.warn("注册失败：账号已存在 account={}", account);
            throw new RuntimeException("账号已存在，请更换账号");
        }

        // ════════════════════════════════════════════════════════════════════════
        // Step 2: 创建用户，密码加盐哈希
        // ════════════════════════════════════════════════════════════════════════
        User user = new User();
        user.setAccount(account);
        
        // 【核心安全】密码加盐哈希，绝不存储明文！
        String hashedPassword = SecurityUtils.hashPassword(request.getPassword());
        user.setPassword(hashedPassword);
        
        // 昵称处理
        String nickname = request.getNickname();
        if (nickname == null || nickname.trim().isEmpty()) {
            int suffix = new Random().nextInt(9000) + 1000;
            nickname = "Focus_" + suffix;
        }
        user.setNickname(nickname);
        
        user.setAvatarId(1);
        user.setTimeFlux(0);
        user.setStatus(0);
        user.setCreatedAt(currentTime);
        user.setUpdatedAt(currentTime);

        userMapper.insert(user);

        log.info("用户注册成功: userId={}, account={}", user.getUserId(), account);

        return AuthResponse.fromEntity(user);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String account = request.getAccount();
        String rawPassword = request.getPassword();

        // ════════════════════════════════════════════════════════════════════════
        // Step 1: 根据账号查询用户
        // ════════════════════════════════════════════════════════════════════════
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getAccount, account)
        );

        if (user == null) {
            log.warn("登录失败：账号不存在 account={}", account);
            throw new RuntimeException("账号或密码错误");
        }

        // ════════════════════════════════════════════════════════════════════════
        // Step 2: 检查账号状态
        // ════════════════════════════════════════════════════════════════════════
        if (user.getStatus() != null && user.getStatus() == 1) {
            log.warn("登录失败：账号已封禁 account={}", account);
            throw new RuntimeException("账号已被封禁，请联系客服");
        }

        // ════════════════════════════════════════════════════════════════════════
        // Step 3: 验证密码（常量时间比较，防时序攻击）
        // ════════════════════════════════════════════════════════════════════════
        boolean passwordValid = SecurityUtils.verifyPassword(rawPassword, user.getPassword());
        
        if (!passwordValid) {
            log.warn("登录失败：密码错误 account={}", account);
            throw new RuntimeException("账号或密码错误");
        }

        // 更新最后登录时间
        user.setUpdatedAt(System.currentTimeMillis());
        userMapper.updateById(user);

        log.info("用户登录成功: userId={}, account={}", user.getUserId(), account);

        return AuthResponse.fromEntity(user);
    }

    @Override
    public User getById(Long userId) {
        return userMapper.selectById(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addTimeFlux(Long userId, Integer amount) {
        int updated = userMapper.update(null,
                new LambdaUpdateWrapper<User>()
                        .eq(User::getUserId, userId)
                        .setSql("time_flux = time_flux + " + amount)
                        .set(User::getUpdatedAt, System.currentTimeMillis())
        );
        return updated > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deductTimeFlux(Long userId, Integer amount) {
        // 先查询当前余额
        User user = userMapper.selectById(userId);
        if (user == null || user.getTimeFlux() < amount) {
            log.warn("扣减光流失败：余额不足 userId={}, current={}, required={}", 
                    userId, user != null ? user.getTimeFlux() : 0, amount);
            return false;
        }
        
        int updated = userMapper.update(null,
                new LambdaUpdateWrapper<User>()
                        .eq(User::getUserId, userId)
                        .ge(User::getTimeFlux, amount)  // 乐观锁：确保余额足够
                        .setSql("time_flux = time_flux - " + amount)
                        .set(User::getUpdatedAt, System.currentTimeMillis())
        );
        
        if (updated > 0) {
            log.info("扣减光流成功: userId={}, amount={}, remaining={}", 
                    userId, amount, user.getTimeFlux() - amount);
        }
        return updated > 0;
    }

    @Override
    public boolean existsByAccount(String account) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>()
                        .eq(User::getAccount, account)
        );
        return count != null && count > 0;
    }

    @Override
    public AuthResponse searchByAccount(String account) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getAccount, account)
        );
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        log.info("搜索用户成功: userId={}, account={}", user.getUserId(), account);
        return AuthResponse.fromEntity(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthResponse updateProfile(Long userId, UpdateProfileRequest request) {
        // 查询用户
        User user = userMapper.selectById(userId);
        if (user == null) {
            log.warn("更新资料失败：用户不存在 userId={}", userId);
            throw new RuntimeException("用户不存在");
        }

        boolean updated = false;

        // 更新昵称
        if (request.getNickname() != null && !request.getNickname().trim().isEmpty()) {
            String nickname = request.getNickname().trim();
            // 限制昵称长度
            if (nickname.length() > 20) {
                throw new RuntimeException("昵称不能超过20个字符");
            }
            user.setNickname(nickname);
            updated = true;
        }

        // 更新头像
        if (request.getAvatarId() != null) {
            if (request.getAvatarId() < 1 || request.getAvatarId() > 12) {
                throw new RuntimeException("头像ID无效");
            }
            user.setAvatarId(request.getAvatarId());
            updated = true;
        }

        if (updated) {
            user.setUpdatedAt(System.currentTimeMillis());
            userMapper.updateById(user);
            log.info("用户资料更新成功: userId={}, nickname={}, avatarId={}", 
                    userId, user.getNickname(), user.getAvatarId());
        }

        return AuthResponse.fromEntity(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean changePassword(Long userId, ChangePasswordRequest request) {
        // 查询用户
        User user = userMapper.selectById(userId);
        if (user == null) {
            log.warn("修改密码失败：用户不存在 userId={}", userId);
            throw new RuntimeException("用户不存在");
        }

        // 验证原密码
        boolean passwordValid = SecurityUtils.verifyPassword(request.getOldPassword(), user.getPassword());
        if (!passwordValid) {
            log.warn("修改密码失败：原密码错误 userId={}", userId);
            throw new RuntimeException("原密码错误");
        }

        // 验证新密码格式
        String newPassword = request.getNewPassword();
        if (newPassword == null || newPassword.length() < 6 || newPassword.length() > 32) {
            throw new RuntimeException("新密码长度必须在6-32位之间");
        }

        // 新密码不能与原密码相同
        if (SecurityUtils.verifyPassword(newPassword, user.getPassword())) {
            throw new RuntimeException("新密码不能与原密码相同");
        }

        // 更新密码
        String hashedPassword = SecurityUtils.hashPassword(newPassword);
        user.setPassword(hashedPassword);
        user.setUpdatedAt(System.currentTimeMillis());
        userMapper.updateById(user);

        log.info("密码修改成功: userId={}", userId);
        return true;
    }
}