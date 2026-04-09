package com.ttikss.nexatalk.vo;

import com.ttikss.nexatalk.entity.User;

import java.time.LocalDateTime;

/**
 * 用户信息视图对象
 * 对外暴露的用户信息，屏蔽 password 等敏感字段
 *
 * 注意：role 和 status 字段默认不对外暴露，防止信息泄露
 * 如需获取完整信息，请使用 fromWithAdmin() 方法
 */
public class UserVO {

    private Long id;
    private String username;
    private String nickname;
    private String avatarUrl;
    private String bannerUrl;
    private String bio;
    private Integer role;
    private Integer status;
    private LocalDateTime createdAt;
    /** 扩展字段：关注数 */
    private Long followingCount;
    /** 扩展字段：粉丝数 */
    private Long followersCount;
    /** 扩展字段：帖子数 */
    private Long postCount;
    /** 扩展字段：获赞数 */
    private Long likedCount;
    /** 当前登录用户是否已关注该用户 */
    private Boolean isFollowing;
    /** 是否是当前登录用户本人 */
    private Boolean isSelf;

    /**
     * 从 User 实体转换为 VO（公开信息，不含 role/status）
     */
    public static UserVO from(User user) {
        if (user == null) return null;
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setBannerUrl(user.getBannerUrl());
        vo.setBio(user.getBio());
        // 公开信息不暴露 role 和 status
        vo.setCreatedAt(user.getCreatedAt());
        return vo;
    }

    /**
     * 从 User 实体转换为 VO（管理员版本，包含 role 和 status）
     */
    public static UserVO fromWithAdmin(User user) {
        if (user == null) return null;
        UserVO vo = from(user);
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        return vo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

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

    public Long getFollowingCount() { return followingCount; }
    public void setFollowingCount(Long followingCount) { this.followingCount = followingCount; }

    public Long getFollowersCount() { return followersCount; }
    public void setFollowersCount(Long followersCount) { this.followersCount = followersCount; }

    public Long getPostCount() { return postCount; }
    public void setPostCount(Long postCount) { this.postCount = postCount; }

    public Long getLikedCount() { return likedCount; }
    public void setLikedCount(Long likedCount) { this.likedCount = likedCount; }

    public Boolean getIsFollowing() { return isFollowing; }
    public void setIsFollowing(Boolean isFollowing) { this.isFollowing = isFollowing; }

    public Boolean getIsSelf() { return isSelf; }
    public void setIsSelf(Boolean isSelf) { this.isSelf = isSelf; }
}
