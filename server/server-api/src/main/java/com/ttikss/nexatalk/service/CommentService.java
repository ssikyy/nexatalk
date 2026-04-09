package com.ttikss.nexatalk.service;

import com.ttikss.nexatalk.dto.CommentCreateRequest;
import com.ttikss.nexatalk.vo.CommentVO;
import com.ttikss.nexatalk.vo.PageVO;

/**
 * 评论模块业务层接口
 */
public interface CommentService {

    /**
     * 发表评论或回复
     *
     * @param userId 当前登录用户 ID
     * @param req    评论请求（包含 postId、parentId、content）
     * @return 新评论 ID
     */
    Long createComment(Long userId, CommentCreateRequest req);

    /**
     * 删除评论（逻辑删除，status=1）
     * 作者或管理员可以删除
     *
     * @param userId    当前登录用户 ID
     * @param commentId 评论 ID
     */
    void deleteComment(Long userId, Long commentId);

    /**
     * 分页查询帖子的一级评论（每条附带最新 3 条回复预览）
     *
     * @param postId   帖子 ID
     * @param page     页码
     * @param pageSize 每页大小
     * @return 分页评论列表
     */
    PageVO<CommentVO> listComments(Long postId, int page, int pageSize);

    /**
     * 分页查询某条一级评论下的所有二级回复
     *
     * @param rootId   根评论 ID（一级评论 ID）
     * @param page     页码
     * @param pageSize 每页大小
     * @return 分页回复列表
     */
    PageVO<CommentVO> listReplies(Long rootId, int page, int pageSize);

    /**
     * 分页查询当前用户的评论列表
     *
     * @param userId   当前用户 ID
     * @param page     页码
     * @param pageSize 每页大小
     * @return 分页评论列表
     */
    PageVO<CommentVO> listMyComments(Long userId, int page, int pageSize);
}
