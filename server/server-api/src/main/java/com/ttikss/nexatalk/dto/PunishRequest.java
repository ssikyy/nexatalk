package com.ttikss.nexatalk.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 下处罚请求 DTO
 */
public class PunishRequest {

    /** 被处罚用户 ID */
    @NotNull(message = "用户 ID 不能为空")
    private Long userId;

    /** 处罚类型：1=禁言，2=封号 */
    @NotNull(message = "处罚类型不能为空")
    @Min(value = 1, message = "处罚类型非法")
    @Max(value = 2, message = "处罚类型非法")
    private Integer type;

    /** 处罚原因 */
    @Size(max = 512, message = "处罚原因不超过 512 字")
    private String reason;

    /**
     * 处罚时长（小时），null 表示永久
     * 例：24 = 禁言 24 小时，null = 永久封号
     */
    private Integer durationHours;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Integer getType() { return type; }
    public void setType(Integer type) { this.type = type; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Integer getDurationHours() { return durationHours; }
    public void setDurationHours(Integer durationHours) { this.durationHours = durationHours; }
}
