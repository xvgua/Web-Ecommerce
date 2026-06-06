-- ============================================
-- Seed data for microservices databases
-- ============================================

-- === ecommerce_user ===
USE ecommerce_user;

-- 管理员 (密码: admin123)
INSERT INTO `admin` (username, password, role, status) VALUES
('admin', '$2a$12$Dz0qAK13FSvutDiAxiFtbOpFs7o/Gvx8Eyo/.yhgKnB35aXjLXkhq', 'SUPER_ADMIN', 1);

-- 用户 (密码: 123456)
INSERT INTO `user` (account_id, username, password, email, status) VALUES
(26010101, 'user_demo', '$2a$12$wmPfZSmio6OT.sLJP37KtedDEyWZng7ahGrxHOrlBi6RL2EHH/G8e', 'demo@example.com', 1);

INSERT INTO `announcement` (title, content, status, sort_order, level) VALUES
('欢迎来到电商平台', '欢迎使用我们的电商平台！新用户注册即享 9 折优惠。', 1, 10, 'info'),
('五一促销活动即将开始', '五一劳动节期间，全场商品低至 5 折，更有满减优惠券等你来领！', 1, 8, 'important'),
('系统维护通知', '平台将于每周日凌晨 2:00-4:00 进行系统维护，届时部分功能可能不可用。', 1, 5, 'warning');

INSERT INTO `banner` (title, image_url, link_url, sort_order) VALUES
('五一狂欢节', '', '', 1),
('新品首发', '', '', 2),
('数码焕新季', '', '', 3);

-- === ecommerce_product ===
USE ecommerce_product;

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

INSERT INTO `product` (name, category_id, price, stock, description, detail, main_image, images, status, sales, listed_at) VALUES
('iPhone 15 Pro Max 256GB', 6, 9999.00, 100, 'Apple iPhone 15 Pro Max，搭载 A17 Pro 芯片，钛金属设计，4800 万像素主摄。',
 '<table class="param-table"><tr><td>品牌</td><td>Apple</td></tr><tr><td>型号</td><td>iPhone 15 Pro Max</td></tr><tr><td>处理器</td><td>A17 Pro 芯片</td></tr><tr><td>屏幕尺寸</td><td>6.7 英寸</td></tr><tr><td>屏幕类型</td><td>Super Retina XDR OLED</td></tr><tr><td>分辨率</td><td>2796×1290 像素</td></tr><tr><td>运行内存</td><td>8GB</td></tr><tr><td>存储容量</td><td>256GB</td></tr><tr><td>后置摄像头</td><td>4800万主摄 + 1200万超广角 + 1200万长焦</td></tr><tr><td>前置摄像头</td><td>1200万像素</td></tr><tr><td>电池容量</td><td>4422mAh</td></tr><tr><td>充电接口</td><td>USB-C</td></tr><tr><td>机身重量</td><td>221g</td></tr><tr><td>操作系统</td><td>iOS 17</td></tr></table>',
 '', '', 1, 256, NOW()),
('Samsung Galaxy S24 Ultra', 6, 8999.00, 80, '三星 Galaxy S24 Ultra，搭载骁龙 8 Gen 3，钛金属框架，2 亿像素摄像头。',
 '<table class="param-table"><tr><td>品牌</td><td>Samsung</td></tr><tr><td>型号</td><td>Galaxy S24 Ultra</td></tr><tr><td>处理器</td><td>骁龙 8 Gen 3 for Galaxy</td></tr><tr><td>屏幕尺寸</td><td>6.8 英寸</td></tr><tr><td>屏幕类型</td><td>Dynamic AMOLED 2X</td></tr><tr><td>分辨率</td><td>3120×1440 像素</td></tr><tr><td>运行内存</td><td>12GB</td></tr><tr><td>存储容量</td><td>256GB</td></tr><tr><td>后置摄像头</td><td>2亿主摄 + 5000万长焦 + 1200万超广角 + 1000万长焦</td></tr><tr><td>前置摄像头</td><td>1200万像素</td></tr><tr><td>电池容量</td><td>5000mAh</td></tr><tr><td>机身重量</td><td>232g</td></tr><tr><td>操作系统</td><td>One UI 6.1 (Android 14)</td></tr></table>',
 '', '', 1, 189, NOW()),
