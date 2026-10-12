package com.mbanni.shop.payment;

import com.mbanni.shop.order.*;
import com.mbanni.shop.order.dto.OrderRecoveryDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

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
        recover(
                10,
                () -> orderRecoveryService.claimOrdersWithoutSession(1)
        );

        recover(
                10,
                () -> orderRecoveryService.claimOverdueOrders(1)
        );

        recover(
                30,
                () -> orderRecoveryService.claimOrdersWithSession(1)
        );
    }

    private void recover(int maxOrders, Supplier<List<OrderRecoveryDto>> claim) {
        for (int i = 0; i < maxOrders; i++) {
            List<OrderRecoveryDto> claimed = claim.get();

            if (claimed.isEmpty()) {
                return;
            }

            OrderRecoveryDto order = claimed.get(0);

            try {
                paymentService.refreshOrderStatus(
                        order.userId(),
                        order.orderId()
                );
            } catch (RuntimeException exception) {
                log.error(
                        "Could not recover order {}",
                        order.orderId(),
                        exception
                );
            }
        }

    }
}
