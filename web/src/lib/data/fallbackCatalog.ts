import type { DiseaseDetailDto, DiseaseSummaryDto, PlantDto, TreatmentDto } from '@/types/pdds';
import { VI_DISEASE_NAMES, VI_PLANT_NAMES } from '@/lib/i18n/dictionary';
import { getDiseaseLocalizationSearchText } from '@/lib/i18n/viCatalog';
import { normalizeSearchText } from '@/lib/i18n/search';

export interface FallbackDiseaseRecord extends DiseaseDetailDto {
  plantCodes: string[];
}

export const FALLBACK_PLANTS: PlantDto[] = [
  {
    id: '11111111-1111-1111-1111-111111111101',
    plantCode: 'POTATO',
    commonName: 'Potato',
    scientificName: 'Solanum tuberosum',
    description: 'Highland and cool-season tuber crop susceptible to late and early blight.',
  },
  {
    id: '11111111-1111-1111-1111-111111111102',
    plantCode: 'TOMATO',
    commonName: 'Tomato',
    scientificName: 'Solanum lycopersicum',
    description: 'Popular fruiting vegetable crop prone to foliar blights and leaf curl viruses.',
  },
  {
    id: '11111111-1111-1111-1111-111111111103',
    plantCode: 'RICE',
    commonName: 'Rice',
    scientificName: 'Oryza sativa',
    description: 'Staple grain crop requiring close surveillance for leaf and neck blast.',
  },
  {
    id: '11111111-1111-1111-1111-111111111104',
    plantCode: 'CORN',
    commonName: 'Corn / Maize',
    scientificName: 'Zea mays',
    description: 'Grain and silage maize commonly monitored for common rust, gray leaf spot, and northern blight.',
  },
  {
    id: '11111111-1111-1111-1111-111111111106',
    plantCode: 'BELL_PEPPER',
    commonName: 'Bell Pepper',
    scientificName: 'Capsicum annuum',
    description: 'Greenhouse and field sweet pepper sensitive to bacterial leaf spot in rainy seasons.',
  },
  {
    id: '11111111-1111-1111-1111-111111111105',
    plantCode: 'APPLE',
    commonName: 'Apple',
    scientificName: 'Malus domestica',
    description: 'Temperate and cool-microclimate orchard crop monitored for scab and cedar apple rust.',
  },
  {
    id: '11111111-1111-1111-1111-111111111109',
    plantCode: 'GRAPE',
    commonName: 'Grape',
    scientificName: 'Vitis vinifera',
    description: 'Table and vineyard grapevine vulnerable to black rot on leaves and berry clusters.',
  },
  {
    id: '11111111-1111-1111-1111-111111111110',
    plantCode: 'PEACH',
    commonName: 'Peach',
    scientificName: 'Prunus persica',
    description: 'Highland stone-fruit orchard crop supported for healthy foliage verification.',
  },
  {
    id: '11111111-1111-1111-1111-111111111112',
    plantCode: 'SOYBEAN',
    commonName: 'Soybean',
    scientificName: 'Glycine max',
    description: 'Nitrogen-fixing rotational legume crop supported for healthy foliage verification.',
  },
];

function buildEnglishBioAndChemTreatments(
  code: string,
  bioInstructions: string,
  chemIngredient: string,
  waitDays: number
): TreatmentDto[] {
  return [
    {
      id: `${code}-bio`,
      treatmentType: 'BIOLOGICAL',
      title: 'Canopy sanitation & microbial bio-control',
      instructions: bioInstructions,
      activeIngredients: 'Trichoderma spp. / Bacillus subtilis',
      dosageRecommendation: '30–40g (or ml) per 16L sprayer in early morning.',
      safetyIntervalDays: 0,
      warning: 'Apply proactively at first sight of isolated foliar lesions.',
    },
    {
      id: `${code}-chem`,
      treatmentType: 'CHEMICAL',
      title: `Targeted ${chemIngredient} protection`,
      instructions:
        'Apply thoroughly to upper and lower leaf surfaces during prolonged wet weather when disease pressure exceeds threshold.',
      activeIngredients: chemIngredient,
      dosageRecommendation: 'Follow labeled rate per 16L knapsack sprayer.',
      safetyIntervalDays: waitDays,
      warning:
        waitDays > 0
          ? `Strictly observe a ${waitDays}-day pre-harvest interval.`
          : null,
    },
  ];
}