('Xiaomi 14 Pro', 6, 4999.00, 150, '小米 14 Pro，搭载骁龙 8 Gen 3，徕卡光学镜头，120W 秒充。',
 '<table class="param-table"><tr><td>品牌</td><td>Xiaomi</td></tr><tr><td>型号</td><td>14 Pro</td></tr><tr><td>处理器</td><td>骁龙 8 Gen 3</td></tr><tr><td>屏幕尺寸</td><td>6.73 英寸</td></tr><tr><td>屏幕类型</td><td>AMOLED LTPO</td></tr><tr><td>分辨率</td><td>3200×1440 像素</td></tr><tr><td>运行内存</td><td>12GB</td></tr><tr><td>存储容量</td><td>256GB</td></tr><tr><td>后置摄像头</td><td>5000万徕卡主摄 + 5000万超广角 + 5000万长焦</td></tr><tr><td>电池容量</td><td>4880mAh</td></tr><tr><td>充电</td><td>120W有线 + 50W无线</td></tr><tr><td>机身重量</td><td>223g</td></tr><tr><td>操作系统</td><td>HyperOS (Android 14)</td></tr></table>',
 '', '', 1, 312, NOW()),
('iPad Pro M4 11英寸', 7, 6799.00, 60, 'Apple iPad Pro M4 芯片，Ultra Retina XDR 显示屏，轻薄设计。',
 '<table class="param-table"><tr><td>品牌</td><td>Apple</td></tr><tr><td>型号</td><td>iPad Pro M4 11英寸</td></tr><tr><td>处理器</td><td>Apple M4 芯片</td></tr><tr><td>屏幕尺寸</td><td>11 英寸</td></tr><tr><td>屏幕类型</td><td>Ultra Retina XDR</td></tr><tr><td>分辨率</td><td>2420×1668 像素</td></tr><tr><td>存储容量</td><td>256GB</td></tr><tr><td>后置摄像头</td><td>1200万广角 + 1000万超广角</td></tr><tr><td>前置摄像头</td><td>1200万超广角</td></tr><tr><td>电池续航</td><td>最长10小时</td></tr><tr><td>机身重量</td><td>444g (Wi-Fi)</td></tr><tr><td>接口</td><td>USB-C (雷雳 3)</td></tr><tr><td>操作系统</td><td>iPadOS 17</td></tr></table>',
 '', '', 1, 98, NOW()),
('Huawei MatePad Pro', 7, 4299.00, 45, '华为 MatePad Pro 13.2英寸，OLED 柔性屏，鸿蒙系统。',
 '<table class="param-table"><tr><td>品牌</td><td>Huawei</td></tr><tr><td>型号</td><td>MatePad Pro 13.2</td></tr><tr><td>处理器</td><td>麒麟 9000S</td></tr><tr><td>屏幕尺寸</td><td>13.2 英寸</td></tr><tr><td>屏幕类型</td><td>OLED 柔性屏</td></tr><tr><td>分辨率</td><td>2880×1920 像素</td></tr><tr><td>运行内存</td><td>12GB</td></tr><tr><td>存储容量</td><td>256GB</td></tr><tr><td>后置摄像头</td><td>1300万主摄 + 800万超广角</td></tr><tr><td>电池容量</td><td>10050mAh</td></tr><tr><td>充电</td><td>88W 有线快充</td></tr><tr><td>机身重量</td><td>580g</td></tr><tr><td>操作系统</td><td>HarmonyOS 4</td></tr></table>',
 '', '', 1, 67, NOW()),
('AirPods Pro 2', 8, 1899.00, 200, 'Apple AirPods Pro 第二代，自适应降噪，个性化空间音频。',
 '<table class="param-table"><tr><td>品牌</td><td>Apple</td></tr><tr><td>型号</td><td>AirPods Pro 2</td></tr><tr><td>芯片</td><td>Apple H2</td></tr><tr><td>降噪</td><td>自适应降噪</td></tr><tr><td>音频技术</td><td>个性化空间音频 + 动态头部追踪</td></tr><tr><td>防水等级</td><td>IPX4 (耳机及充电盒)</td></tr><tr><td>电池续航</td><td>单次6小时 / 配合充电盒30小时</td></tr><tr><td>充电接口</td><td>USB-C / MagSafe / Apple Watch充电器</td></tr><tr><td>重量</td><td>单只5.3g</td></tr></table>',
 '', '', 1, 520, NOW()),
