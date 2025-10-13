package com.backend.streetmed_backend.dto.admin;

public class UpdateVolunteerSubRoleRequest {
    private String adminUsername;
    private String authenticated;
    private Integer userId;
    private String volunteerSubRole;
    private String notes;

    // Getters and setters

    public String getAdminUsername() {
        return adminUsername;
    }

    public void setAdminUsername(String adminUsername) {
        this.adminUsername = adminUsername;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getVolunteerSubRole() {
        return volunteerSubRole;
    }

    public void setVolunteerSubRole(String volunteerSubRole) {
        this.volunteerSubRole = volunteerSubRole;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(String authenticated) {
        this.authenticated = authenticated;
    }
}