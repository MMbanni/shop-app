package com.mbanni.shop.payment;

import com.mbanni.shop.order.OrderRepository;
import com.mbanni.shop.order.OrderStatus;
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
    public void runRecovery() {
        int batchSize = 100;

        Instant cutoff = Instant.now().minus(Duration.ofMinutes(5));

        List<OrderRecoveryDto> orders =
                orderRepository.findOrdersMissingSession(
                        OrderStatus.PENDING,
                        cutoff,
                        PageRequest.of(0, batchSize)
                );

        for (OrderRecoveryDto dto : orders) {
            try {
                paymentService.refreshOrderStatus(
                        dto.userId(),
                        dto.orderId()
                );
            } catch (RuntimeException e) {
                log.error(
                        "Could not recover order {}",
                        dto.orderId(),
                        e
                );
            }
        }

        List<OrderRecoveryDto> overdueOrders =
                orderRepository.findOverdueRecoveryOrders(
                        OrderStatus.PENDING,
                        cutoff,
                        PageRequest.of(0, batchSize)
                );

        for (OrderRecoveryDto dto : overdueOrders) {
            try {
                paymentService.refreshOrderStatus(
                        dto.userId(),
                        dto.orderId()
                );
            } catch (RuntimeException e) {
                log.error(
                        "Overdue order {} needs attention",
                        dto.orderId(),
                        e
                );
            }
        }

        long lastId = 0;
        int maxBatches = 3;
        for(int batch = 0; batch <= maxBatches; batch++){

            List<OrderRecoveryDto> pendingOrdersWithSession =
                    orderRepository.findPendingOrdersWithSession(
                            OrderStatus.PENDING,
                            lastId,
                            PageRequest.of(0, 100)
                    );

            if(pendingOrdersWithSession.isEmpty()) break;

            for (OrderRecoveryDto dto : pendingOrdersWithSession) {
                try {
                    paymentService.refreshOrderStatus(
                            dto.userId(),
                            dto.orderId()
                    );
                } catch (RuntimeException e) {
                    log.error(
                            "Overdue order {} needs attention",
                            dto.orderId(),
                            e
                    );
                }
                lastId = dto.orderId();
            }
            if(pendingOrdersWithSession.size()< batchSize) break;
        }

    }
}
