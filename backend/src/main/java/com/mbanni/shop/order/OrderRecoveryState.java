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

    public Long getId(){
        return this.id;
    }

    public Long getLastCheckedOrderId(){
        return this.lastCheckedOrderId;
    }

    public void setLastCheckedOrderId(Long id){
        this.lastCheckedOrderId = id;
    }


}