package com.mbanni.shop.product.dto;

import com.mbanni.shop.product.ProductStatus;

import java.math.BigDecimal;

public record ProductResponseDto(
    Long id,
    String name,
    Integer stock,
    BigDecimal price,
    String description,
    ProductStatus status
){}
