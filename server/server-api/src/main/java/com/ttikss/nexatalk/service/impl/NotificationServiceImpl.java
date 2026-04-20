package com.ttikss.nexatalk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ttikss.nexatalk.common.ErrorCode;
import com.ttikss.nexatalk.dto.SystemNotificationRequest;
import com.ttikss.nexatalk.entity.Notification;
import com.ttikss.nexatalk.entity.User;
import com.ttikss.nexatalk.exception.BusinessException;
import com.ttikss.nexatalk.mapper.NotificationMapper;
import com.ttikss.nexatalk.mapper.UserMapper;
import com.ttikss.nexatalk.service.NotificationService;
import com.ttikss.nexatalk.vo.PageVO;
import com.ttikss.nexatalk.vo.NotificationVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 通知模块业务实现
 *
 * 性能设计：
 * - send() 使用同步写入，数据量不大时可满足需求
 * - 如后续消息量较大，可将 send() 改为异步（发到消息队列后台处理）
 */
@Service
public class NotificationServiceImpl implements NotificationService {

    private final Object systemNotificationSyncLock = new Object();

    private final NotificationMapper notificationMapper;
    private final UserMapper userMapper;

    public NotificationServiceImpl(NotificationMapper notificationMapper, UserMapper userMapper) {
        this.notificationMapper = notificationMapper;
        this.userMapper = userMapper;
    }

    @Override
    public void send(Long toUserId, Long actorId, int type, int entityType, Long entityId, String content) {
        // 不给自己发通知，防止空指针
        if (toUserId == null || toUserId.equals(actorId)) return;

        Notification notification = new Notification();
        notification.setUserId(toUserId);
        notification.setActorId(actorId);
        notification.setType(type);
        notification.setEntityType(entityType);
        notification.setEntityId(entityId);
        notification.setContent(content != null ? content : "");
        notification.setIsRead(0);
        notificationMapper.insert(notification);
    }

    @Override
    public PageVO<NotificationVO> listMyNotifications(Long userId, int page, int pageSize, Integer type) {
        prepareSystemNotificationsForUser(userId, type);
        pageSize = Math.min(pageSize, 50);

        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .orderByDesc(Notification::getIsPinned)
                .orderByDesc(Notification::getCreatedAt);

        if (type != null) {
            wrapper.eq(Notification::getType, type);
        }

        Page<Notification> result = notificationMapper.selectPage(new Page<>(page, pageSize), wrapper);

        // 转换为VO并填充触发者信息
        List<NotificationVO> voList = result.getRecords().stream()
                .map(NotificationVO::from)
                .collect(Collectors.toList());

        // 批量填充触发者用户信息
        fillActorInfo(voList);

        return PageVO.of(result, voList);
    }

    /** 批量填充通知的触发者信息 */
    private void fillActorInfo(List<NotificationVO> voList) {
        if (voList.isEmpty()) return;

        // 找出需要查询用户的通知（actorId > 0 表示有触发者）
        List<Long> actorIds = voList.stream()
                .map(NotificationVO::getActorId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .collect(Collectors.toList());

        if (actorIds.isEmpty()) return;

        Map<Long, User> userMap = userMapper.selectBatchIds(actorIds)
                .stream().collect(Collectors.toMap(User::getId, u -> u));

        voList.forEach(vo -> {
            if (vo.getActorId() != null && vo.getActorId() > 0) {
                User actor = userMap.get(vo.getActorId());
                if (actor != null) {
                    vo.setActorUserId(actor.getId());
                    vo.setActorNickname(actor.getNickname());
                    vo.setActorAvatar(actor.getAvatarUrl());
                }
            }
        });
    }

    @Override
    public long countUnread(Long userId, Integer type) {
        prepareSystemNotificationsForUser(userId, type);
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0);
        if (type != null) {
            wrapper.eq(Notification::getType, type);
        }
        return notificationMapper.selectCount(wrapper);
    }

