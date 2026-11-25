import type {
  DiagnosisAnalyzeResponse,
  DiseaseDetailDto,
  SampleSpecimen,
  TreatmentDto,
} from '@/types/pdds';
import type { Locale } from './dictionary';

export interface DiseaseLocalizationEntry {
  symptoms: string;
  description: string;
  treatments: TreatmentDto[];
}

export interface SampleSpecimenLocalization {
  shortLabel: string;
  treatments: TreatmentDto[];
}

export const VI_PLANT_DESCRIPTIONS: Record<string, string> = {
  POTATO: 'Cây lấy củ vùng cao nguyên và vụ đông, nhạy cảm với bệnh mốc sương và đốm vòng.',
  TOMATO: 'Cây rau ăn quả phổ biến, dễ nhiễm nấm lá và virus xoăn vàng lá.',
  RICE: 'Cây lương thực chủ lực, cần theo dõi sát bệnh đạo ôn lá và cổ bông.',
  CORN: 'Cây ngô lấy hạt và sinh khối, thường gặp gỉ sắt, đốm xám và cháy lá lớn.',
  BELL_PEPPER: 'Ớt ngọt nhà màng và ngoài đồng, mẫn cảm với đốm lá vi khuẩn mùa mưa.',
  APPLE: 'Cây ăn quả ôn đới và tiểu khí hậu mát, thường gặp bệnh ghẻ sẹo và gỉ sắt.',
  GRAPE: 'Cây nho ăn tươi, dễ phát sinh bệnh thối đen trên lá và chùm quả khi ẩm.',
  PEACH: 'Cây đào ăn quả vùng cao, hệ thống nhận diện tình trạng lá khỏe và đốm vi khuẩn.',
  SOYBEAN: 'Cây đậu nành giàu đạm luân canh cải tạo đất.',
};

function buildViBioAndChemTreatments(
  code: string,
  bioInstructions: string,
  chemIngredient: string,
  waitDays: number
): TreatmentDto[] {
  return [
    {
      id: `${code}-bio`,
      treatmentType: 'BIOLOGICAL',
      title: 'Vệ sinh tán lá & phun vi sinh đối kháng',
      instructions: bioInstructions,
      activeIngredients: 'Trichoderma spp. / Bacillus subtilis',
      dosageRecommendation: '30–40g (hoặc ml) pha bình 16 lít nước, phun sáng sớm.',
      safetyIntervalDays: 0,
      warning: 'Ưu tiên thực hiện ngay khi bệnh mới chớm 1–2 đốm nhỏ trên lá.',
    },
    {
      id: `${code}-chem`,
      treatmentType: 'CHEMICAL',
      title: `Phun đặc trị ${chemIngredient} khi bệnh lan rộng`,
      instructions:
        'Chỉ phun khi thời tiết ẩm ướt kéo dài làm vết bệnh lan nhanh trên tán lá. Phun ướt đều hai mặt lá lúc trời tạnh ráo.',
      activeIngredients: chemIngredient,
      dosageRecommendation: 'Pha đúng liều khuyến cáo trên nhãn cho bình 16 lít nước.',
      safetyIntervalDays: waitDays,
      warning:
        waitDays > 0
          ? `Tuân thủ ngưng thuốc tối thiểu ${waitDays} ngày trước khi thu hoạch.`
          : null,
    },
  ];
}

