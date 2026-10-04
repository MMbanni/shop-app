package com.mbanni.shop.payment;

import com.mbanni.shop.order.Order;
import com.mbanni.shop.order.OrderRepository;
import com.mbanni.shop.order.OrderStatus;
import org.aspectj.weaver.ast.Or;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
public class OrderRecovery {

    private static final Logger log = LoggerFactory.getLogger(OrderRecovery.class);
    private static final Duration CREATION_RETRY_LIMIT = Duration.ofMinutes(5);

    private final OrderRepository orderRepository;
    private final PaymentService paymentService;

    public OrderRecovery(OrderRepository orderRepository, PaymentService paymentService) {
        this.orderRepository = orderRepository;
        this.paymentService = paymentService;
    }

    @Scheduled(
            initialDelay = 5,
            fixedDelay = 60,
            timeUnit = TimeUnit.SECONDS
    )
    public void runRecovery(){
        List<Order> orders = orderRepository.findAllByStatus(OrderStatus.PENDING);

        for(Order order: orders){

            try {
                paymentService.refreshOrderStatus(
                        order.getUser().getId(),
                        order.getId()
                );

            } catch (RuntimeException e) {
                log.error("Could not recover order {}", order.getId(), e);
            }
        }

    }
}
