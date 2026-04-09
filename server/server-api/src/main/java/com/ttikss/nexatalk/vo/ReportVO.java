package com.ttikss.nexatalk.vo;

import java.time.LocalDateTime;

/**
 * 举报详情VO（管理员查看）
 */
public class ReportVO {

    private Long id;
    private Long reporterId;
    private String reporterUsername;
    private String reporterNickname;

    // 被举报人信息（核心优化）
    private Long reportedUserId;
    private String reportedUsername;
    private String reportedNickname;

    private Integer entityType;
    private String entityTypeText;
    private Long entityId;
    private String entityTitle;      // 帖子标题或评论内容摘要
    private String entityAuthor;     // 被举报内容的作者（昵称）
    private String postContent;      // 帖子正文内容（仅帖子类型有值）
    private Integer reason;
    private String reasonText;
    private String detail;
    private Integer status;
    private String statusText;
    private Long reviewerId;
    private String reviewerNickname;
    private String reviewNote;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;

    // Getter & Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getReporterId() { return reporterId; }
    public void setReporterId(Long reporterId) { this.reporterId = reporterId; }
    public String getReporterUsername() { return reporterUsername; }
    public void setReporterUsername(String reporterUsername) { this.reporterUsername = reporterUsername; }
    public String getReporterNickname() { return reporterNickname; }
    public void setReporterNickname(String reporterNickname) { this.reporterNickname = reporterNickname; }

    public Long getReportedUserId() { return reportedUserId; }
    public void setReportedUserId(Long reportedUserId) { this.reportedUserId = reportedUserId; }
    public String getReportedUsername() { return reportedUsername; }
    public void setReportedUsername(String reportedUsername) { this.reportedUsername = reportedUsername; }
    public String getReportedNickname() { return reportedNickname; }
    public void setReportedNickname(String reportedNickname) { this.reportedNickname = reportedNickname; }

    public Integer getEntityType() { return entityType; }
    public void setEntityType(Integer entityType) { this.entityType = entityType; }
    public String getEntityTypeText() { return entityTypeText; }
    public void setEntityTypeText(String entityTypeText) { this.entityTypeText = entityTypeText; }
    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }
    public String getEntityTitle() { return entityTitle; }
    public void setEntityTitle(String entityTitle) { this.entityTitle = entityTitle; }
    public String getEntityAuthor() { return entityAuthor; }
    public void setEntityAuthor(String entityAuthor) { this.entityAuthor = entityAuthor; }
    public String getPostContent() { return postContent; }
    public void setPostContent(String postContent) { this.postContent = postContent; }
    public Integer getReason() { return reason; }
    public void setReason(Integer reason) { this.reason = reason; }
    public String getReasonText() { return reasonText; }
    public void setReasonText(String reasonText) { this.reasonText = reasonText; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getStatusText() { return statusText; }
    public void setStatusText(String statusText) { this.statusText = statusText; }
    public Long getReviewerId() { return reviewerId; }
    public void setReviewerId(Long reviewerId) { this.reviewerId = reviewerId; }
    public String getReviewerNickname() { return reviewerNickname; }
    public void setReviewerNickname(String reviewerNickname) { this.reviewerNickname = reviewerNickname; }
    public String getReviewNote() { return reviewNote; }
    public void setReviewNote(String reviewNote) { this.reviewNote = reviewNote; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