export const VI_DISEASE_LOCALIZATIONS: Record<string, DiseaseLocalizationEntry> = {
  POTATO_LATE_BLIGHT: {
    symptoms:
      'Vết đốm xanh tái như luộc nước sôi ở chóp hoặc mép lá, chuyển nhanh sang nâu đen thối nhũn; sáng sớm ẩm có lớp tơ mốc trắng mịn ở mặt dưới lá.',
    description:
      'Bùng phát rất mạnh khi trời âm u, sương mù lạnh ẩm (15–22°C). Nếu không chặn sớm bào tử sẽ trôi xuống đất gây thối củ hàng loạt.',
    treatments: buildViBioAndChemTreatments(
      'POTATO_LATE_BLIGHT',
      'Cắt bỏ lá gốc thối nhũn đem tiêu hủy xa ruộng, vun cao luống củ và phun nấm đối kháng Trichoderma.',
      'Mancozeb 80WP / Cymoxanil',
      7
    ),
  },
  POTATO_EARLY_BLIGHT: {
    symptoms:
      'Đốm nâu đen có các vòng tròn đồng tâm như bia bắn trên các lá già phía dưới trước, quanh vết đốm có quầng vàng rõ.',
    description:
      'Nấm tồn lưu trong tàn dư cây vụ trước, phát triển mạnh khi thời tiết nắng nóng xen kẽ mưa rào làm lá già suy yếu.',
    treatments: buildViBioAndChemTreatments(
      'POTATO_EARLY_BLIGHT',
      'Ngắt bỏ lá già sát đất có đốm vòng đồng tâm, bón cân đối Kali giúp vách tế bào lá dày khỏe.',
      'Azoxystrobin / Chlorothalonil',
      7
    ),
  },
  TOMATO_LATE_BLIGHT: {
    symptoms:
      'Mảng lá úng nước xanh xám loang rộng thành vết nâu tím sẫm cả trên lá, cuống và thân; mặt dưới có lớp mốc trắng mỏng khi ẩm.',
    description:
      'Lây lan cực nhanh trong những đợt mưa dầm lạnh ẩm, có thể làm cháy rụi dàn cà chua và thối nâu cứng quả xanh.',
    treatments: buildViBioAndChemTreatments(
      'TOMATO_LATE_BLIGHT',
      'Tỉa thông thoáng tán cà chua, làm mái che mưa nếu có thể và phun phòng bằng Bacillus subtilis.',
      'Mancozeb / Mandipropamid',
      5
    ),
  },
  TOMATO_EARLY_BLIGHT: {
    symptoms:
      'Vết nâu tròn có vân vòng đồng tâm xuất hiện từ lá già sát gốc lan dần lên trên, làm vàng rụng lá và rám nắng quả.',
    description:
      'Phát sinh mạnh vào mùa nóng ẩm, sương đêm nhiều hoặc vườn tưới phun mưa làm nước bắn từ đất lên lá chân.',
    treatments: buildViBioAndChemTreatments(
      'TOMATO_EARLY_BLIGHT',
      'Tỉa lá chân dưới chùm quả đầu tiên, phủ rơm gốc chống bắn đất và phun nấm đối kháng.',
      'Azoxystrobin / Chlorothalonil',
      5
    ),
  },
  TOMATO_SEPTORIA_LEAF_SPOT: {
    symptoms:
      'Nhiều đốm tròn nhỏ viền nâu sẫm, tâm màu xám trắng hơi lõm có chấm đen li ti như hạt tiêu ở giữa.',
    description:
      'Lây lan từ tầng lá sát đất ngược lên ngọn nhờ nước mưa và nước tưới bắn tia trong điều kiện ấm ẩm 20–25°C.',
    treatments: buildViBioAndChemTreatments(
      'TOMATO_SEPTORIA_LEAF_SPOT',
      'Thu gom sạch lá rụng dưới gốc, luân canh cây họ khác và phun chế phẩm sinh học sớm.',
      'Copper hydroxide / Mancozeb',
      5
    ),
  },
  TOMATO_BACTERIAL_SPOT: {
    symptoms:
      'Đốm nhỏ sũng nước màu nâu đen bóng nhờn có quầng vàng bao quanh, lâu ngày tâm vết bệnh khô rách làm xơ xác phiến lá.',
    description:
      'Vi khuẩn lây lan cực nhanh qua mưa gió tạt và dụng cụ cắt tỉa khi trời nóng ẩm 24–30°C.',
    treatments: buildViBioAndChemTreatments(
      'TOMATO_BACTERIAL_SPOT',
      'Không đi tỉa cành bấm ngọn khi lá còn ướt sương; sử dụng giống sạch bệnh và phun vi sinh Bacillus.',
      'Kasugamycin / Copper oxychloride',
      7
    ),
  },
  TOMATO_YELLOW_VIRUS: {
    symptoms:
      'Lá ngọn nhỏ lại, cong úp ngược lên như hình thìa, mép và gân lá vàng sáng, chồi ngọn chùn lại thành bụi và rụng hoa.',
    description:
      'Do virus truyền qua bọ phấn trắng (Bemisia tabaci) trong mùa nắng nóng khô; thuốc trừ nấm không diệt được virus nên trọng tâm là chặn bọ phấn.',
    treatments: buildViBioAndChemTreatments(
      'TOMATO_YELLOW_VIRUS',
      'Nhổ bỏ sớm cây xoăn ngọn nặng cho vào bao kín; treo bẫy dính màu vàng và phun dầu khoáng thảo mộc diệt bọ phấn trắng.',
      'Quản lý bọ phấn môi giới (Imidacloprid / Pymetrozine)',
      7
    ),
  },
  RICE_BLAST: {
    symptoms:
      'Vết bệnh hình thoi (mắt én) nhọn hai đầu, tâm xám trắng, viền nâu đỏ trên phiến lá; nếu ăn lên cổ bông làm gãy cổ gié lép trắng.',
    description:
      'Phát triển mạnh khi đêm lạnh nhiều sương mù, ngày âm u, ruộng gieo dày và bón thừa phân đạm.',
    treatments: buildViBioAndChemTreatments(
      'RICE_BLAST',
      'Ngưng ngay bón phân đạm, giữ mực nước ruộng 3–5cm và phun kháng sinh sinh học Kasugamycin.',
      'Tricyclazole 75WP / Isoprothiolane',
      14
    ),
  },
  CORN_COMMON_RUST: {
    symptoms:
      'Các ổ nổi nhỏ màu nâu quế đến nâu đỏ như bột gạch vỡ ra trên cả mặt trên và mặt dưới phiến lá ngô.',
    description:
      'Bào tử bay theo gió, phát triển thuận lợi khi thời tiết mát mẻ 16–23°C và ẩm độ không khí cao.',
    treatments: buildViBioAndChemTreatments(
      'CORN_COMMON_RUST',
      'Chọn giống ngô kháng gỉ sắt, gieo thưa hợp lý để gió lùa khô sương trên phiến lá.',
      'Azoxystrobin / Propiconazole',
      14
    ),
  },
  CORN_GRAY_LEAF_SPOT: {
    symptoms:
      'Vết bệnh hẹp dài hình chữ nhật màu vàng nâu đến xám tro, bị chặn thẳng tắp dọc theo hai đường gân lá song song.',
    description:
      'Gây khô cháy bộ lá rất mạnh giai đoạn trỗ cờ phun râu khi trời nóng ẩm và ruộng trồng ngô liên tiếp nhiều vụ.',
    treatments: buildViBioAndChemTreatments(
      'CORN_GRAY_LEAF_SPOT',
      'Cày vùi rơm rạ thân ngô sau thu hoạch và luân canh với đậu nành để cắt nguồn nấm lưu tồn.',
      'Pyraclostrobin / Propiconazole',
      14
    ),
  },
  CORN_LEAF_BLIGHT: {
    symptoms:
      'Vết cháy lớn hình thoi dài như điếu xì gà (dài 3–15cm) màu xanh xám chuyển nâu vàng chạy dọc phiến lá.',
    description:
      'Phát triển mạnh trong điều kiện thời tiết mát ẩm 18–27°C, sương ướt lá kéo dài trên 6 giờ.',
    treatments: buildViBioAndChemTreatments(
      'CORN_LEAF_BLIGHT',
      'Thu dọn tàn dư lá bệnh cuối vụ, bón cân đối N-P-K và phun phòng chế phẩm sinh học khi lá dưới chớm bệnh.',
      'Mancozeb / Azoxystrobin',
      14
    ),
  },
  BELL_PEPPER_LEAF_SPOT: {
    symptoms:
      'Đốm tròn nhỏ sũng nước chuyển màu nâu sẫm có viền vàng sáng xung quanh, làm lá ớt vàng và rụng hàng loạt từ gốc lên.',
    description:
      'Bệnh vi khuẩn rất phổ biến mùa mưa nóng ẩm 25–30°C, dễ làm trụi lá khiến quả ớt chuông bị nám nắng.',
    treatments: buildViBioAndChemTreatments(
      'BELL_PEPPER_LEAF_SPOT',
      'Tưới nhỏ giọt dưới gốc thay vì phun mưa, ngắt bỏ lá đốm gốc và phun Bacillus subtilis.',
      'Copper hydroxide / Kasugamycin',
      5
    ),
  },
  APPLE_SCAB: {
    symptoms:
      'Đốm mượt như nhung màu xanh ô-liu chuyển nâu đen trên mặt lá, làm phiến lá nhăn nhúm, cong vênh và rụng sớm.',
    description:
      'Nấm phát sinh mạnh vào đầu vụ xuân mưa phùn mát ẩm khi cây vừa bật lộc non và ra hoa.',
    treatments: buildViBioAndChemTreatments(
      'APPLE_SCAB',
      'Thu gom tiêu hủy lá rụng mùa đông dưới gốc để diệt ổ bào tử túi và tỉa thoáng cành bên trong tán.',
      'Captan / Mancozeb',
      14
    ),
  },
  APPLE_RUST: {
    symptoms:
      'Đốm tròn màu vàng cam rực rỡ trên mặt trên lá táo, giữa tâm có chấm đen nhỏ và mặt dưới mọc tua ống nhỏ.',
    description:
      'Nấm gỉ sắt luân chuyển bào tử theo gió mưa mùa xuân từ nhóm cây tùng bách sang lá táo non.',
    treatments: buildViBioAndChemTreatments(
      'APPLE_RUST',
      'Cắt tỉa các u gỉ trên cây tùng bách trồng gần vườn (ký chủ phụ) và phun phòng sinh học lúc lộc non.',
      'Myclobutanil / Sulfur',
      14
    ),
  },
  GRAPE_BLACK_ROT: {
    symptoms:
      'Đốm tròn màu nâu đỏ có viền đen sẫm rõ rệt trên lá nho kèm các chấm đen nhỏ; quả nho bị thối nhăn nheo khô đen trên chùm.',
    description:
      'Gây hại nặng trên giàn nho rậm rạp trong mùa mưa ấm (21–29°C) từ lúc tượng chùm đến khi quả lớn.',
    treatments: buildViBioAndChemTreatments(
      'GRAPE_BLACK_ROT',
      'Cắt sạch các chùm nho khô đen treo trên giàn từ vụ trước, tỉa lá che khuất chùm hoa để nắng chiếu thoáng.',
      'Mancozeb / Myclobutanil',
      14
    ),
  },
};

