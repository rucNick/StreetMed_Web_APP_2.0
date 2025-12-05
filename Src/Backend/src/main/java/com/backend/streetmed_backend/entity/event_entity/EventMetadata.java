package com.backend.streetmed_backend.entity.event_entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "event_metadata")
public class EventMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "metadata_id")
    private Integer metadataId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "event_date")
    private LocalDateTime eventDate;

    @Column(name = "event_end_date")
    private LocalDateTime eventEndDate;

    @Column(name = "max_participants")
    private Integer maxParticipants;

    @Column(name = "current_participants")
    private Integer currentParticipants = 0;

    @Column(name = "is_recurring")
    private Boolean isRecurring = false;

    @Column(name = "recurrence_pattern")
    private String recurrencePattern;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    // Default constructor with initialization
    public EventMetadata() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Integer getMetadataId() {
        return metadataId;
    }

    public void setMetadataId(Integer metadataId) {
        this.metadataId = metadataId;
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

    public LocalDateTime getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDateTime eventDate) {
        this.eventDate = eventDate;
    }

    public LocalDateTime getEventEndDate() {
        return eventEndDate;
    }

    public void setEventEndDate(LocalDateTime eventEndDate) {
        this.eventEndDate = eventEndDate;
    }

    public Integer getMaxParticipants() {
        return maxParticipants;
    }

    public void setMaxParticipants(Integer maxParticipants) {
        this.maxParticipants = maxParticipants;
    }

    public Integer getCurrentParticipants() {
        return currentParticipants;
    }

    public void setCurrentParticipants(Integer currentParticipants) {
        this.currentParticipants = currentParticipants;
    }

    public Boolean getIsRecurring() {
        return isRecurring;
    }

    public void setIsRecurring(Boolean isRecurring) {
        this.isRecurring = isRecurring;
    }

    public String getRecurrencePattern() {
        return recurrencePattern;
    }

    public void setRecurrencePattern(String recurrencePattern) {
        this.recurrencePattern = recurrencePattern;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}