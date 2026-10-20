-- Flyway Migration: V2__seed_master_data.sql
-- Project: EIU Plant Disease Detection System (EIU PDDS)
-- Description: Core baseline master data for primary crops, pathologies, and agricultural treatments (in English).

-- ============================================================================
-- 1. Supported Crop Catalog (Core Plants)
-- ============================================================================
INSERT INTO plants (id, plant_code, common_name, scientific_name, icon_url, description)
VALUES 
    ('11111111-1111-1111-1111-111111111101', 'POTATO', 'Potato', 'Solanum tuberosum', 'https://cdn.eiu.edu.vn/icons/potato.svg', 'Major tuber crop widely cultivated in high plateaus and temperate zones.'),
    ('11111111-1111-1111-1111-111111111102', 'TOMATO', 'Tomato', 'Solanum lycopersicum', 'https://cdn.eiu.edu.vn/icons/tomato.svg', 'Extensively grown fruit vegetable susceptible to various fungal, bacterial, and viral blights.'),
    ('11111111-1111-1111-1111-111111111104', 'CORN', 'Corn / Maize', 'Zea mays', 'https://cdn.eiu.edu.vn/icons/corn.svg', 'Major cereal grain crop cultivated for food and animal feedstock.')
ON CONFLICT (plant_code) DO NOTHING;

-- ============================================================================
-- 2. Disease Catalog (Core Pathologies)
-- ============================================================================
INSERT INTO diseases (id, disease_code, common_name, scientific_name, symptoms, description, severity)
VALUES
    ('22222222-2222-2222-2222-222222222201', 'POTATO_LATE_BLIGHT', 'Potato late blight', 'Phytophthora infestans', 'Dark brown to black water-soaked lesions on leaves with white fungal growth on undersides under humid conditions.', 'Destructive fungal-like oomycete disease causing rapid defoliation and severe tuber rot.', 'CRITICAL'),
    ('22222222-2222-2222-2222-222222222202', 'POTATO_EARLY_BLIGHT', 'Potato early blight', 'Alternaria solani', 'Small brown-black spots with characteristic target-like concentric rings on older lower foliage.', 'Common fungal disease leading to premature leaf aging, canopy collapse, and reduced yield.', 'HIGH'),
    ('22222222-2222-2222-2222-222222222203', 'TOMATO_LATE_BLIGHT', 'Tomato late blight', 'Phytophthora infestans', 'Pale green irregular lesions rapidly enlarging into dark purplish-brown greasy patches with white sporulation.', 'Devastating disease causing complete plant rot within days under cool wet weather.', 'CRITICAL'),
    ('22222222-2222-2222-2222-222222222204', 'TOMATO_SEPTORIA_LEAF_SPOT', 'Tomato Septoria leaf spot', 'Septoria lycopersici', 'Circular spots with dark brown margins and sunken grayish-tan centers dotted with tiny black pycnidia.', 'Foliar fungal pathology that progressively spreads from bottom foliage upward.', 'MODERATE')
ON CONFLICT (disease_code) DO NOTHING;

-- ============================================================================
-- 3. Plant Disease Junctions (Many-to-Many Bridge)
-- ============================================================================
INSERT INTO plant_diseases (id, plant_id, disease_id, specific_symptoms, risk_season)
VALUES
    ('33333333-3333-3333-3333-333333333301', '11111111-1111-1111-1111-111111111101', '22222222-2222-2222-2222-222222222201', 'Leaves turn dark brown; white mildew visible on leaf undersides in morning dew.', 'Cool, foggy, wet winter-spring crop season (15-22°C, humidity > 90%)'),
    ('33333333-3333-3333-3333-333333333302', '11111111-1111-1111-1111-111111111101', '22222222-2222-2222-2222-222222222202', 'Target-board concentric lesions on lower mature potato leaves.', 'Warm, humid weather alternating with dry spells (24-29°C)'),
    ('33333333-3333-3333-3333-333333333303', '11111111-1111-1111-1111-111111111102', '22222222-2222-2222-2222-222222222203', 'Greasy water-soaked lesions on tomato leaves, stems, and immature green fruit.', 'Prolonged rainfall or heavy fog during fruit development'),
    ('33333333-3333-3333-3333-333333333304', '11111111-1111-1111-1111-111111111102', '22222222-2222-2222-2222-222222222204', 'Small speckled leaf spots causing yellowing and early leaf drop.', 'Warm wet seasons with frequent overhead splash')
