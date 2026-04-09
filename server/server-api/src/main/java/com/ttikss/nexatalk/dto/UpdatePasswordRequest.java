package com.ttikss.nexatalk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 修改密码请求体
 */
public class UpdatePasswordRequest {

    /** 当前密码（用于校验身份） */
    @NotBlank(message = "当前密码不能为空")
    private String oldPassword;

    /** 新密码，至少 6 位 */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, message = "新密码不能少于 6 位")
    private String newPassword;

    public String getOldPassword() { return oldPassword; }
    public void setOldPassword(String oldPassword) { this.oldPassword = oldPassword; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
