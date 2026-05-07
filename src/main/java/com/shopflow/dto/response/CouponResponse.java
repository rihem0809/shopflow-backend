package com.shopflow.dto.response;

import com.shopflow.entity.enums.CouponType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponResponse {
    private Long id;
    private String code;
    private CouponType type;
    private Double value;
    private LocalDateTime expirationDate;
    private Integer maxUsages;
    private Integer currentUsages;
    private Boolean active;
    private Boolean valid;
}