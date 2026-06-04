-- ============================================
-- Ecommerce Marketplace — 数据库初始化脚本
-- 用法: mysql -u root -p < init.sql
-- ============================================

CREATE DATABASE IF NOT EXISTS ecommerce DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE ecommerce;

-- ============================================
-- 1. 用户表
-- ============================================
CREATE TABLE IF NOT EXISTS `user` (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  account_id  BIGINT       NOT NULL UNIQUE COMMENT '8位系统账号ID(YYMMDDNN)',
  username    VARCHAR(50)  NOT NULL UNIQUE,
  password    VARCHAR(255) NOT NULL,
  email       VARCHAR(100) NOT NULL,
  avatar      VARCHAR(500) DEFAULT '',
  phone       VARCHAR(20)  DEFAULT '',
  status      TINYINT      DEFAULT 1 COMMENT '1=启用 0=禁用',
  create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 2. 管理员表
-- ============================================
CREATE TABLE IF NOT EXISTS `admin` (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  username    VARCHAR(50)  NOT NULL UNIQUE,
  password    VARCHAR(255) NOT NULL,
  role        VARCHAR(20)  DEFAULT 'ADMIN',
  status      TINYINT      DEFAULT 1,
  create_time DATETIME     DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 3. 商品分类表
-- ============================================
CREATE TABLE IF NOT EXISTS `category` (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  name        VARCHAR(50)  NOT NULL,
  parent_id   BIGINT       DEFAULT 0 COMMENT '0=一级分类',
  sort_order  INT          DEFAULT 0,
  create_time DATETIME     DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 4. 商品表
-- ============================================
CREATE TABLE IF NOT EXISTS `product` (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  name        VARCHAR(200)   NOT NULL,
  category_id BIGINT         NOT NULL,
  price       DECIMAL(10,2)  NOT NULL,
  stock       INT            DEFAULT 0,
  description TEXT,
  detail      TEXT COMMENT '商品详细参数（HTML）',
  main_image  VARCHAR(500),
  images      VARCHAR(2000)  DEFAULT '' COMMENT 'JSON array of URLs',
  status      TINYINT        DEFAULT 1 COMMENT '1=上架 0=下架',
  sales       INT            DEFAULT 0,
  avg_rating  DECIMAL(2,1)   DEFAULT 0 COMMENT '平均评分 0.0~5.0',
  review_count INT           DEFAULT 0 COMMENT '评价总数',
  listed_at   DATETIME       DEFAULT NULL COMMENT '首次上架时间',
  create_time DATETIME       DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 全文索引：商品名称模糊搜索（ngram 解析器，支持中文）
ALTER TABLE `product` ADD FULLTEXT INDEX ft_product_name (name) WITH PARSER ngram;

-- ============================================
-- 5. 商品 SKU 表
-- ============================================
CREATE TABLE IF NOT EXISTS `product_sku` (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  product_id  BIGINT         NOT NULL,
  spec_name   VARCHAR(255)   NOT NULL,
  spec_value  VARCHAR(255)   DEFAULT '',
  price       DECIMAL(10,2)  NOT NULL,
  stock       INT            DEFAULT 0,
  sales       INT            DEFAULT 0 COMMENT 'SKU销量',
  status      TINYINT        DEFAULT 1 COMMENT '1=上架 0=下架',
  image       VARCHAR(255)   DEFAULT NULL COMMENT '规格缩略图'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 6. 购物车表
-- ============================================
CREATE TABLE IF NOT EXISTS `cart` (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id     BIGINT  NOT NULL,
  product_id  BIGINT  NOT NULL,
  sku_id      BIGINT  DEFAULT 0,
  quantity    INT     DEFAULT 1,
  checked     TINYINT DEFAULT 1 COMMENT '1=选中 0=未选',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_product_sku (user_id, product_id, sku_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 7. 订单表
-- ============================================
CREATE TABLE IF NOT EXISTS `order` (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_no      VARCHAR(32)    NOT NULL UNIQUE,
  user_id       BIGINT         NOT NULL,
  address_id    BIGINT         NOT NULL,
  total_amount  DECIMAL(10,2)  NOT NULL COMMENT '商品原价合计',
  coupon_id     BIGINT         DEFAULT NULL COMMENT '使用的user_coupon.id',
  coupon_discount DECIMAL(10,2) DEFAULT 0.00 COMMENT '优惠券抵扣金额',
  discount_amount DECIMAL(10,2) DEFAULT 0.00 COMMENT '商品级折扣金额',
  pay_amount    DECIMAL(10,2)  DEFAULT 0.00 COMMENT '实付金额',
  status        TINYINT        DEFAULT 0 COMMENT '0=待支付 1=待发货 2=待收货 3=已完成 4=已取消 5=退款中',
  remark        VARCHAR(500)   DEFAULT '',
  pay_time      DATETIME       NULL,
  deal_time     DATETIME       NULL COMMENT '成交时间（确认收货时写入）',
  address_modified TINYINT     DEFAULT 0 COMMENT '0=未修改 1=已修改',
  create_time   DATETIME       DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 8. 订单明细表
-- ============================================
CREATE TABLE IF NOT EXISTS `order_item` (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id      BIGINT         NOT NULL,
  product_id    BIGINT         NOT NULL,
  product_name  VARCHAR(200)   NOT NULL,
  product_image VARCHAR(500)   DEFAULT '',
  sku_id        BIGINT         DEFAULT 0,
  spec_desc     VARCHAR(100)   DEFAULT '',
  quantity      INT            NOT NULL,
  price         DECIMAL(10,2)  NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 8-b. 支付会话表
-- ============================================
CREATE TABLE IF NOT EXISTS `payment_session` (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id    BIGINT       NOT NULL UNIQUE,
  pay_method  VARCHAR(20)  NOT NULL COMMENT 'wechat/alipay/card',
  qr_token    VARCHAR(64)  NOT NULL UNIQUE,
  qr_scanned  TINYINT      DEFAULT 0 COMMENT '0=未扫码 1=已扫码',
  scan_time   DATETIME     NULL,
  status      VARCHAR(20)  DEFAULT 'WAITING_SCAN' COMMENT 'WAITING_SCAN/SCANNED/PAID',
  create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 9. 收货地址表
-- ============================================
CREATE TABLE IF NOT EXISTS `address` (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id     BIGINT       NOT NULL,
  name        VARCHAR(50)  NOT NULL,
  phone       VARCHAR(20)  NOT NULL,
  province    VARCHAR(50)  NOT NULL,
  city        VARCHAR(50)  NOT NULL,
  district    VARCHAR(50)  NOT NULL,
  detail      VARCHAR(200) NOT NULL,
  is_default  TINYINT      DEFAULT 0,
  create_time DATETIME     DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 10. 商品评价表
-- ============================================
CREATE TABLE IF NOT EXISTS `review` (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id     BIGINT         NOT NULL,
  username    VARCHAR(50)    NOT NULL,
  avatar      VARCHAR(500)   DEFAULT '',
  product_id  BIGINT         NOT NULL,
  order_id    BIGINT         NOT NULL,
  rating             DECIMAL(2,1)   NOT NULL COMMENT '综合评分 0.0~5.0（三维度平均）',
  rating_desc        DECIMAL(2,1)   DEFAULT NULL COMMENT '描述相符 0.5~5.0',
  rating_logistics   DECIMAL(2,1)   DEFAULT NULL COMMENT '物流服务 0.5~5.0',
  rating_service     DECIMAL(2,1)   DEFAULT NULL COMMENT '服务态度 0.5~5.0',
  content     TEXT,
  images      VARCHAR(2000)  DEFAULT '',
  is_followup TINYINT        NOT NULL DEFAULT 0 COMMENT '0=initial, 1=followup',
  like_count  INT            DEFAULT 0 COMMENT '点赞数',
  comment_count INT          DEFAULT 0 COMMENT '评论数',
  create_time DATETIME       DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_product_id (product_id),
  INDEX idx_user_id (user_id),
  UNIQUE KEY uk_order_product_user_type (order_id, product_id, user_id, is_followup)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 11. 商品收藏表
-- ============================================
CREATE TABLE IF NOT EXISTS `favorite` (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id     BIGINT   NOT NULL,
  product_id  BIGINT   NOT NULL,
  sku_id      BIGINT   DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_product (user_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 11-b. 评价点赞表
-- ============================================
CREATE TABLE IF NOT EXISTS `review_like` (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id     BIGINT   NOT NULL,
  review_id   BIGINT   NOT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_review (user_id, review_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 11-c. 评价评论表
-- ============================================
CREATE TABLE IF NOT EXISTS `review_comment` (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  review_id   BIGINT       NOT NULL,
  user_id     BIGINT       NOT NULL,
  username    VARCHAR(50)  NOT NULL,
  avatar      VARCHAR(500) DEFAULT '',
  content     TEXT         NOT NULL,
  create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_review_id (review_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 12. 轮播图表
-- ============================================
CREATE TABLE IF NOT EXISTS `banner` (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  title       VARCHAR(100) NOT NULL,
  image_url   VARCHAR(500) NOT NULL,
  link_url    VARCHAR(500) DEFAULT '',
  sort_order  INT          DEFAULT 0,
  status      INT          DEFAULT 1,
  create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 13. 公告表
-- ============================================
CREATE TABLE IF NOT EXISTS `announcement` (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  title       VARCHAR(200) NOT NULL,
  content     TEXT         NOT NULL,
  status      TINYINT      DEFAULT 1 COMMENT '0=draft, 1=published, 2=archived',
  sort_order  INT          DEFAULT 0 COMMENT 'sort order, higher = first',
  level       VARCHAR(20)  DEFAULT 'info' COMMENT 'info / warning / important',
  create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 14. 优惠券表
-- ============================================
CREATE TABLE IF NOT EXISTS `coupon` (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  name        VARCHAR(100)   NOT NULL COMMENT '券名称',
  type        TINYINT        NOT NULL COMMENT '1=满减券 2=折扣券 3=免邮券',
  discount    DECIMAL(10,2)  NOT NULL COMMENT '优惠金额/折扣率',
  min_amount  DECIMAL(10,2)  DEFAULT 0 COMMENT '最低消费门槛',
  total_qty   INT            DEFAULT 0 COMMENT '发行总量',
  remain_qty  INT            DEFAULT 0 COMMENT '剩余数量',
  start_time  DATETIME       NOT NULL COMMENT '有效期开始',
  end_time    DATETIME       NOT NULL COMMENT '有效期结束',
  grab_start_time DATETIME   DEFAULT NULL COMMENT '抢购开始时间(NULL=无抢购限制)',
  grab_end_time   DATETIME   DEFAULT NULL COMMENT '抢购结束时间',
  scope_type  TINYINT        DEFAULT 1 COMMENT '适用范围: 1=通用 2=指定分类 3=指定商品',
  scope_ids   VARCHAR(500)   DEFAULT '' COMMENT '适用范围ID列表(JSON数组)',
  is_large    TINYINT        DEFAULT 0 COMMENT '是否大额券: 0=小额 1=大额(有抢购时间)',
  status      TINYINT        DEFAULT 1 COMMENT '1=启用 0=停用',
  create_time DATETIME       DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 15. 用户优惠券表
-- ============================================
CREATE TABLE IF NOT EXISTS `user_coupon` (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id     BIGINT   NOT NULL,
  coupon_id   BIGINT   NOT NULL,
  status      TINYINT  DEFAULT 0 COMMENT '0=未使用 1=已使用 2=已过期',
  used_time   DATETIME DEFAULT NULL,
  use_order_id BIGINT  DEFAULT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_coupon (user_id, coupon_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 16. 热门关键词表
-- ============================================
CREATE TABLE IF NOT EXISTS `hot_keyword` (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  keyword     VARCHAR(100) NOT NULL COMMENT '关键词',
  search_count INT         DEFAULT 0 COMMENT '搜索次数',
  is_manual   TINYINT     DEFAULT 0 COMMENT '是否人工添加: 0=自动 1=人工',
  is_pinned   TINYINT     DEFAULT 0 COMMENT '是否置顶: 0=否 1=是',
  sort_order  INT         DEFAULT 0 COMMENT '排序',
  status      TINYINT     DEFAULT 1 COMMENT '状态: 1=启用 0=停用',
  create_time DATETIME    DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='热门关键词表';

-- ============================================
-- 种子数据
-- ============================================

-- 管理员（密码: admin123，BCrypt 加密）
INSERT INTO `admin` (username, password, role, status) VALUES
('admin', '$2b$12$Ep/yDhIzfHNsC2Bt0GiJNOul.Gi1O6tkimFmmcE9PLUMHp9ek9w0C', 'SUPER_ADMIN', 1);

-- 商品分类
INSERT INTO `category` (id, name, parent_id, sort_order) VALUES
(1, '手机数码', 0, 1),
(2, '电脑办公', 0, 2),
(3, '家用电器', 0, 3),
(4, '服饰鞋包', 0, 4),
(5, '食品生鲜', 0, 5),
(6, '手机', 1, 1),
(7, '平板电脑', 1, 2),
(8, '耳机/音箱', 1, 3),
(9, '笔记本电脑', 2, 1),
(10, '台式机', 2, 2),
(11, '电脑配件', 2, 3);

-- 示例商品
INSERT INTO `product` (name, category_id, price, stock, description, detail, main_image, images, status, sales) VALUES
('iPhone 15 Pro Max 256GB', 6, 9999.00, 100, 'Apple iPhone 15 Pro Max，搭载 A17 Pro 芯片，钛金属设计，4800 万像素主摄。',
 '<table class=\"param-table\"><tr><td>品牌</td><td>Apple</td></tr><tr><td>型号</td><td>iPhone 15 Pro Max</td></tr><tr><td>处理器</td><td>A17 Pro 芯片</td></tr><tr><td>屏幕尺寸</td><td>6.7 英寸</td></tr><tr><td>屏幕类型</td><td>Super Retina XDR OLED</td></tr><tr><td>分辨率</td><td>2796×1290 像素</td></tr><tr><td>运行内存</td><td>8GB</td></tr><tr><td>存储容量</td><td>256GB</td></tr><tr><td>后置摄像头</td><td>4800万主摄 + 1200万超广角 + 1200万长焦</td></tr><tr><td>前置摄像头</td><td>1200万像素</td></tr><tr><td>电池容量</td><td>4422mAh</td></tr><tr><td>充电接口</td><td>USB-C</td></tr><tr><td>机身重量</td><td>221g</td></tr><tr><td>操作系统</td><td>iOS 17</td></tr></table>',
 '', '', 1, 256),
('Samsung Galaxy S24 Ultra', 6, 8999.00, 80, '三星 Galaxy S24 Ultra，搭载骁龙 8 Gen 3，钛金属框架，2 亿像素摄像头。',
 '<table class=\"param-table\"><tr><td>品牌</td><td>Samsung</td></tr><tr><td>型号</td><td>Galaxy S24 Ultra</td></tr><tr><td>处理器</td><td>骁龙 8 Gen 3 for Galaxy</td></tr><tr><td>屏幕尺寸</td><td>6.8 英寸</td></tr><tr><td>屏幕类型</td><td>Dynamic AMOLED 2X</td></tr><tr><td>分辨率</td><td>3120×1440 像素</td></tr><tr><td>运行内存</td><td>12GB</td></tr><tr><td>存储容量</td><td>256GB</td></tr><tr><td>后置摄像头</td><td>2亿主摄 + 5000万长焦 + 1200万超广角 + 1000万长焦</td></tr><tr><td>前置摄像头</td><td>1200万像素</td></tr><tr><td>电池容量</td><td>5000mAh</td></tr><tr><td>机身重量</td><td>232g</td></tr><tr><td>操作系统</td><td>One UI 6.1 (Android 14)</td></tr></table>',
 '', '', 1, 189),
('Xiaomi 14 Pro', 6, 4999.00, 150, '小米 14 Pro，搭载骁龙 8 Gen 3，徕卡光学镜头，120W 秒充。',
 '<table class=\"param-table\"><tr><td>品牌</td><td>Xiaomi</td></tr><tr><td>型号</td><td>14 Pro</td></tr><tr><td>处理器</td><td>骁龙 8 Gen 3</td></tr><tr><td>屏幕尺寸</td><td>6.73 英寸</td></tr><tr><td>屏幕类型</td><td>AMOLED LTPO</td></tr><tr><td>分辨率</td><td>3200×1440 像素</td></tr><tr><td>运行内存</td><td>12GB</td></tr><tr><td>存储容量</td><td>256GB</td></tr><tr><td>后置摄像头</td><td>5000万徕卡主摄 + 5000万超广角 + 5000万长焦</td></tr><tr><td>电池容量</td><td>4880mAh</td></tr><tr><td>充电</td><td>120W有线 + 50W无线</td></tr><tr><td>机身重量</td><td>223g</td></tr><tr><td>操作系统</td><td>HyperOS (Android 14)</td></tr></table>',
 '', '', 1, 312),
('iPad Pro M4 11英寸', 7, 6799.00, 60, 'Apple iPad Pro M4 芯片，Ultra Retina XDR 显示屏，轻薄设计。',
 '<table class=\"param-table\"><tr><td>品牌</td><td>Apple</td></tr><tr><td>型号</td><td>iPad Pro M4 11英寸</td></tr><tr><td>处理器</td><td>Apple M4 芯片</td></tr><tr><td>屏幕尺寸</td><td>11 英寸</td></tr><tr><td>屏幕类型</td><td>Ultra Retina XDR</td></tr><tr><td>分辨率</td><td>2420×1668 像素</td></tr><tr><td>存储容量</td><td>256GB</td></tr><tr><td>后置摄像头</td><td>1200万广角 + 1000万超广角</td></tr><tr><td>前置摄像头</td><td>1200万超广角</td></tr><tr><td>电池续航</td><td>最长10小时</td></tr><tr><td>机身重量</td><td>444g (Wi-Fi)</td></tr><tr><td>接口</td><td>USB-C (雷雳 3)</td></tr><tr><td>操作系统</td><td>iPadOS 17</td></tr></table>',
 '', '', 1, 98),
('Huawei MatePad Pro', 7, 4299.00, 45, '华为 MatePad Pro 13.2英寸，OLED 柔性屏，鸿蒙系统。',
 '<table class=\"param-table\"><tr><td>品牌</td><td>Huawei</td></tr><tr><td>型号</td><td>MatePad Pro 13.2</td></tr><tr><td>处理器</td><td>麒麟 9000S</td></tr><tr><td>屏幕尺寸</td><td>13.2 英寸</td></tr><tr><td>屏幕类型</td><td>OLED 柔性屏</td></tr><tr><td>分辨率</td><td>2880×1920 像素</td></tr><tr><td>运行内存</td><td>12GB</td></tr><tr><td>存储容量</td><td>256GB</td></tr><tr><td>后置摄像头</td><td>1300万主摄 + 800万超广角</td></tr><tr><td>电池容量</td><td>10050mAh</td></tr><tr><td>充电</td><td>88W 有线快充</td></tr><tr><td>机身重量</td><td>580g</td></tr><tr><td>操作系统</td><td>HarmonyOS 4</td></tr></table>',
 '', '', 1, 67),
('AirPods Pro 2', 8, 1899.00, 200, 'Apple AirPods Pro 第二代，自适应降噪，个性化空间音频。',
 '<table class=\"param-table\"><tr><td>品牌</td><td>Apple</td></tr><tr><td>型号</td><td>AirPods Pro 2</td></tr><tr><td>芯片</td><td>Apple H2</td></tr><tr><td>降噪</td><td>自适应降噪</td></tr><tr><td>音频技术</td><td>个性化空间音频 + 动态头部追踪</td></tr><tr><td>防水等级</td><td>IPX4 (耳机及充电盒)</td></tr><tr><td>电池续航</td><td>单次6小时 / 配合充电盒30小时</td></tr><tr><td>充电接口</td><td>USB-C / MagSafe / Apple Watch充电器</td></tr><tr><td>重量</td><td>单只5.3g</td></tr></table>',
 '', '', 1, 520),
('Sony WH-1000XM5', 8, 2499.00, 70, '索尼头戴式降噪耳机，行业领先降噪，30 小时续航。',
 '<table class=\"param-table\"><tr><td>品牌</td><td>Sony</td></tr><tr><td>型号</td><td>WH-1000XM5</td></tr><tr><td>驱动单元</td><td>30mm 驱动单元</td></tr><tr><td>降噪</td><td>双芯片降噪 (QN1 + V1)</td></tr><tr><td>音频编码</td><td>LDAC / AAC / SBC</td></tr><tr><td>蓝牙版本</td><td>蓝牙 5.2</td></tr><tr><td>电池续航</td><td>30 小时 (ANC开) / 40 小时 (ANC关)</td></tr><tr><td>充电</td><td>USB-C 快充 (3分钟≈3小时)</td></tr><tr><td>重量</td><td>约 250g</td></tr><tr><td>接口</td><td>3.5mm 耳机孔 / USB-C</td></tr></table>',
 '', '', 1, 234),
('MacBook Pro 14 M3 Pro', 9, 12999.00, 40, 'Apple MacBook Pro 14英寸，M3 Pro 芯片，Liquid Retina XDR 显示屏。',
 '<table class=\"param-table\"><tr><td>品牌</td><td>Apple</td></tr><tr><td>型号</td><td>MacBook Pro 14 (M3 Pro)</td></tr><tr><td>处理器</td><td>Apple M3 Pro (11核CPU / 14核GPU)</td></tr><tr><td>屏幕尺寸</td><td>14.2 英寸</td></tr><tr><td>屏幕类型</td><td>Liquid Retina XDR</td></tr><tr><td>分辨率</td><td>3024×1964 像素</td></tr><tr><td>运行内存</td><td>18GB 统一内存</td></tr><tr><td>存储容量</td><td>512GB SSD</td></tr><tr><td>接口</td><td>雷雳4×2 / HDMI / SDXC / MagSafe 3</td></tr><tr><td>电池续航</td><td>最长17小时</td></tr><tr><td>机身重量</td><td>1.55kg</td></tr><tr><td>操作系统</td><td>macOS Sonoma</td></tr></table>',
 '', '', 1, 156),
('ThinkPad X1 Carbon', 9, 8999.00, 35, '联想 ThinkPad X1 Carbon Gen 12，商务旗舰，14英寸 2.8K OLED。',
 '<table class=\"param-table\"><tr><td>品牌</td><td>Lenovo</td></tr><tr><td>型号</td><td>ThinkPad X1 Carbon Gen 12</td></tr><tr><td>处理器</td><td>Intel Core Ultra 7 155H</td></tr><tr><td>屏幕尺寸</td><td>14 英寸</td></tr><tr><td>屏幕类型</td><td>2.8K OLED</td></tr><tr><td>分辨率</td><td>2880×1800 像素</td></tr><tr><td>运行内存</td><td>32GB LPDDR5x</td></tr><tr><td>存储容量</td><td>1TB SSD</td></tr><tr><td>接口</td><td>Thunderbolt 4×2 / USB-A×2 / HDMI 2.1</td></tr><tr><td>电池续航</td><td>最长14小时</td></tr><tr><td>机身重量</td><td>1.09kg</td></tr><tr><td>操作系统</td><td>Windows 11 Pro</td></tr></table>',
 '', '', 1, 89),
('Dell XPS 15', 9, 10999.00, 25, '戴尔 XPS 15，i9-13900H，RTX 4070，3.5K OLED 触控屏。',
 '<table class=\"param-table\"><tr><td>品牌</td><td>Dell</td></tr><tr><td>型号</td><td>XPS 15 9530</td></tr><tr><td>处理器</td><td>Intel Core i9-13900H</td></tr><tr><td>显卡</td><td>NVIDIA GeForce RTX 4070</td></tr><tr><td>屏幕尺寸</td><td>15.6 英寸</td></tr><tr><td>屏幕类型</td><td>3.5K OLED 触控屏</td></tr><tr><td>分辨率</td><td>3456×2160 像素</td></tr><tr><td>运行内存</td><td>32GB DDR5</td></tr><tr><td>存储容量</td><td>1TB SSD</td></tr><tr><td>接口</td><td>Thunderbolt 4×2 / USB-C / SD卡槽</td></tr><tr><td>电池容量</td><td>86Whr</td></tr><tr><td>机身重量</td><td>1.92kg</td></tr><tr><td>操作系统</td><td>Windows 11 Home</td></tr></table>',
 '', '', 1, 45),
('Kingston DDR5 32GB', 11, 799.00, 300, '金士顿 Fury DDR5 5600MHz 32GB 台式机内存条。',
 '<table class=\"param-table\"><tr><td>品牌</td><td>Kingston</td></tr><tr><td>型号</td><td>Fury Beast DDR5</td></tr><tr><td>内存类型</td><td>DDR5</td></tr><tr><td>容量</td><td>32GB (16GB×2)</td></tr><tr><td>频率</td><td>5600MHz</td></tr><tr><td>时序</td><td>CL40-40-40</td></tr><tr><td>工作电压</td><td>1.25V</td></tr><tr><td>散热</td><td>铝制散热马甲</td></tr><tr><td>兼容平台</td><td>Intel 700 / AMD AM5系列</td></tr><tr><td>质保</td><td>终身质保</td></tr></table>',
 '', '', 1, 678),
('Samsung 990 Pro 2TB', 11, 1299.00, 150, '三星 990 Pro 2TB NVMe M.2 SSD，读取速度 7450MB/s。',
 '<table class=\"param-table\"><tr><td>品牌</td><td>Samsung</td></tr><tr><td>型号</td><td>990 Pro</td></tr><tr><td>容量</td><td>2TB</td></tr><tr><td>接口类型</td><td>PCIe 4.0 ×4 / NVMe M.2</td></tr><tr><td>顺序读取</td><td>最高 7450 MB/s</td></tr><tr><td>顺序写入</td><td>最高 6900 MB/s</td></tr><tr><td>随机读取</td><td>最高 1400K IOPS</td></tr><tr><td>随机写入</td><td>最高 1550K IOPS</td></tr><tr><td>闪存类型</td><td>Samsung V-NAND V8</td></tr><tr><td>缓存</td><td>2GB LPDDR4</td></tr><tr><td>写入寿命</td><td>1200 TBW</td></tr><tr><td>质保</td><td>5年有限保修</td></tr></table>',
 '', '', 1, 432);

-- 示例公告
INSERT INTO `announcement` (title, content, status, sort_order, level) VALUES
('欢迎来到电商平台', '欢迎使用我们的电商平台！新用户注册即享 9 折优惠。', 1, 10, 'info'),
('五一促销活动即将开始', '五一劳动节期间，全场商品低至 5 折，更有满减优惠券等你来领！', 1, 8, 'important'),
('系统维护通知', '平台将于每周日凌晨 2:00-4:00 进行系统维护，届时部分功能可能不可用。', 1, 5, 'warning');

-- 示例轮播图
INSERT INTO `banner` (title, image_url, link_url, sort_order) VALUES
('五一狂欢节', '', '', 1),
('新品首发', '', '', 2),
('数码焕新季', '', '', 3);

-- 示例评价（需先有已完成的订单，此处为演示数据）
-- 用户 user_demo / 密码 123456
INSERT INTO `user` (account_id, username, password, email, status) VALUES
(26010101, 'user_demo', '$2b$12$Ep/yDhIzfHNsC2Bt0GiJNOul.Gi1O6tkimFmmcE9PLUMHp9ek9w0C', 'demo@example.com', 1);

INSERT INTO `review` (user_id, username, avatar, product_id, order_id, rating, rating_desc, rating_logistics, rating_service, content, images, create_time) VALUES
(1, 'Demo用户', '', 1, 1, 5, 5, 5, 5, '手机非常好用，拍照效果一流，续航也很给力！', '', NOW() - INTERVAL 3 DAY),
(1, 'Demo用户', '', 2, 2, 4, 4, 4, 4, '三星屏幕果然名不虚传，显示效果很棒。', '', NOW() - INTERVAL 2 DAY),
(1, 'Demo用户', '', 3, 3, 5, 5, 5, 5, '性价比超高！徕卡拍照真的厉害，充电也快。', '', NOW() - INTERVAL 4 DAY),
(1, 'Demo用户', '', 3, 4, 3, 3, 3, 3, '用了一个月，系统偶尔卡顿，待优化。', '', NOW() - INTERVAL 6 DAY),
(1, 'Demo用户', '', 6, 5, 5, 5, 5, 5, '降噪效果惊艳，地铁上也能安静听歌。', '', NOW() - INTERVAL 1 DAY),
(1, 'Demo用户', '', 6, 6, 4, 4, 4, 4, '音质很好，佩戴舒适，续航也不错。', '', NOW() - INTERVAL 3 DAY),
(1, 'Demo用户', '', 8, 7, 5, 5, 5, 5, 'M3 Pro 性能炸裂，剪辑视频一点不卡。', '', NOW() - INTERVAL 2 DAY),
(1, 'Demo用户', '', 11, 8, 4, 4, 4, 4, '内存稳定运行，兼容性好，价格实惠。', '', NOW() - INTERVAL 1 DAY),
(1, 'Demo用户', '', 12, 9, 5, 5, 5, 5, '读取速度飞快，安装游戏秒开，强烈推荐！', '', NOW() - INTERVAL 1 DAY);

-- 更新商品的评分和评价数
UPDATE product SET avg_rating = 5.0, review_count = 1 WHERE id = 1;
UPDATE product SET avg_rating = 4.0, review_count = 1 WHERE id = 2;
UPDATE product SET avg_rating = 4.0, review_count = 2 WHERE id = 3;
UPDATE product SET avg_rating = 4.5, review_count = 2 WHERE id = 6;
UPDATE product SET avg_rating = 5.0, review_count = 1 WHERE id = 8;
UPDATE product SET avg_rating = 4.0, review_count = 1 WHERE id = 11;
UPDATE product SET avg_rating = 5.0, review_count = 1 WHERE id = 12;

-- 示例优惠券
INSERT INTO `coupon` (name, type, discount, min_amount, total_qty, remain_qty, start_time, end_time, status) VALUES
('满100减5', 1, 5.00, 100.00, 1000, 980, NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 30 DAY, 1),
('满200减15', 1, 15.00, 200.00, 500, 498, NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 30 DAY, 1),
('满500减50', 1, 50.00, 500.00, 200, 200, NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 15 DAY, 1),
('满1000减120', 1, 120.00, 1000.00, 100, 96, NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 15 DAY, 1),
('9.5折优惠券', 2, 0.95, 0.00, 300, 288, NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 60 DAY, 1),
('满200享8.8折', 2, 0.88, 200.00, 200, 190, NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 30 DAY, 1),
('免邮券', 3, 0.00, 0.00, 500, 460, NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 90 DAY, 1),
('满300减30', 1, 30.00, 300.00, 300, 295, NOW() + INTERVAL 3 DAY, NOW() + INTERVAL 45 DAY, 1);

-- ============================================
-- 14. 数据库迁移（已有数据库升级用，新库跳过）
-- ============================================
-- 新增商品详细参数字段（若已存在则忽略）
ALTER TABLE `product` ADD COLUMN `detail` TEXT COMMENT '商品详细参数（HTML）' AFTER `description`;

-- 新增SKU销量字段
ALTER TABLE `product_sku` ADD COLUMN `sales` INT DEFAULT 0 COMMENT 'SKU销量' AFTER `stock`;

-- 新增SKU状态字段
ALTER TABLE `product_sku` ADD COLUMN `status` TINYINT DEFAULT 1 COMMENT '1=上架 0=下架' AFTER `sales`;

-- 评价三维度评分字段（若已存在则忽略）
ALTER TABLE `review` ADD COLUMN `rating_desc` DECIMAL(2,1) DEFAULT NULL COMMENT '描述相符 0.5~5.0' AFTER `rating`,
                    ADD COLUMN `rating_logistics` DECIMAL(2,1) DEFAULT NULL COMMENT '物流服务 0.5~5.0' AFTER `rating_desc`,
                    ADD COLUMN `rating_service` DECIMAL(2,1) DEFAULT NULL COMMENT '服务态度 0.5~5.0' AFTER `rating_logistics`;

-- 评价综合评分字段改为支持半星（若已是 DECIMAL 则忽略）
ALTER TABLE `review` MODIFY COLUMN `rating` DECIMAL(2,1) NOT NULL COMMENT '综合评分 0.0~5.0（三维度平均）';

-- 为已有商品填充详细参数
UPDATE `product` SET `detail` = '<table class=\"param-table\"><tr><td>品牌</td><td>Apple</td></tr><tr><td>型号</td><td>iPhone 15 Pro Max</td></tr><tr><td>处理器</td><td>A17 Pro 芯片</td></tr><tr><td>屏幕尺寸</td><td>6.7 英寸</td></tr><tr><td>屏幕类型</td><td>Super Retina XDR OLED</td></tr><tr><td>分辨率</td><td>2796×1290 像素</td></tr><tr><td>运行内存</td><td>8GB</td></tr><tr><td>存储容量</td><td>256GB</td></tr><tr><td>后置摄像头</td><td>4800万主摄 + 1200万超广角 + 1200万长焦</td></tr><tr><td>前置摄像头</td><td>1200万像素</td></tr><tr><td>电池容量</td><td>4422mAh</td></tr><tr><td>充电接口</td><td>USB-C</td></tr><tr><td>机身重量</td><td>221g</td></tr><tr><td>操作系统</td><td>iOS 17</td></tr></table>' WHERE `name` = 'iPhone 15 Pro Max 256GB' AND (`detail` IS NULL OR `detail` = '');

-- 新增优惠券表（若已存在则跳过）
CREATE TABLE IF NOT EXISTS `coupon` (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  name        VARCHAR(100)   NOT NULL COMMENT '券名称',
  type        TINYINT        NOT NULL COMMENT '1=满减券 2=折扣券 3=免邮券',
  discount    DECIMAL(10,2)  NOT NULL COMMENT '优惠金额/折扣率',
  min_amount  DECIMAL(10,2)  DEFAULT 0 COMMENT '最低消费门槛',
  total_qty   INT            DEFAULT 0 COMMENT '发行总量',
  remain_qty  INT            DEFAULT 0 COMMENT '剩余数量',
  start_time  DATETIME       NOT NULL COMMENT '有效期开始',
  end_time    DATETIME       NOT NULL COMMENT '有效期结束',
  grab_start_time DATETIME   DEFAULT NULL COMMENT '抢购开始时间(NULL=无抢购限制)',
  grab_end_time   DATETIME   DEFAULT NULL COMMENT '抢购结束时间',
  scope_type  TINYINT        DEFAULT 1 COMMENT '适用范围: 1=通用 2=指定分类 3=指定商品',
  scope_ids   VARCHAR(500)   DEFAULT '' COMMENT '适用范围ID列表(JSON数组)',
  is_large    TINYINT        DEFAULT 0 COMMENT '是否大额券: 0=小额 1=大额(有抢购时间)',
  status      TINYINT        DEFAULT 1 COMMENT '1=启用 0=停用',
  create_time DATETIME       DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `user_coupon` (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id     BIGINT   NOT NULL,
  coupon_id   BIGINT   NOT NULL,
  status      TINYINT  DEFAULT 0 COMMENT '0=未使用 1=已使用 2=已过期',
  used_time   DATETIME DEFAULT NULL,
  use_order_id BIGINT  DEFAULT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_coupon (user_id, coupon_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 优惠券种子数据（仅当 coupon 表为空时插入）
INSERT IGNORE INTO `coupon` (name, type, discount, min_amount, total_qty, remain_qty, start_time, end_time, status) VALUES
('满100减5', 1, 5.00, 100.00, 1000, 980, NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 30 DAY, 1),
('满200减15', 1, 15.00, 200.00, 500, 498, NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 30 DAY, 1),
('满500减50', 1, 50.00, 500.00, 200, 200, NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 15 DAY, 1),
('满1000减120', 1, 120.00, 1000.00, 100, 96, NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 15 DAY, 1),
('9.5折优惠券', 2, 0.95, 0.00, 300, 288, NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 60 DAY, 1),
('满200享8.8折', 2, 0.88, 200.00, 200, 190, NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 30 DAY, 1),
('免邮券', 3, 0.00, 0.00, 500, 460, NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 90 DAY, 1),
('满300减30', 1, 30.00, 300.00, 300, 295, NOW() + INTERVAL 3 DAY, NOW() + INTERVAL 45 DAY, 1);

-- 订单表优惠券/折扣字段（已有数据库升级用）
ALTER TABLE `order`
  ADD COLUMN IF NOT EXISTS `coupon_id`        BIGINT        DEFAULT NULL COMMENT '使用的user_coupon.id',
  ADD COLUMN IF NOT EXISTS `coupon_discount`  DECIMAL(10,2) DEFAULT 0.00 COMMENT '优惠券抵扣金额',
  ADD COLUMN IF NOT EXISTS `discount_amount`  DECIMAL(10,2) DEFAULT 0.00 COMMENT '商品级折扣金额',
  ADD COLUMN IF NOT EXISTS `pay_amount`       DECIMAL(10,2) DEFAULT 0.00 COMMENT '实付金额';

-- 优惠券表抢购/适用范围字段（已有数据库升级用）
ALTER TABLE `coupon`
  ADD COLUMN IF NOT EXISTS `grab_start_time` DATETIME     DEFAULT NULL COMMENT '抢购开始时间(NULL=无抢购限制)',
  ADD COLUMN IF NOT EXISTS `grab_end_time`   DATETIME     DEFAULT NULL COMMENT '抢购结束时间',
  ADD COLUMN IF NOT EXISTS `scope_type`      TINYINT      DEFAULT 1   COMMENT '适用范围: 1=通用 2=指定分类 3=指定商品',
  ADD COLUMN IF NOT EXISTS `scope_ids`       VARCHAR(500) DEFAULT ''  COMMENT '适用范围ID列表(JSON数组)',
  ADD COLUMN IF NOT EXISTS `is_large`        TINYINT      DEFAULT 0   COMMENT '是否大额券: 0=小额 1=大额';

UPDATE `product` SET `detail` = '<table class=\"param-table\"><tr><td>品牌</td><td>Samsung</td></tr><tr><td>型号</td><td>Galaxy S24 Ultra</td></tr><tr><td>处理器</td><td>骁龙 8 Gen 3 for Galaxy</td></tr><tr><td>屏幕尺寸</td><td>6.8 英寸</td></tr><tr><td>屏幕类型</td><td>Dynamic AMOLED 2X</td></tr><tr><td>分辨率</td><td>3120×1440 像素</td></tr><tr><td>运行内存</td><td>12GB</td></tr><tr><td>存储容量</td><td>256GB</td></tr><tr><td>后置摄像头</td><td>2亿主摄 + 5000万长焦 + 1200万超广角 + 1000万长焦</td></tr><tr><td>前置摄像头</td><td>1200万像素</td></tr><tr><td>电池容量</td><td>5000mAh</td></tr><tr><td>机身重量</td><td>232g</td></tr><tr><td>操作系统</td><td>One UI 6.1 (Android 14)</td></tr></table>' WHERE `name` = 'Samsung Galaxy S24 Ultra' AND (`detail` IS NULL OR `detail` = '');

UPDATE `product` SET `detail` = '<table class=\"param-table\"><tr><td>品牌</td><td>Xiaomi</td></tr><tr><td>型号</td><td>14 Pro</td></tr><tr><td>处理器</td><td>骁龙 8 Gen 3</td></tr><tr><td>屏幕尺寸</td><td>6.73 英寸</td></tr><tr><td>屏幕类型</td><td>AMOLED LTPO</td></tr><tr><td>分辨率</td><td>3200×1440 像素</td></tr><tr><td>运行内存</td><td>12GB</td></tr><tr><td>存储容量</td><td>256GB</td></tr><tr><td>后置摄像头</td><td>5000万徕卡主摄 + 5000万超广角 + 5000万长焦</td></tr><tr><td>电池容量</td><td>4880mAh</td></tr><tr><td>充电</td><td>120W有线 + 50W无线</td></tr><tr><td>机身重量</td><td>223g</td></tr><tr><td>操作系统</td><td>HyperOS (Android 14)</td></tr></table>' WHERE `name` = 'Xiaomi 14 Pro' AND (`detail` IS NULL OR `detail` = '');

UPDATE `product` SET `detail` = '<table class=\"param-table\"><tr><td>品牌</td><td>Apple</td></tr><tr><td>型号</td><td>iPad Pro M4 11英寸</td></tr><tr><td>处理器</td><td>Apple M4 芯片</td></tr><tr><td>屏幕尺寸</td><td>11 英寸</td></tr><tr><td>屏幕类型</td><td>Ultra Retina XDR</td></tr><tr><td>分辨率</td><td>2420×1668 像素</td></tr><tr><td>存储容量</td><td>256GB</td></tr><tr><td>后置摄像头</td><td>1200万广角 + 1000万超广角</td></tr><tr><td>前置摄像头</td><td>1200万超广角</td></tr><tr><td>电池续航</td><td>最长10小时</td></tr><tr><td>机身重量</td><td>444g (Wi-Fi)</td></tr><tr><td>接口</td><td>USB-C (雷雳 3)</td></tr><tr><td>操作系统</td><td>iPadOS 17</td></tr></table>' WHERE `name` = 'iPad Pro M4 11英寸' AND (`detail` IS NULL OR `detail` = '');

UPDATE `product` SET `detail` = '<table class=\"param-table\"><tr><td>品牌</td><td>Huawei</td></tr><tr><td>型号</td><td>MatePad Pro 13.2</td></tr><tr><td>处理器</td><td>麒麟 9000S</td></tr><tr><td>屏幕尺寸</td><td>13.2 英寸</td></tr><tr><td>屏幕类型</td><td>OLED 柔性屏</td></tr><tr><td>分辨率</td><td>2880×1920 像素</td></tr><tr><td>运行内存</td><td>12GB</td></tr><tr><td>存储容量</td><td>256GB</td></tr><tr><td>后置摄像头</td><td>1300万主摄 + 800万超广角</td></tr><tr><td>电池容量</td><td>10050mAh</td></tr><tr><td>充电</td><td>88W 有线快充</td></tr><tr><td>机身重量</td><td>580g</td></tr><tr><td>操作系统</td><td>HarmonyOS 4</td></tr></table>' WHERE `name` = 'Huawei MatePad Pro' AND (`detail` IS NULL OR `detail` = '');

UPDATE `product` SET `detail` = '<table class=\"param-table\"><tr><td>品牌</td><td>Apple</td></tr><tr><td>型号</td><td>AirPods Pro 2</td></tr><tr><td>芯片</td><td>Apple H2</td></tr><tr><td>降噪</td><td>自适应降噪</td></tr><tr><td>音频技术</td><td>个性化空间音频 + 动态头部追踪</td></tr><tr><td>防水等级</td><td>IPX4 (耳机及充电盒)</td></tr><tr><td>电池续航</td><td>单次6小时 / 配合充电盒30小时</td></tr><tr><td>充电接口</td><td>USB-C / MagSafe / Apple Watch充电器</td></tr><tr><td>重量</td><td>单只5.3g</td></tr></table>' WHERE `name` = 'AirPods Pro 2' AND (`detail` IS NULL OR `detail` = '');

UPDATE `product` SET `detail` = '<table class=\"param-table\"><tr><td>品牌</td><td>Sony</td></tr><tr><td>型号</td><td>WH-1000XM5</td></tr><tr><td>驱动单元</td><td>30mm 驱动单元</td></tr><tr><td>降噪</td><td>双芯片降噪 (QN1 + V1)</td></tr><tr><td>音频编码</td><td>LDAC / AAC / SBC</td></tr><tr><td>蓝牙版本</td><td>蓝牙 5.2</td></tr><tr><td>电池续航</td><td>30 小时 (ANC开) / 40 小时 (ANC关)</td></tr><tr><td>充电</td><td>USB-C 快充 (3分钟≈3小时)</td></tr><tr><td>重量</td><td>约 250g</td></tr><tr><td>接口</td><td>3.5mm 耳机孔 / USB-C</td></tr></table>' WHERE `name` = 'Sony WH-1000XM5' AND (`detail` IS NULL OR `detail` = '');

UPDATE `product` SET `detail` = '<table class=\"param-table\"><tr><td>品牌</td><td>Apple</td></tr><tr><td>型号</td><td>MacBook Pro 14 (M3 Pro)</td></tr><tr><td>处理器</td><td>Apple M3 Pro (11核CPU / 14核GPU)</td></tr><tr><td>屏幕尺寸</td><td>14.2 英寸</td></tr><tr><td>屏幕类型</td><td>Liquid Retina XDR</td></tr><tr><td>分辨率</td><td>3024×1964 像素</td></tr><tr><td>运行内存</td><td>18GB 统一内存</td></tr><tr><td>存储容量</td><td>512GB SSD</td></tr><tr><td>接口</td><td>雷雳4×2 / HDMI / SDXC / MagSafe 3</td></tr><tr><td>电池续航</td><td>最长17小时</td></tr><tr><td>机身重量</td><td>1.55kg</td></tr><tr><td>操作系统</td><td>macOS Sonoma</td></tr></table>' WHERE `name` = 'MacBook Pro 14 M3 Pro' AND (`detail` IS NULL OR `detail` = '');

UPDATE `product` SET `detail` = '<table class=\"param-table\"><tr><td>品牌</td><td>Lenovo</td></tr><tr><td>型号</td><td>ThinkPad X1 Carbon Gen 12</td></tr><tr><td>处理器</td><td>Intel Core Ultra 7 155H</td></tr><tr><td>屏幕尺寸</td><td>14 英寸</td></tr><tr><td>屏幕类型</td><td>2.8K OLED</td></tr><tr><td>分辨率</td><td>2880×1800 像素</td></tr><tr><td>运行内存</td><td>32GB LPDDR5x</td></tr><tr><td>存储容量</td><td>1TB SSD</td></tr><tr><td>接口</td><td>Thunderbolt 4×2 / USB-A×2 / HDMI 2.1</td></tr><tr><td>电池续航</td><td>最长14小时</td></tr><tr><td>机身重量</td><td>1.09kg</td></tr><tr><td>操作系统</td><td>Windows 11 Pro</td></tr></table>' WHERE `name` = 'ThinkPad X1 Carbon' AND (`detail` IS NULL OR `detail` = '');

UPDATE `product` SET `detail` = '<table class=\"param-table\"><tr><td>品牌</td><td>Dell</td></tr><tr><td>型号</td><td>XPS 15 9530</td></tr><tr><td>处理器</td><td>Intel Core i9-13900H</td></tr><tr><td>显卡</td><td>NVIDIA GeForce RTX 4070</td></tr><tr><td>屏幕尺寸</td><td>15.6 英寸</td></tr><tr><td>屏幕类型</td><td>3.5K OLED 触控屏</td></tr><tr><td>分辨率</td><td>3456×2160 像素</td></tr><tr><td>运行内存</td><td>32GB DDR5</td></tr><tr><td>存储容量</td><td>1TB SSD</td></tr><tr><td>接口</td><td>Thunderbolt 4×2 / USB-C / SD卡槽</td></tr><tr><td>电池容量</td><td>86Whr</td></tr><tr><td>机身重量</td><td>1.92kg</td></tr><tr><td>操作系统</td><td>Windows 11 Home</td></tr></table>' WHERE `name` = 'Dell XPS 15' AND (`detail` IS NULL OR `detail` = '');

UPDATE `product` SET `detail` = '<table class=\"param-table\"><tr><td>品牌</td><td>Kingston</td></tr><tr><td>型号</td><td>Fury Beast DDR5</td></tr><tr><td>内存类型</td><td>DDR5</td></tr><tr><td>容量</td><td>32GB (16GB×2)</td></tr><tr><td>频率</td><td>5600MHz</td></tr><tr><td>时序</td><td>CL40-40-40</td></tr><tr><td>工作电压</td><td>1.25V</td></tr><tr><td>散热</td><td>铝制散热马甲</td></tr><tr><td>兼容平台</td><td>Intel 700 / AMD AM5系列</td></tr><tr><td>质保</td><td>终身质保</td></tr></table>' WHERE `name` = 'Kingston DDR5 32GB' AND (`detail` IS NULL OR `detail` = '');

UPDATE `product` SET `detail` = '<table class=\"param-table\"><tr><td>品牌</td><td>Samsung</td></tr><tr><td>型号</td><td>990 Pro</td></tr><tr><td>容量</td><td>2TB</td></tr><tr><td>接口类型</td><td>PCIe 4.0 ×4 / NVMe M.2</td></tr><tr><td>顺序读取</td><td>最高 7450 MB/s</td></tr><tr><td>顺序写入</td><td>最高 6900 MB/s</td></tr><tr><td>随机读取</td><td>最高 1400K IOPS</td></tr><tr><td>随机写入</td><td>最高 1550K IOPS</td></tr><tr><td>闪存类型</td><td>Samsung V-NAND V8</td></tr><tr><td>缓存</td><td>2GB LPDDR4</td></tr><tr><td>写入寿命</td><td>1200 TBW</td></tr><tr><td>质保</td><td>5年有限保修</td></tr></table>' WHERE `name` = 'Samsung 990 Pro 2TB' AND (`detail` IS NULL OR `detail` = '');

-- ============================================
-- 15. 用户扩展字段（已有数据库升级用）
-- ============================================
ALTER TABLE `user`
  ADD COLUMN IF NOT EXISTS `gender`              TINYINT      DEFAULT 0   COMMENT '0=保密 1=男 2=女',
  ADD COLUMN IF NOT EXISTS `intro`               VARCHAR(200) DEFAULT ''  COMMENT '自我介绍',
  ADD COLUMN IF NOT EXISTS `username_update_time` DATETIME    DEFAULT NULL COMMENT '用户名最后修改时间';

-- ============================================
-- 16. Levenshtein 编辑距离函数（模糊搜索）
-- ============================================
DROP FUNCTION IF EXISTS levenshtein;
DELIMITER //

CREATE FUNCTION levenshtein(s1 VARCHAR(255), s2 VARCHAR(255))
RETURNS INT
DETERMINISTIC
READS SQL DATA
BEGIN
  DECLARE s1_len, s2_len, i, j, c, c_temp, cost INT;
  DECLARE s1_char CHAR(1);
  DECLARE cv0, cv1 VARBINARY(256);

  SET s1_len = CHAR_LENGTH(s1), s2_len = CHAR_LENGTH(s2);
  IF s1 = s2 THEN RETURN 0;
  ELSEIF s1_len = 0 THEN RETURN s2_len;
  ELSEIF s2_len = 0 THEN RETURN s1_len;
  END IF;

  SET cv1 = 0x00, j = 1;
  WHILE j <= s2_len DO
    SET cv1 = CONCAT(cv1, UNHEX(HEX(j))), j = j + 1;
  END WHILE;

  SET i = 1;
  WHILE i <= s1_len DO
    SET s1_char = SUBSTRING(s1, i, 1), c = i;
    SET cv0 = UNHEX(HEX(i)), j = 1;
    WHILE j <= s2_len DO
      SET c = c + 1;
      IF s1_char = SUBSTRING(s2, j, 1) THEN SET cost = 0; ELSE SET cost = 1; END IF;
      SET c_temp = CONV(HEX(SUBSTRING(cv1, j, 1)), 16, 10) + cost;
      IF c > c_temp THEN SET c = c_temp; END IF;
      SET c_temp = CONV(HEX(SUBSTRING(cv1, j+1, 1)), 16, 10) + 1;
      IF c > c_temp THEN SET c = c_temp; END IF;
      SET cv0 = CONCAT(cv0, UNHEX(HEX(c))), j = j + 1;
    END WHILE;
    SET cv1 = cv0, i = i + 1;
  END WHILE;
  RETURN c;
END //

DELIMITER ;

-- 热门关键词种子数据
INSERT INTO `hot_keyword` (keyword, search_count, is_manual, is_pinned, sort_order, status) VALUES
('笔记本电脑', 156, 1, 1, 1, 1),
('手机', 132, 1, 1, 2, 1),
('耳机', 98, 0, 0, 3, 1),
('平板电脑', 87, 0, 0, 4, 1),
('机械键盘', 65, 0, 0, 5, 1),
('显示器', 54, 0, 0, 6, 1),
('运动鞋', 48, 0, 0, 7, 1),
('固态硬盘', 42, 1, 0, 8, 1);
