package com.ttikss.nexatalk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ttikss.nexatalk.common.ErrorCode;
import com.ttikss.nexatalk.dto.SendMessageRequest;
import com.ttikss.nexatalk.entity.Conversation;
import com.ttikss.nexatalk.entity.Message;
import com.ttikss.nexatalk.entity.User;
import com.ttikss.nexatalk.exception.BusinessException;
import com.ttikss.nexatalk.mapper.BlacklistMapper;
import com.ttikss.nexatalk.mapper.ConversationMapper;
import com.ttikss.nexatalk.mapper.MessageMapper;
import com.ttikss.nexatalk.mapper.UserMapper;
import com.ttikss.nexatalk.service.AiService;
import com.ttikss.nexatalk.service.MessageService;
import com.ttikss.nexatalk.vo.ConversationVO;
import com.ttikss.nexatalk.vo.PageVO;
import com.ttikss.nexatalk.vo.UserVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 私信模块业务实现
 *
 * 关键设计：会话 ID 由 min(senderId, receiverId) / max(senderId, receiverId) 确定，
 * 保证每对用户只有唯一一条会话记录，避免重复创建。
 */
@Service
public class MessageServiceImpl implements MessageService {

    private static final Logger log = LoggerFactory.getLogger(MessageServiceImpl.class);

    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final UserMapper userMapper;
    private final BlacklistMapper blacklistMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final AiService aiService;

