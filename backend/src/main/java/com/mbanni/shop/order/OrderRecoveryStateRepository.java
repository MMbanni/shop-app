package com.mbanni.shop.order;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRecoveryStateRepository
        extends JpaRepository<OrderRecoveryState, Long> {
}
