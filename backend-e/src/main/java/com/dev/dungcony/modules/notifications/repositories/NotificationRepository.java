package com.dev.dungcony.modules.notifications.repositories;

import com.dev.dungcony.modules.notifications.dtos.res.NotificationRes;
import com.dev.dungcony.modules.notifications.entities.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Optional<Notification> findByCode(String code);

    long countAllBySenderIdAndReaded(UUID senderId, boolean readed);

    long countAllByForAdminAndReaded(boolean forAdmin, boolean readed);

    // ==================================== QUERY =================================//
    @Modifying
    @Query(
            """
                    UPDATE Notification n
                    SET n.isDelete = true
                    WHERE n.isDelete = false
                          AND n.senderId = :sender_id
                          AND n.code IN (:codes)
                    """
    )
    int deleteByListCodesAndSender(
            @Param("codes") List<String> codes,
            @Param("sender_id") UUID senderId);

    @Modifying
    @Query(
            """
                    UPDATE Notification n
                    SET n.isDelete = true
                    WHERE n.isDelete = false
                          AND n.forAdmin = true
                          AND n.code IN (:codes)
                    """
    )
    int deleteByListCodesForAdmin(
            @Param("codes") List<String> codes);

    @Modifying
    @Query("""
            UPDATE Notification n
            SET n.isDelete = true
            WHERE n.senderId = :sender_id
            """)
    int clearBySender(@Param("sender_id") UUID senderId);

    @Modifying
    @Query("""
            UPDATE Notification n
            SET n.isDelete = true
            WHERE n.isDelete = false
                  AND n.forAdmin = true
            """)
    int clearByAdmin();

    @Query("""
            SELECT new com.dev.dungcony.modules.notifications.dtos.res.NotificationRes(
                n.code,
                u.firstName,
                null,
                n.title,
                n.message
            )
            FROM Notification n
            LEFT JOIN User u ON u.id = n.senderId
            WHERE n.isDelete = false AND n.senderId = :sender_id
            ORDER BY n.createdAt DESC
            """)
    Page<NotificationRes> findAllBySender(
            @Param("sender_id") UUID senderId,
            Pageable pageable
    );

    @Query("""
            SELECT new com.dev.dungcony.modules.notifications.dtos.res.NotificationRes(
                n.code,
                u.firstName,
                null,
                n.title,
                n.message
            )
            FROM Notification n
            left join User u on u.id = n.senderId
            WHERE n.isDelete = false AND n.forAdmin = true
            ORDER BY n.createdAt DESC
            """)
    Page<NotificationRes> findAllForAdmin(
            Pageable pageable
    );

    @Modifying
    @Query("""
            UPDATE Notification n
            SET n.readed = true
            WHERE n.isDelete = false AND n.forAdmin = true
            """)
    long updateReadAllForAdmin();

    @Modifying
    @Query("""
            UPDATE Notification n
            SET n.readed = true
            WHERE n.isDelete = false
                  AND n.senderId = :sender_id
                  AND n.forAdmin = false
            """)
    long updateReadAllUser(
            @Param("sender_id") UUID senderId
    );
}
