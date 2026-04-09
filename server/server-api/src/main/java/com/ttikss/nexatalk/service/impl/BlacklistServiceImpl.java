package com.ttikss.nexatalk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ttikss.nexatalk.common.ErrorCode;
import com.ttikss.nexatalk.entity.Blacklist;
import com.ttikss.nexatalk.entity.User;
import com.ttikss.nexatalk.exception.BusinessException;
import com.ttikss.nexatalk.mapper.BlacklistMapper;
import com.ttikss.nexatalk.mapper.UserMapper;
import com.ttikss.nexatalk.security.UserContext;
import com.ttikss.nexatalk.service.BlacklistService;
import com.ttikss.nexatalk.vo.UserVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 黑名单业务实现
 */
@Service
public class BlacklistServiceImpl implements BlacklistService {

    private final BlacklistMapper blacklistMapper;
    private final UserMapper userMapper;

    public BlacklistServiceImpl(BlacklistMapper blacklistMapper, UserMapper userMapper) {
        this.blacklistMapper = blacklistMapper;
        this.userMapper = userMapper;
    }

    @Override
    public List<UserVO> getBlacklist() {
        Long userId = getCurrentUserId();
        // 查询黑名单记录
        List<Blacklist> blacklist = blacklistMapper.selectList(
                new LambdaQueryWrapper<Blacklist>()
                        .eq(Blacklist::getUserId, userId)
                        .orderByDesc(Blacklist::getCreatedAt)
        );

        if (blacklist.isEmpty()) {
            return List.of();
        }

        // 获取被拉黑的用户ID列表
        List<Long> blockedUserIds = blacklist.stream()
                .map(Blacklist::getBlockedUserId)
                .collect(Collectors.toList());

        // 查询用户信息
        List<User> users = userMapper.selectBatchIds(blockedUserIds);

        // 返回用户VO列表
        return users.stream()
                .map(UserVO::from)
                .collect(Collectors.toList());
    }

    @Override
    public void addToBlacklist(Long blockedUserId) {
        Long userId = getCurrentUserId();
        if (userId.equals(blockedUserId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.code(), "不能拉黑自己");
        }
        // 检查被拉黑用户是否存在
        User blockedUser = userMapper.selectById(blockedUserId);
        if (blockedUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.code(), "用户不存在");
        }

        try {
            Blacklist blacklist = new Blacklist();
            blacklist.setUserId(userId);
            blacklist.setBlockedUserId(blockedUserId);
            blacklist.setCreatedAt(LocalDateTime.now());
            blacklistMapper.insert(blacklist);
        } catch (DuplicateKeyException e) {
            // 已在黑名单，幂等处理
        }
    }

    @Override
    public void removeFromBlacklist(Long blockedUserId) {
        Long userId = getCurrentUserId();
        blacklistMapper.delete(
                new LambdaQueryWrapper<Blacklist>()
                        .eq(Blacklist::getUserId, userId)
                        .eq(Blacklist::getBlockedUserId, blockedUserId)
        );
    }

    @Override
    public boolean isBlocked(Long blockedUserId) {
        Long userId = getCurrentUserId();
        Long count = blacklistMapper.selectCount(
                new LambdaQueryWrapper<Blacklist>()
                        .eq(Blacklist::getUserId, userId)
                        .eq(Blacklist::getBlockedUserId, blockedUserId)
        );
        return count != null && count > 0;
    }

    private Long getCurrentUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED.code(), "请先登录");
        }
        return userId;
    }
}
