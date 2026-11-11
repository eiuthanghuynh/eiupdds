package com.eiu.pdds.common.seeder;

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

import java.util.List;

/**
 * Idempotent master data seeder ensuring essential agricultural crop catalog,
 * plant pathologies, and treatment protocols exist across all environments
 * (including H2 in-memory test profiles).
 *
 * All domain data, scientific taxonomies, and agronomic descriptions strictly in English.
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
        log.info("Starting idempotent agricultural master data seeding...");

        // ====================================================================
        // 1. Seed Major Crops Catalog (9 Crops)
        // ====================================================================
        Plant potato = getOrCreatePlant("POTATO", "Potato", "Solanum tuberosum",
                "https://cdn.eiu.edu.vn/icons/potato.svg",
                "Major tuber crop widely cultivated in high plateaus and temperate zones.");

        Plant tomato = getOrCreatePlant("TOMATO", "Tomato", "Solanum lycopersicum",
                "https://cdn.eiu.edu.vn/icons/tomato.svg",
                "Extensively grown fruit vegetable susceptible to various fungal, bacterial, and viral diseases.");

        Plant corn = getOrCreatePlant("CORN", "Corn / Maize", "Zea mays",
                "https://cdn.eiu.edu.vn/icons/corn.svg",
                "Major cereal grain crop cultivated for human consumption and livestock feedstock.");

        Plant apple = getOrCreatePlant("APPLE", "Apple", "Malus domestica",
                "https://cdn.eiu.edu.vn/icons/apple.svg",
                "Deciduous pome fruit tree widely cultivated across temperate regions.");

        Plant bellPepper = getOrCreatePlant("BELL_PEPPER", "Bell Pepper", "Capsicum annuum",
                "https://cdn.eiu.edu.vn/icons/bell_pepper.svg",
                "Cultivar group of Capsicum annuum producing sweet, bell-shaped fruits.");

        Plant grape = getOrCreatePlant("GRAPE", "Grape", "Vitis vinifera",
                "https://cdn.eiu.edu.vn/icons/grape.svg",
                "Perennial woody deciduous vine producing fruit clusters for table consumption and winemaking.");

        Plant peach = getOrCreatePlant("PEACH", "Peach", "Prunus persica",
                "https://cdn.eiu.edu.vn/icons/peach.svg",
                "Deciduous stone fruit tree native to Northwest China, prized for juicy edible fruit.");

        Plant soybean = getOrCreatePlant("SOYBEAN", "Soybean", "Glycine max",
                "https://cdn.eiu.edu.vn/icons/soybean.svg",
                "High-protein legume crop cultivated worldwide for oil production and animal feed.");

        Plant rice = getOrCreatePlant("RICE", "Rice", "Oryza sativa",
                "https://cdn.eiu.edu.vn/icons/rice.svg",
                "Primary staple cereal grain supporting global food security, cultivated predominantly in submerged paddy ecosystems.");

        // ====================================================================
        // 2. Seed Plant Pathologies Catalog (15 Pathologies)
        // ====================================================================
        // 1. Apple Scab
        Disease appleScab = getOrCreateDisease("APPLE_SCAB", "Apple scab", "Venturia inaequalis",
                Severity.HIGH,
                "Olive-green to dull dark brown velvety spots developing on leaf blades, causing crinkling, cupping, and premature leaf drop.",
                "Severe fungal pathology attacking Malus species that causes extensive defoliation, cracked fruit blemishes, and substantial commercial loss.");

        // 2. Apple Cedar Rust
        Disease appleRust = getOrCreateDisease("APPLE_RUST", "Cedar apple rust", "Gymnosporangium juniperi-virginianae",
                Severity.MODERATE,
                "Bright yellow-orange or reddish circular lesions on the upper leaf surface that develop dark central pycnia and tubular aecia on the underside.",
                "Heteroecious rust fungus cycling between juniper gall hosts and apple trees, leading to severe leaf spot necrosis and fruit drop.");

        // 3. Bell Pepper Bacterial Leaf Spot
        Disease pepperLeafSpot = getOrCreateDisease("BELL_PEPPER_LEAF_SPOT", "Bacterial leaf spot", "Xanthomonas campestris pv. vesicatoria",
                Severity.HIGH,
                "Small water-soaked circular spots turning dark brown with pronounced chlorotic yellow haloes, causing premature canopy defoliation.",
                "Destructive bacterial pathogen causing extensive foliar blight and blistering scab-like necrotic lesions on bell pepper fruit.");

        // 4. Corn Common Rust
        Disease cornRust = getOrCreateDisease("CORN_COMMON_RUST", "Common rust", "Puccinia sorghi",
                Severity.MODERATE,
                "Small cinnamon-brown to dark reddish powdery pustules erupting scattered across both upper and lower leaf surfaces.",
                "Airborne fungal foliar disease favored by cool temperatures and high relative humidity, reducing photosynthetic green leaf area.");

        // 5. Corn Gray Leaf Spot
        Disease cornGraySpot = getOrCreateDisease("CORN_GRAY_LEAF_SPOT", "Gray leaf spot", "Cercospora zeae-maydis",
                Severity.HIGH,
                "Narrow, rectangular tan to grayish lesions strictly delimited by leaf veins running parallel to the leaf margin.",
                "Economically devastating fungal foliar disease in maize production, leading to premature leaf death and stalk lodging.");

        // 6. Corn Northern Leaf Blight
        Disease cornBlight = getOrCreateDisease("CORN_LEAF_BLIGHT", "Northern corn leaf blight", "Exserohilum turcicum",
                Severity.HIGH,
                "Long cigar-shaped elliptical grayish-green to tan necrotic lesions (2.5 to 15 cm long) extending across leaf blades.",
                "Major foliar fungal disease causing rapid canopy blighting and significant grain yield reduction under moderate, wet conditions.");

        // 7. Grape Black Rot
        Disease grapeBlackRot = getOrCreateDisease("GRAPE_BLACK_ROT", "Grape black rot", "Guignardia bidwellii",
                Severity.HIGH,
                "Circular reddish-brown necrotic spots with distinct dark borders on leaves, dotted with tiny black pycnidia; fruit shrivels into black hard mummies.",
                "Pervasive fungal disease attacking all green vineyard tissues, capable of devastating entire grape berry crops in warm humid seasons.");

        // 8. Potato Early Blight
        Disease potatoEarlyBlight = getOrCreateDisease("POTATO_EARLY_BLIGHT", "Potato early blight", "Alternaria solani",
                Severity.HIGH,
                "Small brown-black spots with characteristic concentric target-board rings appearing on older lower foliage with chlorotic yellow margins.",
                "Soilborne fungal pathogen triggering progressive premature foliar senescence, canopy collapse, and secondary dry tuber rot.");

        // 9. Potato Late Blight
        Disease potatoLateBlight = getOrCreateDisease("POTATO_LATE_BLIGHT", "Potato late blight", "Phytophthora infestans",
                Severity.CRITICAL,
                "Rapidly expanding dark brown to black water-soaked foliar lesions with white fuzzy mildew sporulation visible on undersides during humid mornings.",
                "Catastrophic oomycete pathogen historically responsible for the Irish Famine, causing rapid foliar meltdown and devastating tuber decay.");

        // 10. Tomato Early Blight
        Disease tomatoEarlyBlight = getOrCreateDisease("TOMATO_EARLY_BLIGHT", "Tomato early blight", "Alternaria solani",
                Severity.HIGH,
                "Concentric target-board ringed brown lesions starting on bottom mature leaves, progressing into chlorotic leaf blight and stem lesions.",
                "Widespread fungal disease causing severe lower leaf defoliation, exposing fruit to sunscald and weakening plant vigor.");

        // 11. Tomato Late Blight
        Disease tomatoLateBlight = getOrCreateDisease("TOMATO_LATE_BLIGHT", "Tomato late blight", "Phytophthora infestans",
                Severity.CRITICAL,
                "Pale green irregular water-soaked foliar lesions turning dark purplish-brown greasy patches with delicate white fungal sporulation underneath.",
                "Aggressive oomycete capable of destroying entire tomato greenhouse and open-field plantings within days during cool rainy periods.");

        // 12. Tomato Septoria Leaf Spot
        Disease tomatoSeptoria = getOrCreateDisease("TOMATO_SEPTORIA_LEAF_SPOT", "Tomato Septoria leaf spot", "Septoria lycopersici",
                Severity.HIGH,
                "Circular spots with dark brown margins and sunken grayish-tan centers densely dotted with tiny black reproductive pycnidia.",
                "Foliar fungal pathogen that spreads rapidly from ground level upward, causing extensive lower foliage loss and fruit sunburn.");

        // 13. Tomato Bacterial Spot
        Disease tomatoBacterialSpot = getOrCreateDisease("TOMATO_BACTERIAL_SPOT", "Tomato bacterial spot", "Xanthomonas vesicatoria",
                Severity.HIGH,
                "Small, water-soaked dark brown to black angular greasy spots often surrounded by prominent yellow haloes, causing shredded foliage.",
                "Highly contagious seedborne bacterial disease that severely degrades tomato foliage and produces raised rough scabs on fruit.");

        // 14. Tomato Yellow Leaf Curl Virus
        Disease tomatoYellowVirus = getOrCreateDisease("TOMATO_YELLOW_VIRUS", "Tomato yellow leaf curl virus", "Tomato yellow leaf curl virus (TYLCV)",
                Severity.CRITICAL,
                "Severe upward leaf cupping, pronounced interveinal chlorosis, bush-like dwarfed apical shoots, and total flower abscission.",
                "Devastating begomovirus transmitted exclusively by the sweetpotato whitefly (Bemisia tabaci), causing near-total crop failure when infected early.");

        // 15. Rice Blast
        Disease riceBlast = getOrCreateDisease("RICE_BLAST", "Rice blast", "Magnaporthe oryzae",
                Severity.CRITICAL,
                "Spindle- or diamond-shaped lesions with grayish-white necrotic centers and reddish-brown margins on leaf blades; dark rotten lesions at the panicle neck.",
                "The most destructive fungal pathology in global rice agriculture, capable of collapsing entire paddy stands and choking grain filling.");

        // ====================================================================
        // 3. Seed Plant Disease Junctions
        // ====================================================================
        // Apple
        getOrCreatePlantDisease(apple, appleScab,
                "Olive-green to dark brown velvety spots on apple leaves causing leaf puckering, distortion, and premature drop.",
                "Cool, wet spring during bud break to petal fall (10-20°C with prolonged leaf wetness)");
        getOrCreatePlantDisease(apple, appleRust,
                "Bright orange-yellow circular lesions on upper leaf surface producing tiny black dots (pycnia) and tubular rust cups underneath.",
                "Spring rainy periods with windblown basidiospores from nearby eastern red cedars or junipers");

        // Bell Pepper
        getOrCreatePlantDisease(bellPepper, pepperLeafSpot,
                "Small water-soaked circular lesions turning brown with chlorotic yellow borders, causing severe blighting and defoliation.",
                "Warm, rainy tropical and subtropical summer seasons (25-30°C with wind-driven rain or overhead sprinkler)");

        // Corn
        getOrCreatePlantDisease(corn, cornRust,
                "Small, cinnamon-brown to dark reddish powdery pustules erupting on both upper and lower corn leaves.",
                "Cool, humid conditions (16-25°C with high relative humidity > 95% and heavy dew)");
        getOrCreatePlantDisease(corn, cornGraySpot,
                "Distinct narrow rectangular tan to grayish lesions strictly delimited by corn leaf veins running parallel to margins.",
                "Warm, humid weather (25-32°C) with persistent foliar moisture and minimum tillage fields");
        getOrCreatePlantDisease(corn, cornBlight,
                "Large, elongated cigar-shaped grayish-green to tan lesions extending across leaf blades, leading to extensive canopy necrosis.",
                "Moderate temperatures (18-27°C) accompanied by extended periods of morning fog and frequent rain");

        // Grape
        getOrCreatePlantDisease(grape, grapeBlackRot,
                "Circular reddish-brown necrotic spots with dark distinct margins dotted with pepper-like black pycnidia on grapevine leaves.",
                "Warm, humid spring and early summer rains during shoot emergence and cluster flowering (21-27°C)");

        // Potato
        getOrCreatePlantDisease(potato, potatoEarlyBlight,
                "Dark brown to black lesions featuring concentric target-board ridges surrounded by chlorotic yellow haloes on older lower potato leaves.",
                "Warm, humid weather alternating with dry spells during tuber bulking (24-29°C)");
        getOrCreatePlantDisease(potato, potatoLateBlight,
                "Rapidly expanding dark brown to purplish water-soaked foliar lesions with delicate white sporulating downy mildew on leaf undersides in morning dew.",
                "Cool, foggy, wet winter-spring crop season with prolonged leaf wetness (15-22°C, humidity > 90%)");

        // Tomato
        getOrCreatePlantDisease(tomato, tomatoEarlyBlight,
                "Target-ringed dark necrotic spots beginning on bottom mature tomato foliage, progressing into severe bottom-up defoliation.",
                "Warm, rainy periods or heavy overhead irrigation alternating with sunny dry intervals (24-30°C)");
        getOrCreatePlantDisease(tomato, tomatoLateBlight,
                "Pale green to grayish greasy water-soaked lesions rapidly turning dark purplish-brown, collapsing vines and rotting green fruit within 3-5 days.",
                "Cool, overcast, wet weather with continuous drizzle, fog, or heavy morning dew (16-22°C)");
        getOrCreatePlantDisease(tomato, tomatoSeptoria,
                "Numerous small circular spots with dark brown margins and sunken grayish-white centers containing tiny black fruiting specks (pycnidia).",
                "Prolonged wet, warm weather with rain splash transferring soilborne spores to lower tomato leaves");
        getOrCreatePlantDisease(tomato, tomatoBacterialSpot,
                "Small, greasy dark brown to black circular lesions often surrounded by prominent yellow chlorotic halos, causing ragged tear-outs and blighting.",
                "Warm, rainy weather (24-30°C) with wind-driven rain and high relative humidity");
        getOrCreatePlantDisease(tomato, tomatoYellowVirus,
                "Marked upward cupping and curling of leaf margins, severe interveinal chlorosis, stunted terminal growth, and erect bushy floral abortion.",
                "Hot, dry seasons coinciding with heavy populations of sweetpotato whiteflies (Bemisia tabaci)");

        // Rice
        getOrCreatePlantDisease(rice, riceBlast,
                "Diamond-shaped or spindle-shaped lesions with grayish-white centers and reddish-brown borders on rice blades, causing leaf death and panicle neck blast.",
                "Cloudy rainy days with high humidity (> 90%), prolonged leaf dew, and excessive nitrogen fertilization (20-28°C)");

        // ====================================================================
        // 4. Seed Detailed Agronomic Treatments (2-3 per disease)
        // ====================================================================
        // 1. APPLE_SCAB
        createTreatmentIfNotExists(appleScab, apple, TreatmentType.CHEMICAL,
                "Captan / Mancozeb Protective Fungicide Spray",
                "Apply protective sprays beginning at green tip stage and repeat at 7-10 day intervals until petal fall. Ensure thorough foliar coverage on both leaf sides.",
                "Captan 50WP / Mancozeb 75DF",
                "2.0 - 2.5 kg/ha in 1000L water", 14,
                "Do not apply Captan within 14 days of an oil spray to prevent severe foliar phytotoxicity.");

        createTreatmentIfNotExists(appleScab, apple, TreatmentType.BIOLOGICAL,
                "Bacillus subtilis & Potassium Bicarbonate Protocol",
                "Apply foliar bio-fungicide during early bud swell and repeat every 7 days during humid infection windows.",
                "Bacillus subtilis strain QST 713 (min 1x10^9 CFU/g) + Potassium Bicarbonate 85%",
                "3.0 L/ha Bacillus subtilis + 3.0 kg/ha Potassium Bicarbonate", 0,
                "Apply in the early morning or evening; avoid mixing directly with concentrated copper fungicides.");

        createTreatmentIfNotExists(appleScab, apple, TreatmentType.CULTURAL,
                "Orchard Floor Sanitation and Canopy Pruning",
                "Rake, flail-mow, or shred fallen overwintered leaves in autumn. Spray 5% urea solution onto orchard floor before leaf fall to accelerate leaf decomposition and prevent ascospore discharge.",
                null,
                "Urea 5% solution (50 kg/1000L water/ha applied to leaf litter)", 0,
                "Prune trees annually to maintain open canopy structure for rapid foliage drying.");

        // 2. APPLE_RUST
        createTreatmentIfNotExists(appleRust, apple, TreatmentType.CHEMICAL,
                "Myclobutanil Systemic Triazole Fungicide",
                "Spray thoroughly from pink bud stage through early fruit development at 10-14 day intervals to protect young leaves and developing fruit.",
                "Myclobutanil 20EW (Rally equivalent)",
                "350 - 450 ml/ha in 800L water", 14,
                "Limit to a maximum of 4 applications per season to prevent pathogen resistance.");

        createTreatmentIfNotExists(appleRust, apple, TreatmentType.PREVENTIVE,
                "Juniper Host Eradication & Resistant Cultivar Selection",
                "Remove eastern red cedar and ornamental juniper bushes within a 1-2 km radius of the commercial orchard. Plant rust-immune apple cultivars like Liberty, Enterprise, or Freedom.",
                null,
                "Buffer zone separation: minimum 500m to 1km radius", 0,
                "Inspect nearby cedar trees in early spring and prune out orange gelatinous galls before spore release.");

        // 3. BELL_PEPPER_LEAF_SPOT
        createTreatmentIfNotExists(pepperLeafSpot, bellPepper, TreatmentType.CHEMICAL,
                "Fixed Copper Hydroxide + Mancozeb Synergistic Tank Mix",
                "Spray foliage at first sign of bacterial spots. The copper-mancozeb combination provides enhanced efficacy against copper-tolerant strains.",
                "Copper Hydroxide 53.8% + Mancozeb 75DF",
                "1.5 kg Copper Hydroxide + 1.5 kg Mancozeb per 1000L water", 7,
                "Avoid spraying during high midday temperatures (> 30°C) to prevent chemical leaf burn.");

        createTreatmentIfNotExists(pepperLeafSpot, bellPepper, TreatmentType.BIOLOGICAL,
                "Bacillus amyloliquefaciens Foliar Biobactericide",
                "Apply preventive bio-spray beginning 2 weeks after transplanting and continue every 7 days through harvest.",
                "Bacillus amyloliquefaciens strain D747 (min 5x10^10 viable spores/g)",
                "1.0 - 1.5 kg/ha in 400L water", 0,
                "Do not mix in tank with strong alkaline solutions or oxidizing agents.");

        createTreatmentIfNotExists(pepperLeafSpot, bellPepper, TreatmentType.CULTURAL,
                "Drip Irrigation & Seed Disinfection Protocol",
                "Soak seeds in hot water (50°C for 25 minutes) or 1.3% sodium hypochlorite for 1 minute before sowing. Switch entirely to drip irrigation to keep canopy dry.",
                "Sodium hypochlorite 1.3% (seed dip)",
                "Hot water: 50°C for 25 min; or NaOCl 1.3% for 1 min", 0,
                "Never work in pepper fields when foliage is wet from rain or dew to prevent mechanical bacterial transmission.");

        // 4. CORN_COMMON_RUST
        createTreatmentIfNotExists(cornRust, corn, TreatmentType.CHEMICAL,
                "Azoxystrobin + Propiconazole Broad-Spectrum Foliar Spray",
                "Apply when rust pustules first appear on lower canopy and environmental conditions favor spread (cool, humid). Ensure thorough coverage of both leaf sides.",
                "Azoxystrobin 13.5% + Propiconazole 11.7% (Quilt Xcel equivalent)",
                "750 - 1000 ml/ha in 200L water", 30,
                "Ensure good spray coverage of ear leaf and foliage above the ear.");

        createTreatmentIfNotExists(cornRust, corn, TreatmentType.CULTURAL,
                "Resistant Hybrid Corn Planting & Early Seeding",
                "Select commercially adapted hybrid seed containing Rp gene resistance. Plant early in the season so crop matures before airborne rust spores migrate from southern regions.",
                null,
                "Seeding rate: 70,000 - 80,000 plants/ha", 0,
                "Monitor hybrid resistance breakdown periodically as new rust races evolve.");

        // 5. CORN_GRAY_LEAF_SPOT
        createTreatmentIfNotExists(cornGraySpot, corn, TreatmentType.CHEMICAL,
                "Azoxystrobin + Difenoconazole Premix Spray",
                "Apply between V12 and early silking (R1) when lesions first appear on the lower third of the plant canopy.",
                "Azoxystrobin 200g/L + Difenoconazole 125g/L",
                "1.0 L/ha in 200L water", 30,
                "Rotate fungicide classes with different FRAC codes to prevent strobilurin resistance.");

        createTreatmentIfNotExists(cornGraySpot, corn, TreatmentType.CULTURAL,
                "Crop Rotation with Legumes and Tillage Residue Management",
                "Rotate corn with non-host crops like soybean or alfalfa for at least one full season. In heavy disease history fields, practice clean plow-down tillage to bury infested corn stubble.",
                null,
                "2-year rotation cycle with non-host legumes", 0,
                "Continuous zero-till corn severely amplifies gray leaf spot inoculum potential.");

        // 6. CORN_LEAF_BLIGHT
        createTreatmentIfNotExists(cornBlight, corn, TreatmentType.CHEMICAL,
                "Pyraclostrobin Foliar Fungicide",
                "Apply at VT-R1 growth stages when environmental forecast indicates extended wet periods and lesions appear below the ear.",
                "Pyraclostrobin 250 EC (Headline equivalent)",
                "500 ml/ha in 200L water", 21,
                "Do not exceed two applications of strobilurin fungicides per crop cycle.");

        createTreatmentIfNotExists(cornBlight, corn, TreatmentType.BIOLOGICAL,
                "Trichoderma harzianum Biofungicide Seed & Foliar Treatment",
                "Treat seeds before planting and apply foliar spray during early vegetative stages (V4-V8) to colonize root rhizosphere and leaf surfaces.",
                "Trichoderma harzianum strain T-22 (min 1x10^9 CFU/g)",
                "Seed treatment: 5g/kg seed; Foliar: 1.5 kg/ha", 0,
                "Store biofungicide below 25°C; do not apply in conjunction with systemic fungicides.");

        createTreatmentIfNotExists(cornBlight, corn, TreatmentType.PREVENTIVE,
                "Field Scouting and Balanced Potassium Nutrition",
                "Maintain balanced soil fertility (adequate potassium and avoid excessive nitrogen). Scout fields weekly starting at V8 to assess disease incidence.",
                null,
                "N:P:K balanced ratio 120:60:80 kg/ha based on soil test", 0,
                "High nitrogen application combined with potassium deficiency greatly exacerbates leaf blight susceptibility.");

        // 7. GRAPE_BLACK_ROT
        createTreatmentIfNotExists(grapeBlackRot, grape, TreatmentType.CHEMICAL,
                "Tebuconazole / Mancozeb Early Season Protective Spray",
                "Spray from 10cm shoot growth until 4-5 weeks post-bloom to protect young developing foliage and berry clusters.",
                "Tebuconazole 250 EW or Mancozeb 75DF",
                "400 ml/ha (Tebuconazole) or 2.0 kg/ha (Mancozeb)", 28,
                "Critical protection window is from bloom to pea-sized berry stage.");

        createTreatmentIfNotExists(grapeBlackRot, grape, TreatmentType.BIOLOGICAL,
                "Copper Octanoate & Sulfur Wettable Powder",
                "Apply organic protective spray from early bud burst through veraison at 7-10 day intervals.",
                "Copper Octanoate 10% + Wettable Sulfur 80%",
                "2.5 L/ha Copper Octanoate + 3.0 kg/ha Sulfur in 800L water", 3,
                "Do not apply sulfur when ambient temperature exceeds 30°C to avoid leaf and berry scorching.");

        createTreatmentIfNotExists(grapeBlackRot, grape, TreatmentType.CULTURAL,
                "Vine Canopy Training & Mummified Berry Sanitation",
                "Prune off all mummified grape clusters and infected canes during winter dormancy and burn them. Train vine canopy using vertical shoot positioning (VSP) for maximum sunlight exposure.",
                null,
                "Canopy shoot density: 12-15 shoots per linear meter of trellis", 0,
                "Never leave shriveled black rot mummies on the vine or vineyard floor over winter.");

        // 8. POTATO_EARLY_BLIGHT
        createTreatmentIfNotExists(potatoEarlyBlight, potato, TreatmentType.CHEMICAL,
                "Azoxystrobin + Difenoconazole Protective Fungicide",
                "Apply protective spray when target spots first develop on bottom leaves. Provides translaminar and systemic control.",
                "Azoxystrobin 200g/L + Difenoconazole 125g/L (Amistar Top 325SC equivalent)",
                "15-20ml per 16L sprayer (400-500 ml/ha)", 14,
                "Alternate with protectant fungicides (Chlorothalonil) to prevent strobilurin resistance.");

        createTreatmentIfNotExists(potatoEarlyBlight, potato, TreatmentType.CULTURAL,
                "Drip Irrigation & Lower Senescent Foliage Pruning",
                "Irrigate at root zone via drip lines rather than overhead sprinkler. Prune lower senescent leaves that touch the soil.",
                null,
                "Apply drip irrigation in early morning; prune bottom 15cm leaves", 0,
                "Ensure pruners are disinfected with 70% alcohol between plant rows.");

        createTreatmentIfNotExists(potatoEarlyBlight, potato, TreatmentType.BIOLOGICAL,
                "Trichoderma viride & Copper Hydroxide Foliar Shield",
                "Spray foliar bio-shield prior to tuber bulking stage. Trichoderma parasitizes Alternaria mycelium while copper provides physical spore germination barrier.",
                "Trichoderma viride (min 2x10^9 CFU/g) + Copper Hydroxide 50% WP",
                "1.5 kg Trichoderma + 1.0 kg Copper Hydroxide per ha in 500L water", 3,
                "Allow 48 hours between biofungicide and copper application if applied separately.");

        // 9. POTATO_LATE_BLIGHT
        createTreatmentIfNotExists(potatoLateBlight, potato, TreatmentType.CHEMICAL,
                "Metalaxyl + Mancozeb Systemic Fungicide Spray",
                "Spray thoroughly on both upper and lower leaf surfaces upon first appearance of symptoms. Reapply after 7-10 days if humid conditions persist.",
                "Metalaxyl 8% + Mancozeb 64% (Ridomil Gold 68WG equivalent)",
                "50g per 16L sprayer (2.5 - 3.0 kg/ha)", 7,
                "Do not apply more than 3 consecutive sprays of systemic phenylamides per season to avoid chemical resistance.");

        createTreatmentIfNotExists(potatoLateBlight, potato, TreatmentType.BIOLOGICAL,
                "Bacillus subtilis & Copper Octanoate Biopesticide",
                "Apply as preventive spray or early stage treatment. Enhances plant defense and forms protective barrier on foliage.",
                "Bacillus subtilis QST 713 (min 1x10^9 CFU/g) + Copper Octanoate 10%",
                "30-40ml per 16L water (2.0 L/ha)", 1,
                "Safe for beneficial insects; avoid spraying during intense midday sunlight.");

        createTreatmentIfNotExists(potatoLateBlight, potato, TreatmentType.PREVENTIVE,
                "High Ridge Drainage & Canopy Aeration Cultural Practice",
                "Increase bed height to 25-30cm to prevent waterlogging. Maintain 30cm spacing between plants to enhance air circulation and accelerate foliage drying.",
                null,
                "Planting spacing: 30cm x 70cm; Ridge height: 25-30cm", 0,
                "Promptly destroy and bury severely infected cull piles outside the cultivated field.");

        // 10. TOMATO_EARLY_BLIGHT
        createTreatmentIfNotExists(tomatoEarlyBlight, tomato, TreatmentType.CHEMICAL,
                "Chlorothalonil + Difenoconazole Alternating Foliar Spray",
                "Spray lower leaves at first sight of concentric target lesions. Alternate between multi-site contact and systemic triazole.",
                "Chlorothalonil 720 SC (Daconil) / Difenoconazole 250 EC",
                "2.0 L/ha (Chlorothalonil) or 300 ml/ha (Difenoconazole)", 7,
                "Ensure good bottom canopy coverage. Prune dead leaves and destroy off-field.");

        createTreatmentIfNotExists(tomatoEarlyBlight, tomato, TreatmentType.BIOLOGICAL,
                "Bacillus amyloliquefaciens Strain D747 Biofungicide",
                "Apply preventive bio-spray weekly from initial flowering stage. Competitively excludes early blight spores from germinating on foliage.",
                "Bacillus amyloliquefaciens strain D747 (min 5x10^10 viable spores/g)",
                "1.0 - 1.5 kg/ha in 400L water", 0,
                "Safe for organic production and same-day harvest (0-day pre-harvest interval).");

        createTreatmentIfNotExists(tomatoEarlyBlight, tomato, TreatmentType.CULTURAL,
                "Bottom Staking, Mulching, and Lower Sucker Pruning",
                "Stake tomato plants vertically, lay plastic or organic straw mulch to stop rain-splash spore transmission, and prune lower 30cm of suckers.",
                null,
                "Mulch layer: 5-8cm thickness; Staking height: 1.5 - 1.8m", 0,
                "Do not compost pruned infected tomato foliage in backyard compost piles.");

        // 11. TOMATO_LATE_BLIGHT
        createTreatmentIfNotExists(tomatoLateBlight, tomato, TreatmentType.CHEMICAL,
                "Dimethomorph + Mancozeb Translaminar Fungicide Spray",
                "Apply preventive or early curative spray upon regional late blight warning. Provides translaminar movement across leaf thickness.",
                "Dimethomorph 50% + Mancozeb 60% (Acrobat MZ equivalent)",
                "2.0 kg/ha in 500L water", 7,
                "Do not exceed 4 total applications per growing season; rotate with cymoxanil or mandipropamid.");

        createTreatmentIfNotExists(tomatoLateBlight, tomato, TreatmentType.BIOLOGICAL,
                "Copper Sulfate Pentahydrate & Bio-stimulant Barrier",
                "Spray copper-based organic formulation weekly during wet, cloudy periods to establish a lethal protective barrier against oomycete zoospores.",
                "Basic Copper Sulfate 53% + Seaweed extract bio-stimulant",
                "1.5 - 2.0 kg/ha Copper Sulfate in 500L water", 3,
                "Excessive continuous copper applications may lead to soil copper accumulation.");

        createTreatmentIfNotExists(tomatoLateBlight, tomato, TreatmentType.PREVENTIVE,
                "Greenhouse Humidity Reduction & Drip Irrigation",
                "Ensure greenhouse relative humidity remains below 85% through active ventilation and heating. Use drip irrigation exclusively.",
                null,
                "Ventilation: 60 air exchanges/hour; Target RH < 80%", 0,
                "Never use overhead sprinkler irrigation on tomato crops in regions with late blight history.");

        // 12. TOMATO_SEPTORIA_LEAF_SPOT
        createTreatmentIfNotExists(tomatoSeptoria, tomato, TreatmentType.CHEMICAL,
                "Chlorothalonil 500 SC Protective Foliar Spray",
                "Apply multi-site protectant spray starting at first bloom and reapply every 7-10 days throughout rainy weather.",
                "Chlorothalonil 500g/L SC (Bravo 500 equivalent)",
                "2.5 - 3.0 L/ha in 600L water", 7,
                "Do not combine with crop oils or surfactants that could increase plant absorption and cause leaf phytotoxicity.");

        createTreatmentIfNotExists(tomatoSeptoria, tomato, TreatmentType.CULTURAL,
                "Organic Straw Mulching and Weed Eradication",
                "Spread clean wheat or rice straw mulch 7-10 cm deep around plant bases to prevent soilborne fungal pycnidia splashing onto lower leaves. Keep field free of Solanaceous weed hosts (e.g. black nightshade).",
                null,
                "Straw mulch: 4-5 tonnes/ha; 3-year crop rotation without Solanaceae", 0,
                "Do not bury Septoria-infested crop debris in zero-till settings.");

        // 13. TOMATO_BACTERIAL_SPOT
        createTreatmentIfNotExists(tomatoBacterialSpot, tomato, TreatmentType.CHEMICAL,
                "Copper Octanoate / Copper Sulfate Pentahydrate Spray",
                "Apply protectant copper spray prior to rain events. Avoid overhead sprinkler irrigation.",
                "Copper Octanoate 10% or Basic Copper Sulfate 53%",
                "30-40 ml per 16L sprayer (2.0 L/ha)", 3,
                "Disinfect pruning shears between plant rows using 70% isopropyl alcohol.");

        createTreatmentIfNotExists(tomatoBacterialSpot, tomato, TreatmentType.BIOLOGICAL,
                "Bacteriophage Biocontrol Spray (AgriPhage Tomato)",
                "Apply targeted bacteriophage mixture in late afternoon or evening. Phages specifically infect and lyse Xanthomonas bacterial cells without harming beneficial flora.",
                "Host-specific Xanthomonas vesicatoria bacteriophages (min 1x10^9 PFU/ml)",
                "1.0 - 2.0 L/ha in 400L non-chlorinated water", 0,
                "Use only non-chlorinated well or reverse-osmosis water; chlorine destroys bacteriophages.");

        createTreatmentIfNotExists(tomatoBacterialSpot, tomato, TreatmentType.PREVENTIVE,
                "Certified Disease-Free Seed & 3-Year Solanaceous Crop Rotation",
                "Use hot water treated (50°C for 25 min) certified disease-free seeds. Practice a minimum 3-year rotation away from tomato, pepper, and eggplant.",
                null,
                "Seed treatment: 50°C for 25 min; Field rotation: 36 months", 0,
                "Never save seeds from bacterial spot infected tomato fruit.");

        // 14. TOMATO_YELLOW_VIRUS
        createTreatmentIfNotExists(tomatoYellowVirus, tomato, TreatmentType.BIOLOGICAL,
                "Vector Management: Yellow Sticky Traps + Beauveria bassiana",
                "Deploy yellow sticky cards (1 per 100m2) to trap sweetpotato whitefly vectors. Apply entomopathogenic fungus foliar spray.",
                "Beauveria bassiana strain GHA (min 2x10^10 viable spores/g)",
                "1.5 - 2.0 kg/ha in late afternoon", 0,
                "Controlling whitefly vector populations is the sole effective defense against TYLCV transmission.");

        createTreatmentIfNotExists(tomatoYellowVirus, tomato, TreatmentType.PREVENTIVE,
                "50-Mesh Insect Netting & Roguing of Symptomatic Plants",
                "Screen greenhouse ventilation openings with 50-mesh (0.28mm pore) insect-proof netting. Immediately rogue and seal in plastic bags any early infected plants.",
                null,
                "Netting: 50-mesh anti-insect screen; Daily roguing scouting", 0,
                "Burn or deeply bury rogued plants; never leave them exposed on field margins.");

        createTreatmentIfNotExists(tomatoYellowVirus, tomato, TreatmentType.CHEMICAL,
                "Spiromesifen / Pyriproxyfen Whitefly Insect Growth Regulator",
                "Target whitefly nymph and egg stages with insect growth regulators (IGR) to break vector reproductive cycle.",
                "Spiromesifen 240 SC or Pyriproxyfen 100 EC",
                "400 - 500 ml/ha in 500L water", 7,
                "Alternate chemical modes of action (IRAC Group 23 and 7C) to manage rapid whitefly resistance.");

        // 15. RICE_BLAST
        createTreatmentIfNotExists(riceBlast, rice, TreatmentType.CHEMICAL,
                "Tricyclazole 75% WP Systemic Blast Fungicide",
                "Spray at early tillering upon first spindle leaf lesions, and spray again at 5% panicle heading to protect against catastrophic panicle neck blast.",
                "Tricyclazole 75% WP (Beam 75WP equivalent)",
                "300 - 400 g/ha in 400L water (15-20g per 16L sprayer)", 21,
                "Apply second spray at 5-10% heading regardless of weather if field has leaf blast history.");

        createTreatmentIfNotExists(riceBlast, rice, TreatmentType.BIOLOGICAL,
                "Pseudomonas fluorescens & Bacillus subtilis Bio-formulation",
                "Soak pre-germinated paddy seeds for 12 hours before broadcasting. Apply foliar bio-spray at active tillering and boot leaf stages.",
                "Pseudomonas fluorescens strain Pf-1 (min 1x10^9 CFU/g) + Bacillus subtilis",
                "Seed soak: 10g/kg seed; Foliar: 1.0 kg/ha in 400L water", 0,
                "Maintain thin water layer in paddy during biofungicide application.");

        createTreatmentIfNotExists(riceBlast, rice, TreatmentType.CULTURAL,
                "Controlled Nitrogen Fertilization & Water Depth Management",
                "Avoid excessive or late nitrogen top-dressing. Split nitrogen into 3-4 applications with adequate potassium. Maintain continuous 5-7cm water depth during heading to moderate field microclimate.",
                null,
                "Split N: 30% basal, 40% tillering, 30% panicle initiation; K2O: 60-80 kg/ha", 0,
                "Excessive nitrogen produces lush, thin-walled leaf tissue highly susceptible to blast spore penetration.");

        log.info("Agricultural master data seeding completed successfully.");
    }

    private Plant getOrCreatePlant(String plantCode, String commonName, String scientificName, String iconUrl, String description) {
        return plantRepository.findByPlantCode(plantCode)
                .orElseGet(() -> plantRepository.save(Plant.builder()
                        .plantCode(plantCode)
                        .commonName(commonName)
                        .scientificName(scientificName)
                        .iconUrl(iconUrl)
                        .description(description)
                        .build()));
    }

    private Disease getOrCreateDisease(String diseaseCode, String commonName, String scientificName,
                                       Severity severity, String symptoms, String description) {
        return diseaseRepository.findByDiseaseCode(diseaseCode)
                .orElseGet(() -> diseaseRepository.save(Disease.builder()
                        .diseaseCode(diseaseCode)
                        .commonName(commonName)
                        .scientificName(scientificName)
                        .severity(severity)
                        .symptoms(symptoms)
                        .description(description)
                        .build()));
    }

    private PlantDisease getOrCreatePlantDisease(Plant plant, Disease disease, String specificSymptoms, String riskSeason) {
        return plantDiseaseRepository.findByPlantIdAndDiseaseId(plant.getId(), disease.getId())
                .orElseGet(() -> plantDiseaseRepository.save(PlantDisease.builder()
                        .plant(plant)
                        .disease(disease)
                        .specificSymptoms(specificSymptoms)
                        .riskSeason(riskSeason)
                        .build()));
    }

    private Treatment createTreatmentIfNotExists(Disease disease, Plant plant, TreatmentType type, String title,
                                                String instructions, String activeIngredients,
                                                String dosage, Integer safetyIntervalDays, String warning) {
        List<Treatment> existing = treatmentRepository.findByDiseaseIdAndTreatmentTypeAndDeletedAtIsNull(disease.getId(), type);
        for (Treatment t : existing) {
            if (t.getTitle().equalsIgnoreCase(title)) {
                return t;
            }
        }
        return treatmentRepository.save(Treatment.builder()
                .disease(disease)
                .plant(plant)
                .treatmentType(type)
                .title(title)
                .instructions(instructions)
                .activeIngredients(activeIngredients)
                .dosageRecommendation(dosage)
                .safetyIntervalDays(safetyIntervalDays)
                .warning(warning)
                .build());
    }
}
