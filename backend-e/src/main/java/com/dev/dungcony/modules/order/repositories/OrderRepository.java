package com.dev.dungcony.modules.order.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dev.dungcony.modules.order.dtos.res.OrderSummaryRes;
import com.dev.dungcony.modules.order.entities.Order;
import com.dev.dungcony.modules.order.enums.OrderStatus;

public interface OrderRepository extends JpaRepository<Order, Integer> {

    Optional<Order> findByCode(String orderCode);

    @Query("""
            SELECT o FROM Order o
            LEFT JOIN FETCH o.items
            WHERE o.code = :orderCode
            """)
    Optional<Order> findByOrderCodeWithItems(@Param("orderCode") String orderCode);

    @Query("""
            SELECT new com.dev.dungcony.modules.order.dtos.res.OrderSummaryRes(
                o.code,
                o.status,
                o.finalPrice,
                SIZE(o.items),
                o.createdAt
            )
            FROM Order o
            WHERE o.userId = :userId
            ORDER BY o.createdAt DESC
            """)
    Page<OrderSummaryRes> findAllByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("""
            SELECT new com.dev.dungcony.modules.order.dtos.res.OrderSummaryRes(
                o.code,
                o.status,
                o.finalPrice,
                SIZE(o.items),
                o.createdAt
            )
            FROM Order o
            WHERE o.userId = :userId AND o.status = :status
            ORDER BY o.createdAt DESC
            """)
    Page<OrderSummaryRes> findAllByUserIdAndStatus(
            @Param("userId") UUID userId,
            @Param("status") OrderStatus status,
            Pageable pageable);

    @Query("""
            SELECT new com.dev.dungcony.modules.order.dtos.res.OrderSummaryRes(
                o.code,
                o.status,
                o.finalPrice,
                SIZE(o.items),
                o.createdAt
            )
            FROM Order o
            ORDER BY o.createdAt DESC
            """)
    Page<OrderSummaryRes> findAllOrders(Pageable pageable);

    @Query("""
            SELECT new com.dev.dungcony.modules.order.dtos.res.OrderSummaryRes(
                o.code,
                o.status,
                o.finalPrice,
                SIZE(o.items),
                o.createdAt
            )
            FROM Order o
            WHERE o.status = :status
            ORDER BY o.createdAt DESC
            """)
    Page<OrderSummaryRes> findAllByStatus(@Param("status") OrderStatus status, Pageable pageable);
}
