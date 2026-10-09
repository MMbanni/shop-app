package com.mbanni.shop.product;

import com.mbanni.shop.common.exception.BusinessException;
import com.mbanni.shop.common.exception.ErrorCode;
import com.mbanni.shop.supplier.Supplier;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;

import static com.mbanni.shop.common.Constants.*;

@Entity
@Table(
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_product_normalized_name",
                        columnNames = "normalized_name"
                )
        }
)
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = MAX_NAME_LENGTH)
    private String name;

    @Column(nullable = false, length = MAX_NAME_LENGTH)
    private String normalizedName;

    @Column(nullable = false)
    private int stock = 0;

    @Column(nullable = false, precision = PRECISION, scale = SCALE)
    private BigDecimal price;

    @ManyToOne
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Column(nullable = false, precision = PRECISION, scale = SCALE)
    private BigDecimal cost = BigDecimal.valueOf(0);

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ProductStatus status = ProductStatus.INACTIVE;

    @Column(length = 500)
    private String description;

    @Version
    private Long version;

    public Product() {}

    public Product(String name, BigDecimal price, String description) {
        setName(name);
        setNormalizedName(normalizeName(name));
        setPrice(price);
        setDescription(description);
    }

    public void deactivate() {
        this.status = ProductStatus.INACTIVE;
    }

    public void activate() {
        this.status = ProductStatus.ACTIVE;
    }

    public void archive() {
        this.status = ProductStatus.ARCHIVED;
    }

    public ProductStatus getProductStatus() {
        return status;
    }

    public Long getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getNormalizedName(){return this.normalizedName;}

    public int getStock() {
        return this.stock;
    }

    public BigDecimal getPrice() {
        return this.price;
    }

    public BigDecimal getCost() {
        return this.cost;
    }

    public Supplier getSupplier() {
        return this.supplier;
    }

    public String getDescription() {
        return description;
    }

    public Long getVersion() { return version; }

    // Setters
    public void setName(String name) {
        this.name = validateName(name);
    }
    public void setNormalizedName(String name) {
        this.normalizedName = normalizeName(name);
    }

    public void setStock(int stock) {
        if (stock < 0 || stock > MAX_STOCK) {
            throw new BusinessException(ErrorCode.ILLEGAL_OPERATION);
        }
        this.stock = stock;
    }

    public void decreaseStock(int amount) {
        if (amount <= 0) {
            throw new BusinessException(ErrorCode.ILLEGAL_OPERATION);
        }

        if (stock < amount) {
            throw new BusinessException(ErrorCode.CART_ERROR);
        }

        stock -= amount;
    }

    public void increaseStock(int amount) {
        if (amount <= 0) {
            throw new BusinessException(ErrorCode.ILLEGAL_OPERATION);
        }

        try {
            stock = Math.addExact(stock, amount);
        } catch (ArithmeticException e) {
            throw new BusinessException(ErrorCode.ILLEGAL_OPERATION);
        }
    }

    public void setPrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.ILLEGAL_OPERATION);
        }

        this.price=price;
    }
    public void setCost(BigDecimal cost) {
        if (cost == null || cost.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.ILLEGAL_OPERATION);
        }
        this.cost = cost;
    }
    public void setSupplier(Supplier supplier) { this.supplier = supplier; }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(!(o instanceof Product product)) return false;

        return id != null && Objects.equals(this.id, product.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public static String validateName(String name) {
        if(name == null) {
            throw new BusinessException(ErrorCode.ILLEGAL_OPERATION);
        }

        String trimmedName = name.trim();

        if(trimmedName.isEmpty()) {
            throw new BusinessException(ErrorCode.ILLEGAL_OPERATION);
        }

        return trimmedName;
    }

    public static String normalizeName(String name){
        return name.trim().toLowerCase(Locale.ROOT);

    }

}
