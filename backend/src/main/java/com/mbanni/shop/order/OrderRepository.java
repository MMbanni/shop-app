package com.mbanni.shop.order;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    boolean existsByUser_Id(Long userId);

    @Query("select count(o) > 0 from Order o join o.items item "
            + "where o.status = :status and item.productIdSnapshot = :productId")
    boolean existsByStatusAndProductId(@Param("status") OrderStatus status, @Param("productId") Long productId);

    Optional<Order> findByStripeSessionId(String stripeSessionId);

    Optional<Order> findFirstByUser_IdAndStatus(Long userId, OrderStatus status);
    List<Order> findAllByStatus(OrderStatus status);

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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    select o from Order o
    where o.stripeSessionId = :sessionId
    """)
    Optional<Order> findByStripeSessionIdForUpdate(
            @Param("sessionId") String sessionId
    );

    long countByUser_IdAndStatusAndCreatedAtAfter(
            Long userId,
            OrderStatus status,
            Instant createdAt
    );

    long countByUser_IdAndStatusInAndCreatedAtAfter(
            Long userId,
            List<OrderStatus> status,
            Instant createdAt
    );

    Optional<Order> findByUserIdAndStripeSessionId(Long userId, String sessionId);

    @Query("""
    select o.user.id from Order o
    where o.stripeSessionId = :sessionId
    """)
    Optional<Long> findUserIdByStripeSessionId(
            @Param("sessionId") String sessionId
    );
}
