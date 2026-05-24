package com.dev.dungcony.modules.payment.controllers;

import com.dev.dungcony.commons.dtos.AccountDetails;
import com.dev.dungcony.commons.dtos.ApiRes;
import com.dev.dungcony.modules.payment.dtos.res.PaymentRes;
import com.dev.dungcony.modules.payment.services.interfaces.VnPayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payments")
public class PaymentController {

    private final VnPayService vnPayService;

    @Operation(summary = "Tạo URL thanh toán VNPay", description = "Tạo link thanh toán VNPay cho đơn hàng ONLINE ở trạng thái UNPAID")
    @PostMapping("/v1/api/user/payment/vnpay/{order-code}")
    public ResponseEntity<ApiRes<PaymentRes>> createPayment(
            @AuthenticationPrincipal AccountDetails account,
            @PathVariable("order-code") String orderCode,
            HttpServletRequest request) {

        String ipAddress = getClientIp(request);

        PaymentRes res = vnPayService.createPaymentUrl(
                account.requireUserUuid(),
                orderCode,
                ipAddress);

        return ResponseEntity.ok(ApiRes.success("Tạo link thanh toán thành công", res));
    }

    @Operation(summary = "VNPay callback (return URL)", description = "Endpoint cho VNPay redirect sau khi thanh toán — không gọi trực tiếp")
    @GetMapping("/v1/api/public/payment/vnpay/return")
    public ResponseEntity<ApiRes<Map<String, Object>>> vnpayReturn(
            @RequestParam Map<String, String> params) {

        boolean success = vnPayService.processPaymentReturn(params);
        String orderCode = params.get("vnp_TxnRef");

        if (success) {
            return ResponseEntity.ok(ApiRes.success("Thanh toán thành công",
                    Map.of("orderCode", orderCode != null ? orderCode : "",
                            "paid", true)));
        }

        return ResponseEntity.ok(ApiRes.error("Thanh toán thất bại",
                Map.of("orderCode", orderCode != null ? orderCode : "",
                        "paid", false)));
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String ip = request.getRemoteAddr();
        return ip != null ? ip : "127.0.0.1";
    }
}
