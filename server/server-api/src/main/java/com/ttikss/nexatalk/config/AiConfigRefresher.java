package com.ttikss.nexatalk.config;

import com.ttikss.nexatalk.service.SystemConfigService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * AI 配置刷新器
 * 从数据库加载 AI 配置到 AiProperties
 * 优先级：数据库配置 > application.yml 环境变量配置
 */
@Component
public class AiConfigRefresher {

    private static final Logger log = LoggerFactory.getLogger(AiConfigRefresher.class);

    private final AiProperties aiProperties;
    private final SystemConfigService systemConfigService;

    public AiConfigRefresher(AiProperties aiProperties, SystemConfigService systemConfigService) {
        this.aiProperties = aiProperties;
        this.systemConfigService = systemConfigService;
    }

    @PostConstruct
    public void init() {
        refreshConfig();
    }

    /**
     * 从数据库刷新 AI 配置
     */
    public void refreshConfig() {
        try {
            // 从数据库读取配置（数据库配置优先）
            String enabled = systemConfigService.getStringValue("ai_enabled");
            String baseUrl = systemConfigService.getStringValue("ai_base_url");
            String apiKey = systemConfigService.getStringValue("ai_api_key");
            String model = systemConfigService.getStringValue("ai_model");
            long timeoutMs = systemConfigService.getLongValue("ai_timeout_ms", 30000);

            // 如果数据库中有配置，则使用数据库配置
            if (enabled != null) {
                aiProperties.setEnabled("true".equalsIgnoreCase(enabled) || "1".equals(enabled));
            }
            if (baseUrl != null && !baseUrl.isEmpty()) {
                aiProperties.setBaseUrl(baseUrl);
            }
            if (apiKey != null) {
                aiProperties.setApiKey(apiKey);
            }
            if (model != null && !model.isEmpty()) {
                aiProperties.setModel(model);
            }
            aiProperties.setTimeoutMs(timeoutMs);

            log.info("AI 配置已从数据库加载: enabled={}, baseUrl={}, model={}, timeoutMs={}",
                    aiProperties.isEnabled(), aiProperties.getBaseUrl(), aiProperties.getModel(), aiProperties.getTimeoutMs());
        } catch (Exception e) {
            log.warn("从数据库加载 AI 配置失败，使用 application.yml 配置: {}", e.getMessage());
        }
    }
}
