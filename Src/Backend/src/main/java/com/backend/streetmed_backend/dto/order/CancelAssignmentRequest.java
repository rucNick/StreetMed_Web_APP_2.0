package com.backend.streetmed_backend.dto.order;

public class CancelAssignmentRequest {
    private String authStatus;
    private Integer userId;
    private String userRole;
    private Integer orderId;

    // Constructors
    public CancelAssignmentRequest() {}

    public CancelAssignmentRequest(String authStatus, Integer userId, String userRole, Integer orderId) {
        this.authStatus = authStatus;
        this.userId = userId;
        this.userRole = userRole;
        this.orderId = orderId;
    }

    // Getters and Setters
    public String getAuthStatus() { return authStatus; }
    public void setAuthStatus(String authStatus) { this.authStatus = authStatus; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }

    public Integer getOrderId() { return orderId; }
    public void setOrderId(Integer orderId) { this.orderId = orderId; }
}