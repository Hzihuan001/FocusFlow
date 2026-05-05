package com.focusflow.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.focusflow.server.entity.FocusRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 专注记录 Mapper 接口
 *
 * 【离线优先架构支持】
 * recordId 使用 UUID，由 Android 端生成，确保：
 * 1. 断网时 Android 端可直接落库
 * 2. 网络恢复后同步到云端，主键无冲突
 */
@Mapper
public interface FocusRecordMapper extends BaseMapper<FocusRecord> {

    /**
     * 统计总专注时长（分钟）
     */
    @Select("SELECT IFNULL(SUM(duration_minutes), 0) FROM biz_focus_record WHERE deleted = 0")
    int sumTotalDuration();

    /**
     * 统计指定时间段内的专注时长
     */
    @Select("SELECT IFNULL(SUM(duration_minutes), 0) FROM biz_focus_record WHERE deleted = 0 AND start_time >= #{startTime} AND start_time < #{endTime}")
    int sumDurationBetween(@Param("startTime") long startTime, @Param("endTime") long endTime);

    /**
     * 统计总专注记录数
     */
    @Select("SELECT COUNT(*) FROM biz_focus_record WHERE deleted = 0")
    long countTotal();

    /**
     * 统计指定时间段内的专注记录数
     */
    @Select("SELECT COUNT(*) FROM biz_focus_record WHERE deleted = 0 AND start_time >= #{startTime} AND start_time < #{endTime}")
    long countBetween(@Param("startTime") long startTime, @Param("endTime") long endTime);

    /**
     * 统计指定时间段内的活跃用户数（DAU）
     */
    @Select("SELECT COUNT(DISTINCT user_id) FROM biz_focus_record WHERE deleted = 0 AND start_time >= #{startTime} AND start_time < #{endTime}")
    long countDistinctUsersBetween(@Param("startTime") long startTime, @Param("endTime") long endTime);

    /**
     * 统计每小时的专注分布（热力图数据）
     */
    @Select("SELECT HOUR(FROM_UNIXTIME(start_time/1000)) as hour, COUNT(*) as count, IFNULL(SUM(duration_minutes), 0) as duration " +
            "FROM biz_focus_record WHERE deleted = 0 AND start_time IS NOT NULL " +
            "GROUP BY HOUR(FROM_UNIXTIME(start_time/1000))")
    List<Map<String, Object>> getHourlyDistribution();

    /**
     * 获取专注模式分布
     * 根据时长推断：25分钟倍数=番茄，52/104分钟=52/17法则，90分钟倍数=沉浸
     */
    @Select("SELECT duration_minutes, COUNT(*) as count FROM biz_focus_record WHERE deleted = 0 AND duration_minutes IS NOT NULL GROUP BY duration_minutes")
    List<Map<String, Object>> getDurationDistribution();
}
