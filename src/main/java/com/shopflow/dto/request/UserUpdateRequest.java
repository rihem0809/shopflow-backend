package com.shopflow.dto.request;

import lombok.Data;

@Data
public class UserUpdateRequest {
    private String firstName;
    private String lastName;
    private String phone;
    private String address;
    private String city;
    private String region;
    private String postalCode;
    private String country;
}