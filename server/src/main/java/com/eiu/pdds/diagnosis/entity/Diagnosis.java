package com.eiu.pdds.diagnosis.entity;

import com.eiu.pdds.common.entity.BaseAuditEntity;
import com.eiu.pdds.disease.entity.Disease;
import com.eiu.pdds.feedback.entity.DiagnosisFeedback;
import com.eiu.pdds.plant.entity.Plant;
import com.eiu.pdds.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

/**
 * Diagnosis session recording farmer image upload, geolocation, dominant inference outcome,
 * raw AI telemetry, and linked bounding box detections.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "diagnoses")
public class Diagnosis extends BaseAuditEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plant_id")
    private Plant plant;

    @Column(name = "original_image_url", nullable = false)
    private String originalImageUrl;

    @Column(name = "image_width")
    private Integer imageWidth;

    @Column(name = "image_height")
    private Integer imageHeight;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "device_info")
    private String deviceInfo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dominant_disease_id")
    private Disease dominantDisease;

    @Column(name = "dominant_confidence")
    private Double dominantConfidence;

    @Column(name = "inference_time_ms")
    private Integer inferenceTimeMs;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_ai_response", nullable = false)
    @Builder.Default
    private String rawAiResponse = "{}";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private DiagnosisStatus status = DiagnosisStatus.COMPLETED;

    @OneToMany(mappedBy = "diagnosis", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BoundingBox> boundingBoxes = new ArrayList<>();

    @OneToMany(mappedBy = "diagnosis", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DiagnosisFeedback> feedbacks = new ArrayList<>();

    public void addBoundingBox(BoundingBox box) {
        boundingBoxes.add(box);
        box.setDiagnosis(this);
    }

    public void removeBoundingBox(BoundingBox box) {
        boundingBoxes.remove(box);
        box.setDiagnosis(null);
    }

    public void addFeedback(DiagnosisFeedback feedback) {
        feedbacks.add(feedback);
        feedback.setDiagnosis(this);
    }
}
