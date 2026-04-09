package com.ttikss.nexatalk.config;

import com.ttikss.nexatalk.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class WebSocketAuthInterceptorTest {

    private JwtUtil jwtUtil;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private WebSocketSecurityConfig.WebSocketAuthInterceptor interceptor;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        jwtUtil = Mockito.mock(JwtUtil.class);
        redisTemplate = Mockito.mock(StringRedisTemplate.class);
        valueOperations = Mockito.mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        interceptor = new WebSocketSecurityConfig.WebSocketAuthInterceptor(jwtUtil, redisTemplate);
    }

    @Test
    void shouldRejectConnectWhenRedisTokenDoesNotMatch() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer revoked-token");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(jwtUtil.isValid("revoked-token")).thenReturn(true);
        when(jwtUtil.getUserId("revoked-token")).thenReturn(7L);
        when(valueOperations.get("token:7")).thenReturn("another-token");

        assertThrows(IllegalArgumentException.class, () -> interceptor.preSend(message, null));
    }

    @Test
    void shouldAttachPrincipalForValidConnect() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setSessionAttributes(new HashMap<>());
        accessor.setNativeHeader("Authorization", "Bearer valid-token");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(jwtUtil.isValid("valid-token")).thenReturn(true);
        when(jwtUtil.getUserId("valid-token")).thenReturn(9L);
        when(valueOperations.get("token:9")).thenReturn("valid-token");

        Message<?> result = assertDoesNotThrow(() -> interceptor.preSend(message, null));
        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);

        assertNotNull(resultAccessor.getUser());
        assertEquals("9", resultAccessor.getUser().getName());
    }
}
