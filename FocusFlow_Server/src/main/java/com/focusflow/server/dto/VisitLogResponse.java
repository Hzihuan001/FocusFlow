package com.focusflow.server.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 互访日志响应 DTO
 */
@Data
public class VisitLogResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 日志ID
     */
    private Long logId;

    /**
     * 访客ID
     */
    private Long visitorId;

    /**
     * 访客昵称
     */
    private String visitorNickname;

    /**
     * 访客头像ID
     */
    private Integer visitorAvatarId;

    /**
     * 互动类型（1=充能，2=留言）
     */
    private Integer actionType;

    /**
     * 留言内容
     */
    private String content;

    /**
     * 是否已读
     */
    private Integer isRead;

    /**
     * 创建时间
     */
    private Long createdAt;
}
