package com.focusflow.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.focusflow.server.entity.Friendship;
import org.apache.ibatis.annotations.Mapper;

/**
 * 好友关系 Mapper 接口
 *
 * 继承 MyBatis-Plus BaseMapper，自动拥有 CRUD 能力
 */
@Mapper
public interface FriendshipMapper extends BaseMapper<Friendship> {
}
