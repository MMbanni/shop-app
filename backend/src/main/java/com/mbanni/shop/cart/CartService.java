package com.mbanni.shop.cart;

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


        if(product.getStock()<quantity || existingItem!=null && existingItem.getQuantity()>=product.getStock()){
            throw new BusinessException(ErrorCode.CART_ERROR, "Not enough stock");
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

        if (quantity < 0) {
            cart.removeItem(cartItemId, Math.abs(quantity));
            return;
        }

        CartItem cartItem = cart.findItemById(cartItemId);
        Product product = cartItem.getProduct();

        if (quantity > product.getStock() || cartItem.getQuantity() >= product.getStock()) {
            throw new BusinessException(
                    ErrorCode.CART_ERROR,
                    Map.of(
                            "cartItemId", cartItemId,
                            "productId", product.getId(),
                            "stock", product.getStock()
                    )
            );
        }

        addToCart(
                userId,
                product.getId(),
                quantity
        );
    }


    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

    }
}
