-- Flyway Migration: V3__seed_roboflow_30_classes.sql
-- Project: EIU Plant Disease Detection System (EIU PDDS)
-- Description: Complete 30-class agricultural catalog inferred directly from Roboflow model (college-whx3b/plant-leaf-diseases/10).
-- Contains exactly 13 plant species corresponding to the 30 classes. All names in English.

-- ============================================================================
-- 1. Supported Crop Catalog (13 Plants inferred from 30 Roboflow Classes)
-- ============================================================================
INSERT INTO plants (id, plant_code, common_name, scientific_name, icon_url, description)
VALUES 
    ('11111111-1111-1111-1111-111111111101', 'POTATO', 'Potato', 'Solanum tuberosum', 'https://cdn.eiu.edu.vn/icons/potato.svg', 'Major tuber crop widely cultivated in high plateaus and temperate zones.'),
    ('11111111-1111-1111-1111-111111111102', 'TOMATO', 'Tomato', 'Solanum lycopersicum', 'https://cdn.eiu.edu.vn/icons/tomato.svg', 'Extensively grown fruit vegetable susceptible to various fungal, bacterial, and viral diseases.'),
    ('11111111-1111-1111-1111-111111111104', 'CORN', 'Corn / Maize', 'Zea mays', 'https://cdn.eiu.edu.vn/icons/corn.svg', 'Major cereal grain crop cultivated for human consumption and livestock feedstock.'),
    ('11111111-1111-1111-1111-111111111105', 'APPLE', 'Apple', 'Malus domestica', 'https://cdn.eiu.edu.vn/icons/apple.svg', 'Deciduous pome fruit tree widely cultivated across temperate regions.'),
    ('11111111-1111-1111-1111-111111111106', 'BELL_PEPPER', 'Bell Pepper', 'Capsicum annuum', 'https://cdn.eiu.edu.vn/icons/bell_pepper.svg', 'Cultivar group of the species Capsicum annuum cultivated for mild sweet peppers.'),
    ('11111111-1111-1111-1111-111111111107', 'BLUEBERRY', 'Blueberry', 'Vaccinium corymbosum', 'https://cdn.eiu.edu.vn/icons/blueberry.svg', 'Perennial flowering plant with blue- or purple-colored berries.'),
    ('11111111-1111-1111-1111-111111111108', 'CHERRY', 'Cherry', 'Prunus avium', 'https://cdn.eiu.edu.vn/icons/cherry.svg', 'Stone fruit tree cultivated for sweet cherries.'),
    ('11111111-1111-1111-1111-111111111109', 'GRAPE', 'Grape', 'Vitis vinifera', 'https://cdn.eiu.edu.vn/icons/grape.svg', 'Perennial woody deciduous vine producing fruit clusters for table fruit and winemaking.'),
    ('11111111-1111-1111-1111-111111111110', 'PEACH', 'Peach', 'Prunus persica', 'https://cdn.eiu.edu.vn/icons/peach.svg', 'Deciduous stone fruit tree native to Northwest China.'),
    ('11111111-1111-1111-1111-111111111111', 'RASPBERRY', 'Raspberry', 'Rubus idaeus', 'https://cdn.eiu.edu.vn/icons/raspberry.svg', 'Edible fruit of a multitude of plant species in the genus Rubus of the rose family.'),
    ('11111111-1111-1111-1111-111111111112', 'SOYBEAN', 'Soybean', 'Glycine max', 'https://cdn.eiu.edu.vn/icons/soybean.svg', 'Species of legume native to East Asia widely grown for edible bean and protein meal.'),
    ('11111111-1111-1111-1111-111111111113', 'SQUASH', 'Squash', 'Cucurbita pepo', 'https://cdn.eiu.edu.vn/icons/squash.svg', 'Herbaceous vine species belonging to the Cucurbitaceae family cultivated for marrow/squash.'),
    ('11111111-1111-1111-1111-111111111114', 'STRAWBERRY', 'Strawberry', 'Fragaria ananassa', 'https://cdn.eiu.edu.vn/icons/strawberry.svg', 'Widely grown hybrid species of the genus Fragaria cultivated for sweet red berries.')
ON CONFLICT (plant_code) DO UPDATE 
SET common_name = EXCLUDED.common_name,
    description = EXCLUDED.description;

