-- V8: Add images column to post table
ALTER TABLE post ADD COLUMN images TEXT DEFAULT NULL COMMENT '正文图片列表（JSON格式存储URL数组）' AFTER cover_url;
