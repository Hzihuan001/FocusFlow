package com.focusflow.server.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 认证响应 DTO
 *
 * 注册和登录成功后返回的用户信息
 */
@Data
public class AuthResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 登录账号
     */
    private String account;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 头像ID
     */
    private Integer avatarId;

    /**
     * 光流余额
     */
    private Integer timeFlux;

    /**
     * 连续专注天数
     */
    private Integer streakDays;

    /**
     * 从 User 实体构建响应
     */
    public static AuthResponse fromEntity(com.focusflow.server.entity.User user) {
        AuthResponse response = new AuthResponse();
        response.setUserId(user.getUserId());
        response.setAccount(user.getAccount());
        response.setNickname(user.getNickname());
        response.setAvatarId(user.getAvatarId());
        response.setTimeFlux(user.getTimeFlux());
        response.setStreakDays(user.getStreakDays() != null ? user.getStreakDays() : 0);
        return response;
    }
}
