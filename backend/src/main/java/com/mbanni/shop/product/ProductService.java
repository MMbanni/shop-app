package com.mbanni.shop.product;

import com.mbanni.shop.common.exception.BusinessException;
import com.mbanni.shop.common.exception.ErrorCode;
import com.mbanni.shop.product.command.CreateProductCommand;
import com.mbanni.shop.product.command.UpdateProductCommand;
import com.mbanni.shop.product.dto.ProductRequestDto;
import com.mbanni.shop.product.dto.ProductResponseDto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Product create(CreateProductCommand request) {

        String name = Product.validateName(request.name());

        Optional<Product> existingProduct = productRepository.findByNameIgnoreCase(name);
        if(existingProduct.isPresent()){
            if(existingProduct.get().getProductStatus()==ProductStatus.ACTIVE){
                throw new BusinessException(ErrorCode.PRODUCT_ALREADY_EXISTS);
            } else {
                Product product = existingProduct.get();
                product.activate();
                return product;

            }

        }

        Product product = new Product(name, request.price(), request.description());

        Integer stock = request.stock();
        product.setStock(stock == null ? 0 : stock);

        return productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public Product getProduct(Long Id) {

        return productRepository.findById(Id)
                .orElseThrow(()-> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }


    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public List<Product> getAdminProducts(ProductStatus status) {
        if (status == null) {
            return productRepository.findAll();
        } else {
            return productRepository.findByStatus(status);
        }
    }

    @Transactional(readOnly = true)
    public List<Product> getActive() {
        return productRepository.findByStatus(ProductStatus.ACTIVE);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Product updateProduct(Long Id, UpdateProductCommand request) {

        Product product = productRepository.findById(Id).
                orElseThrow(()-> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));


        if(request.name()!= null) {
            String name = Product.validateName(request.name());

            if(!product.getName().equalsIgnoreCase(name)
                    && productRepository.existsByNameIgnoreCase(name)) {
                throw new BusinessException(ErrorCode.PRODUCT_ALREADY_EXISTS);
            }

            product.setName(name);
        }

        if(request.price()!= null) {
            product.setPrice(request.price());
        }

        if(request.stock()!= null) {
            product.setStock(request.stock());
        }



        return product;
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteProduct(Long Id) {
        productRepository.deleteById(Id);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void changeProductStatus(Long productId, ProductStatus status) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        if (status == ProductStatus.ACTIVE) {
            product.activate();
        } else if (status == ProductStatus.INACTIVE) {
            product.deactivate();
        } else if (status == ProductStatus.ARCHIVED) {
            product.archive();
        }
    }

}
