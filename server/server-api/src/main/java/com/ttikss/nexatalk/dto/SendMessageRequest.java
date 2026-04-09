package com.ttikss.nexatalk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 发送私信请求 DTO
 */
public class SendMessageRequest {

    /** 接收方用户 ID */
    @NotNull(message = "接收方用户 ID 不能为空")
    private Long receiverId;

    /** 消息内容 */
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 5000, message = "消息内容不超过 5000 字")
    private String content;

    /** 消息类型：0=文本（默认），1=图片，2=语音 */
    private Integer type;

    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getType() { return type; }
    public void setType(Integer type) { this.type = type; }
}
