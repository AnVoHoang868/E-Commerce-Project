package com.dev.dungcony.modules.payment.dtos.res;

public record VietQrTransactionSyncRes(
        boolean error,
        String errorReason,
        String toastMessage,
        VietQrTransactionRefRes object
) {
    public static VietQrTransactionSyncRes success(String refTransactionId) {
        return new VietQrTransactionSyncRes(
                false,
                null,
                "Transaction processed successfully",
                new VietQrTransactionRefRes(refTransactionId));
    }

    public static VietQrTransactionSyncRes failure(String errorReason, String toastMessage) {
        return new VietQrTransactionSyncRes(true, errorReason, toastMessage, null);
    }
}
