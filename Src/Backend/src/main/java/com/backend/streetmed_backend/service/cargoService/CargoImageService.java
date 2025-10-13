package com.backend.streetmed_backend.service.cargoService;

import com.backend.streetmed_backend.document.CargoImage;
import com.backend.streetmed_backend.repository.Cargo.CargoImageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.LocalDateTime;

@Service
public class CargoImageService {
    private final CargoImageRepository imageRepository;

    @Autowired
    public CargoImageService(CargoImageRepository imageRepository) {
        this.imageRepository = imageRepository;
    }

    public CargoImage storeImage(MultipartFile file, Integer cargoItemId) throws IOException {
        CargoImage image = new CargoImage();
        image.setFilename(file.getOriginalFilename());
        image.setContentType(file.getContentType());
        image.setData(file.getBytes());
        image.setSize(file.getSize());
        image.setUploadDate(LocalDateTime.now());

        if (cargoItemId != null) {
            image.setCargoItemId(cargoItemId.toString());
        }
        return imageRepository.save(image);
    }

    public CargoImage getImage(String imageId) {
        return imageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Image not found with ID: " + imageId));
    }

    public void deleteImage(String imageId) {
        imageRepository.deleteById(imageId);
    }
}