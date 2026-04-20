package com.ttikss.nexatalk.util;

import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 帖子正文处理工具：
 * - 兼容旧版 Markdown 正文
 * - 正式支持 Tiptap 输出的富文本 HTML
 * - 统一处理净化、摘要提取和图片提取
 */
public final class PostContentUtils {

    private static final Set<String> ALLOWED_TAGS = Set.of(
            "p", "br", "strong", "b", "em", "i", "s", "del", "strike",
            "h1", "h2", "h3", "h4", "h5", "h6",
            "ul", "ol", "li",
            "blockquote", "pre", "code",
            "a", "img"
    );

    private static final Pattern HTML_TAG_PATTERN =
            Pattern.compile("<(/?)([a-zA-Z0-9]+)([^>]*)>", Pattern.CASE_INSENSITIVE);
    private static final Pattern HTML_PATTERN =
            Pattern.compile("<([a-zA-Z][a-zA-Z0-9]*)(\\s[^>]*)?>", Pattern.CASE_INSENSITIVE);
    private static final Pattern HTML_COMMENT_PATTERN =
            Pattern.compile("(?is)<!--.*?-->");
    private static final Pattern BLOCKED_CONTAINER_PATTERN =
            Pattern.compile("(?is)<(script|style|iframe|object|embed|svg|math|form|textarea|select|option|button)[^>]*>.*?</\\1>");
    private static final Pattern BLOCKED_SINGLE_TAG_PATTERN =
            Pattern.compile("(?is)<(script|style|iframe|object|embed|meta|link|input|button|textarea|select|option|svg|math)[^>]*?/?>");
    private static final Pattern ATTRIBUTE_PATTERN =
            Pattern.compile("([a-zA-Z_:][-a-zA-Z0-9_:.]*)\\s*=\\s*(\"([^\"]*)\"|'([^']*)'|([^\\s\"'=<>`]+))");
    private static final Pattern HTML_IMAGE_PATTERN =
            Pattern.compile("<img\\b[^>]*\\bsrc=[\"']([^\"']+)[\"'][^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern MARKDOWN_IMAGE_PATTERN =
            Pattern.compile("!\\[[^\\]]*]\\((.*?)\\)");

    private PostContentUtils() {
    }

    public static String normalizeContent(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }

        String normalized = content.trim();
        if (!looksLikeHtml(normalized)) {
            return normalized;
        }

        normalized = HTML_COMMENT_PATTERN.matcher(normalized).replaceAll("");
        normalized = BLOCKED_CONTAINER_PATTERN.matcher(normalized).replaceAll("");
        normalized = BLOCKED_SINGLE_TAG_PATTERN.matcher(normalized).replaceAll("");
        return sanitizeAllowedTags(normalized).trim();
    }

    public static boolean looksLikeHtml(String content) {
        return StringUtils.hasText(content) && HTML_PATTERN.matcher(content).find();
    }

    public static boolean hasMeaningfulContent(String content, List<String> fallbackImages) {
        if (StringUtils.hasText(toPlainText(content))) {
            return true;
        }
        if (!extractImages(content).isEmpty()) {
            return true;
        }
        return !sanitizeImageList(fallbackImages).isEmpty();
    }

    public static List<String> extractImages(String content) {
        if (!StringUtils.hasText(content)) {
            return Collections.emptyList();
        }

        LinkedHashSet<String> urls = new LinkedHashSet<>();
        String normalized = normalizeContent(content);

        Matcher htmlMatcher = HTML_IMAGE_PATTERN.matcher(normalized);
        while (htmlMatcher.find()) {
            String src = normalizeMediaUrl(htmlMatcher.group(1));
            if (StringUtils.hasText(src)) {
                urls.add(src);
            }
        }

        Matcher markdownMatcher = MARKDOWN_IMAGE_PATTERN.matcher(content);
        while (markdownMatcher.find()) {
            String src = normalizeMediaUrl(markdownMatcher.group(1));
            if (StringUtils.hasText(src)) {
                urls.add(src);
            }
        }

        return new ArrayList<>(urls);
    }

    public static List<String> resolveImages(String content, List<String> fallbackImages) {
        List<String> contentImages = extractImages(content);
        if (!contentImages.isEmpty()) {
            return contentImages;
        }
        return sanitizeImageList(fallbackImages);
    }

    public static String generateSummary(String content, int maxLength) {
        String plainText = toPlainText(content);
        if (!StringUtils.hasText(plainText)) {
            return "";
        }
        if (plainText.length() <= maxLength) {
            return plainText;
        }
        return plainText.substring(0, maxLength) + "...";
    }

