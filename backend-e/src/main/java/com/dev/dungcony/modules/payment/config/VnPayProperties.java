package com.dev.dungcony.modules.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** VNPay: {@link #resolvedReturnUrl()} ưu tiên return-url → public base → RAILWAY_PUBLIC_DOMAIN → localhost. */
@ConfigurationProperties(prefix = "vnpay")
public record VnPayProperties(
        String tmnCode,
        String hashSecret,
        String payUrl,
        String returnUrl,
        String publicAppBaseUrl,
        String version,
        String command,
        String orderType
) {
    private static final String RETURN_PATH = "/v1/api/public/payment/vnpay/return";
    private static final String DEFAULT_LOCAL_RETURN = "http://localhost:8080" + RETURN_PATH;
    private static final String DEFAULT_SANDBOX_PAY = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";

    public VnPayProperties {
        if (version == null) version = "2.1.0";
        if (command == null) command = "pay";
        if (orderType == null) orderType = "other";
    }

    public String resolvedPayUrl() {
        if (payUrl != null && !payUrl.isBlank()) {
            return payUrl.trim();
        }
        return DEFAULT_SANDBOX_PAY;
    }

    public String resolvedReturnUrl() {
        if (returnUrl != null && !returnUrl.isBlank()) {
            return returnUrl.trim();
        }
        String base = publicAppBaseUrl != null ? publicAppBaseUrl.trim() : "";
        if (base.isEmpty()) {
            String domain = System.getenv("RAILWAY_PUBLIC_DOMAIN");
            if (domain != null && !domain.isBlank()) {
                domain = domain.replaceFirst("^https?://", "").replaceFirst("/$", "");
                base = "https://" + domain;
            }
        }
        if (!base.isEmpty()) {
            return stripTrailingSlashes(base) + RETURN_PATH;
        }
        return DEFAULT_LOCAL_RETURN;
    }

    private static String stripTrailingSlashes(String s) {
        int end = s.length();
        while (end > 0 && (s.charAt(end - 1) == '/' || s.charAt(end - 1) == '\\')) {
            end--;
        }
        return s.substring(0, end);
    }
}
