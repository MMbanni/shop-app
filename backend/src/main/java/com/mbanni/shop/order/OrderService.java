package com.mbanni.shop.order;

import com.mbanni.shop.common.exception.BusinessException;
import com.mbanni.shop.common.exception.ErrorCode;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository){
        this.orderRepository=orderRepository;
    }

    public Order getOrder(Long userId, String sessionId){
        return orderRepository.findByUserIdAndStripeSessionId(userId, sessionId)
                .orElseThrow(()->new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }
}