-- ============================================================================
-- 2. Disease Catalog (30 Roboflow Classes — Strictly English)
-- ============================================================================
INSERT INTO diseases (id, disease_code, common_name, scientific_name, symptoms, description, severity)
VALUES
    -- Apple Classes (3)
    ('22222222-2222-2222-2222-222222222211', 'APPLE_HEALTHY', 'Healthy apple leaf', 'Malus domestica', 'Smooth green leaf blade without spots, discoloration, or deformities.', 'Healthy uninfected apple foliage.', 'LOW'),
    ('22222222-2222-2222-2222-222222222212', 'APPLE_SCAB', 'Apple scab', 'Venturia inaequalis', 'Olive-green to dark brown velvety spots developing on leaf blades, causing crinkling and leaf drop.', 'Fungal disease of Malus trees causing severe defoliation and fruit blemishes.', 'HIGH'),
    ('22222222-2222-2222-2222-222222222213', 'APPLE_RUST', 'Cedar apple rust', 'Gymnosporangium juniperi-virginianae', 'Bright yellow-orange or reddish circular spots on upper leaf surface producing tiny dark pustules.', 'Heteroecious rust fungus cycling between juniper and apple trees.', 'MODERATE'),
    
    -- Bell Pepper Classes (2)
    ('22222222-2222-2222-2222-222222222214', 'BELL_PEPPER_HEALTHY', 'Healthy bell pepper leaf', 'Capsicum annuum', 'Vibrant green uniform foliar canopy with healthy turgor.', 'Healthy bell pepper foliage.', 'LOW'),
    ('22222222-2222-2222-2222-222222222215', 'BELL_PEPPER_LEAF_SPOT', 'Bell pepper bacterial leaf spot', 'Xanthomonas campestris pv. vesicatoria', 'Water-soaked small circular spots turning brown with distinct yellow chlorotic halos.', 'Destructive bacterial disease causing extensive premature leaf dropping.', 'HIGH'),

    -- Blueberry, Cherry, Peach, Raspberry, Strawberry, Soybean Healthy Classes (6)
    ('22222222-2222-2222-2222-222222222216', 'BLUEBERRY_HEALTHY', 'Healthy blueberry leaf', 'Vaccinium corymbosum', 'Glossy green ovate leaves free of fungal blight or rust.', 'Healthy blueberry foliage.', 'LOW'),
    ('22222222-2222-2222-2222-222222222217', 'CHERRY_HEALTHY', 'Healthy cherry leaf', 'Prunus avium', 'Healthy dark green elliptical leaves with serrate margins.', 'Healthy cherry foliage.', 'LOW'),
    ('22222222-2222-2222-2222-222222222218', 'PEACH_HEALTHY', 'Healthy peach leaf', 'Prunus persica', 'Lanceolate green leaves without blistering or curl.', 'Healthy peach foliage.', 'LOW'),
    ('22222222-2222-2222-2222-222222222219', 'RASPBERRY_HEALTHY', 'Healthy raspberry leaf', 'Rubus idaeus', 'Compound green leaves with whitish undersides and healthy vigor.', 'Healthy raspberry foliage.', 'LOW'),
    ('22222222-2222-2222-2222-222222222220', 'SOYBEAN_HEALTHY', 'Healthy soybean leaf', 'Glycine max', 'Trifoliolate green foliage free of bacterial pustules or rust.', 'Healthy soybean foliage.', 'LOW'),
    ('22222222-2222-2222-2222-222222222221', 'STRAWBERRY_HEALTHY', 'Healthy strawberry leaf', 'Fragaria ananassa', 'Vigorous trifoliolate serrated leaves with rich green color.', 'Healthy strawberry foliage.', 'LOW'),

    -- Corn Classes (3)
    ('22222222-2222-2222-2222-222222222222', 'CORN_GRAY_LEAF_SPOT', 'Corn gray leaf spot', 'Cercospora zeae-maydis', 'Narrow rectangular tan to grayish lesions delimited by leaf veins running parallel to the margin.', 'Major fungal foliar disease of maize causing significant yield reduction.', 'HIGH'),
    ('22222222-2222-2222-2222-222222222223', 'CORN_LEAF_BLIGHT', 'Corn northern leaf blight', 'Exserohilum turcicum', 'Long cigar-shaped elliptical grayish-green to tan lesions extending across leaf blades.', 'Severe foliar blight causing premature canopy death.', 'HIGH'),
    ('22222222-2222-2222-2222-222222222224', 'CORN_COMMON_RUST', 'Corn common rust', 'Puccinia sorghi', 'Small cinnamon-brown to dark red powdery pustules scattered across both leaf surfaces.', 'Fungal rust pathology prevalent under cool, high-humidity growing conditions.', 'MODERATE'),

    -- Grape Classes (2)
    ('22222222-2222-2222-2222-222222222225', 'GRAPE_HEALTHY', 'Healthy grape leaf', 'Vitis vinifera', 'Healthy palmately lobed green vine foliage.', 'Healthy grape foliage.', 'LOW'),
    ('22222222-2222-2222-2222-222222222226', 'GRAPE_BLACK_ROT', 'Grape black rot', 'Phyllosticta ampelicida', 'Reddish-brown circular to oval lesions with dark margins dotted with minute black pycnidia.', 'Destructive fungal disease affecting grape foliage and berry clusters.', 'HIGH'),

    -- Potato Classes (3)
    ('22222222-2222-2222-2222-222222222227', 'POTATO_HEALTHY', 'Healthy potato leaf', 'Solanum tuberosum', 'Uniformly green compound foliage without blight lesions or chlorosis.', 'Healthy potato foliage.', 'LOW'),
    ('22222222-2222-2222-2222-222222222201', 'POTATO_LATE_BLIGHT', 'Potato late blight', 'Phytophthora infestans', 'Dark brown to black water-soaked lesions on leaves with white fuzzy fungal growth on undersides under humid conditions.', 'Destructive oomycete pathogen responsible for rapid foliar collapse and tuber rot.', 'CRITICAL'),
    ('22222222-2222-2222-2222-222222222202', 'POTATO_EARLY_BLIGHT', 'Potato early blight', 'Alternaria solani', 'Small brown-black spots with characteristic target-like concentric rings on older lower foliage.', 'Fungal disease causing premature foliar senescence and canopy collapse.', 'HIGH'),

    -- Squash Classes (1)
    ('22222222-2222-2222-2222-222222222228', 'SQUASH_POWDERY_MILDEW', 'Squash powdery mildew', 'Podosphaera xanthii', 'White powdery talcum-like fungal patches spreading over leaf blades, petioles, and stems.', 'Common fungal disease causing premature leaf yellowing and reduced fruit quality.', 'HIGH'),

    -- Tomato Classes (9)
    ('22222222-2222-2222-2222-222222222229', 'TOMATO_HEALTHY', 'Healthy tomato leaf', 'Solanum lycopersicum', 'Deep green healthy pinnate foliage without lesions or chlorotic curling.', 'Healthy tomato foliage.', 'LOW'),
    ('22222222-2222-2222-2222-222222222203', 'TOMATO_LATE_BLIGHT', 'Tomato late blight', 'Phytophthora infestans', 'Pale green irregular water-soaked foliar lesions rapidly turning into dark purplish-brown greasy patches with white sporulation.', 'Devastating disease causing total canopy and fruit destruction within days.', 'CRITICAL'),
    ('22222222-2222-2222-2222-222222222230', 'TOMATO_EARLY_BLIGHT', 'Tomato early blight', 'Alternaria solani', 'Concentric target-board ringed dark lesions appearing on lower leaves, surrounded by yellow chlorosis.', 'Major foliar fungal pathogen causing extensive defoliation from ground level upward.', 'HIGH'),
    ('22222222-2222-2222-2222-222222222231', 'TOMATO_BACTERIAL_SPOT', 'Tomato bacterial spot', 'Xanthomonas perforans', 'Small dark brown to black greasy spots with yellow halos, sometimes with necrotic leaf tear-outs.', 'Severe seedborne bacterial pathology impacting both foliar canopy and marketability.', 'HIGH'),
    ('22222222-2222-2222-2222-222222222232', 'TOMATO_MOSAIC_VIRUS', 'Tomato mosaic virus', 'Tomato mosaic virus (ToMV)', 'Mottled foliage with alternating light and dark green patches, leaf curling, blistering, and stringy fern-like leaves.', 'Highly contagious stable virus spread mechanically by contact and contaminated equipment.', 'CRITICAL'),
    ('22222222-2222-2222-2222-222222222233', 'TOMATO_YELLOW_VIRUS', 'Tomato yellow leaf curl virus', 'Tomato yellow leaf curl virus (TYLCV)', 'Severe upward leaf curling, marked interveinal yellowing, stunted growth, and bushy apical shoot architecture.', 'Geminivirus transmitted exclusively by the sweetpotato whitefly (Bemisia tabaci).', 'CRITICAL'),
    ('22222222-2222-2222-2222-222222222234', 'TOMATO_LEAF_MOLD', 'Tomato leaf mold', 'Passalora fulva', 'Pale green to yellowish chlorotic spots on upper leaf surface with dense olive-green velvety mold on undersides.', 'Fungal disease common in high-humidity greenhouse environments.', 'MODERATE'),
    ('22222222-2222-2222-2222-222222222204', 'TOMATO_SEPTORIA_LEAF_SPOT', 'Tomato Septoria leaf spot', 'Septoria lycopersici', 'Circular spots with dark brown margins and sunken grayish-tan centers dotted with tiny black pycnidia.', 'Foliar fungal pathology that progressively defoliates bottom foliage upward.', 'HIGH'),
    ('22222222-2222-2222-2222-222222222235', 'TOMATO_SPIDER_MITES', 'Tomato two-spotted spider mites', 'Tetranychus urticae', 'Dense pale stippling (yellow speckling) on upper leaf surface, followed by yellowing, bronzing, and fine silken webbing.', 'Pest infestation causing rapid leaf desiccation and chlorotic foliar drop.', 'HIGH')
