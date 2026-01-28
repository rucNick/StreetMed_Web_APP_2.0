package com.backend.streetmed_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cargo_images")
public class CargoImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String filename;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Lob
    @Column(name = "data", nullable = false, columnDefinition = "LONGBLOB")
    private byte[] data;

    @Column(name = "size")
    private Long size;

    @Column(name = "upload_date", nullable = false)
    private LocalDateTime uploadDate;

    @Column(name = "cargo_item_id")
    private Integer cargoItemId;

    // Default constructor
    public CargoImage() {
        this.uploadDate = LocalDateTime.now();
    }

    // Getters and setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public LocalDateTime getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(LocalDateTime uploadDate) {
        this.uploadDate = uploadDate;
    }

    public Integer getCargoItemId() {
        return cargoItemId;
    }

    public void setCargoItemId(Integer cargoItemId) {
        this.cargoItemId = cargoItemId;
    }

    @Override
    public String toString() {
        return "CargoImage{" +
                "id=" + id +
                ", filename='" + filename + '\'' +
                ", contentType='" + contentType + '\'' +
                ", size=" + size +
                ", uploadDate=" + uploadDate +
                ", cargoItemId=" + cargoItemId +
                '}';
    }
}