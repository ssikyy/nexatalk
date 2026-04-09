-- 系统通知「广播 ID」：同一次发布的多条记录（每个用户一条）共用同一 broadcast_id，管理端按广播去重展示
ALTER TABLE notification
    ADD COLUMN broadcast_id VARCHAR(64) DEFAULT NULL COMMENT '系统通知广播ID，同一次发布的多条记录相同' AFTER entity_id;

CREATE INDEX idx_notification_type_broadcast ON notification(type, broadcast_id);
