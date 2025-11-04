package com.backend.streetmed_backend.service.orderService;

import com.backend.streetmed_backend.entity.order_entity.OrderRateLimit;
import com.backend.streetmed_backend.repository.Order.OrderRateLimitRepository;
import com.backend.streetmed_backend.repository.Order.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class OrderRateLimitService {
    private static final Logger logger = LoggerFactory.getLogger(OrderRateLimitService.class);

    // Rate limit constants
    private static final int MAX_ORDERS_PER_HOUR = 3;
    private static final int MAX_PENDING_ORDERS_PER_USER = 5;
    private static final int MAX_GUEST_ORDERS_PER_HOUR = 2;
    private static final int MAX_PENDING_GUEST_ORDERS = 3;

    private final OrderRateLimitRepository rateLimitRepository;
    private final OrderRepository orderRepository;

    @Autowired
    public OrderRateLimitService(OrderRateLimitRepository rateLimitRepository,
                                 OrderRepository orderRepository) {
        this.rateLimitRepository = rateLimitRepository;
        this.orderRepository = orderRepository;
    }

    /**
     * Check if a user can create a new order based on rate limits
     */
    @Transactional
    public void checkUserRateLimit(Integer userId) {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);

        // Check hourly rate limit
        Long ordersLastHour = rateLimitRepository.countByUserIdSince(userId, oneHourAgo);
        if (ordersLastHour >= MAX_ORDERS_PER_HOUR) {
            throw new RateLimitExceededException(
                    String.format("Rate limit exceeded: Maximum %d orders per hour", MAX_ORDERS_PER_HOUR)
            );
        }

        // Check pending orders limit
        Long pendingOrders = orderRepository.countByUserIdAndStatus(userId, "PENDING");
        if (pendingOrders >= MAX_PENDING_ORDERS_PER_USER) {
            throw new RateLimitExceededException(
                    String.format("Maximum pending orders limit reached: %d orders", MAX_PENDING_ORDERS_PER_USER)
            );
        }
    }

    /**
     * Check if a guest (by IP) can create a new order
     */
    @Transactional
    public void checkGuestRateLimit(String ipAddress) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("IP address is required for guest orders");
        }

        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);

        // Check hourly rate limit for IP
        Long ordersLastHour = rateLimitRepository.countByIpAddressSince(ipAddress, oneHourAgo);
        if (ordersLastHour >= MAX_GUEST_ORDERS_PER_HOUR) {
            throw new RateLimitExceededException(
                    String.format("Guest rate limit exceeded: Maximum %d orders per hour", MAX_GUEST_ORDERS_PER_HOUR)
            );
        }

        // Check pending orders limit for IP
        Long pendingOrders = orderRepository.countPendingOrdersByIpAddress(ipAddress);
        if (pendingOrders >= MAX_PENDING_GUEST_ORDERS) {
            throw new RateLimitExceededException(
                    String.format("Maximum pending orders limit reached for guests: %d orders", MAX_PENDING_GUEST_ORDERS)
            );
        }
    }

    /**
     * Record an order creation for rate limiting
     */
    @Transactional
    public void recordOrderCreation(Integer userId, String ipAddress, Integer orderId) {
        OrderRateLimit rateLimit;

        if (userId != null && userId != -1) {
            // Record for registered user
            rateLimit = new OrderRateLimit(userId, orderId);
        } else {
            // Record for guest (IP-based)
            rateLimit = new OrderRateLimit(ipAddress, orderId);
        }

        rateLimitRepository.save(rateLimit);
        logger.debug("Recorded order creation for rate limiting: userId={}, ip={}, orderId={}",
                userId, ipAddress, orderId);
    }

    /**
     * Clean up old rate limit records (runs daily)
     */
    @Scheduled(cron = "0 0 2 * * ?") // Run at 2 AM daily
    public void cleanupOldRecords() {
        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);
        rateLimitRepository.deleteOldRecords(threeDaysAgo);
        logger.info("Cleaned up rate limit records older than {}", threeDaysAgo);
    }

    /**
     * Custom exception for rate limit violations
     */
    public static class RateLimitExceededException extends RuntimeException {
        public RateLimitExceededException(String message) {
            super(message);
        }
    }
}