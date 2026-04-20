package com.ttikss.nexatalk.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 私信消息实体，对应数据库 message 表
 */
@TableName("message")
public class Message {

    /** 消息类型：0=文本，1=图片，2=语音 */
    public static final int TYPE_TEXT = 0;
    public static final int TYPE_IMAGE = 1;
    public static final int TYPE_VOICE = 2;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("conversation_id")
    private Long conversationId;

    @TableField("sender_id")
    private Long senderId;

    @TableField("receiver_id")
    private Long receiverId;

    private String content;

    /** 消息类型：0=文本，1=图片，2=语音 */
    private Integer type;

    @TableField("is_read")
    private Integer isRead;

    /** 是否已撤回：0=否，1=是 */
    @TableField("is_recalled")
    private Integer isRecalled;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("deleted_at")
    private LocalDateTime deletedAt;

    @TableField("sender_deleted_at")
    private LocalDateTime senderDeletedAt;

    @TableField("receiver_deleted_at")
    private LocalDateTime receiverDeletedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }
    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }
    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getType() { return type; }
    public void setType(Integer type) { this.type = type; }
    public Integer getIsRead() { return isRead; }
    public void setIsRead(Integer isRead) { this.isRead = isRead; }
    public Integer getIsRecalled() { return isRecalled; }
    public void setIsRecalled(Integer isRecalled) { this.isRecalled = isRecalled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
    public LocalDateTime getSenderDeletedAt() { return senderDeletedAt; }
    public void setSenderDeletedAt(LocalDateTime senderDeletedAt) { this.senderDeletedAt = senderDeletedAt; }
    public LocalDateTime getReceiverDeletedAt() { return receiverDeletedAt; }
    public void setReceiverDeletedAt(LocalDateTime receiverDeletedAt) { this.receiverDeletedAt = receiverDeletedAt; }
}
