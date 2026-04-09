package com.ttikss.nexatalk.vo;

import com.ttikss.nexatalk.entity.Post;

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
        vo.setContent(post.getContent());
        // 生成摘要：从正文中截取前100个字符
        vo.setSummary(generateSummary(post.getContent(), 100));
        vo.setCoverUrl(post.getCoverUrl());
        // 优先使用 images 字段解析图片列表，如果没有则从正文解析 Markdown 图片
        List<String> images = parseImages(post.getImages());
        if (images == null || images.isEmpty()) {
            images = extractMarkdownImages(post.getContent());
        }
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

    /** 从正文中提取 Markdown 图片 */
    private static List<String> extractMarkdownImages(String content) {
        if (content == null || content.isEmpty()) {
            return Collections.emptyList();
        }
        // 匹配 Markdown 图片语法：![alt](url)
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("!\\[.*?\\]\\((.*?)\\)");
        java.util.regex.Matcher matcher = pattern.matcher(content);
        java.util.List<String> images = new java.util.ArrayList<>();
        while (matcher.find()) {
            String url = matcher.group(1);
            if (url != null && !url.isEmpty()) {
                images.add(url);
            }
        }
        return images;
    }

    /** 生成内容摘要 */
    private static String generateSummary(String content, int maxLength) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        // 先处理图片和链接，避免在摘要中出现 URL
        String plainText = content;
        // 移除 Markdown 图片：![alt](url)
        plainText = plainText.replaceAll("!\\[[^\\]]*]\\([^)]*\\)", " ");
        // 将 Markdown 链接 [text](url) 转为纯文本 text
        plainText = plainText.replaceAll("\\[([^\\]]*)]\\([^)]*\\)", "$1");

        // 再移除其它 Markdown 语法符号
        plainText = plainText
                .replaceAll("#+ ", "")
                .replaceAll("\\*\\*(.+?)\\*\\*", "$1")
                .replaceAll("\\*(.+?)\\*", "$1")
                .replaceAll("~~(.+?)~~", "$1")
                .replaceAll("`(.+?)`", "$1")
                .replaceAll("\\n+", " ")
                .trim();
        if (plainText.length() <= maxLength) {
            return plainText;
        }
        return plainText.substring(0, maxLength) + "...";
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
