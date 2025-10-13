package com.backend.streetmed_backend.dto.admin;

import java.util.Map;

public class UpdateUserRequest {
    private Integer userId;
    private String adminUsername;
    private String authenticated;
    private Map<String, String> updateData;

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getAdminUsername() {
        return adminUsername;
    }

    public void setAdminUsername(String adminUsername) {
        this.adminUsername = adminUsername;
    }

    public String getAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(String authenticated) {
        this.authenticated = authenticated;
    }

    public Map<String, String> getUpdateData() {
        return updateData;
    }

    public void setUpdateData(Map<String, String> updateData) {
        this.updateData = updateData;
    }
}