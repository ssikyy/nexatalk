package com.ttikss.nexatalk.controller;

import com.ttikss.nexatalk.common.ErrorCode;
import com.ttikss.nexatalk.common.Result;
import com.ttikss.nexatalk.dto.PunishRequest;
import com.ttikss.nexatalk.entity.Punishment;
import com.ttikss.nexatalk.exception.BusinessException;
import com.ttikss.nexatalk.security.CurrentUser;
import com.ttikss.nexatalk.security.LoginUser;
import com.ttikss.nexatalk.security.UserContext;
import com.ttikss.nexatalk.service.PunishmentService;
import com.ttikss.nexatalk.vo.PageVO;
import com.ttikss.nexatalk.vo.PunishmentVO;
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
 * 处罚模块控制器（仅管理员可用）
 *
 * 接口清单：
 * POST   /api/punishments             下处罚（禁言/封号）
 * DELETE /api/punishments/{id}        解除处罚
 * GET    /api/punishments/active      查询当前生效的处罚列表
 * GET    /api/punishments/user/{id}   查询某用户的处罚历史
 */
@RestController
@RequestMapping("/api/punishments")
public class PunishmentController {

    private final PunishmentService punishmentService;

    public PunishmentController(PunishmentService punishmentService) {
        this.punishmentService = punishmentService;
    }

    /** 下处罚 */
    @PostMapping
    public Result<String> punish(@CurrentUser Long adminId,
                                  @Valid @RequestBody PunishRequest request) {
        requireAdmin();
        punishmentService.punish(adminId, request);
        return Result.ok("处罚已生效");
    }

    /** 解除处罚 */
    @DeleteMapping("/{punishmentId}")
    public Result<String> lift(@CurrentUser Long adminId, @PathVariable Long punishmentId) {
        requireAdmin();
        punishmentService.lift(adminId, punishmentId);
        return Result.ok("处罚已解除");
    }

    /** 查询当前生效的处罚列表 */
    @GetMapping("/active")
    public Result<PageVO<PunishmentVO>> listActive(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        requireAdmin();
        return Result.ok(punishmentService.listActive(page, pageSize));
    }

    /** 查询某用户的处罚历史 */
    @GetMapping("/user/{userId}")
    public Result<PageVO<PunishmentVO>> listByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        requireAdmin();
        return Result.ok(punishmentService.listByUser(userId, page, pageSize));
    }

    private void requireAdmin() {
        LoginUser loginUser = UserContext.get();
        if (loginUser == null || !loginUser.isAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN.code(), "仅管理员可操作");
        }
    }
}
