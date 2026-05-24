package com.dev.dungcony.modules.users.dtos;

public record ReceiverDto(
        int id,
        AddressDto addr,
        String fName,
        String lName,
        String phone
) {
}
