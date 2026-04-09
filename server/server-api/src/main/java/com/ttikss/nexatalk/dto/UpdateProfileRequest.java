package com.ttikss.nexatalk.dto;

import jakarta.validation.constraints.Size;

/**
 * 修改用户资料请求体
 */
public class UpdateProfileRequest {

    /** 昵称，可选，最长 64 字符 */
    @Size(max = 64, message = "昵称不能超过 64 个字符")
    private String nickname;

    /** 个人简介，可选，最长 255 字符 */
    @Size(max = 255, message = "简介不能超过 255 个字符")
    private String bio;

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
}
