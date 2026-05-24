package com.dev.dungcony.modules.payment.dtos.res;

import java.math.BigDecimal;

public record PaymentQrRes(
        String provider,
        String bankId,
        String accountNo,
        String accountName,
        BigDecimal amount,
        String transferContent,
        String qrContent,
        String qrImageUrl
) {
}
