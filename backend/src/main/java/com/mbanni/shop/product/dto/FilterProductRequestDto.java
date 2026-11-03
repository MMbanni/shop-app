package com.mbanni.shop.product.dto;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;

public record FilterProductRequestDto(
        PriceQuery priceQuery,
        HashMap<String, List<String>> filters


) {
    public record PriceQuery(
            BigDecimal min,
            BigDecimal max
    ){
    }

}
