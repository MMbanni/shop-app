package com.mbanni.shop.payment;

import com.mbanni.shop.cart.Cart;
import com.mbanni.shop.cart.CartItem;
import com.mbanni.shop.checkout.CheckoutItemError;
import com.mbanni.shop.checkout.CheckoutResponse;
import com.mbanni.shop.checkout.CheckoutValidationException;
import com.mbanni.shop.common.exception.BusinessException;
import com.mbanni.shop.common.exception.ErrorCode;
import com.mbanni.shop.order.Order;
import com.mbanni.shop.order.OrderItem;
import com.mbanni.shop.order.OrderRepository;
import com.mbanni.shop.order.OrderStatus;
import com.mbanni.shop.product.Product;
import com.mbanni.shop.product.ProductRepository;
import com.mbanni.shop.user.User;
import com.mbanni.shop.user.UserRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PaymentService {

    private static final Duration CHECKOUT_EXPIRY = Duration.ofMinutes(30);
    private static final int MAX_EXPIRED_CHECKOUTS_PER_DAY = 4;


    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    private final String stripeSecretKey;
    private String frontendUrl;


    public PaymentService(
            UserRepository userRepository,
            OrderRepository orderRepository,
            ProductRepository productRepository,
            @Value("${stripe.secret-key}") String stripeSecretKey,
            @Value("${app.frontend-url}") String frontendUrl
    ) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.stripeSecretKey = stripeSecretKey;
        this.frontendUrl = frontendUrl;
    }

    @PostConstruct
    public void setupStripe() {
        Stripe.apiKey = stripeSecretKey;
    } // Run once after creating PaymentService

    @Transactional
    public CheckoutResponse createCheckoutSession(Long userId) {
        Instant now = Instant.now();

        checkForCheckoutAbuse(userId, now);

        Optional<Order> existingPending =
                orderRepository.findFirstByUser_IdAndStatus(userId, OrderStatus.PENDING);

        if (existingPending.isPresent()) {
            Order pendingOrder = existingPending.get();

            if (!pendingOrder.hasExpired(now)) {
                return new CheckoutResponse(pendingOrder.getCheckoutUrl());
            }

            expirePendingOrderAndReleaseStock(pendingOrder);
        }

        User user = getUserOrThrow(userId);
        Cart cart = getValidCartOrThrow(user);

        Instant expiresAt = now.plus(CHECKOUT_EXPIRY);

        Order order = new Order(user, expiresAt);

        reserveStockAndCopyCartItems(cart, order);

        orderRepository.save(order);

        Session session = createStripeSession(order, userId);

        order.setStripeSessionId(session.getId());
        order.setCheckoutUrl(session.getUrl());

        return new CheckoutResponse(session.getUrl());
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


            return Session.create(params);
        } catch (StripeException exception) {
            throw new RuntimeException("Could not create Stripe checkout session", exception);
        }
    }

    @Transactional
    public void cancelCurrentCheckout(Long userId) {
        Order order = orderRepository.findByUserIdAndStatusForUpdate(userId, OrderStatus.PENDING)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() != OrderStatus.PENDING) {
            return;
        }

        expireStripeSessionIfPossible(order);
        releaseStock(order);

        order.markCancelled();
    }


    @Transactional
    public void handleCheckoutCompleted(Session session) {
        Order order = orderRepository.findByStripeSessionId(session.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() == OrderStatus.PAID) {
            return;
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            return;
        }

        order.markPaid(session.getId());

        order.getUser().getCart().getItems().clear();
    }

    @Transactional
    public void handleCheckoutExpired(Session session) {
        String stripeSessionId = session.getId();

        Order order = orderRepository.findByStripeSessionId(stripeSessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        expirePendingOrderAndReleaseStock(order);
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private Cart getValidCartOrThrow(User user) {
        Cart cart = user.getCart();

        if (cart == null || cart.getItems().isEmpty()) {
            throw new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND);
        }

        return cart;
    }

    private void reserveStockAndCopyCartItems(Cart cart, Order order) {
        List<CheckoutItemError> errors = new ArrayList<>();
        List<ProductReservation> reservations = new ArrayList<>();

        // First pass: lock products and validate every cart item
        for (CartItem cartItem : cart.getItems()) {
            Product product = productRepository.findByIdForUpdate(
                    cartItem.getProduct().getId()
            ).orElseThrow(
                    () -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND)
            );

            int requestedQuantity = cartItem.getQuantity();
            int stock = product.getStock();

            if (stock < requestedQuantity) {
                errors.add(
                        new CheckoutItemError(
                                cartItem.getId(),
                                String.valueOf(ErrorCode.CART_ERROR),
                                stock == 0
                                        ? "Product is out of stock."
                                        : "Only " + stock
                                        + (stock == 1
                                        ? " unit is"
                                        : " units are")
                                        + " available.",
                                stock
                        )
                );
            }

            reservations.add(
                    new ProductReservation(
                            product,
                            requestedQuantity
                    )
            );
        }

        // Throw only after every cart item has been checked
        if (!errors.isEmpty()) {
            throw new CheckoutValidationException(errors);
        }

        // Second pass: reserve stock only when the whole cart is valid
        for (ProductReservation reservation : reservations) {
            Product product = reservation.product();
            int quantity = reservation.quantity();

            product.decreaseStock(quantity);

            OrderItem orderItem = new OrderItem(
                    product.getId(),
                    product.getName(),
                    quantity,
                    product.getPrice()
            );

            order.addItem(orderItem);
        }
    }

    private record ProductReservation(
            Product product,
            int quantity
    ) {
    }

    private void releaseStock(Order order) {
        for (OrderItem item : order.getItems()) {
            Product product = productRepository.findByIdForUpdate(item.getProductIdSnapshot())
                    .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

            product.increaseStock(item.getQuantity());
        }
    }

    private void expireStripeSessionIfPossible(Order order) {
        if (order.getStripeSessionId() == null) {
            return;
        }

        try {
            Session session = Session.retrieve(order.getStripeSessionId());

            if ("open".equals(session.getStatus())) {
                session.expire();
            }
        } catch (StripeException exception) {
            throw new RuntimeException("Could not expire Stripe checkout session", exception);
        }
    }

    private void expirePendingOrderAndReleaseStock(Order order) {
        if (order.getStatus() == OrderStatus.PAID) {
            return;
        }

        if (order.getStatus() == OrderStatus.EXPIRED) {
            return;
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            return;
        }

        releaseStock(order);

        order.markExpired();
    }

    private List<SessionCreateParams.LineItem> toStripeLineItems(Order order) {
        return order.getItems().stream()
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

    private void checkForCheckoutAbuse(Long userId, Instant now) {
        Instant since = now.minus(Duration.ofHours(24));

        long expiredCheckouts =
                orderRepository.countByUser_IdAndStatusAndCreatedAtAfter(
                        userId,
                        OrderStatus.EXPIRED,
                        since
                );

        if (expiredCheckouts >= MAX_EXPIRED_CHECKOUTS_PER_DAY) {
            throw new BusinessException(ErrorCode.TOO_MANY_ATTEMPTS);
        }
    }
}
