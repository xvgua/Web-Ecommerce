-- Refund module migration
-- Extend `order` table with refund-related fields
-- Uses conditional ALTER to be safe on re-run / Docker fresh deploy

DROP PROCEDURE IF EXISTS migrate_refund;
DELIMITER //
CREATE PROCEDURE migrate_refund()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = 'ecommerce' AND TABLE_NAME = 'order' AND COLUMN_NAME = 'refund_type') THEN
        ALTER TABLE `order` ADD COLUMN refund_type TINYINT DEFAULT NULL COMMENT '退款类型: 1=仅退款, 2=退货退款';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = 'ecommerce' AND TABLE_NAME = 'order' AND COLUMN_NAME = 'refund_reason') THEN
        ALTER TABLE `order` ADD COLUMN refund_reason VARCHAR(50) DEFAULT NULL COMMENT '退款原因枚举';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = 'ecommerce' AND TABLE_NAME = 'order' AND COLUMN_NAME = 'refund_desc') THEN
        ALTER TABLE `order` ADD COLUMN refund_desc VARCHAR(500) DEFAULT NULL COMMENT '退款补充说明';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = 'ecommerce' AND TABLE_NAME = 'order' AND COLUMN_NAME = 'refund_amount') THEN
        ALTER TABLE `order` ADD COLUMN refund_amount DECIMAL(10,2) DEFAULT NULL COMMENT '退款金额';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = 'ecommerce' AND TABLE_NAME = 'order' AND COLUMN_NAME = 'refund_item_ids') THEN
        ALTER TABLE `order` ADD COLUMN refund_item_ids VARCHAR(500) DEFAULT NULL COMMENT '退款商品项ID(JSON数组)';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = 'ecommerce' AND TABLE_NAME = 'order' AND COLUMN_NAME = 'refund_status') THEN
        ALTER TABLE `order` ADD COLUMN refund_status TINYINT DEFAULT NULL COMMENT '退款子状态: 0=待审核 1=处理中 2=已拒绝 3=已完成 4=待退货 5=退货中 6=已撤销';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = 'ecommerce' AND TABLE_NAME = 'order' AND COLUMN_NAME = 'refund_reject_reason') THEN
        ALTER TABLE `order` ADD COLUMN refund_reject_reason VARCHAR(500) DEFAULT NULL COMMENT '拒绝原因';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = 'ecommerce' AND TABLE_NAME = 'order' AND COLUMN_NAME = 'refund_apply_time') THEN
        ALTER TABLE `order` ADD COLUMN refund_apply_time DATETIME DEFAULT NULL COMMENT '退款申请时间';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = 'ecommerce' AND TABLE_NAME = 'order' AND COLUMN_NAME = 'refund_deal_time') THEN
        ALTER TABLE `order` ADD COLUMN refund_deal_time DATETIME DEFAULT NULL COMMENT '退款处理时间';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = 'ecommerce' AND TABLE_NAME = 'order' AND COLUMN_NAME = 'return_logistics_company') THEN
        ALTER TABLE `order` ADD COLUMN return_logistics_company VARCHAR(100) DEFAULT NULL COMMENT '退货物流公司';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = 'ecommerce' AND TABLE_NAME = 'order' AND COLUMN_NAME = 'return_logistics_no') THEN
        ALTER TABLE `order` ADD COLUMN return_logistics_no VARCHAR(100) DEFAULT NULL COMMENT '退货物流单号';
    END IF;
END //
DELIMITER ;
CALL migrate_refund();
DROP PROCEDURE IF EXISTS migrate_refund;
