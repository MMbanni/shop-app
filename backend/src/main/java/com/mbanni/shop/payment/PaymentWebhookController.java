package com.mbanni.shop.payment;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
public class PaymentWebhookController {

    private final PaymentService paymentService;
    private final String webhookSecret;

    public PaymentWebhookController(PaymentService paymentService,
                                    @Value("${stripe.webhook-secret}") String webhookSecret) {
        this.paymentService = paymentService;
        this.webhookSecret = webhookSecret;
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(@RequestBody String payload,
                                                       @RequestHeader("Stripe-Signature") String stripeSignature) {
        Event event;

        try {
            event = Webhook.constructEvent(payload, stripeSignature, webhookSecret);
        } catch (SignatureVerificationException exception) {
            return ResponseEntity.badRequest().body("Invalid Stripe signature");
        }



        if (event.getType().equals("checkout.session.completed")) {
            Session session = (Session) event.getDataObjectDeserializer()
                    .getObject()
                    .orElse(null);

            if (session != null) {
                paymentService.handleCheckoutCompleted(session);
            }
        }

        if ("checkout.session.expired".equals(event.getType())) {
            Session session = (Session) event.getDataObjectDeserializer()
                    .getObject()
                    .orElse(null);

            if (session != null) {
                paymentService.handleCheckoutExpired(session);
            }
        }

        return ResponseEntity.ok("ok");
    }
}
