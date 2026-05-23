package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRatingStats {
    private BigDecimal avgRating;
    private long reviewCount;
    private Map<Integer, Long> distribution;
}
