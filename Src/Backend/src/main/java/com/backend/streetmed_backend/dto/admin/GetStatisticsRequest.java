package com.backend.streetmed_backend.dto.admin;

public class GetStatisticsRequest {
    private String adminUsername;
    private String authStatus;

    public GetStatisticsRequest(String adminUsername, String authStatus) {
        this.adminUsername = adminUsername;
        this.authStatus = authStatus;
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
}
