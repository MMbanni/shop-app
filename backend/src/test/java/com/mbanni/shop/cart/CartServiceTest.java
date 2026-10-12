package com.mbanni.shop.cart;

import com.mbanni.shop.cart.dto.CartItemProblem;
import com.mbanni.shop.cart.mapper.CartMapper;
import com.mbanni.shop.common.exception.BusinessException;
import com.mbanni.shop.common.exception.ErrorCode;
import com.mbanni.shop.order.OrderRepository;
import com.mbanni.shop.order.OrderStatus;
import com.mbanni.shop.product.Product;
import com.mbanni.shop.product.ProductRepository;
import com.mbanni.shop.user.User;
import com.mbanni.shop.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Optional;
import static com.mbanni.shop.support.TestSetup.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CartServiceTest {
    final ProductRepository products = mock(ProductRepository.class);
    final UserRepository users = mock(UserRepository.class);
    final OrderRepository orders = mock(OrderRepository.class);
    final CartService service = new CartService(products, users, orders, new CartMapper());
    User user; Product product;

    @BeforeEach void setUp() {
        user = user(); product = product();
        user.getCart().addItem(product, 2);
        id(user.getCart().getItems().get(0), 10);

        when(users.findByIdForUpdate(7L)).thenReturn(Optional.of(user));

        when(products.findById(20L)).thenReturn(Optional.of(product));

        when(orders.findByUserIdAndStatusForUpdate(7L, OrderStatus.PENDING))
                .thenReturn(Optional.empty());
    }

    @Test void countsTheCustomersOwnReservationAsAvailableStock() {
        product.setStock(1);
        when(orders.findByUserIdAndStatusForUpdate(7L, OrderStatus.PENDING))
                .thenReturn(Optional.of(order(user, product)));

        service.updateCart(7L, 10L, 1);
        assertEquals(3, user.getCart().getItems().get(0).getQuantity());
    }

    @Test void stockFailureDoesNotChangeQuantity() {
        product.setStock(2);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.updateCart(7L, 10L, 1));

        assertEquals(ErrorCode.CART_ERROR, ex.getErrorCode());
        assertTrue(ex.getProperties().containsKey("itemErrors"));

        var problems = (java.util.List<?>) ex.getProperties().get("itemErrors");

        assertEquals(ErrorCode.INSUFFICIENT_STOCK,
                ((CartItemProblem) problems.get(0)).code());

        assertEquals(2, user.getCart().getItems().get(0).getQuantity());
    }

    @Test void customerCanReduceQuantityOfArchivedProduct() {
        product.archive();
        service.updateCart(7L, 10L, -1);

        assertEquals(1, user.getCart().getItems().get(0).getQuantity());
    }

    @Test void rejectsAnotherCustomersItemId() {
        BusinessException ex = assertThrows(
                BusinessException.class, () -> service.removeFromCart(7L, 999L)
        );

        assertEquals(ErrorCode.CART_ITEM_NOT_FOUND, ex.getErrorCode());

        assertEquals(1, user.getCart().getItems().size());
    }

    @Test void priceAcceptanceMustMatchTheCurrentPrice() {
        product.setPrice(new BigDecimal("110.00"));

        assertThrows(
                BusinessException.class,
                () -> service.acceptNewPrice(7L, 10L, new BigDecimal("100.00"))
        );

        assertTrue(user.getCart().getItems().get(0).hasPriceChanged());

        service.acceptNewPrice(7L, 10L, new BigDecimal("110.00"));

        assertFalse(user.getCart().getItems().get(0).hasPriceChanged());
    }
}
