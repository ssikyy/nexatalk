-- =============================================================
-- NexaTalk 数据库迁移脚本
-- 版本: V13 -- 私信消息表扩展
-- 说明: 添加消息类型、撤回标记等字段
-- =============================================================

-- 1. 为 message 表添加 type 字段（消息类型：0=文本，1=图片，2=语音）
ALTER TABLE message ADD COLUMN type TINYINT NOT NULL DEFAULT 0 COMMENT '消息类型: 0=文本, 1=图片, 2=语音';

-- 2. 为 message 表添加 is_recalled 字段（是否已撤回）
ALTER TABLE message ADD COLUMN is_recalled TINYINT NOT NULL DEFAULT 0 COMMENT '是否已撤回: 0=否, 1=是';

-- 3. 添加索引优化查询性能
-- 优化：根据会话ID和创建时间查询消息
ALTER TABLE message ADD INDEX idx_conversation_created (conversation_id, created_at DESC);

-- 4. 为 conversation 表添加 deleted_at 字段（软删除支持）
ALTER TABLE conversation ADD COLUMN deleted_at DATETIME NULL COMMENT '软删除时间';

-- 5. 为 message 表添加 deleted_at 字段（软删除支持）
ALTER TABLE message ADD COLUMN deleted_at DATETIME NULL COMMENT '软删除时间';
