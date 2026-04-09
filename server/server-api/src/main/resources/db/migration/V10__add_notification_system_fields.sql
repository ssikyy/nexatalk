-- 为 notification 表添加系统通知所需的字段
ALTER TABLE notification
    ADD COLUMN title VARCHAR(255) DEFAULT '' COMMENT '通知标题' AFTER content,
    ADD COLUMN content_type TINYINT NOT NULL DEFAULT 1 COMMENT '内容类型: 1=纯文字, 2=图片, 3=图文混排' AFTER title,
    ADD COLUMN image_url VARCHAR(512) DEFAULT '' COMMENT '图片URL' AFTER content_type,
    ADD COLUMN is_pinned TINYINT NOT NULL DEFAULT 0 COMMENT '是否置顶: 0=不置顶, 1=置顶' AFTER image_url,
    ADD COLUMN is_bold TINYINT NOT NULL DEFAULT 0 COMMENT '是否粗体: 0=普通, 1=粗体' AFTER is_pinned;

-- 创建索引优化查询
CREATE INDEX idx_notification_type_pinned ON notification(type, is_pinned, created_at DESC);
CREATE INDEX idx_notification_user_type ON notification(user_id, type, created_at DESC);
