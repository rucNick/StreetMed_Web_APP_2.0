package com.backend.streetmed_backend.repository.User;

import com.backend.streetmed_backend.entity.user_entity.UserMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserMetadataRepository extends JpaRepository<UserMetadata, Integer> {
}