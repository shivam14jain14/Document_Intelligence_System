package com.docint.repository;

import com.docint.entity.QuestionnaireResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface QuestionnaireResponseRepository extends JpaRepository<QuestionnaireResponse, UUID> {
    Optional<QuestionnaireResponse> findByUserId(UUID userId);
}