export const FALLBACK_DISEASES: FallbackDiseaseRecord[] = [
  {
    id: '22222222-2222-2222-2222-222222222201',
    diseaseCode: 'POTATO_LATE_BLIGHT',
    commonName: 'Potato late blight',
    scientificName: 'Phytophthora infestans',
    severity: 'CRITICAL',
    plantCodes: ['POTATO'],
    symptoms:
      'Rapidly expanding dark brown to black water-soaked foliar lesions with white fuzzy mildew sporulation visible on undersides during humid mornings.',
    description:
      'Aggressive oomycete pathogen favored by cool temperatures (15–22°C), fog, and prolonged leaf wetness, capable of rotting tubers rapidly.',
    treatments: buildEnglishBioAndChemTreatments(
      'POTATO_LATE_BLIGHT',
      'Remove water-soaked lower foliage away from plots, hill soil over tubers, and spray Trichoderma bio-agent.',
      'Mancozeb 80WP / Cymoxanil',
      7
    ),
  },
  {
    id: '22222222-2222-2222-2222-222222222202',
    diseaseCode: 'POTATO_EARLY_BLIGHT',
    commonName: 'Potato early blight',
    scientificName: 'Alternaria solani',
    severity: 'HIGH',
    plantCodes: ['POTATO'],
    symptoms:
      'Small brown-black spots with characteristic concentric target-board rings appearing on older lower foliage with chlorotic yellow margins.',
    description:
      'Soilborne fungal pathogen favored by alternating wet and warm dry periods (24–29°C), starting on mature lower foliage.',
    treatments: buildEnglishBioAndChemTreatments(
      'POTATO_EARLY_BLIGHT',
      'Prune senescent lower leaves showing target-ring lesions and maintain balanced potassium nutrition.',
      'Azoxystrobin / Chlorothalonil',
      7
    ),
  },
  {
    id: '22222222-2222-2222-2222-222222222203',
    diseaseCode: 'TOMATO_LATE_BLIGHT',
    commonName: 'Tomato late blight',
    scientificName: 'Phytophthora infestans',
    severity: 'CRITICAL',
    plantCodes: ['TOMATO'],
    symptoms:
      'Pale green irregular water-soaked foliar lesions turning dark purplish-brown greasy patches with delicate white fungal sporulation underneath.',
    description:
      'Destructive oomycete spreading rapidly in cool rainy weather, attacking leaves, stems, and green tomato fruit.',
    treatments: buildEnglishBioAndChemTreatments(
      'TOMATO_LATE_BLIGHT',
      'Prune dense tomato foliage for airflow, shelter from rain splash, and apply Bacillus subtilis.',
      'Mancozeb / Mandipropamid',
      5
    ),
  },
  {
    id: '22222222-2222-2222-2222-222222222230',
    diseaseCode: 'TOMATO_EARLY_BLIGHT',
    commonName: 'Tomato early blight',
    scientificName: 'Alternaria solani',
    severity: 'HIGH',
    plantCodes: ['TOMATO'],
    symptoms:
      'Concentric target-board ringed brown lesions starting on bottom mature leaves, progressing into chlorotic leaf blight and stem lesions.',
    description:
      'Widespread fungal disease thriving in warm humid conditions with heavy dew or overhead watering.',
    treatments: buildEnglishBioAndChemTreatments(
      'TOMATO_EARLY_BLIGHT',
      'Remove lower leaves beneath the first fruit cluster, mulch soil beds, and apply microbial bio-fungicide.',
      'Azoxystrobin / Chlorothalonil',
      5
    ),
  },
  {
    id: '22222222-2222-2222-2222-222222222204',
    diseaseCode: 'TOMATO_SEPTORIA_LEAF_SPOT',
    commonName: 'Tomato Septoria leaf spot',
    scientificName: 'Septoria lycopersici',
    severity: 'HIGH',
    plantCodes: ['TOMATO'],
    symptoms:
      'Circular spots with dark brown margins and sunken grayish-tan centers densely dotted with tiny black reproductive pycnidia.',
    description:
      'Foliar fungal pathogen spreading upward from lower leaves via rain splash at 20–25°C.',
    treatments: buildEnglishBioAndChemTreatments(
      'TOMATO_SEPTORIA_LEAF_SPOT',
      'Clear fallen leaf debris promptly, rotate out of solanaceous crops, and apply biological foliar spray.',
      'Copper hydroxide / Mancozeb',
      5
    ),
  },
  {
    id: '22222222-2222-2222-2222-222222222231',
    diseaseCode: 'TOMATO_BACTERIAL_SPOT',
    commonName: 'Tomato bacterial spot',
    scientificName: 'Xanthomonas vesicatoria',
    severity: 'HIGH',
    plantCodes: ['TOMATO'],
    symptoms:
      'Small water-soaked dark brown to black angular greasy spots surrounded by yellow haloes, causing tattered foliage.',
    description:
      'Contagious bacterial pathogen driven by warm driving rain (24–30°C) and handling wet foliage.',
    treatments: buildEnglishBioAndChemTreatments(
      'TOMATO_BACTERIAL_SPOT',
      'Avoid pruning or handling wet plants; plant certified clean seed and apply Bacillus bio-bactericide.',
      'Kasugamycin / Copper oxychloride',
      7
    ),
  },
  {
    id: '22222222-2222-2222-2222-222222222233',
    diseaseCode: 'TOMATO_YELLOW_VIRUS',
    commonName: 'Tomato yellow leaf curl virus',
    scientificName: 'Tomato yellow leaf curl virus (TYLCV)',
    severity: 'CRITICAL',
    plantCodes: ['TOMATO'],
    symptoms:
      'Severe upward leaf cupping, pronounced interveinal chlorosis, bush-like dwarfed apical shoots, and flower drop.',
    description:
      'Begomovirus transmitted by whiteflies (Bemisia tabaci), most severe in hot dry weather with high vector populations.',
    treatments: buildEnglishBioAndChemTreatments(
      'TOMATO_YELLOW_VIRUS',
      'Rogue severely stunted plants in sealed bags; hang yellow sticky traps and spray botanical neem/mineral oil against whiteflies.',
      'Vector control (Imidacloprid / Pymetrozine)',
      7
    ),
  },
  {
    id: '22222222-2222-2222-2222-222222222205',
    diseaseCode: 'RICE_BLAST',
    commonName: 'Rice blast',
    scientificName: 'Magnaporthe oryzae',
    severity: 'CRITICAL',
    plantCodes: ['RICE'],
    symptoms:
      'Spindle- or diamond-shaped lesions with grayish-white necrotic centers and reddish-brown margins on leaf blades; dark rotten neck lesions.',
    description:
      'Major rice fungal pathology favored by heavy morning dew, cool nights, cloudy days, and excess nitrogen.',
    treatments: buildEnglishBioAndChemTreatments(
      'RICE_BLAST',
      'Stop nitrogen top-dressing immediately, hold 3–5cm paddy water, and spray Kasugamycin.',
      'Tricyclazole 75WP / Isoprothiolane',
      14
    ),
  },
  {
    id: '22222222-2222-2222-2222-222222222224',
    diseaseCode: 'CORN_COMMON_RUST',
    commonName: 'Common rust',
    scientificName: 'Puccinia sorghi',
    severity: 'MODERATE',
    plantCodes: ['CORN'],
    symptoms:
      'Small cinnamon-brown to dark reddish powdery pustules erupting scattered across both upper and lower leaf surfaces.',
    description:
      'Airborne rust fungus favored by cool temperatures (16–23°C) and high humidity.',
    treatments: buildEnglishBioAndChemTreatments(
      'CORN_COMMON_RUST',
      'Select rust-tolerant hybrids and maintain row spacing for rapid morning canopy drying.',
      'Azoxystrobin / Propiconazole',
      14
    ),
  },
  {
    id: '22222222-2222-2222-2222-222222222222',
    diseaseCode: 'CORN_GRAY_LEAF_SPOT',
    commonName: 'Gray leaf spot',
    scientificName: 'Cercospora zeae-maydis',
    severity: 'HIGH',
    plantCodes: ['CORN'],
    symptoms:
      'Narrow rectangular tan to grayish lesions strictly delimited by leaf veins running parallel to the leaf margin.',
    description:
      'Foliar fungal disease favored by warm humid weather and heavy residue from continuous corn.',
    treatments: buildEnglishBioAndChemTreatments(
      'CORN_GRAY_LEAF_SPOT',
      'Incorporate corn residue after harvest and rotate with soybean to break inoculum carryover.',
      'Pyraclostrobin / Propiconazole',
      14
    ),
  },
  {
    id: '22222222-2222-2222-2222-222222222223',
    diseaseCode: 'CORN_LEAF_BLIGHT',
    commonName: 'Northern corn leaf blight',
    scientificName: 'Exserohilum turcicum',
    severity: 'HIGH',
    plantCodes: ['CORN'],
    symptoms:
      'Long cigar-shaped elliptical grayish-green to tan necrotic lesions (2.5 to 15 cm long) extending across leaf blades.',
    description:
      'Major maize foliar fungus thriving in moderate temperatures (18–27°C) with prolonged dew.',
    treatments: buildEnglishBioAndChemTreatments(
      'CORN_LEAF_BLIGHT',
      'Manage infected crop debris, balance N-P-K fertilization, and apply early bio-fungicide.',
      'Mancozeb / Azoxystrobin',
      14
    ),
  },
  {
    id: '22222222-2222-2222-2222-222222222215',
    diseaseCode: 'BELL_PEPPER_LEAF_SPOT',
    commonName: 'Bacterial leaf spot',
    scientificName: 'Xanthomonas campestris pv. vesicatoria',
    severity: 'HIGH',
    plantCodes: ['BELL_PEPPER'],
    symptoms:
      'Small water-soaked circular spots turning dark brown with pronounced chlorotic yellow haloes, causing premature canopy defoliation.',
    description:
      'Bacterial pathogen spreading rapidly in warm tropical rain (25–30°C) and wind-driven splash.',
    treatments: buildEnglishBioAndChemTreatments(
      'BELL_PEPPER_LEAF_SPOT',
      'Switch from overhead sprinklers to drip irrigation, remove infected lower leaves, and spray Bacillus.',
      'Copper hydroxide / Kasugamycin',
      5
    ),
  },
  {
    id: '22222222-2222-2222-2222-222222222212',
    diseaseCode: 'APPLE_SCAB',
    commonName: 'Apple scab',
    scientificName: 'Venturia inaequalis',
    severity: 'HIGH',
    plantCodes: ['APPLE'],
    symptoms:
      'Olive-green to dull dark brown velvety spots developing on leaf blades, causing crinkling, cupping, and premature leaf drop.',
    description:
      'Fungal disease driven by cool spring rain (10–20°C) during bud break and leaf expansion.',
    treatments: buildEnglishBioAndChemTreatments(
      'APPLE_SCAB',
      'Rake and destroy fallen overwintering leaves and prune interior branches for rapid drying.',
      'Captan / Mancozeb',
      14
    ),
  },
  {
    id: '22222222-2222-2222-2222-222222222213',
    diseaseCode: 'APPLE_RUST',
    commonName: 'Cedar apple rust',
    scientificName: 'Gymnosporangium juniperi-virginianae',
    severity: 'MODERATE',
    plantCodes: ['APPLE'],
    symptoms:
      'Bright yellow-orange or reddish circular lesions on the upper leaf surface that develop dark central pycnia and tubular aecia on the underside.',
    description:
      'Rust fungus cycling between juniper hosts and apple foliage during wet spring winds.',
    treatments: buildEnglishBioAndChemTreatments(
      'APPLE_RUST',
      'Prune rust galls on nearby juniper/cedar hosts and protect young leaves early.',
      'Myclobutanil / Sulfur',
      14
    ),
  },
  {
    id: '22222222-2222-2222-2222-222222222226',
    diseaseCode: 'GRAPE_BLACK_ROT',
    commonName: 'Grape black rot',
    scientificName: 'Guignardia bidwellii',
    severity: 'HIGH',
    plantCodes: ['GRAPE'],
    symptoms:
      'Circular reddish-brown necrotic spots with distinct dark borders on leaves, dotted with tiny black pycnidia; fruit shrivels into black hard mummies.',
    description:
      'Fungal vineyard disease thriving in warm humid weather (21–29°C) around bloom and berry set.',
    treatments: buildEnglishBioAndChemTreatments(
      'GRAPE_BLACK_ROT',
      'Sanitize mummified berries from trellises and open the fruiting zone canopy to sunlight.',
      'Mancozeb / Myclobutanil',
      14
    ),
  },
];

