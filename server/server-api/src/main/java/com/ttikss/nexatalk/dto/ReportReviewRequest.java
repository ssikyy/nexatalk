package com.ttikss.nexatalk.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 管理员审核举报请求 DTO
 */
public class ReportReviewRequest {

    /** 审核结果：1=违规，2=无问题 */
    @NotNull(message = "审核结果不能为空")
    @Min(value = 1, message = "审核结果非法")
    @Max(value = 2, message = "审核结果非法")
    private Integer status;

    /** 是否隐藏被举报内容：true=隐藏，false=不隐藏（仅违规时有效） */
    private Boolean hideContent;

    /** 处罚类型：1=禁言，2=封号（可选，不传表示不处罚） */
    @Min(value = 1, message = "处罚类型非法")
    @Max(value = 2, message = "处罚类型非法")
    private Integer punishType;

    /** 处罚时长（小时），仅禁言时有效，null 表示永久禁言 */
    private Integer punishDurationHours;

    /** 处罚原因 */
    @Size(max = 512, message = "处罚原因不超过 512 字")
    private String punishReason;

    /** 审核备注 */
    @Size(max = 512, message = "审核备注不超过 512 字")
    private String reviewNote;

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Boolean getHideContent() { return hideContent; }
    public void setHideContent(Boolean hideContent) { this.hideContent = hideContent; }
    public Integer getPunishType() { return punishType; }
    public void setPunishType(Integer punishType) { this.punishType = punishType; }
    public Integer getPunishDurationHours() { return punishDurationHours; }
    public void setPunishDurationHours(Integer punishDurationHours) { this.punishDurationHours = punishDurationHours; }
    public String getPunishReason() { return punishReason; }
    public void setPunishReason(String punishReason) { this.punishReason = punishReason; }
    public String getReviewNote() { return reviewNote; }
    public void setReviewNote(String reviewNote) { this.reviewNote = reviewNote; }
}
