package com.backend.streetmed_backend.dto.order;

public class GetRoundCapacityRequest {
    private String authStatus;
    private String userRole;
    private Integer roundId;

    public GetRoundCapacityRequest(String authStatus, String userRole, Integer roundId) {
        this.authStatus = authStatus;
        this.userRole = userRole;
        this.roundId = roundId;
    }

    public String getAuthStatus() { return authStatus; }
    public String getUserRole() { return userRole; }
    public Integer getRoundId() { return roundId; }
}