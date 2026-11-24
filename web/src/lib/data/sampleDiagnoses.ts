import type { SampleSpecimen } from '@/types/pdds';

function svgToDataUri(svg: string): string {
  return `data:image/svg+xml;utf8,${encodeURIComponent(svg.trim())}`;
}

const POTATO_LEAF_SVG = svgToDataUri(`
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 520">
  <rect width="800" height="520" fill="#2A3319"/>
  <g opacity="0.22" stroke="#6E7B4F" stroke-width="1">
    <line x1="0" y1="130" x2="800" y2="130"/>
    <line x1="0" y1="260" x2="800" y2="260"/>
    <line x1="0" y1="390" x2="800" y2="390"/>
    <line x1="200" y1="0" x2="200" y2="520"/>
    <line x1="400" y1="0" x2="400" y2="520"/>
    <line x1="600" y1="0" x2="600" y2="520"/>
  </g>
  <path d="M130 278 C210 108, 455 82, 650 220 C555 392, 305 426, 130 278 Z" fill="#4D7836" stroke="#233817" stroke-width="4"/>
  <path d="M92 292 L134 277" stroke="#6C8F4B" stroke-width="8" stroke-linecap="round"/>
  <path d="M132 278 Q390 250 644 221" stroke="#8AB267" stroke-width="4.5" fill="none"/>
  <path d="M245 266 L195 178 M335 256 L282 148 M430 245 L385 142 M518 235 L482 164 M270 268 L228 355 M372 256 L332 372 M474 244 L442 346" stroke="#8AB267" stroke-width="2.6" fill="none" stroke-linecap="round"/>
  <!-- Water-soaked late blight lesion #1 -->
  <path d="M408 166 C446 148, 508 164, 520 206 C530 244, 480 278, 434 264 C394 252, 382 190, 408 166 Z" fill="#5B4423"/>
  <path d="M424 180 C452 168, 492 178, 498 208 C504 234, 468 254, 440 244 C416 235, 410 194, 424 180 Z" fill="#321D0E"/>
  <path d="M418 238 C438 252, 474 256, 496 240" stroke="#DDD6C5" stroke-width="3" stroke-dasharray="4 4" fill="none" opacity="0.75"/>
  <!-- Secondary lesion #2 -->
  <ellipse cx="322" cy="304" rx="32" ry="22" fill="#4A2E18"/>
  <ellipse cx="325" cy="305" rx="18" ry="12" fill="#29160A"/>
</svg>
`);

const TOMATO_LEAF_SVG = svgToDataUri(`
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 520">
  <rect width="800" height="520" fill="#2A3319"/>
  <path d="M145 268 C195 162, 290 120, 410 128 C515 135, 608 182, 658 252 C590 344, 485 394, 375 392 C265 390, 185 342, 145 268 Z" fill="#4B7F39" stroke="#223B19" stroke-width="4"/>
  <path d="M100 275 L148 268" stroke="#769E58" stroke-width="7" stroke-linecap="round"/>
  <path d="M148 268 Q400 256 652 252" stroke="#90BC70" stroke-width="4" fill="none"/>
  <path d="M250 264 L204 185 M356 260 L312 158 M468 256 L426 164 M282 266 L240 348 M396 260 L356 362 M506 255 L472 338" stroke="#90BC70" stroke-width="2.4" fill="none"/>
  <!-- Concentric target-ring early blight lesion #1 -->
  <circle cx="348" cy="214" r="38" fill="#9E8231" opacity="0.85"/>
  <circle cx="348" cy="214" r="28" fill="#523318"/>
  <circle cx="348" cy="214" r="20" fill="none" stroke="#85582F" stroke-width="2.5"/>
  <circle cx="348" cy="214" r="11" fill="none" stroke="#85582F" stroke-width="2.2"/>
  <!-- Target-ring lesion #2 -->
  <circle cx="486" cy="292" r="30" fill="#A38734" opacity="0.85"/>
  <circle cx="486" cy="292" r="21" fill="#4C2E15"/>
  <circle cx="486" cy="292" r="13" fill="none" stroke="#875A31" stroke-width="2.2"/>
</svg>
`);

