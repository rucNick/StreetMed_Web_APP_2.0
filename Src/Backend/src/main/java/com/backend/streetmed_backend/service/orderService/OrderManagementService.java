package com.backend.streetmed_backend.service.orderService;

import com.backend.streetmed_backend.dto.order.*;
import com.backend.streetmed_backend.entity.order_entity.Order;
import com.backend.streetmed_backend.entity.order_entity.OrderAssignment;
import com.backend.streetmed_backend.entity.order_entity.OrderItem;
import com.backend.streetmed_backend.repository.Order.OrderRepository;
import com.backend.streetmed_backend.service.roundService.RoundCapacityService;
import com.backend.streetmed_backend.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;


import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.*;

@Service
@Transactional
public class OrderManagementService {

    private static final Logger logger = LoggerFactory.getLogger(OrderManagementService.class);

    private final OrderService orderService;
    private final OrderAssignmentService orderAssignmentService;
    private final RoundCapacityService roundCapacityService;
    private final OrderRepository orderRepository;

    @Autowired
    public OrderManagementService(OrderService orderService,
                                  OrderAssignmentService orderAssignmentService,
                                  RoundCapacityService roundCapacityService,
                                  OrderRepository orderRepository) {
        this.orderService = orderService;
        this.orderAssignmentService = orderAssignmentService;
        this.roundCapacityService = roundCapacityService;
        this.orderRepository = orderRepository;
    }

    public ResponseEntity<Map<String, Object>> getPendingOrders(GetPendingOrdersRequest request) {
        if (!"true".equals(request.getAuthStatus())) {
            return ResponseUtil.unauthorized();
        }

        if (!"VOLUNTEER".equals(request.getUserRole())) {
            return ResponseUtil.forbidden("Only volunteers can view pending orders");
        }

        try {
            Page<Order> ordersPage = orderRepository.findPendingOrdersPrioritized(
                    PageRequest.of(request.getPage(), request.getSize())
            );

            List<Map<String, Object>> orderList = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();

            for (Order order : ordersPage.getContent()) {
                Map<String, Object> orderInfo = new HashMap<>();
                long waitingHours = Duration.between(order.getRequestTime(), now).toHours();

                orderInfo.put("orderId", order.getOrderId());
                orderInfo.put("requestTime", order.getRequestTime());
                orderInfo.put("waitingHours", waitingHours);
                orderInfo.put("priority", calculatePriority(waitingHours));
                orderInfo.put("deliveryAddress", order.getDeliveryAddress());
                orderInfo.put("items", order.getOrderItems());
                orderInfo.put("status", order.getStatus());
                orderInfo.put("phoneNumber", order.getPhoneNumber());
                orderInfo.put("notes", order.getNotes());

                if (order.getRoundId() != null) {
                    orderInfo.put("estimatedRoundDate", "Assigned to round " + order.getRoundId());
                }

                orderList.add(orderInfo);
            }

            LocalDateTime oldestTime = orderRepository.findOldestPendingOrderTime();
            long oldestWaitingHours = oldestTime != null ?
                    Duration.between(oldestTime, now).toHours() : 0;

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("orders", orderList);

            Map<String, Object> pagination = new HashMap<>();
            pagination.put("page", request.getPage());
            pagination.put("totalOrders", ordersPage.getTotalElements());
            pagination.put("oldestWaitingHours", oldestWaitingHours);
            response.put("pagination", pagination);

            response.put("authenticated", true);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting pending orders: {}", e.getMessage());
            return ResponseUtil.internalError("Failed to retrieve pending orders");
        }
    }

    private int calculatePriority(long waitingHours) {
        if (waitingHours >= 72) return 1;
        if (waitingHours >= 48) return 2;
        if (waitingHours >= 24) return 3;
        return 4;
    }

