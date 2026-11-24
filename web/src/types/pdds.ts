export type Severity = 'LOW' | 'MODERATE' | 'HIGH' | 'CRITICAL';

export type TreatmentType = 'BIOLOGICAL' | 'CHEMICAL' | 'PREVENTIVE' | 'CULTURAL';

export interface ApiResponse<T> {
  success: boolean;
  code: string;
  message: string;
  data: T;
  timestamp: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface PlantDto {
  id: string;
  plantCode: string;
  commonName: string;
  scientificName: string | null;
  iconUrl?: string | null;
  description?: string | null;
}

export interface DiseaseSummaryDto {
  id: string;
  diseaseCode: string;
  commonName: string;
  scientificName: string | null;
  severity: Severity;
}

export interface TreatmentDto {
  id: string;
  treatmentType: TreatmentType;
  title: string;
  instructions: string;
  activeIngredients: string | null;
  dosageRecommendation: string | null;
  safetyIntervalDays: number | null;
  warning: string | null;
}

export interface DiseaseDetailDto {
  id: string;
  diseaseCode: string;
  commonName: string;
  scientificName: string | null;
  severity: Severity;
  symptoms: string | null;
  description: string | null;
  treatments: TreatmentDto[];
}

export interface BoundingBoxDto {
  id: string;
  label: string;
  diseaseCode: string | null;
  confidence: number;
  x: number;
  y: number;
  width: number;
  height: number;
  xMin: number;
  yMin: number;
  xMinNormalized: number;
  yMinNormalized: number;
  widthNormalized: number;
  heightNormalized: number;
}

export interface DiagnosisAnalyzeResponse {
  diagnosisId: string;
  originalImageUrl: string;
  imageWidth: number;
  imageHeight: number;
  plant: PlantDto | null;
  dominantDisease: DiseaseSummaryDto | null;
  dominantConfidence: number | null;
  boundingBoxes: BoundingBoxDto[];
  coOccurringDiseases: DiseaseSummaryDto[];
  recommendedTreatments: TreatmentDto[];
  inferenceTimeMs: number | null;
  latitude?: number | null;
  longitude?: number | null;
  deviceInfo?: string | null;
  status: string;
  diagnosedAt: string;
}

export interface GuestQuotaInfo {
  limit: number;
  remaining: number;
  retryAfterSeconds?: number;
}

export interface SampleSpecimen {
  key: string;
  shortLabel: string;
  response: DiagnosisAnalyzeResponse;
}
