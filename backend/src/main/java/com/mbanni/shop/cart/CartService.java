package com.mbanni.shop.cart;

import com.mbanni.shop.cart.dto.CartItemProblem;
import com.mbanni.shop.cart.dto.CartResponseDto;
import com.mbanni.shop.cart.mapper.CartMapper;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CartService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CartMapper cartMapper;


    // Inject dependencies
    public CartService(
            ProductRepository productRepository,
            UserRepository userRepository,
            OrderRepository orderRepository,
            CartMapper cartMapper) {
        this.productRepository = productRepository;
        this.orderRepository=orderRepository;
        this.userRepository = userRepository;
        this.cartMapper = cartMapper;
    }

    @Transactional(readOnly = true)
    public CartResponseDto getCart(Long userId) {
        User user = findUserOrThrow(userId);
        return cartMapper.toResponse(user.getCart());
    }

    @Transactional
    public void addToCart(Long userId, Long productId, int quantity) {
        User user = findUserForUpdateOrThrow(userId);

        Cart cart = user.getCart();

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        if(product.getProductStatus() != ProductStatus.ACTIVE){
            throw new BusinessException(ErrorCode.PRODUCT_NOT_AVAILABLE);
        }
        CartItem existingItem = cart.findItemByProductId(productId);

        int existingQuantity = existingItem == null ? 0 : existingItem.getQuantity();
        int requestedQuantity = existingQuantity + quantity;
        long availableStock = stockAvailableForUser(userId,product);


        if(requestedQuantity > availableStock){
            throw insufficientStock(existingItem, product, requestedQuantity, availableStock);
        }

        cart.addItem(product, quantity);
    }

    @Transactional
    public void removeFromCart(Long userId, Long cartItemId) {
        User user = findUserForUpdateOrThrow(userId);

        Cart cart = user.getCart();
        cart.removeAll(cartItemId);
    }

    @Transactional
    public void updateCart(Long userId, Long cartItemId, int quantity) {
        User user = findUserForUpdateOrThrow(userId);
        Cart cart = user.getCart();

        if (quantity == 0) {
            return;
        }

        CartItem cartItem = cart.findItemById(cartItemId);
        if(cartItem == null) {
            throw new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND);
        }

        if (quantity < 0) {
            cart.removeItem(cartItemId, Math.abs(quantity));
            return;
        }

        Product product = cartItem.getProduct();
        if(product.getProductStatus() != ProductStatus.ACTIVE){
            throw new BusinessException(ErrorCode.PRODUCT_NOT_AVAILABLE);
        }

        int requestedQuantity =
                cartItem.getQuantity() + quantity;
        long availableStock = stockAvailableForUser(userId, product);

        if (requestedQuantity > availableStock) {
            throw insufficientStock(
                    cartItem,
                    product,
                    requestedQuantity,
                    availableStock

            );
        }

        cart.addItem( product, quantity );
    }

    @Transactional
    public void acceptNewPrice(Long userId, Long cartItemId, BigDecimal agreedPrice) {
        if (agreedPrice == null || agreedPrice.signum() < 0) {
            throw BusinessException.forField(
                    ErrorCode.ILLEGAL_OPERATION,
                    "agreedPrice",
                    "Agreed price must be zero or greater"
            );
        }

        User user = findUserForUpdateOrThrow(userId);
        CartItem cartItem = user.getCart().findItemById(cartItemId);

        if (cartItem == null) {
            throw new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND);
        }
        BigDecimal currentPrice = cartItem.getProduct().getPrice();
        if(currentPrice.compareTo(agreedPrice)!= 0) {
            throw new BusinessException(ErrorCode.PRICE_CHANGED, "Price has changed again, please review the latest price");
        }

        cartItem.acceptCurrentPrice();
    }


    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

    }

    private User findUserForUpdateOrThrow(Long userId) {
        return userRepository.findByIdForUpdate(userId)
                .orElseThrow(
                        () -> new BusinessException(ErrorCode.USER_NOT_FOUND)
                );
    }

    private long stockAvailableForUser(Long userId, Product product) {
        Optional<Order> pendingOrder = orderRepository.findByUserIdAndStatusForUpdate(
                userId, OrderStatus.PENDING);

        int reserved = 0;

        if(pendingOrder.isPresent()) {

            List<OrderItem> items = pendingOrder.get().getItems();

            Optional <OrderItem> matchingItem = items.stream()
                    .filter(item -> product.getId().
                            equals(item.getProductIdSnapshot())
                    )
                    .findFirst();

            reserved = matchingItem.map(OrderItem::getQuantity).orElse(0);

        }
        return (long) product.getStock() + reserved;

    }

    private BusinessException insufficientStock( CartItem existingItem, Product product, int requestedQuantity, long availableStock){
        CartItemProblem itemProblem = new CartItemProblem(
                ErrorCode.INSUFFICIENT_STOCK,
                existingItem == null? null : existingItem.getId(),
                product.getId(),
                Math.toIntExact(availableStock),
                requestedQuantity,
                null,
                "Only " + availableStock + " units are available"
        );
        return new BusinessException(
                ErrorCode.CART_ERROR,
                "The requested quantity is unavailable.",
                Map.of("itemErrors", List.of(itemProblem))
        );
    }
}
