package com.mbanni.shop.order;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByStripeSessionId(String stripeSessionId);

    Optional<Order> findFirstByUser_IdAndStatus(Long userId, OrderStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select o from Order o
        where o.user.id = :userId
        and o.status = :status
        """)
    Optional<Order> findByUserIdAndStatusForUpdate(
            @Param("userId") Long userId,
            @Param("status") OrderStatus status
    );

    long countByUser_IdAndStatusAndCreatedAtAfter(
            Long userId,
            OrderStatus status,
            Instant createdAt
    );
}
