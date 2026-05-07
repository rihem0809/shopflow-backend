package com.shopflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerProfileDto {
    private Long id;
    private String storeName;
    private String description;
    private String logo;
    private Double rating;
    private Long userId;
    private String userEmail;
    private String userFirstName;
    private String userLastName;
}