package com.mbanni.shop.checkout;

import com.mbanni.shop.checkout.dto.CheckoutReviewDto;
import com.mbanni.shop.checkout.dto.ReconcileRequestDto;
import com.mbanni.shop.payment.PaymentService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/checkouts")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCheckoutController {
    private final PaymentService payments;

    public AdminCheckoutController(PaymentService payments) { this.payments = payments; }

    @GetMapping("/review")
    public List<CheckoutReviewDto> reviews(@RequestParam(defaultValue = "0") int page) {
        return payments.reviews(page);
    }

    @PostMapping("/{orderId}/reconcile")
    public CheckoutReviewDto reconcile(@PathVariable Long orderId,
                                       @RequestBody ReconcileRequestDto request,
                                       Authentication authentication) {
        return payments.reconcileCheckout(Long.valueOf(authentication.getName()), orderId, request.sessionId());
    }

}
