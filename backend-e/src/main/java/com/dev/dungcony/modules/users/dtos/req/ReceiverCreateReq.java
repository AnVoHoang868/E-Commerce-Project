package com.dev.dungcony.modules.users.dtos.req;

import com.dev.dungcony.modules.users.dtos.AddressDto;

public record ReceiverCreateReq(
        String fName,
        String lName,
        String phone,
        AddressDto addr

) {
}
