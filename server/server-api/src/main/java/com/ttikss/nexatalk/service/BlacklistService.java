package com.ttikss.nexatalk.service;

import com.ttikss.nexatalk.vo.UserVO;
import java.util.List;

/**
 * 黑名单业务层接口
 */
public interface BlacklistService {

    /**
     * 获取当前用户黑名单列表
     */
    List<UserVO> getBlacklist();

    /**
     * 添加用户到黑名单
     */
    void addToBlacklist(Long blockedUserId);

    /**
     * 从黑名单移除用户
     */
    void removeFromBlacklist(Long blockedUserId);

    /**
     * 检查是否在黑名单中
     */
    boolean isBlocked(Long blockedUserId);
}
