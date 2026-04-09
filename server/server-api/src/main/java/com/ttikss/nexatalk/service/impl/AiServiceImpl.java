package com.ttikss.nexatalk.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ttikss.nexatalk.config.AiProperties;
import com.ttikss.nexatalk.service.AiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 助手模块业务实现
 *
 * 对接标准：OpenAI Chat Completions API（/v1/chat/completions）
 * 兼容厂商：通义千问（baseUrl=https://dashscope.aliyuncs.com/compatible-mode）
 *           讯飞星火（需确认 API 规范）、Moonshot 等兼容 OpenAI 协议的接口均可直接替换 baseUrl
 *
 * 使用原生 java.net.http.HttpClient（JDK 11+），无需额外依赖
 */
@Service
public class AiServiceImpl implements AiService {

    private static final Logger log = LoggerFactory.getLogger(AiServiceImpl.class);

    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;

    public AiServiceImpl(AiProperties aiProperties, ObjectMapper objectMapper) {
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public String summarize(String content) {
        if (!aiProperties.isEnabled()) {
            // 降级：截取前 200 字作为摘要
            return content.length() > 200 ? content.substring(0, 200) + "..." : content;
        }
        String prompt = "请将以下内容压缩为 100 字以内的简洁摘要，直接输出摘要文本，不要额外说明：\n\n" + content;
        try {
            return callChatApi(prompt);
        } catch (Exception e) {
            log.warn("AI 摘要调用失败，降级返回原文截取. error={}", e.getMessage());
            return content.length() > 200 ? content.substring(0, 200) + "..." : content;
        }
    }

    @Override
    public String polish(String content) {
        return polish(content, "standard");
    }

    @Override
    public String polish(String content, String style) {
        if (!aiProperties.isEnabled()) {
            return content;
        }
        String prompt = buildPolishPrompt(content, style);
        try {
            String raw = callChatApi(prompt);
            return stripHtmlWrapping(raw);
        } catch (Exception e) {
            log.warn("AI 润色（风格：{}）调用失败，降级返回原始内容. error={}", style, e.getMessage());
            return content;
        }
    }

    @Override
    public String expand(String content, String style) {
        if (!aiProperties.isEnabled()) {
            return content;
        }
        String prompt = buildExpandPrompt(content, style);
        try {
            String raw = callChatApi(prompt);
            return stripHtmlWrapping(raw);
        } catch (Exception e) {
            log.warn("AI 扩写（风格：{}）调用失败，降级返回原始内容. error={}", style, e.getMessage());
            return content;
        }
    }

    /** 去除 AI 可能返回的 <p>、</p> 等包裹标签，只保留纯文本/HTML 正文 */
    private static String stripHtmlWrapping(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        if (s.startsWith("<p>") && s.endsWith("</p>")) {
            s = s.substring(3, s.length() - 4).trim();
        }
        s = s.replace("</p><p>", "\n").replace("<p>", "").replace("</p>", "");
        return s;
    }

    private String buildExpandPrompt(String content, String style) {
        return switch (style) {
            case "vivid" -> "请对以下中文文本进行扩写，要求：增加生动描写、细节与画面感，保持原意，直接输出扩写后的文本，不要输出任何HTML标签或<p>标签：\n\n" + content;
            case "logical" -> "请对以下中文文本进行扩写，要求：按逻辑递进补充论据或步骤，结构清晰，保持原意，直接输出扩写后的文本，不要输出任何HTML标签或<p>标签：\n\n" + content;
            case "narrative" -> "请对以下中文文本进行扩写，要求：以叙述方式扩充情节或背景，保持原意，直接输出扩写后的文本，不要输出任何HTML标签或<p>标签：\n\n" + content;
            case "professional" -> "请对以下中文文本进行扩写，要求：从专业角度延伸解释或举例，保持原意，直接输出扩写后的文本，不要输出任何HTML标签或<p>标签：\n\n" + content;
            default -> "请对以下中文文本进行扩写，要求：在保持原意的基础上适当展开、补充细节，使内容更丰富，直接输出扩写后的文本，不要输出任何HTML标签或<p>标签：\n\n" + content;
        };
    }

    /**
     * 根据润色风格构建不同的提示词
     */
    private String buildPolishPrompt(String content, String style) {
        String suffix = "直接输出润色后的文本，不要输出任何HTML标签或<p>标签：\n\n" + content;
        return switch (style) {
            case "formal" -> "请对以下中文文本进行润色，转换为正式商务风格：语气正式、用词规范、表达专业，保持原意不变，" + suffix;
            case "concise" -> "请对以下中文文本进行润色，要求：精简内容、去除冗余表达、保留核心信息，保持原意不变，" + suffix;
            case "friendly" -> "请对以下中文文本进行润色，转换为活泼亲切的风格：语气友好、亲切自然、略带轻松，保持原意不变，" + suffix;
            case "professional" -> "请对以下中文文本进行润色，转换为专业深度风格：增加专业术语、详细阐述、逻辑严谨，保持原意不变，" + suffix;
            default -> "请对以下中文文本进行润色，要求：修正语法错误、优化措辞使其更流畅，保持原意不变，" + suffix;
        };
    }

    @Override
    public String chat(String message) {
        if (!aiProperties.isEnabled()) {
            return "AI 功能已关闭，请联系管理员开启";
        }
        String prompt = "你是一个友好的AI助手，请用简洁、有帮助的方式回答用户的问题。\n\n用户：" + message;
        try {
            return callChatApi(prompt);
        } catch (Exception e) {
            log.warn("AI 对话调用失败. error={}", e.getMessage());
            return "抱歉，我现在暂时无法回答您的问题，请稍后再试。";
        }
    }

    @Override
    public String chatWithContext(List<String> messages) {
        if (!aiProperties.isEnabled()) {
            return "AI 功能已关闭，请联系管理员开启";
        }
        try {
            // 构建消息列表
            List<Map<String, String>> msgList = new ArrayList<>();

            // 添加系统提示
            msgList.add(Map.of("role", "system", "content", "你是一个友好的AI助手，请用简洁、有帮助的方式回答用户的问题。"));

            // 添加历史消息
            for (String msg : messages) {
                if (msg.startsWith("user:") || msg.startsWith("user：")) {
                    msgList.add(Map.of("role", "user", "content", msg.substring(4).trim()));
                } else if (msg.startsWith("assistant:") || msg.startsWith("assistant：")) {
                    msgList.add(Map.of("role", "assistant", "content", msg.substring(9).trim()));
                }
            }

            return callChatApiWithMessages(msgList);
        } catch (Exception e) {
            log.warn("AI 对话（上下文）调用失败. error={}", e.getMessage());
            return "抱歉，我现在暂时无法回答您的问题，请稍后再试。";
        }
    }

    /**
     * 调用 Chat Completions API 并返回模型回复内容
     */
    private String callChatApi(String userMessage) throws Exception {
        // 构建请求体（手动组装 Map 再序列化为 JSON，避免引入额外依赖）
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", aiProperties.getModel());
        body.put("messages", List.of(
                Map.of("role", "user", "content", userMessage)
        ));
        body.put("max_tokens", 1024);
        body.put("temperature", 0.7);
        String requestBody = objectMapper.writeValueAsString(body);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(aiProperties.getTimeoutMs()))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(resolveChatCompletionsUrl()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + aiProperties.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofMillis(aiProperties.getTimeoutMs()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("AI API 返回非 200 状态：" + response.statusCode() + " body=" + response.body());
        }

        // 解析响应，提取 choices[0].message.content
        JsonNode root = objectMapper.readTree(response.body());
        JsonNode content = root.path("choices").get(0).path("message").path("content");
        return content.asText();
    }

    /**
     * 调用 Chat Completions API（带消息列表）并返回模型回复内容
     */
    private String callChatApiWithMessages(List<Map<String, String>> messages) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", aiProperties.getModel());
        body.put("messages", messages);
        body.put("max_tokens", 1024);
        body.put("temperature", 0.7);
        String requestBody = objectMapper.writeValueAsString(body);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(aiProperties.getTimeoutMs()))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(resolveChatCompletionsUrl()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + aiProperties.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofMillis(aiProperties.getTimeoutMs()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("AI API 返回非 200 状态：" + response.statusCode() + " body=" + response.body());
        }

        // 解析响应，提取 choices[0].message.content
        JsonNode root = objectMapper.readTree(response.body());
        JsonNode content = root.path("choices").get(0).path("message").path("content");
        return content.asText();
    }

    /**
     * 计算 Chat Completions 接口完整 URL，兼容两种配置方式：
     * 1) baseUrl = https://api.openai.com           -> 追加 /v1/chat/completions
     * 2) baseUrl = https://coding.dashscope.aliyuncs.com/v1 -> 追加 /chat/completions
     */
    private String resolveChatCompletionsUrl() {
        String baseUrl = aiProperties.getBaseUrl();
        if (baseUrl == null || baseUrl.isEmpty()) {
            throw new IllegalStateException("AI Base URL 未配置");
        }
        // 统一去掉末尾多余的斜杠，方便拼接
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        if (baseUrl.endsWith("/v1")) {
            // 已经带有 /v1（如 coding.dashscope.aliyuncs.com/v1），只追加 /chat/completions
            return baseUrl + "/chat/completions";
        }
        // 默认情况：追加 /v1/chat/completions（如 api.openai.com、dashscope.aliyuncs.com/compatible-mode）
        return baseUrl + "/v1/chat/completions";
    }
}
