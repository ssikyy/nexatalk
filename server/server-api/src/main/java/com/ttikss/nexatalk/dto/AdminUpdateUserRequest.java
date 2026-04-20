package com.ttikss.nexatalk.dto;

import lombok.Data;

/**
 * 管理员更新用户请求
 */
@Data
public class AdminUpdateUserRequest {

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 个人简介
     */
    private String bio;

    /**
     * 角色: 0=普通用户, 1=管理员
     * 超级管理员不允许通过后台直接授予
     */
    private Integer role;

    /**
     * 账号状态: 0=正常, 1=禁言, 2=封禁
     */
    private Integer status;
}
