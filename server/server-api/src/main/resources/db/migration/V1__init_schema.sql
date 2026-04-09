-- =============================================================
-- NexaTalk 数据库初始化脚本
-- 版本: V1 — 全量表骨架
-- 说明: 一次性建立所有模块核心表结构，后续通过 V2、V3... 扩展字段
-- =============================================================

-- -------------------------------------------------------
-- 1. 用户表（User 模块）
--    存储用户基础信息、角色与账号状态
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户主键 ID',
    `username`     VARCHAR(64)  NOT NULL                COMMENT '用户名，唯一',
    `password`     VARCHAR(255) NOT NULL                COMMENT 'BCrypt 加密后的密码',
    `nickname`     VARCHAR(64)  NOT NULL DEFAULT ''     COMMENT '昵称，用于展示',
    `avatar_url`   VARCHAR(512) NOT NULL DEFAULT ''     COMMENT '头像 URL',
    `bio`          VARCHAR(255) NOT NULL DEFAULT ''     COMMENT '个人简介',
    `role`         TINYINT      NOT NULL DEFAULT 0      COMMENT '角色: 0=普通用户, 1=管理员',
    `status`       TINYINT      NOT NULL DEFAULT 0      COMMENT '账号状态: 0=正常, 1=禁言, 2=封禁',
    `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- -------------------------------------------------------
-- 2. 分区表（Section 模块）
--    论坛版块分类，如"技术讨论"、"生活随想"等
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS `section` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '分区主键 ID',
    `name`         VARCHAR(64)  NOT NULL                COMMENT '分区名称',
    `description`  VARCHAR(255) NOT NULL DEFAULT ''     COMMENT '分区描述',
    `icon_url`     VARCHAR(512) NOT NULL DEFAULT ''     COMMENT '分区图标 URL',
    `sort_order`   INT          NOT NULL DEFAULT 0      COMMENT '排序权重，数字越大越靠前',
    `status`       TINYINT      NOT NULL DEFAULT 0      COMMENT '状态: 0=正常, 1=禁用',
    `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分区表';

