package com.ecommerce.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductExcelDTO {
    @ExcelProperty("商品ID")
    private Long id;

    @ExcelProperty("商品名称")
    private String name;

    @ExcelProperty("分类名称")
    private String categoryName;

    @ExcelProperty("价格")
    private BigDecimal price;

    @ExcelProperty("库存")
    private Integer stock;

    @ExcelProperty("描述")
    private String description;

    @ExcelProperty("状态")
    private String statusText;

    @ExcelProperty("规格汇总")
    private String skuSummary;

    @ExcelProperty("销量")
    private Integer sales;

    @ExcelProperty("创建时间")
    private String createTime;
}
