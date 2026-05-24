package com.dev.dungcony.modules.voucher.repositories;

import com.dev.dungcony.modules.voucher.entities.Voucher;
import com.dev.dungcony.modules.voucher.enums.VoucherStatus;
import com.dev.dungcony.modules.voucher.enums.VoucherType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface VoucherRepository extends JpaRepository<Voucher, Integer> {
    Optional<Voucher> findByCode(String code);

    @Modifying
    @Query("""
            UPDATE Voucher v
            SET v.status = CASE
                WHEN v.endAt <= :now THEN :inactiveStatus
                WHEN v.startAt <= :now THEN :activeStatus
            END
            WHERE v.status NOT IN :listNotIn
                 AND (v.endAt <= :now OR v.startAt <= :now)
            """)
    int checkOrUpdate(
            @Param("activeStatus") VoucherStatus activeStatus,
            @Param("inactiveStatus") VoucherStatus inactiveStatus,
            @Param("listNotIn") List<VoucherStatus> notIn,
            @Param("now") Instant now);

    boolean existsByCode(String code);

    List<Voucher> findAllByVoucherTypeAndStatus(VoucherType type, VoucherStatus voucherStatus);
}