ON CONFLICT (plant_id, disease_id) DO NOTHING;

-- ============================================================================
-- 4. Agricultural Treatments & Mitigation Protocols
-- ============================================================================
-- Potato Late Blight Treatments
INSERT INTO treatments (id, disease_id, plant_id, treatment_type, title, instructions, active_ingredients, dosage_recommendation, safety_interval_days, warning)
VALUES
    ('44444444-4444-4444-4444-444444444401', '22222222-2222-2222-2222-222222222201', '11111111-1111-1111-1111-111111111101', 'CHEMICAL', 
     'Metalaxyl + Mancozeb Systemic Fungicide Spray', 
     'Spray thoroughly on both upper and lower leaf surfaces upon first appearance of symptoms. Reapply after 7-10 days if humid conditions persist.', 
     'Metalaxyl 8% + Mancozeb 64% (Ridomil Gold 68WG equivalent)', 
     '50g per 16L sprayer (2.5 - 3.0 kg/ha)', 
     7, 
     'Do not apply more than 3 consecutive sprays of systemic phenylamides per season to avoid chemical resistance.'),
    
    ('44444444-4444-4444-4444-444444444402', '22222222-2222-2222-2222-222222222201', '11111111-1111-1111-1111-111111111101', 'BIOLOGICAL', 
     'Bacillus subtilis & Copper Octanoate Biopesticide', 
     'Apply as preventive spray or early stage treatment. Enhances plant defense and forms protective barrier on foliage.', 
     'Bacillus subtilis QST 713 (min 1x10^9 CFU/g) + Copper Octanoate 10%', 
     '30-40ml per 16L water', 
     1, 
     'Safe for beneficial insects; avoid spraying during intense midday sunlight.'),
    
    ('44444444-4444-4444-4444-444444444403', '22222222-2222-2222-2222-222222222201', '11111111-1111-1111-1111-111111111101', 'PREVENTIVE', 
     'Drainage & Canopy Aeration Cultural Practice', 
     'Increase bed height to 25-30cm to prevent waterlogging. Maintain 30cm spacing between plants to enhance air circulation and accelerate foliage drying.', 
     NULL, 
     'Planting spacing: 30cm x 70cm; Ridge height: 25-30cm', 
     0, 
     'Promptly destroy and bury severely infected cull piles outside the cultivated field.');

-- Potato Early Blight Treatments
INSERT INTO treatments (id, disease_id, plant_id, treatment_type, title, instructions, active_ingredients, dosage_recommendation, safety_interval_days, warning)
VALUES
    ('44444444-4444-4444-4444-444444444404', '22222222-2222-2222-2222-222222222202', '11111111-1111-1111-1111-111111111101', 'CHEMICAL', 
     'Azoxystrobin + Difenoconazole Protective Fungicide', 
     'Apply protective spray when target spots first develop on bottom leaves. Provides translaminar and systemic control.', 
     'Azoxystrobin 200g/L + Difenoconazole 125g/L (Amistar Top 325SC equivalent)', 
     '15-20ml per 16L sprayer', 
     14, 
     'Alternate with protectant fungicides (Chlorothalonil) to prevent strobilurin resistance.'),
    
    ('44444444-4444-4444-4444-444444444405', '22222222-2222-2222-2222-222222222202', '11111111-1111-1111-1111-111111111101', 'CULTURAL', 
     'Drip Irrigation & Lower Foliage Pruning', 
     'Irrigate at root zone via drip lines rather than overhead sprinkler. Prune lower senescent leaves that touch the soil.', 
     NULL, 
     'Apply drip irrigation in early morning', 
     0, 
     'Ensure pruners are disinfected with 70% alcohol between plant rows.');
