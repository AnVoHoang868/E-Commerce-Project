package com.dev.dungcony.modules.users.dtos.res;

import java.util.UUID;

public record UserRes(
        UUID id,
        String firstName,
        String lastName,
        String avatar
) {
}
