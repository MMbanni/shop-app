package com.mbanni.shop.order;

import com.mbanni.shop.order.dto.OrderResponseDto;
import com.mbanni.shop.payment.PaymentService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {
    private final OrderService orderService;
    private final PaymentService paymentService;

    public OrderController(OrderService orderService, PaymentService paymentService){
        this.orderService=orderService;
        this.paymentService=paymentService;
    }


    @GetMapping("/by-session/{sessionId}")
    public OrderResponseDto getOrder(@PathVariable String sessionId, Authentication authentication) {
        Long userId = Long.valueOf(authentication.getName());
        Order order = orderService.getOrder(userId, sessionId);

        return new OrderResponseDto(
                order.getId(),
                order.getStatus(),
                order.getPaidAt()

        );

    }

    @PostMapping("/by-session/{sessionId}/refresh")
    public OrderResponseDto refreshOrder(
            @PathVariable String sessionId,
            Authentication authentication
    ) {
        Long userId = Long.valueOf(authentication.getName());

        Order order = paymentService.refreshOrderStatus(userId, sessionId);

        return new OrderResponseDto(
                order.getId(),
                order.getStatus(),
                order.getPaidAt()
        );
    }

}
