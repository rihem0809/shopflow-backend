package com.shopflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {
    private Long id;
    private String customerName;
    private Long customerId;
    private Long productId;
    private String productName;
    private Integer rating;
    private String comment;
    private boolean approved;
    private LocalDateTime createdAt;
}