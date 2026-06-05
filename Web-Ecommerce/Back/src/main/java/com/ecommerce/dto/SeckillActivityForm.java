package com.ecommerce.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class SeckillActivityForm {
    @NotBlank(message = "活动名称不能为空")
    private String name;

    private String backgroundImage;

    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    @NotEmpty(message = "请选择参与秒杀的商品")
    private List<SeckillProductForm> products;

    @Data
    public static class SeckillProductForm {
        @NotNull(message = "商品不能为空")
        private Long productId;
        private Long skuId;
        @NotNull(message = "秒杀价不能为空")
        private BigDecimal seckillPrice;
        @NotNull(message = "秒杀库存不能为空")
        @Min(value = 1, message = "秒杀库存至少为1")
        private Integer seckillStock;
        private Integer limitPerUser;
    }
}
