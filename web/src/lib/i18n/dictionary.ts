export type Locale = 'vi' | 'en';

export interface UiCopy {
  brandSubtitle: string;
  navWorkbench: string;
  navHowItWorks: string;
  navAlmanac: string;
  langToggleAriaToEn: string;
  langToggleAriaToVi: string;
  appButton: string;
  appSheetTitle: string;
  appSheetBody: string;
  appFeatureHistory: string;
  appFeatureExpert: string;
  appFeatureGps: string;
  appSheetClose: string;
  heroHeading: string;
  heroLead: string;
  cropHintLabel: string;
  cropHintAll: string;
  uploadCta: string;
  uploadDropHint: string;
  quotaLabel: (remaining: number, limit: number) => string;
  sampleGroupLabel: string;
  sampleReferenceBadge: string;
  toggleBoxesHide: string;
  toggleBoxesShow: string;
  confidenceLabel: (pct: number, count: number) => string;
  stageNeutralLoadingHeading: string;
  stageNeutralErrorHeading: string;
  loadingStageTitle: string;
  loadingStageBody: string;
  error400Title: string;
  error422Title: string;
  error422Body: string;
  error429Title: string;
  error429Body: (minutes: number) => string;
  errorGenericBody: string;
  retryUploadButton: string;
  prescriptionHeading: string;
  colInstructions: string;
  colDosageAndCoInfection: string;
  colHarvestSafety: string;
  activeIngredientLabel: string;
  dosageLabel: string;
  noChemicalNote: string;
  coInfectionPrefix: string;
  harvestWaitDays: (days: number) => string;
  harvestSafeZero: string;
  howHeading: string;
  howLead: string;
  howStep1Title: string;
  howStep1Body: string;
  howStep2Title: string;
  howStep2Body: string;
  howStep3Title: string;
  howStep3Body: string;
  almanacHeading: string;
  almanacLead: string;
  almanacAllCrops: string;
  almanacSearchPlaceholder: string;
  almanacOfflineNote: string;
  almanacEmpty: string;
  almanacHealthyOnlyCrop: string;
  sheetSymptomsHeading: string;
  sheetConditionsHeading: string;
  sheetTreatmentsHeading: string;
  footerSummary: string;
  footerSafetyNote: string;
  footerCopyright: string;
}

