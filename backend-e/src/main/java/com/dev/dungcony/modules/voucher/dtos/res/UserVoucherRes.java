package com.dev.dungcony.modules.voucher.dtos.res;

import com.dev.dungcony.modules.voucher.enums.UserVoucherStatus;
import com.dev.dungcony.modules.voucher.enums.DiscountType;

import java.math.BigDecimal;
import java.time.Instant;

public record UserVoucherRes(
        String code,
        DiscountType type,
        int value,
        BigDecimal minOrderAmount,
        UserVoucherStatus status,
        Instant endAt) {
}
