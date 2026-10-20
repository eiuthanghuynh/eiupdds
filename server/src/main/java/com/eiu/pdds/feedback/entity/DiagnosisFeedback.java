package com.eiu.pdds.feedback.entity;

import com.eiu.pdds.common.entity.BaseAuditEntity;
import com.eiu.pdds.diagnosis.entity.Diagnosis;
import com.eiu.pdds.disease.entity.Disease;
import com.eiu.pdds.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Human-in-the-loop feedback entity capturing farmer rating, user disease suggestions,
 * and expert agronomist validation for active learning and model refinement.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "diagnosis_feedbacks")
public class DiagnosisFeedback extends BaseAuditEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diagnosis_id", nullable = false)
    private Diagnosis diagnosis;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "is_accurate", nullable = false)
    private Boolean isAccurate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suggested_disease_id")
    private Disease suggestedDisease;

    @Column(name = "user_comment", columnDefinition = "TEXT")
    private String userComment;

    @Column(name = "expert_verified", nullable = false)
    @Builder.Default
    private Boolean expertVerified = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by_user_id")
    private User verifiedByUser;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "expert_notes", columnDefinition = "TEXT")
    private String expertNotes;
}