export const UI_DICTIONARY: Record<Locale, UiCopy> = {
  vi: {
    brandSubtitle: 'Hệ Thống Nhận Diện Bệnh Cây Trồng',
    navWorkbench: 'Soi Lá Bệnh',
    navHowItWorks: 'Cách Hoạt Động',
    navAlmanac: '9 Cây & 15 Bệnh',
    langToggleAriaToEn: 'Chuyển sang Tiếng Anh (EN)',
    langToggleAriaToVi: 'Chuyển sang Tiếng Việt (VN)',
    appButton: 'Ứng Dụng',
    appSheetTitle: 'Ứng Dụng Di Động EIU PDDS Dành Cho Nhà Nông',
    appSheetBody:
      'Trang web này cho phép bà con soi nhanh lá bệnh ngay trên trình duyệt mà không cần tài khoản. Khi cần đi thăm đồng thường xuyên, ứng dụng di động EIU PDDS mở rộng thêm 3 tiện ích chuyên sâu:',
    appFeatureHistory: 'Lưu nhật ký đồng ruộng kèm ảnh khoanh vùng vết bệnh qua từng đợt phun.',
    appFeatureExpert: 'Gửi phản hồi để chuyên gia nông nghiệp kiểm chứng lại các ca bệnh khó.',
    appFeatureGps: 'Ghi nhận vị trí thửa ruộng tự động giúp theo dõi ổ bệnh theo mùa vụ.',
    appSheetClose: 'Thu gọn',
    heroHeading: 'Soi Đốm Lá — Giữ Trọn Vụ Mùa',
    heroLead:
      'Chụp hoặc tải lên một tấm ảnh mặt lá có đốm vàng, cháy mép hay mốc trắng. Hệ thống khoanh vùng ngay vết bệnh trên lá và hướng dẫn cách xử lý an toàn, chữ to dễ đọc, không cần đăng nhập.',
    cropHintLabel: 'Loại cây đang trồng (không bắt buộc):',
    cropHintAll: 'Tự động nhận diện từ lá',
    uploadCta: 'Chụp / Tải Ảnh Lá Lên',
    uploadDropHint: 'Hoặc kéo thả ảnh lá cây vào đây (JPEG, PNG, WebP — tối đa\u00A015MB)',
    quotaLabel: (remaining, limit) => `Còn ${remaining}/${limit} lượt quét ảnh thật miễn phí trong giờ này`,
    sampleGroupLabel: 'Chưa có sẵn ảnh? Bấm xem thử lá mẫu:',
    sampleReferenceBadge: 'Mẫu đối chứng có sẵn',
    toggleBoxesHide: 'Tắt khung đốm bệnh',
    toggleBoxesShow: 'Bật khung đốm bệnh',
    confidenceLabel: (pct, count) => `Độ chắc chắn ${pct}% · Khoanh ${count} vùng trên lá`,
    stageNeutralLoadingHeading: 'Bàn Soi Mẫu Lá Đang Phân Tích',
    stageNeutralErrorHeading: 'Cần Kiểm Tra Lại Ảnh Lá Tải Lên',
    loadingStageTitle: 'Đang soi mặt lá & đối chiếu phác đồ nông học...',
    loadingStageBody: 'Bà con chờ khoảng 2–4 giây để máy khoanh vùng đốm bệnh và tra ngày cách ly.',
    error400Title: 'Tệp ảnh tải lên chưa đúng định dạng hoặc quá lớn',
    error422Title: 'Chưa nhìn rõ lá cây hoặc vết bệnh trong ảnh',
    error422Body:
      'Bà con hãy đặt chiếc lá lên nền phẳng hoặc giữ tay thật vững, đưa camera lại gần mặt lá dưới chỗ đủ ánh sáng tự nhiên rồi chụp lại nhé.',
    error429Title: 'Đã dùng hết 3 lượt quét ảnh thật miễn phí trong giờ này',
    error429Body: (minutes) =>
      `Để tránh quá tải máy chủ dùng chung, khách chưa đăng nhập được gửi tối đa 3 ảnh mỗi giờ (mở lại sau khoảng ${minutes} phút). Trong lúc chờ, bà con vẫn bấm xem thoải mái 3 mẫu lá đối chứng bên dưới.`,
    errorGenericBody:
      'Không kết nối được tới máy chủ phân tích lúc này. Bà con hãy kiểm tra mạng hoặc bấm chọn 3 mẫu lá đối chứng để xem thử giao diện kết quả.',
    retryUploadButton: 'Chọn ảnh lá khác',
    prescriptionHeading: 'Hướng Xử Lý & Thời Gian Cách Ly An Toàn',
    colInstructions: 'Các bước thực hiện ngoài vườn',
    colDosageAndCoInfection: 'Hoạt chất & Liều pha khuyến cáo',
    colHarvestSafety: 'An toàn trước thu hoạch',
    activeIngredientLabel: 'Hoạt chất / Chế phẩm:',
    dosageLabel: 'Liều lượng tham khảo:',
    noChemicalNote: 'Biện pháp sinh học / canh tác tự nhiên — không để lại dư lượng hóa chất.',
    coInfectionPrefix: 'Dấu hiệu đi kèm trên cùng lá:',
    harvestWaitDays: (days) => `Ngưng phun tối thiểu ${days} ngày trước khi thu hoạch.`,
    harvestSafeZero: '0 ngày · An toàn thu hoạch bình thường theo lịch canh tác.',
    howHeading: 'Ba Bước Khám Lá Đơn Giản Ngoài Vườn',
    howLead:
      'Thiết kế tối giản thao tác để bất kỳ người trồng cây nào cũng kiểm tra được ngay ngoài hiên nhà hay bờ ruộng.',
    howStep1Title: 'Chụp Rõ Một Mặt Lá Đang Nghi Bệnh',
    howStep1Body:
      'Chọn chiếc lá có vết đốm đặc trưng nhất, hướng ra chỗ sáng tự nhiên để thấy rõ màu vết bệnh và gân\u00A0lá.',
    howStep2Title: 'Đối Chiếu Khung Vết Bệnh Bằng\u00A0Mắt',
    howStep2Body:
      'Bật hoặc tắt khung vàng trên ảnh để tự mắt kiểm tra xem máy có khoanh đúng vết cháy lá thật hay\u00A0không.',
    howStep3Title: 'Ưu Tiên Sinh Học & Đúng Ngày Cách\u00A0Ly',
    howStep3Body:
      'Luôn xem biện pháp cắt tỉa và nấm đối kháng trước; khi buộc phải dùng thuốc đặc trị, tuân thủ tuyệt đối số ngày ngưng\u00A0thuốc.',
    almanacHeading: 'Sổ Tay 9 Cây Trồng & 15 Bệnh Thường Gặp',
    almanacLead:
      'Bấm chọn loại cây bà con đang trồng, sau đó bấm vào tên bệnh để lật mở tờ hướng dẫn triệu chứng và cách phòng trị ngay tại chỗ.',
    almanacAllCrops: 'Tất cả 9 cây',
    almanacSearchPlaceholder: 'Tìm tên bệnh hoặc tên cây (ví dụ: mốc sương, đạo ôn, cà chua)...',
    almanacOfflineNote: 'Đang hiển thị bản dữ liệu chuẩn lưu sẵn (máy chủ API nội bộ đang tạm nghỉ).',
    almanacEmpty: 'Không tìm thấy bệnh nào khớp với từ khóa đang lọc.',
    almanacHealthyOnlyCrop:
      'Cây trồng này hiện hỗ trợ nhận diện lá khỏe mạnh; chưa có bệnh hại riêng trong danh mục 15\u00A0bệnh.',
    sheetSymptomsHeading: 'Dấu hiệu nhận biết trên lá & thân',
    sheetConditionsHeading: 'Điều kiện thời tiết dễ bùng bệnh',
    sheetTreatmentsHeading: 'Phác đồ xử lý tham khảo',
    footerSummary:
      'EIU Plant Disease Detection System (EIU PDDS) — Dự án Kỹ thuật Phần mềm ứng dụng Thị giác Máy tính hỗ trợ nhận diện bệnh lá cây và tra cứu phác đồ nông học an\u00A0toàn.',
    footerSafetyNote:
      'Lưu ý nông học: Kết quả nhận diện qua hình ảnh là tài liệu tham khảo nhanh tại vườn. Với diện tích canh tác lớn hoặc bệnh lây lan bất thường, bà con nên tham khảo thêm cán bộ bảo vệ thực vật địa\u00A0phương.',
    footerCopyright:
      '© 2026 EIU Plant Disease Detection System (EIU PDDS) · Eastern International\u00A0University.',
  },
  en: {
    brandSubtitle: 'EIU Plant Disease Detection System',
    navWorkbench: 'Leaf Scanner',
    navHowItWorks: 'How It Works',
    navAlmanac: '9 Crops & 15 Diseases',
    langToggleAriaToEn: 'Switch language to English (EN)',
    langToggleAriaToVi: 'Switch language to Vietnamese (VN)',
    appButton: 'Mobile App',
    appSheetTitle: 'EIU PDDS Field Companion Mobile App',
    appSheetBody:
      'This web homepage lets growers inspect diseased leaves instantly in the browser without an account. For daily field scouting, the EIU PDDS mobile app adds three field-ready capabilities:',
    appFeatureHistory: 'Save chronological field diagnosis logs with bounding-box overlays across spray cycles.',
    appFeatureExpert: 'Submit human-in-the-loop feedback for agricultural experts to verify tricky lesions.',
    appFeatureGps: 'Attach field GPS coordinates automatically to track seasonal disease hotspots.',
    appSheetClose: 'Close',
    heroHeading: 'Inspect Leaf Spots — Protect Your Harvest',
    heroLead:
      'Snap or upload a clear photo of a spotted, blighted, or curling leaf. EIU PDDS highlights lesions directly on your leaf and pairs biological-first remedies with clear pre-harvest safety intervals.',
    cropHintLabel: 'Crop being grown (optional):',
    cropHintAll: 'Auto-detect from leaf photo',
    uploadCta: 'Capture / Upload Leaf Photo',
    uploadDropHint: 'Or drag and drop a leaf image here (JPEG, PNG, WebP — max\u00A015MB)',
    quotaLabel: (remaining, limit) => `${remaining}/${limit} free live guest scans remaining this hour`,
    sampleGroupLabel: 'No leaf photo handy? Try a reference specimen:',
    sampleReferenceBadge: 'Sample reference',
    toggleBoxesHide: 'Hide lesion boxes',
    toggleBoxesShow: 'Show lesion boxes',
    confidenceLabel: (pct, count) => `${pct}% confidence · ${count} region(s) bounded`,
    stageNeutralLoadingHeading: 'Analyzing Uploaded Leaf Specimen',
    stageNeutralErrorHeading: 'Uploaded Leaf Photo Needs Attention',
    loadingStageTitle: 'Inspecting leaf surface & matching agronomic protocols...',
    loadingStageBody: 'Please wait 2–4 seconds while lesion coordinates and pre-harvest intervals are prepared.',
    error400Title: 'Unsupported file format or file exceeds size limit',
    error422Title: 'No clear leaf or disease lesion detected in this photo',
    error422Body:
      'Place the leaf against a steady surface in natural daylight, move the camera closer to the affected blade, and try another photo.',
    error429Title: 'All 3 free live guest scans for this hour have been used',
    error429Body: (minutes) =>
      `To protect shared inference capacity, unauthenticated guests can run 3 live uploads per hour (resets in ~${minutes} min). Meanwhile, click any of the 3 reference specimens below to explore full results.`,
    errorGenericBody:
      'Unable to reach the inference server right now. Check your connection or click one of the 3 reference specimens below to preview a complete diagnosis.',
    retryUploadButton: 'Choose another leaf photo',
    prescriptionHeading: 'Treatment Protocol & Pre-Harvest Safety Interval',
    colInstructions: 'Field Action Steps',
    colDosageAndCoInfection: 'Active Ingredient & Dosage',
    colHarvestSafety: 'Pre-Harvest Safety',
    activeIngredientLabel: 'Active ingredient / Agent:',
    dosageLabel: 'Recommended dosage:',
    noChemicalNote: 'Biological or cultural practice — zero synthetic chemical residue.',
    coInfectionPrefix: 'Co-occurring signs on leaf:',
    harvestWaitDays: (days) => `Stop spraying at least ${days} day(s) before harvest.`,
    harvestSafeZero: '0 days · Safe for normal harvest schedule.',
    howHeading: 'Three Straightforward Field Steps',
    howLead:
      'Built with large type and calm controls so growers can verify plant health effortlessly outdoors or at home.',
    howStep1Title: 'Photograph One Affected Leaf Clearly',
    howStep1Body:
      'Pick a leaf showing representative spots or blight margins in natural light so veins and lesion borders read clearly.',
    howStep2Title: 'Cross-Check the Bounded Lesions Yourself',
    howStep2Body:
      'Toggle the mustard bounding boxes on and off to verify the highlighted spots against the real leaf surface.',
    howStep3Title: 'Start With Biological Care & Respect Wait Days',
    howStep3Body:
      'Prioritize sanitation and biological agents first; whenever targeted fungicides are needed, honor the pre-harvest safety interval.',
    almanacHeading: 'Field Almanac: 9 Supported Crops & 15 Pathologies',
    almanacLead:
      'Filter by crop and select any disease row to unfold an inline reference sheet with morphological symptoms and curated remedies.',
    almanacAllCrops: 'All 9 crops',
    almanacSearchPlaceholder: 'Search disease or crop name (e.g., late blight, blast, tomato)...',
    almanacOfflineNote: 'Showing bundled reference catalog snapshot (local API server is currently offline).',
    almanacEmpty: 'No diseases match the current crop and keyword filter.',
    almanacHealthyOnlyCrop:
      'This crop currently supports healthy-leaf verification; no separate pathology entry is listed among the 15 catalog\u00A0diseases.',
    sheetSymptomsHeading: 'Foliar & Stem Symptoms',
    sheetConditionsHeading: 'Favorable Weather & Risk Conditions',
    sheetTreatmentsHeading: 'Curated Agronomic Treatments',
    footerSummary:
      'EIU Plant Disease Detection System (EIU PDDS) — Applied Computer Vision & Distributed Systems capstone empowering accessible crop health diagnostics.',
    footerSafetyNote:
      'Agronomic notice: Visual AI diagnosis serves as a rapid field reference. For commercial acreage or severe outbreaks, consult certified local plant protection specialists.',
    footerCopyright: '© 2026 EIU Plant Disease Detection System (EIU PDDS) · Eastern International University.',
  },
};