    @Override
    public void markAllRead(Long userId, Integer type) {
        prepareSystemNotificationsForUser(userId, type);
        if (type == null) {
            notificationMapper.markAllRead(userId);
        } else {
            notificationMapper.markAllReadByType(userId, type);
        }
    }

    @Override
    public void markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification != null && notification.getUserId().equals(userId)) {
            notification.setIsRead(1);
            notificationMapper.updateById(notification);
        }
    }

    @Override
    public void deleteNotification(Long userId, Long notificationId) {
        notificationMapper.delete(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getId, notificationId)
                .eq(Notification::getUserId, userId));
    }

    @Override
    public NotificationVO getMyNotificationDetail(Long userId, Long notificationId) {
        Notification notification = notificationMapper.selectOne(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getId, notificationId)
                        .eq(Notification::getUserId, userId)
        );
        if (notification == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.code(), "通知不存在");
        }

        if (notification.getIsRead() != null && notification.getIsRead() == 0) {
            notification.setIsRead(1);
            notificationMapper.updateById(notification);
        }

        NotificationVO vo = NotificationVO.from(notification);
        fillActorInfo(java.util.List.of(vo));
        return vo;
    }

    @Override
    public void syncSystemNotificationsForUser(Long userId) {
        if (userId == null) {
            return;
        }

        synchronized (systemNotificationSyncLock) {
            normalizeLegacySystemNotifications();

            Map<String, Notification> templateMap = loadSystemNotificationTemplates();
            if (templateMap.isEmpty()) {
                return;
            }

            Set<String> existingKeys = notificationMapper.selectList(
                    new LambdaQueryWrapper<Notification>()
                            .eq(Notification::getUserId, userId)
                            .eq(Notification::getType, Notification.TYPE_SYSTEM)
                            .orderByDesc(Notification::getCreatedAt)
                            .orderByAsc(Notification::getId)
            ).stream()
                    .map(this::buildSystemNotificationKey)
                    .collect(Collectors.toSet());

            List<Notification> missingNotifications = new ArrayList<>();
            for (Map.Entry<String, Notification> entry : templateMap.entrySet()) {
                if (existingKeys.contains(entry.getKey())) {
                    continue;
                }
                missingNotifications.add(copySystemNotificationForUser(entry.getValue(), userId));
            }

            if (!missingNotifications.isEmpty()) {
                notificationMapper.batchInsert(missingNotifications);
            }
        }
    }

    // ==================== 管理员接口实现 ====================

    /** 根据 content 与 imageUrl 推断内容类型，并规范化 content */
    private static void normalizeRequest(SystemNotificationRequest request) {
        String content = request.getContent() != null ? request.getContent().trim() : "";
        String imageUrl = request.getImageUrl() != null ? request.getImageUrl().trim() : "";
        if (content.isEmpty() && imageUrl.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.code(), "内容与图片至少填一项");
        }

        // 确保 content 不为空
        request.setContent(content.isEmpty() ? " " : content);

        // 推断并设置内容类型（如果前端未传，则根据内容自动推断）
        if (request.getContentType() == null) {
            int type = imageUrl.isEmpty() ? Notification.CONTENT_TYPE_TEXT
                    : content.isEmpty() || content.equals(" ") ? Notification.CONTENT_TYPE_IMAGE
                    : Notification.CONTENT_TYPE_MIXED;
            request.setContentType(type);
        }

        // 双重保险：确保 contentType 永不为 null
        if (request.getContentType() == null) {
            request.setContentType(Notification.CONTENT_TYPE_TEXT);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createSystemNotification(SystemNotificationRequest request) {
        normalizeRequest(request);
        normalizeLegacySystemNotifications();

        // 查询所有用户ID，为每个用户创建一条通知（同一次发布共用同一 broadcastId，管理端按广播去重展示）
        List<Long> userIds = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .select(User::getId)
        ).stream().map(User::getId).collect(Collectors.toList());

        if (userIds.isEmpty()) {
            return;
        }

        String broadcastId = UUID.randomUUID().toString();
        LocalDateTime createdAt = LocalDateTime.now();

        // 构建通知列表
        List<Notification> notifications = userIds.stream()
                .map(userId -> {
                    Notification notification = new Notification();
                    notification.setUserId(userId);
                    notification.setActorId(0L);
                    notification.setType(Notification.TYPE_SYSTEM);
                    notification.setTitle(request.getTitle());
                    notification.setContent(request.getContent());
                    notification.setContentType(request.getContentType());
                    notification.setImageUrl(request.getImageUrl() != null ? request.getImageUrl().trim() : "");
                    notification.setIsPinned(request.getIsPinned() != null ? request.getIsPinned() : 0);
                    notification.setIsBold(request.getIsBold() != null ? request.getIsBold() : 0);
                    notification.setIsRead(0);
                    notification.setEntityType(Notification.ENTITY_TYPE_NONE);
                    notification.setEntityId(0L);
                    notification.setBroadcastId(broadcastId);
                    notification.setCreatedAt(createdAt);
                    return notification;
                })
                .collect(Collectors.toList());

        // 批量插入（性能更好）
        notificationMapper.batchInsert(notifications);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSystemNotification(Long id, SystemNotificationRequest request) {
        normalizeRequest(request);
        normalizeLegacySystemNotifications();
        Notification notification = notificationMapper.selectById(id);
        if (notification == null) {
            throw new RuntimeException("通知不存在");
        }
        notification.setTitle(request.getTitle());
        notification.setContent(request.getContent());
        notification.setContentType(request.getContentType());
        notification.setImageUrl(request.getImageUrl() != null ? request.getImageUrl().trim() : "");
        notification.setIsPinned(request.getIsPinned() != null ? request.getIsPinned() : 0);
        notification.setIsBold(request.getIsBold() != null ? request.getIsBold() : 0);
        String broadcastId = notification.getBroadcastId();
        if (broadcastId != null && !broadcastId.isEmpty()) {
            notificationMapper.updateByBroadcastId(broadcastId, notification);
        } else {
            notificationMapper.updateById(notification);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSystemNotification(Long id) {
        normalizeLegacySystemNotifications();
        Notification notification = notificationMapper.selectById(id);
        if (notification == null) {
            return;
        }
        String broadcastId = notification.getBroadcastId();
        if (broadcastId != null && !broadcastId.isEmpty()) {
            notificationMapper.deleteByBroadcastId(broadcastId);
        } else {
            notificationMapper.deleteById(id);
        }
    }

    @Override
    public Notification getSystemNotificationById(Long id) {
        normalizeLegacySystemNotifications();
        Notification notification = notificationMapper.selectById(id);
        if (notification == null || notification.getType() == null
                || notification.getType() != Notification.TYPE_SYSTEM) {
            throw new BusinessException(ErrorCode.NOT_FOUND.code(), "通知不存在");
        }
        return notification;
    }

    @Override
    public PageVO<Notification> listSystemNotifications(int page, int pageSize) {
        normalizeLegacySystemNotifications();
        pageSize = Math.min(pageSize, 50);
        long total = notificationMapper.countDistinctSystemNotifications();
        long offset = (long) (page - 1) * pageSize;
        List<Notification> list = notificationMapper.selectDistinctSystemNotifications(offset, pageSize);
        PageVO<Notification> vo = new PageVO<>();
        vo.setList(list);
        vo.setPage(page);
        vo.setPageSize(pageSize);
        vo.setTotal(total);
        vo.setTotalPages(total > 0 ? (total + pageSize - 1) / pageSize : 0);
        return vo;
    }

    @Override
    public List<Notification> listPinnedNotifications() {
        normalizeLegacySystemNotifications();
        return loadSystemNotificationTemplates().values().stream()
                .filter(notification -> Objects.equals(notification.getIsPinned(), 1))
                .limit(5)
                .collect(Collectors.toList());
    }

    private void prepareSystemNotificationsForUser(Long userId, Integer type) {
        if (userId == null) {
            return;
        }
        if (type == null || Objects.equals(type, Notification.TYPE_SYSTEM)) {
            syncSystemNotificationsForUser(userId);
        }
    }

    private void normalizeLegacySystemNotifications() {
        List<Notification> legacyNotifications = notificationMapper.selectList(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getType, Notification.TYPE_SYSTEM)
                        .isNull(Notification::getBroadcastId)
                        .orderByAsc(Notification::getCreatedAt)
                        .orderByAsc(Notification::getId)
        );

        if (legacyNotifications.isEmpty()) {
            return;
        }

        Map<String, List<Notification>> groupedNotifications = new LinkedHashMap<>();
        for (Notification notification : legacyNotifications) {
            groupedNotifications
                    .computeIfAbsent(buildLegacySystemNotificationKey(notification), key -> new ArrayList<>())
                    .add(notification);
        }

        for (List<Notification> group : groupedNotifications.values()) {
            if (group.isEmpty()) {
                continue;
            }
            String broadcastId = "legacy-" + group.get(0).getId();
            List<Long> ids = group.stream().map(Notification::getId).toList();
            notificationMapper.update(
                    null,
                    new LambdaUpdateWrapper<Notification>()
                            .in(Notification::getId, ids)
                            .set(Notification::getBroadcastId, broadcastId)
            );
        }
    }

    private Map<String, Notification> loadSystemNotificationTemplates() {
        Map<String, Notification> templateMap = new LinkedHashMap<>();
        List<Notification> notifications = notificationMapper.selectList(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getType, Notification.TYPE_SYSTEM)
                        .orderByDesc(Notification::getIsPinned)
                        .orderByDesc(Notification::getCreatedAt)
                        .orderByAsc(Notification::getId)
        );

        for (Notification notification : notifications) {
            templateMap.putIfAbsent(buildSystemNotificationKey(notification), notification);
        }

        return templateMap;
    }

    private Notification copySystemNotificationForUser(Notification template, Long userId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setActorId(0L);
        notification.setType(Notification.TYPE_SYSTEM);
        notification.setTitle(template.getTitle());
        notification.setContent(template.getContent());
        notification.setContentType(template.getContentType());
        notification.setImageUrl(template.getImageUrl());
        notification.setIsPinned(template.getIsPinned() != null ? template.getIsPinned() : 0);
        notification.setIsBold(template.getIsBold() != null ? template.getIsBold() : 0);
        notification.setIsRead(0);
        notification.setEntityType(template.getEntityType() != null ? template.getEntityType() : Notification.ENTITY_TYPE_NONE);
        notification.setEntityId(template.getEntityId() != null ? template.getEntityId() : 0L);
        notification.setBroadcastId(template.getBroadcastId());
        notification.setCreatedAt(template.getCreatedAt() != null ? template.getCreatedAt() : LocalDateTime.now());
        return notification;
    }

    private String buildSystemNotificationKey(Notification notification) {
        if (notification == null) {
            return "";
        }
        String broadcastId = notification.getBroadcastId();
        if (broadcastId != null && !broadcastId.isBlank()) {
            return "broadcast:" + broadcastId;
        }
        return buildLegacySystemNotificationKey(notification);
    }

    private String buildLegacySystemNotificationKey(Notification notification) {
        return String.join("|",
                "legacy",
                String.valueOf(notification.getType()),
                safeValue(notification.getTitle()),
                safeValue(notification.getContent()),
                String.valueOf(notification.getContentType()),
                safeValue(notification.getImageUrl()),
                String.valueOf(notification.getIsPinned()),
                String.valueOf(notification.getIsBold()),
                String.valueOf(notification.getCreatedAt())
        );
    }

    private String safeValue(String value) {
        return value == null ? "" : value;
    }
}
