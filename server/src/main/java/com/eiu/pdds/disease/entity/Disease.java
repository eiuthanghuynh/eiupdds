package com.eiu.pdds.disease.entity;

import com.eiu.pdds.common.entity.BaseAuditEntity;
import com.eiu.pdds.treatment.entity.Treatment;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Disease master entity representing plant pathologies identified by computer vision or agricultural experts.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "diseases")
public class Disease extends BaseAuditEntity {

    @Column(name = "disease_code", nullable = false, unique = true)
    private String diseaseCode;

    @Column(name = "common_name", nullable = false)
    private String commonName;

    @Column(name = "scientific_name")
    private String scientificName;

    @Column(name = "symptoms", columnDefinition = "TEXT")
    private String symptoms;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    @Builder.Default
    private Severity severity = Severity.MODERATE;

    @OneToMany(mappedBy = "disease", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<PlantDisease> plantDiseases = new HashSet<>();

    @OneToMany(mappedBy = "disease", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Treatment> treatments = new ArrayList<>();
}
