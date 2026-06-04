-- Refund module migration
-- Extend `order` table with refund-related fields
-- Run this SQL against the ecommerce database

ALTER TABLE `order`
  ADD COLUMN refund_type        TINYINT      DEFAULT NULL COMMENT '退款类型: 1=仅退款, 2=退货退款',
  ADD COLUMN refund_reason      VARCHAR(50)  DEFAULT NULL COMMENT '退款原因枚举',
  ADD COLUMN refund_desc        VARCHAR(500) DEFAULT NULL COMMENT '退款补充说明',
  ADD COLUMN refund_amount      DECIMAL(10,2) DEFAULT NULL COMMENT '退款金额',
  ADD COLUMN refund_item_ids    VARCHAR(500) DEFAULT NULL COMMENT '退款商品项ID(JSON数组)',
  ADD COLUMN refund_status      TINYINT      DEFAULT NULL COMMENT '退款子状态: 0=待审核 1=处理中 2=已拒绝 3=已完成 4=待退货 5=退货中 6=已撤销',
  ADD COLUMN refund_reject_reason VARCHAR(500) DEFAULT NULL COMMENT '拒绝原因',
  ADD COLUMN refund_apply_time  DATETIME     DEFAULT NULL COMMENT '退款申请时间',
  ADD COLUMN refund_deal_time   DATETIME     DEFAULT NULL COMMENT '退款处理时间',
  ADD COLUMN return_logistics_company VARCHAR(100) DEFAULT NULL COMMENT '退货物流公司',
  ADD COLUMN return_logistics_no     VARCHAR(100) DEFAULT NULL COMMENT '退货物流单号';
