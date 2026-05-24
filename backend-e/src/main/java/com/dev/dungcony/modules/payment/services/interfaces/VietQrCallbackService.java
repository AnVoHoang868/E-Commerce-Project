package com.dev.dungcony.modules.payment.services.interfaces;

import com.dev.dungcony.modules.payment.dtos.req.VietQrTransactionSyncReq;
import com.dev.dungcony.modules.payment.dtos.res.VietQrTokenRes;
import com.dev.dungcony.modules.payment.dtos.res.VietQrTransactionSyncRes;

import java.util.Optional;

public interface VietQrCallbackService {
    Optional<VietQrTokenRes> createToken(String authorizationHeader);

    boolean validateBearerToken(String authorizationHeader);

    VietQrTransactionSyncRes syncTransaction(VietQrTransactionSyncReq req);
}
