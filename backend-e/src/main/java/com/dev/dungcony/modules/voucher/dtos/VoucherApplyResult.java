package com.dev.dungcony.modules.voucher.dtos;

import java.math.BigDecimal;

public record VoucherApplyResult(
        Integer userVoucherId,
        String voucherCode,
        BigDecimal discountAmount,
        BigDecimal finalAmount) {

    public static VoucherApplyResult noVoucher(BigDecimal amount) {
        return new VoucherApplyResult(null, null, BigDecimal.ZERO, amount);
    }
}
