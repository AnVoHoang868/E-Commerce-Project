package com.dev.dungcony.modules.payment.controllers;

import com.dev.dungcony.modules.payment.dtos.req.VietQrTransactionSyncReq;
import com.dev.dungcony.modules.payment.dtos.res.VietQrTokenErrorRes;
import com.dev.dungcony.modules.payment.dtos.res.VietQrTransactionSyncRes;
import com.dev.dungcony.modules.payment.services.interfaces.VietQrCallbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "VietQR Callback")
public class VietQrCallbackController {

    private final VietQrCallbackService vietQrCallbackService;

    @Operation(summary = "VietQR get token", description = "Endpoint cấp Bearer token cho VietQR callback")
    @PostMapping("/vqr/api/token_generate")
    public ResponseEntity<?> generateToken(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        return vietQrCallbackService.createToken(authorization)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(new VietQrTokenErrorRes("FAILED", "INVALID_CREDENTIALS")));
    }

    @Operation(summary = "VietQR transaction sync", description = "Endpoint nhận biến động số dư từ VietQR")
    @PostMapping("/vqr/bank/api/transaction-sync")
    public ResponseEntity<VietQrTransactionSyncRes> syncTransaction(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody VietQrTransactionSyncReq req) {

        if (!vietQrCallbackService.validateBearerToken(authorization)) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(VietQrTransactionSyncRes.failure(
                            "INVALID_TOKEN",
                            "Invalid or expired token"));
        }

        try {
            VietQrTransactionSyncRes res = vietQrCallbackService.syncTransaction(req);
            return ResponseEntity.ok(res);
        } catch (RuntimeException ex) {
            log.warn("Đồng bộ giao dịch VietQR thất bại: {}", ex.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(VietQrTransactionSyncRes.failure(
                            "TRANSACTION_FAILED",
                            ex.getMessage()));
        }
    }
}
