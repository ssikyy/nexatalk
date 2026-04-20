package com.ttikss.nexatalk.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostContentUtilsTest {

    @Test
    void shouldSanitizeRichTextAndKeepSafeElements() {
        String content = """
                <p onclick="alert(1)">Hello<script>alert(1)</script></p>
                <a href="javascript:alert(2)" onclick="alert(3)">unsafe</a>
                <a href="https://example.com/post" target="_self">safe</a>
                <img src="/uploads/a.png" onerror="alert(4)" alt="cover">
                <iframe src="https://bad.example"></iframe>
                """;

        String normalized = PostContentUtils.normalizeContent(content);

        assertFalse(normalized.contains("script"));
        assertFalse(normalized.contains("onclick"));
        assertFalse(normalized.contains("iframe"));
        assertFalse(normalized.contains("javascript:"));
        assertTrue(normalized.contains("<a>unsafe</a>"));
        assertTrue(normalized.contains("<a href=\"https://example.com/post\" target=\"_blank\" rel=\"noopener noreferrer nofollow\">safe</a>"));
        assertTrue(normalized.contains("<img src=\"/uploads/a.png\" alt=\"cover\">"));
    }

    @Test
    void shouldExtractImagesFromHtmlAndMarkdown() {
        String content = """
                <p>正文</p>
                <img src="/uploads/1.png">
                ![cover](https://example.com/2.png)
                """;

        assertEquals(
                List.of("/uploads/1.png", "https://example.com/2.png"),
                PostContentUtils.extractImages(content)
        );
    }

    @Test
    void shouldGenerateSummaryFromRichText() {
        String content = "<p>Hello <strong>NexaTalk</strong></p><p>欢迎回来</p>";

        assertEquals("Hello NexaTalk 欢迎回来", PostContentUtils.generateSummary(content, 100));
    }

    @Test
    void shouldTreatImageOnlyPostAsMeaningful() {
        String content = "<p><img src=\"/uploads/only-image.png\"></p>";

        assertTrue(PostContentUtils.hasMeaningfulContent(content, List.of()));
    }
}
