-- =============================================================
-- V2: 补充 user 表缺失字段
-- 原因: user 表在 Flyway 引入前已存在（仅 4 列），
--       V1 的 CREATE TABLE IF NOT EXISTS 未对其生效
-- =============================================================

ALTER TABLE `user`
    ADD COLUMN IF NOT EXISTS `nickname`   VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '昵称，用于展示' AFTER `password`,
    ADD COLUMN IF NOT EXISTS `avatar_url` VARCHAR(512) NOT NULL DEFAULT '' COMMENT '头像 URL' AFTER `nickname`,
    ADD COLUMN IF NOT EXISTS `bio`        VARCHAR(255) NOT NULL DEFAULT '' COMMENT '个人简介' AFTER `avatar_url`,
    ADD COLUMN IF NOT EXISTS `role`       TINYINT      NOT NULL DEFAULT 0  COMMENT '角色: 0=普通用户, 1=管理员' AFTER `bio`,
    ADD COLUMN IF NOT EXISTS `status`     TINYINT      NOT NULL DEFAULT 0  COMMENT '账号状态: 0=正常, 1=禁言, 2=封禁' AFTER `role`,
    ADD COLUMN IF NOT EXISTS `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间' AFTER `created_at`;
