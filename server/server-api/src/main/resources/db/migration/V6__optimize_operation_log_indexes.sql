-- =============================================================
-- NexaTalk 数据库迁移脚本
-- 版本: V6 — 优化操作日志表索引
-- =============================================================

-- 为 operation_log 表添加复合索引，提升常见查询场景性能
-- 场景1: 按模块 + 时间范围查询
-- 场景2: 按用户名 + 时间范围查询
-- 场景3: 按状态 + 时间范围查询

-- 复合索引：模块 + 创建时间（最常用的筛选组合）
ALTER TABLE `operation_log` ADD INDEX `idx_module_created` (`module`, `created_at` DESC);

-- 复合索引：用户名 + 创建时间（查询某用户的操作历史）
ALTER TABLE `operation_log` ADD INDEX `idx_username_created` (`username`, `created_at` DESC);

-- 复合索引：状态 + 创建时间（查询失败操作）
ALTER TABLE `operation_log` ADD INDEX `idx_status_created` (`status`, `created_at` DESC);

-- 复合索引：用户ID + 模块 + 创建时间（查询某用户在某模块的操作）
ALTER TABLE `operation_log` ADD INDEX `idx_user_module_created` (`user_id`, `module`, `created_at` DESC);
