package com.ttikss.nexatalk.service;

import com.ttikss.nexatalk.vo.PageVO;
import com.ttikss.nexatalk.vo.UserVO;

/**
 * 关注模块业务层接口
 */
public interface FollowService {

    /**
     * 关注用户（幂等：已关注则不重复操作）
     *
     * @param followerId 关注者（当前登录用户）
     * @param followeeId 被关注者
     */
    void follow(Long followerId, Long followeeId);

    /**
     * 取消关注（幂等：未关注则不操作）
     *
     * @param followerId 关注者（当前登录用户）
     * @param followeeId 被关注者
     */
    void unfollow(Long followerId, Long followeeId);

    /**
     * 检查是否已关注某用户
     *
     * @param followerId 关注者
     * @param followeeId 被关注者
     * @return true=已关注
     */
    boolean isFollowing(Long followerId, Long followeeId);

    /**
     * 分页查询我的关注列表
     *
     * @param userId   当前用户 ID
     * @param page     页码
     * @param pageSize 每页大小
     */
    PageVO<UserVO> listFollowing(Long userId, Long viewerId, int page, int pageSize);

    /**
     * 分页查询我的粉丝列表
     *
     * @param userId   当前用户 ID
     * @param page     页码
     * @param pageSize 每页大小
     */
    PageVO<UserVO> listFollowers(Long userId, Long viewerId, int page, int pageSize);

    /**
     * 查询关注数量
     *
     * @param userId 用户 ID
     * @return 该用户关注了多少人
     */
    long countFollowing(Long userId);

    /**
     * 查询粉丝数量
     *
     * @param userId 用户 ID
     * @return 该用户有多少粉丝
     */
    long countFollowers(Long userId);
}
