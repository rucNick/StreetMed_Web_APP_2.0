package com.backend.streetmed_backend.entity.rounds_entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "round_signups",
        uniqueConstraints = @UniqueConstraint(columnNames = {"round_id", "user_id"}))
public class RoundSignup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "signup_id")
    private Integer signupId;

    @Column(name = "round_id", nullable = false)
    private Integer roundId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "role", nullable = false)
    private String role; // VOLUNTEER, CLINICIAN, TEAM_LEAD

    @Column(name = "status", nullable = false)
    private String status; // PENDING, CONFIRMED, WAITLISTED, CANCELED

    @Column(name = "lottery_number")
    private Integer lotteryNumber;

    @Column(name = "signup_time", nullable = false)
    private LocalDateTime signupTime;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Default constructor
    public RoundSignup() {
        this.signupTime = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = "PENDING";
    }

    // Constructor with required fields
    public RoundSignup(Integer roundId, Integer userId, String role) {
        this();
        this.roundId = roundId;
        this.userId = userId;
        this.role = role;
    }

    // Getters and Setters
    public Integer getSignupId() {
        return signupId;
    }

    public void setSignupId(Integer signupId) {
        this.signupId = signupId;
    }

    public Integer getRoundId() {
        return roundId;
    }

    public void setRoundId(Integer roundId) {
        this.roundId = roundId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getLotteryNumber() {
        return lotteryNumber;
    }

    public void setLotteryNumber(Integer lotteryNumber) {
        this.lotteryNumber = lotteryNumber;
    }

    public LocalDateTime getSignupTime() {
        return signupTime;
    }

    public void setSignupTime(LocalDateTime signupTime) {
        this.signupTime = signupTime;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Helper methods
    public boolean isTeamLead() {
        return "TEAM_LEAD".equals(role);
    }

    public boolean isClinician() {
        return "CLINICIAN".equals(role);
    }

    public boolean isConfirmed() {
        return "CONFIRMED".equals(status);
    }

    public boolean isWaitlisted() {
        return "WAITLISTED".equals(status);
    }

    // Pre-update callback
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}