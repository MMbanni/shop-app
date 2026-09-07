package com.mbanni.shop.order;

import com.mbanni.shop.order.dto.OrderResponseDto;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService){
        this.orderService=orderService;
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

}
