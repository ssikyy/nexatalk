package com.ttikss.nexatalk.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ttikss.nexatalk.controller.NotificationController;
import com.ttikss.nexatalk.service.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class JwtAuthInterceptorTest {

    private JwtUtil jwtUtil;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private JwtAuthInterceptor interceptor;
    private HandlerMethod adminHandlerMethod;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws NoSuchMethodException {
        jwtUtil = Mockito.mock(JwtUtil.class);
        redisTemplate = Mockito.mock(StringRedisTemplate.class);
        valueOperations = Mockito.mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        interceptor = new JwtAuthInterceptor(jwtUtil, redisTemplate, new ObjectMapper());

        NotificationController controller = new NotificationController(Mockito.mock(NotificationService.class));
        adminHandlerMethod = new HandlerMethod(
                controller,
                NotificationController.class.getMethod("listSystemNotifications", int.class, int.class)
        );
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void shouldRejectAnonymousRequestForAdminEndpoint() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, adminHandlerMethod);

        assertFalse(allowed);
        assertEquals(401, response.getStatus());
    }

    @Test
    void shouldRejectNonAdminUserForAdminEndpoint() throws Exception {
        String token = "user-token";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtUtil.getUserId(token)).thenReturn(1L);
        when(jwtUtil.getRole(token)).thenReturn(0);
        when(jwtUtil.getUsername(token)).thenReturn("user");
        when(valueOperations.get("token:1")).thenReturn(token);

        boolean allowed = interceptor.preHandle(request, response, adminHandlerMethod);

        assertFalse(allowed);
        assertEquals(403, response.getStatus());
    }

    @Test
    void shouldAllowAdminUserForAdminEndpoint() throws Exception {
        String token = "admin-token";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtUtil.getUserId(token)).thenReturn(2L);
        when(jwtUtil.getRole(token)).thenReturn(1);
        when(jwtUtil.getUsername(token)).thenReturn("admin");
        when(valueOperations.get("token:2")).thenReturn(token);

        boolean allowed = interceptor.preHandle(request, response, adminHandlerMethod);

        assertTrue(allowed);
        assertEquals(2L, UserContext.getUserId());
    }
}
