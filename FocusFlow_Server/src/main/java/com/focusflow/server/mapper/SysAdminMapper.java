package com.focusflow.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.focusflow.server.entity.SysAdmin;
import org.apache.ibatis.annotations.Mapper;

/**
 * 管理员Mapper
 */
@Mapper
public interface SysAdminMapper extends BaseMapper<SysAdmin> {
}
