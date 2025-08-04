package com.backend.streetmed_backend.repository.Cargo;

import com.backend.streetmed_backend.document.CargoImage;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface CargoImageRepository extends MongoRepository<CargoImage, String> {
    Optional<CargoImage> findByCargoItemId(String cargoItemId);
    void deleteByCargoItemId(String cargoItemId);
}