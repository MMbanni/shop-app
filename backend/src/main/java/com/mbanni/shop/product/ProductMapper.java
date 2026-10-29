package com.mbanni.shop.product;

import com.mbanni.shop.product.command.CreateProductCommand;
import com.mbanni.shop.product.command.UpdateProductCommand;
import com.mbanni.shop.product.dto.AdminProductResponseDto;
import com.mbanni.shop.product.dto.CreateProductRequestDto;
import com.mbanni.shop.product.dto.ProductResponseDto;
import com.mbanni.shop.product.dto.UpdateProductRequestDto;
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
                product.getImageUrl(),
                product.getAttributes(),
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
                product.getImageUrl(),
                product.getAttributes(),
                product.getProductStatus(),
                product.getVersion()
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



    public CreateProductCommand toCreateCommand(CreateProductRequestDto request) {
        return new CreateProductCommand(
                request.name(),
                request.stock(),
                request.price(),
                request.description(),
                request.imageUrl(),
                request.attributes()
        );
    }

    public UpdateProductCommand toUpdateCommand(UpdateProductRequestDto request) {
        return new UpdateProductCommand(
                request.name(),
                request.stock(),
                request.price(),
                request.description(),
                request.imageUrl(),
                request.attributes(),
                request.expectedVersion()

        );
    }

}
