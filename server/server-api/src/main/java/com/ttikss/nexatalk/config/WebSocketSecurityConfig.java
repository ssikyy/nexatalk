package com.ttikss.nexatalk.config;

import com.ttikss.nexatalk.common.ErrorCode;
import com.ttikss.nexatalk.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WebSocket 安全配置
 * 用于在 WebSocket 连接时从 JWT token 中解析用户身份
 * 这样 convertAndSendToUser 才能正确地将消息推送给指定用户
 */
@Configuration
public class WebSocketSecurityConfig implements WebSocketMessageBrokerConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebSocketSecurityConfig.class);
    private static final String TOKEN_KEY_PREFIX = "token:";

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    public WebSocketSecurityConfig(WebSocketAuthInterceptor webSocketAuthInterceptor) {
        this.webSocketAuthInterceptor = webSocketAuthInterceptor;
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketAuthInterceptor);
    }

    @Component
    public static class WebSocketAuthInterceptor implements ChannelInterceptor {

        private final JwtUtil jwtUtil;
        private final StringRedisTemplate redisTemplate;

        public WebSocketAuthInterceptor(JwtUtil jwtUtil, StringRedisTemplate redisTemplate) {
            this.jwtUtil = jwtUtil;
            this.redisTemplate = redisTemplate;
        }

        @Override
        public Message<?> preSend(Message<?> message, MessageChannel channel) {
            StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
            if (accessor == null) {
                return message;
            }

            if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                String token = extractBearerToken(accessor);
                if (token == null) {
                    throw new IllegalArgumentException("未提供 WebSocket 认证信息");
                }

                try {
                    if (!jwtUtil.isValid(token)) {
                        throw new IllegalArgumentException("WebSocket Token 非法或已过期");
                    }

                    Long userId = jwtUtil.getUserId(token);
                    String storedToken = redisTemplate.opsForValue().get(TOKEN_KEY_PREFIX + userId);
                    if (!token.equals(storedToken)) {
                        throw new IllegalArgumentException("WebSocket 登录已失效，请重新登录");
                    }

                    StompPrincipal principal = new StompPrincipal(userId);
                    accessor.setUser(principal);
                    Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
                    if (sessionAttributes == null) {
                        sessionAttributes = new HashMap<>();
                        accessor.setSessionAttributes(sessionAttributes);
                    }
                    sessionAttributes.put("ws_user_id", userId);
                    sessionAttributes.put("ws_token", token);
                    log.info("【WebSocket认证】用户 {} 已连接", userId);
                } catch (Exception e) {
                    log.warn("WebSocket connect rejected: {}", e.getMessage());
                    throw new IllegalArgumentException(ErrorCode.UNAUTHORIZED.message());
                }
            } else if (accessor.getUser() instanceof StompPrincipal principal) {
                Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
                Object sessionToken = sessionAttributes != null ? sessionAttributes.get("ws_token") : null;
                if (!(sessionToken instanceof String token)) {
                    log.warn("WebSocket session for user {} is missing token state", principal.getUserId());
                    throw new IllegalArgumentException("WebSocket 登录已失效，请重新登录");
                }
                String storedToken = redisTemplate.opsForValue().get(TOKEN_KEY_PREFIX + principal.getUserId());
                if (!token.equals(storedToken)) {
                    log.warn("WebSocket session for user {} is no longer valid", principal.getUserId());
                    throw new IllegalArgumentException("WebSocket 登录已失效，请重新登录");
                }
            }

            return message;
        }

        private String extractBearerToken(StompHeaderAccessor accessor) {
            List<String> authHeaders = accessor.getNativeHeader("Authorization");
            if (authHeaders == null || authHeaders.isEmpty()) {
                return null;
            }

            String authHeader = authHeaders.get(0);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return null;
            }
            return authHeader.substring(7).trim();
        }
    }

    /**
     * 自定义 STOMP Principal，用于存储用户 ID
     */
    public static class StompPrincipal implements Principal {
        private final Long userId;

        public StompPrincipal(Long userId) {
            this.userId = userId;
        }

        @Override
        public String getName() {
            return userId.toString();
        }

        public Long getUserId() {
            return userId;
        }
    }
}