ON CONFLICT (disease_code) DO UPDATE 
SET common_name = EXCLUDED.common_name,
    description = EXCLUDED.description,
    symptoms = EXCLUDED.symptoms,
    severity = EXCLUDED.severity;

-- ============================================================================
-- 3. Plant Disease Junctions (Bridging all 13 Plants & 30 Classes)
-- ============================================================================
INSERT INTO plant_diseases (id, plant_id, disease_id, specific_symptoms, risk_season)
VALUES
    -- Apple (3)
    ('33333333-3333-3333-3333-333333333310', '11111111-1111-1111-1111-111111111105', '22222222-2222-2222-2222-222222222211', 'Healthy foliage with no disease symptoms.', 'Growing season'),
    ('33333333-3333-3333-3333-333333333311', '11111111-1111-1111-1111-111111111105', '22222222-2222-2222-2222-222222222212', 'Olive-green spots on apple leaves causing distortion.', 'Cool wet spring during bud break to petal fall'),
    ('33333333-3333-3333-3333-333333333312', '11111111-1111-1111-1111-111111111105', '22222222-2222-2222-2222-222222222213', 'Yellowish-orange spots with dark dots on apple leaves.', 'Spring rainfalls with wind dispersal from cedars'),
    
    -- Bell Pepper (2)
    ('33333333-3333-3333-3333-333333333309', '11111111-1111-1111-1111-111111111106', '22222222-2222-2222-2222-222222222214', 'Healthy foliage with no bacterial spots.', 'Growing season'),
    ('33333333-3333-3333-3333-333333333313', '11111111-1111-1111-1111-111111111106', '22222222-2222-2222-2222-222222222215', 'Water-soaked spots turning necrotic on bell pepper foliage.', 'Warm, rainy tropical/subtropical summer'),
    
    -- Blueberry, Cherry, Peach, Raspberry, Soybean, Strawberry (6)
    ('33333333-3333-3333-3333-333333333325', '11111111-1111-1111-1111-111111111107', '22222222-2222-2222-2222-222222222216', 'Healthy blueberry foliage.', 'Growing season'),
    ('33333333-3333-3333-3333-333333333326', '11111111-1111-1111-1111-111111111108', '22222222-2222-2222-2222-222222222217', 'Healthy cherry foliage.', 'Growing season'),
    ('33333333-3333-3333-3333-333333333327', '11111111-1111-1111-1111-111111111110', '22222222-2222-2222-2222-222222222218', 'Healthy peach foliage.', 'Growing season'),
    ('33333333-3333-3333-3333-333333333328', '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222219', 'Healthy raspberry foliage.', 'Growing season'),
    ('33333333-3333-3333-3333-333333333329', '11111111-1111-1111-1111-111111111112', '22222222-2222-2222-2222-222222222220', 'Healthy soybean foliage.', 'Growing season'),
    ('33333333-3333-3333-3333-333333333330', '11111111-1111-1111-1111-111111111114', '22222222-2222-2222-2222-222222222221', 'Healthy strawberry foliage.', 'Growing season'),

    -- Corn (3)
    ('33333333-3333-3333-3333-333333333314', '11111111-1111-1111-1111-111111111104', '22222222-2222-2222-2222-222222222222', 'Rectangular vein-limited gray spots on corn leaves.', 'Warm humid seasons with persistent leaf wetness'),
    ('33333333-3333-3333-3333-333333333315', '11111111-1111-1111-1111-111111111104', '22222222-2222-2222-2222-222222222223', 'Cigar-shaped grayish lesions on corn leaves.', 'Moderate temperatures with heavy dew periods'),
    ('33333333-3333-3333-3333-333333333316', '11111111-1111-1111-1111-111111111104', '22222222-2222-2222-2222-222222222224', 'Powdery brown pustules on upper and lower corn leaves.', 'Cool humid conditions (16-25°C)'),

    -- Grape (2)
    ('33333333-3333-3333-3333-333333333308', '11111111-1111-1111-1111-111111111109', '22222222-2222-2222-2222-222222222225', 'Healthy grape vine foliage.', 'Growing season'),
    ('33333333-3333-3333-3333-333333333317', '11111111-1111-1111-1111-111111111109', '22222222-2222-2222-2222-222222222226', 'Circular brown lesions with black pycnidia on grape leaves.', 'Warm rainy spring during vine shoot emergence'),

    -- Potato (3)
    ('33333333-3333-3333-3333-333333333307', '11111111-1111-1111-1111-111111111101', '22222222-2222-2222-2222-222222222227', 'Healthy potato foliage without blight lesions.', 'Growing season'),

    -- Squash (1)
    ('33333333-3333-3333-3333-333333333318', '11111111-1111-1111-1111-111111111113', '22222222-2222-2222-2222-222222222228', 'White powdery talc-like patches on squash foliage.', 'Dry canopy with high relative ambient humidity'),

    -- Tomato (9)
    ('33333333-3333-3333-3333-333333333306', '11111111-1111-1111-1111-111111111102', '22222222-2222-2222-2222-222222222229', 'Healthy tomato foliage.', 'Growing season'),
    ('33333333-3333-3333-3333-333333333319', '11111111-1111-1111-1111-111111111102', '22222222-2222-2222-2222-222222222230', 'Target-ringed lesions on lower mature tomato foliage.', 'Warm humid weather alternating with showers'),
    ('33333333-3333-3333-3333-333333333320', '11111111-1111-1111-1111-111111111102', '22222222-2222-2222-2222-222222222231', 'Greasy small black spots with yellow halos.', 'Warm rainy periods with windblown overhead splash'),
    ('33333333-3333-3333-3333-333333333321', '11111111-1111-1111-1111-111111111102', '22222222-2222-2222-2222-222222222232', 'Light and dark green mosaic pattern and leaf deformity.', 'Transplanting and pruning stages via physical contact'),
    ('33333333-3333-3333-3333-333333333322', '11111111-1111-1111-1111-111111111102', '22222222-2222-2222-2222-222222222233', 'Upward cupping and yellowing of terminal tomato leaves.', 'Dry warm season coinciding with whitefly outbreaks'),
    ('33333333-3333-3333-3333-333333333323', '11111111-1111-1111-1111-111111111102', '22222222-2222-2222-2222-222222222234', 'Olive velvety fungal growth on leaf undersides.', 'Poorly ventilated greenhouses with humidity > 85%'),
    ('33333333-3333-3333-3333-333333333324', '11111111-1111-1111-1111-111111111102', '22222222-2222-2222-2222-222222222235', 'Fine yellow stippling and silk webbing under leaves.', 'Hot dry weather causing explosive population growth')
