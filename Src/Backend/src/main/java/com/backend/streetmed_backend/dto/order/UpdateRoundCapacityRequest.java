package com.backend.streetmed_backend.dto.order;

public class UpdateRoundCapacityRequest {
    private String authenticated;
    private String adminUsername;
    private Integer adminId;
    private Integer roundId;
    private Integer maxOrdersPerVolunteer;

    // Getters and Setters
    public String getAuthenticated() { return authenticated; }
    public void setAuthenticated(String authenticated) { this.authenticated = authenticated; }

    public String getAdminUsername() { return adminUsername; }
    public void setAdminUsername(String adminUsername) { this.adminUsername = adminUsername; }

    public Integer getAdminId() { return adminId; }
    public void setAdminId(Integer adminId) { this.adminId = adminId; }

    public Integer getRoundId() { return roundId; }
    public void setRoundId(Integer roundId) { this.roundId = roundId; }

    public Integer getMaxOrdersPerVolunteer() { return maxOrdersPerVolunteer; }
    public void setMaxOrdersPerVolunteer(Integer max) { this.maxOrdersPerVolunteer = max; }
}