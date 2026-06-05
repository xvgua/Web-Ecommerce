package com.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
public class FeedbackSubmitRequest {
    @NotNull(message = "反馈类型不能为空")
    private Integer type;

    @NotBlank(message = "反馈标题不能为空")
    @Size(max = 200, message = "标题不能超过200字")
    private String title;

    @NotBlank(message = "反馈内容不能为空")
    @Size(max = 2000, message = "内容不能超过2000字")
    private String content;

    @Size(max = 100, message = "联系方式不能超过100字")
    private String contact;

    @Size(max = 3, message = "最多上传3张截图")
    private List<String> images;
}
