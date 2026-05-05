package com.focusflow.server.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 用户背包实体
 * 表名: biz_user_bag
 */
@Data
@TableName("biz_user_bag")
public class UserBag {

    /**
     * 背包物品ID（UUID）
     */
    @TableId
    private String bagId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 植物字典ID
     */
    private Integer plantId;

    /**
     * 状态
     * 0 = 未解析种子
     * 1 = 成熟植物
     * 2 = 已种植
     */
    private Integer status;

    /**
     * 获取时间戳
     */
    private Long obtainedAt;

    /**
     * 创建时间戳
     */
    private Long createdAt;

    /**
     * 逻辑删除标记
     */
    @TableLogic
    private Integer deleted;
}
