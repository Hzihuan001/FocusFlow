package com.focusflow.server.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 好友信息响应 DTO
 */
@Data
public class FriendInfoResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 好友关系ID（用于同意/拒绝申请）
     */
    private Long friendshipId;

    /**
     * 好友用户ID
     */
    private Long userId;

    /**
     * 好友账号
     */
    private String account;

    /**
     * 好友昵称
     */
    private String nickname;

    /**
     * 好友头像ID
     */
    private Integer avatarId;

    /**
     * 好友光流余额
     */
    private Integer timeFlux;

    /**
     * 关系状态（0=申请中，1=已同意）
     */
    private Integer status;

    /**
     * 关系建立时间
     */
    private Long createdAt;

    /**
     * 从 User 实体构建好友信息
     */
    public static FriendInfoResponse fromEntity(com.focusflow.server.entity.User user, Integer status, Long createdAt) {
        return fromEntity(user, null, status, createdAt);
    }

    /**
     * 从 User 实体构建好友信息（包含 friendshipId）
     */
    public static FriendInfoResponse fromEntity(com.focusflow.server.entity.User user, Long friendshipId, Integer status, Long createdAt) {
        FriendInfoResponse response = new FriendInfoResponse();
        response.setFriendshipId(friendshipId);
        response.setUserId(user.getUserId());
        response.setAccount(user.getAccount());
        response.setNickname(user.getNickname());
        response.setAvatarId(user.getAvatarId());
        response.setTimeFlux(user.getTimeFlux());
        response.setStatus(status);
        response.setCreatedAt(createdAt);
        return response;
    }
}
