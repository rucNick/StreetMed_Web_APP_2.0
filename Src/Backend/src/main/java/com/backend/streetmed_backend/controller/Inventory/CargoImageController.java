package com.backend.streetmed_backend.controller.Inventory;

import com.backend.streetmed_backend.document.CargoImage;
import com.backend.streetmed_backend.service.CargoImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/cargo/images")
public class CargoImageController {

    private final CargoImageService cargoImageService;

    @Autowired
    public CargoImageController(CargoImageService cargoImageService) {
        this.cargoImageService = cargoImageService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "cargoItemId", required = false) Integer cargoItemId,
            @RequestHeader("Authentication-Status") String authStatus) {

        try {
            if (!"true".equals(authStatus)) {
                return ResponseEntity.status(401).body(Map.of(
                        "status", "error",
                        "message", "Not authenticated"
                ));
            }

            CargoImage savedImage = cargoImageService.storeImage(file, cargoItemId);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "imageId", savedImage.getId(),
                    "message", "Image uploaded successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/{imageId}")
    public ResponseEntity<?> getImage(@PathVariable String imageId) {
        try {
            CargoImage image = cargoImageService.getImage(imageId);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(image.getContentType()))
                    .body(image.getData());
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{imageId}")
    public ResponseEntity<?> deleteImage(
            @PathVariable String imageId,
            @RequestHeader("Authentication-Status") String authStatus) {

        try {
            if (!"true".equals(authStatus)) {
                return ResponseEntity.status(401).body(Map.of(
                        "status", "error",
                        "message", "Not authenticated"
                ));
            }

            cargoImageService.deleteImage(imageId);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Image deleted successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }
}