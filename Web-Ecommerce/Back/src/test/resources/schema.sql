CREATE TABLE IF NOT EXISTS product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    category_id BIGINT,
    price DECIMAL(10,2) DEFAULT 0,
    stock INT DEFAULT 0,
    description TEXT,
    detail TEXT,
    main_image VARCHAR(500),
    images VARCHAR(2000),
    status TINYINT DEFAULT 1,
    sales INT DEFAULT 0,
    avg_rating DECIMAL(2,1) DEFAULT 0.0,
    review_count INT DEFAULT 0,
    listed_at DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    parent_id BIGINT DEFAULT 0,
    sort_order INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS product_sku (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    spec_name VARCHAR(50),
    spec_value VARCHAR(50),
    price DECIMAL(10,2),
    stock INT DEFAULT 0,
    sales INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    image VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS search_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    keyword VARCHAR(200) NOT NULL,
    user_id BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS hot_keyword (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    keyword VARCHAR(200) NOT NULL,
    search_count INT DEFAULT 0,
    is_manual TINYINT DEFAULT 0,
    is_pinned TINYINT DEFAULT 0,
    sort_order INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_keyword (keyword)
);
