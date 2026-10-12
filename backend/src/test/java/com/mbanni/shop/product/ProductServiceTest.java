package com.mbanni.shop.product;

import com.mbanni.shop.common.exception.BusinessException;
import com.mbanni.shop.order.OrderRepository;
import com.mbanni.shop.order.OrderStatus;
import com.mbanni.shop.product.command.UpdateProductCommand;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Optional;
import static com.mbanni.shop.support.TestSetup.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductServiceTest {
    final ProductRepository products = mock(ProductRepository.class);
    final OrderRepository orders = mock(OrderRepository.class);
    final ProductService service = new ProductService(products, orders);

    @Test void staleEditorCannotOverwriteCurrentStock() {
        Product product = product();
        when(products.findByIdForUpdate(20L)).thenReturn(Optional.of(product));

        assertThrows(BusinessException.class, () -> service.updateProduct(20L,
                new UpdateProductCommand(null, 999, null, null, 2L)));
        assertEquals(10, product.getStock());
    }

    @Test void renamingUpdatesTheNormalizedKey() {
        Product product = product();
        when(products.findByIdForUpdate(20L)).thenReturn(Optional.of(product));

        service.updateProduct(20L, new UpdateProductCommand("  TABLE  ", null, null, null, 3L));

        assertEquals("TABLE", product.getName()); assertEquals("table", product.getNormalizedName());
    }

    @Test void reservedProductCannotBeDeleted() {
        Product product = product();

        when(products.findByIdForUpdate(20L)).thenReturn(Optional.of(product));
        when(orders.existsByStatusAndProductId(OrderStatus.PENDING, 20L)).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.deleteProduct(20L));
        verify(products, never()).delete(product);
    }

    @Test void cartProductCannotBeDeleted() {
        Product product = product();

        when(products.findByIdForUpdate(20L)).thenReturn(Optional.of(product));
        when(products.existsInAnyCart(20L)).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.deleteProduct(20L));

        verify(products, never()).delete(product);
    }

    @Test void stockCannotBecomeNegative() {
        Product product = product();

        assertThrows(BusinessException.class, () -> product.decreaseStock(11));
        assertEquals(10, product.getStock());
    }
}
