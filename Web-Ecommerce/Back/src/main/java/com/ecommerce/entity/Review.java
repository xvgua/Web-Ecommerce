package com.ecommerce.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Data
@TableName("review")
public class Review {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String username;
    private String avatar;
    private Long productId;
    private Long orderId;
    private BigDecimal rating;
    private BigDecimal ratingDesc;
    private BigDecimal ratingLogistics;
    private BigDecimal ratingService;
    private String content;

    private String images;

    private Integer isFollowup;
    private Integer likeCount;
    private Integer commentCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(exist = false)
    private String productName;
    @TableField(exist = false)
    private String productImage;
    @TableField(exist = false)
    private java.math.BigDecimal productPrice;
    @TableField(exist = false)
    private Boolean isLiked;
    @TableField(exist = false)
    private Boolean hasFollowUp;
    @TableField(exist = false)
    private List<Review> followUpReviews;

    @JsonProperty("images")
    public List<String> getImageList() {
        if (images == null || images.isEmpty() || images.equals("[]")) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(images, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @JsonProperty("images")
    public void setImageList(List<String> imageList) {
        if (imageList == null || imageList.isEmpty()) {
            this.images = "[]";
            return;
        }
        try {
            this.images = objectMapper.writeValueAsString(imageList);
        } catch (Exception e) {
            this.images = "[]";
        }
    }
}
