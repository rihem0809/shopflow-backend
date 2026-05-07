package com.shopflow.dto.response;

import com.shopflow.entity.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long id;
    private String orderNumber;
    private OrderStatus status;
    private String statusDescription;
    private String customerName;
    private AddressResponse shippingAddress;
    private Double subtotal;
    private Double shippingFee;
    private Double totalTtc;
    private List<OrderItemResponse> items;
    private LocalDateTime orderDate;
    private Boolean isNew;
    private Boolean cancellable;
}