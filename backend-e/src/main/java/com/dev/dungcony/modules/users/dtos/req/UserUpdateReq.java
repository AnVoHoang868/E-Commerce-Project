package com.dev.dungcony.modules.users.dtos.req;

import java.util.UUID;

public record UserUpdateReq(
        UUID id,
        String firstName,
        String lastName,
        String avatar) {

}
