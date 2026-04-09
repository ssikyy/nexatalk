-- 添加用户背景图字段
ALTER TABLE user ADD COLUMN banner_url VARCHAR(512) DEFAULT NULL COMMENT '背景图URL' AFTER avatar_url;