ON CONFLICT (plant_id, disease_id) DO NOTHING;

-- ============================================================================
-- 4. English Agricultural Treatment Protocols
-- ============================================================================
INSERT INTO treatments (id, disease_id, plant_id, treatment_type, title, instructions, active_ingredients, dosage_recommendation, safety_interval_days, warning)
VALUES
    -- Apple Scab
    ('44444444-4444-4444-4444-444444444411', '22222222-2222-2222-2222-222222222212', '11111111-1111-1111-1111-111111111105', 'CHEMICAL',
     'Captan / Mancozeb Protective Fungicide Spray',
     'Apply protective sprays beginning at green tip stage and repeat at 7-10 day intervals until petal fall.',
     'Captan 50WP / Mancozeb 75DF',
     '2.0 - 2.5 kg/ha', 14,
     'Do not apply Captan within 14 days of an oil spray to prevent severe phytotoxicity.'),

    -- Cedar Apple Rust
    ('44444444-4444-4444-4444-444444444412', '22222222-2222-2222-2222-222222222213', '11111111-1111-1111-1111-111111111105', 'CHEMICAL',
     'Myclobutanil Systemic Triazole Fungicide',
     'Spray thoroughly from pink bud stage through early fruit development at 10-14 day intervals.',
     'Myclobutanil 20EW (Rally equivalent)',
     '350 - 450 ml/ha', 14,
     'Limit to a maximum of 4 applications per season to prevent pathogen resistance.'),

    -- Bell Pepper Bacterial Leaf Spot
    ('44444444-4444-4444-4444-444444444413', '22222222-2222-2222-2222-222222222215', '11111111-1111-1111-1111-111111111106', 'CHEMICAL',
     'Fixed Copper Hydroxide + Mancozeb Synergistic Tank Mix',
     'Spray foliage at first sign of bacterial spots. The copper-mancozeb combination provides enhanced efficacy against copper-tolerant strains.',
     'Copper Hydroxide 53.8% + Mancozeb 75DF',
     '1.5 kg Copper + 1.5 kg Mancozeb per 1000L water', 7,
     'Avoid spraying during high midday temperatures to prevent chemical leaf burn.'),

    -- Corn Gray Leaf Spot
    ('44444444-4444-4444-4444-444444444414', '22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111104', 'CHEMICAL',
     'Azoxystrobin + Propiconazole Broad-Spectrum Spray',
     'Apply between V12 and early silking (R1) when lesions first appear on the lower third of the plant canopy.',
     'Azoxystrobin 13.5% + Propiconazole 11.7%',
     '1.0 L/ha in 200L water', 30,
     'Ensure good spray coverage of ear leaf and foliage above the ear.'),

    -- Corn Northern Leaf Blight
    ('44444444-4444-4444-4444-444444444415', '22222222-2222-2222-2222-222222222223', '11111111-1111-1111-1111-111111111104', 'CHEMICAL',
     'Pyraclostrobin Foliar Fungicide',
     'Apply at VT-R1 growth stages when environmental forecast indicates extended wet periods.',
     'Pyraclostrobin 250 EC',
     '500 ml/ha', 21,
     'Rotate fungicide classes with different FRAC codes to prevent resistant strains.'),

    -- Grape Black Rot
    ('44444444-4444-4444-4444-444444444416', '22222222-2222-2222-2222-222222222226', '11111111-1111-1111-1111-111111111109', 'CHEMICAL',
     'Tebuconazole / Mancozeb Early Season Protective Spray',
     'Spray from 10cm shoot growth until 4-5 weeks post-bloom to protect young developing foliage and berries.',
     'Tebuconazole 250 EW or Mancozeb 75DF',
     '400 ml/ha (Tebuconazole) or 2.0 kg/ha (Mancozeb)', 28,
     'Critical protection window is from bloom to pea-sized berry stage.'),

    -- Squash Powdery Mildew
    ('44444444-4444-4444-4444-444444444417', '22222222-2222-2222-2222-222222222228', '11111111-1111-1111-1111-111111111113', 'BIOLOGICAL',
     'Potassium Bicarbonate + Pure Neem Oil Organic Protocol',
     'Spray thoroughly covering both upper and lower leaf surfaces every 5-7 days upon first appearance of powdery spots.',
     'Potassium Bicarbonate 85% + Clarified Hydrophobic Extract of Neem Oil 70%',
     '5g Potassium Bicarbonate + 5ml Neem Oil per Liter of water', 0,
     'Safe for harvest on the same day. Do not apply in direct hot sunlight (> 32°C).'),

    -- Tomato Early Blight
    ('44444444-4444-4444-4444-444444444418', '22222222-2222-2222-2222-222222222230', '11111111-1111-1111-1111-111111111102', 'CHEMICAL',
     'Chlorothalonil + Difenoconazole Alternating Foliar Spray',
     'Spray lower leaves at first sight of concentric target lesions. Prune bottom 30cm of foliage to stop splash dispersal.',
     'Chlorothalonil 720 SC (Daconil) / Difenoconazole 250 EC',
     '2.0 L/ha (Chlorothalonil) or 300 ml/ha (Difenoconazole)', 7,
     'Ensure good bottom canopy coverage. Prune dead leaves and destroy off-field.'),

    -- Tomato Bacterial Spot
    ('44444444-4444-4444-4444-444444444419', '22222222-2222-2222-2222-222222222231', '11111111-1111-1111-1111-111111111102', 'CHEMICAL',
     'Copper Octanoate / Copper Sulfate Pentahydrate Spray',
     'Apply protectant copper spray prior to rain events. Avoid overhead sprinkler irrigation.',
     'Copper Octanoate 10% or Basic Copper Sulfate 53%',
     '30-40 ml per 16L sprayer', 3,
     'Disinfect pruning shears between plant rows using 70% isopropyl alcohol.'),

    -- Tomato Mosaic Virus
    ('44444444-4444-4444-4444-444444444420', '22222222-2222-2222-2222-222222222232', '11111111-1111-1111-1111-111111111102', 'PREVENTIVE',
     'Sanitation and Roguing Containment Protocol',
     'Immediately dig up and bag infected plants showing mosaic mottling or fern-leaf distortion. Burn or dispose of in sealed landfill. Wash hands with skim milk or soap.',
     'Trisodium Phosphate (TSP) 10% solution or Non-fat dry milk 20% solution for tool sterilization',
     'Dip pruning shears for 60 seconds between plants', 0,
     'There is no chemical cure for viral infections once established. Never compost infected plant residue.'),

    -- Tomato Yellow Leaf Curl Virus
    ('44444444-4444-4444-4444-444444444421', '22222222-2222-2222-2222-222222222233', '11111111-1111-1111-1111-111111111102', 'BIOLOGICAL',
     'Vector Management: Yellow Sticky Traps + Beauveria bassiana',
     'Deploy yellow sticky cards (1 per 100m2) to trap sweetpotato whitefly vectors. Apply entomopathogenic fungus foliar spray.',
     'Beauveria bassiana strain GHA (min 2x10^10 viable spores/g)',
     '1.5 - 2.0 kg/ha in late afternoon', 0,
     'Controlling whitefly vector populations is the sole effective defense against TYLCV transmission.'),

    -- Tomato Leaf Mold
    ('44444444-4444-4444-4444-444444444422', '22222222-2222-2222-2222-222222222234', '11111111-1111-1111-1111-111111111102', 'CULTURAL',
     'Greenhouse Humidity Reduction & Copper Protectant',
     'Increase ventilation fans, elevate greenhouse temperature, and space plants to ensure relative humidity stays below 85%.',
     'Copper Hydroxide 77% WP',
     '2.0 kg/ha', 3,
     'Avoid wetting foliage during late evening watering.'),

    -- Tomato Spider Mites
    ('44444444-4444-4444-4444-444444444423', '22222222-2222-2222-2222-222222222235', '11111111-1111-1111-1111-111111111102', 'BIOLOGICAL',
     'Phytoseiid Predatory Mites & Abamectin Targeted Spray',
     'Release predatory mites (Phytoseiulus persimilis or Neoseiulus californicus) at 5-10 mites/m2. For severe outbreaks, spot-spray abamectin.',
     'Phytoseiulus persimilis (beneficial predator) / Abamectin 1.8% EC',
     'Predators: 5-10/m2; Abamectin: 15-20 ml per 16L water', 7,
     'Spray underside of leaves thoroughly where spider mite colonies reside.')
ON CONFLICT (id) DO NOTHING;
