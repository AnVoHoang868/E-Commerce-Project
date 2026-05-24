package com.dev.dungcony.modules.users.dtos.req;

public record AddressUpdateReq(
                String country,
                String province,
                String district,
                String street,
                String detail) {

}
