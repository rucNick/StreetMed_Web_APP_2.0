package com.backend.streetmed_backend.dto.order;

public class GetMyAssignmentsRequest {
    private String authStatus;
    private Integer userId;
    private String userRole;

    // Constructors
    public GetMyAssignmentsRequest() {}

    public GetMyAssignmentsRequest(String authStatus, Integer userId, String userRole) {
        this.authStatus = authStatus;
        this.userId = userId;
        this.userRole = userRole;
    }

    // Getters and Setters
    public String getAuthStatus() { return authStatus; }
    public void setAuthStatus(String authStatus) { this.authStatus = authStatus; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }
}