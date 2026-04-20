package com.ttikss.nexatalk.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ttikss.nexatalk.entity.Conversation;
import com.ttikss.nexatalk.entity.Message;
import com.ttikss.nexatalk.entity.User;
import com.ttikss.nexatalk.mapper.BlacklistMapper;
import com.ttikss.nexatalk.mapper.ConversationMapper;
import com.ttikss.nexatalk.mapper.MessageMapper;
import com.ttikss.nexatalk.mapper.UserMapper;
import com.ttikss.nexatalk.service.AiService;
import com.ttikss.nexatalk.vo.PageVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceImplTest {

    @Mock
    private ConversationMapper conversationMapper;
    @Mock
    private MessageMapper messageMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private BlacklistMapper blacklistMapper;
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private AiService aiService;

    private MessageServiceImpl messageService;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Conversation.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Message.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), User.class);
        messageService = new MessageServiceImpl(
                conversationMapper,
                messageMapper,
                userMapper,
                blacklistMapper,
                messagingTemplate,
                aiService
        );
    }

    @Test
    void listMessages_returnsLatestChunkButKeepsAscendingOrderInsidePage() {
        Conversation conversation = buildConversation(99L, 1L, 2L);
        Page<Message> page = new Page<>(1, 2);
        page.setTotal(3);
        page.setRecords(List.of(
                buildMessage(3L, 99L, 2L, 1L, "第三条", LocalDateTime.now()),
                buildMessage(2L, 99L, 1L, 2L, "第二条", LocalDateTime.now().minusMinutes(1))
        ));

        when(conversationMapper.selectById(99L)).thenReturn(conversation);
        when(messageMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        PageVO<Message> result = messageService.listMessages(1L, 99L, 1, 2);

        assertEquals(List.of(2L, 3L), result.getList().stream().map(Message::getId).toList());
        assertEquals(1L, result.getPage());
        assertEquals(2L, result.getPageSize());
        assertEquals(3L, result.getTotal());
        assertEquals(2L, result.getTotalPages());
    }

    @Test
    void deleteConversation_marksOnlyCurrentUserAsDeleted() {
        Conversation conversation = buildConversation(11L, 1L, 2L);
        conversation.setUser1Unread(4);
        conversation.setUser2Unread(1);

        when(conversationMapper.selectById(11L)).thenReturn(conversation);

        messageService.deleteConversation(1L, 11L);

        ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);
        verify(messageMapper).markConversationRead(11L, 1L);
        verify(conversationMapper).updateById(captor.capture());
        verify(messageMapper, never()).delete(any(LambdaQueryWrapper.class));

        Conversation updated = captor.getValue();
        assertSame(conversation, updated);
        assertEquals(0, updated.getUser1Unread());
        assertEquals(1, updated.getUser2Unread());
        assertNotNull(updated.getUser1DeletedAt());
        assertNull(updated.getUser2DeletedAt());
        assertNull(updated.getUser1ClearedAt());
        assertNull(updated.getUser2ClearedAt());
    }

    @Test
    void clearMessages_marksOnlyCurrentUserAsCleared() {
        Conversation conversation = buildConversation(12L, 1L, 2L);
        conversation.setUser1Unread(2);
        conversation.setUser2Unread(5);

        when(conversationMapper.selectById(12L)).thenReturn(conversation);

        messageService.clearMessages(2L, 12L);

        ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);
        verify(messageMapper).markConversationRead(12L, 2L);
        verify(conversationMapper).updateById(captor.capture());
        verify(messageMapper, never()).delete(any(LambdaQueryWrapper.class));

        Conversation updated = captor.getValue();
        assertEquals(2, updated.getUser1Unread());
        assertEquals(0, updated.getUser2Unread());
        assertNotNull(updated.getUser2ClearedAt());
        assertNull(updated.getUser1ClearedAt());
        assertNull(updated.getUser1DeletedAt());
        assertNull(updated.getUser2DeletedAt());
    }

    @Test
    void deleteMessage_onlyHidesMessageForCurrentViewer() {
        Message message = buildMessage(5L, 88L, 1L, 2L, "hello", LocalDateTime.now());

        when(messageMapper.selectById(5L)).thenReturn(message);

        messageService.deleteMessage(2L, 5L);

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageMapper).updateById(captor.capture());

        Message updated = captor.getValue();
        assertNotNull(updated.getReceiverDeletedAt());
        assertNull(updated.getSenderDeletedAt());
    }

    @Test
    void recallMessage_recomputesConversationPreview() {
        LocalDateTime createdAt = LocalDateTime.now().minusMinutes(1);
        Message message = buildMessage(7L, 77L, 1L, 2L, "原消息", createdAt);
        Conversation conversation = buildConversation(77L, 1L, 2L);
        Message latestAfterRecall = buildMessage(7L, 77L, 1L, 2L, "【此消息已被撤回】", createdAt);
        latestAfterRecall.setIsRecalled(1);

        when(messageMapper.selectById(7L)).thenReturn(message);
        when(conversationMapper.selectById(77L)).thenReturn(conversation);
        when(messageMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(latestAfterRecall);

        messageService.recallMessage(1L, 7L);

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        ArgumentCaptor<Conversation> conversationCaptor = ArgumentCaptor.forClass(Conversation.class);
        verify(messageMapper).updateById(messageCaptor.capture());
        verify(conversationMapper).updateById(conversationCaptor.capture());

        Message updatedMessage = messageCaptor.getValue();
        assertEquals("【此消息已被撤回】", updatedMessage.getContent());
        assertEquals(1, updatedMessage.getIsRecalled());

        Conversation updatedConversation = conversationCaptor.getValue();
        assertEquals("【此消息已被撤回】", updatedConversation.getLastMessage());
        assertEquals(createdAt, updatedConversation.getLastMessageAt());
        verify(messagingTemplate).convertAndSendToUser(eq("2"), eq("/queue/messages"), any());
    }

    private Conversation buildConversation(Long id, Long user1Id, Long user2Id) {
        Conversation conversation = new Conversation();
        conversation.setId(id);
        conversation.setUser1Id(user1Id);
        conversation.setUser2Id(user2Id);
        conversation.setUser1Unread(0);
        conversation.setUser2Unread(0);
        return conversation;
    }

    private Message buildMessage(Long id, Long conversationId, Long senderId, Long receiverId, String content, LocalDateTime createdAt) {
        Message message = new Message();
        message.setId(id);
        message.setConversationId(conversationId);
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setContent(content);
        message.setCreatedAt(createdAt);
        message.setIsRead(0);
        message.setIsRecalled(0);
        return message;
    }
}
