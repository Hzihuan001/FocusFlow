package com.focusflow.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.focusflow.server.dto.AddFriendRequest;
import com.focusflow.server.dto.FriendInfoResponse;
import com.focusflow.server.entity.Friendship;
import com.focusflow.server.entity.User;
import com.focusflow.server.mapper.FriendshipMapper;
import com.focusflow.server.mapper.UserMapper;
import com.focusflow.server.service.FriendshipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * 好友服务实现类
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * 【业务逻辑】
 * 1. 好友申请：创建一条 status=0 的记录
 * 2. 同意申请：更新 status=1
 * 3. 好友列表：双向查询（userId 或 friendId 等于当前用户）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FriendshipServiceImpl implements FriendshipService {

    private final FriendshipMapper friendshipMapper;
    private final UserMapper userMapper;

    /** 好友状态：申请中 */
    private static final int STATUS_PENDING = 0;
    /** 好友状态：已同意 */
    private static final int STATUS_ACCEPTED = 1;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean sendFriendRequest(Long userId, AddFriendRequest request) {
        Long friendId = request.getFriendId();

        // 不能添加自己为好友
        if (userId.equals(friendId)) {
            throw new RuntimeException("不能添加自己为好友");
        }

        // 检查目标用户是否存在
        User friend = userMapper.selectById(friendId);
        if (friend == null) {
            throw new RuntimeException("用户不存在");
        }

        // 检查是否已经是好友或有待处理的申请
        LambdaQueryWrapper<Friendship> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w
            .eq(Friendship::getUserId, userId).eq(Friendship::getFriendId, friendId)
            .or()
            .eq(Friendship::getUserId, friendId).eq(Friendship::getFriendId, userId)
        );

        Friendship existing = friendshipMapper.selectOne(wrapper);
        if (existing != null) {
            if (existing.getStatus() == STATUS_ACCEPTED) {
                throw new RuntimeException("已经是好友关系");
            } else {
                throw new RuntimeException("已有待处理的好友申请");
            }
        }

        // 创建好友申请记录
        Friendship friendship = new Friendship();
        friendship.setUserId(userId);
        friendship.setFriendId(friendId);
        friendship.setStatus(STATUS_PENDING);
        friendship.setCreatedAt(System.currentTimeMillis());
        friendship.setUpdatedAt(System.currentTimeMillis());

        friendshipMapper.insert(friendship);
        log.info("好友申请已发送: userId={}, friendId={}", userId, friendId);

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean acceptFriendRequest(Long userId, Long friendshipId) {
        Friendship friendship = friendshipMapper.selectById(friendshipId);

        if (friendship == null) {
            throw new RuntimeException("好友申请不存在");
        }

        // 只有接收方才能同意申请
        if (!userId.equals(friendship.getFriendId())) {
            throw new RuntimeException("无权处理此申请");
        }

        if (friendship.getStatus() != STATUS_PENDING) {
            throw new RuntimeException("申请已被处理");
        }

        // 更新状态为已同意
        friendship.setStatus(STATUS_ACCEPTED);
        friendship.setUpdatedAt(System.currentTimeMillis());
        friendshipMapper.updateById(friendship);

        log.info("好友申请已同意: friendshipId={}, userId={}", friendshipId, userId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean rejectFriendRequest(Long userId, Long friendshipId) {
        Friendship friendship = friendshipMapper.selectById(friendshipId);

        if (friendship == null) {
            throw new RuntimeException("好友申请不存在");
        }

        // 只有接收方才能拒绝申请
        if (!userId.equals(friendship.getFriendId())) {
            throw new RuntimeException("无权处理此申请");
        }

        // 删除记录（逻辑删除）
        friendshipMapper.deleteById(friendshipId);

        log.info("好友申请已拒绝: friendshipId={}, userId={}", friendshipId, userId);
        return true;
    }

    @Override
    public List<FriendInfoResponse> getFriendList(Long userId) {
        List<FriendInfoResponse> friends = new ArrayList<>();

        // 查询我发起的好友关系（status=1）
        LambdaQueryWrapper<Friendship> wrapper1 = new LambdaQueryWrapper<>();
        wrapper1.eq(Friendship::getUserId, userId).eq(Friendship::getStatus, STATUS_ACCEPTED);
        List<Friendship> sentRequests = friendshipMapper.selectList(wrapper1);

        for (Friendship f : sentRequests) {
            User friend = userMapper.selectById(f.getFriendId());
            if (friend != null) {
                friends.add(FriendInfoResponse.fromEntity(friend, STATUS_ACCEPTED, f.getUpdatedAt()));
            }
        }

        // 查询我接收的好友关系（status=1）
        LambdaQueryWrapper<Friendship> wrapper2 = new LambdaQueryWrapper<>();
        wrapper2.eq(Friendship::getFriendId, userId).eq(Friendship::getStatus, STATUS_ACCEPTED);
        List<Friendship> receivedRequests = friendshipMapper.selectList(wrapper2);

        for (Friendship f : receivedRequests) {
            User friend = userMapper.selectById(f.getUserId());
            if (friend != null) {
                friends.add(FriendInfoResponse.fromEntity(friend, STATUS_ACCEPTED, f.getUpdatedAt()));
            }
        }

        return friends;
    }

    @Override
    public List<FriendInfoResponse> getPendingRequests(Long userId) {
        List<FriendInfoResponse> requests = new ArrayList<>();

        // 查询发送给我的好友申请（status=0）
        LambdaQueryWrapper<Friendship> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Friendship::getFriendId, userId).eq(Friendship::getStatus, STATUS_PENDING);
        List<Friendship> pendingList = friendshipMapper.selectList(wrapper);

        for (Friendship f : pendingList) {
            User requester = userMapper.selectById(f.getUserId());
            if (requester != null) {
                requests.add(FriendInfoResponse.fromEntity(requester, f.getId(), STATUS_PENDING, f.getCreatedAt()));
            }
        }

        return requests;
    }

    @Override
    public boolean isFriend(Long userId, Long friendId) {
        LambdaQueryWrapper<Friendship> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w
            .eq(Friendship::getUserId, userId).eq(Friendship::getFriendId, friendId)
            .or()
            .eq(Friendship::getUserId, friendId).eq(Friendship::getFriendId, userId)
        ).eq(Friendship::getStatus, STATUS_ACCEPTED);

        Long count = friendshipMapper.selectCount(wrapper);
        return count != null && count > 0;
    }
}
