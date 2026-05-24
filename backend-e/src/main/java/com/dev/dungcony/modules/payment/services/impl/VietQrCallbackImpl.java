package com.dev.dungcony.modules.payment.services.impl;

import com.dev.dungcony.modules.order.dtos.OrderDto;
import com.dev.dungcony.modules.order.enums.OrderStatus;
import com.dev.dungcony.modules.order.enums.PaymentType;
import com.dev.dungcony.modules.order.services.interfaces.OrderGetService;
import com.dev.dungcony.modules.order.services.interfaces.OrderUpdateService;
import com.dev.dungcony.modules.payment.config.VietQrProperties;
import com.dev.dungcony.modules.payment.dtos.req.VietQrTransactionSyncReq;
import com.dev.dungcony.modules.payment.dtos.res.VietQrTokenRes;
import com.dev.dungcony.modules.payment.dtos.res.VietQrTransactionSyncRes;
import com.dev.dungcony.modules.payment.exceptions.PaymentException;
import com.dev.dungcony.modules.payment.services.interfaces.VietQrCallbackService;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class VietQrCallbackImpl implements VietQrCallbackService {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String BASIC_PREFIX = "Basic ";
    private static final String TOKEN_SUBJECT = "vietqr-callback";
    private static final Pattern ORDER_CODE_PATTERN =
            Pattern.compile("ORD-?\\d{8}-?[A-Z0-9]{4}", Pattern.CASE_INSENSITIVE);

    private final VietQrProperties properties;
    private final OrderGetService orderGetService;
    private final OrderUpdateService orderUpdateService;

    @Override
    public Optional<VietQrTokenRes> createToken(String authorizationHeader) {
        String[] credentials = parseBasicCredentials(authorizationHeader);
        if (credentials == null || credentials.length != 2) {
            return Optional.empty();
        }

        if (!secureEquals(credentials[0], properties.callbackUsername())
                || !secureEquals(credentials[1], properties.callbackPassword())) {
            return Optional.empty();
        }

        Date now = new Date();
        Date expiration = Date.from(Instant.ofEpochMilli(now.getTime())
                .plusSeconds(properties.callbackTokenTtlSeconds()));

        String token = Jwts.builder()
                .setSubject(TOKEN_SUBJECT)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(signingKey(), SignatureAlgorithm.HS512)
                .compact();

        return Optional.of(new VietQrTokenRes(token, "Bearer", properties.callbackTokenTtlSeconds()));
    }

    @Override
    public boolean validateBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return false;
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isBlank()) {
            return false;
        }

        try {
            String subject = Jwts.parserBuilder()
                    .setSigningKey(signingKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
            return TOKEN_SUBJECT.equals(subject);
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("Token VietQR gửi callback không hợp lệ: {}", ex.getMessage());
            return false;
        }
    }

    @Override
    public VietQrTransactionSyncRes syncTransaction(VietQrTransactionSyncReq req) {
        validateCallbackPayload(req);

        String orderCode = resolveOrderCode(req);
        OrderDto order = orderGetService.getDtoByCode(orderCode);

        validateOrder(order, req);
        markOrderPaidIfNeeded(order);

        String refTransactionId = firstNotBlank(req.transactionid(), req.referencenumber(), orderCode);
        log.info("Đã đồng bộ giao dịch VietQR: đơn hàng={}, giao dịch={}, số tiền={}",
                orderCode, refTransactionId, req.amount());

        return VietQrTransactionSyncRes.success(refTransactionId);
    }

    private void validateCallbackPayload(VietQrTransactionSyncReq req) {
        if (req == null) {
            throw new PaymentException("VIETQR_EMPTY_BODY", "Payload VietQR trống");
        }
        if (!properties.configured()) {
            throw new PaymentException("VIETQR_NOT_CONFIGURED", "VietQR chưa được cấu hình");
        }
        if (!"C".equalsIgnoreCase(trimToEmpty(req.transType()))) {
            throw new PaymentException("VIETQR_NOT_CREDIT", "Giao dịch VietQR không phải ghi có");
        }
        if (req.amount() == null || req.amount().signum() <= 0) {
            throw new PaymentException("VIETQR_INVALID_AMOUNT", "Số tiền VietQR không hợp lệ");
        }
        if (!sameValue(properties.accountNo(), req.bankaccount())) {
            throw new PaymentException("VIETQR_WRONG_BANK_ACCOUNT", "Tài khoản nhận không khớp cấu hình VietQR");
        }
    }

    private void validateOrder(OrderDto order, VietQrTransactionSyncReq req) {
        if (order.paymentType() != PaymentType.ONLINE) {
            throw new PaymentException("VIETQR_NOT_ONLINE_ORDER", "Đơn hàng không phải thanh toán online");
        }

        BigDecimal expected = toVndAmount(order.finalPrice());
        BigDecimal actual = toVndAmount(req.amount());
        if (expected.compareTo(actual) != 0) {
            throw new PaymentException(
                    "VIETQR_AMOUNT_MISMATCH",
                    "Số tiền VietQR không khớp đơn hàng");
        }

        if (order.status() == OrderStatus.CANCELLED || order.status() == OrderStatus.RETURNED) {
            throw new PaymentException("VIETQR_ORDER_CLOSED", "Đơn hàng đã đóng, không thể ghi nhận thanh toán");
        }
    }

    private void markOrderPaidIfNeeded(OrderDto order) {
        if (order.status() == OrderStatus.UNPAID) {
            orderUpdateService.userPaidOrder(order.uid(), order.orderCode());
        }
    }

    private String resolveOrderCode(VietQrTransactionSyncReq req) {
        String orderCode = extractOrderCode(req.orderId());
        if (orderCode == null) {
            orderCode = extractOrderCode(req.content());
        }

        if (orderCode == null) {
            throw new PaymentException("VIETQR_ORDER_CODE_NOT_FOUND", "Không tìm thấy mã đơn trong giao dịch VietQR");
        }

        return orderCode;
    }

    private String extractOrderCode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        Matcher matcher = ORDER_CODE_PATTERN.matcher(value.toUpperCase(Locale.ROOT));
        if (!matcher.find()) {
            return null;
        }

        String cleaned = matcher.group().replaceAll("[^A-Z0-9]", "");
        if (cleaned.length() != 15) {
            return null;
        }

        return "ORD-" + cleaned.substring(3, 11) + "-" + cleaned.substring(11);
    }

    private String[] parseBasicCredentials(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BASIC_PREFIX)) {
            return null;
        }

        try {
            String raw = authorizationHeader.substring(BASIC_PREFIX.length()).trim();
            String decoded = new String(Base64.getDecoder().decode(raw), StandardCharsets.UTF_8);
            return decoded.split(":", 2);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private Key signingKey() {
        return Keys.hmacShaKeyFor(properties.callbackTokenSecret().getBytes(StandardCharsets.UTF_8));
    }

    private BigDecimal toVndAmount(BigDecimal amount) {
        return amount.setScale(0, RoundingMode.HALF_UP);
    }

    private boolean secureEquals(String left, String right) {
        byte[] leftBytes = trimToEmpty(left).getBytes(StandardCharsets.UTF_8);
        byte[] rightBytes = trimToEmpty(right).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(leftBytes, rightBytes);
    }

    private boolean sameValue(String left, String right) {
        return trimToEmpty(left).equals(trimToEmpty(right));
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
