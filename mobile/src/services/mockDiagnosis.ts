/**
 * Mock Data Repository for Offline Development and Testing
 * Exact match with Backend Spring Boot DTOs (EIU PDDS)
 */

import { DiagnosisAnalyzeResponse } from '@/types/diagnosis';

export const MOCK_POTATO_LATE_BLIGHT_DIAGNOSIS: DiagnosisAnalyzeResponse = {
  diagnosisId: '3fa85f64-5717-4562-b3fc-2c963f66afa6',
  originalImageUrl: 'https://images.unsplash.com/photo-1592417817098-8f3d6eb22509?auto=format&fit=crop&w=1200&q=80',
  imageWidth: 1024,
  imageHeight: 768,
  dominantDisease: {
    id: 'd1111111-1111-1111-1111-111111111101',
    diseaseCode: 'POTATO_LATE_BLIGHT',
    commonNameVi: 'Bệnh mốc sương khoai tây',
    commonNameEn: 'Potato Late Blight',
    scientificName: 'Phytophthora infestans',
    severityLevel: 'HIGH',
    thumbnailUrl: 'https://images.unsplash.com/photo-1592417817098-8f3d6eb22509?auto=format&fit=crop&w=300&q=80',
    symptomsVi: 'Xuất hiện các đốm nâu sẫm úng nước ở đầu hoặc mép lá, khi độ ẩm cao mặt dưới xuất hiện lớp mốc trắng bao phủ.',
  },
  dominantConfidence: 0.884,
  boundingBoxes: [
    {
      id: 'bb-001',
      label: 'Potato leaf late blight',
      diseaseCode: 'POTATO_LATE_BLIGHT',
      confidence: 0.884,
      x: 399,
      y: 303,
      width: 430,
      height: 268,
      xMin: 184,
      yMin: 169,
      xMinNormalized: 0.18,
      yMinNormalized: 0.22,
      widthNormalized: 0.42,
      heightNormalized: 0.35,
    },
    {
      id: 'bb-002',
      label: 'Potato leaf late blight',
      diseaseCode: 'POTATO_LATE_BLIGHT',
      confidence: 0.742,
      x: 727,
      y: 552,
      width: 327,
      height: 215,
      xMin: 563,
      yMin: 445,
      xMinNormalized: 0.55,
      yMinNormalized: 0.58,
      widthNormalized: 0.32,
      heightNormalized: 0.28,
    },
  ],
  coOccurringDiseases: [
    {
      id: 'd1111111-1111-1111-1111-111111111102',
      diseaseCode: 'POTATO_EARLY_BLIGHT',
      commonNameVi: 'Bệnh đốm vòng khoai tây',
      commonNameEn: 'Potato Early Blight',
      scientificName: 'Alternaria solani',
      severityLevel: 'MODERATE',
      symptomsVi: 'Các vết đốm nâu hình tròn đồng tâm như bia bắn, mép lá có viền vàng úa.',
    },
  ],
  recommendedTreatments: [
    {
      id: 'tr-001',
      treatmentType: 'CHEMICAL',
      titleVi: 'Phun thuốc đặc trị nấm hoại sinh Metalaxyl + Mancozeb',
      contentVi:
        'Sử dụng các thuốc gốc Metalaxyl-M (80g/kg) kết hợp Mancozeb (640g/kg) như Ridomil Gold 68WG. Phun ướt đều tán lá cả hai mặt, ưu tiên mặt dưới lá nơi bào tử tập trung.',
      activeIngredients: 'Metalaxyl-M 40g/kg + Mancozeb 640g/kg',
      dosageInstructions: '50g pha cho bình 16 lít nước, phun định kỳ cách nhau 5-7 ngày trong thời tiết sương ẩm.',
      safetyIntervalDays: 7,
    },
    {
      id: 'tr-002',
      treatmentType: 'BIOLOGICAL',
      titleVi: 'Chế phẩm nấm đối kháng Trichoderma viride',
      contentVi:
        'Phun sương dung dịch vi sinh nấm Trichoderma kết hợp vi khuẩn đối kháng Bacillus subtilis để tiêu diệt màng sợi nấm tự nhiên, an toàn cho môi trường.',
      activeIngredients: 'Bào tử sống Trichoderma viride (10^9 CFU/g)',
      dosageInstructions: '30g cho bình 16 lít, phun vào lúc trời râm mát hoặc sau mưa rào.',
      safetyIntervalDays: 0,
    },
    {
      id: 'tr-003',
      treatmentType: 'CULTURAL',
      titleVi: 'Cắt tỉa bộ phận nhiễm bệnh & cải tạo lưu thông gió',
      contentVi:
        'Khẩn trương cắt tỉa các nhánh lá bị thâm đen nặng gom vào bao tiêu hủy hoặc chôn rải vôi bột; dừng bón phân đạm (N) và không tưới phun mưa vào buổi chiều tối.',
      dosageInstructions: 'Thực hiện ngay trong vòng 24 giờ sau khi phát hiện bệnh.',
    },
    {
      id: 'tr-004',
      treatmentType: 'PREVENTIVE',
      titleVi: 'Phòng ngừa lây lan diện rộng & luân canh',
      contentVi:
        'Rắc vôi bột khử khuẩn mặt liếp với liều lượng 30kg/1000m2. Luân canh với các giống cây họ hoa thập tự hoặc họ lúa trong ít nhất 2 vụ tiếp theo.',
    },
  ],
  inferenceTimeMs: 245,
  status: 'COMPLETED',
  diagnosedAt: new Date().toISOString(),
};

