package com.backend.streetmed_backend.dto.order;

public class GetAllOrdersRequest {
    private Boolean authenticated;
    private Integer userId;
    private String userRole;

    public GetAllOrdersRequest() {}

    public GetAllOrdersRequest(Boolean authenticated, Integer userId, String userRole) {
        this.authenticated = authenticated;
        this.userId = userId;
        this.userRole = userRole;
    }

    // Getters and setters
    public Boolean getAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(Boolean authenticated) {
        this.authenticated = authenticated;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getUserRole() {
        return userRole;
    }

    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }
}