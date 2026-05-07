package com.shopflow.dto.request;

import lombok.Data;

@Data
public class ProductVariantRequest {
    private String attribute;  // ex: "Taille", "Couleur"
    private String value;      // ex: "M", "Rouge"
    private Integer extraStock;
    private Double priceDelta;
}