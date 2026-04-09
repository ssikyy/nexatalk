package com.ttikss.nexatalk.controller;

import com.ttikss.nexatalk.common.Result;
import com.ttikss.nexatalk.dto.CommentCreateRequest;
import com.ttikss.nexatalk.security.CurrentUser;
import com.ttikss.nexatalk.service.CommentService;
import com.ttikss.nexatalk.vo.CommentVO;
import com.ttikss.nexatalk.vo.PageVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 评论模块控制器
 *
 * 接口清单：
 * GET    /api/comments               查询帖子的一级评论列表（含回复预览）
 * GET    /api/comments/{id}/replies  查询某条评论下的所有回复（分页）
 * POST   /api/comments               发表评论或回复
 * DELETE /api/comments/{id}          删除评论
 */
@RestController
@RequestMapping("/api/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    /**
     * 查询帖子的一级评论（每条附带最新 3 条回复预览）
     */
    @GetMapping
    public Result<PageVO<CommentVO>> listComments(
            @RequestParam Long postId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(commentService.listComments(postId, page, pageSize));
    }

    /**
     * 查询某条一级评论下的所有回复（完整分页，"查看更多回复"使用）
     */
    @GetMapping("/{id}/replies")
    public Result<PageVO<CommentVO>> listReplies(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(commentService.listReplies(id, page, pageSize));
    }

    /**
     * 发表评论或回复
     * parentId = 0 或不传：一级评论；parentId 有值：二级回复
     */
    @PostMapping
    public Result<Long> createComment(@CurrentUser Long userId,
                                      @Valid @RequestBody CommentCreateRequest req) {
        return Result.ok(commentService.createComment(userId, req));
    }

    /**
     * 删除评论（逻辑删除，作者或管理员可操作）
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteComment(@CurrentUser Long userId, @PathVariable Long id) {
        commentService.deleteComment(userId, id);
        return Result.ok("删除成功");
    }

    /**
     * 查询当前用户的评论列表
     */
    @GetMapping("/mine")
    public Result<PageVO<CommentVO>> listMyComments(
            @CurrentUser Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(commentService.listMyComments(userId, page, pageSize));
    }
}
