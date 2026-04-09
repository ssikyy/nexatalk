package com.ttikss.nexatalk.controller;

import com.ttikss.nexatalk.entity.User;
import com.ttikss.nexatalk.mapper.ConversationMapper;
import com.ttikss.nexatalk.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.HashMap;
import java.util.Map;
import java.security.Principal;

/**
 * WebSocket 消息处理控制器
 * 处理实时消息推送、正在输入等 WebSocket 事件
 */
@Controller
@CrossOrigin(origins = "*")
public class WebSocketController {

    private static final Logger log = LoggerFactory.getLogger(WebSocketController.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final UserMapper userMapper;
    private final ConversationMapper conversationMapper;

    public WebSocketController(SimpMessagingTemplate messagingTemplate,
                               UserMapper userMapper,
                               ConversationMapper conversationMapper) {
        this.messagingTemplate = messagingTemplate;
        this.userMapper = userMapper;
        this.conversationMapper = conversationMapper;
    }

    /**
     * 处理「正在输入」事件
     * 前端通过 STOMP 发送 /app/typing，服务器推送给对方
     */
    @MessageMapping("/typing")
    public void handleTyping(@Payload Map<String, Object> payload, Principal principal) {
        try {
            if (principal == null) {
                log.warn("Rejected typing event without authenticated principal");
                return;
            }

            Long senderId = Long.valueOf(principal.getName());
            Long receiverId = Long.valueOf(payload.get("receiverId").toString());
            Long conversationId = Long.valueOf(payload.get("conversationId").toString());
            Boolean isTyping = Boolean.TRUE.equals(payload.get("isTyping"));

            var conversation = conversationMapper.selectById(conversationId);
            if (conversation == null) {
                log.warn("Rejected typing event for missing conversation {}", conversationId);
                return;
            }

            boolean senderInConversation = senderId.equals(conversation.getUser1Id())
                    || senderId.equals(conversation.getUser2Id());
            if (!senderInConversation) {
                log.warn("Rejected typing event: user {} is not in conversation {}", senderId, conversationId);
                return;
            }

            Long expectedReceiverId = senderId.equals(conversation.getUser1Id())
                    ? conversation.getUser2Id()
                    : conversation.getUser1Id();
            if (!expectedReceiverId.equals(receiverId)) {
                log.warn("Rejected typing event: receiver {} does not match conversation {}", receiverId, conversationId);
                return;
            }

            log.debug("Typing event: sender={}, receiver={}, conversation={}, isTyping={}",
                    senderId, receiverId, conversationId, isTyping);

            // 获取发送方信息
            User sender = userMapper.selectById(senderId);

            // 构建通知
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "typing");
            notification.put("conversationId", conversationId);
            notification.put("senderId", senderId);
            notification.put("senderNickname", sender != null ? sender.getNickname() : "未知用户");
            notification.put("senderAvatar", sender != null ? sender.getAvatarUrl() : "");
            notification.put("isTyping", isTyping);

            // 推送给接收方
            messagingTemplate.convertAndSendToUser(
                    receiverId.toString(),
                    "/queue/messages",
                    notification
            );

            log.debug("Typing notification sent to user: {}", receiverId);
        } catch (Exception e) {
            log.warn("Failed to handle typing event: {}", e.getMessage());
        }
    }
}
