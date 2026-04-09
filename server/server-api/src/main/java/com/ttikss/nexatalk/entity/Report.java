package com.ttikss.nexatalk.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 举报实体，对应数据库 report 表
 */
@TableName("report")
public class Report {

    /** 举报对象类型：帖子 */
    public static final Integer ENTITY_TYPE_POST = 1;
    /** 举报对象类型：评论 */
    public static final Integer ENTITY_TYPE_COMMENT = 2;

    /** 举报原因：垃圾广告 */
    public static final Integer REASON_SPAM = 1;
    /** 举报原因：违规内容 */
    public static final Integer REASON_ILLEGAL = 2;
    /** 举报原因：侮辱谩骂 */
    public static final Integer REASON_ABUSE = 3;
    /** 举报原因：其他 */
    public static final Integer REASON_OTHER = 4;

    /** 审核状态：待审核 */
    public static final Integer STATUS_PENDING = 0;
    /** 审核状态：已处理-违规 */
    public static final Integer STATUS_VIOLATED = 1;
    /** 审核状态：已处理-无问题 */
    public static final Integer STATUS_INNOCENT = 2;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("reporter_id")
    private Long reporterId;

    @TableField("entity_type")
    private Integer entityType;

    @TableField("entity_id")
    private Long entityId;

    private Integer reason;

    private String detail;

    private Integer status;

    @TableField("reviewer_id")
    private Long reviewerId;

    @TableField("review_note")
    private String reviewNote;

    @TableField("reviewed_at")
    private LocalDateTime reviewedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getReporterId() { return reporterId; }
    public void setReporterId(Long reporterId) { this.reporterId = reporterId; }
    public Integer getEntityType() { return entityType; }
    public void setEntityType(Integer entityType) { this.entityType = entityType; }
    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }
    public Integer getReason() { return reason; }
    public void setReason(Integer reason) { this.reason = reason; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Long getReviewerId() { return reviewerId; }
    public void setReviewerId(Long reviewerId) { this.reviewerId = reviewerId; }
    public String getReviewNote() { return reviewNote; }
    public void setReviewNote(String reviewNote) { this.reviewNote = reviewNote; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
