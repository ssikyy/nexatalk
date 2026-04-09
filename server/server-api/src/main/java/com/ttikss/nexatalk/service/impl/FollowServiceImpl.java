package com.ttikss.nexatalk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ttikss.nexatalk.common.ErrorCode;
import com.ttikss.nexatalk.entity.Follow;
import com.ttikss.nexatalk.entity.Notification;
import com.ttikss.nexatalk.entity.User;
import com.ttikss.nexatalk.exception.BusinessException;
import com.ttikss.nexatalk.mapper.FollowMapper;
import com.ttikss.nexatalk.mapper.UserMapper;
import com.ttikss.nexatalk.service.FollowService;
import com.ttikss.nexatalk.service.NotificationService;
import com.ttikss.nexatalk.vo.PageVO;
import com.ttikss.nexatalk.vo.UserVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 关注模块业务实现
 */
@Service
public class FollowServiceImpl implements FollowService {

    private final FollowMapper followMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;

    public FollowServiceImpl(FollowMapper followMapper, UserMapper userMapper, NotificationService notificationService) {
        this.followMapper = followMapper;
        this.userMapper = userMapper;
        this.notificationService = notificationService;
    }

    @Override
    public void follow(Long followerId, Long followeeId) {
        if (followerId.equals(followeeId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.code(), "不能关注自己");
        }
        // 检查被关注用户是否存在
        User followee = userMapper.selectById(followeeId);
        if (followee == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.code(), "用户不存在");
        }

        try {
            Follow follow = new Follow();
            follow.setFollowerId(followerId);
            follow.setFolloweeId(followeeId);
            followMapper.insert(follow);

            // 发送关注通知
            notificationService.send(
                    followeeId,
                    followerId,
                    Notification.TYPE_FOLLOW,
                    Notification.ENTITY_TYPE_NONE,
                    null,
                    "关注了你"
            );
        } catch (DuplicateKeyException e) {
            // 已关注，幂等处理
        }
    }

    @Override
    public void unfollow(Long followerId, Long followeeId) {
        followMapper.delete(
                new LambdaQueryWrapper<Follow>()
                        .eq(Follow::getFollowerId, followerId)
                        .eq(Follow::getFolloweeId, followeeId)
        );
    }

    @Override
    public boolean isFollowing(Long followerId, Long followeeId) {
        Long count = followMapper.selectCount(
                new LambdaQueryWrapper<Follow>()
                        .eq(Follow::getFollowerId, followerId)
                        .eq(Follow::getFolloweeId, followeeId)
        );
        return count != null && count > 0;
    }

    @Override
    public PageVO<UserVO> listFollowing(Long userId, Long viewerId, int page, int pageSize) {
        pageSize = Math.min(pageSize, 50);
        // 查我关注的人的 ID 列表
        Page<Follow> pageResult = followMapper.selectPage(
                new Page<>(page, pageSize),
                new LambdaQueryWrapper<Follow>()
                        .eq(Follow::getFollowerId, userId)
                        .orderByDesc(Follow::getCreatedAt)
        );
        List<UserVO> voList = toUserVOList(pageResult.getRecords(), viewerId,
                follow -> follow.getFolloweeId());
        return PageVO.of(pageResult, voList);
    }

    @Override
    public PageVO<UserVO> listFollowers(Long userId, Long viewerId, int page, int pageSize) {
        pageSize = Math.min(pageSize, 50);
        // 查关注我的人的 ID 列表
        Page<Follow> pageResult = followMapper.selectPage(
                new Page<>(page, pageSize),
                new LambdaQueryWrapper<Follow>()
                        .eq(Follow::getFolloweeId, userId)
                        .orderByDesc(Follow::getCreatedAt)
        );
        List<UserVO> voList = toUserVOList(pageResult.getRecords(), viewerId,
                follow -> follow.getFollowerId());
        return PageVO.of(pageResult, voList);
    }

    @Override
    public long countFollowing(Long userId) {
        return followMapper.selectCount(
                new LambdaQueryWrapper<Follow>().eq(Follow::getFollowerId, userId)
        );
    }

    @Override
    public long countFollowers(Long userId) {
        return followMapper.selectCount(
                new LambdaQueryWrapper<Follow>().eq(Follow::getFolloweeId, userId)
        );
    }

    /** 将 Follow 列表转为 UserVO 列表 */
    private List<UserVO> toUserVOList(List<Follow> follows,
                                       Long viewerId,
                                       java.util.function.Function<Follow, Long> idExtractor) {
        List<Long> userIds = follows.stream()
                .map(idExtractor)
                .distinct()
                .collect(Collectors.toList());
        if (userIds.isEmpty()) return List.of();

        Map<Long, User> userMap = userMapper.selectBatchIds(userIds)
                .stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        Set<Long> followingIds = Collections.emptySet();
        if (viewerId != null) {
            followingIds = followMapper.selectList(
                    new LambdaQueryWrapper<Follow>()
                            .eq(Follow::getFollowerId, viewerId)
                            .in(Follow::getFolloweeId, userIds)
            ).stream().map(Follow::getFolloweeId).collect(Collectors.toSet());
        }

        Set<Long> finalFollowingIds = followingIds;
        return userIds.stream()
                .map(userMap::get)
                .filter(Objects::nonNull)
                .map(user -> {
                    UserVO vo = UserVO.from(user);
                    if (viewerId != null) {
                        boolean isSelf = viewerId.equals(user.getId());
                        vo.setIsSelf(isSelf);
                        if (!isSelf) {
                            vo.setIsFollowing(finalFollowingIds.contains(user.getId()));
                        }
                    }
                    return vo;
                })
                .collect(Collectors.toList());
    }
}
