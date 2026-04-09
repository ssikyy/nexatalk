package com.ttikss.nexatalk.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ttikss.nexatalk.common.Result;
import com.ttikss.nexatalk.security.CurrentUser;
import com.ttikss.nexatalk.security.RequireAdmin;
import com.ttikss.nexatalk.service.OperationLogService;
import com.ttikss.nexatalk.vo.OperationLogVO;
import com.ttikss.nexatalk.vo.PageVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 操作日志控制器（仅管理员可访问）
 */
@RestController
@RequestMapping("/api/admin/logs")
public class OperationLogController {

    private final OperationLogService operationLogService;

    public OperationLogController(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    /**
     * 分页查询操作日志（需管理员）
     */
    @GetMapping
    @RequireAdmin
    public Result<PageVO<OperationLogVO>> listLogs(@CurrentUser Long currentUserId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        IPage<OperationLogVO> pageResult = operationLogService.listLogs(
                page, pageSize, username, module, operation, status, startTime, endTime);
        return Result.ok(PageVO.from(pageResult));
    }

    /**
     * 清空所有操作日志（仅超级管理员可执行）
     */
    @PostMapping("/clear")
    @RequireAdmin(2)
    public Result<Void> clearLogs() {
        operationLogService.clearAll();
        return Result.ok(null);
    }
}
