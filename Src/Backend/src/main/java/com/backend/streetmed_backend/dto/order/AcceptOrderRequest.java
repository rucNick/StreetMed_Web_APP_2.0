package com.backend.streetmed_backend.dto.order;

public class AcceptOrderRequest {
    private Boolean authenticated;
    private Integer volunteerId;
    private String userRole;
    private Integer orderId;
    private Integer version;

    public Boolean getAuthenticated() { return authenticated; }
    public void setAuthenticated(Boolean authenticated) { this.authenticated = authenticated; }

    public Integer getVolunteerId() { return volunteerId; }
    public void setVolunteerId(Integer volunteerId) { this.volunteerId = volunteerId; }

    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }

    public Integer getOrderId() { return orderId; }
    public void setOrderId(Integer orderId) { this.orderId = orderId; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}