package com.backend.streetmed_backend.service.cargoService;

import com.backend.streetmed_backend.entity.CargoImage;
import com.backend.streetmed_backend.repository.Cargo.CargoImageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class CargoImageService {
    private final CargoImageRepository imageRepository;

    @Autowired
    public CargoImageService(CargoImageRepository imageRepository) {
        this.imageRepository = imageRepository;
    }

    @Transactional
    public CargoImage storeImage(MultipartFile file, Integer cargoItemId) throws IOException {
        CargoImage image = new CargoImage();
        image.setFilename(file.getOriginalFilename());
        image.setContentType(file.getContentType());
        image.setData(file.getBytes());
        image.setSize(file.getSize());
        image.setUploadDate(LocalDateTime.now());
        image.setCargoItemId(cargoItemId);

        return imageRepository.save(image);
    }

    public CargoImage getImage(Integer imageId) {
        return imageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Image not found with ID: " + imageId));
    }

    public Optional<CargoImage> findByCargoItemId(Integer cargoItemId) {
        return imageRepository.findByCargoItemId(cargoItemId);
    }

    @Transactional
    public void deleteImage(Integer imageId) {
        imageRepository.deleteById(imageId);
    }

    @Transactional
    public void deleteByCargoItemId(Integer cargoItemId) {
        imageRepository.deleteByCargoItemId(cargoItemId);
    }
}