('Sony WH-1000XM5', 8, 2499.00, 70, '索尼头戴式降噪耳机，行业领先降噪，30 小时续航。',
 '<table class="param-table"><tr><td>品牌</td><td>Sony</td></tr><tr><td>型号</td><td>WH-1000XM5</td></tr><tr><td>驱动单元</td><td>30mm 驱动单元</td></tr><tr><td>降噪</td><td>双芯片降噪 (QN1 + V1)</td></tr><tr><td>音频编码</td><td>LDAC / AAC / SBC</td></tr><tr><td>蓝牙版本</td><td>蓝牙 5.2</td></tr><tr><td>电池续航</td><td>30 小时 (ANC开) / 40 小时 (ANC关)</td></tr><tr><td>充电</td><td>USB-C 快充 (3分钟≈3小时)</td></tr><tr><td>重量</td><td>约 250g</td></tr><tr><td>接口</td><td>3.5mm 耳机孔 / USB-C</td></tr></table>',
 '', '', 1, 234, NOW()),
('MacBook Pro 14 M3 Pro', 9, 12999.00, 40, 'Apple MacBook Pro 14英寸，M3 Pro 芯片，Liquid Retina XDR 显示屏。',
 '<table class="param-table"><tr><td>品牌</td><td>Apple</td></tr><tr><td>型号</td><td>MacBook Pro 14 (M3 Pro)</td></tr><tr><td>处理器</td><td>Apple M3 Pro (11核CPU / 14核GPU)</td></tr><tr><td>屏幕尺寸</td><td>14.2 英寸</td></tr><tr><td>屏幕类型</td><td>Liquid Retina XDR</td></tr><tr><td>分辨率</td><td>3024×1964 像素</td></tr><tr><td>运行内存</td><td>18GB 统一内存</td></tr><tr><td>存储容量</td><td>512GB SSD</td></tr><tr><td>接口</td><td>雷雳4×2 / HDMI / SDXC / MagSafe 3</td></tr><tr><td>电池续航</td><td>最长17小时</td></tr><tr><td>机身重量</td><td>1.55kg</td></tr><tr><td>操作系统</td><td>macOS Sonoma</td></tr></table>',
 '', '', 1, 156, NOW()),
('ThinkPad X1 Carbon', 9, 8999.00, 35, '联想 ThinkPad X1 Carbon Gen 12，商务旗舰，14英寸 2.8K OLED。',
 '<table class="param-table"><tr><td>品牌</td><td>Lenovo</td></tr><tr><td>型号</td><td>ThinkPad X1 Carbon Gen 12</td></tr><tr><td>处理器</td><td>Intel Core Ultra 7 155H</td></tr><tr><td>屏幕尺寸</td><td>14 英寸</td></tr><tr><td>屏幕类型</td><td>2.8K OLED</td></tr><tr><td>分辨率</td><td>2880×1800 像素</td></tr><tr><td>运行内存</td><td>32GB LPDDR5x</td></tr><tr><td>存储容量</td><td>1TB SSD</td></tr><tr><td>接口</td><td>Thunderbolt 4×2 / USB-A×2 / HDMI 2.1</td></tr><tr><td>电池续航</td><td>最长14小时</td></tr><tr><td>机身重量</td><td>1.09kg</td></tr><tr><td>操作系统</td><td>Windows 11 Pro</td></tr></table>',
 '', '', 1, 89, NOW()),
