package com.mbanni.shop.payment;

import com.mbanni.shop.order.Order;
import com.mbanni.shop.order.OrderRepository;
import com.mbanni.shop.order.OrderStatus;
import com.mbanni.shop.order.dto.OrderRecoveryDto;
import org.aspectj.weaver.ast.Or;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
        int maxBatches = 5;
        int batchesProcessed = 0;
        int batchSize = 100;
        long lastId = 0;

        while(batchesProcessed<=maxBatches){

            while (true){
                List<OrderRecoveryDto> orders = orderRepository.findPendingOrderIds(OrderStatus.PENDING, lastId, PageRequest.of(0,batchSize));

                if (orders.isEmpty()) {
                    break;
                }

                for(OrderRecoveryDto dto: orders){
                    try {
                        paymentService.refreshOrderStatus(
                                dto.userId(),
                                dto.orderId()
                        );



                    } catch (RuntimeException e) {
                        log.error("Could not recover order {}", dto.orderId(), e);
                    }
                    lastId = dto.orderId();

                }

            }
            batchesProcessed++;
        }


    }
}
