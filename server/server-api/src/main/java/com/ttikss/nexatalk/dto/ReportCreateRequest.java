package com.ttikss.nexatalk.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 举报创建请求 DTO
 */
public class ReportCreateRequest {

    /** 举报对象类型：1=帖子，2=评论 */
    @NotNull(message = "举报对象类型不能为空")
    @Min(value = 1, message = "举报对象类型非法")
    @Max(value = 2, message = "举报对象类型非法")
    private Integer entityType;

    /** 被举报实体 ID */
    @NotNull(message = "举报对象 ID 不能为空")
    private Long entityId;

    /** 举报原因：1=垃圾广告，2=违规内容，3=侮辱谩骂，4=其他 */
    @NotNull(message = "举报原因不能为空")
    @Min(value = 1, message = "举报原因非法")
    @Max(value = 4, message = "举报原因非法")
    private Integer reason;

    /** 补充说明（可为空） */
    @Size(max = 512, message = "补充说明不超过 512 字")
    private String detail;

    public Integer getEntityType() { return entityType; }
    public void setEntityType(Integer entityType) { this.entityType = entityType; }
    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }
    public Integer getReason() { return reason; }
    public void setReason(Integer reason) { this.reason = reason; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
}
