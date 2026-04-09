package com.ttikss.nexatalk.controller;

import com.ttikss.nexatalk.common.Result;
import com.ttikss.nexatalk.security.CurrentUser;
import com.ttikss.nexatalk.service.FavoriteService;
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
 * 收藏模块控制器
 *
 * 接口清单：
 * POST   /api/favorites/{postId}  收藏帖子
 * DELETE /api/favorites/{postId}  取消收藏
 * GET    /api/favorites/{postId}  是否已收藏
 * GET    /api/favorites/mine      我的收藏列表
 */
@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    /** 收藏帖子 */
    @PostMapping("/{postId}")
    public Result<String> favorite(@CurrentUser Long userId, @PathVariable Long postId) {
        favoriteService.favorite(userId, postId);
        return Result.ok("收藏成功");
    }

    /** 取消收藏 */
    @DeleteMapping("/{postId}")
    public Result<String> unfavorite(@CurrentUser Long userId, @PathVariable Long postId) {
        favoriteService.unfavorite(userId, postId);
        return Result.ok("已取消收藏");
    }

    /** 是否已收藏 */
    @GetMapping("/{postId}")
    public Result<Boolean> hasFavorited(@CurrentUser Long userId, @PathVariable Long postId) {
        return Result.ok(favoriteService.hasFavorited(userId, postId));
    }

    /** 我的收藏列表（分页） */
    @GetMapping("/mine")
    public Result<PageVO<PostVO>> listMyFavorites(
            @CurrentUser Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(favoriteService.listMyFavorites(userId, page, pageSize));
    }
}
