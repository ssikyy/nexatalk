package com.ttikss.nexatalk.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Array;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 操作日志脱敏工具。
 */
public final class LogSanitizer {

    private static final String MASK = "***";
    private static final Set<String> SENSITIVE_FIELD_NAMES = Set.of(
            "password",
            "oldpassword",
            "newpassword",
            "confirmpassword",
            "token",
            "authorization",
            "apikey",
            "secret"
    );

    private LogSanitizer() {
    }

    public static String sanitizeForLog(ObjectMapper objectMapper, Object value) {
        try {
            JsonNode sanitizedNode = sanitizeValue(objectMapper, value);
            return objectMapper.writeValueAsString(sanitizedNode);
        } catch (Exception ignored) {
            return "\"<unserializable>\"";
        }
    }

    private static JsonNode sanitizeValue(ObjectMapper objectMapper, Object value) {
        if (value == null) {
            return JsonNodeFactory.instance.nullNode();
        }
        if (value instanceof MultipartFile multipartFile) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("name", multipartFile.getOriginalFilename());
            node.put("contentType", multipartFile.getContentType());
            node.put("size", multipartFile.getSize());
            return node;
        }
        if (value instanceof ServletRequest || value instanceof ServletResponse || value instanceof BindingResult) {
            return JsonNodeFactory.instance.textNode("<omitted>");
        }
        if (value instanceof Map<?, ?> mapValue) {
            ObjectNode node = objectMapper.createObjectNode();
            for (Map.Entry<?, ?> entry : mapValue.entrySet()) {
                String fieldName = String.valueOf(entry.getKey());
                node.set(fieldName, sanitizeField(objectMapper, fieldName, entry.getValue()));
            }
            return node;
        }
        if (value instanceof Iterable<?> iterable) {
            ArrayNode arrayNode = objectMapper.createArrayNode();
            for (Object item : iterable) {
                arrayNode.add(sanitizeValue(objectMapper, item));
            }
            return arrayNode;
        }
        if (value.getClass().isArray()) {
            ArrayNode arrayNode = objectMapper.createArrayNode();
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                arrayNode.add(sanitizeValue(objectMapper, Array.get(value, i)));
            }
            return arrayNode;
        }

        JsonNode jsonNode = objectMapper.valueToTree(value);
        return sanitizeJsonNode(objectMapper, null, jsonNode);
    }

    private static JsonNode sanitizeField(ObjectMapper objectMapper, String fieldName, Object value) {
        if (isSensitiveField(fieldName)) {
            return JsonNodeFactory.instance.textNode(MASK);
        }
        return sanitizeValue(objectMapper, value);
    }

    private static JsonNode sanitizeJsonNode(ObjectMapper objectMapper, String fieldName, JsonNode node) {
        if (isSensitiveField(fieldName)) {
            return JsonNodeFactory.instance.textNode(MASK);
        }
        if (node == null || node.isNull() || node.isValueNode()) {
            return node;
        }
        if (node.isArray()) {
            ArrayNode sanitizedArray = objectMapper.createArrayNode();
            for (JsonNode item : node) {
                sanitizedArray.add(sanitizeJsonNode(objectMapper, null, item));
            }
            return sanitizedArray;
        }

        ObjectNode sanitizedObject = objectMapper.createObjectNode();
        node.fields().forEachRemaining(entry ->
                sanitizedObject.set(
                        entry.getKey(),
                        sanitizeJsonNode(objectMapper, entry.getKey(), entry.getValue())
                )
        );
        return sanitizedObject;
    }

    private static boolean isSensitiveField(String fieldName) {
        if (fieldName == null) {
            return false;
        }
        String normalized = fieldName.toLowerCase(Locale.ROOT).replace("_", "");
        if (SENSITIVE_FIELD_NAMES.contains(normalized)) {
            return true;
        }
        List<String> keywords = List.of("password", "token", "secret", "apikey", "authorization");
        return keywords.stream().anyMatch(normalized::contains);
    }
}
