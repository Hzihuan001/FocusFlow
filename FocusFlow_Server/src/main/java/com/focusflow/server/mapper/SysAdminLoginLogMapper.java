package com.focusflow.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.focusflow.server.entity.SysAdminLoginLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 管理员登录日志Mapper
 */
@Mapper
public interface SysAdminLoginLogMapper extends BaseMapper<SysAdminLoginLog> {
}