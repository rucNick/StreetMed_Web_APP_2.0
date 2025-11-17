package com.backend.streetmed_backend.entity.order_entity;

import com.backend.streetmed_backend.entity.rounds_entity.Rounds;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_assignments",
        uniqueConstraints = @UniqueConstraint(columnNames = {"order_id", "volunteer_id"}))
public class OrderAssignment {

    public enum AssignmentStatus {
        PENDING_ACCEPT,
        ACCEPTED,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "assignment_id")
    private Integer assignmentId;

    @Column(name = "order_id", nullable = false)
    private Integer orderId;

    @Column(name = "volunteer_id", nullable = false)
    private Integer volunteerId;

    @Column(name = "round_id")
    private Integer roundId;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AssignmentStatus status;

    @Version
    @Column(name = "version")
    private Integer version;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    // Default constructor
    public OrderAssignment() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = AssignmentStatus.PENDING_ACCEPT;
        this.version = 0;
    }

    // Constructor with required fields
    public OrderAssignment(Integer orderId, Integer volunteerId) {
        this();
        this.orderId = orderId;
        this.volunteerId = volunteerId;
    }

    // Getters and Setters
    public Integer getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(Integer assignmentId) {
        this.assignmentId = assignmentId;
    }

    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    public Integer getVolunteerId() {
        return volunteerId;
    }

    public void setVolunteerId(Integer volunteerId) {
        this.volunteerId = volunteerId;
    }

    public Integer getRoundId() {
        return roundId;
    }

    public void setRoundId(Integer roundId) {
        this.roundId = roundId;
    }

    public LocalDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(LocalDateTime acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    public AssignmentStatus getStatus() {
        return status;
    }

    public void setStatus(AssignmentStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Helper methods
    public boolean isPendingAccept() {
        return AssignmentStatus.PENDING_ACCEPT.equals(status);
    }

    public boolean isAccepted() {
        return AssignmentStatus.ACCEPTED.equals(status);
    }

    public boolean isInProgress() {
        return AssignmentStatus.IN_PROGRESS.equals(status);
    }

    public boolean isCompleted() {
        return AssignmentStatus.COMPLETED.equals(status);
    }

    public boolean isCancelled() {
        return AssignmentStatus.CANCELLED.equals(status);
    }

    // Pre-update callback
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}