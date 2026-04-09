-- V12: Add tags and share_count columns to post table
ALTER TABLE post ADD COLUMN tags TEXT DEFAULT NULL COMMENT '标签列表（JSON格式存储标签数组）' AFTER images;
ALTER TABLE post ADD COLUMN share_count INT NOT NULL DEFAULT 0 COMMENT '分享次数（冗余字段）' AFTER favorite_count;
