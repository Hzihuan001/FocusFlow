package com.focusflow.server.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 留言请求 DTO
 */
@Data
public class LeaveMessageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 花园主人ID
     */
    @NotNull(message = "主人ID不能为空")
    private Long hostId;

    /**
     * 留言内容
     */
    @Size(max = 200, message = "留言内容最长200字")
    private String content;
}
