package com.ttikss.nexatalk.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 帖子实体类，对应数据库 post 表
 */
@TableName("post")
public class Post {

    /** 帖子状态：正常 */
    public static final int STATUS_NORMAL = 0;
    /** 帖子状态：草稿（仅作者可见） */
    public static final int STATUS_DRAFT = 1;
    /** 帖子状态：待审核 */
    public static final int STATUS_PENDING = 2;
    /** 帖子状态：下架 */
    public static final int STATUS_HIDDEN = 3;
    /** 帖子状态：已删除（逻辑删除） */
    public static final int STATUS_DELETED = 4;

    /** 内容最大长度 */
    public static final int MAX_CONTENT_LENGTH = 50000;
    /** 标题最大长度 */
    public static final int MAX_TITLE_LENGTH = 100;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 发帖用户 ID */
    @TableField("user_id")
    private Long userId;

    /** 所属分区 ID */
    @TableField("section_id")
    private Long sectionId;

    /** 帖子标题 */
    private String title;

    /** 帖子正文（优先存储富文本 HTML，兼容旧 Markdown） */
    private String content;

    /** 封面图 URL */
    @TableField("cover_url")
    private String coverUrl;

    /** 正文图片列表（JSON格式存储URL数组） */
    @TableField("images")
    private String images;

    /** 标签列表（JSON格式存储标签数组） */
    @TableField("tags")
    private String tags;

    /**
     * 帖子状态：0=正常, 1=草稿, 2=待审核, 3=下架, 4=已删除
     * 使用常量 STATUS_* 引用
     */
    private Integer status;

    /** 浏览次数 */
    @TableField("view_count")
    private Integer viewCount;

    /** 点赞数（冗余字段，提升列表查询性能） */
    @TableField("like_count")
    private Integer likeCount;

    /** 评论数（冗余字段） */
    @TableField("comment_count")
    private Integer commentCount;

    /** 收藏数（冗余字段） */
    @TableField("favorite_count")
    private Integer favoriteCount;

    /** 分享次数（冗余字段） */
    @TableField("share_count")
    private Integer shareCount;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getSectionId() { return sectionId; }
    public void setSectionId(Long sectionId) { this.sectionId = sectionId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    public String getImages() { return images; }
    public void setImages(String images) { this.images = images; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public Integer getViewCount() { return viewCount; }
    public void setViewCount(Integer viewCount) { this.viewCount = viewCount; }

    public Integer getLikeCount() { return likeCount; }
    public void setLikeCount(Integer likeCount) { this.likeCount = likeCount; }

    public Integer getCommentCount() { return commentCount; }
    public void setCommentCount(Integer commentCount) { this.commentCount = commentCount; }

    public Integer getFavoriteCount() { return favoriteCount; }
    public void setFavoriteCount(Integer favoriteCount) { this.favoriteCount = favoriteCount; }

    public Integer getShareCount() { return shareCount; }
    public void setShareCount(Integer shareCount) { this.shareCount = shareCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
