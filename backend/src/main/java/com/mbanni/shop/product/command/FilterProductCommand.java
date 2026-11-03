package com.mbanni.shop.product.command;

import com.mbanni.shop.product.dto.FilterProductRequestDto;

import java.util.HashMap;
import java.util.List;

public record FilterProductCommand(
        FilterProductRequestDto.PriceQuery priceQuery,
        HashMap<String, List<String>> filters
) {
}
