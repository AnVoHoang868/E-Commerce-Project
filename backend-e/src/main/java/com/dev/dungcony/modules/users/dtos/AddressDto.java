package com.dev.dungcony.modules.users.dtos;

public record AddressDto(
        String country,
        String province,
        String district,
        String street,
        String detail) {

}
