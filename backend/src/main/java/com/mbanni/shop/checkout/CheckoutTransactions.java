package com.mbanni.shop.checkout;

import com.mbanni.shop.cart.Cart;
import com.mbanni.shop.cart.CartItem;
import com.mbanni.shop.cart.dto.CartItemProblem;
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
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;



@Service
public class CheckoutTransactions {

    private static final Duration CHECKOUT_EXPIRY = Duration.ofMinutes(31);
    private static final int MAX_UNPAID_CHECKOUTS_PER_DAY = 5;

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final EntityManager entityManager;

    public CheckoutTransactions(
            OrderRepository orderRepository,
            UserRepository userRepository,
            ProductRepository productRepository,
            EntityManager entityManager)
    {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.entityManager = entityManager;
    }

    @Transactional
    public BeginCheckoutDto prepareOrResumeCheckout(Long userId){
        User user = lockUserOrThrow(userId);
        Cart cart = getValidCartOrThrow(user);

        // Product Ids of items in cart
        List<Long> productIds = new ArrayList<>(cart.getItems().stream()
                .map(item -> item.getProduct().getId())
                .toList());

        // Possible existing order
        Optional<Order> preexisting = orderRepository.findByUserIdAndStatusForUpdate(
                userId, OrderStatus.PENDING);

        preexisting.ifPresent(order -> productIds.addAll(getOrderProductIds(order)));
        Map<Long, Product> lockedProducts = lockProducts(productIds);

        Instant now = Instant.now();
        checkForCheckoutAbuse(userId, now);

        List<ProductReservation> reservedProducts = validateCart(cart, lockedProducts, preexisting.orElse(null));


        if (preexisting.isPresent()) {
            Order pendingOrder = preexisting.get();
            return new BeginCheckoutDto(
                    pendingOrder,
                    productIds,
                    true,
                    pendingOrder.hasExpired(now),
                    checkoutMatchesCart(pendingOrder, cart));

        }


        Instant expiresAt = now.plus(CHECKOUT_EXPIRY);

        Order order = new Order(user, expiresAt);

        reserveStockAndCopyCartItemsToOrder(order, reservedProducts);

        orderRepository.save(order);

        return new BeginCheckoutDto(order, productIds,false, false, false);
    }

    @Transactional
    public void attachStripeSession(FinalizeCheckoutDto finalizeCheckoutDto) {
        Order order = orderRepository.findByIdForUpdate(finalizeCheckoutDto.orderId())
                .orElseThrow(()-> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        order.setStripeSessionId(finalizeCheckoutDto.sessionId());
        order.setCheckoutUrl(finalizeCheckoutDto.sessionUrl());
    }

    @Transactional
    public void expirePendingOrderAndReleaseStock(Long orderId, List<Long> productIds) {
        Order order = lockOrderOrThrow(orderId);
        if (order.getStatus() != OrderStatus.PENDING) {
            return;
        }


        releaseStock(order, productIds);
        order.markExpired();
    }


    public User lockUserOrThrow(Long userId) {
        return userRepository.findByIdForUpdate(userId)
                .orElseThrow(
                        () -> new BusinessException(ErrorCode.USER_NOT_FOUND)
                );
    }

    private Cart getValidCartOrThrow(User user) {
        Cart cart = user.getCart();

        if (cart == null || cart.getItems().isEmpty()) {
            throw new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND);
        }

        return cart;
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

    private List<Long> getOrderProductIds(Order order) {
        return order.getItems().stream()
                .map(OrderItem::getProductIdSnapshot).toList();
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

    public Order lockOrderOrThrow(Long orderId){
        return orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(()-> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }

    private void reserveStockAndCopyCartItemsToOrder(Order order, List<ProductReservation> reservations) {

        // Second pass: reserve stock only when the whole cart is valid
        for (ProductReservation reservation : reservations) {
            Product product = reservation.product();
            int quantity = reservation.quantity();

            product.decreaseStock(quantity);

            OrderItem orderItem = new OrderItem(
                    reservation.sourceCartItemId(),
                    product.getId(),
                    product.getName(),
                    quantity,
                    reservation.unitPrice()
            );

            order.addItem(orderItem);
        }
    }

    @Transactional
    public void releaseStock(Order order, List<Long> productIds) {
        Map<Long, Product> lockedProducts = lockProducts(productIds);
        for (OrderItem item : order.getItems()) {
            Product product = lockedProducts.get(item.getProductIdSnapshot());

            product.increaseStock(item.getQuantity());
        }
    }

    @Transactional
    public Order lockUserAndOrderBySession(String sessionId) {
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

    @Transactional
    public Order lockPendingOrderByUserId(Long userId) {
         return orderRepository
                .findByUserIdAndStatusForUpdate(userId, OrderStatus.PENDING)
                .orElseThrow(
                        () -> new BusinessException(ErrorCode.ORDER_NOT_FOUND)
                );

    }

    @Transactional
    public Order supersedeOrder(Long orderId, List<Long> productIds) {
        Order order = lockOrderOrThrow(orderId);

        releaseStock(order, productIds);

        order.markSuperseded();

        return replaceOrder(order, productIds);
    }

    @Transactional
    public Order replaceOrder(Order previousOrder, List<Long> productIds){
        Order order = new Order(previousOrder.getUser(), Instant.now().plus(CHECKOUT_EXPIRY));
        Map<Long, Product> lockedProducts = lockProducts(productIds);
        List<ProductReservation> reservedProducts =
                validateCart(order.getUser().getCart(), lockedProducts, null);

        reserveStockAndCopyCartItemsToOrder(order, reservedProducts);

        return orderRepository.save(order);

    }
    @Transactional
    public void cancelOrder(Order order, List<Long> productIds) {

        Order lockedOrder = lockOrderOrThrow(order.getId());

        if(lockedOrder.getStatus() != OrderStatus.PENDING){
            return;
        }
        releaseStock(lockedOrder, productIds);
        lockedOrder.markCancelled();
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
