package com.ttikss.nexatalk.service;

/**
 * AI 助手模块业务层接口
 *
 * 降级策略：
 * - ai.enabled=false 时直接返回原始内容，不调用外部 API
 * - API 调用异常时 catch 并降级返回原始内容，记录 WARN 日志
 */
public interface AiService {

    /**
     * 对帖子正文生成 AI 摘要
     *
     * @param content 原始正文（富文本 HTML 或旧 Markdown）
     * @return 摘要文本；降级时返回原文前 200 字
     */
    String summarize(String content);

    /**
     * 对文本进行 AI 润色（修正语言表达、优化措辞）
     *
     * @param content 待润色文本
     * @return 润色后文本；降级时返回原始内容
     */
    String polish(String content);

    /**
     * 对文本进行 AI 润色（支持风格选择）
     *
     * @param content 待润色文本
     * @param style   润色风格：standard(标准), formal(正式商务), concise(简洁精炼), friendly(活泼亲切), professional(专业深度)
     * @return 润色后文本（已去除 AI 可能返回的 &lt;p&gt; 等标签）；降级时返回原始内容
     */
    String polish(String content, String style);

    /**
     * AI 扩写：在保持原意的基础上扩展内容，支持风格选择
     *
     * @param content 原文
     * @param style   扩写风格：detailed(详细展开), vivid(生动描写), logical(逻辑递进), narrative(叙述扩充), professional(专业延伸)
     * @return 扩写后文本；降级时返回原始内容
     */
    String expand(String content, String style);

    /**
     * AI 对话（单轮）
     *
     * @param message 用户消息
     * @return AI 回复
     */
    String chat(String message);

    /**
     * AI 对话（带上下文）
     *
     * @param messages 消息历史列表，每项格式为 "role:content"，role 为 "user" 或 "assistant"
     * @return AI 回复
     */
    String chatWithContext(java.util.List<String> messages);
}
