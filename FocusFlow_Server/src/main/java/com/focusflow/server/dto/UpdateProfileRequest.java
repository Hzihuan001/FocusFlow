package com.focusflow.server.dto;

import lombok.Data;

/**
 * 更新用户资料请求
 */
@Data
public class UpdateProfileRequest {
    
    /**
     * 昵称（可选）
     */
    private String nickname;
    
    /**
     * 头像ID（可选）
     */
    private Integer avatarId;
}
