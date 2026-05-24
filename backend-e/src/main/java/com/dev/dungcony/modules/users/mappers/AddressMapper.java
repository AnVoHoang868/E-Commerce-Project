package com.dev.dungcony.modules.users.mappers;

import com.dev.dungcony.modules.users.dtos.AddressDto;
import com.dev.dungcony.modules.users.entities.Address;

public class AddressMapper {

    public static AddressDto toDto(Address address) {
        return new AddressDto(
                address.getCountry(),
                address.getProvince(),
                address.getDistrict(),
                address.getStreet(),
                address.getDetail());
    }

    public static Address toEntity(AddressDto dto) {
        Address address = new Address();
        address.setCountry(dto.country());
        address.setProvince(dto.province());
        address.setDistrict(dto.district());
        address.setStreet(dto.street());
        address.setDetail(dto.detail());
        return address;
    }
}