package com.focusflow.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.focusflow.server.entity.VisitLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 互访日志 Mapper 接口
 *
 * 继承 MyBatis-Plus BaseMapper，自动拥有 CRUD 能力
 */
@Mapper
public interface VisitLogMapper extends BaseMapper<VisitLog> {
}
