package com.dev.dungcony.modules.payment.dtos.res;

import com.fasterxml.jackson.annotation.JsonProperty;

public record VietQrTokenRes(
        @JsonProperty("access_token")
        String accessToken,
        @JsonProperty("token_type")
        String tokenType,
        @JsonProperty("expires_in")
        long expiresIn
) {
}
