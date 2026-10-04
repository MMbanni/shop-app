package com.mbanni.shop.payment;

import com.mbanni.shop.cart.Cart;
import com.mbanni.shop.checkout.*;
import com.mbanni.shop.common.exception.BusinessException;
import com.mbanni.shop.common.exception.ErrorCode;
import com.mbanni.shop.order.Order;
import com.mbanni.shop.order.OrderItem;
import com.mbanni.shop.order.OrderRepository;
import com.mbanni.shop.order.OrderStatus;
import com.mbanni.shop.product.ProductRepository;
import com.mbanni.shop.user.User;
import com.mbanni.shop.user.UserRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class PaymentService {

    private static final Duration CREATION_RETRY_LIMIT = Duration.ofMinutes(5);
    private static final Duration MINIMUM_EXPIRY_REMAINING =Duration.ofMinutes(31);
    private static final int MAX_UNPAID_CHECKOUTS_PER_DAY = 5;

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CheckoutTransactions checkoutTransactions;
    private EntityManager entityManager;

    private final String stripeSecretKey;
    private String frontendUrl;


    public PaymentService(
            UserRepository userRepository,
            OrderRepository orderRepository,
            ProductRepository productRepository,
            CheckoutTransactions checkoutTransactions,
            EntityManager entityManager,
            @Value("${stripe.secret-key}") String stripeSecretKey,
            @Value("${app.frontend-url}") String frontendUrl
    ) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.checkoutTransactions = checkoutTransactions;
        this.stripeSecretKey = stripeSecretKey;
        this.frontendUrl = frontendUrl;
        this.entityManager = entityManager;
    }

    @PostConstruct
    public void setupStripe() {
        Stripe.apiKey = stripeSecretKey;
    } // Run once after creating PaymentService


    public CheckoutResponse createCheckoutSession(Long userId) {
        BeginCheckoutDto incomingOrder = checkoutTransactions.prepareOrResumeCheckout(userId);
        List<Long> productIds = incomingOrder.productIds();
        Order order = incomingOrder.order();


        if (incomingOrder.isPreexisting()) {
            Session session = ensureStripeSession(incomingOrder.order(), userId);

            if (isComplete(session)) {
                throw new BusinessException(ErrorCode.PROCESSING);
            }

            // Resume same checkout with same cart
            if (isOpen(session) && !incomingOrder.hasExpired()) {
                if (incomingOrder.hasSameCart()) {
                    return checkoutResponseFor(session);
                } else {
                    requireExpired(session.getId());

                    Order newOrder = checkoutTransactions.supersedeOrder(
                            userId,
                            order.getId(),
                            productIds
                    );

                    Session newSession = ensureStripeSession(newOrder, userId);
                    return checkoutResponseFor(newSession);
                }

            }
            if (isExpired(session) || incomingOrder.hasExpired()) {
                requireExpired(session.getId());
                Order newOrder = checkoutTransactions.handleOrderExpired(userId, order.getId(), productIds);
                Session newSession = ensureStripeSession(newOrder, userId);
                return checkoutResponseFor(newSession);
            } else if (!isOpen(session) && !isExpired(session)) {
                throw new BusinessException(
                        ErrorCode.ILLEGAL_OPERATION,
                        "Unknown Stripe checkout status"
                );
            }

        }


        Session session = ensureStripeSession(order, userId);

        return checkoutResponseFor(session);
    }

    private Session createStripeSession(Order order, Long userId) {
        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(frontendUrl + "/checkout/success?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(frontendUrl + "/checkout/cancel")
                    .setClientReferenceId(String.valueOf(order.getId()))
                    .setExpiresAt(order.getExpiresAt().getEpochSecond())
                    .putMetadata("orderId", String.valueOf(order.getId()))
                    .putMetadata("userId", String.valueOf(userId))
                    .addAllLineItem(toStripeLineItems(order))
                    .build();

            RequestOptions options = RequestOptions.builder()
                    .setIdempotencyKey("checkout:create:order:" + order.getId())
                    .build();

            return Session.create(params, options);
        } catch (StripeException exception) {
            throw new RuntimeException("Could not create Stripe checkout session", exception);
        }
    }

    private Session ensureStripeSession(Order order, Long userId) {
        if (order.getStripeSessionId() != null) {
            return retrieveStripeSession(order);
        }

        Instant now = Instant.now();

        // Stop creation attempts five minutes after saving the order.
        Instant retryUntil = order.getCreatedAt().plus(CREATION_RETRY_LIMIT);

        boolean retryTimeOver = !now.isBefore(retryUntil);

        if (retryTimeOver) {
            throw new BusinessException(
                    ErrorCode.CHECKOUT_NEEDS_REVIEW,
                    Map.of("orderId", order.getId())
            );
        }

        Session session = createStripeSession(order, userId);
        Order updatedOrder = checkoutTransactions.attachStripeSession(new FinalizeCheckoutDto(
                order.getId(),
                session.getId(),
                session.getUrl()
        ));


        return  retrieveStripeSession(updatedOrder);

    }

    public void cancelCurrentCheckout(Long userId) {
        Order order = orderRepository.findFirstByUser_IdAndStatus(userId, OrderStatus.PENDING)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        Session session = retrieveStripeSession(order);

        if (isComplete(session)) {
            throw new BusinessException(ErrorCode.PROCESSING);
        }

        if (isExpired(session)) {
            checkoutTransactions.expirePendingOrderAndReleaseStock(order, orderProductIds(order));
            return;
        }

        if (!isOpen(session)) {
            throw new BusinessException(ErrorCode.ILLEGAL_OPERATION,
                    "Unknown Stripe Session status: "
                            + session.getStatus()
            );
        }


        requireExpired(order.getStripeSessionId());
        checkoutTransactions.handleOrderCancelled(userId, order, orderProductIds(order));

    }

    @Transactional
    public void handleCheckoutCompleted(Session session) {
        if (!isComplete(session)) {
            return;
        }

        boolean paid = "paid".equals(session.getPaymentStatus());

        boolean free =
                "no_payment_required".equals(session.getPaymentStatus())
                        && Long.valueOf(0L).equals(session.getAmountTotal());

        if (!paid && !free) {
            return;
        }
        Order order = checkoutTransactions.lockUserAndOrderBySession(session.getId());

        if (order.getStatus() != OrderStatus.PENDING) {
            return;
        }

        order.markPaid(session.getId());
        List<OrderItem> orderItems = order.getItems();
        Cart cart = order.getUser().getCart();
        for (OrderItem item : orderItems) {
            cart.removePurchasedQuantity(item.getSourceCartItemId(), item.getQuantity());
        }
    }

    @Transactional
    public void handleCheckoutExpired(Session session) {
        Order order = checkoutTransactions.lockUserAndOrderBySession(session.getId());

        checkoutTransactions.expirePendingOrderAndReleaseStock(order, orderProductIds(order));
    }

    @Transactional
    public Order refreshOrderStatus(Long userId, Long orderId) {

        Order order = checkoutTransactions.lockOrderForRefresh(userId, orderId);

        Session session = ensureStripeSession(order, userId);

        if ("paid".equals(session.getPaymentStatus()) || "no_payment_required".equals(session.getPaymentStatus())) {
            handleCheckoutCompleted(session);

        } else if ("expired".equals(session.getStatus())) {
            handleCheckoutExpired(session);
        }

        return order;
    }

    @Transactional
    public Order refreshOrderStatus(Long userId, String sessionId) {

        Order order = checkoutTransactions.lockOrderForRefresh(userId, sessionId);

        Session session = ensureStripeSession(order, userId);

        if ("paid".equals(session.getPaymentStatus()) || "no_payment_required".equals(session.getPaymentStatus())) {
            handleCheckoutCompleted(session);

        } else if ("expired".equals(session.getStatus())) {
            handleCheckoutExpired(session);
        }

        return order;
    }

    private List<Long> orderProductIds(Order order) {
        return order.getItems().stream()
                .map(OrderItem::getProductIdSnapshot).toList();
    }

    private void requireExpired(String sessionId) {

        try {
            Session session = Session.retrieve(sessionId);
            if (isExpired(session)) {
                return;
            }

            if (isComplete(session)) {
                throw new BusinessException(ErrorCode.PROCESSING);
            }

            if (!isOpen(session)) {
                throw new BusinessException(ErrorCode.ILLEGAL_OPERATION);
            }
            session = session.expire();
            if (!isExpired(session)) {
                throw new BusinessException(ErrorCode.ILLEGAL_OPERATION, "Could not confirm checkout expiration");
            }
        } catch (StripeException e) {
            throw new RuntimeException("Could not verify or expire checkout session ", e);
        }

    }

    private Session retrieveStripeSession(Order order) {
        if (order.getStripeSessionId() == null) {
            throw new BusinessException(
                    ErrorCode.ILLEGAL_OPERATION,
                    "Order has no Stripe session"
            );
        }

        try {
            return Session.retrieve(order.getStripeSessionId());
        } catch (StripeException exception) {
            throw new RuntimeException(
                    "Could not retrieve Stripe checkout session",
                    exception
            );
        }
    }

    private CheckoutResponse checkoutResponseFor(Session session) {
        if(isComplete(session)){
            throw  new BusinessException(ErrorCode.PROCESSING);
        }

        if (!isOpen(session) || session.getUrl() == null) {
            throw new BusinessException(
                    ErrorCode.ILLEGAL_OPERATION,
                    "Checkout is no longer available. Please try checkout again."
            );
        }

        return new CheckoutResponse(session.getUrl());

    }

    private boolean isOpen(Session session) {
        return "open".equals(session.getStatus());
    }

    private boolean isComplete(Session session) {
        return "complete".equals(session.getStatus());
    }

    private boolean isExpired(Session session) {
        return "expired".equals(session.getStatus());
    }

    private List<SessionCreateParams.LineItem> toStripeLineItems(Order order) {
        return order.getItems().stream().sorted(Comparator.comparing(OrderItem::getId))
                .map(item -> SessionCreateParams.LineItem.builder()
                        .setQuantity((long) item.getQuantity())
                        .setPriceData(
                                SessionCreateParams.LineItem.PriceData.builder()
                                        .setCurrency("sek")
                                        .setUnitAmount(toMinorUnit(item.getPrice()))
                                        .setProductData(
                                                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                        .setName(item.getProductNameSnapshot())
                                                        .build()
                                        )
                                        .build()
                        )
                        .build())
                .toList();
    }

    private long toMinorUnit(BigDecimal amount) {
        return amount
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }


}
