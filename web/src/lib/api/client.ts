import type {
  ApiResponse,
  DiagnosisAnalyzeResponse,
  DiseaseDetailDto,
  DiseaseSummaryDto,
  GuestQuotaInfo,
  PageResponse,
  PlantDto,
} from '@/types/pdds';
import {
  FALLBACK_DISEASES,
  FALLBACK_PLANTS,
  filterFallbackDiseases,
  findFallbackDiseaseRecord,
} from '@/lib/data/fallbackCatalog';

const DEFAULT_API_BASE = 'http://localhost:8080';
const MAX_LEAF_IMAGE_BYTES = 15 * 1024 * 1024;
const ALLOWED_IMAGE_MIMES = new Set(['image/jpeg', 'image/png', 'image/webp']);

export function getApiBaseUrl(): string {
  const raw = process.env.NEXT_PUBLIC_API_BASE_URL?.trim();
  return (raw && raw.length > 0 ? raw : DEFAULT_API_BASE).replace(/\/+$/, '');
}

export class PddsApiError extends Error {
  public readonly status: number;
  public readonly code: string;
  public readonly retryAfterSeconds?: number;

  constructor(status: number, code: string, message: string, retryAfterSeconds?: number) {
    super(message);
    this.name = 'PddsApiError';
    this.status = status;
    this.code = code;
    this.retryAfterSeconds = retryAfterSeconds;
  }
}

export function validateLeafFile(file: File): void {
  if (!ALLOWED_IMAGE_MIMES.has(file.type)) {
    throw new PddsApiError(
      400,
      'INVALID_FILE_TYPE',
      'Chỉ hỗ trợ định dạng ảnh JPEG, PNG hoặc WebP (Supported formats: JPEG, PNG, WebP).'
    );
  }
  if (file.size > MAX_LEAF_IMAGE_BYTES) {
    throw new PddsApiError(
      400,
      'FILE_TOO_LARGE',
      'Dung lượng ảnh vượt quá giới hạn 15MB (Maximum allowed size is 15MB).'
    );
  }
}

function parseHeaderInt(headers: Headers, name: string): number | undefined {
  const raw = headers.get(name);
  if (!raw) return undefined;
  const parsed = Number.parseInt(raw, 10);
  return Number.isFinite(parsed) ? parsed : undefined;
}

export async function analyzeLeafGuest(
  file: File,
  plantCode?: string
): Promise<{ result: DiagnosisAnalyzeResponse; quota: GuestQuotaInfo }> {
  validateLeafFile(file);

  const formData = new FormData();
  formData.append('image', file);
  if (plantCode && plantCode.trim().length > 0) {
    formData.append('plantCode', plantCode.trim().toUpperCase());
  }
  formData.append('deviceInfo', 'EIU PDDS Web Homepage');

  const response = await fetch(`${getApiBaseUrl()}/api/v1/diagnoses/analyze`, {
    method: 'POST',
    body: formData,
  });

  const limit = parseHeaderInt(response.headers, 'X-RateLimit-Limit') ?? 3;
  const remaining = parseHeaderInt(response.headers, 'X-RateLimit-Remaining');
  const retryAfterSeconds = parseHeaderInt(response.headers, 'Retry-After');

  let body: Partial<ApiResponse<DiagnosisAnalyzeResponse>> | null = null;
  try {
    body = (await response.json()) as Partial<ApiResponse<DiagnosisAnalyzeResponse>>;
  } catch {
    body = null;
  }

  if (!response.ok || !body?.success || !body.data) {
    const code =
      body?.code ??
      (response.status === 429
        ? 'RATE_001'
        : response.status === 422
          ? 'DIAGNOSIS_003'
          : 'API_ERROR');
    const message = body?.message ?? `HTTP ${response.status}`;
    throw new PddsApiError(response.status, code, message, retryAfterSeconds);
  }

  return {
    result: body.data,
    quota: {
      limit,
      remaining: remaining ?? Math.max(0, limit - 1),
      retryAfterSeconds,
    },
  };
}

export async function fetchPlants(): Promise<{
  plants: PlantDto[];
  isOfflineFallback: boolean;
}> {
  try {
    const response = await fetch(`${getApiBaseUrl()}/api/v1/plants`, {
      method: 'GET',
      headers: { Accept: 'application/json' },
    });
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }
    const body = (await response.json()) as ApiResponse<PlantDto[]>;
    if (Array.isArray(body.data) && body.data.length > 0) {
      return { plants: body.data, isOfflineFallback: false };
    }
    return { plants: FALLBACK_PLANTS, isOfflineFallback: true };
  } catch {
    return { plants: FALLBACK_PLANTS, isOfflineFallback: true };
  }
}

export async function fetchDiseases(params?: {
  plantCode?: string;
  keyword?: string;
}): Promise<{
  diseases: DiseaseSummaryDto[];
  isOfflineFallback: boolean;
}> {
  try {
    const search = new URLSearchParams({ unpaged: 'true' });
    if (params?.plantCode) {
      search.set('plantCode', params.plantCode);
    }
    if (params?.keyword?.trim()) {
      search.set('keyword', params.keyword.trim());
    }
    const response = await fetch(`${getApiBaseUrl()}/api/v1/diseases?${search.toString()}`, {
      method: 'GET',
      headers: { Accept: 'application/json' },
    });
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }
    const body = (await response.json()) as ApiResponse<PageResponse<DiseaseSummaryDto>>;
    if (body?.data?.content) {
      return { diseases: body.data.content, isOfflineFallback: false };
    }
    return { diseases: filterFallbackDiseases(params), isOfflineFallback: true };
  } catch {
    return { diseases: filterFallbackDiseases(params), isOfflineFallback: true };
  }
}

export async function fetchDiseaseDetail(codeOrId: string): Promise<DiseaseDetailDto> {
  try {
    const response = await fetch(
      `${getApiBaseUrl()}/api/v1/diseases/${encodeURIComponent(codeOrId)}`,
      {
        method: 'GET',
        headers: { Accept: 'application/json' },
      }
    );
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }
    const body = (await response.json()) as ApiResponse<DiseaseDetailDto>;
    if (body?.data) {
      return body.data;
    }
  } catch {
    // Fall through to bundled reference catalog
  }

  const fallback = findFallbackDiseaseRecord(codeOrId) ?? FALLBACK_DISEASES[0];
  return {
    id: fallback.id,
    diseaseCode: fallback.diseaseCode,
    commonName: fallback.commonName,
    scientificName: fallback.scientificName,
    severity: fallback.severity,
    symptoms: fallback.symptoms,
    description: fallback.description,
    treatments: fallback.treatments,
  };
}
