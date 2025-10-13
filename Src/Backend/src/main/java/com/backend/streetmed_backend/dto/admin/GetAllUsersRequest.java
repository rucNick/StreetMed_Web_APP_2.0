package com.backend.streetmed_backend.dto.admin;

public class GetAllUsersRequest {
    private String adminUsername;
    private final String authStatus;

    public GetAllUsersRequest(String adminUsername, String authStatus) {
        this.adminUsername = adminUsername;
        this.authStatus = authStatus;
    }

    public String getAdminUsername() {
        return adminUsername;
    }

    public void setAdminUsername(String adminUsername) {
        this.adminUsername = adminUsername;
    }

    public String getAuthStatus() {
        return authStatus;
    }
}