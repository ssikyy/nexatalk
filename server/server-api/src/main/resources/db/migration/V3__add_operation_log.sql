-- =============================================================
-- NexaTalk 数据库迁移脚本
-- 版本: V3 — 添加系统日志表
-- =============================================================

-- -------------------------------------------------------
-- 系统日志表（OperationLog 模块）
--    记录用户操作日志，用于审计和问题排查
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS `operation_log` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '日志主键 ID',
    `user_id`      BIGINT                               COMMENT '操作用户 ID',
    `username`     VARCHAR(64)  NOT NULL DEFAULT ''     COMMENT '操作用户名',
    `module`       VARCHAR(64)  NOT NULL DEFAULT ''     COMMENT '操作模块，如：用户管理、帖子管理',
    `operation`    VARCHAR(64)  NOT NULL DEFAULT ''     COMMENT '操作类型，如：新增、编辑、删除',
    `method`       VARCHAR(256) NOT NULL DEFAULT ''     COMMENT '请求方法，如：POST /api/users',
    `ip`           VARCHAR(64)  NOT NULL DEFAULT ''     COMMENT '客户端 IP',
    `params`       TEXT                             COMMENT '请求参数',
    `result`       TEXT                             COMMENT '返回结果',
    `status`       TINYINT      NOT NULL DEFAULT 1      COMMENT '操作结果: 1=成功, 0=失败',
    `error_msg`    VARCHAR(512) NOT NULL DEFAULT ''     COMMENT '错误信息',
    `duration`     INT          NOT NULL DEFAULT 0      COMMENT '操作耗时（毫秒）',
    `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_module` (`module`),
    KEY `idx_created_at` (`created_at` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统操作日志表';
