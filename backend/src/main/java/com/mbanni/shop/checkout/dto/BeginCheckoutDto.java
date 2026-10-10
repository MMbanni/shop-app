package com.mbanni.shop.checkout.dto;
import com.mbanni.shop.order.Order;
import com.mbanni.shop.order.dto.OrderSnapshot;

import java.util.List;


public record BeginCheckoutDto(
        OrderSnapshot order,
        boolean isPreexisting,
        boolean hasExpired,
        boolean hasSameCart
) {

}
