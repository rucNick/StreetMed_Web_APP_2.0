package com.backend.streetmed_backend.repository.Cargo;

import com.backend.streetmed_backend.entity.CargoImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CargoImageRepository extends JpaRepository<CargoImage, Integer> {
    Optional<CargoImage> findByCargoItemId(Integer cargoItemId);
    void deleteByCargoItemId(Integer cargoItemId);
}