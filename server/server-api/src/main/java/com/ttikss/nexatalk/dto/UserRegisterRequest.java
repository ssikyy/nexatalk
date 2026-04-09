package com.ttikss.nexatalk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 用户注册请求体（DTO）
 *
 * 现在这个类除了“接收 JSON”，还负责：
 * - 定义参数规则（比如不能为空）
 * - 让 Spring 在进入 Controller 前自动校验
 */
public class UserRegisterRequest {

    /** 用户名：不能为空、不能全是空格 */
    @NotBlank(message = "username 不能为空")
    private String username;

    /**
     * 密码：
     * - 不能为空
     * - 最少 6 位（MVP 阶段先做基础规则，后续可升级到 8 位或复杂度）
     */
    @NotBlank(message = "password 不能为空")
    @Size(min = 6, message = "password 长度不能少于 6 位")
    private String password;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}