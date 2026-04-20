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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 私信模块业务实现
 *
 * 关键设计：
 * 1. 会话 ID 由 min(senderId, receiverId) / max(senderId, receiverId) 唯一确定；
 * 2. 会话删除/清空、消息删除均为“仅当前用户可见”的本地行为；
 * 3. 第 1 页消息永远返回最新一页，适合聊天窗口直接打开。
 */
@Service
public class MessageServiceImpl implements MessageService {

    private static final Logger log = LoggerFactory.getLogger(MessageServiceImpl.class);
    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_PREVIEW_LENGTH = 50;
    private static final String RECALLED_PLACEHOLDER = "【此消息已被撤回】";

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

        User receiver = userMapper.selectById(receiverId);
        if (receiver == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.code(), "用户不存在");
        }
        if (receiver.getStatus() != null && receiver.getStatus() == User.STATUS_BANNED) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.code(), "该用户已被封号");
        }
        if (blacklistMapper.isBlocked(receiverId, senderId) > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.code(), "您已被对方拉黑，无法发送消息");
        }

        Long user1Id = Math.min(senderId, receiverId);
        Long user2Id = Math.max(senderId, receiverId);

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

        Message message = new Message();
        message.setConversationId(conversation.getId());
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setContent(request.getContent());
        message.setIsRead(0);
        message.setType(request.getType() != null ? request.getType() : Message.TYPE_TEXT);
        messageMapper.insert(message);

        LocalDateTime sentAt = message.getCreatedAt() != null ? message.getCreatedAt() : LocalDateTime.now();
        touchConversationOnNewMessage(conversation, receiverId, buildPreview(request.getContent()), sentAt);
        conversationMapper.updateById(conversation);

        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "new_message");
            notification.put("messageId", message.getId());
            notification.put("conversationId", conversation.getId());
            notification.put("senderId", senderId);
            notification.put("senderNickname", sender.getNickname());
            notification.put("senderUsername", sender.getUsername());
            notification.put("senderAvatar", sender.getAvatarUrl());
            notification.put("receiverId", receiverId);
            notification.put("content", request.getContent());
            notification.put("messageType", message.getType());
            notification.put("createdAt", sentAt.toString());

            messagingTemplate.convertAndSendToUser(
                    receiverId.toString(),
                    "/queue/messages",
                    notification
            );
            log.info("【WebSocket推送】已发送消息给 user: {}, conversation: {}", receiverId, conversation.getId());
        } catch (Exception e) {
            log.warn("WebSocket push failed for user {}: {}", receiverId, e.getMessage(), e);
        }

        Long aiUserId = getAiUserId();
        if (aiUserId != null && receiverId.equals(aiUserId)) {
            try {
                Map<String, Object> typingNotification = new HashMap<>();
                typingNotification.put("type", "typing");
                typingNotification.put("conversationId", conversation.getId());
                typingNotification.put("senderId", aiUserId);
                typingNotification.put("senderNickname", "Talk");
                typingNotification.put("senderUsername", "talk");
                typingNotification.put("isTyping", true);
                messagingTemplate.convertAndSendToUser(
                        senderId.toString(),
                        "/queue/messages",
                        typingNotification
                );
            } catch (Exception e) {
                log.warn("Typing notification failed: {}", e.getMessage());
            }

            final Conversation aiConversation = conversation;
            final Long senderIdFinal = senderId;
            new Thread(() -> {
                try {
                    Thread.sleep(1500);
                    handleAiReply(senderIdFinal, aiConversation, request.getContent());
                } catch (Exception e) {
                    log.error("AI reply failed: {}", e.getMessage());
                }
            }).start();
        }

        return message;
    }

    private void handleAiReply(Long userId, Conversation conversation, String userMessage) {
        try {
            String aiReply = aiService.chat(userMessage);
            if (aiReply == null || aiReply.isEmpty()) {
                aiReply = "抱歉，我暂时无法回答这个问题。";
            }

            Long aiUserId = getAiUserId();
            if (aiUserId == null) {
                return;
            }

            Message aiMessage = new Message();
            aiMessage.setConversationId(conversation.getId());
            aiMessage.setSenderId(aiUserId);
            aiMessage.setReceiverId(userId);
            aiMessage.setContent(aiReply);
            aiMessage.setIsRead(0);
            aiMessage.setType(Message.TYPE_TEXT);
            messageMapper.insert(aiMessage);

            LocalDateTime sentAt = aiMessage.getCreatedAt() != null ? aiMessage.getCreatedAt() : LocalDateTime.now();
            touchConversationOnNewMessage(conversation, userId, buildPreview(aiReply), sentAt);
            conversationMapper.updateById(conversation);

            User aiUser = userMapper.selectById(aiUserId);
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "new_message");
            notification.put("messageId", aiMessage.getId());
            notification.put("conversationId", conversation.getId());
            notification.put("senderId", aiUserId);
            notification.put("senderNickname", aiUser != null ? aiUser.getNickname() : "Talk");
            notification.put("senderUsername", aiUser != null ? aiUser.getUsername() : "talk");
            notification.put("senderAvatar", aiUser != null ? aiUser.getAvatarUrl() : "");
            notification.put("receiverId", userId);
            notification.put("content", aiReply);
            notification.put("messageType", aiMessage.getType());
            notification.put("createdAt", sentAt.toString());
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
        page = Math.max(page, 1);
        pageSize = normalizePageSize(pageSize);

        Long aiUserId = getAiUserId();
        Conversation aiConversation = ensureAiConversation(userId, aiUserId);

        List<Conversation> allConversations = conversationMapper.selectList(
                new LambdaQueryWrapper<Conversation>()
                        .and(wrapper -> wrapper
                                .nested(inner -> inner
                                        .eq(Conversation::getUser1Id, userId)
                                        .isNull(Conversation::getUser1DeletedAt))
                                .or(inner -> inner
                                        .eq(Conversation::getUser2Id, userId)
                                        .isNull(Conversation::getUser2DeletedAt)))
        );

        List<ConversationVO> items = new ArrayList<>();
        for (Conversation conversation : allConversations) {
            if (aiConversation != null && Objects.equals(conversation.getId(), aiConversation.getId())) {
                continue;
            }
            items.add(buildConversationVO(conversation, userId));
        }

        items.sort(conversationComparator());

        if (aiConversation != null) {
            ConversationVO aiVO = buildConversationVO(aiConversation, userId);
            aiVO.setIsPinned(true);
            aiVO.setIsAiConversation(true);
            aiVO.setOpponentNickname("AI好友Talk");
            items.add(0, aiVO);
        }

        return paginate(items, page, pageSize);
    }

    @Override
    public PageVO<Message> listMessages(Long userId, Long conversationId, int page, int pageSize) {
        page = Math.max(page, 1);
        pageSize = normalizePageSize(pageSize);

        Conversation conversation = requireConversationParticipant(userId, conversationId);
        LocalDateTime clearedAt = getConversationClearedAt(conversation, userId);

        Page<Message> result = messageMapper.selectPage(
                new Page<>(page, pageSize),
                buildVisibleMessageQuery(conversationId, userId, clearedAt)
                        .orderByDesc(Message::getCreatedAt)
        );
        List<Message> records = new ArrayList<>(result.getRecords());
        Collections.reverse(records);
        return PageVO.of(result, records);
    }

    @Override
    @Transactional
    public void readConversation(Long userId, Long conversationId) {
        Conversation conversation = requireConversationParticipant(userId, conversationId);
        messageMapper.markConversationRead(conversationId, userId);
        clearConversationUnreadForUser(conversation, userId);
        conversationMapper.updateById(conversation);
    }

    @Override
    public long countTotalUnread(Long userId) {
        return conversationMapper.countTotalUnread(userId);
    }

    @Override
    @Transactional
    public void deleteConversation(Long userId, Long conversationId) {
        Conversation conversation = requireConversationParticipant(userId, conversationId);
        messageMapper.markConversationRead(conversationId, userId);
        clearConversationUnreadForUser(conversation, userId);

        LocalDateTime now = LocalDateTime.now();
        if (userId.equals(conversation.getUser1Id())) {
            conversation.setUser1DeletedAt(now);
        } else {
            conversation.setUser2DeletedAt(now);
        }
        conversationMapper.updateById(conversation);
    }

    @Override
    @Transactional
    public void clearMessages(Long userId, Long conversationId) {
        Conversation conversation = requireConversationParticipant(userId, conversationId);
        messageMapper.markConversationRead(conversationId, userId);
        clearConversationUnreadForUser(conversation, userId);

        LocalDateTime now = LocalDateTime.now();
        if (userId.equals(conversation.getUser1Id())) {
            conversation.setUser1ClearedAt(now);
        } else {
            conversation.setUser2ClearedAt(now);
        }
        conversationMapper.updateById(conversation);
    }

    @Override
    @Transactional
    public void recallMessage(Long userId, Long messageId) {
        Message message = messageMapper.selectById(messageId);
        if (message == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.code(), "消息不存在");
        }
        if (!message.getSenderId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN.code(), "只能撤回自己发送的消息");
        }
        if (message.getCreatedAt() != null) {
            LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
            if (message.getCreatedAt().isBefore(fiveMinutesAgo)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST.code(), "消息已超过5分钟，无法撤回");
            }
        }

        message.setContent(RECALLED_PLACEHOLDER);
        message.setIsRecalled(1);
        messageMapper.updateById(message);

        refreshConversationLastMessage(message.getConversationId());

        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "message_recalled");
            notification.put("messageId", messageId);
            notification.put("conversationId", message.getConversationId());
            messagingTemplate.convertAndSendToUser(
                    message.getReceiverId().toString(),
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
        if (!userId.equals(message.getSenderId()) && !userId.equals(message.getReceiverId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN.code(), "无权删除此消息");
        }

        LocalDateTime now = LocalDateTime.now();
        if (userId.equals(message.getSenderId())) {
            message.setSenderDeletedAt(now);
        } else {
            message.setReceiverDeletedAt(now);
        }
        messageMapper.updateById(message);
    }

    private Conversation ensureAiConversation(Long userId, Long aiUserId) {
        if (aiUserId == null) {
            return null;
        }

        Long user1Id = Math.min(userId, aiUserId);
        Long user2Id = Math.max(userId, aiUserId);
        Conversation aiConversation = conversationMapper.selectOne(
                new LambdaQueryWrapper<Conversation>()
                        .eq(Conversation::getUser1Id, user1Id)
                        .eq(Conversation::getUser2Id, user2Id)
        );
        if (aiConversation != null) {
            return aiConversation;
        }

        Conversation newConversation = new Conversation();
        newConversation.setUser1Id(user1Id);
        newConversation.setUser2Id(user2Id);
        newConversation.setUser1Unread(0);
        newConversation.setUser2Unread(0);
        try {
            conversationMapper.insert(newConversation);
            return newConversation;
        } catch (Exception e) {
            return conversationMapper.selectOne(
                    new LambdaQueryWrapper<Conversation>()
                            .eq(Conversation::getUser1Id, user1Id)
                            .eq(Conversation::getUser2Id, user2Id)
            );
        }
    }

    private Long getAiUserId() {
        User aiUser = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getRole, User.ROLE_AI)
                        .last("LIMIT 1")
        );
        return aiUser != null ? aiUser.getId() : null;
    }

    private Conversation requireConversationParticipant(Long userId, Long conversationId) {
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.code(), "会话不存在");
        }
        if (!userId.equals(conversation.getUser1Id()) && !userId.equals(conversation.getUser2Id())) {
            throw new BusinessException(ErrorCode.FORBIDDEN.code(), "无权操作此会话");
        }
        return conversation;
    }

    private void touchConversationOnNewMessage(Conversation conversation, Long receiverId, String preview, LocalDateTime sentAt) {
        conversation.setLastMessage(preview);
        conversation.setLastMessageAt(sentAt);
        conversation.setUser1DeletedAt(null);
        conversation.setUser2DeletedAt(null);
        conversation.setUser1Unread(defaultZero(conversation.getUser1Unread()));
        conversation.setUser2Unread(defaultZero(conversation.getUser2Unread()));
        if (receiverId.equals(conversation.getUser1Id())) {
            conversation.setUser1Unread(conversation.getUser1Unread() + 1);
        } else {
            conversation.setUser2Unread(conversation.getUser2Unread() + 1);
        }
    }

    private void clearConversationUnreadForUser(Conversation conversation, Long userId) {
        if (userId.equals(conversation.getUser1Id())) {
            conversation.setUser1Unread(0);
        } else {
            conversation.setUser2Unread(0);
        }
    }

    private LocalDateTime getConversationClearedAt(Conversation conversation, Long userId) {
        return userId.equals(conversation.getUser1Id())
                ? conversation.getUser1ClearedAt()
                : conversation.getUser2ClearedAt();
    }

    private LambdaQueryWrapper<Message> buildVisibleMessageQuery(Long conversationId, Long userId, LocalDateTime clearedAt) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<Message>()
                .eq(Message::getConversationId, conversationId)
                .and(inner -> inner
                        .nested(sender -> sender
                                .eq(Message::getSenderId, userId)
                                .isNull(Message::getSenderDeletedAt))
                        .or(receiver -> receiver
                                .eq(Message::getReceiverId, userId)
                                .isNull(Message::getReceiverDeletedAt)));
        if (clearedAt != null) {
            wrapper.gt(Message::getCreatedAt, clearedAt);
        }
        return wrapper;
    }

    private Message findLatestVisibleMessage(Long conversationId, Long userId, LocalDateTime clearedAt) {
        return messageMapper.selectOne(
                buildVisibleMessageQuery(conversationId, userId, clearedAt)
                        .orderByDesc(Message::getCreatedAt)
                        .last("LIMIT 1")
        );
    }

    private void refreshConversationLastMessage(Long conversationId) {
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            return;
        }

        Message latestMessage = messageMapper.selectOne(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getConversationId, conversationId)
                        .orderByDesc(Message::getCreatedAt)
                        .last("LIMIT 1")
        );

        if (latestMessage == null) {
            conversation.setLastMessage(null);
            conversation.setLastMessageAt(null);
        } else {
            conversation.setLastMessage(buildPreview(latestMessage.getContent()));
            conversation.setLastMessageAt(latestMessage.getCreatedAt());
        }
        conversationMapper.updateById(conversation);
    }

    private ConversationVO buildConversationVO(Conversation conversation, Long currentUserId) {
        Long opponentId = currentUserId.equals(conversation.getUser1Id())
                ? conversation.getUser2Id()
                : conversation.getUser1Id();
        User opponent = userMapper.selectById(opponentId);
        ConversationVO vo = ConversationVO.from(conversation, currentUserId, UserVO.from(opponent));
        if (vo.getOpponentId() == null) {
            vo.setOpponentId(opponentId);
        }

        Message latestVisibleMessage = findLatestVisibleMessage(
                conversation.getId(),
                currentUserId,
                getConversationClearedAt(conversation, currentUserId)
        );
        if (latestVisibleMessage == null) {
            vo.setLastMessage(null);
            vo.setLastMessageAt(null);
        } else {
            vo.setLastMessage(buildPreview(latestVisibleMessage.getContent()));
            vo.setLastMessageAt(latestVisibleMessage.getCreatedAt());
        }
        return vo;
    }

    private Comparator<ConversationVO> conversationComparator() {
        Comparator<LocalDateTime> timeComparator = Comparator.nullsLast(Comparator.reverseOrder());
        return Comparator
                .comparing((ConversationVO vo) -> Boolean.TRUE.equals(vo.getIsPinned())).reversed()
                .thenComparing(ConversationVO::getLastMessageAt, timeComparator)
                .thenComparing(ConversationVO::getId, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private PageVO<ConversationVO> paginate(List<ConversationVO> items, int page, int pageSize) {
        int total = items.size();
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / pageSize);
        int fromIndex = Math.min((page - 1) * pageSize, total);
        int toIndex = Math.min(fromIndex + pageSize, total);

        PageVO<ConversationVO> pageVO = new PageVO<>();
        pageVO.setList(new ArrayList<>(items.subList(fromIndex, toIndex)));
        pageVO.setPage(page);
        pageVO.setPageSize(pageSize);
        pageVO.setTotal(total);
        pageVO.setTotalPages(totalPages);
        return pageVO;
    }

    private int normalizePageSize(int pageSize) {
        return Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
    }

    private int defaultZero(Integer value) {
        return value == null ? 0 : value;
    }

    private String buildPreview(String content) {
        if (content == null) {
            return null;
        }
        String normalized = content.replaceAll("\\s+", " ").trim();
        if (normalized.isEmpty()) {
            return "";
        }
        if (normalized.length() > MAX_PREVIEW_LENGTH) {
            return normalized.substring(0, MAX_PREVIEW_LENGTH) + "...";
        }
        return normalized;
    }
}
