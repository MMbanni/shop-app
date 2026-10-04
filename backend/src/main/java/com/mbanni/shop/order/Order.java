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
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant paidAt;

    @Column(length = 2048)
    private String checkoutSuccessUrl;
    @Column(length = 2048)
    private String checkoutCancelUrl;

    private Instant reviewNeededAt;
    @Column(length = 500)
    private String reviewReason;
    private Instant reviewResolvedAt;
    private Long reviewedBy;
    @Column(length = 1000)
    private String reviewResolution;


    protected Order() {
    }

    public Order(User user, Instant createdAt,Instant expiresAt) {
        this.user = user;
        this.createdAt=createdAt;
        this.expiresAt = expiresAt;
    }

    public void addItem(OrderItem item) {
        if (!isPending()) {
            throw new BusinessException(ErrorCode.ILLEGAL_OPERATION,
                    "Items can only be added to pending orders");
        }
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

    public void markPaid(String stripeSessionId, Instant now) {
        if (status != OrderStatus.PENDING) {
            throw new BusinessException(ErrorCode.ILLEGAL_OPERATION);
        }

        this.status = OrderStatus.PAID;
        this.stripeSessionId = stripeSessionId;
        this.paidAt = now;
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

    public void markSuperseded() {
        if (status != OrderStatus.PENDING) {
            throw new IllegalStateException(
                    "Only pending orders can be superseded"
            );
        }

        status = OrderStatus.SUPERSEDED;
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

    public Instant getCreatedAt() { return createdAt; }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public String getCheckoutSuccessUrl() { return checkoutSuccessUrl; }

    public String getCheckoutCancelUrl() { return checkoutCancelUrl; }

    public Instant getReviewNeededAt() { return reviewNeededAt; }

    public String getReviewReason() { return reviewReason; }

    public Instant getReviewResolvedAt() { return reviewResolvedAt; }

    public Long getReviewedBy() { return reviewedBy; }

    public String getReviewResolution() { return reviewResolution; }

    public void configureCheckout(String successUrl, String cancelUrl) {
        if (checkoutSuccessUrl != null || checkoutCancelUrl != null) {
            throw new IllegalStateException("Checkout request settings are immutable");
        }
        checkoutSuccessUrl = successUrl;
        checkoutCancelUrl = cancelUrl;
    }

    public void requireReview(Instant now, String reason) {
        if (reviewNeededAt == null) reviewNeededAt = now;
        reviewReason = reason;
    }

    public void clearReview() {
        reviewNeededAt = null;
        reviewReason = null;
    }

    public void recordReviewResolution(Long adminId, Instant now, String resolution) {
        reviewedBy = adminId;
        reviewResolvedAt = now;
        reviewResolution = resolution;
    }
}
