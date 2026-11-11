export type SeverityLevel = 'LOW' | 'MODERATE' | 'HIGH' | 'CRITICAL';

export type TreatmentType = 'BIOLOGICAL' | 'CHEMICAL' | 'PREVENTIVE' | 'CULTURAL';

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

export interface PlantDto {
  id: string;
  plantCode: string;
  commonNameVi: string;
  commonNameEn: string;
  scientificName?: string | null;
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

export interface DiagnosisSummaryDto {
  diagnosisId: string;
  originalImageUrl: string;
  plant?: PlantDto | null;
  dominantDisease?: DiseaseSummaryDto | null;
  dominantConfidence: number;
  boundingBoxCount: number;
  latitude?: number | null;
  longitude?: number | null;
  deviceInfo?: string | null;
  status: string;
  createdAt: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}
