package com.backend.streetmed_backend.dto.admin;

public class DeleteUserRequest {
    private String authenticated;
    private String adminUsername;
    private String username;

    public String getAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(String authenticated) {
        this.authenticated = authenticated;
    }

    public String getAdminUsername() {
        return adminUsername;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setAdminUsername(String adminUsername) {
        this.adminUsername = adminUsername;
    }
}