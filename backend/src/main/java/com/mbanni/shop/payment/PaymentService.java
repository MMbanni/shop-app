package com.mbanni.shop.payment;

import com.mbanni.shop.cart.Cart;
import com.mbanni.shop.cart.CartItem;
import com.mbanni.shop.cart.dto.CartItemProblem;
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
import com.mbanni.shop.product.ProductStatus;
import com.mbanni.shop.user.User;
import com.mbanni.shop.user.UserRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
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

    private static final Duration CHECKOUT_EXPIRY = Duration.ofMinutes(31);
    private static final int MAX_UNPAID_CHECKOUTS_PER_DAY = 5;

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private EntityManager entityManager;

    private final String stripeSecretKey;
    private String frontendUrl;


    public PaymentService(
            UserRepository userRepository,
            OrderRepository orderRepository,
            ProductRepository productRepository,
            EntityManager entityManager,
            @Value("${stripe.secret-key}") String stripeSecretKey,
            @Value("${app.frontend-url}") String frontendUrl
    ) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.stripeSecretKey = stripeSecretKey;
        this.frontendUrl = frontendUrl;
        this.entityManager = entityManager;
    }

    @PostConstruct
    public void setupStripe() {
        Stripe.apiKey = stripeSecretKey;
    } // Run once after creating PaymentService

    @Transactional
    public CheckoutResponse createCheckoutSession(Long userId) {
        User user = lockUserOrThrow(userId);
        Cart cart = getValidCartOrThrow(user);

        // Product Ids of items in cart
        List<Long> productIds = new ArrayList<>(cart.getItems().stream()
                .map(item -> item.getProduct().getId())
                .toList());


        // Possible existing order
        Optional<Order> existingPending = orderRepository.findByUserIdAndStatusForUpdate(
                userId, OrderStatus.PENDING);


        if (existingPending.isPresent()) {
            productIds.addAll(orderProductIds(existingPending.get()));
        }

        Map<Long, Product> lockedProducts = lockProducts(productIds);

        Instant now = Instant.now();

        if (existingPending.isPresent()) {
            Order pendingOrder = existingPending.get();
            Session session = retrieveStripeSession(pendingOrder);

            if ("complete".equals(session.getStatus())) {
                // Wait for payment processing/webhook confirmation.
                throw new BusinessException(ErrorCode.PROCESSING);
            }

            if ("open".equals(session.getStatus())
                    && !pendingOrder.hasExpired(now)
                    &&checkoutMatchesCart(pendingOrder,cart)) {
                return new CheckoutResponse(session.getUrl());

            }
            else if (
                    !"open".equals(session.getStatus())
                    && !"expired".equals(session.getStatus())){
                throw new BusinessException(
                        ErrorCode.ILLEGAL_OPERATION,
                        "Unknown Stripe checkout status"
                );
            }

        }

        checkForCheckoutAbuse(userId, now);

        List<ProductReservation> reservations = validateCart(cart, lockedProducts, existingPending.orElse(null));

        if(existingPending.isPresent()) {
            Order pendingOrder = existingPending.get();

            if (pendingOrder.hasExpired(now)){
                expireCheckout(pendingOrder, lockedProducts);
            } else {
                supersedePendingCheckout(pendingOrder, lockedProducts);
            }
        }

        Instant expiresAt = now.plus(CHECKOUT_EXPIRY);

        Order order = new Order(user, expiresAt);


        reserveStockAndCopyCartItems(order, reservations);

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
        lockUserOrThrow(userId);
        Order order = orderRepository.findByUserIdAndStatusForUpdate(userId, OrderStatus.PENDING)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        Session session = retrieveStripeSession(order);

        if ("complete".equals(session.getStatus())) {
            throw new BusinessException(ErrorCode.PROCESSING);
        }

        if ("expired".equals(session.getStatus())) {
            expirePendingOrderAndReleaseStock(order, lockProducts(orderProductIds(order)));
            return;
        }

        if (!"open".equals(session.getStatus())) {
            throw new BusinessException(ErrorCode.ILLEGAL_OPERATION,
                    "Unknown Stripe Session status: "
                            + session.getStatus()
            );
        }
        try {
            session = session.expire();
        } catch (StripeException e) {
            throw new RuntimeException("Could not expire Stripe session", e);
        }

        requireExpired(session);

        releaseStock(order, lockProducts(orderProductIds(order)));
        order.markCancelled();

    }

    @Transactional
    public void handleCheckoutCompleted(Session session) {
        if (!"complete".equals(session.getStatus())) {
            return;
        }

        boolean paid = "paid".equals(session.getPaymentStatus());

        boolean free =
                "no_payment_required".equals(session.getPaymentStatus())
                        && Long.valueOf(0L).equals(session.getAmountTotal());

        if (!paid && !free) {
            return;
        }
        Order order = lockOrderForSession(session.getId());

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
        Order order = lockOrderForSession(session.getId());

        expirePendingOrderAndReleaseStock(order, lockProducts(orderProductIds(order)));
    }

    @Transactional
    public Order refreshOrderStatus(Long userId, String sessionId) {
        lockUserOrThrow(userId);

        Order order = orderRepository
                .findByUserIdAndStripeSessionId(userId, sessionId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.ORDER_NOT_FOUND)
                );

        if (!order.isPending()) {
            return order;
        }

        Session session = retrieveStripeSession(order);

        if ("paid".equals(session.getPaymentStatus()) || "no_payment_required".equals(session.getPaymentStatus())) {
            handleCheckoutCompleted(session);

        } else if ("expired".equals(session.getStatus())) {
            handleCheckoutExpired(session);
        }

        return order;
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

    private List<ProductReservation> validateCart(Cart cart, Map<Long, Product> lockedProducts, Order pendingOrder) {
        List<CartItemProblem> errors = new ArrayList<>();
        List<ProductReservation> validatedItems = new ArrayList<>();

        for (CartItem cartItem : cart.getItems()) {
            Product product = lockedProducts.get(cartItem.getProduct().getId());

            int requestedQuantity = cartItem.getQuantity();
            int existingQuantity =
                    pendingOrder == null? 0
                    : pendingOrder.getItems().stream()
                            .filter(orderItem -> orderItem.getProductIdSnapshot().equals(product.getId()))
                            .findFirst()
                            .map(OrderItem::getQuantity).orElse(0);

            int stock = product.getStock();

            long availableStock = stock + existingQuantity;

            if (availableStock < requestedQuantity) {
                errors.add(
                        new CartItemProblem(
                                ErrorCode.INSUFFICIENT_STOCK,
                                cartItem.getId(),
                                product.getId(),
                                (int) availableStock,
                                requestedQuantity,
                                null,
                                ErrorCode.INSUFFICIENT_STOCK
                                        .getDefaultMessage()
                        )
                );
            }
            if (cartItem.hasPriceChanged()) {
                errors.add(
                        new CartItemProblem(
                                ErrorCode.PRICE_CHANGED,
                                cartItem.getId(),
                                product.getId(),
                                stock,
                                requestedQuantity,
                                null,
                                "The price of this item has changed from " + cartItem.getPriceWhenAdded() + " to " + cartItem.getProduct().getPrice()
                        )
                );
            }

            if (cartItem.getProduct().getProductStatus() != ProductStatus.ACTIVE) {
                errors.add(
                        new CartItemProblem(
                                ErrorCode.PRODUCT_NOT_AVAILABLE,
                                cartItem.getId(),
                                product.getId(),
                                stock,
                                requestedQuantity,
                                null,
                                ErrorCode.PRODUCT_NOT_AVAILABLE.getDefaultMessage()
                        )
                );
            }

            validatedItems.add(
                    new ProductReservation(
                            cartItem.getId(),
                            product,
                            requestedQuantity,
                            cartItem.calculateUnitPrice()
                    )
            );
        }

        // Throw only after every cart item has been checked
        if (!errors.isEmpty()) {
            throw new CheckoutValidationException(errors);
        }

        return validatedItems;

    }

    private void reserveStockAndCopyCartItems(Order order, List<ProductReservation> reservations) {

        // Second pass: reserve stock only when the whole cart is valid
        for (ProductReservation reservation : reservations) {
            Product product = reservation.product();
            int quantity = reservation.quantity();

            product.decreaseStock(quantity);

            OrderItem orderItem = new OrderItem(
                    reservation.sourceCartItemId,
                    product.getId(),
                    product.getName(),
                    quantity,
                    reservation.unitPrice()
            );

            order.addItem(orderItem);
        }
    }

    // TOTO Extract
    private record ProductReservation(
            Long sourceCartItemId,
            Product product,
            int quantity,
            BigDecimal unitPrice
    ) {
    }

    private Map<Long, Product> lockProducts(Collection<Long> productIds) {
        Map<Long, Product> locked = new HashMap<>();
        List<Long> processed = new ArrayList<>();
        for (Long id : productIds.stream().sorted().toList()) {
            if (processed.contains(id)) continue;
            processed.add(id);
            Product product = productRepository.findByIdForUpdate(id)
                    .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
            entityManager.refresh(product);
            locked.put(id, product);
        }
        return locked;

    }

    private void releaseStock(Order order, Map<Long, Product> lockedProducts) {
        for (OrderItem item : order.getItems()) {
            Product product = lockedProducts.get(item.getProductIdSnapshot());

            product.increaseStock(item.getQuantity());
        }
    }

    private List<Long> orderProductIds(Order order) {
        return order.getItems().stream()
                .map(OrderItem::getProductIdSnapshot).toList();
    }

    private User lockUserOrThrow(Long userId) {
        return userRepository.findByIdForUpdate(userId)
                .orElseThrow(
                        () -> new BusinessException(ErrorCode.USER_NOT_FOUND)
                );
    }

    private Order lockOrderForSession(String sessionId) {
        Long userId = orderRepository
                .findUserIdByStripeSessionId(sessionId)
                .orElseThrow(
                        () -> new BusinessException(ErrorCode.ORDER_NOT_FOUND)
                );

        lockUserOrThrow(userId);

        return orderRepository
                .findByStripeSessionIdForUpdate(sessionId)
                .orElseThrow(
                        () -> new BusinessException(ErrorCode.ORDER_NOT_FOUND)
                );
    }

    private void requireExpired(Session session) {
        if (!"expired".equals(session.getStatus())) {
            throw new BusinessException(ErrorCode.ILLEGAL_OPERATION, "Could not confirm checkout expiration");
        }
    }

    private void expirePendingOrderAndReleaseStock(Order order, Map<Long, Product> lockedProducts) {
        if (order.getStatus() != OrderStatus.PENDING) {
            return;
        }
        releaseStock(order, lockedProducts);

        order.markExpired();
    }

    private void expireCheckout(Order order, Map<Long, Product> lockedProducts) {
        if (order.getStripeSessionId() == null) {
            throw new BusinessException(ErrorCode.ILLEGAL_OPERATION, "Order has no Stripe session");
        }

        try {
            Session session = Session.retrieve(order.getStripeSessionId());
            if ("complete".equals(session.getStatus())) {
                // Keep the reservation while payment is processed.
                throw new BusinessException(ErrorCode.PROCESSING);
            }

            if ("open".equals(session.getStatus())) {
                // Release stock only if Stripe confirms expiration.
                session = session.expire();
                requireExpired(session);
            }

            if (!"expired".equals(session.getStatus())) {
                throw new BusinessException(
                        ErrorCode.ILLEGAL_OPERATION,
                        "Could not confirm checkout expiration"
                );
            }

            expirePendingOrderAndReleaseStock(order, lockedProducts);
        } catch (StripeException e) {
            throw new RuntimeException("Could not verify or expire checkout session ", e);
        }
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
                orderRepository.countByUser_IdAndStatusInAndCreatedAtAfter(
                        userId,
                        List.of(OrderStatus.EXPIRED, OrderStatus.CANCELLED, OrderStatus.SUPERSEDED),
                        since
                );

        if (expiredCheckouts >= MAX_UNPAID_CHECKOUTS_PER_DAY) {
            throw new BusinessException(ErrorCode.TOO_MANY_ATTEMPTS);
        }
    }

    private void supersedePendingCheckout(Order order, Map<Long, Product> lockedProducts) {
        if (order.getStripeSessionId() == null) {
            throw new BusinessException(
                    ErrorCode.ILLEGAL_OPERATION
            );
        }

        try {
            // Note: Simplify
            Session session =
                    Session.retrieve(order.getStripeSessionId());

            if ("complete".equals(session.getStatus())) {
                // Payment may already be completing.
                // Do not release stock or create a replacement.
                throw new BusinessException(
                        ErrorCode.PROCESSING
                );
            }

            if ("expired".equals(session.getStatus())) {
                expirePendingOrderAndReleaseStock(order, lockedProducts);
                return;
            }

            if (!"open".equals(session.getStatus())) {
                throw new BusinessException(
                        ErrorCode.ILLEGAL_OPERATION
                );
            }

            // If payment wins the race, Stripe rejects this call
            // and the database transaction rolls back.
            session = session.expire();
            requireExpired(session);

            releaseStock(order, lockedProducts);
            order.markSuperseded();

        } catch (StripeException exception) {
            throw new RuntimeException(
                    "Could not replace Stripe checkout session",
                    exception
            );
        }
    }


    private boolean checkoutMatchesCart(Order order, Cart cart) {
        List<CartItem> cartItems = cart.getItems();
        List<OrderItem> orderItems = order.getItems();

        if (cartItems.size() != orderItems.size()) {
            return false;
        }

        for (OrderItem orderItem : orderItems) {
            CartItem match = cartItems.stream()
                    .filter(cartItem -> Objects.equals(
                            cartItem.getId(), orderItem.getSourceCartItemId()
                    ))
                    .findFirst().orElse(null);

            if (match == null) return false;

            Product product = match.getProduct();

            if (product.getProductStatus() != ProductStatus.ACTIVE) {
                return false;
            }

            if (!Objects.equals(product.getId(), orderItem.getProductIdSnapshot())) return false;
            if (match.getQuantity() != orderItem.getQuantity()) return false;

            if (match.calculateUnitPrice()
                    .compareTo(orderItem.getPrice()) != 0) {
                return false;
            }

        }
        return true;

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
}
