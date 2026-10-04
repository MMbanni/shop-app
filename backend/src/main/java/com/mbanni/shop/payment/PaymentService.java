package com.mbanni.shop.payment;

import com.mbanni.shop.cart.Cart;
import com.mbanni.shop.checkout.*;
import com.mbanni.shop.checkout.dto.BeginCheckoutDto;
import com.mbanni.shop.checkout.dto.FinalizeCheckoutDto;
import com.mbanni.shop.common.exception.BusinessException;
import com.mbanni.shop.common.exception.ErrorCode;
import com.mbanni.shop.order.Order;
import com.mbanni.shop.order.OrderItem;
import com.mbanni.shop.order.OrderRepository;
import com.mbanni.shop.order.OrderStatus;
import com.mbanni.shop.order.dto.OrderResponseDto;
import com.mbanni.shop.order.dto.OrderSnapshot;
import com.mbanni.shop.payment.dto.StripeSessionSnapshot;
import com.mbanni.shop.product.ProductRepository;

import com.mbanni.shop.user.UserRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
@Transactional(propagation = Propagation.NEVER)
public class PaymentService {

    private static final Duration CREATION_RETRY_LIMIT = Duration.ofMinutes(5);
    private final CheckoutTransactions checkoutTransactions;
    private final StripeCheckoutGateway stripe;
    private  final Clock clock;


    public PaymentService(CheckoutTransactions checkoutTransactions,StripeCheckoutGateway stripe, Clock clock) {
        this.checkoutTransactions = checkoutTransactions;
        this.stripe= stripe;
        this.clock = clock;
    }

    public CheckoutResponse createCheckoutSession(Long userId) {
        BeginCheckoutDto incomingOrder = checkoutTransactions.prepareOrResumeCheckout(userId);

        for (int attempt = 0; attempt < 3; attempt++) {
            OrderSnapshot order = incomingOrder.order();
            StripeSessionSnapshot session = obtainSession(order);

            if (session.isComplete()) {
                checkoutTransactions.applySession(userId, order.orderId(), session, false);

                throw new BusinessException(ErrorCode.PROCESSING);
            }

            if (session.isOpen() && !incomingOrder.hasExpired()
                    && (!incomingOrder.isPreexisting() || incomingOrder.hasSameCart())) {
                OrderSnapshot current = checkoutTransactions.applySession(userId, order.orderId(), session, false);
                if (!current.isPending()) throw new BusinessException(ErrorCode.PROCESSING);
                return checkoutResponseFor(session);
            }
            boolean superseded = session.isOpen() && !incomingOrder.hasExpired() && !incomingOrder.hasSameCart();
            if (session.isOpen()) session = stripe.expire(session.id());
            if (session.isComplete()) {
                checkoutTransactions.applySession(userId, order.orderId(), session, false);

                throw new BusinessException(ErrorCode.PROCESSING);
            }
            if (!session.isExpired()) throw new BusinessException(ErrorCode.PROCESSING);
            incomingOrder = checkoutTransactions.replaceAfterExpiry(userId, order.orderId(), session, superseded);
        }
        throw new BusinessException(ErrorCode.PROCESSING);

    }

    public void cancelCurrentCheckout(Long userId) {
        OrderSnapshot order = checkoutTransactions.loadPendingSnapshot(userId);
        StripeSessionSnapshot session = obtainSession(order);
        boolean cancelled = session.isOpen();

        if (session.isOpen()) session = stripe.expire(session.id());
        checkoutTransactions.applySession(userId, order.orderId(), session, cancelled);
        if (!session.isExpired()) throw new BusinessException(ErrorCode.PROCESSING);

    }

    public void handleCheckoutCompleted(Session session) {
        StripeSessionSnapshot verified = StripeSessionSnapshot.from(session);
        if (verified.isComplete()) checkoutTransactions.applyWebhook(verified);

    }

    public void handleCheckoutExpired(Session session) {
        StripeSessionSnapshot verified = StripeSessionSnapshot.from(session);
        if (verified.isExpired()) checkoutTransactions.applyWebhook(verified);
    }

    public OrderResponseDto refreshOrderStatus(Long userId, Long orderId) {
        return refresh(checkoutTransactions.loadSnapshot(userId, orderId));
    }

    public OrderResponseDto refreshOrderStatus(Long userId, String sessionId) {
        return refresh(checkoutTransactions.loadSnapshotBySession(userId, sessionId));
    }

    private OrderResponseDto refresh(OrderSnapshot order) {
        if (!order.isPending()) return order.toResponse();
        StripeSessionSnapshot session = obtainSession(order);
        return checkoutTransactions.applySession(order.userId(), order.orderId(), session, false).toResponse();
    }


    private CheckoutResponse checkoutResponseFor(StripeSessionSnapshot session) {
        if (!session.isOpen() || session.url() == null) throw new BusinessException(ErrorCode.PROCESSING);
        return new CheckoutResponse(session.url());
    }

    private StripeSessionSnapshot obtainSession(OrderSnapshot order) {
        if (order.stripeSessionId() != null) return stripe.retrieve(order.stripeSessionId());
        if (!order.isPending()) throw new BusinessException(ErrorCode.PROCESSING);

        boolean retryWindowOver = !clock.instant().isBefore(order.createdAt().plus(CREATION_RETRY_LIMIT));

        if (order.needsReview() || retryWindowOver || !order.hasCreationSettings()) {
            String reason = !order.hasCreationSettings()
                    ? "Checkout has no saved request settings; verify its original Stripe session"
                    : "No saved Stripe session after the creation retry window";
            OrderSnapshot current = checkoutTransactions.markNeedsReview(order.userId(), order.orderId(), reason);
            // A webhook might have attached the session while this request was in flight.
            if (current.stripeSessionId() != null) return stripe.retrieve(current.stripeSessionId());
            if (!current.isPending()) throw new BusinessException(ErrorCode.PROCESSING);
            throw new BusinessException(ErrorCode.CHECKOUT_NEEDS_REVIEW, Map.of("orderId", order.orderId()));
        }
        return stripe.create(order);
    }



}
