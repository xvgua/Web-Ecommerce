package com.ecommerce.dto;

import lombok.Data;
import java.util.List;

@Data
public class BatchFavoriteRequest {
    private List<BatchFavoriteItem> items;
}
