package com.ttikss.nexatalk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 创建评论/回复请求体
 */
public class CommentCreateRequest {

    /** 帖子 ID */
    @NotNull(message = "帖子 ID 不能为空")
    private Long postId;

    /**
     * 父评论 ID
     * - 0 或 null：表示一级评论（直接评论帖子）
     * - 非 0：表示二级回复（回复某条评论）
     */
    private Long parentId;

    /** 评论内容 */
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 2000, message = "评论内容不能超过 2000 个字符")
    private String content;

    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
