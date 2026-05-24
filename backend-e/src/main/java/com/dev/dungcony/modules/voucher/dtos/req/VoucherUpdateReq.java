package com.dev.dungcony.modules.voucher.dtos.req;

import com.dev.dungcony.modules.voucher.enums.DiscountType;
import com.dev.dungcony.modules.voucher.enums.VoucherStatus;
import com.dev.dungcony.modules.voucher.enums.VoucherType;

import java.math.BigDecimal;
import java.time.Instant;

public record VoucherUpdateReq(
        DiscountType discountType,
        VoucherType voucherType,
        VoucherStatus status,
        Integer value,
        BigDecimal minOrderAmount,
        Instant startAt,
        Instant endAt
) {
}
