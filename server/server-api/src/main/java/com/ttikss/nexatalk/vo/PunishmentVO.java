package com.ttikss.nexatalk.vo;

import java.time.LocalDateTime;

/**
 * 处罚详情VO（管理员查看）
 */
public class PunishmentVO {

    private Long id;
    private Long userId;
    private String userNickname;
    private String userUsername;
    private Integer type;
    private String typeText;
    private String reason;
    private Integer isActive;
    private String isActiveText;
    private LocalDateTime expireAt;
    private LocalDateTime createdAt;

    // Getter & Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUserNickname() { return userNickname; }
    public void setUserNickname(String userNickname) { this.userNickname = userNickname; }
    public String getUserUsername() { return userUsername; }
    public void setUserUsername(String userUsername) { this.userUsername = userUsername; }
    public Integer getType() { return type; }
    public void setType(Integer type) { this.type = type; }
    public String getTypeText() { return typeText; }
    public void setTypeText(String typeText) { this.typeText = typeText; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Integer getIsActive() { return isActive; }
    public void setIsActive(Integer isActive) { this.isActive = isActive; }
    public String getIsActiveText() { return isActiveText; }
    public void setIsActiveText(String isActiveText) { this.isActiveText = isActiveText; }
    public LocalDateTime getExpireAt() { return expireAt; }
    public void setExpireAt(LocalDateTime expireAt) { this.expireAt = expireAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
