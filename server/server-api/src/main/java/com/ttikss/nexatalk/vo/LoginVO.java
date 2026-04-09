package com.ttikss.nexatalk.vo;

/**
 * 登录成功响应数据
 * 返回 Token 和当前用户基础信息，前端存储 Token 用于后续请求
 */
public class LoginVO {

    /** JWT Token，前端放入 Authorization: Bearer <token> 请求头 */
    private String token;

    /** 用户 ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 头像 URL */
    private String avatarUrl;

    /** 背景图 URL */
    private String bannerUrl;

    /** 个人简介 */
    private String bio;

    /** 角色：0=普通用户, 1=管理员 */
    private Integer role;

    public LoginVO() {}

    public LoginVO(String token, Long userId, String username,
                   String nickname, String avatarUrl, String bannerUrl,
                   String bio, Integer role) {
        this.token = token;
        this.userId = userId;
        this.username = username;
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
        this.bannerUrl = bannerUrl;
        this.bio = bio;
        this.role = role;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getBannerUrl() { return bannerUrl; }
    public void setBannerUrl(String bannerUrl) { this.bannerUrl = bannerUrl; }

    public Integer getRole() { return role; }
    public void setRole(Integer role) { this.role = role; }
}