    public static String toPlainText(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }

        if (looksLikeHtml(content)) {
            String normalized = normalizeContent(content)
                    .replaceAll("(?i)<br\\s*/?>", "\n")
                    .replaceAll("(?i)</(p|li|blockquote|h[1-6]|pre|ul|ol)>", "\n")
                    .replaceAll("<[^>]+>", " ");
            return normalizeWhitespace(decodeHtml(normalized));
        }

        String plainText = content
                .replaceAll("!\\[[^\\]]*]\\([^)]*\\)", " ")
                .replaceAll("\\[([^\\]]*)]\\([^)]*\\)", "$1")
                .replaceAll("(?m)^#{1,6}\\s+", "")
                .replaceAll("(?m)^>\\s?", "")
                .replaceAll("(?m)^[-*+]\\s+", "")
                .replaceAll("(?m)^\\d+\\.\\s+", "")
                .replaceAll("\\*\\*(.+?)\\*\\*", "$1")
                .replaceAll("\\*(.+?)\\*", "$1")
                .replaceAll("~~(.+?)~~", "$1")
                .replaceAll("`(.+?)`", "$1");
        return normalizeWhitespace(plainText);
    }

    public static String normalizeMediaUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return null;
        }
        String trimmed = url.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.startsWith("javascript:")
                || lower.startsWith("vbscript:")
                || lower.startsWith("data:")
                || lower.startsWith("file:")) {
            return null;
        }
        return trimmed;
    }

    private static List<String> sanitizeImageList(List<String> images) {
        if (images == null || images.isEmpty()) {
            return Collections.emptyList();
        }

        LinkedHashSet<String> safeImages = new LinkedHashSet<>();
        for (String image : images) {
            String normalized = normalizeMediaUrl(image);
            if (StringUtils.hasText(normalized)) {
                safeImages.add(normalized);
            }
        }
        return new ArrayList<>(safeImages);
    }

    private static String sanitizeAllowedTags(String html) {
        Matcher matcher = HTML_TAG_PATTERN.matcher(html);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String slash = matcher.group(1);
            String tagName = matcher.group(2).toLowerCase(Locale.ROOT);
            String attributes = matcher.group(3);

            String replacement = sanitizeTag(slash, tagName, attributes);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String sanitizeTag(String slash, String tagName, String attributes) {
        if (!ALLOWED_TAGS.contains(tagName)) {
            return "";
        }

        boolean closing = "/".equals(slash);
        if (closing) {
            if ("img".equals(tagName) || "br".equals(tagName)) {
                return "";
            }
            return "</" + tagName + ">";
        }

        if ("br".equals(tagName)) {
            return "<br>";
        }

        if ("img".equals(tagName)) {
            String src = normalizeMediaUrl(findAttribute(attributes, "src"));
            if (!StringUtils.hasText(src)) {
                return "";
            }
            String alt = findAttribute(attributes, "alt");
            if (StringUtils.hasText(alt)) {
                return "<img src=\"" + HtmlUtils.htmlEscape(src) + "\" alt=\"" + HtmlUtils.htmlEscape(alt) + "\">";
            }
            return "<img src=\"" + HtmlUtils.htmlEscape(src) + "\">";
        }

        if ("a".equals(tagName)) {
            String href = normalizeLinkUrl(findAttribute(attributes, "href"));
            if (!StringUtils.hasText(href)) {
                return "<a>";
            }
            return "<a href=\"" + HtmlUtils.htmlEscape(href)
                    + "\" target=\"_blank\" rel=\"noopener noreferrer nofollow\">";
        }

        return "<" + tagName + ">";
    }

    private static String findAttribute(String attributes, String name) {
        if (!StringUtils.hasText(attributes)) {
            return null;
        }

        Matcher matcher = ATTRIBUTE_PATTERN.matcher(attributes);
        while (matcher.find()) {
            String attrName = matcher.group(1);
            if (!name.equalsIgnoreCase(attrName)) {
                continue;
            }
            if (matcher.group(3) != null) {
                return matcher.group(3);
            }
            if (matcher.group(4) != null) {
                return matcher.group(4);
            }
            return matcher.group(5);
        }
        return null;
    }

    private static String normalizeLinkUrl(String url) {
        String normalized = normalizeMediaUrl(url);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (lower.startsWith("mailto:") || lower.startsWith("tel:")) {
            return normalized;
        }
        return normalized;
    }

    private static String decodeHtml(String text) {
        return text
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'");
    }

    private static String normalizeWhitespace(String text) {
        return text
                .replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }
}
