package com.ttikss.nexatalk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * AI 对话请求
 */
@Data
public class AiChatRequest {

    /**
     * 用户消息
     */
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 2000, message = "消息内容不超过 2000 字")
    private String message;

    /**
     * 对话历史（可选），每项格式为 "user:xxx" 或 "assistant:xxx"
     */
    private List<String> history;
}
