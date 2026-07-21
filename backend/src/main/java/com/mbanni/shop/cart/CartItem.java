package com.mbanni.shop.cart;

import com.mbanni.shop.common.exception.BusinessException;
import com.mbanni.shop.common.exception.ErrorCode;
import com.mbanni.shop.product.Product;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable=false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable=false)
    private Product product;

    @Column(nullable = false)
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(nullable = false)
    private int quantity;


    public CartItem(){}

    CartItem(Cart cart, Product product, int quantity) {

        this.product = product;
        this.cart = cart;
        setQuantity(quantity);
    }

    public Long getId() {
        return id;
    }

    public BigDecimal getDiscount() {
        return this.discount;
    }

    public Product getProduct() {
        return this.product;
    }

    public int getQuantity() {
        return this.quantity;
    }

    void detachFromCart() {
        this.cart = null;
    }

    public void setDiscount(BigDecimal discount ) {
        if(discount == null ||
                discount.compareTo(BigDecimal.ZERO) < 0 ||
                discount.compareTo(BigDecimal.ONE) > 0) {
            throw new BusinessException(ErrorCode.ILLEGAL_OPERATION);
        }

        this.discount = discount;
    }

    void setQuantity(int value) {
        if (value < 1 || value > Cart.MAX_QUANTITY) {
            throw new BusinessException(ErrorCode.ILLEGAL_OPERATION);
        }

        this.quantity = value;
    }

    public BigDecimal calculateLineTotal() {
        BigDecimal price = product.getPrice();
        BigDecimal discountMultiplier = BigDecimal.ONE.subtract(discount);

        return price
                .multiply(discountMultiplier)
                .multiply(BigDecimal.valueOf(quantity));
    }

}
