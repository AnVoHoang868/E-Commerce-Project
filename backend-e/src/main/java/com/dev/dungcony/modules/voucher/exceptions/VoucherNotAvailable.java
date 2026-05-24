package com.dev.dungcony.modules.voucher.exceptions;

import com.dev.dungcony.commons.exceptions.UnProcessableException;

public class VoucherNotAvailable extends UnProcessableException {
    protected VoucherNotAvailable() {
        super("422", "Voucher is not available");
    }
}
