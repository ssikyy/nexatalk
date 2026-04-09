package com.ttikss.nexatalk.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ttikss.nexatalk.dto.UserLoginRequest;
import com.ttikss.nexatalk.vo.LoginVO;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogSanitizerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldMaskSensitiveFieldsInNestedObjects() {
        UserLoginRequest request = new UserLoginRequest();
        request.setUsername("alice");
        request.setPassword("super-secret");

        LoginVO response = new LoginVO("jwt-token-value", 1L, "alice", "Alice", "", "", "", 0);
        String sanitized = LogSanitizer.sanitizeForLog(objectMapper, Map.of(
                "request", request,
                "response", response,
                "apiKey", "key-123"
        ));

        assertTrue(sanitized.contains("\"username\":\"alice\""));
        assertTrue(sanitized.contains("\"password\":\"***\""));
        assertTrue(sanitized.contains("\"token\":\"***\""));
        assertTrue(sanitized.contains("\"apiKey\":\"***\""));
        assertFalse(sanitized.contains("super-secret"));
        assertFalse(sanitized.contains("jwt-token-value"));
        assertFalse(sanitized.contains("key-123"));
    }
}
