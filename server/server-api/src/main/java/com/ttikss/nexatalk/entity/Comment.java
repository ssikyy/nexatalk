package com.ttikss.nexatalk.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 评论实体类，对应数据库 comment 表
 *
 * 层级设计：
 * - parentId = 0：一级评论（直接评论帖子）
 * - parentId != 0：二级回复（回复某条评论）
 * - rootId：根评论 ID，二级回复的 rootId = 一级评论 ID，方便批量查某楼的所有回复
 */
@TableName("comment")
public class Comment {

    /** 评论状态：正常 */
    public static final int STATUS_NORMAL = 0;
    /** 评论状态：已删除 */
    public static final int STATUS_DELETED = 1;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属帖子 ID */
    @TableField("post_id")
    private Long postId;

    /** 评论者用户 ID */
    @TableField("user_id")
    private Long userId;

    /** 父评论 ID：0 表示一级评论，非 0 表示二级回复 */
    @TableField("parent_id")
    private Long parentId;

    /** 根评论 ID：用于快速查某一楼所有回复；一级评论的 rootId = 0 */
    @TableField("root_id")
    private Long rootId;

    /** 评论正文 */
    private String content;

    /** 状态：0=正常, 1=已删除 */
    private Integer status;

    /** 点赞数（冗余字段） */
    @TableField("like_count")
    private Integer likeCount;

    /** 回复数（冗余字段，仅一级评论有效） */
    @TableField("reply_count")
    private Integer replyCount;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }

    public Long getRootId() { return rootId; }
    public void setRootId(Long rootId) { this.rootId = rootId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public Integer getLikeCount() { return likeCount; }
    public void setLikeCount(Integer likeCount) { this.likeCount = likeCount; }

    public Integer getReplyCount() { return replyCount; }
    public void setReplyCount(Integer replyCount) { this.replyCount = replyCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
