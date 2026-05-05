package com.focusflow.server.service;

import com.focusflow.server.dto.LeaveMessageRequest;
import com.focusflow.server.dto.VisitLogResponse;

import java.util.List;

/**
 * 互访日志服务接口
 */
public interface VisitLogService {

    /**
     * 为好友花园充能
     *
     * @param visitorId 访客ID
     * @param hostId 花园主人ID
     * @return 是否成功
     */
    boolean chargeForFriend(Long visitorId, Long hostId);

    /**
     * 在好友花园留言
     *
     * @param visitorId 访客ID
     * @param request 留言请求
     * @return 是否成功
     */
    boolean leaveMessage(Long visitorId, LeaveMessageRequest request);

    /**
     * 获取花园的访客日志
     *
     * @param hostId 花园主人ID
     * @param limit 数量限制
     * @return 日志列表
     */
    List<VisitLogResponse> getGardenVisitLogs(Long hostId, int limit);

    /**
     * 获取未读通知数量
     *
     * @param userId 用户ID
     * @return 未读数量
     */
    int getUnreadCount(Long userId);

    /**
     * 标记所有通知为已读
     *
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean markAllAsRead(Long userId);

    /**
     * 检查今日是否已为某好友点亮花园（充能）
     *
     * @param visitorId 访客ID
     * @param hostId 花园主人ID
     * @return 今日是否已点亮
     */
    boolean hasChargedToday(Long visitorId, Long hostId);
}