export const MOCK_TOMATO_LEAF_CURL_DIAGNOSIS: DiagnosisAnalyzeResponse = {
  diagnosisId: '4b7a1234-8888-4321-b3fc-112233445566',
  originalImageUrl: 'https://images.unsplash.com/photo-1591857177580-dc82b9ac4e1e?auto=format&fit=crop&w=1200&q=80',
  imageWidth: 800,
  imageHeight: 600,
  dominantDisease: {
    id: 'd2222222-2222-2222-2222-222222222201',
    diseaseCode: 'TOMATO_YELLOW_LEAF_CURL',
    commonNameVi: 'Bệnh xoăn vàng lá cà chua',
    commonNameEn: 'Tomato Yellow Leaf Curl Virus',
    scientificName: 'Begomovirus',
    severityLevel: 'CRITICAL',
    symptomsVi: 'Lá ngọn co xoăn dựng đứng, phiến lá thu nhỏ, gân lá dày sần sùi và mép lá biến vàng.',
  },
  dominantConfidence: 0.912,
  boundingBoxes: [
    {
      id: 'bb-003',
      label: 'Tomato yellow leaf curl',
      diseaseCode: 'TOMATO_YELLOW_LEAF_CURL',
      confidence: 0.912,
      x: 400,
      y: 280,
      width: 480,
      height: 320,
      xMin: 160,
      yMin: 120,
      xMinNormalized: 0.2,
      yMinNormalized: 0.2,
      widthNormalized: 0.6,
      heightNormalized: 0.53,
    },
  ],
  coOccurringDiseases: [],
  recommendedTreatments: [
    {
      id: 'tr-005',
      treatmentType: 'CHEMICAL',
      titleVi: 'Kiểm soát côn trùng môi giới (Bọ phấn trắng)',
      contentVi: 'Virus truyền qua bọ phấn trắng (Bemisia tabaci). Cần phun thuốc trừ bọ phấn như Thiamethoxam hoặc Dinotefuran.',
      activeIngredients: 'Thiamethoxam 25% WDG',
      dosageInstructions: '4g pha 16 lít nước, phun tập trung mặt dưới lá non vào sáng sớm.',
      safetyIntervalDays: 7,
    },
    {
      id: 'tr-006',
      treatmentType: 'CULTURAL',
      titleVi: 'Nhổ bỏ cây bệnh & căng bẫy dính màu vàng',
      contentVi: 'Cây đã nhiễm virus không thể khỏi bệnh; nhổ bỏ triệt để để tránh lây sang toàn vườn; cắm bẫy keo màu vàng bẫy bọ phấn.',
      dosageInstructions: 'Cắm 30-40 bẫy dính vàng/sào đất trồng.',
    },
  ],
  inferenceTimeMs: 310,
  status: 'COMPLETED',
  diagnosedAt: new Date().toISOString(),
};

export const MOCK_DIAGNOSES = [
  MOCK_POTATO_LATE_BLIGHT_DIAGNOSIS,
  MOCK_TOMATO_LEAF_CURL_DIAGNOSIS,
];

export function getRandomMockDiagnosis(originalImageUri?: string): DiagnosisAnalyzeResponse {
  const base = Math.random() > 0.3 ? MOCK_POTATO_LATE_BLIGHT_DIAGNOSIS : MOCK_TOMATO_LEAF_CURL_DIAGNOSIS;
  return {
    ...base,
    diagnosisId: 'mock-' + Math.random().toString(36).substring(2, 10),
    originalImageUrl: originalImageUri || base.originalImageUrl,
    diagnosedAt: new Date().toISOString(),
  };
}