    public MessageServiceImpl(ConversationMapper conversationMapper,
                             MessageMapper messageMapper,
                             UserMapper userMapper,
                             BlacklistMapper blacklistMapper,
                             SimpMessagingTemplate messagingTemplate,
                             AiService aiService) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.userMapper = userMapper;
        this.blacklistMapper = blacklistMapper;
        this.messagingTemplate = messagingTemplate;
        this.aiService = aiService;
    }

    @Override
    @Transactional
    public Message sendMessage(Long senderId, SendMessageRequest request) {
        Long receiverId = request.getReceiverId();
        if (senderId.equals(receiverId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.code(), "不能给自己发消息");
        }

        // 检查发送方（当前用户）是否被禁言/封号
        User sender = userMapper.selectById(senderId);
        if (sender == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.code(), "用户不存在");
        }
        if (sender.getStatus() != null) {
            if (sender.getStatus() == User.STATUS_MUTED) {
                throw new BusinessException(ErrorCode.BAD_REQUEST.code(), "您已被禁言，无法发送消息");
            }
            if (sender.getStatus() == User.STATUS_BANNED) {
                throw new BusinessException(ErrorCode.BAD_REQUEST.code(), "您已被封号，无法发送消息");
            }
        }

        // 检查接收方用户是否存在
        User receiver = userMapper.selectById(receiverId);
        if (receiver == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.code(), "用户不存在");
        }

        // 检查接收方用户是否被封号
        if (receiver.getStatus() != null && receiver.getStatus() == User.STATUS_BANNED) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.code(), "该用户已被封号");
        }

        // 【重要】检查当前用户是否在接收方的黑名单中
        if (blacklistMapper.isBlocked(receiverId, senderId) > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.code(), "您已被对方拉黑，无法发送消息");
        }

        // 保证 user1_id < user2_id
        Long user1Id = Math.min(senderId, receiverId);
        Long user2Id = Math.max(senderId, receiverId);

        // 查找或创建会话
        Conversation conversation = conversationMapper.selectOne(
                new LambdaQueryWrapper<Conversation>()
                        .eq(Conversation::getUser1Id, user1Id)
                        .eq(Conversation::getUser2Id, user2Id)
        );
        if (conversation == null) {
            conversation = new Conversation();
            conversation.setUser1Id(user1Id);
            conversation.setUser2Id(user2Id);
            conversation.setUser1Unread(0);
            conversation.setUser2Unread(0);
            conversationMapper.insert(conversation);
        }

        // 写入消息
        Message message = new Message();
        message.setConversationId(conversation.getId());
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setContent(request.getContent());
        message.setIsRead(0);
        // 支持消息类型扩展（文本=0，图片=1，语音=2）
        message.setType(request.getType() != null ? request.getType() : 0);
        messageMapper.insert(message);

        // 更新会话：last_message + 接收方未读数 +1
        String preview = request.getContent() != null && request.getContent().length() > 50
                ? request.getContent().substring(0, 50) + "..."
                : request.getContent();
        conversation.setLastMessage(preview);
        conversation.setLastMessageAt(LocalDateTime.now());
        if (receiverId.equals(user1Id)) {
            // 接收方是 user1
            conversation.setUser1Unread(conversation.getUser1Unread() + 1);
        } else {
            // 接收方是 user2
            conversation.setUser2Unread(conversation.getUser2Unread() + 1);
        }
        conversationMapper.updateById(conversation);

        // 通过 WebSocket 推送实时消息给接收方
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "new_message");
            notification.put("messageId", message.getId());
            notification.put("conversationId", conversation.getId());
            notification.put("senderId", senderId);
            notification.put("senderNickname", sender != null ? sender.getNickname() : "未知用户");
            notification.put("senderAvatar", sender != null ? sender.getAvatarUrl() : "");
            notification.put("receiverId", receiverId);
            notification.put("content", request.getContent());
            notification.put("messageType", message.getType());
            notification.put("createdAt", LocalDateTime.now().toString());
            
            log.info("【WebSocket推送】准备发送给 receiverId={}, conversationId={}", receiverId, conversation.getId());
            messagingTemplate.convertAndSendToUser(
                    receiverId.toString(),
                    "/queue/messages",
                    notification
            );
            log.info("【WebSocket推送】已发送消息给 user: {}, conversation: {}", receiverId, conversation.getId());
        } catch (Exception e) {
            log.warn("WebSocket push failed for user {}: {}", receiverId, e.getMessage(), e);
        }

        // 如果是发送给 AI 的消息，触发 AI 自动回复
        Long aiUserId = getAiUserId();
        if (aiUserId != null && receiverId.equals(aiUserId)) {
            // 通知用户 AI 正在输入
            try {
                Map<String, Object> typingNotification = new HashMap<>();
                typingNotification.put("type", "typing");
                typingNotification.put("conversationId", conversation.getId());
                typingNotification.put("senderId", aiUserId);
                typingNotification.put("senderNickname", "Talk");
                typingNotification.put("isTyping", true);
                messagingTemplate.convertAndSendToUser(
                        senderId.toString(),
                        "/queue/messages",
                        typingNotification
                );
            } catch (Exception e) {
                log.warn("Typing notification failed: {}", e.getMessage());
            }

            // 异步调用 AI 回复（不阻塞主线程）
            // 注意：conversation 必须是 effectively final
            final Conversation aiConversation = conversation;
            final Long senderIdFinal = senderId;
            new Thread(() -> {
                try {
                    Thread.sleep(1500); // 延迟回复，模拟人工思考
                    handleAiReply(senderIdFinal, aiConversation, request.getContent());
                } catch (Exception e) {
                    log.error("AI reply failed: {}", e.getMessage());
                }
            }).start();
        }

        return message;
    }

    /**
     * 处理 AI 自动回复
     */
    private void handleAiReply(Long userId, Conversation conversation, String userMessage) {
        try {
            // 调用 AI 服务获取回复
            String aiReply = aiService.chat(userMessage);
            if (aiReply == null || aiReply.isEmpty()) {
                aiReply = "抱歉，我暂时无法回答这个问题。";
            }

            // 保存 AI 回复消息
            Long aiUserId = getAiUserId();
            if (aiUserId == null) return;

            Message aiMessage = new Message();
            aiMessage.setConversationId(conversation.getId());
            aiMessage.setSenderId(aiUserId);
            aiMessage.setReceiverId(userId);
            aiMessage.setContent(aiReply);
            aiMessage.setIsRead(0);
            aiMessage.setType(Message.TYPE_TEXT);
            messageMapper.insert(aiMessage);

            // 更新会话
            String preview = aiReply.length() > 50 ? aiReply.substring(0, 50) + "..." : aiReply;
            conversation.setLastMessage(preview);
            conversation.setLastMessageAt(LocalDateTime.now());
            // 用户的未读数 +1
            if (userId.equals(conversation.getUser1Id())) {
                conversation.setUser1Unread(conversation.getUser1Unread() + 1);
            } else {
                conversation.setUser2Unread(conversation.getUser2Unread() + 1);
            }
            conversationMapper.updateById(conversation);

            // 通过 WebSocket 推送 AI 回复给用户
            User aiUser = userMapper.selectById(aiUserId);
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "new_message");
            notification.put("messageId", aiMessage.getId());
            notification.put("conversationId", conversation.getId());
            notification.put("senderId", aiUserId);
            notification.put("senderNickname", aiUser != null ? aiUser.getNickname() : "Talk");
            notification.put("senderAvatar", aiUser != null ? aiUser.getAvatarUrl() : "");
            notification.put("receiverId", userId);
            notification.put("content", aiReply);
            notification.put("messageType", aiMessage.getType());
            notification.put("createdAt", LocalDateTime.now().toString());
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/messages",
                    notification
            );
            log.info("AI reply sent to user: {}, conversation: {}", userId, conversation.getId());
        } catch (Exception e) {
            log.error("Failed to send AI reply: {}", e.getMessage());
        }
    }

    @Override
    public PageVO<ConversationVO> listMyConversations(Long userId, int page, int pageSize) {
        pageSize = Math.min(pageSize, 50);

        // 查询 AI 用户的 ID（用于查找或创建 AI 会话）
        Long aiUserId = getAiUserId();

        // 查找或懒创建 AI 会话，保证「AI好友Talk」始终出现在会话列表中
        Conversation aiConversation = null;
        if (aiUserId != null) {
            Long user1Id = Math.min(userId, aiUserId);
            Long user2Id = Math.max(userId, aiUserId);
            aiConversation = conversationMapper.selectOne(
                    new LambdaQueryWrapper<Conversation>()
                            .eq(Conversation::getUser1Id, user1Id)
                            .eq(Conversation::getUser2Id, user2Id)
            );
            // 若用户从未与 AI 聊过，则没有会话记录，此处懒创建一条空会话以便列表展示入口
            if (aiConversation == null) {
                Conversation newConv = new Conversation();
                newConv.setUser1Id(user1Id);
                newConv.setUser2Id(user2Id);
                newConv.setUser1Unread(0);
                newConv.setUser2Unread(0);
                try {
                    conversationMapper.insert(newConv);
                    aiConversation = newConv;
                } catch (Exception e) {
                    // 并发下可能已存在，重新查询
                    aiConversation = conversationMapper.selectOne(
                            new LambdaQueryWrapper<Conversation>()
                                    .eq(Conversation::getUser1Id, user1Id)
                                    .eq(Conversation::getUser2Id, user2Id)
                    );
                }
            }
        }

        // 查询普通会话（按最后消息时间倒序；仅包含有消息的会话，避免空会话占满列表）
        Page<Conversation> result = conversationMapper.selectPage(
                new Page<>(page, pageSize),
                new LambdaQueryWrapper<Conversation>()
                        .and(w -> w.eq(Conversation::getUser1Id, userId)
                                .or().eq(Conversation::getUser2Id, userId))
                        .isNotNull(Conversation::getLastMessageAt)
                        .orderByDesc(Conversation::getLastMessageAt)
        );

        // 转换为 VO 并填充对方用户信息；排除与 AI 的会话，由下面统一置顶展示避免重复
        List<ConversationVO> voList = result.getRecords().stream()
                .filter(conv -> {
                    Long oppId = userId.equals(conv.getUser1Id()) ? conv.getUser2Id() : conv.getUser1Id();
                    return !Long.valueOf(oppId).equals(aiUserId);
                })
                .map(conv -> {
                    Long opponentId = userId.equals(conv.getUser1Id()) ? conv.getUser2Id() : conv.getUser1Id();
                    User opponent = userMapper.selectById(opponentId);
                    return ConversationVO.from(conv, userId, UserVO.from(opponent));
                })
                .collect(Collectors.toList());

        // 始终将 AI 会话置顶展示（无论是否有消息），便于用户找到「AI好友Talk」
        if (aiConversation != null && aiUserId != null) {
            User aiUser = userMapper.selectById(aiUserId);
            ConversationVO aiVO = ConversationVO.from(aiConversation, userId, UserVO.from(aiUser));
            if (aiVO != null) {
                aiVO.setIsPinned(true);
                aiVO.setIsAiConversation(true);
                aiVO.setOpponentNickname("AI好友Talk"); // 统一展示名称
                voList.add(0, aiVO);
            }
        }

        return PageVO.of(result, voList);
    }

    /**
     * 获取 AI 用户的 ID
     */
    private Long getAiUserId() {
        // 查找 role=3 的 AI 用户
        User aiUser = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getRole, User.ROLE_AI)
                        .last("LIMIT 1")
        );
        return aiUser != null ? aiUser.getId() : null;
    }

    @Override
    public PageVO<Message> listMessages(Long userId, Long conversationId, int page, int pageSize) {
        pageSize = Math.min(pageSize, 50);
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.code(), "会话不存在");
        }
        // 鉴权：只有会话中的用户才能查看消息
        if (!userId.equals(conversation.getUser1Id()) && !userId.equals(conversation.getUser2Id())) {
            throw new BusinessException(ErrorCode.FORBIDDEN.code(), "无权查看此会话");
        }

        Page<Message> result = messageMapper.selectPage(
                new Page<>(page, pageSize),
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getConversationId, conversationId)
                        .orderByAsc(Message::getCreatedAt)
        );
        return PageVO.of(result, result.getRecords());
    }

    @Override
    @Transactional
    public void readConversation(Long userId, Long conversationId) {
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) return;
        if (!userId.equals(conversation.getUser1Id()) && !userId.equals(conversation.getUser2Id())) {
            throw new BusinessException(ErrorCode.FORBIDDEN.code(), "无权操作此会话");
        }

        // 标记消息已读
        messageMapper.markConversationRead(conversationId, userId);

        // 清零会话维度未读计数
        if (userId.equals(conversation.getUser1Id())) {
            conversationMapper.clearUser1Unread(conversationId);
        } else {
            conversationMapper.clearUser2Unread(conversationId);
        }
    }

    @Override
    public long countTotalUnread(Long userId) {
        // 使用数据库聚合函数直接在SQL层计算，更高效
        return conversationMapper.countTotalUnread(userId);
    }

    @Override
    @Transactional
    public void deleteConversation(Long userId, Long conversationId) {
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.code(), "会话不存在");
        }
        // 鉴权：只有会话中的用户才能删除
        if (!userId.equals(conversation.getUser1Id()) && !userId.equals(conversation.getUser2Id())) {
            throw new BusinessException(ErrorCode.FORBIDDEN.code(), "无权操作此会话");
        }

        // 删除会话的所有消息
        messageMapper.delete(new LambdaQueryWrapper<Message>()
                .eq(Message::getConversationId, conversationId));

        // 删除会话
        conversationMapper.deleteById(conversationId);
    }

    @Override
    @Transactional
    public void clearMessages(Long userId, Long conversationId) {
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.code(), "会话不存在");
        }
        // 鉴权：只有会话中的用户才能清空
        if (!userId.equals(conversation.getUser1Id()) && !userId.equals(conversation.getUser2Id())) {
            throw new BusinessException(ErrorCode.FORBIDDEN.code(), "无权操作此会话");
        }

        // 删除会话的所有消息
        messageMapper.delete(new LambdaQueryWrapper<Message>()
                .eq(Message::getConversationId, conversationId));

        // 重置会话的最后消息
        conversation.setLastMessage(null);
        conversation.setLastMessageAt(null);
        conversation.setUser1Unread(0);
        conversation.setUser2Unread(0);
        conversationMapper.updateById(conversation);
    }

    @Override
    @Transactional
    public void recallMessage(Long userId, Long messageId) {
        Message message = messageMapper.selectById(messageId);
        if (message == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.code(), "消息不存在");
        }

        // 鉴权：只能撤回自己发送的消息
        if (!message.getSenderId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN.code(), "只能撤回自己发送的消息");
        }

        // 检查消息是否在可撤回时间范围内（5分钟内）
        if (message.getCreatedAt() != null) {
            LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
            if (message.getCreatedAt().isBefore(fiveMinutesAgo)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST.code(), "消息已超过5分钟，无法撤回");
            }
        }

        // 将消息内容替换为撤回提示
        message.setContent("【此消息已被撤回】");
        message.setIsRecalled(1); // 标记为已撤回
        messageMapper.updateById(message);

        // 更新会话的最后消息
        Conversation conversation = conversationMapper.selectById(message.getConversationId());
        if (conversation != null && conversation.getLastMessage() != null
                && conversation.getLastMessage().contains(message.getContent())) {
            // 查找倒数第二条消息作为新的 last_message
            Message lastMsg = messageMapper.selectOne(
                    new LambdaQueryWrapper<Message>()
                            .eq(Message::getConversationId, message.getConversationId())
                            .ne(Message::getId, messageId)
                            .orderByDesc(Message::getCreatedAt)
                            .last("LIMIT 1")
            );
            if (lastMsg != null) {
                conversation.setLastMessage(lastMsg.getContent());
                conversation.setLastMessageAt(lastMsg.getCreatedAt());
            } else {
                conversation.setLastMessage(null);
                conversation.setLastMessageAt(null);
            }
            conversationMapper.updateById(conversation);
        }

        // 通过 WebSocket 通知对方消息被撤回
        try {
            Long receiverId = message.getReceiverId();
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "message_recalled");
            notification.put("messageId", messageId);
            notification.put("conversationId", message.getConversationId());
            messagingTemplate.convertAndSendToUser(
                    receiverId.toString(),
                    "/queue/messages",
                    notification
            );
            log.info("Message recall notification sent: messageId={}, conversationId={}", messageId, message.getConversationId());
        } catch (Exception e) {
            log.warn("WebSocket recall notification failed: {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    public void deleteMessage(Long userId, Long messageId) {
        Message message = messageMapper.selectById(messageId);
        if (message == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.code(), "消息不存在");
        }

        // 鉴权：发送方或接收方都可以删除消息（删除仅本地可见）
        if (!message.getSenderId().equals(userId) && !message.getReceiverId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN.code(), "无权删除此消息");
        }

        // 执行删除
        messageMapper.deleteById(messageId);

        // 如果删除的是最后一条消息，更新会话的 last_message
        Conversation conversation = conversationMapper.selectById(message.getConversationId());
        if (conversation != null) {
            Message lastMsg = messageMapper.selectOne(
                    new LambdaQueryWrapper<Message>()
                            .eq(Message::getConversationId, message.getConversationId())
                            .ne(Message::getId, messageId)
                            .orderByDesc(Message::getCreatedAt)
                            .last("LIMIT 1")
            );
            if (lastMsg != null) {
                conversation.setLastMessage(lastMsg.getContent());
                conversation.setLastMessageAt(lastMsg.getCreatedAt());
            } else {
                conversation.setLastMessage(null);
                conversation.setLastMessageAt(null);
                conversation.setUser1Unread(0);
                conversation.setUser2Unread(0);
            }
            conversationMapper.updateById(conversation);
        }
    }
}