export function findFallbackDiseaseRecord(codeOrId: string): FallbackDiseaseRecord | undefined {
  const norm = codeOrId.trim().toUpperCase();
  return FALLBACK_DISEASES.find(
    (d) => d.diseaseCode.toUpperCase() === norm || d.id.toUpperCase() === norm
  );
}

export function filterFallbackDiseases(params?: {
  plantCode?: string;
  keyword?: string;
}): DiseaseSummaryDto[] {
  const plantNorm = params?.plantCode?.trim().toUpperCase();
  const kw = normalizeSearchText(params?.keyword ?? '');

  return FALLBACK_DISEASES.filter((d) => {
    if (plantNorm && !d.plantCodes.includes(plantNorm)) {
      return false;
    }
    if (kw) {
      const localizedDisease = VI_DISEASE_NAMES[d.diseaseCode] ?? '';
      const localizedPlants = d.plantCodes.map((code) => VI_PLANT_NAMES[code] ?? '').join(' ');
      const localizedSymptoms = getDiseaseLocalizationSearchText(d.diseaseCode);
      const haystack = normalizeSearchText(
        `${d.diseaseCode} ${d.commonName} ${d.scientificName ?? ''} ${localizedDisease} ${localizedPlants} ${localizedSymptoms} ${d.plantCodes.join(
          ' '
        )}`
      );
      if (!haystack.includes(kw)) {
        return false;
      }
    }
    return true;
  }).map(({ id, diseaseCode, commonName, scientificName, severity }) => ({
    id,
    diseaseCode,
    commonName,
    scientificName,
    severity,
  }));
}
