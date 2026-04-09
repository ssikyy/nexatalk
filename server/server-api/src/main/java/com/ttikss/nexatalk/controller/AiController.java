package com.ttikss.nexatalk.controller;

import com.ttikss.nexatalk.common.ErrorCode;
import com.ttikss.nexatalk.common.Result;
import com.ttikss.nexatalk.config.AiConfigRefresher;
import com.ttikss.nexatalk.config.AiProperties;
import com.ttikss.nexatalk.dto.AiConfigRequest;
import com.ttikss.nexatalk.security.CurrentUser;
import com.ttikss.nexatalk.security.RequireAdmin;
import com.ttikss.nexatalk.service.AiService;
import com.ttikss.nexatalk.service.SystemConfigService;
import com.ttikss.nexatalk.vo.AiConfigVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * AI 助手模块控制器
 *
 * 接口清单：
 * POST /api/ai/summarize  帖子摘要（传正文，返回 AI 生成摘要）
 * POST /api/ai/polish     文本润色（传草稿，返回润色后内容）
 *
 * 管理员接口：
 * GET  /api/admin/ai/config      获取 AI 配置
 * PUT  /api/admin/ai/config      更新 AI 配置
 * POST /api/admin/ai/config/refresh  刷新 AI 配置
 */
@RestController
@RequestMapping("/api")
@Validated
public class AiController {

    private final AiService aiService;
    private final AiProperties aiProperties;
    private final SystemConfigService systemConfigService;
    private final AiConfigRefresher aiConfigRefresher;

    private static final List<String> AI_CONFIG_KEYS = Arrays.asList(
            "ai_enabled", "ai_base_url", "ai_api_key", "ai_model", "ai_timeout_ms"
    );

    public AiController(AiService aiService, AiProperties aiProperties,
                       SystemConfigService systemConfigService, AiConfigRefresher aiConfigRefresher) {
        this.aiService = aiService;
        this.aiProperties = aiProperties;
        this.systemConfigService = systemConfigService;
        this.aiConfigRefresher = aiConfigRefresher;
    }

    /** 生成帖子摘要 */
    @PostMapping("/ai/summarize")
    public Result<String> summarize(
            @CurrentUser Long userId,
            @Validated @RequestBody TextRequest request) {
        return Result.ok(aiService.summarize(request.getContent()));
    }

    /** 文本润色 */
    @PostMapping("/ai/polish")
    public Result<String> polish(
            @CurrentUser Long userId,
            @Validated @RequestBody PolishRequest request) {
        return Result.ok(aiService.polish(request.getContent(), request.getStyle()));
    }

    /** AI 扩写（支持风格选择） */
    @PostMapping("/ai/expand")
    public Result<String> expand(
            @CurrentUser Long userId,
            @Validated @RequestBody ExpandRequest request) {
        return Result.ok(aiService.expand(request.getContent(), request.getStyle()));
    }

    /** 润色请求体（支持风格选择） */
    public static class PolishRequest extends TextRequest {
        private String style = "standard";

        public String getStyle() { return style; }
        public void setStyle(String style) { this.style = style; }
    }

    /** 扩写请求体（支持风格选择） */
    public static class ExpandRequest extends TextRequest {
        private String style = "detailed";

        public String getStyle() { return style; }
        public void setStyle(String style) { this.style = style; }
    }

    /** 查询 AI 功能是否开启（无需登录） */
    @GetMapping("/ai/status")
    public Result<Boolean> status() {
        return Result.ok(aiProperties.isEnabled());
    }

    // ==================== 管理员接口 ====================

    /** 获取 AI 配置（需管理员） */
    @GetMapping("/admin/ai/config")
    @RequireAdmin
    public Result<AiConfigVO> getAiConfig(@CurrentUser Long currentUserId) {
        AiConfigVO vo = new AiConfigVO();
        vo.setEnabled(aiProperties.isEnabled());
        vo.setBaseUrl(aiProperties.getBaseUrl());
        // API Key 脱敏显示
        String apiKey = aiProperties.getApiKey();
        if (apiKey != null && apiKey.length() > 8) {
            vo.setApiKey(apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4));
        } else {
            vo.setApiKey("");
        }
        vo.setModel(aiProperties.getModel());
        vo.setTimeoutMs(aiProperties.getTimeoutMs());
        return Result.ok(vo);
    }

    /** 更新 AI 配置（需管理员） */
    @PostMapping("/admin/ai/config")
    @RequireAdmin
    public Result<String> updateAiConfig(@CurrentUser Long currentUserId,
                                          @Valid @RequestBody AiConfigRequest request) {
        String key = request.getKey();
        if (!AI_CONFIG_KEYS.contains(key)) {
            return Result.fail(ErrorCode.BAD_REQUEST.code(), "无效的 AI 配置键");
        }
        systemConfigService.updateConfig(key, request.getValue(), request.getDescription());
        // 刷新配置
        aiConfigRefresher.refreshConfig();
        return Result.ok("AI 配置已更新");
    }

    /** 批量更新 AI 配置（需管理员） */
    @PostMapping("/admin/ai/configs")
    @RequireAdmin
    public Result<String> updateAiConfigs(@CurrentUser Long currentUserId,
                                          @Valid @RequestBody List<AiConfigRequest> requests) {
        for (AiConfigRequest request : requests) {
            String key = request.getKey();
            if (!AI_CONFIG_KEYS.contains(key)) {
                return Result.fail(ErrorCode.BAD_REQUEST.code(), "无效的 AI 配置键: " + key);
            }
            systemConfigService.updateConfig(key, request.getValue(), request.getDescription());
        }
        // 刷新配置
        aiConfigRefresher.refreshConfig();
        return Result.ok("AI 配置已更新");
    }

    /** 刷新 AI 配置（需管理员） */
    @PostMapping("/admin/ai/config/refresh")
    @RequireAdmin
    public Result<String> refreshAiConfig(@CurrentUser Long currentUserId) {
        aiConfigRefresher.refreshConfig();
        return Result.ok("AI 配置已刷新");
    }

    /** 内嵌请求体 DTO */
    public static class TextRequest {
        @NotBlank(message = "内容不能为空")
        @Size(max = 10000, message = "内容不超过 10000 字")
        private String content;

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }
}
