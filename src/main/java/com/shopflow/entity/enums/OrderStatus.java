package com.shopflow.entity.enums;

public enum OrderStatus {
    PENDING("En attente"),
    PAID("Payée"),
    PROCESSING("En traitement"),
    SHIPPED("Expédiée"),
    DELIVERED("Livrée"),
    CANCELLED("Annulée");

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}