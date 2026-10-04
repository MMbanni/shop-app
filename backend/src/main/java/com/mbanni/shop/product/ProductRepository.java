package com.mbanni.shop.product;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    public boolean existsByNameIgnoreCase(String name);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Product> findByNameIgnoreCase(String name);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findByIdForUpdate(Long id);

    List<Product> findByStatus(ProductStatus status);
    Optional<Product> findByIdAndStatus(Long id, ProductStatus status);

    @Query("""
    select count(item) > 0
    from CartItem item
    where item.product.id = :productId
    """)
    boolean existsInAnyCart(@Param("productId") Long productId);
}
