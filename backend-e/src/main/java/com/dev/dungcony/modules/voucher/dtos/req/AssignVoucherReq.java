package com.dev.dungcony.modules.voucher.dtos.req;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record AssignVoucherReq(
        @NotEmpty List<UUID> userIds) {
}
