package com.ecommerce.dto;

import lombok.Data;

@Data
public class ProductQuery {
    private Integer page = 1;
    private Integer pageSize = 20;
    private String keyword;
    private Long categoryId;
    private String sort;
    private Double minPrice;
    private Double maxPrice;
    private Integer status;
    private Long userId;
    private String searchMode;
}
