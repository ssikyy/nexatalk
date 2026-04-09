-- V14: Create AI user Talk and add conversation pinned field
-- 1. 创建 AI 用户 Talk
INSERT INTO user (username, password, nickname, role, status, created_at, updated_at)
SELECT 'talk', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'Talk', 3, 0, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM user WHERE username = 'talk');

-- 2. 给 conversation 表添加 is_pinned 字段（用于置顶会话）
-- 使用存储过程来处理列已存在的情况
DELIMITER //
CREATE PROCEDURE add_column_if_not_exists()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'conversation' AND COLUMN_NAME = 'is_pinned') THEN
        ALTER TABLE conversation ADD COLUMN is_pinned TINYINT NOT NULL DEFAULT 0 COMMENT '是否置顶: 0=否, 1=是';
    END IF;
END //
DELIMITER ;

CALL add_column_if_not_exists();
DROP PROCEDURE IF EXISTS add_column_if_not_exists;