('Dell XPS 15', 9, 10999.00, 25, '戴尔 XPS 15，i9-13900H，RTX 4070，3.5K OLED 触控屏。',
 '<table class="param-table"><tr><td>品牌</td><td>Dell</td></tr><tr><td>型号</td><td>XPS 15 9530</td></tr><tr><td>处理器</td><td>Intel Core i9-13900H</td></tr><tr><td>显卡</td><td>NVIDIA GeForce RTX 4070</td></tr><tr><td>屏幕尺寸</td><td>15.6 英寸</td></tr><tr><td>屏幕类型</td><td>3.5K OLED 触控屏</td></tr><tr><td>分辨率</td><td>3456×2160 像素</td></tr><tr><td>运行内存</td><td>32GB DDR5</td></tr><tr><td>存储容量</td><td>1TB SSD</td></tr><tr><td>接口</td><td>Thunderbolt 4×2 / USB-C / SD卡槽</td></tr><tr><td>电池容量</td><td>86Whr</td></tr><tr><td>机身重量</td><td>1.92kg</td></tr><tr><td>操作系统</td><td>Windows 11 Home</td></tr></table>',
 '', '', 1, 45, NOW()),
('Kingston DDR5 32GB', 11, 799.00, 300, '金士顿 Fury DDR5 5600MHz 32GB 台式机内存条。',
 '<table class="param-table"><tr><td>品牌</td><td>Kingston</td></tr><tr><td>型号</td><td>Fury Beast DDR5</td></tr><tr><td>内存类型</td><td>DDR5</td></tr><tr><td>容量</td><td>32GB (16GB×2)</td></tr><tr><td>频率</td><td>5600MHz</td></tr><tr><td>时序</td><td>CL40-40-40</td></tr><tr><td>工作电压</td><td>1.25V</td></tr><tr><td>散热</td><td>铝制散热马甲</td></tr><tr><td>兼容平台</td><td>Intel 700 / AMD AM5系列</td></tr><tr><td>质保</td><td>终身质保</td></tr></table>',
 '', '', 1, 678, NOW()),
('Samsung 990 Pro 2TB', 11, 1299.00, 150, '三星 990 Pro 2TB NVMe M.2 SSD，读取速度 7450MB/s。',
 '<table class="param-table"><tr><td>品牌</td><td>Samsung</td></tr><tr><td>型号</td><td>990 Pro</td></tr><tr><td>容量</td><td>2TB</td></tr><tr><td>接口类型</td><td>PCIe 4.0 ×4 / NVMe M.2</td></tr><tr><td>顺序读取</td><td>最高 7450 MB/s</td></tr><tr><td>顺序写入</td><td>最高 6900 MB/s</td></tr><tr><td>随机读取</td><td>最高 1400K IOPS</td></tr><tr><td>随机写入</td><td>最高 1550K IOPS</td></tr><tr><td>闪存类型</td><td>Samsung V-NAND V8</td></tr><tr><td>缓存</td><td>2GB LPDDR4</td></tr><tr><td>写入寿命</td><td>1200 TBW</td></tr><tr><td>质保</td><td>5年有限保修</td></tr></table>',
 '', '', 1, 432, NOW());

INSERT INTO `hot_keyword` (keyword, search_count, is_manual, is_pinned, sort_order, status) VALUES
('笔记本电脑', 156, 1, 1, 1, 1),
('手机', 132, 1, 1, 2, 1),
('耳机', 98, 0, 0, 3, 1),
('平板电脑', 87, 0, 0, 4, 1),
('机械键盘', 65, 0, 0, 5, 1),
('显示器', 54, 0, 0, 6, 1),
('运动鞋', 48, 0, 0, 7, 1),
('固态硬盘', 42, 1, 0, 8, 1);

-- === ecommerce_order ===
USE ecommerce_order;

INSERT INTO `coupon` (name, type, discount, min_amount, total_qty, remain_qty, start_time, end_time, status) VALUES
('满100减5', 1, 5.00, 100.00, 1000, 980, NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 30 DAY, 1),
('满200减15', 1, 15.00, 200.00, 500, 498, NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 30 DAY, 1),
('满500减50', 1, 50.00, 500.00, 200, 200, NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 15 DAY, 1),
('满1000减120', 1, 120.00, 1000.00, 100, 96, NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 15 DAY, 1),
('9.5折优惠券', 2, 0.95, 0.00, 300, 288, NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 60 DAY, 1),
('满200享8.8折', 2, 0.88, 200.00, 200, 190, NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 30 DAY, 1),
('免邮券', 3, 0.00, 0.00, 500, 460, NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 90 DAY, 1),
('满300减30', 1, 30.00, 300.00, 300, 295, NOW() + INTERVAL 3 DAY, NOW() + INTERVAL 45 DAY, 1);
