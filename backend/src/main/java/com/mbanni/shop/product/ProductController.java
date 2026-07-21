package com.mbanni.shop.product;

import com.mbanni.shop.product.dto.AdminProductResponseDto;
import com.mbanni.shop.product.dto.ProductStatusRequestDto;
import com.mbanni.shop.product.dto.ProductRequestDto;
import com.mbanni.shop.product.dto.ProductResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
public class ProductController {
    private final ProductService productService;
    private final ProductMapper productMapper;

    public ProductController(ProductService productService, ProductMapper productMapper) {
        this.productService=productService;
        this.productMapper=productMapper;
    }

    @PostMapping("/admin/products")
    public ProductResponseDto addProduct(@RequestBody ProductRequestDto request) {
        Product product = productService.create(productMapper.toCreateCommand(request));
        return productMapper.toResponse(product);
    }


    @GetMapping("/products")
    public List<ProductResponseDto> getProducts() {

        List<Product> products = productService.getActive();
        return productMapper.toResponseList(products);
    }

    @GetMapping("/admin/products")
    public List<AdminProductResponseDto> getAdminProducts(
            @RequestParam(required = false) ProductStatus status
    ) {

        List<Product> products = productService.getAdminProducts(status);
        return productMapper.toAdminResponseList(products);
    }

    // Get one
    @GetMapping("/products/{id}")
    public ProductResponseDto getProduct(@PathVariable Long id) {
        Product product = productService.getProduct(id);
        return productMapper.toResponse(product);

    }

    @PatchMapping("/admin/products/{id}")
    public AdminProductResponseDto updateProduct(
            @PathVariable Long id,
            @RequestBody ProductRequestDto request
    ) {

        Product product = productService.updateProduct(id, productMapper.toUpdateCommand(request));
        return productMapper.toAdminResponse(product);

    }

    @PatchMapping("/admin/products/{id}/status")
    public ResponseEntity<Void> changeStatus(
            @PathVariable Long id,
            @RequestBody ProductStatusRequestDto request
    ) {
        productService.changeProductStatus(id, request.status());

        return ResponseEntity.noContent().build();
    }


    @DeleteMapping("/admin/products/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}


