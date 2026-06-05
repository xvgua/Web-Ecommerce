package com.ecommerce.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class FeedbackQuery extends PageQuery {
    private Integer type;
    private Integer status;
}
