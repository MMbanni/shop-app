package com.mbanni.shop.payment;
import com.google.gson.JsonParseException;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(PaymentWebhookController.class);

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
        } catch (JsonParseException exception) {
            return ResponseEntity.badRequest().body("Invalid Stripe payload");
        }

        if (event == null || event.getType() == null) {
            return ResponseEntity.badRequest().body("Invalid Stripe event");
        }

        boolean handled = switch (event.getType()) {
            case "checkout.session.completed",
                 "checkout.session.expired" -> true;
            default -> false;
        };

        if (!handled) return ResponseEntity.ok("ok");

        var object = event.getDataObjectDeserializer()
                .getObject()
                .orElse(null);

        if (!(object instanceof Session session)) {
            log.error(
                    "Cannot deserialize Stripe event {} ({}). Check webhook API version against SDK version {}.",
                    event.getId(),
                    event.getType(),
                    Stripe.API_VERSION
            );

            return ResponseEntity.internalServerError()
                    .body("Could not decode checkout event");
        }

        switch (event.getType()) {
            case "checkout.session.completed" -> paymentService.handleCheckoutCompleted(session);
            case "checkout.session.expired" -> paymentService.handleCheckoutExpired(session);
        }

        return ResponseEntity.ok("ok");
    }
}
