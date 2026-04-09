package com.ttikss.nexatalk.controller;

import com.ttikss.nexatalk.common.Result;
import com.ttikss.nexatalk.dto.SystemNotificationRequest;
import com.ttikss.nexatalk.entity.Notification;
import com.ttikss.nexatalk.security.CurrentUser;
import com.ttikss.nexatalk.security.RequireAdmin;
import com.ttikss.nexatalk.service.NotificationService;
import com.ttikss.nexatalk.vo.PageVO;
import com.ttikss.nexatalk.vo.NotificationVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 通知模块控制器
 *
 * 接口清单：
 * GET  /api/notifications          查询我的通知列表（分页）
 * GET  /api/notifications/unread   查询未读通知数量
 * POST /api/notifications/read     标记所有通知为已读
 *
 * 管理员接口：
 * GET    /api/admin/notifications           查询系统通知列表（分页）
 * GET    /api/admin/notifications/{id}      获取系统通知详情
 * POST   /api/admin/notifications           创建系统通知
 * PUT    /api/admin/notifications/{id}      更新系统通知
 * DELETE /api/admin/notifications/{id}      删除系统通知
 * GET    /api/admin/notifications/pinned    获取置顶通知列表
 */
@RestController
@RequestMapping("/api")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /** 查询我的通知列表 */
    @GetMapping("/notifications")
    public Result<PageVO<NotificationVO>> listMyNotifications(
            @CurrentUser Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Integer type) {
        return Result.ok(notificationService.listMyNotifications(userId, page, pageSize, type));
    }

    /** 查询未读通知数量（不传 type 统计全部，传 type=10 仅统计系统通知） */
    @GetMapping("/notifications/unread")
    public Result<Long> countUnread(
            @CurrentUser Long userId,
            @RequestParam(required = false) Integer type) {
        return Result.ok(notificationService.countUnread(userId, type));
    }

    /** 一键已读（不传 type 标记全部，传 type=10 仅标记系统通知） */
    @PostMapping("/notifications/read")
    public Result<String> markAllRead(
            @CurrentUser Long userId,
            @RequestParam(required = false) Integer type) {
        notificationService.markAllRead(userId, type);
        return Result.ok("已全部标记为已读");
    }

    /** 单个通知标记为已读 */
    @PostMapping("/notifications/{id}/read")
    public Result<String> markAsRead(@CurrentUser Long userId, @PathVariable Long id) {
        notificationService.markAsRead(userId, id);
        return Result.ok("标记已读成功");
    }

    /** 获取当前用户可查看的通知详情 */
    @GetMapping("/notifications/{id}")
    public Result<NotificationVO> getNotificationDetail(@CurrentUser Long userId, @PathVariable Long id) {
        return Result.ok(notificationService.getMyNotificationDetail(userId, id));
    }

    /** 删除单条通知 */
    @DeleteMapping("/notifications/{id}")
    public Result<String> deleteNotification(@CurrentUser Long userId, @PathVariable Long id) {
        notificationService.deleteNotification(userId, id);
        return Result.ok("删除成功");
    }

    // ==================== 管理员接口 ====================

    /** 查询系统通知列表 */
    @GetMapping("/admin/notifications")
    @RequireAdmin
    public Result<PageVO<Notification>> listSystemNotifications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(notificationService.listSystemNotifications(page, pageSize));
    }

    /** 获取系统通知详情 */
    @GetMapping("/admin/notifications/{id}")
    @RequireAdmin
    public Result<Notification> getSystemNotification(@PathVariable Long id) {
        Notification notification = notificationService.getSystemNotificationById(id);
        return Result.ok(notification);
    }

    /** 创建系统通知 */
    @PostMapping("/admin/notifications")
    @RequireAdmin
    public Result<String> createSystemNotification(
            @Valid @RequestBody SystemNotificationRequest request) {
        notificationService.createSystemNotification(request);
        return Result.ok("通知创建成功");
    }

    /** 更新系统通知 */
    @PutMapping("/admin/notifications/{id}")
    @RequireAdmin
    public Result<String> updateSystemNotification(
            @PathVariable Long id,
            @Valid @RequestBody SystemNotificationRequest request) {
        notificationService.updateSystemNotification(id, request);
        return Result.ok("通知更新成功");
    }

    /** 删除系统通知 */
    @DeleteMapping("/admin/notifications/{id}")
    @RequireAdmin
    public Result<String> deleteSystemNotification(@PathVariable Long id) {
        notificationService.deleteSystemNotification(id);
        return Result.ok("通知删除成功");
    }

    /** 获取置顶通知列表 */
    @GetMapping("/admin/notifications/pinned")
    @RequireAdmin
    public Result<List<Notification>> listPinnedNotifications() {
        return Result.ok(notificationService.listPinnedNotifications());
    }
}
