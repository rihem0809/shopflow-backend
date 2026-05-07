package com.shopflow.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class ProductRequest {
    
    @NotBlank(message = "Le nom du produit est requis")
    @Size(min = 3, max = 100, message = "Le nom doit contenir entre 3 et 100 caractères")
    private String name;
    
    @Size(max = 2000, message = "La description ne peut pas dépasser 2000 caractères")
    private String description;
    
    @NotNull(message = "Le prix est requis")
    @Positive(message = "Le prix doit être positif")
    private Double price;
    
    @PositiveOrZero(message = "Le prix promo doit être positif ou zéro")
    private Double promoPrice;
    
    @NotNull(message = "Le stock est requis")
    @Min(value = 0, message = "Le stock doit être supérieur ou égal à 0")
    private Integer stock;
    
    private String image;
    
    private List<Long> categoryIds;
    
    // Variantes du produit
    private List<ProductVariantRequest> variants;
}