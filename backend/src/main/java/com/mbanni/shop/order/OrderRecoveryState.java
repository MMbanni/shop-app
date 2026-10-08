package com.mbanni.shop.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_recovery_state")
public class OrderRecoveryState {

    @Id
    private Long id;

    @Column(nullable = false)
    private Long lastCheckedOrderId;

    protected OrderRecoveryState() {
        // Required by JPA
    }

    public OrderRecoveryState(Long id, Long lastCheckedOrderId) {
        this.id = id;
        this.lastCheckedOrderId = lastCheckedOrderId;
    }

    public Long getId() {
        return id;
    }

    public Long getLastCheckedOrderId() {
        return lastCheckedOrderId;
    }

    public void setLastCheckedOrderId(Long lastCheckedOrderId) {
        this.lastCheckedOrderId = lastCheckedOrderId;
    }
}