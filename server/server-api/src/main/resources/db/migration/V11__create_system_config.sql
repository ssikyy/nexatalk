-- =============================================================
-- NexaTalk 系统配置表
-- 用于存储系统级配置，如AI配置等，支持后台管理
-- =============================================================

CREATE TABLE IF NOT EXISTS `system_config` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '配置主键ID',
    `config_key`      VARCHAR(64)  NOT NULL                COMMENT '配置键（如 ai_enabled, ai_api_key 等）',
    `config_value`    TEXT         NOT NULL                COMMENT '配置值',
    `config_type`     VARCHAR(32)  NOT NULL DEFAULT 'string' COMMENT '配置类型: string, number, boolean',
    `description`     VARCHAR(255) NOT NULL DEFAULT ''     COMMENT '配置描述',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- 初始化默认AI配置
INSERT INTO `system_config` (`config_key`, `config_value`, `config_type`, `description`) VALUES
('ai_enabled', 'false', 'boolean', '是否开启AI功能'),
('ai_base_url', 'https://api.openai.com', 'string', 'AI API Base URL'),
('ai_api_key', '', 'string', 'AI API Key'),
('ai_model', 'qwen3.5-plus', 'string', 'AI模型名称'),
('ai_timeout_ms', '30000', 'number', 'AI请求超时时间(毫秒)');
