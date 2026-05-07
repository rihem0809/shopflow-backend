package com.shopflow.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoryRequest {
    
    @NotBlank(message = "Le nom de la catégorie est requis")
    private String name;
    
    private String description;
    
    private Long parentId;  // Pour les sous-catégories
}