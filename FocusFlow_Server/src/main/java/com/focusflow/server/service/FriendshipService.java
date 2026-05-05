package com.focusflow.server.service;

import com.focusflow.server.dto.AddFriendRequest;
import com.focusflow.server.dto.FriendInfoResponse;

import java.util.List;

/**
 * 好友服务接口
 */
public interface FriendshipService {

    /**
     * 发送好友申请
     *
     * @param userId 发起申请的用户ID
     * @param request 申请请求
     * @return 是否成功
     */
    boolean sendFriendRequest(Long userId, AddFriendRequest request);

    /**
     * 同意好友申请
     *
     * @param userId 同意申请的用户ID
     * @param friendshipId 关系记录ID
     * @return 是否成功
     */
    boolean acceptFriendRequest(Long userId, Long friendshipId);

    /**
     * 拒绝好友申请
     *
     * @param userId 拒绝申请的用户ID
     * @param friendshipId 关系记录ID
     * @return 是否成功
     */
    boolean rejectFriendRequest(Long userId, Long friendshipId);

    /**
     * 获取好友列表
     *
     * @param userId 用户ID
     * @return 好友列表
     */
    List<FriendInfoResponse> getFriendList(Long userId);

    /**
     * 获取待处理的好友申请列表
     *
     * @param userId 用户ID
     * @return 申请列表
     */
    List<FriendInfoResponse> getPendingRequests(Long userId);

    /**
     * 检查是否为好友关系
     *
     * @param userId 用户ID
     * @param friendId 好友ID
     * @return 是否为好友
     */
    boolean isFriend(Long userId, Long friendId);
}
