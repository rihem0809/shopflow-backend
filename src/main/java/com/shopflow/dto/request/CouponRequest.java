package com.shopflow.dto.request;

import com.shopflow.entity.enums.CouponType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponRequest {

    @NotBlank(message = "Le code est requis")
    @Size(min = 3, max = 50, message = "Le code doit contenir entre 3 et 50 caractères")
    private String code;

    @NotNull(message = "Le type est requis")
    private CouponType type;

    @NotNull(message = "La valeur est requise")
    @Positive(message = "La valeur doit être positive")
    private Double value;

    private LocalDateTime expirationDate;

    @Positive(message = "Le nombre d'utilisations maximum doit être positif")
    private Integer maxUsages;
}