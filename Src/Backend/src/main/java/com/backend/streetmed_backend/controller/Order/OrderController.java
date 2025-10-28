package com.backend.streetmed_backend.controller.Order;

import com.backend.streetmed_backend.dto.order.*;
import com.backend.streetmed_backend.security.TLSService;
import com.backend.streetmed_backend.service.orderService.OrderManagementService;
import com.backend.streetmed_backend.util.ResponseUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Tag(name = "Order Management", description = "APIs for managing orders")
@RestController
@RequestMapping("/api/orders")
@CrossOrigin
public class OrderController {

    private final OrderManagementService orderManagementService;
    private final TLSService tlsService;
    private final Executor authExecutor;
    private final Executor readOnlyExecutor;

    @Autowired
    public OrderController(OrderManagementService orderManagementService,
                           TLSService tlsService,
                           @Qualifier("authExecutor") Executor authExecutor,
                           @Qualifier("readOnlyExecutor") Executor readOnlyExecutor) {
        this.orderManagementService = orderManagementService;
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

        return CompletableFuture.supplyAsync(() ->
                orderManagementService.createOrder(request), authExecutor);
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
}