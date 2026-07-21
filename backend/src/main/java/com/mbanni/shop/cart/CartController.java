package com.mbanni.shop.cart;

import com.mbanni.shop.cart.dto.AddToCartRequestDto;
import com.mbanni.shop.cart.dto.CartResponseDto;
import com.mbanni.shop.cart.dto.RemoveFromCartRequestDto;
import com.mbanni.shop.cart.mapper.CartMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public CartResponseDto getCart(Authentication authentication) {
        Long userId = (Long.valueOf(authentication.getName()));
        return cartService.getCart(userId);
    }

    @PostMapping
    public ResponseEntity<Void> addToCart(
            Authentication authentication,
            @Valid @RequestBody AddToCartRequestDto request
    ) {
        Long userId = Long.valueOf(authentication.getName());

        cartService.addToCart(userId, request.productId(), request.quantity());

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/items/{cartItemId}")
    public ResponseEntity<Void> updateCartItem(
            Authentication authentication,
            @PathVariable Long cartItemId,
            @RequestBody @Min(-1) @Max(1) int quantity
    ) {

        Long userId = Long.valueOf(authentication.getName());

        cartService.updateCart(userId, cartItemId, quantity);

        return ResponseEntity.noContent().build();

    }

    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<Void> removeFromCart(
            Authentication authentication,
            @PathVariable Long cartItemId
    ) {
        Long userId = Long.valueOf(authentication.getName());

        cartService.removeFromCart(userId, cartItemId);

        return ResponseEntity.noContent().build();

    }
}
