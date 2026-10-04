package com.mbanni.shop.checkout;
import com.mbanni.shop.order.Order;
import java.util.List;


public record BeginCheckoutDto(
        Order order,
        List<Long> productIds,
        boolean isPreexisting,
        boolean hasExpired,
        boolean hasSameCart
) {

}
