
package com.backend.streetmed_backend.repository.Order;

import com.backend.streetmed_backend.entity.order_entity.OrderRateLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRateLimitRepository extends JpaRepository<OrderRateLimit, Integer> {

    @Query("SELECT COUNT(o) FROM OrderRateLimit o WHERE o.userId = :userId AND o.requestTimestamp > :sinceTime")
    Long countByUserIdSince(@Param("userId") Integer userId, @Param("sinceTime") LocalDateTime sinceTime);

    @Query("SELECT COUNT(o) FROM OrderRateLimit o WHERE o.ipAddress = :ipAddress AND o.requestTimestamp > :sinceTime")
    Long countByIpAddressSince(@Param("ipAddress") String ipAddress, @Param("sinceTime") LocalDateTime sinceTime);

    @Query("SELECT o FROM OrderRateLimit o WHERE o.userId = :userId ORDER BY o.requestTimestamp DESC")
    List<OrderRateLimit> findRecentByUserId(@Param("userId") Integer userId);

    @Query("SELECT o FROM OrderRateLimit o WHERE o.ipAddress = :ipAddress ORDER BY o.requestTimestamp DESC")
    List<OrderRateLimit> findRecentByIpAddress(@Param("ipAddress") String ipAddress);

    @Modifying
    @Transactional
    @Query("DELETE FROM OrderRateLimit o WHERE o.requestTimestamp < :beforeTime")
    void deleteOldRecords(@Param("beforeTime") LocalDateTime beforeTime);
}