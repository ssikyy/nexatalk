package com.ttikss.nexatalk.service.impl;

import com.ttikss.nexatalk.entity.Notification;
import com.ttikss.nexatalk.exception.BusinessException;
import com.ttikss.nexatalk.mapper.NotificationMapper;
import com.ttikss.nexatalk.mapper.UserMapper;
import com.ttikss.nexatalk.vo.NotificationVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

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
}
