export const TITLE_MAX_LENGTH = 100

export function looksLikeHtml(content) {
  return typeof content === 'string' && /<([a-z][\w-]*)(\s[^>]*)?>/i.test(content)
}

function normalizeWhitespace(text) {
  return (text || '')
    .replace(/\u00a0/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}

function normalizeMediaUrl(url) {
  const value = typeof url === 'string' ? url.trim() : ''
  if (!value) return ''
  if (/^(javascript:|vbscript:|data:)/i.test(value)) return ''
  return value
}

export function extractImageUrls(content) {
  if (!content || typeof content !== 'string') return []

  const urls = []
  const pushUrl = (url) => {
    const normalized = normalizeMediaUrl(url)
    if (!normalized || urls.includes(normalized) || urls.length >= 9) return
    urls.push(normalized)
  }

  const htmlImgRegex = /<img\b[^>]*\bsrc=["']([^"']+)["'][^>]*>/gi
  let match
  while ((match = htmlImgRegex.exec(content)) !== null) {
    pushUrl(match[1])
  }

  const markdownImgRegex = /!\[[^\]]*]\((.*?)\)/g
  while ((match = markdownImgRegex.exec(content)) !== null) {
    pushUrl(match[1])
  }

  return urls
}

function extractHtmlText(content) {
  const parser = new DOMParser()
  const doc = parser.parseFromString(`<div>${content}</div>`, 'text/html')
  doc.body.querySelectorAll('img').forEach((node) => node.remove())
  return normalizeWhitespace(doc.body.textContent || '')
}

function extractMarkdownText(content) {
  return normalizeWhitespace(
    content
      .replace(/!\[[^\]]*]\([^)]*\)/g, ' ')
      .replace(/\[([^\]]*)]\([^)]*\)/g, '$1')
      .replace(/^#{1,6}\s+/gm, '')
      .replace(/[*_~`>#-]+/g, ' ')
      .replace(/\d+\.\s+/g, ' ')
  )
}

export function getMeaningfulContent(content) {
  if (!content || typeof content !== 'string') {
    return { text: '', images: [] }
  }

  return {
    text: looksLikeHtml(content) ? extractHtmlText(content) : extractMarkdownText(content),
    images: extractImageUrls(content)
  }
}

export function stripHtmlParagraphWrapper(text) {
  if (!text || typeof text !== 'string') return ''

  let normalized = text.trim()
  if (normalized.startsWith('<p>') && normalized.endsWith('</p>')) {
    normalized = normalized.slice(3, -4).trim()
  }

  return normalized
    .replace(/<\/p>\s*<p>/gi, '\n')
    .replace(/<p>/gi, '')
    .replace(/<\/p>/gi, '')
    .trim()
}

export function formatPreviewText(content) {
  if (!content || typeof content !== 'string') return ''

  if (looksLikeHtml(content)) {
    const normalizedHtml = content
      .replace(/<img[^>]*>/gi, '\n[图片]\n')
      .replace(/<br\s*\/?>/gi, '\n')
      .replace(/<li[^>]*>/gi, '• ')
      .replace(/<\/(p|div|li|blockquote|h[1-6]|pre|ul|ol)>/gi, '\n')

    const parser = new DOMParser()
    const doc = parser.parseFromString(`<div>${normalizedHtml}</div>`, 'text/html')
    return normalizePreviewWhitespace(doc.body.textContent || '')
  }

  return normalizePreviewWhitespace(
    content
      .replace(/!\[[^\]]*]\([^)]*\)/g, '[图片]')
      .replace(/\[([^\]]*)]\([^)]*\)/g, '$1')
      .replace(/^#{1,6}\s+/gm, '')
      .replace(/^>\s?/gm, '')
      .replace(/^[-*+]\s+/gm, '• ')
      .replace(/^\d+\.\s+/gm, '')
      .replace(/```[\s\S]*?```/g, (match) => match.replace(/```/g, ''))
      .replace(/`([^`]+)`/g, '$1')
  )
}

function normalizePreviewWhitespace(text) {
  return (text || '')
    .replace(/\u00a0/g, ' ')
    .replace(/\r\n?/g, '\n')
    .replace(/[ \t]+\n/g, '\n')
    .replace(/\n{3,}/g, '\n\n')
    .split('\n')
    .map((line) => line.trim())
    .join('\n')
    .trim()
}
