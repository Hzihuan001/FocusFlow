package com.focusflow.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.focusflow.server.entity.UserBag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户背包 Mapper
 */
@Mapper
public interface UserBagMapper extends BaseMapper<UserBag> {

    /**
     * 查询用户的所有背包物品
     */
    @Select("SELECT * FROM biz_user_bag WHERE user_id = #{userId} AND deleted = 0 ORDER BY obtained_at DESC")
    List<UserBag> selectByUserId(@Param("userId") Long userId);

    /**
     * 查询用户指定状态的背包物品
     */
    @Select("SELECT * FROM biz_user_bag WHERE user_id = #{userId} AND status = #{status} AND deleted = 0")
    List<UserBag> selectByUserIdAndStatus(@Param("userId") Long userId, @Param("status") Integer status);

    /**
     * 统计用户指定状态物品数量
     */
    @Select("SELECT COUNT(*) FROM biz_user_bag WHERE user_id = #{userId} AND plant_id = #{plantId} AND status = #{status} AND deleted = 0")
    Integer countByUserIdAndPlantAndStatus(@Param("userId") Long userId, @Param("plantId") Integer plantId, @Param("status") Integer status);
}
