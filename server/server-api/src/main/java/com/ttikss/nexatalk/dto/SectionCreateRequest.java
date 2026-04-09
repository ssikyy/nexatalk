package com.ttikss.nexatalk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建/更新分区请求体（管理员使用）
 */
public class SectionCreateRequest {

    /** 分区名称 */
    @NotBlank(message = "分区名称不能为空")
    @Size(max = 64, message = "分区名称不能超过 64 个字符")
    private String name;

    /** 分区描述 */
    @Size(max = 255, message = "分区描述不能超过 255 个字符")
    private String description;

    /** 图标 URL */
    private String iconUrl;

    /** 排序权重 */
    private Integer sortOrder;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIconUrl() { return iconUrl; }
    public void setIconUrl(String iconUrl) { this.iconUrl = iconUrl; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
