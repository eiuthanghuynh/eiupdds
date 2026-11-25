export type Locale = 'vi' | 'en';

export interface UiCopy {
  brandSubtitle: string;
  navAriaLabel: string;
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
  appDownloadApkButton: string;
  appQrCaption: string;
  appUnavailableNotice: string;
  prescriptionAppPrompt: string;
  prescriptionAppButton: string;
  error429AppButton: string;
  heroHeading: string;
  heroLead: string;
  cropHintLabel: string;
  cropHintAll: string;
  uploadCta: string;
  uploadPhotoTip: string;
  quotaLabel: (remaining: number, limit: number) => string;
  sampleGroupLabel: string;
  sampleReferenceBadge: string;
  resetUploadButton: string;
  toggleBoxesHide: string;
  toggleBoxesShow: string;
  confidenceLabel: (pct: number, count: number) => string;
  stageNeutralLoadingHeading: string;
  stageNeutralErrorHeading: string;
  healthyLeafTitle: string;
  loadingStageTitle: string;
  loadingStageBody: string;
  error400Title: string;
  errorInvalidFileType: string;
  errorFileTooLarge: string;
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
  culturalNoteLabel: string;
  noChemicalNote: string;
  coInfectionPrefix: string;
  harvestWaitDays: (days: number) => string;
  harvestSafeZero: string;
  harvestDayUnit: string;
  chemicalWaitCrossNotice: (days: number) => string;
  almanacHeading: string;
  almanacLead: string;
  almanacAllCrops: string;
  almanacSearchPlaceholder: string;
  almanacOfflineNote: string;
  almanacEmpty: string;
  almanacShowAll: (count: number) => string;
  almanacShowLess: string;
  almanacHealthyCropsNote: (cropsList: string) => string;
  sheetLoading: string;
  scientificNameLabel: string;
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
    navAriaLabel: 'Điều hướng trang chủ',
    navWorkbench: 'Soi Lá Bệnh',
    navAlmanac: 'Sổ Tay Bệnh',
    navHowItWorks: 'Cách Hoạt Động',
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
    appDownloadApkButton: 'Tải APK Android',
    appQrCaption: 'Quét mã để cài trên điện thoại',
    appUnavailableNotice: 'Bản cài Android nội bộ đang cập nhật liên kết — liên hệ nhóm EIU PDDS khi trình chiếu.',
    prescriptionAppPrompt: 'Cần lưu lịch sử phun & vị trí thửa ruộng?',
    prescriptionAppButton: 'Tải ứng dụng Android',
    error429AppButton: 'Cài app quét không giới hạn',
    heroHeading: 'Soi Đốm Lá — Giữ Trọn Vụ Mùa',
    heroLead: 'Chụp ảnh mặt lá có đốm bệnh để xem vùng bệnh và cách xử lý an toàn ngay.',
    cropHintLabel: 'Loại cây (không bắt buộc):',
    cropHintAll: 'Tự động nhận diện từ lá',
    uploadCta: 'Chụp / Tải Ảnh Lá Lên',
    uploadPhotoTip: 'Mẹo: đặt 1 lá đủ sáng, giữ vững tay.',
    quotaLabel: (remaining, limit) => `Còn ${remaining}/${limit} lượt quét miễn phí giờ này`,
    sampleGroupLabel: 'Xem nhanh lá mẫu:',
    sampleReferenceBadge: 'Mẫu có sẵn',
    resetUploadButton: 'Về mẫu mặc định',
    toggleBoxesHide: 'Tắt khung đốm bệnh',
    toggleBoxesShow: 'Bật khung đốm bệnh',
    confidenceLabel: (pct, count) => `Độ chắc chắn ${pct}% · Khoanh ${count} vùng trên lá`,
    stageNeutralLoadingHeading: 'Bàn Soi Mẫu Lá Đang Phân Tích',
    stageNeutralErrorHeading: 'Cần Kiểm Tra Lại Ảnh Lá Tải Lên',
    healthyLeafTitle: 'Lá cây khỏe mạnh',
    loadingStageTitle: 'Đang soi mặt lá & đối chiếu phác đồ nông học...',
    loadingStageBody: 'Bà con chờ khoảng 2–4 giây để máy khoanh vùng đốm bệnh và tra ngày cách ly.',
    error400Title: 'Tệp ảnh tải lên chưa đúng định dạng hoặc quá lớn',
    errorInvalidFileType: 'Chỉ hỗ trợ định dạng ảnh JPEG, PNG hoặc WebP.',
    errorFileTooLarge: 'Dung lượng ảnh vượt quá giới hạn 15MB cho phép.',
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
    culturalNoteLabel: 'Ghi chú canh tác:',
    noChemicalNote: 'Biện pháp sinh học / canh tác tự nhiên — không để lại dư lượng hóa chất.',
    coInfectionPrefix: 'Dấu hiệu đi kèm trên cùng lá:',
    harvestWaitDays: (days) => `Ngưng phun tối thiểu ${days} ngày trước khi thu hoạch.`,
    harvestSafeZero: '0 ngày · An toàn thu hoạch bình thường theo lịch canh tác.',
    harvestDayUnit: 'Ngày',
    chemicalWaitCrossNotice: (days) =>
      `Lưu ý: nếu chuyển sang thuốc đặc trị, cần cách ly ${days} ngày trước thu hoạch.`,
    almanacHeading: 'Sổ Tay Bệnh Cây Trồng',
    almanacLead: 'Chọn cây hoặc gõ từ khóa để xem triệu chứng và phác đồ xử lý.',
    almanacAllCrops: 'Tất cả',
    almanacSearchPlaceholder: 'Tìm tên bệnh hoặc cây (vd: mốc sương, đạo ôn)...',
    almanacOfflineNote: 'Dữ liệu ngoại tuyến.',
    almanacEmpty: 'Không tìm thấy bệnh nào khớp từ khóa.',
    almanacShowAll: (count) => `Xem tất cả ${count} bệnh`,
    almanacShowLess: 'Thu gọn',
    almanacHealthyCropsNote: (cropsList) => `Chỉ hỗ trợ kiểm tra lá khỏe: ${cropsList}.`,
    sheetLoading: 'Đang mở tờ hướng dẫn chi tiết...',
    scientificNameLabel: 'Tên khoa học:',
    sheetSymptomsHeading: 'Dấu hiệu nhận biết trên lá & thân',
    sheetConditionsHeading: 'Điều kiện thời tiết dễ bùng bệnh',
    sheetTreatmentsHeading: 'Phác đồ xử lý tham khảo',
    footerSummary: 'EIU PDDS — Nhận diện bệnh lá cây & phác đồ.',
    footerSafetyNote: 'Tham khảo nhanh; cần chuyên gia với diện tích lớn.',
    footerCopyright:
      '© 2026 EIU Plant Disease Detection System · EIU.',
  },
  en: {
    brandSubtitle: 'EIU Plant Disease Detection System',
    navAriaLabel: 'Homepage navigation',
    navWorkbench: 'Leaf Scanner',
    navHowItWorks: 'How It Works',
    navAlmanac: 'Disease Guide',
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
    appDownloadApkButton: 'Download Android APK',
    appQrCaption: 'Scan with your phone to install',
    appUnavailableNotice: 'Internal Android APK link is being prepared — ask the EIU PDDS demo team for direct install.',
    prescriptionAppPrompt: 'Want field spray logs & GPS tracking?',
    prescriptionAppButton: 'Get Android App',
    error429AppButton: 'Install mobile app for full scans',
    heroHeading: 'Inspect Leaf Spots — Protect Your Harvest',
    heroLead: 'Snap a spotted leaf to locate lesions and get safe treatment steps instantly.',
    cropHintLabel: 'Crop (optional):',
    cropHintAll: 'Auto-detect from leaf photo',
    uploadCta: 'Capture / Upload Leaf Photo',
    uploadPhotoTip: 'Tip: photograph one clear leaf in daylight.',
    quotaLabel: (remaining, limit) => `${remaining}/${limit} free guest scans left this hour`,
    sampleGroupLabel: 'Try a real leaf sample:',
    sampleReferenceBadge: 'Sample',
    resetUploadButton: 'Reset view',
    toggleBoxesHide: 'Hide lesion boxes',
    toggleBoxesShow: 'Show lesion boxes',
    confidenceLabel: (pct, count) => `${pct}% confidence · ${count} region(s) bounded`,
    stageNeutralLoadingHeading: 'Analyzing Uploaded Leaf Specimen',
    stageNeutralErrorHeading: 'Uploaded Leaf Photo Needs Attention',
    healthyLeafTitle: 'Healthy leaf',
    loadingStageTitle: 'Inspecting leaf surface & matching agronomic protocols...',
    loadingStageBody: 'Please wait 2–4 seconds while lesion coordinates and pre-harvest intervals are prepared.',
    error400Title: 'Unsupported file format or file exceeds size limit',
    errorInvalidFileType: 'Supported image formats are JPEG, PNG, or WebP.',
    errorFileTooLarge: 'Image file size exceeds the 15MB maximum limit.',
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
    culturalNoteLabel: 'Field note:',
    noChemicalNote: 'Biological or cultural practice — zero synthetic chemical residue.',
    coInfectionPrefix: 'Co-occurring signs on leaf:',
    harvestWaitDays: (days) => `Stop spraying at least ${days} day(s) before harvest.`,
    harvestSafeZero: '0 days · Safe for normal harvest schedule.',
    harvestDayUnit: 'Days',
    chemicalWaitCrossNotice: (days) =>
      `Note: switching to targeted chemical spray requires a ${days}-day harvest wait.`,
    almanacHeading: 'Crop Pathology Almanac',
    almanacLead: 'Filter by crop or keyword to inspect foliar symptoms and remedies.',
    almanacAllCrops: 'All crops',
    almanacSearchPlaceholder: 'Search disease or crop (e.g. late blight, blast)...',
    almanacOfflineNote: 'Showing offline reference snapshot.',
    almanacEmpty: 'No diseases match the current filter.',
    almanacShowAll: (count) => `Show all ${count} pathologies`,
    almanacShowLess: 'Show fewer',
    almanacHealthyCropsNote: (cropsList) => `Healthy-leaf screening only: ${cropsList}.`,
    sheetLoading: 'Loading pathology sheet...',
    scientificNameLabel: 'Scientific name:',
    sheetSymptomsHeading: 'Foliar & Stem Symptoms',
    sheetConditionsHeading: 'Favorable Weather & Risk Conditions',
    sheetTreatmentsHeading: 'Curated Agronomic Treatments',
    footerSummary: 'EIU PDDS — Foliar disease detection & treatment.',
    footerSafetyNote: 'Field reference; consult agronomists for large acreage.',
    footerCopyright: '© 2026 EIU Plant Disease Detection System · EIU.',
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
  BLUEBERRY: 'Việt quất',
  CHERRY: 'Anh đào',
  RASPBERRY: 'Mâm xôi',
  SQUASH: 'Bí ngô',
  STRAWBERRY: 'Dâu tây',
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
  TOMATO_MOSAIC_VIRUS: 'Bệnh khảm virus lá cà chua',
  TOMATO_LEAF_MOLD: 'Bệnh mốc lá cà chua',
  TOMATO_SPIDER_MITES: 'Nhện đỏ hai chấm hại lá cà chua',
  SQUASH_POWDERY_MILDEW: 'Bệnh phấn trắng lá bí ngô',
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
  BLUEBERRY_HEALTHY: 'Lá việt quất khỏe mạnh',
  CHERRY_HEALTHY: 'Lá anh đào khỏe mạnh',
  RASPBERRY_HEALTHY: 'Lá mâm xôi khỏe mạnh',
  SQUASH_HEALTHY: 'Lá bí ngô khỏe mạnh',
  STRAWBERRY_HEALTHY: 'Lá dâu tây khỏe mạnh',
};

export const VI_SEVERITY_LABELS: Record<string, string> = {
  LOW: 'Nhẹ',
  MODERATE: 'Trung bình',
  HIGH: 'Cao',
  CRITICAL: 'Nguy hiểm',
};

export const EN_SEVERITY_LABELS: Record<string, string> = {
  LOW: 'Low',
  MODERATE: 'Moderate',
  HIGH: 'High',
  CRITICAL: 'Critical',
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
