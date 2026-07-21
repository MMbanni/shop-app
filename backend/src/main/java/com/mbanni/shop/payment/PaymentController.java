package com.mbanni.shop.payment;

import com.mbanni.shop.checkout.CheckoutResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/checkout")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public CheckoutResponse createCheckout(Authentication authentication) {
        Long userId = Long.valueOf(authentication.getName());
        return paymentService.createCheckoutSession(userId);
    }

    @PostMapping("/checkout/cancel")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<Void> cancelCheckout(Authentication authentication) {
        Long userId = Long.valueOf(authentication.getName());

        paymentService.cancelCurrentCheckout(userId);

        return ResponseEntity.noContent().build();
    }
}
