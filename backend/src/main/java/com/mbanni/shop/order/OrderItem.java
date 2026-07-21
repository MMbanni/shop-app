package com.mbanni.shop.order;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_item")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id")
    private Order order;

    // Snapshots in case price changes in future
    private Long productIdSnapshot;
    private String productNameSnapshot;
    private int quantity;
    private BigDecimal price;
    private BigDecimal lineTotal;

    protected OrderItem() {
    }

    public OrderItem(Long productIdSnapshot, String productNameSnapshot, int quantity, BigDecimal price) {
        this.productIdSnapshot = productIdSnapshot;
        this.productNameSnapshot = productNameSnapshot;
        this.quantity = quantity;
        this.price = price;
        this.lineTotal = price.multiply(BigDecimal.valueOf(quantity));
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Long getId() {
        return id;
    }

    public Long getProductIdSnapshot() {
        return productIdSnapshot;
    }

    public String getProductNameSnapshot() {
        return productNameSnapshot;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }
}
