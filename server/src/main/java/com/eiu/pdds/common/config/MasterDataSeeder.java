package com.eiu.pdds.common.config;

import com.eiu.pdds.disease.entity.Disease;
import com.eiu.pdds.disease.entity.PlantDisease;
import com.eiu.pdds.disease.entity.Severity;
import com.eiu.pdds.disease.repository.DiseaseRepository;
import com.eiu.pdds.disease.repository.PlantDiseaseRepository;
import com.eiu.pdds.plant.entity.Plant;
import com.eiu.pdds.plant.repository.PlantRepository;
import com.eiu.pdds.treatment.entity.Treatment;
import com.eiu.pdds.treatment.entity.TreatmentType;
import com.eiu.pdds.treatment.repository.TreatmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Idempotent master data seeder ensuring essential agricultural crop catalog,
 * pathologies, and treatment protocols exist across all environments (including H2 test profile).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MasterDataSeeder implements CommandLineRunner {

    private final PlantRepository plantRepository;
    private final DiseaseRepository diseaseRepository;
    private final PlantDiseaseRepository plantDiseaseRepository;
    private final TreatmentRepository treatmentRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (plantRepository.count() > 0) {
            log.info("Master data already initialized ({} plants found). Skipping seeder.", plantRepository.count());
            return;
        }

        log.info("Seeding initial agricultural master data into database...");

        // 1. Seed Plants
        Plant potato = plantRepository.save(Plant.builder()
                .plantCode("POTATO")
                .commonName("Potato")
                .scientificName("Solanum tuberosum")
                .iconUrl("https://cdn.eiu.edu.vn/icons/potato.svg")
                .description("Major tuber crop widely cultivated in high plateaus and temperate zones.")
                .build());

        Plant tomato = plantRepository.save(Plant.builder()
                .plantCode("TOMATO")
                .commonName("Tomato")
                .scientificName("Solanum lycopersicum")
                .iconUrl("https://cdn.eiu.edu.vn/icons/tomato.svg")
                .description("Extensively grown fruit vegetable susceptible to various fungal and bacterial blights.")
                .build());

        Plant corn = plantRepository.save(Plant.builder()
                .plantCode("CORN")
                .commonName("Corn / Maize")
                .scientificName("Zea mays")
                .iconUrl("https://cdn.eiu.edu.vn/icons/corn.svg")
                .description("Major cereal grain crop cultivated for food and animal feedstock.")
                .build());

        // 2. Seed Diseases
        Disease lateBlight = diseaseRepository.save(Disease.builder()
                .diseaseCode("POTATO_LATE_BLIGHT")
                .commonName("Potato late blight")
                .scientificName("Phytophthora infestans")
                .severity(Severity.CRITICAL)
                .symptoms("Dark brown to black water-soaked lesions on leaves with white fungal growth on undersides under humid conditions.")
                .description("Destructive fungal-like oomycete disease causing rapid defoliation and severe tuber rot.")
                .build());

        Disease earlyBlight = diseaseRepository.save(Disease.builder()
                .diseaseCode("POTATO_EARLY_BLIGHT")
                .commonName("Potato early blight")
                .scientificName("Alternaria solani")
                .severity(Severity.HIGH)
                .symptoms("Small brown-black spots with characteristic target-like concentric rings on older lower foliage.")
                .description("Common fungal disease leading to premature leaf aging, canopy collapse, and reduced yield.")
                .build());

        Disease tomatoLateBlight = diseaseRepository.save(Disease.builder()
                .diseaseCode("TOMATO_LATE_BLIGHT")
                .commonName("Tomato late blight")
                .scientificName("Phytophthora infestans")
                .severity(Severity.CRITICAL)
                .symptoms("Pale green irregular lesions rapidly enlarging into dark purplish-brown greasy patches with white sporulation.")
                .description("Devastating disease causing complete plant rot within days under cool wet weather.")
                .build());

        // 3. Plant Disease Junctions
        plantDiseaseRepository.save(PlantDisease.builder()
                .plant(potato)
                .disease(lateBlight)
                .specificSymptoms("Leaves turn dark brown; white mildew visible on leaf undersides in morning dew.")
                .riskSeason("Cool, foggy, wet winter-spring crop season (15-22°C, humidity > 90%)")
                .build());

        plantDiseaseRepository.save(PlantDisease.builder()
                .plant(potato)
                .disease(earlyBlight)
                .specificSymptoms("Target-board concentric lesions on lower mature potato leaves.")
                .riskSeason("Warm, humid weather alternating with dry spells (24-29°C)")
                .build());

        plantDiseaseRepository.save(PlantDisease.builder()
                .plant(tomato)
                .disease(tomatoLateBlight)
                .specificSymptoms("Greasy water-soaked lesions on tomato leaves, stems, and fruit.")
                .riskSeason("Prolonged rainfall or heavy fog during fruit development")
                .build());

        // 4. Treatments
        treatmentRepository.save(Treatment.builder()
                .disease(lateBlight)
                .plant(potato)
                .treatmentType(TreatmentType.CHEMICAL)
                .title("Metalaxyl + Mancozeb Systemic Fungicide Spray")
                .instructions("Spray thoroughly on both upper and lower leaf surfaces upon first appearance of symptoms. Reapply after 7-10 days if humid conditions persist.")
                .activeIngredients("Metalaxyl 8% + Mancozeb 64% (Ridomil Gold 68WG equivalent)")
                .dosageRecommendation("50g per 16L sprayer (2.5 - 3.0 kg/ha)")
                .safetyIntervalDays(7)
                .warning("Do not apply more than 3 consecutive sprays of systemic phenylamides per season to avoid chemical resistance.")
                .build());

        treatmentRepository.save(Treatment.builder()
                .disease(lateBlight)
                .plant(potato)
                .treatmentType(TreatmentType.BIOLOGICAL)
                .title("Bacillus subtilis & Copper Octanoate Biopesticide")
                .instructions("Apply as preventive spray or early stage treatment. Enhances plant defense and forms protective barrier on foliage.")
                .activeIngredients("Bacillus subtilis QST 713 (min 1x10^9 CFU/g) + Copper Octanoate 10%")
                .dosageRecommendation("30-40ml per 16L water")
                .safetyIntervalDays(1)
                .warning("Safe for beneficial insects; avoid spraying during intense midday sunlight.")
                .build());

        treatmentRepository.save(Treatment.builder()
                .disease(lateBlight)
                .plant(potato)
                .treatmentType(TreatmentType.PREVENTIVE)
                .title("Drainage & Canopy Aeration Cultural Practice")
                .instructions("Increase bed height to 25-30cm to prevent waterlogging. Maintain 30cm spacing between plants to enhance air circulation and accelerate foliage drying.")
                .activeIngredients(null)
                .dosageRecommendation("Planting spacing: 30cm x 70cm; Ridge height: 25-30cm")
                .safetyIntervalDays(0)
                .warning("Promptly destroy and bury severely infected cull piles outside the cultivated field.")
                .build());

        treatmentRepository.save(Treatment.builder()
                .disease(earlyBlight)
                .plant(potato)
                .treatmentType(TreatmentType.CHEMICAL)
                .title("Azoxystrobin + Difenoconazole Protective Fungicide")
                .instructions("Apply protective spray when target spots first develop on bottom leaves. Provides translaminar and systemic control.")
                .activeIngredients("Azoxystrobin 200g/L + Difenoconazole 125g/L (Amistar Top 325SC equivalent)")
                .dosageRecommendation("15-20ml per 16L sprayer")
                .safetyIntervalDays(14)
                .warning("Alternate with protectant fungicides (Chlorothalonil) to prevent strobilurin resistance.")
                .build());

        log.info("Master data successfully seeded into database.");
    }
}
