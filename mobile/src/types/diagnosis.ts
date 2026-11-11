/**
 * TypeScript Data Transfer Objects matching Backend Spring Boot API Contracts (OpenAPI 3.0 / Swagger)
 * System: EIU Plant Disease Detection System (EIU PDDS)
 */

export interface ApiResponse<T> {
  success: boolean;
  code: string;
  message: string;
  data: T;
  timestamp: string;
}

export type SeverityLevel = 'LOW' | 'MODERATE' | 'HIGH' | 'CRITICAL';

export type TreatmentType = 'BIOLOGICAL' | 'CHEMICAL' | 'PREVENTIVE' | 'CULTURAL';

export interface BoundingBoxDto {
  id?: string;
  label: string;
  diseaseCode: string;
  confidence: number;

  // Raw Pixel Coordinates
  x: number;
  y: number;
  width: number;
  height: number;
  xMin: number;
  yMin: number;

  // Normalized Top-Left Coordinates [0.0 - 1.0] for direct Mobile SVG rendering
  xMinNormalized: number;
  yMinNormalized: number;
  widthNormalized: number;
  heightNormalized: number;
}

export interface DiseaseSummaryDto {
  id: string;
  diseaseCode: string;
  commonNameVi: string;
  commonNameEn: string;
  scientificName: string;
  severityLevel: SeverityLevel;
  thumbnailUrl?: string | null;
  symptomsVi?: string | null;
}

export interface TreatmentDto {
  id: string;
  treatmentType: TreatmentType;
  titleVi: string;
  contentVi: string;
  activeIngredients?: string | null;
  dosageInstructions?: string | null;
  safetyIntervalDays?: number | null;
}

export interface DiagnosisAnalyzeResponse {
  diagnosisId: string;
  originalImageUrl: string;
  imageWidth: number;
  imageHeight: number;
  dominantDisease: DiseaseSummaryDto;
  dominantConfidence: number;
  boundingBoxes: BoundingBoxDto[];
  coOccurringDiseases: DiseaseSummaryDto[];
  recommendedTreatments: TreatmentDto[];
  inferenceTimeMs: number;
  status: 'COMPLETED' | 'PENDING' | 'FAILED';
  diagnosedAt: string;
}

export interface AnalyzeImageParams {
  imageUri: string;
  plantCode?: string;
  latitude?: number;
  longitude?: number;
  deviceInfo?: string;
}
