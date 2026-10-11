package com.mbanni.shop.payment;

import com.mbanni.shop.common.exception.PaymentProviderException;
import com.mbanni.shop.order.dto.OrderSnapshot;
import com.mbanni.shop.payment.dto.StripeSessionSnapshot;
import com.stripe.exception.ApiConnectionException;
import com.stripe.exception.RateLimitException;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.checkout.SessionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

@Component
@Transactional(propagation = Propagation.NEVER)
public class StripeCheckoutGateway {
    private final String secretKey;
    private static final Logger log = LoggerFactory.getLogger(StripeCheckoutGateway.class);

    public StripeCheckoutGateway(@Value("${stripe.secret-key}") String secretKey) {
        this.secretKey = secretKey;
    }

    public StripeSessionSnapshot create(OrderSnapshot order){
        try {
            return StripeSessionSnapshot.from(Session.create(creationParams(order), options(
                    "checkout:create:order:" + order.orderId())));
        } catch (StripeException ex) {
            throw paymentError("create", order.orderId(), null, ex);
        }

    }

    SessionCreateParams creationParams(OrderSnapshot order) {
        return SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .setSuccessUrl(order.successUrl())
                .setCancelUrl(order.cancelUrl())
                .setClientReferenceId(order.orderId().toString())
                .setExpiresAt(order.expiresAt().getEpochSecond())
                .putMetadata("orderId", order.orderId().toString())
                .putMetadata("userId", order.userId().toString())
                .addAllLineItem(toStripeLineItems(order))
                .build();
    }

    public StripeSessionSnapshot retrieve(String sessionId) {
        try {
            return StripeSessionSnapshot.from(Session.retrieve(sessionId, options(null)));
        }catch (StripeException ex) {
            throw paymentError("retrieve", null, sessionId, ex);
        }
    }

    public StripeSessionSnapshot expire(String sessionId) {
        try {
            Session session = Session.retrieve(sessionId, options(null));
            if (!"open".equals(session.getStatus())) {
                return StripeSessionSnapshot.from(session);
            }
            return StripeSessionSnapshot.from(session.expire(options(null)));
        } catch (StripeException ex) {
            throw paymentError("expire", null, sessionId, ex);
        }
    }


    private List<SessionCreateParams.LineItem> toStripeLineItems(OrderSnapshot order) {
        return order.items().stream().sorted(Comparator.comparing(item ->item.id()))
                .map(item -> SessionCreateParams.LineItem.builder()
                        .setQuantity((long) item.quantity())
                        .setPriceData(
                                SessionCreateParams.LineItem.PriceData.builder()
                                        .setCurrency("sek")
                                        .setUnitAmount(toMinorUnit(item.unitPrice()))
                                        .setProductData(
                                                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                        .setName(item.productName())
                                                        .build()
                                        )
                                        .build()
                        )
                        .build())
                .toList();
    }

    private RequestOptions options(String idempotencyKey) {
        return RequestOptions.builder()
                .setApiKey(secretKey)
                .setConnectTimeout(5_000)
                .setReadTimeout(10_000)
                .setMaxNetworkRetries(1)
                .setIdempotencyKey(idempotencyKey)
                .build();
    }


    public static long toMinorUnit(BigDecimal amount) {
        return amount.movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).longValueExact();
    }

    private PaymentProviderException paymentError(
            String operation,
            Long orderId,
            String sessionId,
            StripeException ex
    ) {
        Integer status = ex.getStatusCode();

        boolean temporary =
                ex instanceof ApiConnectionException
                        || ex instanceof RateLimitException
                        || (status != null && status >= 500);

        log.error(
                "Stripe {} failed: orderId={}, sessionId={}, status={}, requestId={}",
                operation,
                orderId,
                sessionId,
                status,
                ex.getRequestId(),
                ex
        );

        return new PaymentProviderException(temporary, ex);
    }

}
