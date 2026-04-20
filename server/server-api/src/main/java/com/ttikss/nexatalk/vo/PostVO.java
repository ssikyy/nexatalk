package com.ttikss.nexatalk.vo;

import com.ttikss.nexatalk.entity.Post;
import com.ttikss.nexatalk.util.PostContentUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 帖子列表/详情视图对象
 * 列表页不返回 content（减少流量），详情页返回全部字段
 */
public class PostVO {

    private Long id;
    private Long userId;
    private String authorName;    // 作者昵称（关联查询填充）
    private String authorAvatar;  // 作者头像（关联查询填充）
    private Long sectionId;
    private String sectionName;   // 分区名称（关联查询填充）
    private String title;
    private String content;       // 列表时为 null，详情时有值
    private String summary;        // 摘要：从正文截取
    private String coverUrl;
    private List<String> images;   // 正文图片列表
    private Integer status;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer favoriteCount;
    private Integer shareCount;
    private Boolean isLiked;     // 当前用户是否点赞（登录时有值）
    private Boolean isFavorited; // 当前用户是否收藏（登录时有值）
    private Boolean isFollowed;   // 当前用户是否关注了作者（登录时有值）
    private Boolean isBlocked;   // 当前用户是否被作者拉黑（登录时有值）
    private List<String> tags;   // 标签列表
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 从 Post 实体构建基础 VO */
    public static PostVO from(Post post) {
        PostVO vo = new PostVO();
        vo.setId(post.getId());
        vo.setUserId(post.getUserId());
        vo.setSectionId(post.getSectionId());
        vo.setTitle(post.getTitle());
        String normalizedContent = PostContentUtils.normalizeContent(post.getContent());
        vo.setContent(normalizedContent);
        // 生成摘要：兼容富文本 HTML 和旧 Markdown
        vo.setSummary(PostContentUtils.generateSummary(normalizedContent, 100));
        vo.setCoverUrl(PostContentUtils.normalizeMediaUrl(post.getCoverUrl()));
        // 优先以正文中的真实图片为准，兼容旧数据中仅存 images JSON 的情况
        List<String> images = PostContentUtils.resolveImages(normalizedContent, parseImages(post.getImages()));
        vo.setImages(images);
        vo.setStatus(post.getStatus());
        vo.setViewCount(post.getViewCount());
        vo.setLikeCount(post.getLikeCount());
        vo.setCommentCount(post.getCommentCount());
        vo.setFavoriteCount(post.getFavoriteCount());
        vo.setShareCount(post.getShareCount());
        // 解析标签列表
        vo.setTags(parseTags(post.getTags()));
        vo.setCreatedAt(post.getCreatedAt());
        vo.setUpdatedAt(post.getUpdatedAt());
        return vo;
    }

    /** 解析 tags JSON 字符串为 List */
    private static List<String> parseTags(String tagsJson) {
        if (tagsJson == null || tagsJson.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(tagsJson,
                new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /** 解析 images JSON 字符串为 List */
    private static List<String> parseImages(String imagesJson) {
        if (imagesJson == null || imagesJson.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(imagesJson,
                new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
        } catch (Exception e) {
            // 解析失败返回空列表
            return Collections.emptyList();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getAuthorAvatar() { return authorAvatar; }
    public void setAuthorAvatar(String authorAvatar) { this.authorAvatar = authorAvatar; }

    public Long getSectionId() { return sectionId; }
    public void setSectionId(Long sectionId) { this.sectionId = sectionId; }

    public String getSectionName() { return sectionName; }
    public void setSectionName(String sectionName) { this.sectionName = sectionName; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }

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

    public Boolean getIsLiked() { return isLiked; }
    public void setIsLiked(Boolean isLiked) { this.isLiked = isLiked; }

    public Boolean getIsFavorited() { return isFavorited; }
    public void setIsFavorited(Boolean isFavorited) { this.isFavorited = isFavorited; }

    public Boolean getIsFollowed() { return isFollowed; }
    public void setIsFollowed(Boolean isFollowed) { this.isFollowed = isFollowed; }

    public Boolean getIsBlocked() { return isBlocked; }
    public void setIsBlocked(Boolean isBlocked) { this.isBlocked = isBlocked; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
