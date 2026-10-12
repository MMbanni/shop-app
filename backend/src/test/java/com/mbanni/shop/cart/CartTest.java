package com.mbanni.shop.cart;

import com.mbanni.shop.common.exception.BusinessException;
import com.mbanni.shop.common.exception.ErrorCode;
import com.mbanni.shop.product.Product;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.math.BigDecimal;
import static com.mbanni.shop.support.TestSetup.*;
import static org.junit.jupiter.api.Assertions.*;

class CartTest {
    @Test void addingSameProductMergesQuantities() {
        Cart cart = new Cart();
        Product product = product();

        cart.addItem(product, 2); cart.addItem(product, 3);
        assertEquals(1, cart.getItems().size());
        assertEquals(5, cart.getItems().get(0).getQuantity());
        assertEquals(new BigDecimal("500.00"), cart.calculateTotal());
    }

    @ParameterizedTest @ValueSource(ints = {0, -1, 1000})
    void rejectsInvalidQuantity(int quantity) {
        Cart cart = new Cart();
        assertThrows(BusinessException.class, () -> cart.addItem(product(), quantity));
        assertTrue(cart.getItems().isEmpty());
    }

    @Test void rejectsMergedQuantityAboveLimitWithoutChangingCart() {
        Cart cart = new Cart();
        Product product = product();
        cart.addItem(product, 999);

        BusinessException ex = assertThrows(BusinessException.class, () -> cart.addItem(product, 1));
        assertEquals(ErrorCode.EXCEEDED_QUANTITY_LIMIT, ex.getErrorCode());
        assertEquals(999, cart.getItems().get(0).getQuantity());
    }

    @Test void removingLastUnitDetachesTheItem() {
        Cart cart = new Cart();
        cart.addItem(product(), 1);
        id(cart.getItems().get(0), 10);

        cart.removeItem(10L, 1);
        assertTrue(cart.getItems().isEmpty());
    }

    @Test void paymentCleanupPreservesUnitsAddedAfterCheckout() {
        Cart cart = new Cart();
        cart.addItem(product(), 5);
        id(cart.getItems().get(0), 10);

        cart.removePurchasedQuantity(10L, 2);
        assertEquals(3, cart.getItems().get(0).getQuantity());
    }

    @Test void paymentCleanupToleratesRemovedOrReducedCartItems() {
        Cart cart = new Cart();
        cart.addItem(product(), 1);
        id(cart.getItems().get(0), 10);

        assertDoesNotThrow(() -> cart.removePurchasedQuantity(999L, 2));
        assertDoesNotThrow(() -> cart.removePurchasedQuantity(10L, 2));
        assertTrue(cart.getItems().isEmpty());
    }

    @Test void priceChangesRequireAcceptanceAndDiscountsRoundPerUnit() {
        Product product = product();
        Cart cart = new Cart();
        cart.addItem(product, 3);
        CartItem item = cart.getItems().get(0);

        product.setPrice(new BigDecimal("10.05"));
        assertTrue(item.hasPriceChanged());

        item.acceptCurrentPrice();
        assertFalse(item.hasPriceChanged());

        item.setDiscount(new BigDecimal("0.10"));
        assertEquals(new BigDecimal("9.05"), item.calculateUnitPrice());
        assertEquals(new BigDecimal("27.15"), item.calculateLineTotal());
    }
}
