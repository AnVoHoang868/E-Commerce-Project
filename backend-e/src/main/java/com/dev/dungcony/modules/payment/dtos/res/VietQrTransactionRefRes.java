package com.dev.dungcony.modules.payment.dtos.res;

import com.fasterxml.jackson.annotation.JsonProperty;

public record VietQrTransactionRefRes(
        @JsonProperty("reftransactionid")
        String refTransactionId
) {
}
