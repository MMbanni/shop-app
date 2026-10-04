package com.mbanni.shop.checkout;

import com.mbanni.shop.cart.Cart;
import com.mbanni.shop.cart.CartItem;
import com.mbanni.shop.cart.dto.CartItemProblem;
import com.mbanni.shop.checkout.dto.BeginCheckoutDto;
import com.mbanni.shop.checkout.dto.FinalizeCheckoutDto;
import com.mbanni.shop.checkout.dto.ProductReservation;
import com.mbanni.shop.common.exception.BusinessException;
import com.mbanni.shop.common.exception.ErrorCode;
import com.mbanni.shop.order.Order;
import com.mbanni.shop.order.OrderItem;
import com.mbanni.shop.order.OrderRepository;
import com.mbanni.shop.order.OrderStatus;
import com.mbanni.shop.order.dto.OrderSnapshot;
import com.mbanni.shop.payment.dto.StripeSessionSnapshot;
import com.mbanni.shop.product.Product;
import com.mbanni.shop.product.ProductRepository;
import com.mbanni.shop.product.ProductStatus;
import com.mbanni.shop.user.User;
import com.mbanni.shop.user.UserRepository;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;



@Service
public class CheckoutTransactions {
    private static final Duration CHECKOUT_EXPIRY = Duration.ofMinutes(36);
    private static final int MAX_UNPAID_CHECKOUTS_PER_DAY = 5;

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final EntityManager entityManager;
    private final Clock clock;
    private final String frontendUrl;

    public CheckoutTransactions(OrderRepository orderRepository, UserRepository userRepository,
                                ProductRepository productRepository, EntityManager entityManager,
                                Clock clock, @Value("${app.frontend-url}") String frontendUrl) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.entityManager = entityManager;
        this.clock = clock;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    @Transactional
    public BeginCheckoutDto prepareOrResumeCheckout(Long userId){
        User user = lockUser(userId);

        Optional<Order> pending = orderRepository.findByUserIdAndStatusForUpdate(userId, OrderStatus.PENDING);
        if(pending.isPresent()) return existingCheckout(pending.get(), user.getCart());

        Cart cart = getValidCartOrThrow(user);
        checkForCheckoutAbuse(userId, 0);

        Map<Long, Product> products = lockProducts(cartProductIds(cart));
        Order order = createOrder(user, validateCart(cart, products, null));

        return new BeginCheckoutDto(snapshot(order), false, false, true);
    }

    @Transactional
    public BeginCheckoutDto replaceAfterExpiry(Long userId, Long orderId,
                                               StripeSessionSnapshot session, boolean superseded) {
        if (!session.isExpired()) throw new BusinessException(ErrorCode.PROCESSING);
        User user = lockUser(userId);
        Order previous = lockOrder(orderId, userId);
        attachStripeSession(userId, orderId, session, false);
        if (previous.getStatus() == OrderStatus.PAID) throw new BusinessException(ErrorCode.PROCESSING);

        Optional<Order> current = orderRepository.findByUserIdAndStatusForUpdate(userId, OrderStatus.PENDING);
        if (current.isPresent() && !current.get().getId().equals(orderId)) {
            // Another request already replaced this checkout while Stripe was responding.
            return existingCheckout(current.get(), user.getCart());
        }

        Cart cart = getValidCartOrThrow(user);
        checkForCheckoutAbuse(userId, previous.isPending() ? 1 : 0);
        List<Long> ids = new ArrayList<>(cartProductIds(cart));
        if (previous.isPending()) ids.addAll(orderProductIds(previous));
        Map<Long, Product> products = lockProducts(ids);

        // Lock the complete union once, in ID order. Reuse the same managed products.
        if (previous.isPending()) {
            releaseStock(previous, products);
            if (superseded) previous.markSuperseded();
            else previous.markExpired();
        }
        previous.clearReview();
        Order replacement = createOrder(user, validateCart(cart, products, null));
        return new BeginCheckoutDto(snapshot(replacement), false, false, true);
    }

    @Transactional
    public OrderSnapshot attachStripeSession(Long userId, Long orderId, StripeSessionSnapshot session, boolean cancelled) {
        lockUser(userId);
        Order order = lockOrder(orderId, userId);

        order.setStripeSessionId(session.id());
        order.setCheckoutUrl(session.url());
        return snapshot(order);
    }

    @Transactional
    public void applyWebhook(StripeSessionSnapshot session) {
        Optional<Order> found = orderRepository.findByStripeSessionId(session.id());
        if (found.isEmpty()) {
            // A signed webhook can arrive before the session ID has been saved.
            Long orderId;
            try {
                orderId = Long.valueOf(session.clientReferenceId());
            } catch (NumberFormatException exception) {
                return; // A Checkout Session unrelated to this shop's order references.
            }
            found = orderRepository.findById(orderId);
        }
        if (found.isEmpty()) return;
        Long userId = found.get().getUser().getId();
        Long orderId = found.get().getId();
        lockUser(userId);
        attachStripeSession(userId, orderId, session, false);
    }

