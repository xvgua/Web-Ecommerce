-- Seed reviews for all products
-- Reviews go into ecommerce_order.review

-- iPhone 15 Pro Max (product_id=13)
INSERT INTO `ecommerce_order`.`review` (user_id, username, avatar, product_id, order_id, rating, rating_desc, rating_logistics, rating_service, content, images, is_followup, like_count, comment_count, create_time) VALUES
(3, 'user_demo', NULL, 13, NULL, 4.80, 5.00, 4.50, 5.00, '钛合金边框手感无敌，A17 Pro芯片运行大型游戏流畅得不行。屏幕显示效果惊艳，续航也比上一代强不少。值得入手！', NULL, 0, 12, 2, '2026-05-15 10:30:00'),
(3, 'user_demo', NULL, 13, NULL, 5.00, 5.00, 5.00, 5.00, '用了两周了，非常满意。拍照效果太棒了，特别是夜间模式，细节还原度超高。电池一天半充一次，比之前用的安卓机强太多。', NULL, 0, 8, 1, '2026-06-01 14:20:00'),
(3, 'user_demo', NULL, 13, NULL, 4.50, 4.00, 5.00, 4.50, '整体还不错，就是充电速度一般般。Type-C接口终于换上了，出门一根线搞定所有设备。颜值真的高，颜色选择也多。', NULL, 0, 5, 0, '2026-06-05 09:15:00');

-- Samsung Galaxy S24 Ultra (product_id=14)
INSERT INTO `ecommerce_order`.`review` (user_id, username, avatar, product_id, order_id, rating, rating_desc, rating_logistics, rating_service, content, images, is_followup, like_count, comment_count, create_time) VALUES
(3, 'user_demo', NULL, 14, NULL, 5.00, 5.00, 5.00, 5.00, 'S Pen体验一如既往的好，屏幕直屏设计终于回归了，贴膜方便多了。AI功能很实用，翻译和图片处理速度飞快。', NULL, 0, 15, 3, '2026-05-20 16:45:00'),
(3, 'user_demo', NULL, 14, NULL, 4.70, 5.00, 4.00, 5.00, '拍照很强，长焦镜头100倍变焦真的能拍到很远的东西。系统One UI 6很流畅，三星这几年的优化确实进步很大。', NULL, 0, 9, 1, '2026-06-03 11:30:00');

-- Xiaomi 14 Pro (product_id=15)
INSERT INTO `ecommerce_order`.`review` (user_id, username, avatar, product_id, order_id, rating, rating_desc, rating_logistics, rating_service, content, images, is_followup, like_count, comment_count, create_time) VALUES
(3, 'user_demo', NULL, 15, NULL, 4.60, 5.00, 4.00, 5.00, '性价比无敌！Leica联名拍照效果非常出色，徕卡水印也很有逼格。澎湃OS日常使用非常流畅，充电120W简直飞起。', NULL, 0, 20, 4, '2026-05-18 08:00:00'),
(3, 'user_demo', NULL, 15, NULL, 4.80, 5.00, 4.50, 5.00, '手感很好，小尺寸旗舰里最佳选择。屏幕素质顶级，亮度也很高在阳光下能看清。唯一的遗憾是电池稍小，重度使用一天有点勉强。', NULL, 0, 11, 2, '2026-06-02 19:10:00'),
(3, 'user_demo', NULL, 15, NULL, 5.00, 5.00, 5.00, 5.00, '用了快一个月，没有任何槽点。拍照、性能、续航、充电都很均衡。小米这几年真的是越来越好了。', NULL, 0, 6, 0, '2026-06-06 07:30:00');

-- iPad Pro M4 11" (product_id=16)
INSERT INTO `ecommerce_order`.`review` (user_id, username, avatar, product_id, order_id, rating, rating_desc, rating_logistics, rating_service, content, images, is_followup, like_count, comment_count, create_time) VALUES
(3, 'user_demo', NULL, 16, NULL, 5.00, 5.00, 5.00, 5.00, 'M4芯片太强了，剪4K视频毫无压力。新款的OLED屏幕显示效果绝了，黑色真的够黑。轻薄到拿在手里不可思议！', NULL, 0, 18, 3, '2026-05-22 13:00:00'),
(3, 'user_demo', NULL, 16, NULL, 4.50, 5.00, 4.00, 4.50, '用来画画和做笔记，Apple Pencil Pro的新功能很好用。屏幕尺寸刚好适合随身携带。就是价格有点贵，配件还得另外买。', NULL, 0, 7, 1, '2026-06-04 15:45:00');

