package com.ttikss.nexatalk.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 系统通知创建/更新请求 DTO
 */
public class SystemNotificationRequest {

    /** 通知标题 */
    @NotBlank(message = "标题不能为空")
    private String title;

    /** 通知内容（纯图片时可为空） */
    private String content;

    /** 内容类型：1-纯文字 2-图片 3-图文混排，由服务端根据 content/imageUrl 推断 */
    private Integer contentType;

    /** 图片URL（当内容类型为图片或图文混排时使用） */
    private String imageUrl;

    /** 是否置顶：0-不置顶 1-置顶 */
    private Integer isPinned = 0;

    /** 是否粗体显示：0-普通 1-粗体 */
    private Integer isBold = 0;

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
}
