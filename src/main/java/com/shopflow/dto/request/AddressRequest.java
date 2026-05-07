package com.shopflow.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressRequest {

    @NotBlank(message = "L'adresse est requise")
    private String street;

    @NotBlank(message = "La ville est requise")
    private String city;

    @NotBlank(message = "Le code postal est requis")
    private String postalCode;

    @NotBlank(message = "Le pays est requis")
    private String country;

    private boolean principal = false;
}