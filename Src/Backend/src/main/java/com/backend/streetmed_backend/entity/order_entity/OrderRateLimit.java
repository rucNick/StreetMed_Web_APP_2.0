package com.backend.streetmed_backend.entity.order_entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_rate_limits",
        indexes = {
                @Index(name = "idx_user_id_timestamp", columnList = "user_id,request_timestamp"),
                @Index(name = "idx_ip_address_timestamp", columnList = "ip_address,request_timestamp")
        })
public class OrderRateLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "limit_id")
    private Integer limitId;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "request_timestamp", nullable = false)
    private LocalDateTime requestTimestamp;

    @Column(name = "order_id")
    private Integer orderId;

    // Default constructor
    public OrderRateLimit() {
        this.requestTimestamp = LocalDateTime.now();
    }

    // Constructor for user-based limit
    public OrderRateLimit(Integer userId, Integer orderId) {
        this();
        this.userId = userId;
        this.orderId = orderId;
    }

    // Constructor for IP-based limit
    public OrderRateLimit(String ipAddress, Integer orderId) {
        this();
        this.ipAddress = ipAddress;
        this.orderId = orderId;
    }

    // Getters and Setters
    public Integer getLimitId() {
        return limitId;
    }

    public void setLimitId(Integer limitId) {
        this.limitId = limitId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public LocalDateTime getRequestTimestamp() {
        return requestTimestamp;
    }

    public void setRequestTimestamp(LocalDateTime requestTimestamp) {
        this.requestTimestamp = requestTimestamp;
    }

    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }
}