-- Huawei MatePad Pro (product_id=17)
INSERT INTO `ecommerce_order`.`review` (user_id, username, avatar, product_id, order_id, rating, rating_desc, rating_logistics, rating_service, content, images, is_followup, like_count, comment_count, create_time) VALUES
(3, 'user_demo', NULL, 17, NULL, 4.50, 4.50, 5.00, 4.00, '鸿蒙系统很流畅，多设备协同体验很好。屏幕素质不错，看剧和办公都很舒服。手写笔延迟很低，写字很跟手。', NULL, 0, 10, 2, '2026-05-25 10:00:00'),
(3, 'user_demo', NULL, 17, NULL, 4.80, 5.00, 4.50, 5.00, '外观设计很漂亮，金属机身手感一流。扬声器效果很好，看视频不用戴耳机。续航也够用，一天办公下来还剩40%。', NULL, 0, 6, 0, '2026-06-05 16:20:00');

-- AirPods Pro 2 (product_id=18)
INSERT INTO `ecommerce_order`.`review` (user_id, username, avatar, product_id, order_id, rating, rating_desc, rating_logistics, rating_service, content, images, is_followup, like_count, comment_count, create_time) VALUES
(3, 'user_demo', NULL, 18, NULL, 4.80, 5.00, 4.50, 5.00, '降噪效果太强了，地铁上基本听不到噪音。通透模式也很自然，跟没戴耳机一样。空间音频听歌有临场感，非常推荐！', NULL, 0, 25, 5, '2026-05-10 12:00:00'),
(3, 'user_demo', NULL, 18, NULL, 5.00, 5.00, 5.00, 5.00, '配上iPhone用无缝切换太方便了，续航也不错，一周充两次电。自适应音频功能很智能，根据环境自动调节。', NULL, 0, 14, 2, '2026-05-28 08:30:00'),
(3, 'user_demo', NULL, 18, NULL, 4.50, 4.00, 5.00, 4.50, '音质比一代有提升，低音更饱满了。不过耳塞戴久了还是会有点不舒服，可能因人而异吧。整体性价比很高。', NULL, 0, 8, 1, '2026-06-05 20:00:00');

-- Sony WH-1000XM5 (product_id=19)
INSERT INTO `ecommerce_order`.`review` (user_id, username, avatar, product_id, order_id, rating, rating_desc, rating_logistics, rating_service, content, images, is_followup, like_count, comment_count, create_time) VALUES
(3, 'user_demo', NULL, 19, NULL, 4.80, 5.00, 4.50, 5.00, '降噪天花板！比XM4更轻更舒适，长时间佩戴不会压头。音质通透，LDAC连接听无损音乐太享受了。出差必备。', NULL, 0, 22, 3, '2026-05-12 17:00:00'),
(3, 'user_demo', NULL, 19, NULL, 4.50, 4.50, 4.50, 4.50, '30小时续航真的很耐用，一个星期充一次电。通话降噪也很强，对方说明显比之前清晰很多。就是不能折叠有点遗憾。', NULL, 0, 9, 0, '2026-06-01 10:45:00');

-- MacBook Pro 14 M3 Pro (product_id=20)
INSERT INTO `ecommerce_order`.`review` (user_id, username, avatar, product_id, order_id, rating, rating_desc, rating_logistics, rating_service, content, images, is_followup, like_count, comment_count, create_time) VALUES
(3, 'user_demo', NULL, 20, NULL, 5.00, 5.00, 5.00, 5.00, 'M3 Pro性能太强了，编译代码速度飞快。Liquid Retina XDR屏幕看HDR内容简直是一种享受。续航一天以上，开会不用带充电器。', NULL, 0, 30, 6, '2026-05-08 09:00:00'),
(3, 'user_demo', NULL, 20, NULL, 4.80, 5.00, 4.50, 5.00, '深空黑色很好看，新设计比之前轻薄了不少。键盘手感舒适，触控板依然无敌。接口齐全不用买拓展坞了。', NULL, 0, 18, 2, '2026-06-02 14:30:00'),
(3, 'user_demo', NULL, 20, NULL, 4.50, 4.50, 4.00, 5.00, '机器是好机器，就是18G内存在重度多任务时有点吃力。建议开发的同学上36G版本。总体上非常满意。', NULL, 0, 11, 1, '2026-06-06 11:00:00');

-- ThinkPad X1 Carbon (product_id=21)
INSERT INTO `ecommerce_order`.`review` (user_id, username, avatar, product_id, order_id, rating, rating_desc, rating_logistics, rating_service, content, images, is_followup, like_count, comment_count, create_time) VALUES
(3, 'user_demo', NULL, 21, NULL, 4.80, 5.00, 4.50, 5.00, '商务办公首选！键盘手感一如既往的好，打字会上瘾。重量只有1.1kg，出差带着毫无负担。小红点依然经典好用。', NULL, 0, 16, 2, '2026-05-16 13:30:00'),
(3, 'user_demo', NULL, 21, NULL, 4.70, 4.50, 5.00, 4.50, '散热控制得很好，风扇声音很小。接口齐全，厚度控制也很棒。屏幕16:10比例适合看文档和代码。ThinkPad品质一如既往。', NULL, 0, 8, 0, '2026-06-03 09:20:00');

