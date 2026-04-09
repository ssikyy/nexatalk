package com.ttikss.nexatalk.dto;

import jakarta.validation.constraints.NotNull;
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
     */
    private Integer role;

    /**
     * 账号状态: 0=正常, 1=禁言, 2=封禁
     */
    private Integer status;
}
