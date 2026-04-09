package com.ttikss.nexatalk.controller;

import com.ttikss.nexatalk.common.ErrorCode;
import com.ttikss.nexatalk.common.Result;
import com.ttikss.nexatalk.dto.ReportCreateRequest;
import com.ttikss.nexatalk.dto.ReportReviewRequest;
import com.ttikss.nexatalk.entity.Report;
import com.ttikss.nexatalk.exception.BusinessException;
import com.ttikss.nexatalk.security.CurrentUser;
import com.ttikss.nexatalk.security.LoginUser;
import com.ttikss.nexatalk.security.UserContext;
import com.ttikss.nexatalk.service.ReportService;
import com.ttikss.nexatalk.vo.PageVO;
import com.ttikss.nexatalk.vo.ReportVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 举报模块控制器
 *
 * 接口清单：
 * POST /api/reports               提交举报（登录用户）
 * GET  /api/reports               管理员查询所有举报（可过滤状态）
 * GET  /api/reports/pending       管理员查询待审核举报列表
 * PUT  /api/reports/{id}/review   管理员审核举报
     * DELETE /api/reports/by-entity   管理员按实体清理举报记录
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /** 提交举报 */
    @PostMapping
    public Result<String> createReport(@CurrentUser Long userId,
                                        @Valid @RequestBody ReportCreateRequest request) {
        reportService.createReport(userId, request);
        return Result.ok("举报已提交");
    }

    /** 用户查询自己的举报记录 */
    @GetMapping("/my")
    public Result<PageVO<ReportVO>> listMyReports(
            @CurrentUser Long userId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(reportService.listMyReports(userId, status, page, pageSize));
    }

    /** 管理员查询所有举报列表（支持多条件过滤） */
    @GetMapping
    public Result<PageVO<ReportVO>> listReports(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer entityType,
            @RequestParam(required = false) Integer reason,
            @RequestParam(required = false) Long reporterId,
            @RequestParam(required = false) Long reportedUserId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        requireAdmin();
        return Result.ok(reportService.listReportsVO(status, entityType, reason, reporterId, reportedUserId, page, pageSize));
    }

    /** 管理员查询待审核举报列表 */
    @GetMapping("/pending")
    public Result<PageVO<ReportVO>> listPendingReports(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        requireAdmin();
        return Result.ok(reportService.listReportsVO(Report.STATUS_PENDING, null, null, null, null, page, pageSize));
    }

    /** 管理员审核举报 */
    @PutMapping("/{reportId}/review")
    public Result<String> reviewReport(@CurrentUser Long adminId,
                                        @PathVariable Long reportId,
                                        @Valid @RequestBody ReportReviewRequest request) {
        requireAdmin();
        reportService.reviewReport(adminId, reportId, request);
        return Result.ok("审核完成");
    }

    /** 管理员：根据被举报实体清理所有相关举报记录 */
    @DeleteMapping("/by-entity")
    public Result<String> clearByEntity(
            @RequestParam Integer entityType,
            @RequestParam Long entityId) {
        requireAdmin();
        // 为了避免前端看到 500 错误，这里即使清理失败也不抛出异常
        try {
            reportService.clearReportsByEntity(entityType, entityId);
        } catch (Exception ignored) {
        }
        return Result.ok("已清理举报记录");
    }

    /** 检查当前用户是否为管理员 */
    private void requireAdmin() {
        LoginUser loginUser = UserContext.get();
        if (loginUser == null || !loginUser.isAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN.code(), "仅管理员可操作");
        }
    }
}
