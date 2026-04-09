package com.ttikss.nexatalk.service;

import com.ttikss.nexatalk.vo.PageVO;
import com.ttikss.nexatalk.vo.PostVO;

/**
 * 点赞模块业务层接口
 *
 * 设计原则：
 * - 幂等接口：重复点赞不报错，重复取消不报错
 * - 唯一约束：依赖数据库 (user_id, post_id) 唯一索引兜底，防并发重复写
 */
public interface LikeService {

    /**
     * 点赞帖子（幂等：已点赞则不重复操作）
     *
     * @param userId 当前登录用户 ID
     * @param postId 帖子 ID
     */
    void likePost(Long userId, Long postId);

    /**
     * 取消帖子点赞（幂等：未点赞则不操作）
     *
     * @param userId 当前登录用户 ID
     * @param postId 帖子 ID
     */
    void unlikePost(Long userId, Long postId);

    /**
     * 检查当前用户是否已点赞某帖子
     *
     * @param userId 用户 ID
     * @param postId 帖子 ID
     * @return true=已点赞
     */
    boolean hasLikedPost(Long userId, Long postId);

    /**
     * 点赞评论（幂等）
     *
     * @param userId    当前登录用户 ID
     * @param commentId 评论 ID
     */
    void likeComment(Long userId, Long commentId);

    /**
     * 取消评论点赞（幂等）
     *
     * @param userId    当前登录用户 ID
     * @param commentId 评论 ID
     */
    void unlikeComment(Long userId, Long commentId);

    /**
     * 检查当前用户是否已点赞某评论
     *
     * @param userId    用户 ID
     * @param commentId 评论 ID
     * @return true=已点赞
     */
    boolean hasLikedComment(Long userId, Long commentId);

    /**
     * 获取当前用户点赞的帖子列表（分页）
     *
     * @param userId   用户 ID
     * @param page     页码（从1开始）
     * @param pageSize 每页数量
     * @return 分页的点赞帖子列表
     */
    PageVO<PostVO> listMyLikedPosts(Long userId, int page, int pageSize);

    /**
     * 统计用户收到的点赞数（帖子被点赞的总数）
     *
     * @param userId 用户 ID
     * @return 点赞数
     */
    long countUserLiked(Long userId);
}
