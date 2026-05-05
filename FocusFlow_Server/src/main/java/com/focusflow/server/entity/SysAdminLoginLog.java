package com.focusflow.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;

/**
 * 管理员登录日志实体类
 */
@Data
@TableName("sys_admin_login_log")
public class SysAdminLoginLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "log_id", type = IdType.AUTO)
    private Long logId;

    private Long adminId;

    private String username;

    private String loginIp;

    private String userAgent;

    /**
     * 登录结果: 0=成功, 1=密码错误, 2=账号禁用
     */
    private Integer loginResult;

    private Long loginAt;
}
