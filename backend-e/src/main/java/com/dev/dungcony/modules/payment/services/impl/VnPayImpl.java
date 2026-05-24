package com.dev.dungcony.modules.payment.services.impl;

import com.dev.dungcony.modules.order.dtos.OrderDto;
import com.dev.dungcony.modules.order.enums.OrderStatus;
import com.dev.dungcony.modules.order.enums.PaymentType;
import com.dev.dungcony.modules.order.exceptions.OrderConflictException;
import com.dev.dungcony.modules.order.services.interfaces.OrderGetService;
import com.dev.dungcony.modules.order.services.interfaces.OrderUpdateService;
import com.dev.dungcony.modules.payment.config.VnPayProperties;
import com.dev.dungcony.modules.payment.dtos.res.PaymentRes;
import com.dev.dungcony.modules.payment.dtos.res.PaymentQrRes;
import com.dev.dungcony.modules.payment.exceptions.PaymentException;
import com.dev.dungcony.modules.payment.exceptions.PaymentInvalidException;
import com.dev.dungcony.modules.payment.exceptions.PaymentUserIsIncorrectException;
import com.dev.dungcony.modules.payment.services.interfaces.VietQrService;
import com.dev.dungcony.modules.payment.services.interfaces.VnPayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.PostConstruct;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VnPayImpl implements VnPayService {

    private static final DateTimeFormatter VNP_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final VnPayProperties vnPayProperties;
    private final OrderGetService orderGetService;
    private final OrderUpdateService orderUpdateService;
    private final VietQrService vietQrService;

    @PostConstruct
    void logResolvedVnPayUrls() {
        log.info("Đã xác định URL VNPay: payUrl={}, returnUrl={}", vnPayProperties.resolvedPayUrl(), vnPayProperties.resolvedReturnUrl());
    }

    @Override
    public PaymentRes createPaymentUrl(UUID userId, String orderCode, String ipAddress) {
        OrderDto order = orderGetService.getDtoByCode(orderCode);

        if (!order.uid().equals(userId))
            throw new PaymentUserIsIncorrectException();

        if (order.paymentType() != PaymentType.ONLINE)
            throw new PaymentInvalidException();

        if (order.status() != OrderStatus.UNPAID)
            throw new OrderConflictException("Đơn hàng không ở trạng thái chờ thanh toán");

        long amountInVnd = order.finalPrice()
                .multiply(BigDecimal.valueOf(100))
                .longValue();

        ZonedDateTime now = ZonedDateTime.now(VN_ZONE);
        String createDate = now.format(VNP_DATE_FMT);
        String expireDate = now.plusMinutes(15).format(VNP_DATE_FMT);

        Map<String, String> params = createListKeyValue(amountInVnd, orderCode, ipAddress, createDate, expireDate);

        String queryString = buildQueryString(params, true);
        String secureHash = hmacSHA512(vnPayProperties.hashSecret(), queryString);

        String paymentUrl = vnPayProperties.resolvedPayUrl() + "?" + queryString
                + "&vnp_SecureHash=" + secureHash;
        PaymentQrRes bankTransferQr = vietQrService.createOrderQr(order);

        log.info("Đã tạo URL thanh toán VNPay cho đơn hàng {}", orderCode);
        return new PaymentRes(orderCode, paymentUrl, bankTransferQr);
    }

    //Xác thực và xử lý kết quả thanh toán từ VNPay gửi về
    @Override
    @Transactional
    public boolean processPaymentReturn(Map<String, String> params) {
        String vnpSecureHash = params.get("vnp_SecureHash");
        if (vnpSecureHash == null) return false;

        Map<String, String> sortedParams = new TreeMap<>(params);
        sortedParams.remove("vnp_SecureHash");
        sortedParams.remove("vnp_SecureHashType");

        String hashData = buildQueryString(sortedParams, true);
        String calculatedHash = hmacSHA512(vnPayProperties.hashSecret(), hashData);

        //so sánh chữ ký tự tính và chữ ký vnPay trả về
        if (!calculatedHash.equalsIgnoreCase(vnpSecureHash)) {
            log.warn("Chữ ký VNPay không khớp cho vnp_TxnRef={}", params.get("vnp_TxnRef"));
            return false;
        }

        String orderCode = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        String transactionStatus = params.get("vnp_TransactionStatus");

        OrderDto order = orderGetService.getDtoByCode(orderCode);
        if (order == null) {
            log.warn("Kết quả trả về từ VNPay: không tìm thấy đơn hàng {}", orderCode);
            return false;
        }

        if (order.status() != OrderStatus.UNPAID) {
            log.info("Kết quả trả về từ VNPay: đơn hàng {} đã được xử lý trước đó (trạng thái={})", orderCode, order.status());
            return "00".equals(responseCode);
        }

        if ("00".equals(responseCode) && "00".equals(transactionStatus)) {
            orderUpdateService.userPaidOrder(order.uid(), orderCode);
            log.info("Thanh toán VNPay thành công cho đơn hàng {}", orderCode);
            return true;
        }

        log.warn("Thanh toán VNPay thất bại cho đơn hàng {}, mã phản hồi={}", orderCode, responseCode);
        return false;
    }


    // ---------------------------------- PRIVATE -------------------------------- //

    private Map<String, String> createListKeyValue(long amountInVnd, String orderCode, String ipAddress, String createDate, String expireDate) {
        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version", vnPayProperties.version());
        params.put("vnp_Command", vnPayProperties.command());
        params.put("vnp_TmnCode", vnPayProperties.tmnCode());
        params.put("vnp_Amount", String.valueOf(amountInVnd));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", orderCode);
        params.put("vnp_OrderInfo", "Thanh toan don hang " + orderCode);
        params.put("vnp_OrderType", vnPayProperties.orderType());
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", vnPayProperties.resolvedReturnUrl());
        params.put("vnp_IpAddr", ipAddress);
        params.put("vnp_CreateDate", createDate);
        params.put("vnp_ExpireDate", expireDate);

        return params;
    }

    // tạo chuỗi có dạng key=value&key=value
    private String buildQueryString(Map<String, String> params, boolean encode) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                if (!sb.isEmpty()) sb.append('&');
                sb.append(entry.getKey()).append('=');
                sb.append(encode
                        ? URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8)
                        : entry.getValue());
            }
        }
        return sb.toString();
    }

    // Tính toán chữ ký duựa trên khóa bí mật-key và dữ liệu-data
    private String hmacSHA512(String key, String data) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac.init(secretKey);
            byte[] hash = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new PaymentException("Lỗi tạo chữ ký thanh toán");
        }
    }
}
