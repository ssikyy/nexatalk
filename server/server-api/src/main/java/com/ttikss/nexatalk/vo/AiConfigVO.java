package com.ttikss.nexatalk.vo;

import lombok.Data;

/**
 * AI 配置 VO
 */
@Data
public class AiConfigVO {

    /**
     * 是否开启 AI 功能
     */
    private boolean enabled;

    /**
     * API Base URL
     */
    private String baseUrl;

    /**
     * API Key (脱敏显示)
     */
    private String apiKey;

    /**
     * 模型名称
     */
    private String model;

    /**
     * 超时时间(毫秒)
     */
    private long timeoutMs;
}
