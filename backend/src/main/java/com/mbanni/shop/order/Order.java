package com.mbanni.shop.order;


import com.mbanni.shop.common.exception.BusinessException;
import com.mbanni.shop.common.exception.ErrorCode;
import com.mbanni.shop.user.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "shop_order")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(nullable = false)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(unique = true)
    private String stripeSessionId;

    @Column(length = 2048)
    private String checkoutUrl;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant paidAt;

    protected Order() {
    }

    public Order(User user, Instant expiresAt) {
        this.user = user;
        this.expiresAt = expiresAt;
    }

    public void addItem(OrderItem item) {
        item.setOrder(this);
        items.add(item);
        total = total.add(item.getLineTotal());
    }

    public boolean isPending() {
        return status == OrderStatus.PENDING;
    }

    public boolean hasExpired(Instant now) {
        return expiresAt != null && now.isAfter(expiresAt);
    }

    public void markPaid(String stripeSessionId) {
        if (status != OrderStatus.PENDING) {
            throw new BusinessException(ErrorCode.ILLEGAL_OPERATION);
        }

        this.status = OrderStatus.PAID;
        this.stripeSessionId = stripeSessionId;
        this.paidAt = Instant.now();
    }

    public void markExpired() {
        if (status == OrderStatus.EXPIRED) {
            return;
        }

        if (status != OrderStatus.PENDING) {
            return;
        }

        this.status = OrderStatus.EXPIRED;
    }

    public void markCancelled() {
        if (status != OrderStatus.PENDING) {
            return;
        }

        status = OrderStatus.CANCELLED;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public String getStripeSessionId() {
        return stripeSessionId;
    }

    public String getCheckoutUrl() {return checkoutUrl; }

    public void setStripeSessionId(String stripeSessionId) {
        this.stripeSessionId = stripeSessionId;
    }

    public void setCheckoutUrl(String checkoutUrl) { this.checkoutUrl = checkoutUrl;}

    public void setStatus(OrderStatus orderStatus){ this.status = orderStatus;}

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getPaidAt() {
        return paidAt;
    }
}