    @Transactional
    public ResponseEntity<Map<String, Object>> acceptOrder(AcceptOrderRequest request) {
        if (!Boolean.TRUE.equals(request.getAuthenticated())) {
            return ResponseUtil.unauthorized();
        }

        if (!"VOLUNTEER".equals(request.getUserRole())) {
            return ResponseUtil.forbidden("Only volunteers can accept orders");
        }

        try {
            Optional<Order> orderOpt = orderRepository.findByIdWithLock(request.getOrderId());
            if (orderOpt.isEmpty()) {
                return ResponseUtil.notFound("Order not found");
            }

            Order order = orderOpt.get();
            Integer roundId = order.getRoundId();

            if (roundId != null && !roundCapacityService.canVolunteerAcceptMore(roundId, request.getVolunteerId())) {
                return ResponseUtil.badRequest("Maximum order capacity reached for this round");
            }

            OrderAssignment assignment = orderAssignmentService.acceptOrder(
                    request.getOrderId(), request.getVolunteerId(), roundId
            );

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("assignmentId", assignment.getAssignmentId());
            responseData.put("roundId", roundId);
            responseData.put("status", assignment.getStatus().toString());

            if (roundId != null) {
                RoundCapacityService.RoundCapacityInfo capacityInfo =
                        roundCapacityService.getRoundCapacityInfo(roundId);

                Map<String, Object> capacity = new HashMap<>();
                capacity.put("currentOrders", capacityInfo.totalOrders);
                capacity.put("maxCapacity", capacityInfo.maxCapacity);
                capacity.put("yourOrderCount", orderAssignmentService.countVolunteerOrdersInRound(
                        request.getVolunteerId(), roundId));
                responseData.put("roundCapacity", capacity);
            }

            return ResponseUtil.success("Order accepted successfully", responseData);

        } catch (RuntimeException e) {
            if (e.getMessage().contains("ORDER_ALREADY_ACCEPTED")) {
                return ResponseUtil.conflict(e.getMessage());
            }
            logger.error("Error accepting order: {}", e.getMessage());
            return ResponseUtil.internalError("Failed to accept order");
        }
    }


    public ResponseEntity<Map<String, Object>> getOrderStatus(GetOrderStatusRequest request) {
        if (!"true".equals(request.getAuthStatus())) {
            return ResponseUtil.unauthorized();
        }

        try {
            Order order = orderService.getOrder(request.getOrderId(), request.getUserId(), request.getUserRole());
            Optional<OrderAssignment> assignment = orderAssignmentService.getOrderAssignment(request.getOrderId());

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("orderId", order.getOrderId());
            responseData.put("currentStatus", order.getStatus());

            if (assignment.isPresent()) {
                OrderAssignment a = assignment.get();
                Map<String, Object> assignmentData = new HashMap<>();
                assignmentData.put("volunteerId", a.getVolunteerId());
                assignmentData.put("roundId", a.getRoundId() != null ? a.getRoundId() : null);
                assignmentData.put("acceptedAt", a.getAcceptedAt().toString());
                assignmentData.put("status", a.getStatus().toString());
                responseData.put("assignment", assignmentData);
            }

            List<Map<String, Object>> timeline = new ArrayList<>();
            Map<String, Object> createdEvent = new HashMap<>();
            createdEvent.put("status", "CREATED");
            createdEvent.put("timestamp", order.getRequestTime().toString());
            timeline.add(createdEvent);

            if (assignment.isPresent() && assignment.get().getAcceptedAt() != null) {
                Map<String, Object> acceptedEvent = new HashMap<>();
                acceptedEvent.put("status", "ACCEPTED");
                acceptedEvent.put("timestamp", assignment.get().getAcceptedAt().toString());
                acceptedEvent.put("volunteerId", assignment.get().getVolunteerId());
                timeline.add(acceptedEvent);
            }

            responseData.put("timeline", timeline);

            return ResponseUtil.successData(responseData);

        } catch (Exception e) {
            logger.error("Error getting order status: {}", e.getMessage());
            return ResponseUtil.internalError(e.getMessage());
        }
    }