export const VI_PLANT_NAMES: Record<string, string> = {
  POTATO: 'Khoai tây',
  TOMATO: 'Cà chua',
  CORN: 'Ngô (Bắp)',
  APPLE: 'Táo',
  BELL_PEPPER: 'Ớt chuông',
  GRAPE: 'Nho',
  PEACH: 'Đào',
  SOYBEAN: 'Đậu nành',
  RICE: 'Lúa',
};

export const VI_DISEASE_NAMES: Record<string, string> = {
  APPLE_SCAB: 'Bệnh ghẻ táo (Đốm sẹo lá táo)',
  APPLE_RUST: 'Bệnh gỉ sắt lá táo',
  BELL_PEPPER_LEAF_SPOT: 'Bệnh đốm lá vi khuẩn ớt chuông',
  CORN_COMMON_RUST: 'Bệnh gỉ sắt thông thường trên ngô',
  CORN_GRAY_LEAF_SPOT: 'Bệnh đốm xám lá ngô',
  CORN_LEAF_BLIGHT: 'Bệnh cháy lá lớn trên ngô',
  GRAPE_BLACK_ROT: 'Bệnh thối đen nho',
  POTATO_EARLY_BLIGHT: 'Bệnh đốm vòng khoai tây (Cháy lá sớm)',
  POTATO_LATE_BLIGHT: 'Bệnh mốc sương khoai tây (Cháy lá muộn)',
  TOMATO_EARLY_BLIGHT: 'Bệnh đốm vòng cà chua (Cháy lá sớm)',
  TOMATO_LATE_BLIGHT: 'Bệnh mốc sương cà chua',
  TOMATO_SEPTORIA_LEAF_SPOT: 'Bệnh đốm lá Septoria cà chua',
  TOMATO_BACTERIAL_SPOT: 'Bệnh đốm vi khuẩn cà chua',
  TOMATO_YELLOW_VIRUS: 'Bệnh virus xoăn vàng lá cà chua (TYLCV)',
  RICE_BLAST: 'Bệnh đạo ôn lúa (Cháy lá lúa)',
  POTATO_HEALTHY: 'Lá khoai tây khỏe mạnh',
  TOMATO_HEALTHY: 'Lá cà chua khỏe mạnh',
  CORN_HEALTHY: 'Lá ngô khỏe mạnh',
  APPLE_HEALTHY: 'Lá táo khỏe mạnh',
  BELL_PEPPER_HEALTHY: 'Lá ớt chuông khỏe mạnh',
  GRAPE_HEALTHY: 'Lá nho khỏe mạnh',
  PEACH_HEALTHY: 'Lá đào khỏe mạnh',
  SOYBEAN_HEALTHY: 'Lá đậu nành khỏe mạnh',
  RICE_HEALTHY: 'Lá lúa khỏe mạnh',
};

export const VI_SEVERITY_LABELS: Record<string, string> = {
  LOW: 'Mức nhẹ / Theo dõi',
  MODERATE: 'Mức trung bình',
  HIGH: 'Nguy cơ cao',
  CRITICAL: 'Nguy hiểm khẩn cấp',
};

export const EN_SEVERITY_LABELS: Record<string, string> = {
  LOW: 'Low severity',
  MODERATE: 'Moderate severity',
  HIGH: 'High risk',
  CRITICAL: 'Critical severity',
};

export const VI_TREATMENT_TYPES: Record<string, string> = {
  BIOLOGICAL: 'Hướng sinh học',
  CULTURAL: 'Chăm sóc & Canh tác',
  PREVENTIVE: 'Phòng ngừa',
  CHEMICAL: 'Thuốc đặc trị',
};

export const EN_TREATMENT_TYPES: Record<string, string> = {
  BIOLOGICAL: 'Biological care',
  CULTURAL: 'Cultural practice',
  PREVENTIVE: 'Preventive care',
  CHEMICAL: 'Targeted chemical',
};
