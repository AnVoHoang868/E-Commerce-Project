package com.dev.dungcony.modules.voucher.services.interfaces;

import com.dev.dungcony.modules.voucher.dtos.req.CreateVoucherReq;
import com.dev.dungcony.modules.voucher.dtos.res.VoucherRes;

public interface VoucherCreateService {
    VoucherRes createVoucher(CreateVoucherReq req);

}