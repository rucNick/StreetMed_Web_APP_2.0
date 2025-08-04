// VolunteerApplicationService.java
package com.backend.streetmed_backend.service;

import com.backend.streetmed_backend.entity.user_entity.User;
import com.backend.streetmed_backend.entity.user_entity.VolunteerApplication;
import com.backend.streetmed_backend.repository.VolunteerApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


@Service
public class VolunteerApplicationService {

    private final VolunteerApplicationRepository volunteerApplicationRepository;

    @Autowired
    public VolunteerApplicationService(VolunteerApplicationRepository volunteerApplicationRepository) {
        this.volunteerApplicationRepository = volunteerApplicationRepository;
    }

    public VolunteerApplication submitApplication(VolunteerApplication application) {
        if (existsByEmail(application.getEmail())) {
            throw new RuntimeException("An application with this email already exists");
        }
        application.setStatus(VolunteerApplication.ApplicationStatus.PENDING);
        return volunteerApplicationRepository.save(application);
    }

    public List<VolunteerApplication> getAllApplications() {
        return volunteerApplicationRepository.findAll();
    }

    public List<VolunteerApplication> getPendingApplications() {
        return volunteerApplicationRepository.findByStatus(VolunteerApplication.ApplicationStatus.PENDING);
    }

    public Optional<VolunteerApplication> getApplicationById(Integer id) {
        return volunteerApplicationRepository.findById(id);
    }

    @Transactional
    public VolunteerApplication approveApplication(Integer applicationId, User user) {
        Optional<VolunteerApplication> applicationOpt = volunteerApplicationRepository.findById(applicationId);

        if (applicationOpt.isPresent()) {
            VolunteerApplication application = applicationOpt.get();
            application.setStatus(VolunteerApplication.ApplicationStatus.APPROVED);
            application.setUser(user);
            return volunteerApplicationRepository.save(application);
        }

        throw new RuntimeException("Application not found with id: " + applicationId);
    }

    @Transactional
    public VolunteerApplication rejectApplication(Integer applicationId) {
        Optional<VolunteerApplication> applicationOpt = volunteerApplicationRepository.findById(applicationId);

        if (applicationOpt.isPresent()) {
            VolunteerApplication application = applicationOpt.get();
            application.setStatus(VolunteerApplication.ApplicationStatus.REJECTED);
            return volunteerApplicationRepository.save(application);
        }

        throw new RuntimeException("Application not found with id: " + applicationId);
    }

    public boolean existsByEmail(String email) {
        return volunteerApplicationRepository.existsByEmail(email);
    }

    public Optional<VolunteerApplication> findByEmail(String email) {
        return volunteerApplicationRepository.findByEmail(email);
    }

    public Optional<VolunteerApplication> findByUserId(Integer userId) {
        return volunteerApplicationRepository.findByUser_UserId(userId);
    }
}
