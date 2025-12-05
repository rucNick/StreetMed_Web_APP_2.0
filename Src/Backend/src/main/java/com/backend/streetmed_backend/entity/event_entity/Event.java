package com.backend.streetmed_backend.entity.event_entity;

import jakarta.persistence.*;

@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Integer eventId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", length = 5000)
    private String description;

    @Column(name = "location")
    private String location;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private EventStatus status = EventStatus.DRAFT;

    @OneToOne(mappedBy = "event", cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = false)
    private EventMetadata metadata;

    public enum EventStatus {
        DRAFT,
        PUBLISHED,
        CANCELLED,
        COMPLETED
    }

    // Default constructor
    public Event() {}

    // Constructor with required fields
    public Event(Integer userId, String title) {
        this.userId = userId;
        this.title = title;
    }

    // Constructor with all main fields
    public Event(Integer userId, String title, String description, String location) {
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.location = location;
    }

    public void setMetadata(EventMetadata metadata) {
        if (metadata == null) {
            if (this.metadata != null) {
                this.metadata.setEvent(null);
            }
        } else {
            metadata.setEvent(this);
        }
        this.metadata = metadata;
    }

    // Getters and Setters
    public Integer getEventId() {
        return eventId;
    }

    public void setEventId(Integer eventId) {
        this.eventId = eventId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public EventMetadata getMetadata() {
        return metadata;
    }
}