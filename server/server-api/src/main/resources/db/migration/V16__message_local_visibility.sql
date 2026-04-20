-- V16: Add per-user visibility markers for private messages
-- 说明：
-- 1. deleted_at 只保留作历史兼容，不再承担按用户可见性的职责
-- 2. 会话删除/清空改为本地行为，因此需要分别记录 user1 / user2 的删除与清空时间
-- 3. 单条消息删除也改为本地行为，因此分别记录发送方 / 接收方的隐藏时间

DELIMITER //
CREATE PROCEDURE add_message_local_visibility_columns()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'conversation'
          AND COLUMN_NAME = 'user1_deleted_at'
    ) THEN
        ALTER TABLE conversation
            ADD COLUMN user1_deleted_at DATETIME NULL COMMENT '用户1 本地删除会话时间';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'conversation'
          AND COLUMN_NAME = 'user2_deleted_at'
    ) THEN
        ALTER TABLE conversation
            ADD COLUMN user2_deleted_at DATETIME NULL COMMENT '用户2 本地删除会话时间';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'conversation'
          AND COLUMN_NAME = 'user1_cleared_at'
    ) THEN
        ALTER TABLE conversation
            ADD COLUMN user1_cleared_at DATETIME NULL COMMENT '用户1 本地清空会话时间';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'conversation'
          AND COLUMN_NAME = 'user2_cleared_at'
    ) THEN
        ALTER TABLE conversation
            ADD COLUMN user2_cleared_at DATETIME NULL COMMENT '用户2 本地清空会话时间';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'message'
          AND COLUMN_NAME = 'sender_deleted_at'
    ) THEN
        ALTER TABLE message
            ADD COLUMN sender_deleted_at DATETIME NULL COMMENT '发送方本地删除消息时间';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'message'
          AND COLUMN_NAME = 'receiver_deleted_at'
    ) THEN
        ALTER TABLE message
            ADD COLUMN receiver_deleted_at DATETIME NULL COMMENT '接收方本地删除消息时间';
    END IF;
END //
DELIMITER ;

CALL add_message_local_visibility_columns();
DROP PROCEDURE IF EXISTS add_message_local_visibility_columns;
