package com.ttikss.nexatalk.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * AI 配置更新请求
 */
@Data
public class AiConfigRequest {

    /**
     * 配置键
     */
    @NotBlank(message = "配置键不能为空")
    private String key;

    /**
     * 配置值
     */
    @NotBlank(message = "配置值不能为空")
    private String value;

    /**
     * 配置描述
     */
    private String description;
}