    public ResponseEntity<Map<String, Object>> getRoundCapacity(GetRoundCapacityRequest request) {
        if (!"true".equals(request.getAuthStatus())) {
            return ResponseUtil.unauthorized();
        }

        try {
            RoundCapacityService.RoundCapacityInfo capacityInfo =
                    roundCapacityService.getRoundCapacityInfo(request.getRoundId());

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("roundId", capacityInfo.roundId);

            Map<String, Object> summary = new HashMap<>();
            summary.put("totalVolunteers", capacityInfo.volunteerCount);
            summary.put("totalOrders", capacityInfo.totalOrders);
            summary.put("maxCapacity", capacityInfo.maxCapacity);
            summary.put("availableSlots", capacityInfo.availableSlots);
            summary.put("maxOrdersPerVolunteer", capacityInfo.maxOrdersPerVolunteer);
            responseData.put("summary", summary);

            return ResponseUtil.successData(responseData);

        } catch (Exception e) {
            logger.error("Error getting round capacity: {}", e.getMessage());
            return ResponseUtil.internalError("Failed to get round capacity");
        }
    }

    @Transactional
    public ResponseEntity<Map<String, Object>> updateRoundCapacity(UpdateRoundCapacityRequest request) {
        if (!"true".equals(request.getAuthenticated())) {
            return ResponseUtil.unauthorized();
        }

        try {
            roundCapacityService.updateCapacity(
                    request.getRoundId(),
                    request.getMaxOrdersPerVolunteer(),
                    request.getAdminId()
            );

            return ResponseUtil.success("Round capacity updated successfully");

        } catch (Exception e) {
            logger.error("Error updating round capacity: {}", e.getMessage());
            return ResponseUtil.internalError("Failed to update round capacity");
        }
    }

    /**
     * Get all orders (Admin and Volunteer can view, but with different permissions)
     */
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getAllOrders(GetAllOrdersRequest request) {
        if (!Boolean.TRUE.equals(request.getAuthenticated())) {
            return ResponseUtil.unauthorized();
        }

        // Allow both ADMIN and VOLUNTEER to view orders
        if (!"VOLUNTEER".equals(request.getUserRole()) && !"ADMIN".equals(request.getUserRole())) {
            return ResponseUtil.forbidden("Only volunteers and admins can view orders");
        }

        try {
            // Both roles see all orders
            List<Order> allOrders = orderRepository.findAll();

            // Sort by request time (most recent first)
            allOrders.sort(Comparator.comparing(Order::getRequestTime).reversed());

            // Convert to response format
            List<Map<String, Object>> ordersList = new ArrayList<>();
            for (Order order : allOrders) {
                Map<String, Object> orderData = new HashMap<>();
                orderData.put("orderId", order.getOrderId());
                orderData.put("status", order.getStatus());
                orderData.put("requestTime", order.getRequestTime());
                orderData.put("orderType", order.getOrderType());
                orderData.put("userId", order.getUserId());
                orderData.put("deliveryAddress", order.getDeliveryAddress());
                orderData.put("phoneNumber", order.getPhoneNumber());
                orderData.put("notes", order.getNotes());
                orderData.put("roundId", order.getRoundId());

                // Add order items
                if (order.getOrderItems() != null) {
                    orderData.put("orderItems", order.getOrderItems());
                }

                // Add assignment info if exists
                Optional<OrderAssignment> assignment = orderAssignmentService.getOrderAssignment(order.getOrderId());
                if (assignment.isPresent()) {
                    orderData.put("assignedVolunteerId", assignment.get().getVolunteerId());
                    orderData.put("assignmentStatus", assignment.get().getStatus());
                }

                ordersList.add(orderData);
            }

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("status", "success");
            responseData.put("orders", ordersList);
            responseData.put("total", ordersList.size());
            responseData.put("userRole", request.getUserRole()); // Include role for frontend

            return ResponseUtil.successData(responseData);

        } catch (Exception e) {
            logger.error("Error fetching all orders: {}", e.getMessage());
            return ResponseUtil.internalError("Failed to fetch orders: " + e.getMessage());
        }
    }
}