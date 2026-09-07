package com.mbanni.shop.cart;

import com.mbanni.shop.cart.dto.CartItemProblem;
import com.mbanni.shop.cart.dto.CartResponseDto;
import com.mbanni.shop.cart.mapper.CartMapper;
import com.mbanni.shop.common.exception.BusinessException;
import com.mbanni.shop.common.exception.ErrorCode;
import com.mbanni.shop.product.Product;
import com.mbanni.shop.product.ProductRepository;
import com.mbanni.shop.user.User;
import com.mbanni.shop.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class CartService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CartMapper cartMapper;


    // Inject dependencies
    public CartService(ProductRepository productRepository, UserRepository userRepository, CartMapper cartMapper) {
        this.productRepository = productRepository;
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
        User user = findUserOrThrow(userId);

        Cart cart = user.getCart();

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        CartItem existingItem = cart.findItemByProductId(productId);

        int existingQuantity = existingItem == null ? 0 : existingItem.getQuantity();
        int requestedQuantity = existingQuantity + quantity;


        if(requestedQuantity > product.getStock()){
            throw insufficientStock(existingItem, product, requestedQuantity);
        }

        cart.addItem(product, quantity);
    }

    @Transactional
    public void removeFromCart(Long userId, Long cartItemId) {
        User user = findUserOrThrow(userId);

        Cart cart = user.getCart();
        cart.removeAll(cartItemId);
    }

    @Transactional
    public void updateCart(Long userId, Long cartItemId, int quantity) {
        User user = findUserOrThrow(userId);
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

        int requestedQuantity =
                cartItem.getQuantity() + quantity;

        if (requestedQuantity > product.getStock()) {
            throw insufficientStock(
                    cartItem,
                    product,
                    requestedQuantity
            );
        }

        addToCart(
                userId,
                product.getId(),
                quantity
        );
    }

    @Transactional
    public void acceptNewPrice(Long userId, Long cartItemId) {
        User user = findUserOrThrow(userId);
        CartItem cartItem = user.getCart().findItemById(cartItemId);

        cartItem.setPriceWhenAdded(cartItem.getProduct().getPrice());
    }


    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

    }

    private BusinessException insufficientStock( CartItem existingItem, Product product, int requestedQuantity){
        CartItemProblem itemProblem = new CartItemProblem(
                ErrorCode.INSUFFICIENT_STOCK,
                existingItem == null? null : existingItem.getId(),
                product.getId(),
                product.getStock(),
                requestedQuantity,
                null,
                "Only " + product.getStock() + " units are available"
        );
        return new BusinessException(
                ErrorCode.CART_ERROR,
                "The requested quantity is unavailable.",
                Map.of("itemErrors", List.of(itemProblem))
        );
    }
}
