package com.mbanni.shop.order;

import com.mbanni.shop.order.dto.OrderResponseDto;
import com.mbanni.shop.order.dto.OrderSummary;
import com.mbanni.shop.payment.PaymentService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {
    private final OrderService orderService;
    private final PaymentService paymentService;

    public OrderController(OrderService orderService, PaymentService paymentService) {
        this.orderService = orderService;
        this.paymentService = paymentService;
    }

    @GetMapping("/me")
    public List<OrderSummary> history(Authentication auth, @RequestParam(defaultValue = "0") int page) {
        if (page < 0)
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST);
        return orderService.history(Long.parseLong(auth.getName()), page);
    }


    @GetMapping("/by-session/{sessionId}")
    public OrderResponseDto getOrder(@PathVariable String sessionId, Authentication authentication) {
        Long userId = Long.valueOf(authentication.getName());
        Order order = orderService.getOrder(userId, sessionId);

        return new OrderResponseDto(
                order.getId(),
                order.getStatus(),
                order.getPaidAt(),
                order.getReviewNeededAt()

        );

    }

    @PostMapping("/by-session/{sessionId}/refresh")
    public OrderResponseDto refreshOrder(
            @PathVariable String sessionId,
            Authentication authentication
    ) {
        Long userId = Long.valueOf(authentication.getName());

        return paymentService.refreshOrderStatus(userId, sessionId);

    }

}
