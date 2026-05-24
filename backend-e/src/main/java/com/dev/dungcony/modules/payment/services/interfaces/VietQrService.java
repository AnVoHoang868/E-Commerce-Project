package com.dev.dungcony.modules.payment.services.interfaces;

import com.dev.dungcony.modules.order.dtos.OrderDto;
import com.dev.dungcony.modules.payment.dtos.res.PaymentQrRes;

public interface VietQrService {
    PaymentQrRes createOrderQr(OrderDto order);
}
