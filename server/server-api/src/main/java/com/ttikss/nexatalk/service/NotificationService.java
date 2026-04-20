package com.ttikss.nexatalk.service;

import com.ttikss.nexatalk.dto.SystemNotificationRequest;
import com.ttikss.nexatalk.entity.Notification;
import com.ttikss.nexatalk.vo.PageVO;
import com.ttikss.nexatalk.vo.NotificationVO;

import java.util.List;

/**
 * 通知模块业务层接口
 *
 * 设计说明：
 * - send() 为内部接口，供其他模块（评论/点赞/关注/审核）调用，不对外暴露 REST
 * - 消息推送方向：触发方 → 被触发方（不对自己通知）
 */
public interface NotificationService {

    /**
     * 发送通知（内部调用）
     *
     * @param toUserId   接收者用户 ID
     * @param actorId    触发者用户 ID（系统通知传 0）
     * @param type       通知类型（参见 Notification 常量）
     * @param entityType 关联实体类型
     * @param entityId   关联实体 ID
     * @param content    通知摘要
     */
    void send(Long toUserId, Long actorId, int type, int entityType, Long entityId, String content);

    /**
     * 查询当前用户的通知列表（分页，按时间倒序）
     *
     * @param userId   当前用户 ID
     * @param page     页码
     * @param pageSize 每页大小
     * @param type     通知类型（可选，筛选特定类型）
     */
    PageVO<NotificationVO> listMyNotifications(Long userId, int page, int pageSize, Integer type);

    /**
     * 获取当前用户未读通知数量
     *
     * @param userId 用户 ID
     * @param type   通知类型（可选，为 null 时统计全部，传 10 仅统计系统通知）
     * @return 未读数
     */
    long countUnread(Long userId, Integer type);

    /**
     * 将当前用户通知标记为已读
     *
     * @param userId 用户 ID
     * @param type   通知类型（可选，为 null 时标记全部，传 10 仅标记系统通知）
     */
    void markAllRead(Long userId, Integer type);

    /**
     * 将单个通知标记为已读
     *
     * @param userId 用户 ID
     * @param notificationId 通知 ID
     */
    void markAsRead(Long userId, Long notificationId);

    /**
     * 删除单条通知
     *
     * @param userId 用户 ID
     * @param notificationId 通知 ID
     */
    void deleteNotification(Long userId, Long notificationId);

    /**
     * 获取当前用户可查看的单条通知详情。
     *
     * @param userId 用户 ID
     * @param notificationId 通知 ID
     * @return 通知详情
     */
    NotificationVO getMyNotificationDetail(Long userId, Long notificationId);

    /**
     * 同步当前用户缺失的系统通知。
     * 用于新用户注册后的历史公告补齐，以及旧账号的漏同步自修复。
     *
     * @param userId 用户 ID
     */
    void syncSystemNotificationsForUser(Long userId);

    // ==================== 管理员接口 ====================

    /**
     * 创建系统通知（管理员发布）
     *
     * @param request 系统通知请求
     */
    void createSystemNotification(SystemNotificationRequest request);

    /**
     * 更新系统通知（管理员操作）
     *
     * @param id      通知ID
     * @param request 系统通知请求
     */
    void updateSystemNotification(Long id, SystemNotificationRequest request);

    /**
     * 删除系统通知（管理员操作）
     *
     * @param id 通知ID
     */
    void deleteSystemNotification(Long id);

    /**
     * 获取系统通知详情
     *
     * @param id 通知ID
     * @return 通知详情
     */
    Notification getSystemNotificationById(Long id);

    /**
     * 获取所有用户最新系统通知（用于通知列表展示）
     * 查询所有类型为系统通知的最新消息
     *
     * @param page     页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    PageVO<Notification> listSystemNotifications(int page, int pageSize);

    /**
     * 获取置顶的系统通知
     *
     * @return 置顶通知列表
     */
    List<Notification> listPinnedNotifications();
}
