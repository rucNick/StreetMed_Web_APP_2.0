package com.backend.streetmed_backend.entity.rounds_entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "round_capacity_config")
public class RoundCapacityConfig {

    @Id
    @Column(name = "round_id")
    private Integer roundId;

    @Column(name = "max_orders_per_volunteer", nullable = false)
    private Integer maxOrdersPerVolunteer = 3;

    @Column(name = "override_capacity")
    private Integer overrideCapacity;

    @Column(name = "last_modified_by")
    private Integer lastModifiedBy;

    @Column(name = "last_modified_at")
    private LocalDateTime lastModifiedAt;

    // Just use roundId as foreign key

    public RoundCapacityConfig() {
        this.maxOrdersPerVolunteer = 3;
        this.lastModifiedAt = LocalDateTime.now();
    }
    public RoundCapacityConfig(Integer roundId) {
        this();
        this.roundId = roundId;
    }

    // Getters and Setters
    public Integer getRoundId() { return roundId; }
    public void setRoundId(Integer roundId) { this.roundId = roundId; }

    public Integer getMaxOrdersPerVolunteer() { return maxOrdersPerVolunteer; }
    public void setMaxOrdersPerVolunteer(Integer max) {
        this.maxOrdersPerVolunteer = max;
        this.lastModifiedAt = LocalDateTime.now();
    }

    public Integer getOverrideCapacity() { return overrideCapacity; }
    public void setOverrideCapacity(Integer override) {
        this.overrideCapacity = override;
        this.lastModifiedAt = LocalDateTime.now();
    }

    public Integer getLastModifiedBy() { return lastModifiedBy; }
    public void setLastModifiedBy(Integer modifiedBy) { this.lastModifiedBy = modifiedBy; }

    public LocalDateTime getLastModifiedAt() { return lastModifiedAt; }

    @PreUpdate
    protected void onUpdate() {
        this.lastModifiedAt = LocalDateTime.now();
    }
}