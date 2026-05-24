package com.dev.dungcony.modules.payment.dtos.req;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.math.BigDecimal;

public record VietQrTransactionSyncReq(
        String bankaccount,
        BigDecimal amount,
        String transType,
        String content,
        String transactionid,
        Long transactiontime,
        String referencenumber,
        @JsonAlias({"orderid", "order_id"})
        String orderId,
        String terminalCode,
        String subTerminalCode,
        String serviceCode,
        String urlLink,
        String sign
) {
}
