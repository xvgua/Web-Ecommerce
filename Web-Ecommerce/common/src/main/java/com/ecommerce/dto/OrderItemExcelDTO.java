package com.ecommerce.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderItemExcelDTO {
    @ExcelProperty("订单编号")
    private String orderNo;

    @ExcelProperty("商品名称")
    private String productName;

    @ExcelProperty("规格")
    private String specDesc;

    @ExcelProperty("单价")
    private BigDecimal price;

    @ExcelProperty("数量")
    private Integer quantity;

    @ExcelProperty("小计")
    private BigDecimal subtotal;
}
