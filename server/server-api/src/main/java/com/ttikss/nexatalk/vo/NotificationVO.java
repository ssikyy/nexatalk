package com.ttikss.nexatalk.vo;

import com.ttikss.nexatalk.entity.Notification;

import java.time.LocalDateTime;

/**
 * 通知视图对象
 * 对外暴露的通知信息，包含触发者用户信息
 */
public class NotificationVO {

    private Long id;
    private Long userId;
    private Long actorId;
    private Integer type;
    private String title;
    private String content;
    private Integer contentType;
    private String imageUrl;
    private Integer isPinned;
    private Integer isBold;
    private Integer isRead;
    private LocalDateTime createdAt;
    private Integer entityType;
    private Long entityId;

    /** 触发者用户信息 */
    private Long actorUserId;
    private String actorNickname;
    private String actorAvatar;

    /** 通知类型文本 */
    private String typeText;

    public static NotificationVO from(Notification notification) {
        if (notification == null) return null;
        NotificationVO vo = new NotificationVO();
        vo.setId(notification.getId());
        vo.setUserId(notification.getUserId());
        vo.setActorId(notification.getActorId());
        vo.setType(notification.getType());
        vo.setTitle(notification.getTitle());
        vo.setContent(notification.getContent());
        vo.setContentType(notification.getContentType());
        vo.setImageUrl(notification.getImageUrl());
        vo.setIsPinned(notification.getIsPinned());
        vo.setIsBold(notification.getIsBold());
        vo.setIsRead(notification.getIsRead());
        vo.setCreatedAt(notification.getCreatedAt());
        vo.setEntityType(notification.getEntityType());
        vo.setEntityId(notification.getEntityId());
        vo.setTypeText(getTypeText(notification.getType()));
        return vo;
    }

    private static String getTypeText(Integer type) {
        if (type == null) return "未知";
        return switch (type) {
            case Notification.TYPE_COMMENT -> "评论";
            case Notification.TYPE_LIKE_POST -> "点赞帖子";
            case Notification.TYPE_LIKE_COMMENT -> "点赞评论";
            case Notification.TYPE_FOLLOW -> "关注";
            case Notification.TYPE_AUDIT_PASS -> "审核通过";
            case Notification.TYPE_AUDIT_REJECT -> "审核不通过";
            case Notification.TYPE_SYSTEM -> "系统通知";
            default -> "未知";
        };
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getActorId() { return actorId; }
    public void setActorId(Long actorId) { this.actorId = actorId; }
    public Integer getType() { return type; }
    public void setType(Integer type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getContentType() { return contentType; }
    public void setContentType(Integer contentType) { this.contentType = contentType; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public Integer getIsPinned() { return isPinned; }
    public void setIsPinned(Integer isPinned) { this.isPinned = isPinned; }
    public Integer getIsBold() { return isBold; }
    public void setIsBold(Integer isBold) { this.isBold = isBold; }
    public Integer getIsRead() { return isRead; }
    public void setIsRead(Integer isRead) { this.isRead = isRead; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Integer getEntityType() { return entityType; }
    public void setEntityType(Integer entityType) { this.entityType = entityType; }
    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }
    public Long getActorUserId() { return actorUserId; }
    public void setActorUserId(Long actorUserId) { this.actorUserId = actorUserId; }
    public String getActorNickname() { return actorNickname; }
    public void setActorNickname(String actorNickname) { this.actorNickname = actorNickname; }
    public String getActorAvatar() { return actorAvatar; }
    public void setActorAvatar(String actorAvatar) { this.actorAvatar = actorAvatar; }
    public String getTypeText() { return typeText; }
    public void setTypeText(String typeText) { this.typeText = typeText; }
}