    public User lockUser(Long userId) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        entityManager.refresh(user);
        return user;
    }

    public Map<Long, Product> lockProducts(Collection<Long> productIds) {
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

    public Order lockOrder(Long orderId, Long userId){
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(()-> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        entityManager.refresh(order);
        if (!order.getUser().getId().equals(userId)) throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        return order;
    }

    public List<Long> cartProductIds(Cart cart) {
        return cart.getItems().stream()
                .map(item -> item.getProduct().getId()).toList();
    }

    private List<Long> orderProductIds(Order order) {
        return order.getItems().stream()
                .map(OrderItem::getProductIdSnapshot).toList();
    }

    private Cart getValidCartOrThrow(User user) {
        Cart cart = user.getCart();

        if (cart == null || cart.getItems().isEmpty()) {
            throw new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND);
        }

        return cart;
    }

    public List<ProductReservation> validateCart(Cart cart, Map<Long, Product> lockedProducts, Order pendingOrder) {
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

    private void checkForCheckoutAbuse(Long userId, int newlyClosed) {
        long count = orderRepository.countByUser_IdAndStatusInAndCreatedAtAfter(userId,
                List.of(OrderStatus.EXPIRED, OrderStatus.CANCELLED, OrderStatus.SUPERSEDED),
                clock.instant().minus(Duration.ofHours(24)));
        if (count + newlyClosed >= MAX_UNPAID_CHECKOUTS_PER_DAY) {
            throw new BusinessException(ErrorCode.TOO_MANY_ATTEMPTS);
        }
    }
    @Transactional
    private void releaseStock(Order order, Map<Long, Product> products) {
        for (OrderItem item : order.getItems()) products.get(item.getProductIdSnapshot()).increaseStock(item.getQuantity());
    }

    private BeginCheckoutDto existingCheckout(Order order, Cart cart) {
        List<Long> productIds = cart == null ? List.of() : cartProductIds(cart);
        lockProducts(productIds);
        return new BeginCheckoutDto(snapshot(order), true, order.hasExpired(clock.instant()),
                cart != null && checkoutMatchesCart(order, cart));
    }

    private Order createOrder(User user, List<ProductReservation> reservations) {
        Instant now = clock.instant();
        Order order = new Order(user, now, now.plus(CHECKOUT_EXPIRY));
        order.configureCheckout(frontendUrl + "/checkout/success?session_id={CHECKOUT_SESSION_ID}",
                frontendUrl + "/checkout/cancel");
        for (ProductReservation reservation : reservations) {
            Product product = reservation.product();
            product.decreaseStock(reservation.quantity());
            order.addItem(new OrderItem(reservation.sourceCartItemId(), product.getId(), product.getName(),
                    reservation.quantity(), reservation.unitPrice()));
        }
        return orderRepository.save(order);
    }

    private OrderSnapshot snapshot(Order order) {
        return new OrderSnapshot(order.getId(), order.getUser().getId(), order.getStatus(),
                order.getStripeSessionId(), order.getCheckoutUrl(), order.getCreatedAt(), order.getExpiresAt(),
                order.getPaidAt(), order.getTotal(), order.getCheckoutSuccessUrl(), order.getCheckoutCancelUrl(),
                order.getReviewNeededAt(), order.getItems().stream().sorted(Comparator.comparing(OrderItem::getId))
                .map(item -> new OrderSnapshot.Line(item.getId(), item.getProductNameSnapshot(), item.getQuantity(), item.getPrice()))
                .toList());
    }

    @Transactional(readOnly = true)
    public OrderSnapshot loadPendingSnapshot(Long userId) {
        return snapshot(orderRepository.findFirstByUser_IdAndStatus(userId, OrderStatus.PENDING)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND)));
    }

    @Transactional(readOnly = true)
    public OrderSnapshot loadSnapshot(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .filter(found -> found.getUser().getId().equals(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        return snapshot(order);
    }

    @Transactional(readOnly = true)
    public OrderSnapshot loadSnapshotBySession(Long userId, String sessionId) {
        return snapshot(orderRepository.findByUserIdAndStripeSessionId(userId, sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND)));
    }

    @Transactional
    public OrderSnapshot markNeedsReview(Long userId, Long orderId, String reason) {
        lockUser(userId);
        Order order = lockOrder(orderId, userId);
        if (order.isPending() && order.getStripeSessionId() == null) {
            order.requireReview(clock.instant(), reason);
        }
        // The caller throws the API error AFTER this transaction has committed.
        return snapshot(order);
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












}
