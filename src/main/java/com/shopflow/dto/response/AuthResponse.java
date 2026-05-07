package com.shopflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    private String email;
    private String role;
    private String firstName;
    private String lastName;
    private Boolean active;
    private SellerProfileDto sellerProfile;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SellerProfileDto {
        private Long id;
        private String storeName;
        private String description;
        private Double rating;
        private String logo;
    }
}