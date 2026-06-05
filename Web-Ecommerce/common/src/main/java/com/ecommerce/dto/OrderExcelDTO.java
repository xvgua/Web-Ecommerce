package com.ecommerce.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderExcelDTO {
    @ExcelProperty("订单编号")
    private String orderNo;

    @ExcelProperty("用户ID")
    private Long userId;

    @ExcelProperty("收货人")
    private String receiverName;

    @ExcelProperty("联系电话")
    private String receiverPhone;

    @ExcelProperty("收货地址")
    private String receiverAddress;

    @ExcelProperty("商品原价合计")
    private BigDecimal totalAmount;

    @ExcelProperty("优惠券抵扣")
    private BigDecimal couponDiscount;

    @ExcelProperty("商品折扣")
    private BigDecimal discountAmount;

    @ExcelProperty("实付金额")
    private BigDecimal payAmount;

    @ExcelProperty("订单状态")
    private String statusText;

    @ExcelProperty("下单时间")
    private String createTime;

    @ExcelProperty("支付时间")
    private String payTime;

    @ExcelProperty("成交时间")
    private String dealTime;

    @ExcelProperty("退款类型")
    private String refundTypeText;

    @ExcelProperty("退款金额")
    private BigDecimal refundAmount;

    @ExcelProperty("退款原因")
    private String refundReasonText;

    @ExcelProperty("退款状态")
    private String refundStatusText;

    @ExcelProperty("退款申请时间")
    private String refundApplyTime;

    @ExcelProperty("退款处理时间")
    private String refundDealTime;
}
