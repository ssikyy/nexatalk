package com.ttikss.nexatalk.vo;

import com.ttikss.nexatalk.entity.Comment;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论视图对象
 * 一级评论包含 replies 子列表（部分回复，展示最新 N 条）
 */
public class CommentVO {

    private Long id;
    private Long postId;
    private String postTitle;
    private Long userId;
    private String authorName;
    private String authorAvatar;
    private Long parentId;
    private Long rootId;
    private String content;
    private Integer likeCount;
    private Integer replyCount;
    private LocalDateTime createdAt;

    /** 子回复列表（仅一级评论时有值，二级回复时为 null） */
    private List<CommentVO> replies;

    public static CommentVO from(Comment comment) {
        CommentVO vo = new CommentVO();
        vo.setId(comment.getId());
        vo.setPostId(comment.getPostId());
        vo.setUserId(comment.getUserId());
        vo.setParentId(comment.getParentId());
        vo.setRootId(comment.getRootId());
        // 已删除的评论内容替换为提示文本
        if (Integer.valueOf(Comment.STATUS_DELETED).equals(comment.getStatus())) {
            vo.setContent("该评论已被删除");
        } else {
            vo.setContent(comment.getContent());
        }
        vo.setLikeCount(comment.getLikeCount());
        vo.setReplyCount(comment.getReplyCount());
        vo.setCreatedAt(comment.getCreatedAt());
        return vo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }

    public String getPostTitle() { return postTitle; }
    public void setPostTitle(String postTitle) { this.postTitle = postTitle; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getAuthorAvatar() { return authorAvatar; }
    public void setAuthorAvatar(String authorAvatar) { this.authorAvatar = authorAvatar; }

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }

    public Long getRootId() { return rootId; }
    public void setRootId(Long rootId) { this.rootId = rootId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Integer getLikeCount() { return likeCount; }
    public void setLikeCount(Integer likeCount) { this.likeCount = likeCount; }

    public Integer getReplyCount() { return replyCount; }
    public void setReplyCount(Integer replyCount) { this.replyCount = replyCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<CommentVO> getReplies() { return replies; }
    public void setReplies(List<CommentVO> replies) { this.replies = replies; }
}
