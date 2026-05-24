package com.dev.dungcony.modules.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "vietqr")
public record VietQrProperties(
        boolean enabled,
        String bankId,
        String accountNo,
        String accountName,
        String template,
        String contentPrefix,
        String imageBaseUrl,
        String serviceCode,
        String callbackUsername,
        String callbackPassword,
        String callbackTokenSecret,
        long callbackTokenTtlSeconds
) {
    private static final String DEFAULT_TEMPLATE = "compact2";
    private static final String DEFAULT_IMAGE_BASE_URL = "https://img.vietqr.io/image";
    private static final String DEFAULT_SERVICE_CODE = "QRIBFTTA";
    private static final String DEFAULT_CALLBACK_USERNAME = "vietqr";
    private static final String DEFAULT_CALLBACK_PASSWORD = "vietqr";
    private static final String DEFAULT_CALLBACK_TOKEN_SECRET =
            "vietqr-callback-secret-change-me-in-production-2026-05-19-hs512-key";
    private static final long DEFAULT_CALLBACK_TOKEN_TTL_SECONDS = 300;

    public VietQrProperties {
        if (template == null || template.isBlank()) template = DEFAULT_TEMPLATE;
        if (imageBaseUrl == null || imageBaseUrl.isBlank()) imageBaseUrl = DEFAULT_IMAGE_BASE_URL;
        if (serviceCode == null || serviceCode.isBlank()) serviceCode = DEFAULT_SERVICE_CODE;
        if (callbackUsername == null || callbackUsername.isBlank()) callbackUsername = DEFAULT_CALLBACK_USERNAME;
        if (callbackPassword == null || callbackPassword.isBlank()) callbackPassword = DEFAULT_CALLBACK_PASSWORD;
        if (callbackTokenSecret == null || callbackTokenSecret.isBlank()) {
            callbackTokenSecret = DEFAULT_CALLBACK_TOKEN_SECRET;
        }
        if (callbackTokenTtlSeconds <= 0) callbackTokenTtlSeconds = DEFAULT_CALLBACK_TOKEN_TTL_SECONDS;
    }

    public boolean configured() {
        return enabled
                && bankId != null
                && !bankId.isBlank()
                && accountNo != null
                && !accountNo.isBlank();
    }

    public String resolvedImageBaseUrl() {
        String value = imageBaseUrl.trim();
        while (value.endsWith("/") || value.endsWith("\\")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    public String resolvedTemplate() {
        return template.trim();
    }

    public String resolvedServiceCode() {
        return serviceCode.trim();
    }
}
