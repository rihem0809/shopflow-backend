package com.shopflow.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderRequest {
    @NotNull
    private Long addressId;
    private String couponCode;

    // Information de paiement simulée
    private String paymentMethod; // CARD, PAYPAL, CASH_ON_DELIVERY
}