package com.dev.dungcony.modules.voucher.services.interfaces;

import com.dev.dungcony.modules.voucher.dtos.req.VoucherUpdateReq;
import com.dev.dungcony.modules.voucher.dtos.res.VoucherRes;

import java.time.Instant;

public interface VoucherUpdateService {
    int checkOrUpdate(Instant now);

    VoucherRes update(String vCode, VoucherUpdateReq req);
}
