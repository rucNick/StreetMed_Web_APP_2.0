package com.backend.streetmed_backend.dto.order;

public class GetOrderStatusRequest {
    private String authStatus;
    private Integer userId;
    private String userRole;
    private Integer orderId;

    public GetOrderStatusRequest(String authStatus, Integer userId, String userRole, Integer orderId) {
        this.authStatus = authStatus;
        this.userId = userId;
        this.userRole = userRole;
        this.orderId = orderId;
    }

    // Getters and Setters
    public String getAuthStatus() { return authStatus; }
    public Integer getUserId() { return userId; }
    public String getUserRole() { return userRole; }
    public Integer getOrderId() { return orderId; }
}