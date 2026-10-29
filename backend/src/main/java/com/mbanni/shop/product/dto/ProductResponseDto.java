package com.mbanni.shop.product.dto;

import com.mbanni.shop.product.ProductStatus;

import java.math.BigDecimal;
import java.util.Map;

public record ProductResponseDto(
    Long id,
    String name,
    Integer stock,
    BigDecimal price,
    String description,
    String imageUrl,
    Map<String, String> attributes,
    ProductStatus status
){}
