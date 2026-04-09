package com.ttikss.nexatalk.controller;

import com.ttikss.nexatalk.common.ErrorCode;
import com.ttikss.nexatalk.common.Result;
import com.ttikss.nexatalk.dto.SectionCreateRequest;
import com.ttikss.nexatalk.entity.Section;
import com.ttikss.nexatalk.exception.BusinessException;
import com.ttikss.nexatalk.security.CurrentUser;
import com.ttikss.nexatalk.security.UserContext;
import com.ttikss.nexatalk.service.SectionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 分区模块控制器
 *
 * 接口清单：
 * GET  /api/sections           获取正常状态分区列表（公开）
 * GET  /api/sections/all       获取所有分区，含禁用（管理员）
 * GET  /api/sections/{id}      获取分区详情
 * POST /api/sections           新增分区（管理员）
 * PUT  /api/sections/{id}      更新分区（管理员）
 * PUT  /api/sections/{id}/disable  禁用分区（管理员）
 * PUT  /api/sections/{id}/enable   启用分区（管理员）
 */
@RestController
@RequestMapping("/api/sections")
public class SectionController {

    private final SectionService sectionService;

    public SectionController(SectionService sectionService) {
        this.sectionService = sectionService;
    }

    /** 获取正常状态的分区列表（普通用户可见） */
    @GetMapping
    public Result<List<Section>> listSections() {
        return Result.ok(sectionService.listActiveSections());
    }

    /** 获取所有分区包含禁用（管理员专用） */
    @GetMapping("/all")
    public Result<List<Section>> listAllSections(@CurrentUser Long userId) {
        requireAdmin();
        return Result.ok(sectionService.listAllSections());
    }

    /** 获取单个分区详情 */
    @GetMapping("/{id}")
    public Result<Section> getSectionById(@PathVariable Long id) {
        return Result.ok(sectionService.getSectionById(id));
    }

    /** 新增分区（管理员专用） */
    @PostMapping
    public Result<String> createSection(@CurrentUser Long userId,
                                        @Valid @RequestBody SectionCreateRequest req) {
        requireAdmin();
        sectionService.createSection(req);
        return Result.ok("分区创建成功");
    }

    /** 更新分区（管理员专用） */
    @PutMapping("/{id}")
    public Result<String> updateSection(@CurrentUser Long userId,
                                        @PathVariable Long id,
                                        @Valid @RequestBody SectionCreateRequest req) {
        requireAdmin();
        sectionService.updateSection(id, req);
        return Result.ok("分区更新成功");
    }

    /** 禁用分区（管理员专用） */
    @PutMapping("/{id}/disable")
    public Result<String> disableSection(@CurrentUser Long userId, @PathVariable Long id) {
        requireAdmin();
        sectionService.disableSection(id);
        return Result.ok("分区已禁用");
    }

    /** 启用分区（管理员专用） */
    @PutMapping("/{id}/enable")
    public Result<String> enableSection(@CurrentUser Long userId, @PathVariable Long id) {
        requireAdmin();
        sectionService.enableSection(id);
        return Result.ok("分区已启用");
    }

    /** 校验当前用户是否为管理员，不是则抛异常 */
    private void requireAdmin() {
        if (!UserContext.get().isAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN.code(), "无权限，仅管理员可操作");
        }
    }
}
