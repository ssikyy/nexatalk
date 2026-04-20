package com.ttikss.nexatalk.controller;

import com.ttikss.nexatalk.common.Result;
import com.ttikss.nexatalk.dto.AiChatRequest;
import com.ttikss.nexatalk.dto.SendMessageRequest;
import com.ttikss.nexatalk.entity.Message;
import com.ttikss.nexatalk.security.CurrentUser;
import com.ttikss.nexatalk.service.AiService;
import com.ttikss.nexatalk.service.MessageService;
import com.ttikss.nexatalk.vo.ConversationVO;
import com.ttikss.nexatalk.vo.PageVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 私信模块控制器
 *
 * 接口清单：
 * POST /api/messages                         发送私信
 * GET  /api/messages/conversations           我的会话列表
 * GET  /api/messages/conversations/{id}      某会话的消息记录
 * POST /api/messages/conversations/{id}/read 标记会话为已读
 * GET  /api/messages/unread                  未读消息总数
 * DELETE /api/messages/conversations/{id}     删除会话
 * DELETE /api/messages/conversations/{id}/messages 清空消息记录
 * POST /api/messages/{id}/recall             撤回消息
 * DELETE /api/messages/{id}                  删除单条消息
 *
 * AI 对话接口：
 * POST /api/messages/ai/chat               与 AI 对话
 */
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;
    private final AiService aiService;

    public MessageController(MessageService messageService, AiService aiService) {
        this.messageService = messageService;
        this.aiService = aiService;
    }

    /** 发送私信 */
    @PostMapping
    public Result<Message> sendMessage(@CurrentUser Long userId,
                                        @Valid @RequestBody SendMessageRequest request) {
        return Result.ok(messageService.sendMessage(userId, request));
    }

    /** 查询我的会话列表 */
    @GetMapping("/conversations")
    public Result<PageVO<ConversationVO>> listConversations(
            @CurrentUser Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(messageService.listMyConversations(userId, page, pageSize));
    }

    /** 查询某会话的消息记录（第 1 页返回最新一页消息） */
    @GetMapping("/conversations/{conversationId}")
    public Result<PageVO<Message>> listMessages(
            @CurrentUser Long userId,
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(messageService.listMessages(userId, conversationId, page, pageSize));
    }

    /** 标记会话为已读（清零未读数 + 标记消息已读） */
    @PostMapping("/conversations/{conversationId}/read")
    public Result<String> readConversation(@CurrentUser Long userId,
                                            @PathVariable Long conversationId) {
        messageService.readConversation(userId, conversationId);
        return Result.ok("已标记为已读");
    }

    /** 查询未读消息总数 */
    @GetMapping("/unread")
    public Result<Long> countUnread(@CurrentUser Long userId) {
        return Result.ok(messageService.countTotalUnread(userId));
    }

    /** 本地删除指定会话，仅移除当前用户视角 */
    @DeleteMapping("/conversations/{conversationId}")
    public Result<String> deleteConversation(@CurrentUser Long userId,
                                             @PathVariable Long conversationId) {
        messageService.deleteConversation(userId, conversationId);
        return Result.ok("会话已删除");
    }

    /** 本地清空指定会话的消息记录（保留会话） */
    @DeleteMapping("/conversations/{conversationId}/messages")
    public Result<String> clearMessages(@CurrentUser Long userId,
                                        @PathVariable Long conversationId) {
        messageService.clearMessages(userId, conversationId);
        return Result.ok("消息记录已清空");
    }

    /** 撤回消息（发送后5分钟内可撤回） */
    @PostMapping("/{messageId}/recall")
    public Result<String> recallMessage(@CurrentUser Long userId,
                                        @PathVariable Long messageId) {
        messageService.recallMessage(userId, messageId);
        return Result.ok("消息已撤回");
    }

    /** 本地删除单条消息，仅对当前用户隐藏 */
    @DeleteMapping("/{messageId}")
    public Result<String> deleteMessage(@CurrentUser Long userId,
                                        @PathVariable Long messageId) {
        messageService.deleteMessage(userId, messageId);
        return Result.ok("消息已删除");
    }

    // ==================== AI 对话接口 ====================

    /** 与 AI 对话 */
    @PostMapping("/ai/chat")
    public Result<String> chatWithAi(@CurrentUser Long userId,
                                     @Valid @RequestBody AiChatRequest request) {
        String reply;
        if (request.getHistory() != null && !request.getHistory().isEmpty()) {
            // 带上下文对话
            reply = aiService.chatWithContext(request.getHistory());
        } else {
            // 单轮对话
            reply = aiService.chat(request.getMessage());
        }
        return Result.ok(reply);
    }
}