export const VI_SAMPLE_SPECIMENS: Record<string, SampleSpecimenLocalization> = {
  'potato-late-blight': {
    shortLabel: 'Khoai tây mốc sương',
    treatments: [
      {
        id: 'tr-potato-bio',
        treatmentType: 'BIOLOGICAL',
        title: 'Xử lý sinh học bằng nấm đối kháng Trichoderma',
        instructions:
          'Cắt bỏ ngay các lá gốc bị thối sũng nước đem tiêu hủy xa ruộng. Phun chế phẩm nấm đối kháng Trichoderma harzianum hoặc Bacillus subtilis vào buổi sáng sớm khi lá vừa ráo sương.',
        activeIngredients: 'Trichoderma harzianum / Bacillus subtilis',
        dosageRecommendation: '30–40g chế phẩm pha cho bình 16 lít nước, phun ướt đều mặt\u00A0lá.',
        safetyIntervalDays: 0,
        warning: 'Không pha chung chế phẩm sinh học với thuốc trừ nấm gốc đồng hoặc hóa học.',
      },
      {
        id: 'tr-potato-chem',
        treatmentType: 'CHEMICAL',
        title: 'Phun đặc trị Mancozeb khi trời mưa ẩm kéo dài',
        instructions:
          'Khi vết mốc sương lan nhanh trong tiết trời lạnh ẩm, phun thuốc đặc trị ướt đều cả mặt trên và mặt dưới lá để khóa ổ bào tử.',
        activeIngredients: 'Mancozeb 80WP hoặc Cymoxanil + Mancozeb',
        dosageRecommendation: '40g cho bình 16 lít nước; phun nhắc lại sau 5–7 ngày nếu sương mù dày.',
        safetyIntervalDays: 7,
        warning: 'Mang găng tay, khẩu trang khi pha phun; ngưng thuốc đủ 7 ngày trước khi thu hoạch củ.',
      },
      {
        id: 'tr-potato-cult',
        treatmentType: 'CULTURAL',
        title: 'Vun cao luống & tránh tưới phun mưa chiều tối',
        instructions:
          'Vun đất kín gốc củ để bào tử trên lá không theo nước mưa trôi xuống gây thối củ. Tuyệt đối không tưới phun mưa lên lá vào chiều muộn.',
        activeIngredients: null,
        dosageRecommendation: 'Kiểm tra thoát nước rãnh luống sau mỗi cơn mưa.',
        safetyIntervalDays: 0,
        warning: null,
      },
    ],
  },
  'tomato-bacterial-spot': {
    shortLabel: 'Cà chua đốm lá',
    treatments: [
      {
        id: 'tr-tomato-bio',
        treatmentType: 'BIOLOGICAL',
        title: 'Tránh tỉa lá khi ướt & phun vi sinh Bacillus subtilis',
        instructions:
          'Không đi tỉa cành bấm ngọn khi lá cà chua còn ướt sương để tránh lây lan vi khuẩn. Ngắt bỏ lá chân đốm nặng, chuyển sang tưới gốc và phun Bacillus subtilis.',
        activeIngredients: 'Bacillus subtilis (chủng QST 713)',
        dosageRecommendation: '35ml chế phẩm sinh học cho bình 16 lít nước lúc trời ráo.',
        safetyIntervalDays: 0,
        warning: 'Vệ sinh kéo cắt giữa các luống để không làm lây nhiễm chéo.',
      },
      {
        id: 'tr-tomato-chem',
        treatmentType: 'CHEMICAL',
        title: 'Phun đặc trị Kasugamycin hoặc Copper oxychloride',
        instructions:
          'Khi mưa gió ấm làm vết đốm vi khuẩn loang nhanh kèm đốm vòng lên tán quả, phun đều hai mặt lá vào buổi sáng mát.',
        activeIngredients: 'Kasugamycin 2SL / Copper oxychloride',
        dosageRecommendation: '25–30g cho bình 16 lít nước, không phun giữa trưa nắng gắt.',
        safetyIntervalDays: 7,
        warning: 'Tuân thủ ngưng thuốc tối thiểu 7 ngày trước đợt hái quả chín.',
      },
    ],
  },
  'apple-rust': {
    shortLabel: 'Táo gỉ sắt',
    treatments: [
      {
        id: 'tr-apple-cult',
        treatmentType: 'CULTURAL',
        title: 'Tỉa sạch u gỉ ký chủ phụ & làm thoáng tán vườn',
        instructions:
          'Kiểm tra và cắt bỏ các u gỉ trên cây họ tùng bách trồng gần vườn từ cuối đông trước khi gió mưa mùa xuân phát tán bào tử sang lá táo.',
        activeIngredients: null,
        dosageRecommendation: 'Tỉa cành vượt trong tán để nắng sớm hong khô mặt lá nhanh.',
        safetyIntervalDays: 0,
        warning: 'Thu gom tiêu hủy các lá chớm u gỉ nặng trước khi bào tử chín rộ.',
      },
      {
        id: 'tr-apple-bio',
        treatmentType: 'BIOLOGICAL',
        title: 'Phun phòng lưu huỳnh sinh học / Bacillus giai đoạn lộc non',
        instructions:
          'Phun phòng ngừa bằng chế phẩm lưu huỳnh hòa nước hoặc Bacillus subtilis ngay từ lúc cây bật nụ hoa đến rụng cánh khi trời ẩm.',
        activeIngredients: 'Lưu huỳnh hòa nước / Bacillus subtilis',
        dosageRecommendation: '30–40g cho bình 16 lít nước, phun đều hai mặt lá lúc trời mát.',
        safetyIntervalDays: 0,
        warning: 'Không phun lưu huỳnh chung hoặc sát ngày với dầu khoáng.',
      },
      {
        id: 'tr-apple-chem',
        treatmentType: 'CHEMICAL',
        title: 'Đặc trị Myclobutanil khi đốm vàng cam lan rộng',
        instructions:
          'Khi mưa phùn xuân kéo dài làm nhiều đốm gỉ sắt vàng cam bùng phát trên lá non, sử dụng thuốc nội hấp chuyên biệt theo đúng liều nhãn.',
        activeIngredients: 'Myclobutanil / Propiconazole',
        dosageRecommendation: '15–20ml cho bình 16 lít nước, cách nhau 10–14 ngày nếu mưa tiếp.',
        safetyIntervalDays: 14,
        warning: 'Ngưng phun tối thiểu 14 ngày trước khi thu hoạch quả táo.',
      },
    ],
  },
};

