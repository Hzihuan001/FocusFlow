package com.focusflow.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 添加好友请求 DTO
 */
@Data
public class AddFriendRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 好友的用户ID
     */
    @NotNull(message = "好友ID不能为空")
    private Long friendId;
}