const RICE_LEAF_SVG = svgToDataUri(`
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 520">
  <rect width="800" height="520" fill="#2A3319"/>
  <path d="M68 332 Q395 190 738 168 Q430 286 84 368 Z" fill="#598A39" stroke="#294218" stroke-width="3.5"/>
  <path d="M76 350 Q405 236 732 169" stroke="#98C474" stroke-width="2.8" fill="none"/>
  <!-- Diamond/spindle-shaped rice blast lesion #1 -->
  <path d="M330 258 L388 232 L446 238 L388 266 Z" fill="#7D321D"/>
  <path d="M350 254 L388 239 L426 242 L388 259 Z" fill="#D5CEBE"/>
  <!-- Diamond/spindle-shaped rice blast lesion #2 -->
  <path d="M492 214 L542 194 L590 198 L540 220 Z" fill="#7D321D"/>
  <path d="M510 210 L541 199 L572 201 L541 214 Z" fill="#D5CEBE"/>
</svg>
`);

export const SAMPLE_SPECIMENS: SampleSpecimen[] = [
  {
    key: 'potato-late-blight',
    shortLabel: 'Potato late blight',
    response: {
      diagnosisId: '55555555-5555-5555-5555-555555555501',
      originalImageUrl: POTATO_LEAF_SVG,
      imageWidth: 800,
      imageHeight: 520,
      plant: {
        id: '11111111-1111-1111-1111-111111111101',
        plantCode: 'POTATO',
        commonName: 'Potato',
        scientificName: 'Solanum tuberosum',
      },
      dominantDisease: {
        id: '22222222-2222-2222-2222-222222222201',
        diseaseCode: 'POTATO_LATE_BLIGHT',
        commonName: 'Potato late blight',
        scientificName: 'Phytophthora infestans',
        severity: 'CRITICAL',
      },
      dominantConfidence: 0.894,
      boundingBoxes: [
        {
          id: 'b1-potato',
          label: 'Potato leaf late blight',
          diseaseCode: 'POTATO_LATE_BLIGHT',
          confidence: 0.894,
          x: 456,
          y: 212,
          width: 148,
          height: 128,
          xMin: 382,
          yMin: 148,
          xMinNormalized: 0.4775,
          yMinNormalized: 0.2846,
          widthNormalized: 0.185,
          heightNormalized: 0.2462,
        },
        {
          id: 'b2-potato',
          label: 'Potato leaf late blight',
          diseaseCode: 'POTATO_LATE_BLIGHT',
          confidence: 0.782,
          x: 322,
          y: 304,
          width: 78,
          height: 58,
          xMin: 283,
          yMin: 275,
          xMinNormalized: 0.3538,
          yMinNormalized: 0.5288,
          widthNormalized: 0.0975,
          heightNormalized: 0.1115,
        },
      ],
      coOccurringDiseases: [],
      recommendedTreatments: [
        {
          id: 'tr-potato-bio',
          treatmentType: 'BIOLOGICAL',
          title: 'Biological suppression with Trichoderma & Bacillus',
          instructions:
            'Prune and destroy water-soaked lower leaves away from the field. Spray Trichoderma harzianum or Bacillus subtilis bio-fungicide in early morning after dew dries.',
          activeIngredients: 'Trichoderma harzianum / Bacillus subtilis',
          dosageRecommendation: '30–40g per 16L knapsack sprayer with full foliar coverage.',
          safetyIntervalDays: 0,
          warning: 'Do not tank-mix microbial bio-agents with copper or synthetic chemical fungicides.',
        },
        {
          id: 'tr-potato-chem',
          treatmentType: 'CHEMICAL',
          title: 'Targeted Mancozeb protection during cool wet spells',
          instructions:
            'When lesions expand rapidly in cool, foggy weather, apply protective-curative fungicide thoroughly across upper and lower leaf surfaces.',
          activeIngredients: 'Mancozeb 80WP or Cymoxanil + Mancozeb',
          dosageRecommendation: '40g per 16L water; repeat once after 5–7 days if humid fog persists.',
          safetyIntervalDays: 7,
          warning: 'Wear protective gear during application; strictly observe a 7-day pre-harvest interval.',
        },
        {
          id: 'tr-potato-cult',
          treatmentType: 'CULTURAL',
          title: 'High hilling & morning-only root irrigation',
          instructions:
            'Hill soil firmly over tubers so sporangia washed from leaves cannot infect tubers. Avoid overhead irrigation in late afternoon.',
          activeIngredients: null,
          dosageRecommendation: 'Clear furrow drainage immediately after heavy rain.',
          safetyIntervalDays: 0,
          warning: null,
        },
      ],
      inferenceTimeMs: 248,
      status: 'COMPLETED',
      diagnosedAt: '2026-11-24T08:30:00Z',
    },
  },
  {
    key: 'tomato-early-blight',
    shortLabel: 'Tomato early blight',
    response: {
      diagnosisId: '55555555-5555-5555-5555-555555555502',
      originalImageUrl: TOMATO_LEAF_SVG,
      imageWidth: 800,
      imageHeight: 520,
      plant: {
        id: '11111111-1111-1111-1111-111111111102',
        plantCode: 'TOMATO',
        commonName: 'Tomato',
        scientificName: 'Solanum lycopersicum',
      },
      dominantDisease: {
        id: '22222222-2222-2222-2222-222222222230',
        diseaseCode: 'TOMATO_EARLY_BLIGHT',
        commonName: 'Tomato early blight',
        scientificName: 'Alternaria solani',
        severity: 'HIGH',
      },
      dominantConfidence: 0.912,
      boundingBoxes: [
        {
          id: 'b1-tomato',
          label: 'Tomato Early blight leaf',
          diseaseCode: 'TOMATO_EARLY_BLIGHT',
          confidence: 0.912,
          x: 348,
          y: 214,
          width: 92,
          height: 92,
          xMin: 302,
          yMin: 168,
          xMinNormalized: 0.3775,
          yMinNormalized: 0.3231,
          widthNormalized: 0.115,
          heightNormalized: 0.1769,
        },
        {
          id: 'b2-tomato',
          label: 'Tomato Early blight leaf',
          diseaseCode: 'TOMATO_EARLY_BLIGHT',
          confidence: 0.845,
          x: 486,
          y: 292,
          width: 74,
          height: 74,
          xMin: 449,
          yMin: 255,
          xMinNormalized: 0.5613,
          yMinNormalized: 0.4904,
          widthNormalized: 0.0925,
          heightNormalized: 0.1423,
        },
      ],
      coOccurringDiseases: [],
      recommendedTreatments: [
        {
          id: 'tr-tomato-bio',
          treatmentType: 'BIOLOGICAL',
          title: 'Lower canopy pruning, mulching & Bacillus bio-spray',
          instructions:
            'Remove infected lower leaves within 20–25cm of soil and lay clean straw mulch to block rain-splashed Alternaria spores.',
          activeIngredients: 'Bacillus subtilis (strain QST 713)',
          dosageRecommendation: '35ml bio-concentrate per 16L water.',
          safetyIntervalDays: 0,
          warning: null,
        },
        {
          id: 'tr-tomato-chem',
          treatmentType: 'CHEMICAL',
          title: 'Targeted Azoxystrobin or Chlorothalonil spray',
          instructions:
            'When concentric target spots climb toward fruit-bearing trusses, spray evenly in cool morning hours.',
          activeIngredients: 'Azoxystrobin or Chlorothalonil 75WP',
          dosageRecommendation: '20–25g per 16L sprayer; rotate FRAC groups to prevent resistance.',
          safetyIntervalDays: 5,
          warning: 'Observe at least a 5-day pre-harvest interval before picking ripe fruit.',
        },
      ],
      inferenceTimeMs: 219,
      status: 'COMPLETED',
      diagnosedAt: '2026-11-24T09:00:00Z',
    },
  },
  {
    key: 'rice-blast',
    shortLabel: 'Rice blast',
    response: {
      diagnosisId: '55555555-5555-5555-5555-555555555503',
      originalImageUrl: RICE_LEAF_SVG,
      imageWidth: 800,
      imageHeight: 520,
      plant: {
        id: '11111111-1111-1111-1111-111111111103',
        plantCode: 'RICE',
        commonName: 'Rice',
        scientificName: 'Oryza sativa',
      },
      dominantDisease: {
        id: '22222222-2222-2222-2222-222222222205',
        diseaseCode: 'RICE_BLAST',
        commonName: 'Rice blast',
        scientificName: 'Magnaporthe oryzae',
        severity: 'CRITICAL',
      },
      dominantConfidence: 0.905,
      boundingBoxes: [
        {
          id: 'b1-rice',
          label: 'Rice Blast',
          diseaseCode: 'RICE_BLAST',
          confidence: 0.905,
          x: 388,
          y: 249,
          width: 128,
          height: 52,
          xMin: 324,
          yMin: 223,
          xMinNormalized: 0.405,
          yMinNormalized: 0.4288,
          widthNormalized: 0.16,
          heightNormalized: 0.1,
        },
        {
          id: 'b2-rice',
          label: 'Rice Blast',
          diseaseCode: 'RICE_BLAST',
          confidence: 0.861,
          x: 541,
          y: 207,
          width: 112,
          height: 44,
          xMin: 485,
          yMin: 185,
          xMinNormalized: 0.6063,
          yMinNormalized: 0.3558,
          widthNormalized: 0.14,
          heightNormalized: 0.0846,
        },
      ],
      coOccurringDiseases: [],
      recommendedTreatments: [
        {
          id: 'tr-rice-cult',
          treatmentType: 'CULTURAL',
          title: 'Maintain 3–5cm paddy water & halt nitrogen top-dressing',
          instructions:
            'Immediately suspend urea/nitrogen fertilizer and foliar feeds while spindle lesions are active. Keep 3–5cm standing water to reduce plant stress.',
          activeIngredients: null,
          dosageRecommendation: 'Resume balanced potassium only after lesions dry completely.',
          safetyIntervalDays: 0,
          warning: 'Drained paddies combined with excess nitrogen trigger rapid leaf blast collapse.',
        },
        {
          id: 'tr-rice-bio',
          treatmentType: 'BIOLOGICAL',
          title: 'Fermentation-derived Kasugamycin bio-bactericide/fungicide',
          instructions:
            'Apply at early pinpoint or small spindle stage across the canopy in cool late afternoon.',
          activeIngredients: 'Kasugamycin 2SL',
          dosageRecommendation: '35–40ml per 16L sprayer.',
          safetyIntervalDays: 7,
          warning: null,
        },
        {
          id: 'tr-rice-chem',
          treatmentType: 'CHEMICAL',
          title: 'Systemic Tricyclazole protection against neck blast',
          instructions:
            'Apply systemic melanin-biosynthesis inhibitor prior to heading to protect panicle necks.',
          activeIngredients: 'Tricyclazole 75WP or Isoprothiolane',
          dosageRecommendation: '15–20g per 16L sprayer.',
          safetyIntervalDays: 14,
          warning: 'Strictly observe a 14-day pre-harvest interval before harvesting grain.',
        },
      ],
      inferenceTimeMs: 234,
      status: 'COMPLETED',
      diagnosedAt: '2026-11-24T09:15:00Z',
    },
  },
];
