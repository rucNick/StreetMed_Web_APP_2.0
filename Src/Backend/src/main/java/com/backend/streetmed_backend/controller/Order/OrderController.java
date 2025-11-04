package com.backend.streetmed_backend.controller.Order;

import com.backend.streetmed_backend.dto.order.*;
import com.backend.streetmed_backend.entity.order_entity.Order;
import com.backend.streetmed_backend.entity.order_entity.OrderAssignment;
import com.backend.streetmed_backend.entity.order_entity.OrderItem;
import com.backend.streetmed_backend.security.TLSService;
import com.backend.streetmed_backend.service.orderService.OrderManagementService;
import com.backend.streetmed_backend.service.orderService.OrderAssignmentService;
import com.backend.streetmed_backend.service.orderService.OrderService;
import com.backend.streetmed_backend.service.orderService.OrderRateLimitService;
import com.backend.streetmed_backend.service.roundService.RoundCapacityService;
import com.backend.streetmed_backend.util.ResponseUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Tag(name = "Order Management", description = "APIs for managing orders")
@RestController
@RequestMapping("/api/orders")
@CrossOrigin
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    private final OrderManagementService orderManagementService;
    private final OrderAssignmentService orderAssignmentService;
    private final OrderService orderService;
    private final TLSService tlsService;
    private final Executor authExecutor;
    private final Executor readOnlyExecutor;

    @Autowired
    public OrderController(OrderManagementService orderManagementService,
                           OrderAssignmentService orderAssignmentService,
                           OrderService orderService,
                           TLSService tlsService,
                           @Qualifier("authExecutor") Executor authExecutor,
                           @Qualifier("readOnlyExecutor") Executor readOnlyExecutor) {
        this.orderManagementService = orderManagementService;
        this.orderAssignmentService = orderAssignmentService;
        this.orderService = orderService;
        this.tlsService = tlsService;
        this.authExecutor = authExecutor;
        this.readOnlyExecutor = readOnlyExecutor;
    }

    @Operation(summary = "Get pending orders with priority queue")
    @GetMapping("/pending")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getPendingOrders(
            @RequestHeader("Authentication-Status") String authStatus,
            @RequestHeader("User-Id") Integer userId,
            @RequestHeader("User-Role") String userRole,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            HttpServletRequest httpRequest) {

        return CompletableFuture.supplyAsync(() -> {
            GetPendingOrdersRequest request = new GetPendingOrdersRequest(authStatus, userId, userRole, page, size);
            return orderManagementService.getPendingOrders(request);
        }, readOnlyExecutor);
    }

    @Operation(summary = "Accept an order")
    @PostMapping("/{orderId}/accept")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> acceptOrder(
            @PathVariable Integer orderId,
            @RequestBody AcceptOrderRequest request,
            HttpServletRequest httpRequest) {

        request.setOrderId(orderId);
        return CompletableFuture.supplyAsync(() ->
                orderManagementService.acceptOrder(request), authExecutor);
    }

    @Operation(summary = "Create a new order")
    @PostMapping("/create")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> createOrder(
            @RequestBody CreateOrderRequest request,
            HttpServletRequest httpRequest) {

        return CompletableFuture.supplyAsync(() -> {
            // Set guest user ID if not authenticated
            if (!Boolean.TRUE.equals(request.getAuthenticated()) || request.getUserId() == null) {
                if (request.getUserId() == null) {
                    request.setUserId(-1); // Guest user ID
                }
            }

            try {
                // Extract client IP address
                String clientIpAddress = extractClientIp(httpRequest);

                Order order = new Order(Order.OrderType.CLIENT);
                order.setUserId(request.getUserId());
                order.setDeliveryAddress(request.getDeliveryAddress());
                order.setPhoneNumber(request.getPhoneNumber());
                order.setNotes(request.getNotes());
                order.setLatitude(request.getLatitude());
                order.setLongitude(request.getLongitude());

                List<OrderItem> orderItems = new ArrayList<>();
                if (request.getItems() != null) {
                    for (Map<String, Object> itemData : request.getItems()) {
                        OrderItem item = new OrderItem();
                        item.setItemName((String) itemData.get("itemName"));
                        item.setQuantity((Integer) itemData.get("quantity"));
                        orderItems.add(item);
                    }
                }

                // Pass IP address to service
                Order savedOrder = orderService.createOrder(order, orderItems, clientIpAddress);

                Map<String, Object> responseData = new HashMap<>();
                responseData.put("orderId", savedOrder.getOrderId());
                responseData.put("status", savedOrder.getStatus());
                responseData.put("roundId", savedOrder.getRoundId());

                return ResponseUtil.success("Order created successfully", responseData);

            } catch (OrderRateLimitService.RateLimitExceededException e) {
                return ResponseUtil.error(e.getMessage(), HttpStatus.TOO_MANY_REQUESTS, true);
            } catch (Exception e) {
                logger.error("Error creating order: {}", e.getMessage());
                return ResponseUtil.badRequest(e.getMessage());
            }
        }, authExecutor);
    }

    @Operation(summary = "Cancel order assignment")
    @DeleteMapping("/{orderId}/assignment")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> cancelOrderAssignment(
            @PathVariable Integer orderId,
            @RequestHeader("User-Id") Integer userId,
            @RequestHeader("User-Role") String userRole,
            @RequestHeader("Authentication-Status") String authStatus,
            HttpServletRequest httpRequest) {

        return CompletableFuture.supplyAsync(() -> {
            CancelAssignmentRequest request = new CancelAssignmentRequest(authStatus, userId, userRole, orderId);

            if (!"true".equals(request.getAuthStatus())) {
                return ResponseUtil.unauthorized();
            }

            if (!"VOLUNTEER".equals(request.getUserRole())) {
                return ResponseUtil.forbidden("Only volunteers can cancel assignments");
            }

            try {
                // Get the assignment for this order
                Optional<OrderAssignment> assignment = orderAssignmentService.getOrderAssignment(orderId);
                if (assignment.isEmpty()) {
                    return ResponseUtil.notFound("No assignment found for this order");
                }

                // Verify the volunteer owns this assignment
                if (!assignment.get().getVolunteerId().equals(userId)) {
                    return ResponseUtil.forbidden("You can only cancel your own assignments");
                }

                // Cancel through assignment service
                OrderAssignment cancelledAssignment = orderAssignmentService.cancelAssignment(
                        assignment.get().getAssignmentId(), userId
                );

                Map<String, Object> responseData = new HashMap<>();
                responseData.put("orderId", orderId);
                responseData.put("status", "Order returned to pending queue");
                responseData.put("assignmentStatus", cancelledAssignment.getStatus().toString());

                return ResponseUtil.success("Assignment cancelled successfully", responseData);

            } catch (Exception e) {
                logger.error("Error cancelling assignment: {}", e.getMessage());
                return ResponseUtil.internalError(e.getMessage());
            }
        }, authExecutor);
    }

    @Operation(summary = "Get my active assignments")
    @GetMapping("/my-assignments")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getMyAssignments(
            @RequestHeader("User-Id") Integer userId,
            @RequestHeader("User-Role") String userRole,
            @RequestHeader("Authentication-Status") String authStatus,
            HttpServletRequest httpRequest) {

        return CompletableFuture.supplyAsync(() -> {
            GetMyAssignmentsRequest request = new GetMyAssignmentsRequest(authStatus, userId, userRole);

            if (!"true".equals(request.getAuthStatus())) {
                return ResponseUtil.unauthorized();
            }

            if (!"VOLUNTEER".equals(request.getUserRole())) {
                return ResponseUtil.forbidden("Only volunteers can view assignments");
            }

            try {
                List<OrderAssignment> assignments = orderAssignmentService.getActiveAssignments(userId);

                // Enrich with order details
                List<Map<String, Object>> enrichedAssignments = new ArrayList<>();
                for (OrderAssignment assignment : assignments) {
                    Map<String, Object> assignmentData = new HashMap<>();
                    assignmentData.put("assignmentId", assignment.getAssignmentId());
                    assignmentData.put("orderId", assignment.getOrderId());
                    assignmentData.put("status", assignment.getStatus().toString());
                    assignmentData.put("acceptedAt", assignment.getAcceptedAt());
                    assignmentData.put("roundId", assignment.getRoundId());

                    // Get order details
                    try {
                        Order order = orderService.getOrder(
                                assignment.getOrderId(), userId, userRole
                        );
                        assignmentData.put("deliveryAddress", order.getDeliveryAddress());
                        assignmentData.put("phoneNumber", order.getPhoneNumber());
                        assignmentData.put("notes", order.getNotes());
                        assignmentData.put("items", order.getOrderItems());
                        assignmentData.put("requestTime", order.getRequestTime());
                    } catch (Exception e) {
                        logger.warn("Could not fetch order details for assignment {}: {}",
                                assignment.getAssignmentId(), e.getMessage());
                    }

                    enrichedAssignments.add(assignmentData);
                }

                Map<String, Object> responseData = new HashMap<>();
                responseData.put("assignments", enrichedAssignments);
                responseData.put("totalActive", assignments.size());

                return ResponseUtil.successData(responseData);

            } catch (Exception e) {
                logger.error("Error fetching assignments: {}", e.getMessage());
                return ResponseUtil.internalError(e.getMessage());
            }
        }, readOnlyExecutor);
    }

    @Operation(summary = "Start working on assigned order")
    @PutMapping("/assignment/{assignmentId}/start")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> startAssignment(
            @PathVariable Integer assignmentId,
            @RequestHeader("User-Id") Integer userId,
            @RequestHeader("User-Role") String userRole,
            @RequestHeader("Authentication-Status") String authStatus,
            HttpServletRequest httpRequest) {

        return CompletableFuture.supplyAsync(() -> {
            if (!"true".equals(authStatus)) {
                return ResponseUtil.unauthorized();
            }

            if (!"VOLUNTEER".equals(userRole)) {
                return ResponseUtil.forbidden("Only volunteers can manage assignments");
            }

            try {
                OrderAssignment assignment = orderAssignmentService.startOrder(assignmentId, userId);

                Map<String, Object> responseData = new HashMap<>();
                responseData.put("assignmentId", assignment.getAssignmentId());
                responseData.put("orderId", assignment.getOrderId());
                responseData.put("status", assignment.getStatus().toString());
                responseData.put("message", "Order processing started");

                return ResponseUtil.successData(responseData);

            } catch (Exception e) {
                logger.error("Error starting assignment: {}", e.getMessage());
                return ResponseUtil.internalError(e.getMessage());
            }
        }, authExecutor);
    }

    @Operation(summary = "Complete assigned order")
    @PutMapping("/assignment/{assignmentId}/complete")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> completeAssignment(
            @PathVariable Integer assignmentId,
            @RequestHeader("User-Id") Integer userId,
            @RequestHeader("User-Role") String userRole,
            @RequestHeader("Authentication-Status") String authStatus,
            HttpServletRequest httpRequest) {

        return CompletableFuture.supplyAsync(() -> {
            if (!"true".equals(authStatus)) {
                return ResponseUtil.unauthorized();
            }

            if (!"VOLUNTEER".equals(userRole)) {
                return ResponseUtil.forbidden("Only volunteers can manage assignments");
            }

            try {
                OrderAssignment assignment = orderAssignmentService.completeOrder(assignmentId, userId);

                Map<String, Object> responseData = new HashMap<>();
                responseData.put("assignmentId", assignment.getAssignmentId());
                responseData.put("orderId", assignment.getOrderId());
                responseData.put("status", assignment.getStatus().toString());
                responseData.put("message", "Order completed successfully");

                return ResponseUtil.successData(responseData);

            } catch (Exception e) {
                logger.error("Error completing assignment: {}", e.getMessage());
                return ResponseUtil.internalError(e.getMessage());
            }
        }, authExecutor);
    }

    @Operation(summary = "Get order status")
    @GetMapping("/{orderId}/status")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getOrderStatus(
            @PathVariable Integer orderId,
            @RequestHeader("Authentication-Status") String authStatus,
            @RequestHeader("User-Id") Integer userId,
            @RequestHeader("User-Role") String userRole,
            HttpServletRequest httpRequest) {

        return CompletableFuture.supplyAsync(() -> {
            GetOrderStatusRequest request = new GetOrderStatusRequest(authStatus, userId, userRole, orderId);
            return orderManagementService.getOrderStatus(request);
        }, readOnlyExecutor);
    }

    @Operation(summary = "Get all orders (Admin/Volunteer)")
    @GetMapping("/all")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getAllOrders(
            @RequestParam(required = false) Boolean authenticated,
            @RequestParam(required = false) Integer userId,
            @RequestParam(required = false) String userRole,
            @RequestHeader(value = "Admin-Username", required = false) String adminUsername,
            @RequestHeader(value = "Authentication-Status", required = false) String authStatus,
            @RequestHeader(value = "X-Auth-Token", required = false) String authToken,
            HttpServletRequest httpRequest) {

        // Enforce HTTPS for admin operations
        if (tlsService.isHttpsRequired(httpRequest, true)) {
            return CompletableFuture.completedFuture(
                    ResponseUtil.httpsRequired("Admin operations require secure HTTPS connection"));
        }

        // Validate admin authentication
        if (!tlsService.isAuthenticated(authToken, authStatus)) {
            return CompletableFuture.completedFuture(
                    ResponseUtil.unauthorized("Authentication required"));
        }

        if (!tlsService.hasRole(authToken, "ADMIN", "VOLUNTEER")) {
            return CompletableFuture.completedFuture(
                    ResponseUtil.forbidden("Insufficient permissions"));
        }

        return CompletableFuture.supplyAsync(() -> {
            GetAllOrdersRequest request = new GetAllOrdersRequest(authenticated, userId, userRole);
            return orderManagementService.getAllOrders(request);
        }, readOnlyExecutor);
    }

    @Operation(summary = "Get round capacity")
    @GetMapping("/rounds/{roundId}/capacity")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getRoundCapacity(
            @PathVariable Integer roundId,
            @RequestHeader("Authentication-Status") String authStatus,
            @RequestHeader("User-Role") String userRole,
            HttpServletRequest httpRequest) {

        return CompletableFuture.supplyAsync(() -> {
            GetRoundCapacityRequest request = new GetRoundCapacityRequest(authStatus, userRole, roundId);
            return orderManagementService.getRoundCapacity(request);
        }, readOnlyExecutor);
    }

    @Operation(summary = "Update round capacity (Admin)")
    @PutMapping("/rounds/{roundId}/capacity")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> updateRoundCapacity(
            @PathVariable Integer roundId,
            @RequestBody UpdateRoundCapacityRequest request,
            HttpServletRequest httpRequest) {

        if (tlsService.isHttpsRequired(httpRequest, true)) {
            return CompletableFuture.completedFuture(
                    ResponseUtil.httpsRequired("Admin operations require secure HTTPS connection"));
        }

        request.setRoundId(roundId);
        return CompletableFuture.supplyAsync(() ->
                orderManagementService.updateRoundCapacity(request), authExecutor);
    }

    // Helper method to extract client IP
    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // Take the first IP if there are multiple (proxy chain)
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}