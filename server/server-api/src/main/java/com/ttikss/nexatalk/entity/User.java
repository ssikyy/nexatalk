package com.ttikss.nexatalk.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 用户实体类
 * 对应数据库 user 表，包含用户基础信息、角色与账号状态
 */
@TableName("user")
public class User {

    /** 用户角色：普通用户 */
    public static final int ROLE_USER = 0;
    /** 用户角色：管理员 */
    public static final int ROLE_ADMIN = 1;
    /** 用户角色：超级管理员 */
    public static final int ROLE_SUPER_ADMIN = 2;
    /** 用户角色：AI助手 */
    public static final int ROLE_AI = 3;

    /** 账号状态：正常 */
    public static final int STATUS_NORMAL = 0;
    /** 账号状态：禁言（可登录，但不能发帖/评论/私信） */
    public static final int STATUS_MUTED = 1;
    /** 账号状态：封号（无法登录） */
    public static final int STATUS_BANNED = 2;

    /** 主键 ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名，唯一，用于登录 */
    private String username;

    /** BCrypt 加密后的密码，永不存明文 */
    private String password;

    /** 昵称，用于页面展示 */
    private String nickname;

    /** 头像 URL */
    @TableField("avatar_url")
    private String avatarUrl;

    /** 背景图 URL */
    @TableField("banner_url")
    private String bannerUrl;

    /** 个人简介 */
    private String bio;

    /**
     * 角色：0=普通用户, 1=管理员, 2=超级管理员
     * 使用常量 ROLE_USER / ROLE_ADMIN / ROLE_SUPER_ADMIN
     */
    private Integer role;

    /**
     * 账号状态：0=正常, 1=禁言, 2=封号
     * 使用常量 STATUS_NORMAL / STATUS_MUTED / STATUS_BANNED
     */
    private Integer status;

    /** 注册时间 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 最后更新时间 */
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    // ===== Getter / Setter =====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getBannerUrl() { return bannerUrl; }
    public void setBannerUrl(String bannerUrl) { this.bannerUrl = bannerUrl; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public Integer getRole() { return role; }
    public void setRole(Integer role) { this.role = role; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
