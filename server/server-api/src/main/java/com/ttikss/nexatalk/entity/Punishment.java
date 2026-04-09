package com.ttikss.nexatalk.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 处罚记录实体，对应数据库 punishment 表
 * 与 user.status 联动：下处罚时同步修改用户状态，解除时还原
 */
@TableName("punishment")
public class Punishment {

    /** 处罚类型：禁言 */
    public static final Integer TYPE_MUTE = 1;
    /** 处罚类型：封号 */
    public static final Integer TYPE_BAN = 2;

    /** 是否生效：生效中 */
    public static final Integer ACTIVE = 1;
    /** 是否生效：已解除 */
    public static final Integer INACTIVE = 0;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("operator_id")
    private Long operatorId;

    private Integer type;

    private String reason;

    /** 处罚到期时间，null=永久 */
    @TableField("expire_at")
    private LocalDateTime expireAt;

    @TableField("is_active")
    private Integer isActive;

    @TableField("created_at")
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
    public Integer getType() { return type; }
    public void setType(Integer type) { this.type = type; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public LocalDateTime getExpireAt() { return expireAt; }
    public void setExpireAt(LocalDateTime expireAt) { this.expireAt = expireAt; }
    public Integer getIsActive() { return isActive; }
    public void setIsActive(Integer isActive) { this.isActive = isActive; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
