package com.focusflow.server.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;

/**
 * 后台管理员实体类
 */
@Data
@TableName("sys_admin")
public class SysAdmin implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "admin_id", type = IdType.AUTO)
    private Long adminId;

    private String username;

    private String password;

    private String nickname;

    private String role;

    private Integer status;

    private Long lastLoginAt;

    private String lastLoginIp;

    private Long createdAt;

    private Long updatedAt;

    @TableLogic
    private Integer deleted;
}
