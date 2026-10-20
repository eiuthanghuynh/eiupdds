package com.eiu.pdds.feedback.repository;

import com.eiu.pdds.feedback.entity.DiagnosisFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DiagnosisFeedbackRepository extends JpaRepository<DiagnosisFeedback, UUID> {

    List<DiagnosisFeedback> findByDiagnosisIdAndDeletedAtIsNull(UUID diagnosisId);

    List<DiagnosisFeedback> findByExpertVerifiedFalseAndDeletedAtIsNull();

    List<DiagnosisFeedback> findByUserIdAndDeletedAtIsNull(UUID userId);

    List<DiagnosisFeedback> findByDeletedAtIsNull();
}
