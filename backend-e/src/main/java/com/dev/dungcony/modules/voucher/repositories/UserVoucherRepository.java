package com.dev.dungcony.modules.voucher.repositories;

import com.dev.dungcony.modules.voucher.entities.UserVoucher;
import com.dev.dungcony.modules.voucher.entities.UserVoucherId;
import com.dev.dungcony.modules.voucher.enums.UserVoucherStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface UserVoucherRepository extends JpaRepository<UserVoucher, UserVoucherId> {
        boolean existsById(UserVoucherId id);

        @Query("""
                        SELECT uv
                        FROM UserVoucher uv
                        JOIN FETCH uv.voucher
                        WHERE uv.id.userId = :userId
                        order by uv.endAt desc
                        """)
        List<UserVoucher> findAllByUserId(@Param("userId") UUID userId);

        @Query("""
                        SELECT uv
                        FROM UserVoucher uv
                        JOIN FETCH uv.voucher
                        WHERE uv.id.userId = :userId AND uv.status = :status
                        order by uv.endAt desc
                        """)
        List<UserVoucher> findAllByUserIdByStatus(
                        @Param("userId") UUID userId,
                        @Param("status") UserVoucherStatus status);

        @Modifying(clearAutomatically = true)
        @Query("""
                          UPDATE UserVoucher uv
                          SET uv.status = case
                              WHEN uv.endAt <= :now THEN 'EXPIRED'
                              ELSE uv.status
                          END
                          WHERE uv.status not in :not_check
                        """)
        int checkOrUpdate(
                        @Param("not_check") List<UserVoucherStatus> notChecks,
                        @Param("now") Instant now);

}
