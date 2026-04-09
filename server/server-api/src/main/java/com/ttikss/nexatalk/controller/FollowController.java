package com.ttikss.nexatalk.controller;

import com.ttikss.nexatalk.common.Result;
import com.ttikss.nexatalk.security.CurrentUser;
import com.ttikss.nexatalk.security.UserContext;
import com.ttikss.nexatalk.service.FollowService;
import com.ttikss.nexatalk.vo.PageVO;
import com.ttikss.nexatalk.vo.UserVO;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 关注模块控制器
 *
 * 接口清单：
 * POST   /api/follows/{userId}              关注用户
 * DELETE /api/follows/{userId}              取消关注
 * GET    /api/follows/{userId}/status       查询是否已关注
 * GET    /api/follows/{userId}/following    查询该用户的关注列表
 * GET    /api/follows/{userId}/followers    查询该用户的粉丝列表
 * GET    /api/follows/{userId}/stats        查询关注/粉丝统计数量
 */
@RestController
@RequestMapping("/api/follows")
public class FollowController {

    private final FollowService followService;

    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    /** 关注用户 */
    @PostMapping("/{userId}")
    public Result<String> follow(@CurrentUser Long currentUserId, @PathVariable Long userId) {
        followService.follow(currentUserId, userId);
        return Result.ok("关注成功");
    }

    /** 取消关注 */
    @DeleteMapping("/{userId}")
    public Result<String> unfollow(@CurrentUser Long currentUserId, @PathVariable Long userId) {
        followService.unfollow(currentUserId, userId);
        return Result.ok("已取消关注");
    }

    /** 查询当前用户是否已关注指定用户 */
    @GetMapping("/{userId}/status")
    public Result<Boolean> isFollowing(@CurrentUser Long currentUserId, @PathVariable Long userId) {
        return Result.ok(followService.isFollowing(currentUserId, userId));
    }

    /** 查询指定用户的关注列表（他关注了谁） */
    @GetMapping("/{userId}/following")
    public Result<PageVO<UserVO>> listFollowing(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(followService.listFollowing(userId, UserContext.getUserId(), page, pageSize));
    }

    /** 查询指定用户的粉丝列表（谁关注了他） */
    @GetMapping("/{userId}/followers")
    public Result<PageVO<UserVO>> listFollowers(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(followService.listFollowers(userId, UserContext.getUserId(), page, pageSize));
    }

    /** 查询关注/粉丝数统计 */
    @GetMapping("/{userId}/stats")
    public Result<?> getStats(@PathVariable Long userId) {
        long following = followService.countFollowing(userId);
        long followers = followService.countFollowers(userId);
        java.util.Map<String, Long> stats = new java.util.LinkedHashMap<>();
        stats.put("following", following);
        stats.put("followers", followers);
        return Result.ok(stats);
    }
}
