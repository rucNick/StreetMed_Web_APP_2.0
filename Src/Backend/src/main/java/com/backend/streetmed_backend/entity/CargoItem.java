package com.backend.streetmed_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "cargo_items")
public class CargoItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(length = 3000)
    private String description;

    private String category;

    @Column(nullable = false)
    private Integer quantity;

    // For clothes sizes
    @ElementCollection(fetch = FetchType.EAGER)  // Change to EAGER loading
    @CollectionTable(
            name = "cargo_item_sizes",
            joinColumns = @JoinColumn(name = "cargo_item_id")
    )
    @MapKeyColumn(name = "size")
    @Column(name = "quantity")
    private Map<String, Integer> sizeQuantities = new HashMap<>();

    // Reference to SQL image - Changed from String to Integer
    @Column(name = "image_id")
    private Integer imageId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "min_quantity")
    private Integer minQuantity;

    @Column(name = "is_available")
    private Boolean isAvailable = true;

    @Column(name = "needs_prescription")
    private Boolean needsPrescription = false;

    // Default constructor
    public CargoItem() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Constructor with required fields
    public CargoItem(String name, Integer quantity) {
        this();
        this.name = name;
        this.quantity = quantity;
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
        this.updatedAt = LocalDateTime.now();
    }

    public Map<String, Integer> getSizeQuantities() {
        return sizeQuantities;
    }

    // Update the setter to include validation
    public void setSizeQuantities(Map<String, Integer> sizeQuantities) {
        if (sizeQuantities != null) {
            for (Map.Entry<String, Integer> entry : sizeQuantities.entrySet()) {
                if (entry.getValue() < 0) {
                    throw new IllegalArgumentException(
                            String.format("Size '%s' cannot have negative quantity", entry.getKey())
                    );
                }
            }
        }
        this.sizeQuantities = sizeQuantities;
    }

    // Changed getter to return Integer instead of String
    public Integer getImageId() {
        return imageId;
    }

    public void setImageId(Integer imageId) {
        this.imageId = imageId;
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

    public Integer getMinQuantity() {
        return minQuantity;
    }

    public void setMinQuantity(Integer minQuantity) {
        this.minQuantity = minQuantity;
    }

    public Boolean getIsAvailable() {
        return isAvailable;
    }

    public void setIsAvailable(Boolean available) {
        isAvailable = available;
        this.updatedAt = LocalDateTime.now();
    }

    public Boolean getNeedsPrescription() {
        return needsPrescription;
    }

    public void setNeedsPrescription(Boolean needsPrescription) {
        this.needsPrescription = needsPrescription;
    }

    // Helper methods for size management
    public void addSize(String size, Integer quantity) {
        this.sizeQuantities.put(size, quantity);
        this.updatedAt = LocalDateTime.now();
    }

    public Integer getSizeQuantity(String size) {
        return this.sizeQuantities.getOrDefault(size, 0);
    }

    public void updateSizeQuantity(String size, Integer quantity) {
        if (quantity <= 0) {
            this.sizeQuantities.remove(size);
        } else {
            this.sizeQuantities.put(size, quantity);
        }
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isAvailableInSize(String size, Integer requestedQuantity) {
        Integer available = this.sizeQuantities.get(size);
        return available != null && available >= requestedQuantity;
    }

    public void removeSize(String size) {
        this.sizeQuantities.remove(size);
        this.updatedAt = LocalDateTime.now();
    }

    // Business logic methods
    public boolean isLowStock() {
        if (this.minQuantity == null) return false;
        return this.quantity <= this.minQuantity;
    }

    public boolean hasSizes() {
        return !this.sizeQuantities.isEmpty();
    }

    public boolean isAvailableInQuantity(Integer requestedQuantity) {
        return this.quantity >= requestedQuantity;
    }

    // Pre-update callback
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ToString method for logging
    @Override
    public String toString() {
        return "CargoItem{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", category='" + category + '\'' +
                ", quantity=" + quantity +
                ", isAvailable=" + isAvailable +
                '}';
    }
}