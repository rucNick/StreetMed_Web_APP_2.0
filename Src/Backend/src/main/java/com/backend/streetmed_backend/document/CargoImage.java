package com.backend.streetmed_backend.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "cargoImages")
public class CargoImage {
    @Id
    private String id;

    private String filename;
    private String contentType;
    private byte[] data;
    private Long size;
    private LocalDateTime uploadDate;
    private String cargoItemId;

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public byte[] getData() { return data; }
    public void setData(byte[] data) { this.data = data; }

    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }

    public LocalDateTime getUploadDate() { return uploadDate; }
    public void setUploadDate(LocalDateTime uploadDate) { this.uploadDate = uploadDate; }

    public String getCargoItemId() { return cargoItemId; }
    public void setCargoItemId(String cargoItemId) { this.cargoItemId = cargoItemId; }
}