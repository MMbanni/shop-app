package com.mbanni.shop.order;

import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository){
        this.orderRepository=orderRepository;
    }

    public Order getOrder(Long userId, String sessionId){
        return orderRepository.findByUserIdAndStripeSessionId(userId, sessionId);
    }
}
