package com.ttikss.nexatalk.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 通知实体，对应数据库 notification 表
 * 统一存储系统、社交、审核各类通知
 */
@TableName("notification")
public class Notification {

    /** 通知类型：收到评论 */
    public static final int TYPE_COMMENT = 1;
    /** 通知类型：帖子被点赞 */
    public static final int TYPE_LIKE_POST = 2;
    /** 通知类型：评论被点赞 */
    public static final int TYPE_LIKE_COMMENT = 3;
    /** 通知类型：被关注 */
    public static final int TYPE_FOLLOW = 4;
    /** 通知类型：帖子审核通过 */
    public static final int TYPE_AUDIT_PASS = 5;
    /** 通知类型：帖子审核不通过 */
    public static final int TYPE_AUDIT_REJECT = 6;
    /** 通知类型：系统通知（管理员发布） */
    public static final int TYPE_SYSTEM = 10;

    /** 内容类型：纯文字 */
    public static final int CONTENT_TYPE_TEXT = 1;
    /** 内容类型：图片 */
    public static final int CONTENT_TYPE_IMAGE = 2;
    /** 内容类型：图文混排 */
    public static final int CONTENT_TYPE_MIXED = 3;

    /** 关联实体类型：无 */
    public static final int ENTITY_TYPE_NONE = 0;
    /** 关联实体类型：帖子 */
    public static final int ENTITY_TYPE_POST = 1;
    /** 关联实体类型：评论 */
    public static final int ENTITY_TYPE_COMMENT = 2;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接收通知的用户 ID */
    @TableField("user_id")
    private Long userId;

    /** 触发通知的用户 ID（0 表示系统通知） */
    @TableField("actor_id")
    private Long actorId;

    private Integer type;

    /** 通知标题 */
    private String title;

    /** 通知内容 */
    private String content;

    /** 内容类型：1-纯文字 2-图片 3-图文混排 */
    @TableField("content_type")
    private Integer contentType;

    /** 图片URL（当内容类型为图片或图文混排时使用） */
    private String imageUrl;

    /** 是否置顶：0-不置顶 1-置顶 */
    @TableField("is_pinned")
    private Integer isPinned;

    /** 是否粗体显示：0-普通 1-粗体 */
    @TableField("is_bold")
    private Integer isBold;

    /** 是否已读：0-未读 1-已读 */
    @TableField("is_read")
    private Integer isRead;

    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 关联实体类型 */
    @TableField("entity_type")
    private Integer entityType;

    /** 关联实体ID */
    @TableField("entity_id")
    private Long entityId;

    /** 系统通知广播ID：同一次发布为每个用户生成一条记录，这些记录共用同一 broadcastId，管理端按广播去重 */
    @TableField("broadcast_id")
    private String broadcastId;

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
    public String getBroadcastId() { return broadcastId; }
    public void setBroadcastId(String broadcastId) { this.broadcastId = broadcastId; }
}
