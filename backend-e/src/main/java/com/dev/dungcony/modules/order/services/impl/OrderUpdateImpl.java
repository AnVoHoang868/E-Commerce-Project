package com.dev.dungcony.modules.order.services.impl;

import com.dev.dungcony.modules.notifications.services.interfaces.NotificationCreateService;
import com.dev.dungcony.modules.order.entities.Order;
import com.dev.dungcony.modules.order.entities.OrderItem;
import com.dev.dungcony.modules.order.enums.OrderStatus;
import com.dev.dungcony.modules.order.exceptions.OrderConflictException;
import com.dev.dungcony.modules.order.exceptions.OrderUnAuthException;
import com.dev.dungcony.modules.order.exceptions.OrderNotFoundException;
import com.dev.dungcony.modules.order.repositories.OrderRepository;
import com.dev.dungcony.modules.order.services.interfaces.OrderUpdateService;
import com.dev.dungcony.modules.product.services.interfaces.item.ItemUpdateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderUpdateImpl implements OrderUpdateService {

    private final OrderRepository orderRepository;
    private final ItemUpdateService itemUpdateService;
    private final NotificationCreateService notificationCreateService;

    @Transactional
    @Override
    public void userCancelOrder(UUID userId, String orderCode) {
        Order order = orderRepository.findByCode(orderCode)
                .orElseThrow(OrderNotFoundException::new);

        if (!order.getUserId().equals(userId))
            throw new OrderUnAuthException();

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.UNPAID) {
            throw new OrderConflictException("trạng thái hiện tại của đơn hàng không thể hủy");
        }
        order.setStatus(OrderStatus.CANCELLED);

        // trả lại số lượng đơn hàng
        List<OrderItem> orderItems = order.getItems();
        for (OrderItem orderItem : orderItems) {
            itemUpdateService.increase(
                    orderItem.getId().getProductId(),
                    orderItem.getId().getSizeId(),
                    orderItem.getQuantity());
        }

        log.info("Đã hủy đơn hàng {} bởi người dùng {}", orderCode, userId);
        notificationCreateService.userCancelOrder(userId);
    }

    @Transactional
    @Override
    public void userPaidOrder(UUID userId, String orderCode) {
        Order order = orderRepository.findByCode(orderCode)
                .orElseThrow(OrderNotFoundException::new);

        if (!order.getUserId().equals(userId))
            throw new OrderUnAuthException();

        if (order.getStatus() != OrderStatus.UNPAID) {
            throw new OrderConflictException("chỉ đơn hàng chưa thanh toán");
        }

        order.setStatus(OrderStatus.PAID);
        notificationCreateService.userPailOrder(userId);

        log.info("Thanh toán đơn hàng {} thành công cho người dùng {}", orderCode, userId);
    }

    @Transactional
    @Override
    public void userCompleteOrder(UUID userId, String orderCode) {
        Order order = orderRepository.findByCode(orderCode)
                .orElseThrow(OrderNotFoundException::new);

        if (!order.getUserId().equals(userId))
            throw new OrderUnAuthException();

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new OrderConflictException("chỉ đơn hàng đang chờ xác nhận");
        }

        order.setStatus(OrderStatus.CONFIRMED);
        log.info("Người dùng {} xác nhận đơn hàng {}", userId, orderCode);
    }

    @Transactional
    @Override
    public void adminConfirmOrder(String orderCode) {
        Order order = orderRepository.findByCode(orderCode)
                .orElseThrow(OrderNotFoundException::new);

        if (order.getStatus() != OrderStatus.SHIPPING) {
            throw new OrderConflictException("đơn hàng phải đang được giao");
        }

        order.setStatus(OrderStatus.DELIVERED);
        log.info("Quản trị viên xác nhận đã giao đơn hàng {}", orderCode);
    }

    @Transactional
    @Override
    public void adminShippingOrder(String orderCode) {
        Order order = orderRepository.findByCode(orderCode)
                .orElseThrow(OrderNotFoundException::new);

        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new OrderConflictException("đơn hàng phải được xác nhận");
        }

        order.setStatus(OrderStatus.DELIVERED);
        log.info("Quản trị viên cập nhật trạng thái giao hàng cho đơn hàng {}", orderCode);
    }

    @Transactional
    @Override
    public void deliveredOrder(String orderCode) {
        Order order = orderRepository.findByCode(orderCode)
                .orElseThrow(OrderNotFoundException::new);

        if (order.getStatus() != OrderStatus.SHIPPING) {
            throw new OrderConflictException("đơn hàng phải được giao đến nơi");
        }

        order.setStatus(OrderStatus.DELIVERED);
        log.info("Đơn hàng {} đã giao thành công", orderCode);
    }

    @Transactional
    @Override
    public void adminReturnOrder(String orderCode) {
        Order order = orderRepository.findByCode(orderCode)
                .orElseThrow(OrderNotFoundException::new);

        if (order.getStatus() == OrderStatus.RETURNED) {
            return;
        }

        order.setStatus(OrderStatus.RETURNED);
        log.info("Quản trị viên cập nhật đơn hàng {} thành đã hoàn", orderCode);
    }

    @Override
    @Transactional
    public void adminUpdateOrderStatus(String orderCode, OrderStatus nextStatus) {
        Order order = orderRepository.findByCode(orderCode)
                .orElseThrow(OrderNotFoundException::new);

        OrderStatus currentStatus = order.getStatus();
        if (currentStatus == nextStatus) {
            return;
        }

        validateStatusTransition(currentStatus, nextStatus);
        order.setStatus(nextStatus);
    }

    // ---PRIVATE---//
    private void validateStatusTransition(OrderStatus current, OrderStatus next) {
        boolean valid = switch (current) {
            case UNPAID -> next == OrderStatus.PAID;
            case PAID -> next == OrderStatus.PENDING;
            case PENDING -> next == OrderStatus.CONFIRMED || next == OrderStatus.CANCELLED;
            case CONFIRMED -> next == OrderStatus.SHIPPING;
            case SHIPPING -> next == OrderStatus.DELIVERED || next == OrderStatus.RETURNED;
            case DELIVERED -> next == OrderStatus.RETURNED;
            case COMPLETED, CANCELLED, RETURNED -> false;
        };

        if (!valid) {
            throw new OrderConflictException(
                    "Cannot transition from " + current + " to " + next);
        }
    }

}
