package com.backend.streetmed_backend.repository.Order;

import com.backend.streetmed_backend.entity.order_entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {

    // Priority Queue Queries
    @Query("SELECT o FROM Order o WHERE o.roundId IS NULL " +
            "AND o.status IN ('PENDING', 'PENDING_ACCEPT') " +
            "ORDER BY o.requestTime ASC")
    Page<Order> findPendingOrdersPrioritized(Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.roundId IS NULL " +
            "AND o.status IN ('PENDING', 'PENDING_ACCEPT') " +
            "ORDER BY o.requestTime ASC")
    List<Order> findPendingOrdersWithPriority();

    // Pessimistic locking for concurrency control
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.orderId = :orderId")
    Optional<Order> findByIdWithLock(@Param("orderId") Integer orderId);

    // Statistics queries - using native query for date calculations
    @Query(value = "SELECT TIMESTAMPDIFF(HOUR, MIN(request_time), NOW()) " +
            "FROM orders WHERE round_id IS NULL AND status = 'PENDING'",
            nativeQuery = true)
    Integer findOldestPendingOrderHours();

    @Query("SELECT COUNT(o) FROM Order o WHERE o.roundId IS NULL " +
            "AND o.status IN ('PENDING', 'PENDING_ACCEPT')")
    Long countPendingOrders();

    @Query("SELECT MIN(o.requestTime) FROM Order o WHERE o.roundId IS NULL " +
            "AND o.status = 'PENDING'")
    LocalDateTime findOldestPendingOrderTime();

    @Query("SELECT COUNT(o) FROM Order o WHERE o.userId = :userId AND o.status = :status")
    Long countByUserIdAndStatus(@Param("userId") Integer userId, @Param("status") String status);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.clientIpAddress = :ipAddress AND o.status = 'PENDING'")
    Long countPendingOrdersByIpAddress(@Param("ipAddress") String ipAddress);

    @Query("SELECT o FROM Order o WHERE o.clientIpAddress = :ipAddress AND o.requestTime > :sinceTime")
    List<Order> findOrdersByIpAddressSince(@Param("ipAddress") String ipAddress,
                                           @Param("sinceTime") LocalDateTime sinceTime);

    // Existing methods
    List<Order> findByRoundIdIsNullOrderByRequestTimeAsc();
    List<Order> findByRoundId(Integer roundId);
    long countByRoundId(Integer roundId);
    List<Order> findByUserId(Integer userId);
    List<Order> findByStatus(String status);
}