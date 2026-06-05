-- ============================================
CREATE TABLE IF NOT EXISTS `user` (
    id                   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    account_id           BIGINT                DEFAULT NULL COMMENT '关联账号ID',
    username             VARCHAR(50)  NOT NULL COMMENT '用户名',
    password             VARCHAR(255) NOT NULL COMMENT '密码(BCrypt)',
    email                VARCHAR(100)          DEFAULT NULL COMMENT '邮箱',
    avatar               VARCHAR(500)          DEFAULT NULL COMMENT '头像URL',
    phone                VARCHAR(20)           DEFAULT NULL COMMENT '手机号',
    gender               TINYINT               DEFAULT 0 COMMENT '0=未知 1=男 2=女',
    intro                VARCHAR(500)          DEFAULT NULL COMMENT '简介',
    username_update_time DATETIME              DEFAULT NULL COMMENT '用户名最后修改时间',
    status               TINYINT               DEFAULT 1 COMMENT '0=禁用 1=正常',
    create_time          DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time          DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    KEY idx_email (email),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE IF NOT EXISTS `admin` (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '管理员ID',
    username    VARCHAR(50)  NOT NULL COMMENT '用户名',
    password    VARCHAR(255) NOT NULL COMMENT '密码(BCrypt)',
    role        VARCHAR(50)           DEFAULT 'admin' COMMENT '角色',
    status      TINYINT               DEFAULT 1 COMMENT '0=禁用 1=正常',
    create_time DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员表';

CREATE TABLE IF NOT EXISTS `address` (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '地址ID',
    user_id     BIGINT       NOT NULL COMMENT '用户ID',
    name        VARCHAR(50)  NOT NULL COMMENT '收件人',
    phone       VARCHAR(20)  NOT NULL COMMENT '联系电话',
    province    VARCHAR(50)  NOT NULL COMMENT '省',
    city        VARCHAR(50)  NOT NULL COMMENT '市',
    district    VARCHAR(50)  NOT NULL COMMENT '区/县',
    detail      VARCHAR(200) NOT NULL COMMENT '详细地址',
    is_default  TINYINT               DEFAULT 0 COMMENT '0=非默认 1=默认地址',
    create_time DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收货地址表';

CREATE TABLE IF NOT EXISTS `banner` (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'Banner ID',
    title       VARCHAR(100) NOT NULL COMMENT '标题',
    image_url   VARCHAR(500) NOT NULL COMMENT '图片URL',
    link_url    VARCHAR(500)          DEFAULT NULL COMMENT '跳转链接',
    sort_order  INT                   DEFAULT 0 COMMENT '排序',
    status      TINYINT               DEFAULT 1 COMMENT '0=禁用 1=启用',
    create_time DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='轮播图表';

CREATE TABLE IF NOT EXISTS `announcement` (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '公告ID',
    title       VARCHAR(200)  NOT NULL COMMENT '标题',
    content     TEXT          NOT NULL COMMENT '内容',
    status      TINYINT                DEFAULT 1 COMMENT '0=draft 1=published 2=archived',
    sort_order  INT                    DEFAULT 0 COMMENT '排序',
    level       VARCHAR(20)            DEFAULT 'info' COMMENT 'info/warning/important',
    create_time DATETIME               DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME               DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告表';

CREATE TABLE IF NOT EXISTS `conversation` (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '会话ID',
    user_id       BIGINT       NOT NULL COMMENT '用户ID',
    username      VARCHAR(50)  NOT NULL COMMENT '用户名(快照)',
    avatar        VARCHAR(500)          DEFAULT NULL COMMENT '头像(快照)',
    subject       VARCHAR(200) NOT NULL COMMENT '会话主题',
    source_type   TINYINT               DEFAULT NULL COMMENT '来源类型',
    source_id     BIGINT                DEFAULT NULL COMMENT '来源ID',
    source_name   VARCHAR(200)          DEFAULT NULL COMMENT '来源名称',
    status        TINYINT               DEFAULT 0 COMMENT '0=进行中 1=已关闭',
    unread_count  INT                   DEFAULT 0 COMMENT '未读消息数',
    user_unread   INT                   DEFAULT 0 COMMENT '用户未读数',
    last_message  VARCHAR(500)          DEFAULT NULL COMMENT '最后消息摘要',
    last_active   DATETIME              DEFAULT NULL COMMENT '最后活跃时间',
    create_time   DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    close_time    DATETIME              DEFAULT NULL COMMENT '关闭时间',
    PRIMARY KEY (id),
    KEY idx_user (user_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客服会话表';

CREATE TABLE IF NOT EXISTS `chat_message` (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    conversation_id BIGINT     NOT NULL COMMENT '会话ID',
    sender_id     BIGINT       NOT NULL COMMENT '发送者ID',
    sender_role   VARCHAR(20)  NOT NULL COMMENT 'USER/ADMIN',
    content       TEXT         NOT NULL COMMENT '消息内容',
    content_type  VARCHAR(20)           DEFAULT 'TEXT' COMMENT 'TEXT/IMAGE/PRODUCT',
    extra_data    TEXT                  DEFAULT NULL COMMENT '扩展数据(JSON)',
    create_time   DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    PRIMARY KEY (id),
    KEY idx_conversation (conversation_id),
    KEY idx_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客服消息表';

CREATE TABLE IF NOT EXISTS `quick_reply` (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '快捷回复ID',
    title       VARCHAR(100) NOT NULL COMMENT '标题',
    content     VARCHAR(500) NOT NULL COMMENT '回复内容',
    sort_order  INT                   DEFAULT 0 COMMENT '排序',
    status      TINYINT               DEFAULT 1 COMMENT '0=禁用 1=启用',
    create_time DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='快捷回复表';
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
-- Announcement table migration: add status, sort_order, level
-- Uses conditional ALTER to be safe on re-run / Docker fresh deploy

DROP PROCEDURE IF EXISTS migrate_announcement;
DELIMITER //
CREATE PROCEDURE migrate_announcement()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = 'ecommerce' AND TABLE_NAME = 'announcement' AND COLUMN_NAME = 'status') THEN
        ALTER TABLE announcement ADD COLUMN status TINYINT DEFAULT 1 COMMENT '0=draft, 1=published, 2=archived' AFTER content;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = 'ecommerce' AND TABLE_NAME = 'announcement' AND COLUMN_NAME = 'sort_order') THEN
        ALTER TABLE announcement ADD COLUMN sort_order INT DEFAULT 0 COMMENT 'sort order, higher = first' AFTER status;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = 'ecommerce' AND TABLE_NAME = 'announcement' AND COLUMN_NAME = 'level') THEN
        ALTER TABLE announcement ADD COLUMN level VARCHAR(20) DEFAULT 'info' COMMENT 'info / warning / important' AFTER sort_order;
    END IF;
END //
DELIMITER ;
CALL migrate_announcement();
DROP PROCEDURE IF EXISTS migrate_announcement;

-- Update seed data
UPDATE announcement SET status = 1, sort_order = 0, level = 'info' WHERE status IS NULL;
-- Chat message migration: add extra_data column
-- Uses conditional ALTER to be safe on re-run / Docker fresh deploy

DROP PROCEDURE IF EXISTS migrate_chat_message;
DELIMITER //
CREATE PROCEDURE migrate_chat_message()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = 'ecommerce' AND TABLE_NAME = 'chat_message' AND COLUMN_NAME = 'extra_data') THEN
        ALTER TABLE chat_message ADD COLUMN extra_data TEXT NULL COMMENT 'Extended data (JSON) for product card etc.' AFTER content_type;
    END IF;
END //
DELIMITER ;
CALL migrate_chat_message();
DROP PROCEDURE IF EXISTS migrate_chat_message;