-- -------------------------------------------------------
-- 3. 帖子表（Post 模块）
--    存储帖子核心内容，支持草稿、审核、下架等状态
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS `post` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '帖子主键 ID',
    `user_id`      BIGINT       NOT NULL                COMMENT '发帖用户 ID',
    `section_id`   BIGINT       NOT NULL                COMMENT '所属分区 ID',
    `title`        VARCHAR(255) NOT NULL                COMMENT '帖子标题',
    `content`      MEDIUMTEXT   NOT NULL                COMMENT '帖子正文，支持 Markdown',
    `cover_url`    VARCHAR(512) NOT NULL DEFAULT ''     COMMENT '封面图 URL',
    `status`       TINYINT      NOT NULL DEFAULT 0      COMMENT '状态: 0=正常, 1=草稿, 2=待审核, 3=下架, 4=已删除',
    `view_count`   INT          NOT NULL DEFAULT 0      COMMENT '浏览次数',
    `like_count`   INT          NOT NULL DEFAULT 0      COMMENT '点赞数量（冗余字段，提升查询性能）',
    `comment_count` INT         NOT NULL DEFAULT 0      COMMENT '评论数量（冗余字段）',
    `favorite_count` INT        NOT NULL DEFAULT 0      COMMENT '收藏数量（冗余字段）',
    `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_section_id` (`section_id`),
    KEY `idx_status_created` (`status`, `created_at` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子表';

-- -------------------------------------------------------
-- 4. 评论表（Comment 模块）
--    支持一级评论 + 二级回复（parent_id 标识层级）
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS `comment` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '评论主键 ID',
    `post_id`      BIGINT       NOT NULL                COMMENT '所属帖子 ID',
    `user_id`      BIGINT       NOT NULL                COMMENT '评论者用户 ID',
    `parent_id`    BIGINT       NOT NULL DEFAULT 0      COMMENT '父评论 ID，0 表示一级评论',
    `root_id`      BIGINT       NOT NULL DEFAULT 0      COMMENT '根评论 ID，用于快速查询某楼的所有回复',
    `content`      TEXT         NOT NULL                COMMENT '评论内容',
    `status`       TINYINT      NOT NULL DEFAULT 0      COMMENT '状态: 0=正常, 1=已删除',
    `like_count`   INT          NOT NULL DEFAULT 0      COMMENT '点赞数量（冗余字段）',
    `reply_count`  INT          NOT NULL DEFAULT 0      COMMENT '回复数量（冗余字段）',
    `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',
    `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_post_id` (`post_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_root_id` (`root_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论表';

-- -------------------------------------------------------
-- 5. 点赞表（Like 模块）
--    同时支持帖子点赞和评论点赞；唯一约束防止重复点赞
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS `post_like` (
    `id`           BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `user_id`      BIGINT   NOT NULL                COMMENT '点赞用户 ID',
    `post_id`      BIGINT   NOT NULL                COMMENT '被点赞的帖子 ID',
    `created_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_post` (`user_id`, `post_id`),
    KEY `idx_post_id` (`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子点赞表（唯一约束防并发重复）';

CREATE TABLE IF NOT EXISTS `comment_like` (
    `id`           BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `user_id`      BIGINT   NOT NULL                COMMENT '点赞用户 ID',
    `comment_id`   BIGINT   NOT NULL                COMMENT '被点赞的评论 ID',
    `created_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_comment` (`user_id`, `comment_id`),
    KEY `idx_comment_id` (`comment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论点赞表（唯一约束防并发重复）';

-- -------------------------------------------------------
-- 6. 关注表（Follow 模块）
--    记录用户关注关系，follower 关注了 followee
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS `follow` (
    `id`           BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `follower_id`  BIGINT   NOT NULL                COMMENT '关注者用户 ID（我关注别人）',
    `followee_id`  BIGINT   NOT NULL                COMMENT '被关注者用户 ID（被我关注的人）',
    `created_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_follower_followee` (`follower_id`, `followee_id`),
    KEY `idx_followee_id` (`followee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='关注关系表';

-- -------------------------------------------------------
-- 7. 收藏表（Favorite 模块）
--    用户收藏帖子，唯一约束防重复收藏
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS `favorite` (
    `id`           BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `user_id`      BIGINT   NOT NULL                COMMENT '收藏者用户 ID',
    `post_id`      BIGINT   NOT NULL                COMMENT '被收藏的帖子 ID',
    `created_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_post` (`user_id`, `post_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏表';

-- -------------------------------------------------------
-- 8. 私信表（Message 模块）
--    会话（Conversation）模型：一条会话对应两个用户之间的所有消息
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS `conversation` (
    `id`              BIGINT   NOT NULL AUTO_INCREMENT COMMENT '会话主键 ID',
    `user1_id`        BIGINT   NOT NULL                COMMENT '用户1 ID（较小的那个，保证唯一性）',
    `user2_id`        BIGINT   NOT NULL                COMMENT '用户2 ID（较大的那个）',
    `last_message`    TEXT                             COMMENT '最近一条消息内容（冗余，用于列表预览）',
    `last_message_at` DATETIME                         COMMENT '最近消息时间',
    `user1_unread`    INT      NOT NULL DEFAULT 0      COMMENT '用户1 未读消息数',
    `user2_unread`    INT      NOT NULL DEFAULT 0      COMMENT '用户2 未读消息数',
    `created_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '会话创建时间',
    `updated_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user1_user2` (`user1_id`, `user2_id`),
    KEY `idx_user1_last` (`user1_id`, `last_message_at` DESC),
    KEY `idx_user2_last` (`user2_id`, `last_message_at` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='私信会话表（每对用户唯一一条）';

CREATE TABLE IF NOT EXISTS `message` (
    `id`              BIGINT   NOT NULL AUTO_INCREMENT COMMENT '消息主键 ID',
    `conversation_id` BIGINT   NOT NULL                COMMENT '所属会话 ID',
    `sender_id`       BIGINT   NOT NULL                COMMENT '发送者用户 ID',
    `receiver_id`     BIGINT   NOT NULL                COMMENT '接收者用户 ID',
    `content`         TEXT     NOT NULL                COMMENT '消息内容',
    `is_read`         TINYINT  NOT NULL DEFAULT 0      COMMENT '是否已读: 0=未读, 1=已读',
    `created_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    PRIMARY KEY (`id`),
    KEY `idx_conversation_id` (`conversation_id`, `created_at` DESC),
    KEY `idx_receiver_unread` (`receiver_id`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='私信消息表';

-- -------------------------------------------------------
-- 9. 通知表（Notification 模块）
--    统一存储各类通知：评论、点赞、关注、审核结果
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS `notification` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '通知主键 ID',
    `user_id`      BIGINT       NOT NULL                COMMENT '接收通知的用户 ID',
    `actor_id`     BIGINT       NOT NULL DEFAULT 0      COMMENT '触发通知的用户 ID（0 表示系统通知）',
    `type`         TINYINT      NOT NULL                COMMENT '通知类型: 1=评论, 2=点赞帖子, 3=点赞评论, 4=关注, 5=审核通过, 6=审核拒绝',
    `entity_type`  TINYINT      NOT NULL DEFAULT 0      COMMENT '关联实体类型: 0=无, 1=帖子, 2=评论',
    `entity_id`    BIGINT       NOT NULL DEFAULT 0      COMMENT '关联实体 ID',
    `content`      VARCHAR(512) NOT NULL DEFAULT ''     COMMENT '通知摘要内容',
    `is_read`      TINYINT      NOT NULL DEFAULT 0      COMMENT '是否已读: 0=未读, 1=已读',
    `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '通知创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_unread` (`user_id`, `is_read`, `created_at` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知表';

-- -------------------------------------------------------
-- 10. 举报表（Report 模块）
--     举报帖子或评论，记录举报原因与审核结果
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS `report` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '举报主键 ID',
    `reporter_id`  BIGINT       NOT NULL                COMMENT '举报者用户 ID',
    `entity_type`  TINYINT      NOT NULL                COMMENT '举报对象类型: 1=帖子, 2=评论',
    `entity_id`    BIGINT       NOT NULL                COMMENT '被举报的实体 ID',
    `reason`       TINYINT      NOT NULL                COMMENT '举报原因: 1=垃圾广告, 2=违规内容, 3=侮辱谩骂, 4=其他',
    `detail`       VARCHAR(512) NOT NULL DEFAULT ''     COMMENT '举报补充说明',
    `status`       TINYINT      NOT NULL DEFAULT 0      COMMENT '审核状态: 0=待审核, 1=已处理-违规, 2=已处理-无问题',
    `reviewer_id`  BIGINT                               COMMENT '审核管理员用户 ID',
    `review_note`  VARCHAR(512) NOT NULL DEFAULT ''     COMMENT '审核备注',
    `reviewed_at`  DATETIME                             COMMENT '审核时间',
    `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '举报时间',
    PRIMARY KEY (`id`),
    KEY `idx_entity` (`entity_type`, `entity_id`),
    KEY `idx_status` (`status`, `created_at` DESC),
    KEY `idx_reporter` (`reporter_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='举报表';

-- -------------------------------------------------------
-- 11. 处罚记录表（Punishment 模块）
--     记录禁言、封号的处罚历史，与 user.status 联动
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS `punishment` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '处罚主键 ID',
    `user_id`      BIGINT       NOT NULL                COMMENT '被处罚的用户 ID',
    `operator_id`  BIGINT       NOT NULL                COMMENT '执行处罚的管理员 ID',
    `type`         TINYINT      NOT NULL                COMMENT '处罚类型: 1=禁言, 2=封号',
    `reason`       VARCHAR(512) NOT NULL DEFAULT ''     COMMENT '处罚原因',
    `expire_at`    DATETIME                             COMMENT '处罚到期时间，NULL 表示永久',
    `is_active`    TINYINT      NOT NULL DEFAULT 1      COMMENT '是否生效: 1=生效中, 0=已解除',
    `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '处罚时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`, `is_active`),
    KEY `idx_expire` (`expire_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='处罚记录表';
