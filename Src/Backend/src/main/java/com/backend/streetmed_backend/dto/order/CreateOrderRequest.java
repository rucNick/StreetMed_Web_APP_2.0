package com.backend.streetmed_backend.dto.order;

import java.util.List;
import java.util.Map;

public class CreateOrderRequest {
    private Boolean authenticated;
    private Integer userId;
    private String userRole;
    private String deliveryAddress;
    private String phoneNumber;
    private String notes;
    private Double latitude;
    private Double longitude;
    private List<Map<String, Object>> items;

    // Getters and Setters
    public Boolean getAuthenticated() { return authenticated; }
    public void setAuthenticated(Boolean authenticated) { this.authenticated = authenticated; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }

    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public List<Map<String, Object>> getItems() { return items; }
    public void setItems(List<Map<String, Object>> items) { this.items = items; }
}