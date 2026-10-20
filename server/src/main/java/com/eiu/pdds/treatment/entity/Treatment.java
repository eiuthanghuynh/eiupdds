package com.eiu.pdds.treatment.entity;

import com.eiu.pdds.common.entity.BaseAuditEntity;
import com.eiu.pdds.disease.entity.Disease;
import com.eiu.pdds.plant.entity.Plant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Treatment entity representing agricultural remedies, chemical formulas,
 * biological controls, and preventive recommendations.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "treatments")
public class Treatment extends BaseAuditEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disease_id", nullable = false)
    private Disease disease;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plant_id")
    private Plant plant;

    @Enumerated(EnumType.STRING)
    @Column(name = "treatment_type", nullable = false)
    private TreatmentType treatmentType;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "instructions", nullable = false, columnDefinition = "TEXT")
    private String instructions;

    @Column(name = "active_ingredients", columnDefinition = "TEXT")
    private String activeIngredients;

    @Column(name = "dosage_recommendation", columnDefinition = "TEXT")
    private String dosageRecommendation;

    @Column(name = "safety_interval_days")
    private Integer safetyIntervalDays;

    @Column(name = "warning", columnDefinition = "TEXT")
    private String warning;
}
