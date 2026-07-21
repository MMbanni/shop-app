package com.mbanni.shop.product;

import com.mbanni.shop.product.command.CreateProductCommand;
import com.mbanni.shop.product.command.UpdateProductCommand;
import com.mbanni.shop.product.dto.AdminProductResponseDto;
import com.mbanni.shop.product.dto.ProductRequestDto;
import com.mbanni.shop.product.dto.ProductResponseDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProductMapper {
    public ProductResponseDto toResponse(Product product) {
        return new ProductResponseDto(
                product.getId(),
                product.getName(),
                product.getStock(),
                product.getPrice(),
                product.getDescription(),
                product.getProductStatus()
        );
    }
    public AdminProductResponseDto toAdminResponse(Product product) {
        return new AdminProductResponseDto(
                product.getId(),
                product.getName(),
                product.getStock(),
                product.getPrice(),
                product.getDescription(),
                product.getProductStatus()
        );
    }

    public List<ProductResponseDto> toResponseList(List<Product> productList) {
        return productList
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<AdminProductResponseDto> toAdminResponseList(List<Product> productList) {
        return productList
                .stream()
                .map(this::toAdminResponse)
                .toList();
    }



    public CreateProductCommand toCreateCommand(ProductRequestDto request) {
        return new CreateProductCommand(
                request.name(),
                request.stock(),
                request.price(),
                request.description()
        );
    }

    public UpdateProductCommand toUpdateCommand(ProductRequestDto request) {
        return new UpdateProductCommand(
                request.name(),
                request.stock(),
                request.price()

        );
    }

}
