-- Test products for search testing
INSERT INTO product (id, name, category_id, price, stock, status, sales, avg_rating, review_count) VALUES
(1, '智能手机', 1, 2999.00, 100, 1, 50, 4.5, 20),
(2, '智能手机壳', 2, 29.90, 200, 1, 30, 4.0, 10),
(3, '无线耳机', 3, 599.00, 150, 1, 80, 4.8, 35),
(4, '手机', 1, 999.00, 50, 1, 10, 3.5, 5),
(5, 'iPhone 15', 1, 6999.00, 80, 1, 120, 4.9, 50),
(6, '笔记本电脑', 4, 5999.00, 60, 1, 40, 4.6, 25),
(7, '平板电脑', 4, 3499.00, 40, 0, 5, 4.2, 8);

INSERT INTO category (id, name, parent_id, sort_order) VALUES
(1, '手机', 0, 1),
(2, '手机配件', 0, 2),
(3, '耳机', 0, 3),
(4, '电脑', 0, 4);
