package com.ecommerce.dto;

public class CategorySalesDTO {
    private String categoryName;
    private Long sales;

    public CategorySalesDTO() {}

    public CategorySalesDTO(String categoryName, Long sales) {
        this.categoryName = categoryName;
        this.sales = sales;
    }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public Long getSales() { return sales; }
    public void setSales(Long sales) { this.sales = sales; }
}
