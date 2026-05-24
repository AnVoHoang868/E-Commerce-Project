package com.dev.dungcony.modules.payment.services.impl;

import com.dev.dungcony.modules.order.dtos.OrderDto;
import com.dev.dungcony.modules.payment.config.VietQrProperties;
import com.dev.dungcony.modules.payment.dtos.res.PaymentQrRes;
import com.dev.dungcony.modules.payment.exceptions.PaymentException;
import com.dev.dungcony.modules.payment.services.interfaces.VietQrService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class VietQrImpl implements VietQrService {

    private static final String PROVIDER = "VIETQR";
    private static final String NAPAS_GUID = "A000000727";
    private static final String COUNTRY_CODE = "VN";
    private static final String CURRENCY_VND = "704";
    private static final int MAX_PURPOSE_LENGTH = 25;
    private static final int MAX_MERCHANT_NAME_LENGTH = 25;

    private final VietQrProperties properties;

    @Override
    public PaymentQrRes createOrderQr(OrderDto order) {
        if (!properties.configured()) {
            return null;
        }

        BigDecimal amount = toVndAmount(order.finalPrice());
        String amountText = amount.toPlainString();
        String transferContent = buildTransferContent(order.orderCode());
        String merchantName = normalizeText(properties.accountName(), MAX_MERCHANT_NAME_LENGTH);
        String qrContent = buildQrContent(amountText, transferContent, merchantName);
        String qrImageUrl = buildQrImageUrl(amountText, transferContent);

        return new PaymentQrRes(
                PROVIDER,
                properties.bankId().trim(),
                properties.accountNo().trim(),
                properties.accountName(),
                amount,
                transferContent,
                qrContent,
                qrImageUrl);
    }

    private String buildQrContent(String amountText, String transferContent, String merchantName) {
        String beneficiary = tlv("00", properties.bankId().trim())
                + tlv("01", properties.accountNo().trim());

        String merchantAccountInfo = tlv("00", NAPAS_GUID)
                + tlv("01", beneficiary)
                + tlv("02", properties.resolvedServiceCode());

        String additionalData = tlv("08", transferContent);

        String payload = tlv("00", "01")
                + tlv("01", "12")
                + tlv("38", merchantAccountInfo)
                + tlv("53", CURRENCY_VND)
                + tlv("54", amountText)
                + tlv("58", COUNTRY_CODE);

        if (merchantName != null && !merchantName.isBlank()) {
            payload += tlv("59", merchantName);
        }

        payload += tlv("62", additionalData);

        String payloadForCrc = payload + "6304";
        return payloadForCrc + crc16(payloadForCrc);
    }

    private String buildQrImageUrl(String amountText, String transferContent) {
        String url = properties.resolvedImageBaseUrl()
                + "/"
                + encodePath(properties.bankId().trim())
                + "-"
                + encodePath(properties.accountNo().trim())
                + "-"
                + encodePath(properties.resolvedTemplate())
                + ".png"
                + "?amount="
                + encodeQuery(amountText)
                + "&addInfo="
                + encodeQuery(transferContent);

        if (properties.accountName() != null && !properties.accountName().isBlank()) {
            url += "&accountName=" + encodeQuery(properties.accountName().trim());
        }

        return url;
    }

    private BigDecimal toVndAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new PaymentException("Số tiền thanh toán không hợp lệ");
        }

        BigDecimal rounded = amount.setScale(0, RoundingMode.HALF_UP);
        if (rounded.toPlainString().length() > 13) {
            throw new PaymentException("Số tiền VietQR vượt quá giới hạn");
        }
        return rounded;
    }

    private String buildTransferContent(String orderCode) {
        String prefix = properties.contentPrefix() == null ? "" : properties.contentPrefix();
        String raw = (prefix + " " + orderCode).trim();
        String normalized = normalizeText(raw, MAX_PURPOSE_LENGTH);
        if (normalized == null || normalized.isBlank()) {
            return normalizeText(orderCode, MAX_PURPOSE_LENGTH);
        }
        return normalized;
    }

    private String normalizeText(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String withoutAccents = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String cleaned = withoutAccents
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9 ]", "")
                .replaceAll("\\s+", " ")
                .trim();

        if (cleaned.length() <= maxLength) {
            return cleaned;
        }
        return cleaned.substring(0, maxLength).trim();
    }

    private String tlv(String id, String value) {
        if (value == null) {
            value = "";
        }
        int length = value.length();
        if (length > 99) {
            throw new PaymentException("Dữ liệu VietQR vượt quá giới hạn TLV");
        }
        return id + "%02d".formatted(length) + value;
    }

    private String crc16(String payload) {
        int crc = 0xFFFF;
        byte[] bytes = payload.getBytes(StandardCharsets.US_ASCII);
        for (byte b : bytes) {
            crc ^= (b & 0xFF) << 8;
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x8000) != 0) {
                    crc = (crc << 1) ^ 0x1021;
                } else {
                    crc <<= 1;
                }
                crc &= 0xFFFF;
            }
        }
        return "%04X".formatted(crc);
    }

    private String encodePath(String value) {
        return encodeQuery(value).replace("%2F", "");
    }

    private String encodeQuery(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
