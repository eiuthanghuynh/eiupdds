import type { DiseaseDetailDto, DiseaseSummaryDto, PlantDto, TreatmentDto } from '@/types/pdds';
import { VI_DISEASE_NAMES, VI_PLANT_NAMES } from '@/lib/i18n/dictionary';

export interface FallbackDiseaseRecord extends DiseaseDetailDto {
  plantCodes: string[];
  viSymptoms: string;
  viDescription: string;
  viTreatments: TreatmentDto[];
}

export const FALLBACK_PLANTS: PlantDto[] = [
  {
    id: '11111111-1111-1111-1111-111111111101',
    plantCode: 'POTATO',
    commonName: 'Potato',
    scientificName: 'Solanum tuberosum',
    description: 'Cây lấy củ vùng cao nguyên và vụ đông, nhạy cảm với bệnh mốc sương và đốm vòng.',
  },
  {
    id: '11111111-1111-1111-1111-111111111102',
    plantCode: 'TOMATO',
    commonName: 'Tomato',
    scientificName: 'Solanum lycopersicum',
    description: 'Cây rau ăn quả phổ biến, dễ nhiễm nấm lá và virus xoăn vàng lá.',
  },
  {
    id: '11111111-1111-1111-1111-111111111103',
    plantCode: 'RICE',
    commonName: 'Rice',
    scientificName: 'Oryza sativa',
    description: 'Cây lương thực chủ lực, cần theo dõi sát bệnh đạo ôn lá và cổ bông.',
  },
  {
    id: '11111111-1111-1111-1111-111111111104',
    plantCode: 'CORN',
    commonName: 'Corn / Maize',
    scientificName: 'Zea mays',
    description: 'Cây ngô lấy hạt và sinh khối, thường gặp gỉ sắt, đốm xám và cháy lá lớn.',
  },
  {
    id: '11111111-1111-1111-1111-111111111106',
    plantCode: 'BELL_PEPPER',
    commonName: 'Bell Pepper',
    scientificName: 'Capsicum annuum',
    description: 'Ớt ngọt nhà màng và ngoài đồng, mẫn cảm với đốm lá vi khuẩn mùa mưa.',
  },
  {
    id: '11111111-1111-1111-1111-111111111105',
    plantCode: 'APPLE',
    commonName: 'Apple',
    scientificName: 'Malus domestica',
    description: 'Cây ăn quả ôn đới và tiểu khí hậu mát, thường gặp bệnh ghẻ sẹo và gỉ sắt.',
  },
  {
    id: '11111111-1111-1111-1111-111111111109',
    plantCode: 'GRAPE',
    commonName: 'Grape',
    scientificName: 'Vitis vinifera',
    description: 'Cây nho ăn tươi, dễ phát sinh bệnh thối đen trên lá và chùm quả khi ẩm.',
  },
  {
    id: '11111111-1111-1111-1111-111111111110',
    plantCode: 'PEACH',
    commonName: 'Peach',
    scientificName: 'Prunus persica',
    description: 'Cây đào ăn quả vùng cao, hệ thống nhận diện tình trạng lá khỏe và đốm vi khuẩn.',
  },
  {
    id: '11111111-1111-1111-1111-111111111112',
    plantCode: 'SOYBEAN',
    commonName: 'Soybean',
    scientificName: 'Glycine max',
    description: 'Cây đậu nành giàu đạm luân canh cải tạo đất.',
  },
];

