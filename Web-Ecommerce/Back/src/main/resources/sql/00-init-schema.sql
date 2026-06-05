-- ============================================
-- Ecommerce Marketplace — Full Schema Init
-- Executed first by MySQL Docker entrypoint
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

CREATE TABLE IF NOT EXISTS `category` (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    name        VARCHAR(50) NOT NULL COMMENT '分类名称',
    parent_id   BIGINT               DEFAULT 0 COMMENT '父分类ID，0=顶级',
    sort_order  INT                  DEFAULT 0 COMMENT '排序',
    create_time DATETIME             DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

CREATE TABLE IF NOT EXISTS `product` (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '商品ID',
    name         VARCHAR(200)   NOT NULL COMMENT '商品名称',
    category_id  BIGINT                  DEFAULT NULL COMMENT '分类ID',
    price        DECIMAL(10,2)  NOT NULL COMMENT '价格',
    stock        INT                    DEFAULT 0 COMMENT '库存',
    description  VARCHAR(1000)          DEFAULT NULL COMMENT '简介',
    detail       MEDIUMTEXT             DEFAULT NULL COMMENT '详情(HTML)',
    main_image   VARCHAR(500)           DEFAULT NULL COMMENT '主图URL',
    images       VARCHAR(2000)          DEFAULT NULL COMMENT '图片列表(JSON)',
    status       TINYINT                DEFAULT 1 COMMENT '0=下架 1=上架',
    sales        INT                    DEFAULT 0 COMMENT '销量',
    avg_rating   DECIMAL(3,2)           DEFAULT 0.00 COMMENT '平均评分',
    review_count INT                    DEFAULT 0 COMMENT '评价数',
    listed_at    DATETIME               DEFAULT NULL COMMENT '上架时间',
    create_time  DATETIME               DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME               DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_category (category_id),
    KEY idx_status (status),
    KEY idx_sales (sales),
    KEY idx_rating (avg_rating),
    FULLTEXT KEY ft_name (name) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

CREATE TABLE IF NOT EXISTS `product_sku` (
    id         BIGINT         NOT NULL AUTO_INCREMENT COMMENT 'SKU ID',
    product_id BIGINT         NOT NULL COMMENT '商品ID',
    spec_name  VARCHAR(100)   NOT NULL COMMENT '规格名',
    spec_value VARCHAR(200)            DEFAULT '' COMMENT '规格值',
    price      DECIMAL(10,2)  NOT NULL COMMENT 'SKU价格',
    stock      INT                     DEFAULT 0 COMMENT 'SKU库存',
    sales      INT                     DEFAULT 0 COMMENT 'SKU销量',
    status     TINYINT                 DEFAULT 1 COMMENT '0=禁用 1=启用',
    image      VARCHAR(500)            DEFAULT '' COMMENT 'SKU图片',
    PRIMARY KEY (id),
    KEY idx_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品SKU表';

CREATE TABLE IF NOT EXISTS `order` (
    id                      BIGINT         NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    order_no                VARCHAR(32)    NOT NULL COMMENT '订单编号',
    user_id                 BIGINT         NOT NULL COMMENT '用户ID',
    address_id              BIGINT                  DEFAULT NULL COMMENT '收货地址ID',
    total_amount            DECIMAL(10,2)  NOT NULL COMMENT '订单总金额',
    coupon_ids              VARCHAR(500)            DEFAULT NULL COMMENT '使用优惠券ID列表(JSON)',
    coupon_discount         DECIMAL(10,2)           DEFAULT 0.00 COMMENT '优惠券抵扣金额',
    discount_amount         DECIMAL(10,2)           DEFAULT 0.00 COMMENT '折扣金额',
    pay_amount              DECIMAL(10,2)  NOT NULL COMMENT '实付金额',
    status                  TINYINT                 DEFAULT 0 COMMENT '0=待支付 1=待发货 2=待收货 3=已完成 4=已取消',
    remark                  VARCHAR(500)            DEFAULT NULL COMMENT '订单备注',
    refund_type             TINYINT                 DEFAULT NULL COMMENT '退款类型 1=仅退款 2=退货退款',
    refund_reason           VARCHAR(50)             DEFAULT NULL COMMENT '退款原因',
    refund_desc             VARCHAR(500)            DEFAULT NULL COMMENT '退款说明',
    refund_amount           DECIMAL(10,2)           DEFAULT NULL COMMENT '退款金额',
    refund_item_ids         VARCHAR(500)            DEFAULT NULL COMMENT '退款商品ID列表(JSON)',
    refund_status           TINYINT                 DEFAULT NULL COMMENT '退款状态 0=待审核 1=处理中 2=已拒绝 3=已完成 4=待退货 5=退货中 6=已撤销',
    refund_reject_reason    VARCHAR(500)            DEFAULT NULL COMMENT '拒绝原因',
    refund_apply_time       DATETIME                DEFAULT NULL COMMENT '退款申请时间',
    refund_deal_time        DATETIME                DEFAULT NULL COMMENT '退款处理时间',
    return_logistics_company VARCHAR(100)           DEFAULT NULL COMMENT '退货物流公司',
    return_logistics_no     VARCHAR(100)            DEFAULT NULL COMMENT '退货物流单号',
    pay_time                DATETIME                DEFAULT NULL COMMENT '支付时间',
    deal_time               DATETIME                DEFAULT NULL COMMENT '成交时间',
    address_modified        TINYINT                 DEFAULT 0 COMMENT '地址是否已修改',
    create_time             DATETIME                DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time             DATETIME                DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user (user_id),
    KEY idx_status (status),
    KEY idx_refund_status (refund_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

CREATE TABLE IF NOT EXISTS `order_item` (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '订单项ID',
    order_id     BIGINT         NOT NULL COMMENT '订单ID',
    product_id   BIGINT         NOT NULL COMMENT '商品ID',
    product_name VARCHAR(200)   NOT NULL COMMENT '商品名称(快照)',
    product_image VARCHAR(500)           DEFAULT NULL COMMENT '商品图片(快照)',
    sku_id       BIGINT                  DEFAULT NULL COMMENT 'SKU ID',
    spec_desc    VARCHAR(500)            DEFAULT NULL COMMENT '规格描述(快照)',
    quantity     INT            NOT NULL DEFAULT 1 COMMENT '数量',
    price        DECIMAL(10,2)  NOT NULL COMMENT '单价(快照)',
    PRIMARY KEY (id),
    KEY idx_order (order_id),
    KEY idx_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单项表';

CREATE TABLE IF NOT EXISTS `cart` (
    id          BIGINT NOT NULL AUTO_INCREMENT COMMENT '购物车ID',
    user_id     BIGINT NOT NULL COMMENT '用户ID',
    product_id  BIGINT NOT NULL COMMENT '商品ID',
    sku_id      BIGINT          DEFAULT NULL COMMENT 'SKU ID',
    quantity    INT    NOT NULL DEFAULT 1 COMMENT '数量',
    checked     TINYINT         DEFAULT 1 COMMENT '0=未选中 1=已选中',
    create_time DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_product_sku (user_id, product_id, sku_id),
    KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车表';

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

CREATE TABLE IF NOT EXISTS `review` (
    id               BIGINT         NOT NULL AUTO_INCREMENT COMMENT '评价ID',
    user_id          BIGINT         NOT NULL COMMENT '用户ID',
    username         VARCHAR(50)    NOT NULL COMMENT '用户名(快照)',
    avatar           VARCHAR(500)            DEFAULT NULL COMMENT '头像(快照)',
    product_id       BIGINT         NOT NULL COMMENT '商品ID',
    order_id         BIGINT                  DEFAULT NULL COMMENT '订单ID',
    rating           DECIMAL(3,2)   NOT NULL COMMENT '综合评分',
    rating_desc      DECIMAL(3,2)            DEFAULT NULL COMMENT '描述相符评分',
    rating_logistics DECIMAL(3,2)            DEFAULT NULL COMMENT '物流服务评分',
    rating_service   DECIMAL(3,2)            DEFAULT NULL COMMENT '服务态度评分',
    content          VARCHAR(1000)           DEFAULT NULL COMMENT '评价内容',
    images           VARCHAR(2000)           DEFAULT NULL COMMENT '评价图片(JSON)',
    is_followup      TINYINT                 DEFAULT 0 COMMENT '0=首次 1=追评',
    like_count       INT                     DEFAULT 0 COMMENT '点赞数',
    comment_count    INT                     DEFAULT 0 COMMENT '回复数',
    create_time      DATETIME                DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_product (product_id),
    KEY idx_user (user_id),
    KEY idx_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品评价表';

CREATE TABLE IF NOT EXISTS `review_comment` (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '回复ID',
    review_id   BIGINT       NOT NULL COMMENT '评价ID',
    user_id     BIGINT       NOT NULL COMMENT '用户ID',
    username    VARCHAR(50)  NOT NULL COMMENT '用户名(快照)',
    avatar      VARCHAR(500)          DEFAULT NULL COMMENT '头像(快照)',
    content     VARCHAR(500) NOT NULL COMMENT '回复内容',
    create_time DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_review (review_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价回复表';

CREATE TABLE IF NOT EXISTS `review_like` (
    id          BIGINT NOT NULL AUTO_INCREMENT COMMENT '点赞ID',
    user_id     BIGINT NOT NULL COMMENT '用户ID',
    review_id   BIGINT NOT NULL COMMENT '评价ID',
    create_time DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_review (user_id, review_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价点赞表';

CREATE TABLE IF NOT EXISTS `favorite` (
    id          BIGINT NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
    user_id     BIGINT NOT NULL COMMENT '用户ID',
    product_id  BIGINT NOT NULL COMMENT '商品ID',
    sku_id      BIGINT          DEFAULT NULL COMMENT 'SKU ID',
    create_time DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_product (user_id, product_id),
    KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏表';

CREATE TABLE IF NOT EXISTS `coupon` (
    id             BIGINT         NOT NULL AUTO_INCREMENT COMMENT '优惠券ID',
    name           VARCHAR(100)   NOT NULL COMMENT '优惠券名称',
    type           TINYINT        NOT NULL COMMENT '1=满减 2=折扣',
    discount       DECIMAL(10,2)  NOT NULL COMMENT '优惠金额/折扣率',
    min_amount     DECIMAL(10,2)           DEFAULT 0.00 COMMENT '最低使用金额',
    total_qty      INT            NOT NULL COMMENT '总数量',
    remain_qty     INT            NOT NULL COMMENT '剩余数量',
    start_time     DATETIME       NOT NULL COMMENT '使用开始时间',
    end_time       DATETIME       NOT NULL COMMENT '使用结束时间',
    grab_start_time DATETIME               DEFAULT NULL COMMENT '领取开始时间',
    grab_end_time  DATETIME               DEFAULT NULL COMMENT '领取结束时间',
    scope_type     TINYINT                 DEFAULT 1 COMMENT '1=全场 2=指定分类 3=指定商品',
    scope_ids      VARCHAR(1000)           DEFAULT NULL COMMENT '适用范围ID列表(JSON)',
    is_large       TINYINT                 DEFAULT 0 COMMENT '0=普通 1=大额',
    stackable      TINYINT                 DEFAULT 0 COMMENT '0=不可叠加 1=可叠加',
    status         TINYINT                 DEFAULT 1 COMMENT '0=禁用 1=启用',
    create_time    DATETIME                DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_status_time (status, start_time, end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券表';

CREATE TABLE IF NOT EXISTS `user_coupon` (
    id          BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户优惠券ID',
    user_id     BIGINT NOT NULL COMMENT '用户ID',
    coupon_id   BIGINT NOT NULL COMMENT '优惠券ID',
    status      TINYINT         DEFAULT 0 COMMENT '0=未使用 1=已使用 2=已过期',
    used_time   DATETIME        DEFAULT NULL COMMENT '使用时间',
    use_order_id BIGINT         DEFAULT NULL COMMENT '使用的订单ID',
    create_time DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_coupon (user_id, coupon_id),
    KEY idx_user_status (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户优惠券表';

CREATE TABLE IF NOT EXISTS `payment_session` (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '支付会话ID',
    order_id    BIGINT       NOT NULL COMMENT '订单ID',
    pay_method  VARCHAR(20)  NOT NULL COMMENT '支付方式 wechat/alipay/card',
    qr_token    VARCHAR(64)  NOT NULL COMMENT 'QR Token(UUID)',
    qr_scanned  TINYINT               DEFAULT 0 COMMENT '0=未扫码 1=已扫码',
    scan_time   DATETIME              DEFAULT NULL COMMENT '扫码时间',
    status      VARCHAR(20)  NOT NULL DEFAULT 'WAITING_SCAN' COMMENT 'WAITING_SCAN/SCANNED/PAID',
    create_time DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order (order_id),
    UNIQUE KEY uk_qr_token (qr_token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付会话表';

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
