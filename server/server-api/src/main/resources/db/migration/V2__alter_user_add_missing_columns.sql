-- =============================================================
-- V2: 补充 user 表缺失字段
-- 原因: user 表在 Flyway 引入前已存在（仅 4 列），
--       V1 的 CREATE TABLE IF NOT EXISTS 未对其生效
-- =============================================================

SET @schema_name = DATABASE();

SET @ddl = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @schema_name
              AND TABLE_NAME = 'user'
              AND COLUMN_NAME = 'nickname'
        ),
        'SELECT 1',
        'ALTER TABLE `user` ADD COLUMN `nickname` VARCHAR(64) NOT NULL DEFAULT '''' COMMENT ''昵称，用于展示'' AFTER `password`'
    )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @schema_name
              AND TABLE_NAME = 'user'
              AND COLUMN_NAME = 'avatar_url'
        ),
        'SELECT 1',
        'ALTER TABLE `user` ADD COLUMN `avatar_url` VARCHAR(512) NOT NULL DEFAULT '''' COMMENT ''头像 URL'' AFTER `nickname`'
    )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @schema_name
              AND TABLE_NAME = 'user'
              AND COLUMN_NAME = 'bio'
        ),
        'SELECT 1',
        'ALTER TABLE `user` ADD COLUMN `bio` VARCHAR(255) NOT NULL DEFAULT '''' COMMENT ''个人简介'' AFTER `avatar_url`'
    )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @schema_name
              AND TABLE_NAME = 'user'
              AND COLUMN_NAME = 'role'
        ),
        'SELECT 1',
        'ALTER TABLE `user` ADD COLUMN `role` TINYINT NOT NULL DEFAULT 0 COMMENT ''角色: 0=普通用户, 1=管理员'' AFTER `bio`'
    )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @schema_name
              AND TABLE_NAME = 'user'
              AND COLUMN_NAME = 'status'
        ),
        'SELECT 1',
        'ALTER TABLE `user` ADD COLUMN `status` TINYINT NOT NULL DEFAULT 0 COMMENT ''账号状态: 0=正常, 1=禁言, 2=封禁'' AFTER `role`'
    )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @schema_name
              AND TABLE_NAME = 'user'
              AND COLUMN_NAME = 'updated_at'
        ),
        'SELECT 1',
        'ALTER TABLE `user` ADD COLUMN `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''最后更新时间'' AFTER `created_at`'
    )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
