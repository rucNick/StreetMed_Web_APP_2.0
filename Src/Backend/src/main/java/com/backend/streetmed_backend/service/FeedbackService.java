package com.backend.streetmed_backend.service;

import com.backend.streetmed_backend.entity.Service_entity.Feedback;
import com.backend.streetmed_backend.repository.FeedbackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class FeedbackService {
    private final FeedbackRepository feedbackRepository;

    @Autowired
    public FeedbackService(FeedbackRepository feedbackRepository) {
        this.feedbackRepository = feedbackRepository;
    }

    public Feedback submitFeedback(Feedback feedback) {
        // Validate input
        if (feedback.getName() == null || feedback.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Name is required");
        }

        if (feedback.getContent() == null || feedback.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Feedback content is required");
        }

        feedback.setCreatedAt(LocalDateTime.now());
        return feedbackRepository.save(feedback);
    }

    @Transactional(readOnly = true)
    public List<Feedback> getAllFeedbacks() {
        return feedbackRepository.findByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Optional<Feedback> getFeedbackById(Integer id) {
        return feedbackRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Feedback> searchFeedbacks(String name) {
        return feedbackRepository.findByNameContainingIgnoreCase(name);
    }

    @Transactional(readOnly = true)
    public List<Feedback> getFeedbacksByDateRange(LocalDateTime start, LocalDateTime end) {
        return feedbackRepository.findByCreatedAtBetween(start, end);
    }


    public Feedback markAsRead(Integer id) {
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Feedback not found with ID: " + id));

        feedback.setIsRead(true);
        return feedbackRepository.save(feedback);
    }

    public void deleteFeedback(Integer id) {
        feedbackRepository.deleteById(id);
    }
}