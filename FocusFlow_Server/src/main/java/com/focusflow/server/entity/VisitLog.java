package com.focusflow.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * 互访日志实体类
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * 【业务说明】
 * 记录访客在好友花园中的互动行为（充能、留言等）。
 *
 * 【互动类型】
 * action_type 1 = 充能（为好友植物补充活力）
 * action_type 2 = 留言（在好友花园留言板留言）
 */
@Data
@TableName("biz_visit_log")
public class VisitLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 日志ID（主键，自增）
     */
    @TableId(value = "log_id", type = IdType.AUTO)
    private Long logId;

    /**
     * 访客ID
     */
    private Long visitorId;

    /**
     * 花园主人ID
     */
    private Long hostId;

    /**
     * 互动类型（1=充能，2=留言）
     */
    private Integer actionType;

    /**
     * 留言内容（type=2时有效）
     */
    private String content;

    /**
     * 是否已读（0=未读，1=已读）
     */
    private Integer isRead;

    /**
     * 创建时间戳
     */
    private Long createdAt;

    /**
     * 逻辑删除标记
     */
    @TableLogic
    private Integer deleted;

    // ──────────────── 互动类型常量 ────────────────

    /** 充能互动 */
    public static final int ACTION_CHARGE = 1;

    /** 留言互动 */
    public static final int ACTION_MESSAGE = 2;
}
