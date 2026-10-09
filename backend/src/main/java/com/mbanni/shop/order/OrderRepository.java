package com.mbanni.shop.order;

import com.mbanni.shop.order.dto.OrderRecoveryDto;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    boolean existsByUser_Id(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Long id);


    @Query("select count(o) > 0 from Order o join o.items item "
            + "where o.status = :status and item.productIdSnapshot = :productId")
    boolean existsByStatusAndProductId(@Param("status") OrderStatus status, @Param("productId") Long productId);

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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    select o from Order o
    where o.stripeSessionId = :sessionId
    """)
    Optional<Order> findByStripeSessionIdForUpdate(
            @Param("sessionId") String sessionId
    );

    @Query("""
    SELECT new com.mbanni.shop.order.dto.OrderRecoveryDto(o.id, o.user.id)
    FROM Order o
    WHERE o.status = :status
      AND o.id > :lastId
    ORDER BY o.id ASC
    """)
    List<OrderRecoveryDto> findPendingOrderIds(
            @Param("status") OrderStatus status,
            @Param("lastId") Long lastId,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT o FROM Order o
    WHERE o.status = :status
      AND o.stripeSessionId IS NULL
      AND o.reviewNeededAt IS NULL
      AND o.createdAt > :cutoff
      AND o.recoveryLeaseUntil <= :now
      AND o.nextRecoveryCheckAt <= :now
    ORDER BY o.nextRecoveryCheckAt ASC, o.id ASC
    """)
    List<Order> findOrdersWithoutSession(
            @Param("status") OrderStatus status,
            @Param("cutoff") Instant cutoff,
            @Param("now") Instant now,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT o FROM Order o
    WHERE o.status = :status
      AND o.stripeSessionId IS NULL
      AND o.reviewNeededAt IS NULL
      AND o.createdAt <= :cutoff
      AND o.recoveryLeaseUntil <= :now
      AND o.nextRecoveryCheckAt <= :now
    ORDER BY o.nextRecoveryCheckAt ASC, o.id ASC
    """)
    List<Order> findOverdueOrders(
            @Param("status") OrderStatus status,
            @Param("cutoff") Instant cutoff,
            @Param("now") Instant now,
            Pageable pageable
    );


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT o FROM Order o
    WHERE o.status = :status
      AND o.stripeSessionId IS NOT NULL
      AND o.reviewNeededAt IS NULL
      AND o.recoveryLeaseUntil <= :now
      AND o.nextRecoveryCheckAt <= :now
    ORDER BY o.nextRecoveryCheckAt ASC
    """)
    List<Order> findPendingOrdersWithSession(
            @Param("status") OrderStatus status,
            @Param("now") Instant now,
            Pageable pageable
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

    List<Order> findByReviewNeededAtIsNotNullOrderByReviewNeededAtAsc(PageRequest of);
}
