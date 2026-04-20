package com.ttikss.nexatalk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 创建/更新帖子请求体
 */
public class PostCreateRequest {

    /** 所属分区 ID，发布时必填 */
    @NotNull(message = "分区 ID 不能为空")
    private Long sectionId;

    /** 帖子标题 */
    @NotBlank(message = "帖子标题不能为空")
    @Size(min = 1, max = 100, message = "标题长度需在 1-100 个字符之间")
    private String title;

    /** 帖子正文，优先为富文本 HTML，兼容旧 Markdown 内容 */
    @NotBlank(message = "帖子内容不能为空")
    @Size(min = 1, max = 50000, message = "内容长度不能超过 50000 个字符")
    private String content;

    /** 封面图 URL，可选 */
    private String coverUrl;

    /** 正文图片列表（JSON格式存储URL数组） */
    private List<String> images;

    /**
     * 是否保存为草稿
     * true = 保存草稿（不公开），false 或 null = 直接发布
     */
    private Boolean draft;

    /**
     * 是否使用 AI 润色正文
     * true = 使用 AI 润色内容，false 或 null = 不润色
     */
    private Boolean aiPolish;

    /**
     * 标签列表（最多5个标签）
     */
    @Size(max = 5, message = "最多添加 5 个标签")
    private List<String> tags;

    public Long getSectionId() { return sectionId; }
    public void setSectionId(Long sectionId) { this.sectionId = sectionId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }

    public Boolean getDraft() { return draft; }
    public void setDraft(Boolean draft) { this.draft = draft; }

    public Boolean getAiPolish() { return aiPolish; }
    public void setAiPolish(Boolean aiPolish) { this.aiPolish = aiPolish; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
}
