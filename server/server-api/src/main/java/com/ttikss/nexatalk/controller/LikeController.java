package com.ttikss.nexatalk.controller;

import com.ttikss.nexatalk.common.Result;
import com.ttikss.nexatalk.security.CurrentUser;
import com.ttikss.nexatalk.service.LikeService;
import com.ttikss.nexatalk.vo.PageVO;
import com.ttikss.nexatalk.vo.PostVO;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 点赞模块控制器
 *
 * 接口清单：
 * POST   /api/likes/posts/{id}     点赞帖子（幂等）
 * DELETE /api/likes/posts/{id}     取消帖子点赞（幂等）
 * GET    /api/likes/posts/{id}     查询当前用户是否已点赞该帖子
 * POST   /api/likes/comments/{id}  点赞评论（幂等）
 * DELETE /api/likes/comments/{id}  取消评论点赞（幂等）
 * GET    /api/likes/comments/{id}  查询当前用户是否已点赞该评论
 * GET    /api/likes/mine           我的点赞列表（分页）
 */
@RestController
@RequestMapping("/api/likes")
public class LikeController {

    private final LikeService likeService;

    public LikeController(LikeService likeService) {
        this.likeService = likeService;
    }

    /** 点赞帖子 */
    @PostMapping("/posts/{postId}")
    public Result<String> likePost(@CurrentUser Long userId, @PathVariable Long postId) {
        likeService.likePost(userId, postId);
        return Result.ok("点赞成功");
    }

    /** 取消帖子点赞 */
    @DeleteMapping("/posts/{postId}")
    public Result<String> unlikePost(@CurrentUser Long userId, @PathVariable Long postId) {
        likeService.unlikePost(userId, postId);
        return Result.ok("已取消点赞");
    }

    /** 查询当前用户是否已点赞该帖子 */
    @GetMapping("/posts/{postId}")
    public Result<Boolean> hasLikedPost(@CurrentUser Long userId, @PathVariable Long postId) {
        return Result.ok(likeService.hasLikedPost(userId, postId));
    }

    /** 点赞评论 */
    @PostMapping("/comments/{commentId}")
    public Result<String> likeComment(@CurrentUser Long userId, @PathVariable Long commentId) {
        likeService.likeComment(userId, commentId);
        return Result.ok("点赞成功");
    }

    /** 取消评论点赞 */
    @DeleteMapping("/comments/{commentId}")
    public Result<String> unlikeComment(@CurrentUser Long userId, @PathVariable Long commentId) {
        likeService.unlikeComment(userId, commentId);
        return Result.ok("已取消点赞");
    }

    /** 查询当前用户是否已点赞该评论 */
    @GetMapping("/comments/{commentId}")
    public Result<Boolean> hasLikedComment(@CurrentUser Long userId, @PathVariable Long commentId) {
        return Result.ok(likeService.hasLikedComment(userId, commentId));
    }

    /** 我的点赞列表（分页） */
    @GetMapping("/mine")
    public Result<PageVO<PostVO>> listMyLikes(
            @CurrentUser Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(likeService.listMyLikedPosts(userId, page, pageSize));
    }
}