-- Dell XPS 15 (product_id=22)
INSERT INTO `ecommerce_order`.`review` (user_id, username, avatar, product_id, order_id, rating, rating_desc, rating_logistics, rating_service, content, images, is_followup, like_count, comment_count, create_time) VALUES
(3, 'user_demo', NULL, 22, NULL, 4.50, 5.00, 4.00, 4.50, '窄边框屏幕视觉冲击力很强，4K触控版显示细腻。性能释放不错，做设计和轻度剪辑都够用。外观简约大气。', NULL, 0, 12, 3, '2026-05-19 15:00:00'),
(3, 'user_demo', NULL, 22, NULL, 4.60, 4.50, 4.50, 5.00, '做工精致，全金属机身质感很好。碳纤维掌托摸着很舒服。扬声器效果不错，看电影很爽。散热稍微有点响。', NULL, 0, 7, 1, '2026-06-04 16:10:00');

-- Kingston DDR5 32GB (product_id=23)
INSERT INTO `ecommerce_order`.`review` (user_id, username, avatar, product_id, order_id, rating, rating_desc, rating_logistics, rating_service, content, images, is_followup, like_count, comment_count, create_time) VALUES
(3, 'user_demo', NULL, 23, NULL, 5.00, 5.00, 5.00, 5.00, '插上即用非常稳定，XMP一键超频到6000MHz。兼容性很好，AMD和Intel平台都测试过了没问题。性价比很高的DDR5内存。', NULL, 0, 20, 4, '2026-05-14 11:00:00'),
(3, 'user_demo', NULL, 23, NULL, 4.80, 5.00, 4.50, 5.00, '包装很好，金手指没有氧化。和之前的DDR4相比提升明显，AE渲染和PR导出快了很多。预算有限又想上DDR5的推荐这款。', NULL, 0, 10, 1, '2026-06-02 18:30:00'),
(3, 'user_demo', NULL, 23, NULL, 4.50, 4.50, 4.50, 4.50, '运行稳定没蓝屏过，温度控制也不错。虽然是普条不带RGB，但性价比确实高。双通道32G日常够用了。', NULL, 0, 5, 0, '2026-06-05 21:45:00');

-- Samsung 990 Pro 2TB (product_id=24)
INSERT INTO `ecommerce_order`.`review` (user_id, username, avatar, product_id, order_id, rating, rating_desc, rating_logistics, rating_service, content, images, is_followup, like_count, comment_count, create_time) VALUES
(3, 'user_demo', NULL, 24, NULL, 5.00, 5.00, 5.00, 5.00, '读取速度飙到7450MB/s，开机只要5秒。游戏加载速度提升巨大，之前读图要30秒现在3秒搞定。三星品质值得信赖！', NULL, 0, 28, 5, '2026-05-11 10:00:00'),
(3, 'user_demo', NULL, 24, NULL, 4.80, 5.00, 4.50, 5.00, '2TB大容量装游戏完全够了，不用纠结删哪个。发热控制比980 Pro好，不加散热片也能稳定跑。速度确实快得飞起。', NULL, 0, 14, 2, '2026-05-30 16:00:00'),
(3, 'user_demo', NULL, 24, NULL, 4.70, 4.50, 5.00, 4.50, '装上后整个电脑响应速度快了很多。三星魔术师软件很好用，固件更新和健康检测都很方便。虽然贵点但值这个价。', NULL, 0, 9, 1, '2026-06-05 22:30:00'),
(3, 'user_demo', NULL, 24, NULL, 5.00, 5.00, 5.00, 5.00, 'PCIe 4.0的巅峰之作，顺序读写和随机读写都是顶级水平。视频剪辑和3D渲染素材预览丝般顺滑。', NULL, 0, 6, 0, '2026-06-06 08:00:00');

-- Update product avg_rating and review_count in ecommerce_product
UPDATE ecommerce_product.product SET avg_rating = 4.77, review_count = 3 WHERE id = 13;
UPDATE ecommerce_product.product SET avg_rating = 4.85, review_count = 2 WHERE id = 14;
UPDATE ecommerce_product.product SET avg_rating = 4.80, review_count = 3 WHERE id = 15;
UPDATE ecommerce_product.product SET avg_rating = 4.75, review_count = 2 WHERE id = 16;
UPDATE ecommerce_product.product SET avg_rating = 4.65, review_count = 2 WHERE id = 17;
UPDATE ecommerce_product.product SET avg_rating = 4.77, review_count = 3 WHERE id = 18;
UPDATE ecommerce_product.product SET avg_rating = 4.65, review_count = 2 WHERE id = 19;
UPDATE ecommerce_product.product SET avg_rating = 4.77, review_count = 3 WHERE id = 20;
UPDATE ecommerce_product.product SET avg_rating = 4.75, review_count = 2 WHERE id = 21;
UPDATE ecommerce_product.product SET avg_rating = 4.55, review_count = 2 WHERE id = 22;
UPDATE ecommerce_product.product SET avg_rating = 4.77, review_count = 3 WHERE id = 23;
UPDATE ecommerce_product.product SET avg_rating = 4.88, review_count = 4 WHERE id = 24;
