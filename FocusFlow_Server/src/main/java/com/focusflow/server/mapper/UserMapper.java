package com.focusflow.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.focusflow.server.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户 Mapper 接口
 *
 * 【MyBatis-Plus 零 SQL 开发】
 * 继承 BaseMapper<User> 即可获得以下能力，无需编写任何 SQL：
 * - insert(entity): 插入一条记录
 * - deleteById(id): 根据 ID 删除
 * - updateById(entity): 根据 ID 更新
 * - selectById(id): 根据 ID 查询
 * - selectList(wrapper): 条件查询列表
 * - selectPage(page, wrapper): 分页查询
 *
 * 【性能优势】
 * MyBatis-Plus 会自动生成优化的 SQL，支持：
 * - 批量操作优化
 * - 自动分页
 * - 逻辑删除自动过滤
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 统计总用户数
     */
    @Select("SELECT COUNT(*) FROM biz_user WHERE deleted = 0")
    long countTotal();

    /**
     * 统计指定时间段内的新增用户数
     */
    @Select("SELECT COUNT(*) FROM biz_user WHERE deleted = 0 AND created_at >= #{startTime} AND created_at < #{endTime}")
    long countBetween(@Param("startTime") long startTime, @Param("endTime") long endTime);

    /**
     * 统计总光流余额
     */
    @Select("SELECT IFNULL(SUM(time_flux), 0) FROM biz_user WHERE deleted = 0")
    int sumTimeFlux();
}