function quickBioAndChem(
  code: string,
  bioVi: string,
  bioEn: string,
  chemIngredient: string,
  waitDays: number
): { vi: TreatmentDto[]; en: TreatmentDto[] } {
  return {
    vi: [
      {
        id: `${code}-bio`,
        treatmentType: 'BIOLOGICAL',
        title: 'Vệ sinh tán lá & phun vi sinh đối kháng',
        instructions: bioVi,
        activeIngredients: 'Trichoderma spp. / Bacillus subtilis',
        dosageRecommendation: '30–40g (hoặc ml) pha bình 16 lít nước, phun sáng sớm.',
        safetyIntervalDays: 0,
        warning: 'Ưu tiên thực hiện ngay khi bệnh mới chớm 1–2 đốm nhỏ trên lá.',
      },
      {
        id: `${code}-chem`,
        treatmentType: 'CHEMICAL',
        title: `Phun đặc trị ${chemIngredient} khi bệnh lan rộng`,
        instructions: `Chỉ phun khi thời tiết ẩm ướt kéo dài làm vết bệnh lan nhanh trên tán lá. Phun ướt đều hai mặt lá lúc trời tạnh ráo.`,
        activeIngredients: chemIngredient,
        dosageRecommendation: 'Pha đúng liều khuyến cáo trên nhãn cho bình 16 lít nước.',
        safetyIntervalDays: waitDays,
        warning:
          waitDays > 0
            ? `Tuân thủ ngưng thuốc tối thiểu ${waitDays} ngày trước khi thu hoạch.`
            : null,
      },
    ],
    en: [
      {
        id: `${code}-bio`,
        treatmentType: 'BIOLOGICAL',
        title: 'Canopy sanitation & microbial bio-control',
        instructions: bioEn,
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
    ],
  };
}

export const FALLBACK_DISEASES: FallbackDiseaseRecord[] = [
  (() => {
    const tr = quickBioAndChem(
      'POTATO_LATE_BLIGHT',
      'Cắt bỏ lá gốc thối nhũn đem tiêu hủy xa ruộng, vun cao luống củ và phun nấm đối kháng Trichoderma.',
      'Remove water-soaked lower foliage away from plots, hill soil over tubers, and spray Trichoderma bio-agent.',
      'Mancozeb 80WP / Cymoxanil',
      7
    );
    return {
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
      viSymptoms:
        'Vết đốm xanh tái như luộc nước sôi ở chóp hoặc mép lá, chuyển nhanh sang nâu đen thối nhũn; sáng sớm ẩm có lớp tơ mốc trắng mịn ở mặt dưới lá.',
      viDescription:
        'Bùng phát rất mạnh khi trời âm u, sương mù lạnh ẩm (15–22°C). Nếu không chặn sớm bào tử sẽ trôi xuống đất gây thối củ hàng loạt.',
      treatments: tr.en,
      viTreatments: tr.vi,
    };
  })(),
  (() => {
    const tr = quickBioAndChem(
      'POTATO_EARLY_BLIGHT',
      'Ngắt bỏ lá già sát đất có đốm vòng đồng tâm, bón cân đối Kali giúp vách tế bào lá dày khỏe.',
      'Prune senescent lower leaves showing target-ring lesions and maintain balanced potassium nutrition.',
      'Azoxystrobin / Chlorothalonil',
      7
    );
    return {
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
      viSymptoms:
        'Đốm nâu đen có các vòng tròn đồng tâm như bia bắn trên các lá già phía dưới trước, quanh vết đốm có quầng vàng rõ.',
      viDescription:
        'Nấm tồn lưu trong tàn dư cây vụ trước, phát triển mạnh khi thời tiết nắng nóng xen kẽ mưa rào làm lá già suy yếu.',
      treatments: tr.en,
      viTreatments: tr.vi,
    };
  })(),
  (() => {
    const tr = quickBioAndChem(
      'TOMATO_LATE_BLIGHT',
      'Tỉa thông thoáng tán cà chua, làm mái che mưa nếu có thể và phun phòng bằng Bacillus subtilis.',
      'Prune dense tomato foliage for airflow, shelter from rain splash, and apply Bacillus subtilis.',
      'Mancozeb / Mandipropamid',
      5
    );
    return {
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
      viSymptoms:
        'Mảng lá úng nước xanh xám loang rộng thành vết nâu tím sẫm cả trên lá, cuống và thân; mặt dưới có lớp mốc trắng mỏng khi ẩm.',
      viDescription:
        'Lây lan cực nhanh trong những đợt mưa dầm lạnh ẩm, có thể làm cháy rụi dàn cà chua và thối nâu cứng quả xanh.',
      treatments: tr.en,
      viTreatments: tr.vi,
    };
  })(),
  (() => {
    const tr = quickBioAndChem(
      'TOMATO_EARLY_BLIGHT',
      'Tỉa lá chân dưới chùm quả đầu tiên, phủ rơm gốc chống bắn đất và phun nấm đối kháng.',
      'Remove lower leaves beneath the first fruit cluster, mulch soil beds, and apply microbial bio-fungicide.',
      'Azoxystrobin / Chlorothalonil',
      5
    );
    return {
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
      viSymptoms:
        'Vết nâu tròn có vân vòng đồng tâm xuất hiện từ lá già sát gốc lan dần lên trên, làm vàng rụng lá và rám nắng quả.',
      viDescription:
        'Phát sinh mạnh vào mùa nóng ẩm, sương đêm nhiều hoặc vườn tưới phun mưa làm nước bắn từ đất lên lá chân.',
      treatments: tr.en,
      viTreatments: tr.vi,
    };
  })(),
  (() => {
    const tr = quickBioAndChem(
      'TOMATO_SEPTORIA_LEAF_SPOT',
      'Thu gom sạch lá rụng dưới gốc, luân canh cây họ khác và phun chế phẩm sinh học sớm.',
      'Clear fallen leaf debris promptly, rotate out of solanaceous crops, and apply biological foliar spray.',
      'Copper hydroxide / Mancozeb',
      5
    );
    return {
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
      viSymptoms:
        'Nhiều đốm tròn nhỏ viền nâu sẫm, tâm màu xám trắng hơi lõm có chấm đen li ti như hạt tiêu ở giữa.',
      viDescription:
        'Lây lan từ tầng lá sát đất ngược lên ngọn nhờ nước mưa và nước tưới bắn tia trong điều kiện ấm ẩm 20–25°C.',
      treatments: tr.en,
      viTreatments: tr.vi,
    };
  })(),
  (() => {
    const tr = quickBioAndChem(
      'TOMATO_BACTERIAL_SPOT',
      'Không đi tỉa cành bấm ngọn khi lá còn ướt sương; sử dụng giống sạch bệnh và phun vi sinh Bacillus.',
      'Avoid pruning or handling wet plants; plant certified clean seed and apply Bacillus bio-bactericide.',
      'Kasugamycin / Copper oxychloride',
      7
    );
    return {
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
      viSymptoms:
        'Đốm nhỏ sũng nước màu nâu đen bóng nhờn có quầng vàng bao quanh, lâu ngày tâm vết bệnh khô rách làm xơ xác phiến lá.',
      viDescription:
        'Vi khuẩn lây lan cực nhanh qua mưa gió tạt và dụng cụ cắt tỉa khi trời nóng ẩm 24–30°C.',
      treatments: tr.en,
      viTreatments: tr.vi,
    };
  })(),
  (() => {
    const tr = quickBioAndChem(
      'TOMATO_YELLOW_VIRUS',
      'Nhổ bỏ sớm cây xoăn ngọn nặng cho vào bao kín; treo bẫy dính màu vàng và phun dầu khoáng thảo mộc diệt bọ phấn trắng.',
      'Rogue severely stunted plants in sealed bags; hang yellow sticky traps and spray botanical neem/mineral oil against whiteflies.',
      'Quản lý bọ phấn môi giới (Imidacloprid / Pymetrozine)',
      7
    );
    return {
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
      viSymptoms:
        'Lá ngọn nhỏ lại, congúp ngược lên như hình thìa, mép và gân lá vàng sáng, chồi ngọn chùn lại thành bụi và rụng hoa.',
      viDescription:
        'Do virus truyền qua bọ phấn trắng (Bemisia tabaci) trong mùa nắng nóng khô; thuốc trừ nấm không diệt được virus nên trọng tâm là chặn bọ phấn.',
      treatments: tr.en,
      viTreatments: tr.vi,
    };
  })(),
  (() => {
    const tr = quickBioAndChem(
      'RICE_BLAST',
      'Ngưng ngay bón phân đạm, giữ mực nước ruộng 3–5cm và phun kháng sinh sinh học Kasugamycin.',
      'Stop nitrogen top-dressing immediately, hold 3–5cm paddy water, and spray Kasugamycin.',
      'Tricyclazole 75WP / Isoprothiolane',
      14
    );
    return {
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
      viSymptoms:
        'Vết bệnh hình thoi (mắt én) nhọn hai đầu, tâm xám trắng, viền nâu đỏ trên phiến lá; nếu ăn lên cổ bông làm gãy cổ gié lép trắng.',
      viDescription:
        'Phát triển mạnh khi đêm lạnh nhiều sương mù, ngày âm u, ruộng gieo dày và bón thừa phân đạm.',
      treatments: tr.en,
      viTreatments: tr.vi,
    };
  })(),
  (() => {
    const tr = quickBioAndChem(
      'CORN_COMMON_RUST',
      'Chọn giống ngô kháng gỉ sắt, gieo thưa hợp lý để gió lùa khô sương trên phiến lá.',
      'Select rust-tolerant hybrids and maintain row spacing for rapid morning canopy drying.',
      'Azoxystrobin / Propiconazole',
      14
    );
    return {
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
      viSymptoms:
        'Các ổ nổi nhỏ màu nâu quế đến nâu đỏ như bột gạch vỡ ra trên cả mặt trên và mặt dưới phiến lá ngô.',
      viDescription:
        'Bào tử bay theo gió, phát triển thuận lợi khi thời tiết mát mẻ 16–23°C và ẩm độ không khí cao.',
      treatments: tr.en,
      viTreatments: tr.vi,
    };
  })(),
  (() => {
    const tr = quickBioAndChem(
      'CORN_GRAY_LEAF_SPOT',
      'Cày vùi rơm rạ thân ngô sau thu hoạch và luân canh với đậu nành để cắt nguồn nấm lưu tồn.',
      'Incorporate corn residue after harvest and rotate with soybean to break inoculum carryover.',
      'Pyraclostrobin / Propiconazole',
      14
    );
    return {
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
      viSymptoms:
        'Vết bệnh hẹp dài hình chữ nhật màu vàng nâu đến xám tro, bị chặn thẳng tắp dọc theo hai đường gân lá song song.',
      viDescription:
        'Gây khô cháy bộ lá rất mạnh giai đoạn trỗ cờ phun râu khi trời nóng ẩm và ruộng trồng ngô liên tiếp nhiều vụ.',
      treatments: tr.en,
      viTreatments: tr.vi,
    };
  })(),
  (() => {
    const tr = quickBioAndChem(
      'CORN_LEAF_BLIGHT',
      'Thu dọn tàn dư lá bệnh cuối vụ, bón cân đối N-P-K và phun phòng chế phẩm sinh học khi lá dưới chớm bệnh.',
      'Manage infected crop debris, balance N-P-K fertilization, and apply early bio-fungicide.',
      'Mancozeb / Azoxystrobin',
      14
    );
    return {
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
      viSymptoms:
        'Vết cháy lớn hình thoi dài như điếu xì gà (dài 3–15cm) màu xanh xám chuyển nâu vàng chạy dọc phiến lá.',
      viDescription:
        'Phát triển mạnh trong điều kiện thời tiết mát ẩm 18–27°C, sương ướt lá kéo dài trên 6 giờ.',
      treatments: tr.en,
      viTreatments: tr.vi,
    };
  })(),
  (() => {
    const tr = quickBioAndChem(
      'BELL_PEPPER_LEAF_SPOT',
      'Tưới nhỏ giọt dưới gốc thay vì phun mưa, ngắt bỏ lá đốm gốc và phun Bacillus subtilis.',
      'Switch from overhead sprinklers to drip irrigation, remove infected lower leaves, and spray Bacillus.',
      'Copper hydroxide / Kasugamycin',
      5
    );
    return {
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
      viSymptoms:
        'Đốm tròn nhỏ sũng nước chuyển màu nâu sẫm có viền vàng sáng xung quanh, làm lá ớt vàng và rụng hàng loạt từ gốc lên.',
      viDescription:
        'Bệnh vi khuẩn rất phổ biến mùa mưa nóng ẩm 25–30°C, dễ làm trụi lá khiến quả ớt chuông bị nám nắng.',
      treatments: tr.en,
      viTreatments: tr.vi,
    };
  })(),
  (() => {
    const tr = quickBioAndChem(
      'APPLE_SCAB',
      'Thu gom tiêu hủy lá rụng mùa đông dưới gốc để diệt ổ bào tử túi và tỉa thoáng cành bên trong tán.',
      'Rake and destroy fallen overwintering leaves and prune interior branches for rapid drying.',
      'Captan / Mancozeb',
      14
    );
    return {
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
      viSymptoms:
        'Đốm mượt như nhung màu xanh ô-liu chuyển nâu đen trên mặt lá, làm phiến lá nhăn nhúm, cong vênh và rụng sớm.',
      viDescription:
        'Nấm phát sinh mạnh vào đầu vụ xuân mưa phùn mát ẩm khi cây vừa bật lộc non và ra hoa.',
      treatments: tr.en,
      viTreatments: tr.vi,
    };
  })(),
  (() => {
    const tr = quickBioAndChem(
      'APPLE_RUST',
      'Cắt tỉa các u gỉ trên cây tùng bách trồng gần vườn (ký chủ phụ) và phun phòng sinh học lúc lộc non.',
      'Prune rust galls on nearby juniper/cedar hosts and protect young leaves early.',
      'Myclobutanil / Sulfur',
      14
    );
    return {
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
      viSymptoms:
        'Đốm tròn màu vàng cam rực rỡ trên mặt trên lá táo, giữa tâm có chấm đen nhỏ và mặt dưới mọc tua ống nhỏ.',
      viDescription:
        'Nấm gỉ sắt luân chuyển bào tử theo gió mưa mùa xuân từ nhóm cây tùng bách sang lá táo non.',
      treatments: tr.en,
      viTreatments: tr.vi,
    };
  })(),
  (() => {
    const tr = quickBioAndChem(
      'GRAPE_BLACK_ROT',
      'Cắt sạch các chùm nho khô đen treo trên giàn từ vụ trước, tỉa lá che khuất chùm hoa để nắng chiếu thoáng.',
      'Sanitize mummified berries from trellises and open the fruiting zone canopy to sunlight.',
      'Mancozeb / Myclobutanil',
      14
    );
    return {
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
      viSymptoms:
        'Đốm tròn màu nâu đỏ có viền đen sẫm rõ rệt trên lá nho kèm các chấm đen nhỏ; quả nho bị thối nhăn nheo khô đen trên chùm.',
      viDescription:
        'Gây hại nặng trên giàn nho rậm rạp trong mùa mưa ấm (21–29°C) từ lúc tượng chùm đến khi quả lớn.',
      treatments: tr.en,
      viTreatments: tr.vi,
    };
  })(),
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
  const kw = params?.keyword?.trim().toLowerCase();

  return FALLBACK_DISEASES.filter((d) => {
    if (plantNorm && !d.plantCodes.includes(plantNorm)) {
      return false;
    }
    if (kw) {
      const viDisease = VI_DISEASE_NAMES[d.diseaseCode] ?? '';
      const viPlants = d.plantCodes.map((code) => VI_PLANT_NAMES[code] ?? '').join(' ');
      const hay = `${d.diseaseCode} ${d.commonName} ${d.scientificName ?? ''} ${viDisease} ${viPlants} ${d.viSymptoms} ${d.plantCodes.join(
        ' '
      )}`.toLowerCase();
      if (!hay.includes(kw)) {
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
