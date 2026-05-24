package com.dev.dungcony.modules.voucher.services.impl;

import com.dev.dungcony.modules.voucher.dtos.req.CreateVoucherReq;
import com.dev.dungcony.modules.voucher.dtos.req.VoucherUpdateReq;
import com.dev.dungcony.modules.voucher.dtos.res.VoucherRes;
import com.dev.dungcony.modules.voucher.entities.Voucher;
import com.dev.dungcony.modules.voucher.enums.VoucherStatus;
import com.dev.dungcony.modules.voucher.enums.VoucherType;
import com.dev.dungcony.modules.voucher.exceptions.VoucherCodeConflig;
import com.dev.dungcony.modules.voucher.exceptions.VoucherNotFoundException;
import com.dev.dungcony.modules.voucher.mappers.VoucherMapper;
import com.dev.dungcony.modules.voucher.repositories.VoucherRepository;
import com.dev.dungcony.modules.voucher.services.interfaces.VoucherCreateService;
import com.dev.dungcony.modules.voucher.services.interfaces.VoucherGetService;
import com.dev.dungcony.modules.voucher.services.interfaces.VoucherUpdateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;


@Service
@RequiredArgsConstructor
public class VoucherImpl implements VoucherCreateService, VoucherGetService, VoucherUpdateService {

    private final VoucherRepository voucherRepository;

    //-------------------------------CREATE-------------------------------------------//
    @Override
    public VoucherRes createVoucher(CreateVoucherReq req) {

        if (voucherRepository.existsByCode(req.code()))
            throw new VoucherCodeConflig();

        Voucher voucher = VoucherMapper.toEntity(req);

        if (req.startAt().isAfter(Instant.now())) {
            voucher.setStatus(VoucherStatus.COMMING_SOON);
        } else {
            voucher.setStatus(VoucherStatus.ACTIVE);
        }

        voucherRepository.save(voucher);

        return VoucherMapper.toRes(voucher);
    }

    //-----------------------------GET VOUCHER-------------------------------------------//

    @Override
    public Voucher getVoucherByCode(String code) {

        Voucher v = voucherRepository.findByCode(code)
                .orElseThrow(VoucherNotFoundException::new);

        if (v.getStatus() != VoucherStatus.ACTIVE)
            throw new VoucherNotFoundException();

        return v;
    }

    @Override
    public List<Voucher> getByTypeAndStatus(VoucherType type, VoucherStatus status) {
        return voucherRepository.findAllByVoucherTypeAndStatus(type, status);
    }

    //----------------------------UPDATE VOUCHER-------------------------------------------//
    @Transactional
    @Override
    public int checkOrUpdate(Instant now) {
        List<VoucherStatus> notChecks = List.of(VoucherStatus.INACTIVE);
        return voucherRepository.checkOrUpdate(
                VoucherStatus.ACTIVE,
                VoucherStatus.INACTIVE,
                notChecks,
                now);
    }

    @Override
    public VoucherRes update(String vCode, VoucherUpdateReq req) {
        Voucher v = getVoucherByCode(vCode);

        if (req.discountType() != null) v.setDiscountType(req.discountType());
        if (req.voucherType() != null) v.setVoucherType(req.voucherType());
        if (req.value() != null) v.setValue(req.value());
        if (req.minOrderAmount() != null) v.setMinOrderAmount(req.minOrderAmount());
        if (req.startAt() != null) v.setStartAt(req.startAt());
        if (req.endAt() != null) v.setEndAt(req.endAt());

        voucherRepository.save(v);

        return VoucherMapper.toRes(v);
    }
}
