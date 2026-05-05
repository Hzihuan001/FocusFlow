package com.focusflow.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * 排行榜条目响应
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * 【业务说明】
 * 排行榜条目，包含用户信息和点亮地块数量
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 用户账号
     */
    private String account;

    /**
     * 头像URL
     */
    private String avatarUrl;

    /**
     * 点亮地块数量（已净化地块数）
     */
    private Integer purifiedCount;

    /**
     * 排名
     */
    private Integer rank;
}
