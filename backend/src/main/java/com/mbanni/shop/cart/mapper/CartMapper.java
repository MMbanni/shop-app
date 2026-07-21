package com.mbanni.shop.cart.mapper;

import com.mbanni.shop.cart.Cart;
import com.mbanni.shop.cart.dto.CartItemResponseDto;
import com.mbanni.shop.cart.dto.CartResponseDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class CartMapper {

    public CartResponseDto toResponse(Cart cart) {
        List<CartItemResponseDto> response = cart.getItems()
                .stream()
                .map((item)-> new CartItemResponseDto(
                    item.getId(),
                    item.getProduct().getId(),
                    item.getProduct().getName(),
                    item.getQuantity(),
                    item.getProduct().getPrice(),
                    item.calculateLineTotal()
                ))
                .toList();

        BigDecimal total = cart.calculateTotal();

        return new CartResponseDto(response, total);
    }

}
