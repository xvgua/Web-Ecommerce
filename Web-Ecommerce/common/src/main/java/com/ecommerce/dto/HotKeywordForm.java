package com.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class HotKeywordForm {
    @NotBlank(message = "关键词不能为空")
    private String keyword;
    private Integer isPinned;
    private Integer sortOrder;
}
