package com.ttikss.nexatalk.service.impl;

import com.ttikss.nexatalk.entity.Notification;
import com.ttikss.nexatalk.exception.BusinessException;
import com.ttikss.nexatalk.mapper.NotificationMapper;
import com.ttikss.nexatalk.mapper.UserMapper;
import com.ttikss.nexatalk.vo.NotificationVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceImplTest {

    private NotificationMapper notificationMapper;
    private NotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        notificationMapper = Mockito.mock(NotificationMapper.class);
        notificationService = new NotificationServiceImpl(notificationMapper, Mockito.mock(UserMapper.class));
    }

    @Test
    void shouldRejectNotificationThatDoesNotBelongToCurrentUser() {
        when(notificationMapper.selectOne(any())).thenReturn(null);

        assertThrows(BusinessException.class, () -> notificationService.getMyNotificationDetail(1L, 100L));
    }

    @Test
    void shouldMarkUnreadNotificationAsReadWhenFetchingDetail() {
        Notification notification = new Notification();
        notification.setId(100L);
        notification.setUserId(1L);
        notification.setType(Notification.TYPE_SYSTEM);
        notification.setContent("hello");
        notification.setIsRead(0);
        when(notificationMapper.selectOne(any())).thenReturn(notification);

        NotificationVO vo = notificationService.getMyNotificationDetail(1L, 100L);

        assertEquals(100L, vo.getId());
        assertEquals(1, notification.getIsRead());
        verify(notificationMapper).updateById(notification);
    }

    @Test
    void shouldNotUpdateNotificationWhenAlreadyRead() {
        Notification notification = new Notification();
        notification.setId(101L);
        notification.setUserId(1L);
        notification.setType(Notification.TYPE_SYSTEM);
        notification.setContent("hello");
        notification.setIsRead(1);
        when(notificationMapper.selectOne(any())).thenReturn(notification);

        notificationService.getMyNotificationDetail(1L, 101L);

        verify(notificationMapper, never()).updateById(notification);
    }

    @Test
    void shouldSyncMissingSystemNotificationsForNewUserAndKeepOriginalCreatedAt() {
        LocalDateTime publishedAt = LocalDateTime.of(2026, 3, 10, 12, 30);

        Notification template = new Notification();
        template.setId(1L);
        template.setUserId(1L);
        template.setType(Notification.TYPE_SYSTEM);
        template.setTitle("系统公告");
        template.setContent("欢迎使用 NexaTalk");
        template.setContentType(Notification.CONTENT_TYPE_TEXT);
        template.setIsPinned(1);
        template.setIsBold(0);
        template.setIsRead(0);
        template.setEntityType(Notification.ENTITY_TYPE_NONE);
        template.setEntityId(0L);
        template.setBroadcastId("broadcast-1");
        template.setCreatedAt(publishedAt);

        when(notificationMapper.selectList(any()))
                .thenReturn(List.of(), List.of(template), List.of());
        when(notificationMapper.batchInsert(any())).thenReturn(1);

        notificationService.syncSystemNotificationsForUser(99L);

        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationMapper).batchInsert(captor.capture());
        Notification inserted = captor.getValue().get(0);
        assertEquals(99L, inserted.getUserId());
        assertEquals("broadcast-1", inserted.getBroadcastId());
        assertEquals("系统公告", inserted.getTitle());
        assertEquals(publishedAt, inserted.getCreatedAt());
    }

    @Test
    void shouldNotInsertDuplicateSystemNotificationsForUser() {
        LocalDateTime publishedAt = LocalDateTime.of(2026, 3, 10, 12, 30);

        Notification template = new Notification();
        template.setId(1L);
        template.setUserId(1L);
        template.setType(Notification.TYPE_SYSTEM);
        template.setTitle("系统公告");
        template.setContent("欢迎使用 NexaTalk");
        template.setContentType(Notification.CONTENT_TYPE_TEXT);
        template.setBroadcastId("broadcast-1");
        template.setCreatedAt(publishedAt);

        Notification existing = new Notification();
        existing.setId(2L);
        existing.setUserId(99L);
        existing.setType(Notification.TYPE_SYSTEM);
        existing.setTitle("系统公告");
        existing.setContent("欢迎使用 NexaTalk");
        existing.setContentType(Notification.CONTENT_TYPE_TEXT);
        existing.setBroadcastId("broadcast-1");
        existing.setCreatedAt(publishedAt);

        when(notificationMapper.selectList(any()))
                .thenReturn(List.of(), List.of(template), List.of(existing));

        notificationService.syncSystemNotificationsForUser(99L);

        verify(notificationMapper, never()).batchInsert(any());
    }
}
