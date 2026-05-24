package com.dev.dungcony.modules.voucher.mappers;

import com.dev.dungcony.modules.voucher.dtos.VoucherDto;
import com.dev.dungcony.modules.voucher.dtos.req.CreateVoucherReq;
import com.dev.dungcony.modules.voucher.dtos.res.VoucherRes;
import com.dev.dungcony.modules.voucher.entities.Voucher;

import java.time.Instant;

public class VoucherMapper {

    public static VoucherDto toDto(Voucher voucher) {
        return new VoucherDto(
                voucher.getId(),
                voucher.getCode(),
                voucher.getDiscountType(),
                voucher.getVoucherType(),
                voucher.getStatus(),
                voucher.getValue(),
                voucher.getMinOrderAmount(),
                voucher.getStartAt(),
                voucher.getEndAt()
        );
    }

    public static VoucherRes toRes(Voucher voucher) {
        return new VoucherRes(
                voucher.getDiscountType(),
                voucher.getVoucherType(),
                voucher.getStatus(),
                voucher.getValue(),
                voucher.getMinOrderAmount(),
                voucher.getEndAt()
        );
    }

    public static Voucher toEntity(CreateVoucherReq req) {
        Voucher voucher = new Voucher();
        voucher.setCode(req.code().trim().toUpperCase());
        voucher.setDiscountType(req.discountType());
        voucher.setVoucherType(req.voucherType());
        voucher.setValue(req.value());
        voucher.setMinOrderAmount(req.minOrderAmount());
        if (req.startAt() == null)
            voucher.setStartAt(Instant.now());
        else
            voucher.setStartAt(req.startAt());
        voucher.setEndAt(req.endAt());
        return voucher;
    }
}
