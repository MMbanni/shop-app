package com.mbanni.shop.order;

import com.mbanni.shop.order.dto.OrderRecoveryDto;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static com.mbanni.shop.common.Constants.*;

@Service
public class OrderRecoveryService {

    private final OrderRepository orderRepository;
    private final Clock clock;

    public OrderRecoveryService(
            Clock clock,
            OrderRepository orderRepository
    ) {
        this.orderRepository = orderRepository;
        this.clock=clock;
    }

    @Transactional
    public List<OrderRecoveryDto> claimOrdersWithoutSession(int batchSize) {
        Instant now = clock.instant();
        Instant cutoff = now.minus(CREATION_RETRY_LIMIT);

        List<Order> orders =
                orderRepository.findOrdersWithoutSession(
                        OrderStatus.PENDING,
                        cutoff,
                        now,
                        PageRequest.of(0, batchSize)
                );

        if (orders.isEmpty()) {
            return List.of();
        }

        for(Order order: orders){
            order.setNextRecoveryCheckAt(now.plus(HIGH_PRIORITY_RECOVERY_TIME));
            order.setRecoveryLeaseUntil(now.plus(LEASE_DURATION));
        }

        return orders.stream()
                .map(order -> new OrderRecoveryDto(
                        order.getId(),
                        order.getUser().getId()
                ))
                .toList();
    }

    @Transactional
    public List<OrderRecoveryDto> claimOverdueOrders(int batchSize) {
        Instant now = clock.instant();
        Instant cutoff = now.minus(CREATION_RETRY_LIMIT);


        List<Order> orders =
                orderRepository.findOverdueOrders(
                        OrderStatus.PENDING,
                        cutoff,
                        now,
                        PageRequest.of(0, batchSize)
                );

        if (orders.isEmpty()) {
            return List.of();
        }

        for(Order order: orders){
            order.setRecoveryLeaseUntil(now.plus(LEASE_DURATION));
        }

        return orders.stream()
                .map(order -> new OrderRecoveryDto(
                        order.getId(),
                        order.getUser().getId()
                ))
                .toList();
    }

    @Transactional
    public List<OrderRecoveryDto> claimOrdersWithSession(int batchSize) {
        Instant now = clock.instant();


        List<Order> orders = orderRepository.findPendingOrdersWithSession(
                        OrderStatus.PENDING,
                        now,
                        PageRequest.of(0, batchSize)
        );

        if (orders.isEmpty()) {
            return List.of();
        }

        for(Order order: orders){
            order.setNextRecoveryCheckAt(now.plus(LOW_PRIORITY_RECOVERY_TIME));
            order.setRecoveryLeaseUntil(now.plus(LEASE_DURATION));
        }

        return orders.stream()
                .map(order -> new OrderRecoveryDto(
                        order.getId(),
                        order.getUser().getId()
                ))
                .toList();
    }
}