export function getDiseaseLocalizationSearchText(diseaseCode: string): string {
  return VI_DISEASE_LOCALIZATIONS[diseaseCode.toUpperCase()]?.symptoms ?? '';
}

export function localizeSampleResponse(
  sample: SampleSpecimen,
  locale: Locale
): DiagnosisAnalyzeResponse {
  if (locale === 'vi') {
    const localized = VI_SAMPLE_SPECIMENS[sample.key];
    if (localized) {
      return {
        ...sample.response,
        recommendedTreatments: localized.treatments,
      };
    }
  }
  return sample.response;
}

export function getDiseaseDetailOverride(
  diseaseCode: string,
  locale: Locale,
  englishFallback?: DiseaseDetailDto
): {
  symptoms: string;
  description: string;
  treatments: TreatmentDto[];
} {
  const norm = diseaseCode.trim().toUpperCase();
  const viEntry = locale === 'vi' ? VI_DISEASE_LOCALIZATIONS[norm] : undefined;

  return {
    symptoms: viEntry?.symptoms ?? englishFallback?.symptoms ?? '',
    description: viEntry?.description ?? englishFallback?.description ?? '',
    treatments: viEntry?.treatments.length
      ? viEntry.treatments
      : englishFallback?.treatments ?? [],
  };
}
