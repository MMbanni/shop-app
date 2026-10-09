package com.mbanni.shop.payment;

import com.mbanni.shop.order.*;
import com.mbanni.shop.order.dto.OrderRecoveryDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class OrderRecovery {

    private static final Logger log = LoggerFactory.getLogger(OrderRecovery.class);

    private final OrderRecoveryService orderRecoveryService;
    private final PaymentService paymentService;

    public OrderRecovery(OrderRecoveryService orderRecoveryService, PaymentService paymentService) {
        this.orderRecoveryService = orderRecoveryService;
        this.paymentService = paymentService;
    }

    @Scheduled(initialDelay = 5, fixedDelay = 60, timeUnit = TimeUnit.SECONDS)
    public void runRecovery() {

        /* Resolve orders created less than 5 minutes ago */
        int batchSize = 100;

        List<OrderRecoveryDto> orders = orderRecoveryService.claimOrdersWithoutSession(batchSize);

        for (OrderRecoveryDto dto : orders) {
            try {
                paymentService.refreshOrderStatus(dto.userId(), dto.orderId());
            } catch (RuntimeException e) {
                log.error("Could not recover order {}", dto.orderId(), e);
            }
        }

        List<OrderRecoveryDto> overdueOrders = orderRecoveryService.claimOverdueOrders(batchSize);

        for (OrderRecoveryDto dto : overdueOrders) {
            try {
                paymentService.refreshOrderStatus(dto.userId(), dto.orderId());
            } catch (RuntimeException e) {
                log.error("Overdue order {} needs attention", dto.orderId(), e);
            }
        }

        int maxBatches = 3;
        for (int batch = 0; batch < maxBatches; batch++) {

            List<OrderRecoveryDto> pendingOrdersWithSession = orderRecoveryService.claimOrdersWithSession(batchSize);
            if (pendingOrdersWithSession.isEmpty()) {
                break;
            }

            for (OrderRecoveryDto dto : pendingOrdersWithSession) {
                try {
                    paymentService.refreshOrderStatus(dto.userId(), dto.orderId());
                } catch (RuntimeException e) {
                    log.error("Could not reconcile pending order {}", dto.orderId(), e);
                }

            }
        }

    }
}
