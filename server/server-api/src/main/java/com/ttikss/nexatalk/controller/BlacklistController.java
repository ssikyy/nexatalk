package com.ttikss.nexatalk.controller;

import com.ttikss.nexatalk.common.Result;
import com.ttikss.nexatalk.service.BlacklistService;
import com.ttikss.nexatalk.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 黑名单控制器
 */
@RestController
@RequestMapping("/api/blacklist")
@Tag(name = "黑名单管理")
public class BlacklistController {

    private final BlacklistService blacklistService;

    public BlacklistController(BlacklistService blacklistService) {
        this.blacklistService = blacklistService;
    }

    @GetMapping
    @Operation(summary = "获取黑名单列表")
    public Result<List<UserVO>> getBlacklist() {
        return Result.ok(blacklistService.getBlacklist());
    }

    @PostMapping("/{blockedUserId}")
    @Operation(summary = "添加用户到黑名单")
    public Result<Void> addToBlacklist(@PathVariable Long blockedUserId) {
        blacklistService.addToBlacklist(blockedUserId);
        return Result.ok(null);
    }

    @DeleteMapping("/{blockedUserId}")
    @Operation(summary = "从黑名单移除用户")
    public Result<Void> removeFromBlacklist(@PathVariable Long blockedUserId) {
        blacklistService.removeFromBlacklist(blockedUserId);
        return Result.ok(null);
    }

    @GetMapping("/check/{blockedUserId}")
    @Operation(summary = "检查是否在黑名单中")
    public Result<Boolean> checkBlacklist(@PathVariable Long blockedUserId) {
        return Result.ok(blacklistService.isBlocked(blockedUserId));
    }
}
