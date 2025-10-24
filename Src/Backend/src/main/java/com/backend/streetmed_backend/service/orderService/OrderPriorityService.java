package com.backend.streetmed_backend.service.orderService;

import com.backend.streetmed_backend.entity.order_entity.Order;
import com.backend.streetmed_backend.repository.Order.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class OrderPriorityService {

    private final OrderRepository orderRepository;

    @Autowired
    public OrderPriorityService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * Get pending orders sorted by priority with pagination
     */
    public PendingOrdersResponse getPendingOrders(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Order> ordersPage = orderRepository.findPendingOrdersPrioritized(pageable);

        List<PendingOrderInfo> orderInfos = ordersPage.getContent().stream()
                .map(this::toPendingOrderInfo)
                .collect(Collectors.toList());

        Long oldestWaitingHours = calculateOldestWaitingHours();

        return new PendingOrdersResponse(
                orderInfos,
                page,
                ordersPage.getTotalElements(),
                oldestWaitingHours
        );
    }

    /**
     * Get all pending orders without pagination (for internal use)
     */
    public List<PendingOrderInfo> getAllPendingOrders() {
        List<Order> orders = orderRepository.findPendingOrdersWithPriority();

        return orders.stream()
                .map(this::toPendingOrderInfo)
                .collect(Collectors.toList());
    }

    /**
     * Convert Order entity to PendingOrderInfo with calculated fields
     */
    private PendingOrderInfo toPendingOrderInfo(Order order) {
        LocalDateTime now = LocalDateTime.now();
        long waitingHours = Duration.between(order.getRequestTime(), now).toHours();
        int priority = calculatePriority(waitingHours);

        // Check if order has a lock (if another volunteer is viewing it)
        LockStatus lockStatus = null;
        // This would need to be implemented if you want viewing locks
        // For now, returning null

        // Calculate estimated round date if order is assigned to a round
        String estimatedRoundDate = null;
        if (order.getRoundId() != null) {
            estimatedRoundDate = "Assigned to round " + order.getRoundId();
        }

        return new PendingOrderInfo(
                order.getOrderId(),
                order.getRequestTime(),
                waitingHours,
                priority,
                order.getDeliveryAddress(),
                order.getOrderItems(),
                order.getStatus(),
                lockStatus,
                estimatedRoundDate,
                order.getPhoneNumber(),
                order.getNotes()
        );
    }

    /**
     * Calculate priority based on waiting time
     * Priority 1 = Critical (72+ hours)
     * Priority 2 = High (48-72 hours)
     * Priority 3 = Medium (24-48 hours)
     * Priority 4 = Normal (< 24 hours)
     */
    private int calculatePriority(long waitingHours) {
        if (waitingHours >= 72) return 1; // Critical
        if (waitingHours >= 48) return 2; // High
        if (waitingHours >= 24) return 3; // Medium
        return 4; // Normal
    }

    /**
     * Calculate the age of the oldest pending order in hours
     */
    private Long calculateOldestWaitingHours() {
        LocalDateTime oldestTime = orderRepository.findOldestPendingOrderTime();
        if (oldestTime == null) return 0L;
        return Duration.between(oldestTime, LocalDateTime.now()).toHours();
    }

    /**
     * Get statistics about pending orders
     */
    public PendingOrderStatistics getStatistics() {
        Long totalPending = orderRepository.countPendingOrders();
        Long oldestWaitingHours = calculateOldestWaitingHours();

        // Count by priority
        List<Order> pendingOrders = orderRepository.findPendingOrdersWithPriority();
        Map<Integer, Long> priorityCounts = new HashMap<>();
        priorityCounts.put(1, 0L); // Critical
        priorityCounts.put(2, 0L); // High
        priorityCounts.put(3, 0L); // Medium
        priorityCounts.put(4, 0L); // Normal

        for (Order order : pendingOrders) {
            long waitingHours = Duration.between(order.getRequestTime(), LocalDateTime.now()).toHours();
            int priority = calculatePriority(waitingHours);
            priorityCounts.put(priority, priorityCounts.get(priority) + 1);
        }

        return new PendingOrderStatistics(
                totalPending,
                oldestWaitingHours,
                priorityCounts
        );
    }

    // Response DTOs
    public static class PendingOrdersResponse {
        public final List<PendingOrderInfo> orders;
        public final Integer page;
        public final Long totalOrders;
        public final Long oldestWaitingHours;

        public PendingOrdersResponse(List<PendingOrderInfo> orders, Integer page,
                                     Long totalOrders, Long oldestWaitingHours) {
            this.orders = orders;
            this.page = page;
            this.totalOrders = totalOrders;
            this.oldestWaitingHours = oldestWaitingHours;
        }
    }

    public static class PendingOrderInfo {
        public final Integer orderId;
        public final LocalDateTime requestTime;
        public final Long waitingHours;
        public final Integer priority;
        public final String deliveryAddress;
        public final Object items;
        public final String status;
        public final LockStatus lockStatus;
        public final String estimatedRoundDate;
        public final String phoneNumber;
        public final String notes;

        public PendingOrderInfo(Integer orderId, LocalDateTime requestTime, Long waitingHours,
                                Integer priority, String deliveryAddress, Object items,
                                String status, LockStatus lockStatus, String estimatedRoundDate,
                                String phoneNumber, String notes) {
            this.orderId = orderId;
            this.requestTime = requestTime;
            this.waitingHours = waitingHours;
            this.priority = priority;
            this.deliveryAddress = deliveryAddress;
            this.items = items;
            this.status = status;
            this.lockStatus = lockStatus;
            this.estimatedRoundDate = estimatedRoundDate;
            this.phoneNumber = phoneNumber;
            this.notes = notes;
        }
    }

    public static class LockStatus {
        public final Integer lockedBy;
        public final LocalDateTime lockedUntil;

        public LockStatus(Integer lockedBy, LocalDateTime lockedUntil) {
            this.lockedBy = lockedBy;
            this.lockedUntil = lockedUntil;
        }
    }

    public static class PendingOrderStatistics {
        public final Long totalPending;
        public final Long oldestWaitingHours;
        public final Map<Integer, Long> priorityCounts;

        public PendingOrderStatistics(Long totalPending, Long oldestWaitingHours,
                                      Map<Integer, Long> priorityCounts) {
            this.totalPending = totalPending;
            this.oldestWaitingHours = oldestWaitingHours;
            this.priorityCounts = priorityCounts;
        }
    }
}