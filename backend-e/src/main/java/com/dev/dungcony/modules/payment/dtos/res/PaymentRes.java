package com.dev.dungcony.modules.payment.dtos.res;

public record PaymentRes(
        String orderCode,
        String paymentUrl,
        PaymentQrRes bankTransferQr
) {
    public PaymentRes(String orderCode, String paymentUrl) {
        this(orderCode, paymentUrl, null);
    }
}
