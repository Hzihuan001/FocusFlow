package com.focusflow.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 注册请求 DTO
 *
 * 【参数校验】
 * - account: 必填，4-20位，仅允许字母、数字、下划线
 * - password: 必填，6-32位
 * - nickname: 可选，不填则自动生成
 */
@Data
public class RegisterRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 登录账号
     * 规则：4-20位，仅允许字母、数字、下划线
     */
    @NotBlank(message = "账号不能为空")
    @Size(min = 4, max = 20, message = "账号长度必须在4-20位之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "账号只能包含字母、数字和下划线")
    private String account;

    /**
     * 密码
     * 规则：6-32位
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度必须在6-32位之间")
    private String password;

    /**
     * 昵称（可选）
     * 不填则自动生成
     */
    @Size(max = 20, message = "昵称最长20位")
    private String nickname;
}
