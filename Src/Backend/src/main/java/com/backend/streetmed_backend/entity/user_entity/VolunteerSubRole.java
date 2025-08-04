package com.backend.streetmed_backend.entity.user_entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "volunteer_sub_roles")
public class VolunteerSubRole {

    public enum SubRoleType {
        CLINICIAN,
        TEAM_LEAD,
        REGULAR
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subrole_id")
    private Integer subroleId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "sub_role", nullable = false)
    private SubRoleType subRole;

    @Column(name = "assigned_date", nullable = false)
    private LocalDateTime assignedDate;

    @Column(name = "assigned_by")
    private Integer assignedBy;

    @Column(name = "notes")
    private String notes;

    // Default constructor
    public VolunteerSubRole() {
        this.assignedDate = LocalDateTime.now();
        this.subRole = SubRoleType.REGULAR;
    }

    // Constructor with required fields
    public VolunteerSubRole(Integer userId, SubRoleType subRole) {
        this();
        this.userId = userId;
        this.subRole = subRole;
    }

    // Getters and Setters
    public Integer getSubroleId() {
        return subroleId;
    }

    public void setSubroleId(Integer subroleId) {
        this.subroleId = subroleId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public SubRoleType getSubRole() {
        return subRole;
    }

    public void setSubRole(SubRoleType subRole) {
        this.subRole = subRole;
    }

    public LocalDateTime getAssignedDate() {
        return assignedDate;
    }

    public void setAssignedDate(LocalDateTime assignedDate) {
        this.assignedDate = assignedDate;
    }

    public Integer getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(Integer assignedBy) {
        this.assignedBy = assignedBy;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}