package com.dev.dungcony.modules.payment.services.interfaces;

import com.dev.dungcony.modules.payment.dtos.res.PaymentRes;

import java.util.Map;
import java.util.UUID;

public interface VnPayService {
    PaymentRes createPaymentUrl(UUID userId, String orderCode, String ipAddress);

    boolean processPaymentReturn(Map<String, String> params);
}
