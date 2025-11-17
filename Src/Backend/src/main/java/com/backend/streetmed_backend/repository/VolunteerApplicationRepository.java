// VolunteerApplicationRepository.java
package com.backend.streetmed_backend.repository;

import com.backend.streetmed_backend.entity.user_entity.VolunteerApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VolunteerApplicationRepository extends JpaRepository<VolunteerApplication, Integer> {
    List<VolunteerApplication> findByStatus(VolunteerApplication.ApplicationStatus status);
    Optional<VolunteerApplication> findByEmail(String email);
    Optional<VolunteerApplication> findByUser_UserId(Integer userId);
    boolean existsByEmail(String email);
}