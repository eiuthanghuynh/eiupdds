package com.eiu.pdds.plant.entity;

import com.eiu.pdds.common.entity.BaseAuditEntity;
import com.eiu.pdds.disease.entity.PlantDisease;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * Plant master entity representing supported agricultural crops (e.g., Rice, Tomato, Corn, Potato).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "plants")
public class Plant extends BaseAuditEntity {

    @Column(name = "plant_code", nullable = false, unique = true)
    private String plantCode;

    @Column(name = "common_name", nullable = false)
    private String commonName;

    @Column(name = "scientific_name")
    private String scientificName;

    @Column(name = "icon_url")
    private String iconUrl;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @OneToMany(mappedBy = "plant", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<PlantDisease> plantDiseases = new HashSet<>();
}
