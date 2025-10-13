package com.backend.streetmed_backend.dto.admin;

public class GetUserDetailsRequest {
    private String adminUsername;
    private String authStatus;
    private Integer userId;

    public GetUserDetailsRequest(String adminUsername, String authStatus, Integer userId) {
        this.adminUsername = adminUsername;
        this.authStatus = authStatus;
        this.userId = userId;
    }

    // Getters and setters

    public String getAdminUsername() {
        return adminUsername;
    }

    public void setAdminUsername(String adminUsername) {
        this.adminUsername = adminUsername;
    }

    public String getAuthStatus() {
        return authStatus;
    }

    public void setAuthStatus(String authStatus) {
        this.authStatus = authStatus;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }
}
