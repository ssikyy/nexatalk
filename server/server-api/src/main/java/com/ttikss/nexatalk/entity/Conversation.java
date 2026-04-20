package com.ttikss.nexatalk.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 私信会话实体，对应数据库 conversation 表
 *
 * 设计说明：
 * - 每对用户唯一一条会话记录（user1_id < user2_id 保证唯一性）
 * - 分别维护 user1_unread / user2_unread 实现会话维度未读计数
 * - last_message 冗余字段用于会话列表预览，避免 JOIN message 表
 */
@TableName("conversation")
public class Conversation {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 较小用户 ID（保证 user1_id < user2_id 的不变式） */
    @TableField("user1_id")
    private Long user1Id;

    /** 较大用户 ID */
    @TableField("user2_id")
    private Long user2Id;

    @TableField("last_message")
    private String lastMessage;

    @TableField("last_message_at")
    private LocalDateTime lastMessageAt;

    @TableField("user1_unread")
    private Integer user1Unread;

    @TableField("user2_unread")
    private Integer user2Unread;

    /** 是否置顶 */
    @TableField("is_pinned")
    private Integer isPinned;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("deleted_at")
    private LocalDateTime deletedAt;

    @TableField("user1_deleted_at")
    private LocalDateTime user1DeletedAt;

    @TableField("user2_deleted_at")
    private LocalDateTime user2DeletedAt;

    @TableField("user1_cleared_at")
    private LocalDateTime user1ClearedAt;

    @TableField("user2_cleared_at")
    private LocalDateTime user2ClearedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUser1Id() { return user1Id; }
    public void setUser1Id(Long user1Id) { this.user1Id = user1Id; }
    public Long getUser2Id() { return user2Id; }
    public void setUser2Id(Long user2Id) { this.user2Id = user2Id; }
    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }
    public LocalDateTime getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(LocalDateTime lastMessageAt) { this.lastMessageAt = lastMessageAt; }
    public Integer getUser1Unread() { return user1Unread; }
    public void setUser1Unread(Integer user1Unread) { this.user1Unread = user1Unread; }
    public Integer getUser2Unread() { return user2Unread; }
    public void setUser2Unread(Integer user2Unread) { this.user2Unread = user2Unread; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
    public Integer getIsPinned() { return isPinned; }
    public void setIsPinned(Integer isPinned) { this.isPinned = isPinned; }
    public LocalDateTime getUser1DeletedAt() { return user1DeletedAt; }
    public void setUser1DeletedAt(LocalDateTime user1DeletedAt) { this.user1DeletedAt = user1DeletedAt; }
    public LocalDateTime getUser2DeletedAt() { return user2DeletedAt; }
    public void setUser2DeletedAt(LocalDateTime user2DeletedAt) { this.user2DeletedAt = user2DeletedAt; }
    public LocalDateTime getUser1ClearedAt() { return user1ClearedAt; }
    public void setUser1ClearedAt(LocalDateTime user1ClearedAt) { this.user1ClearedAt = user1ClearedAt; }
    public LocalDateTime getUser2ClearedAt() { return user2ClearedAt; }
    public void setUser2ClearedAt(LocalDateTime user2ClearedAt) { this.user2ClearedAt = user2ClearedAt; }
}
