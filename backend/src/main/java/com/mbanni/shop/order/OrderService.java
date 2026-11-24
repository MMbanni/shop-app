package com.mbanni.shop.order;

import com.mbanni.shop.common.exception.BusinessException;
import com.mbanni.shop.common.exception.ErrorCode;
import com.mbanni.shop.order.dto.OrderSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Order getOrder(Long userId, String sessionId) {
        return orderRepository.findByUserIdAndStripeSessionId(userId, sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<OrderSummary> history(long userId, int page) {
        return orderRepository.findByUser_IdOrderByIdDesc(userId, org.springframework.data.domain.PageRequest.of(page, 10))
                .stream().map(order -> new OrderSummary(order.getId(), order.getStatus().name(), order.getCreatedAt(), order.getTotal())).toList();
    }
}
