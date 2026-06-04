-- ============================================
-- 用户反馈模块 — 数据库迁移
-- ============================================

CREATE TABLE IF NOT EXISTS `feedback` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '反馈ID',
    `user_id`     BIGINT        NOT NULL COMMENT '提交用户ID',
    `type`        TINYINT       NOT NULL DEFAULT 1 COMMENT '反馈类型: 1=问题反馈, 2=功能建议',
    `title`       VARCHAR(200)  NOT NULL COMMENT '反馈标题',
    `content`     VARCHAR(2000) NOT NULL COMMENT '反馈内容',
    `contact`     VARCHAR(100)  DEFAULT NULL COMMENT '联系方式(邮箱/手机)',
    `images`      VARCHAR(2000) DEFAULT NULL COMMENT '截图URL列表(JSON数组)',
    `status`      TINYINT       NOT NULL DEFAULT 0 COMMENT '状态: 0=待处理, 1=处理中, 2=已解决, 3=已关闭',
    `admin_reply` VARCHAR(2000) DEFAULT NULL COMMENT '管理员回复内容',
    `admin_id`    BIGINT        DEFAULT NULL COMMENT '处理管理员ID',
    `handle_time` DATETIME      DEFAULT NULL COMMENT '处理时间',
    `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    `update_time` DATETIME      NULL     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户反馈表';
