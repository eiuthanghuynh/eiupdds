'use client';

import { useMutation, useQuery } from '@tanstack/react-query';
import {
  analyzeLeafGuest,
  fetchDiseaseDetail,
  fetchDiseases,
  fetchPlants,
} from '@/lib/api/client';

export function useAnalyzeLeafMutation() {
  return useMutation({
    mutationFn: ({ file, plantCode }: { file: File; plantCode?: string }) =>
      analyzeLeafGuest(file, plantCode),
  });
}

export function usePlantsQuery() {
  return useQuery({
    queryKey: ['pdds', 'plants'],
    queryFn: fetchPlants,
  });
}

export function useDiseasesQuery(filter?: { plantCode?: string; keyword?: string }) {
  return useQuery({
    queryKey: ['pdds', 'diseases', filter?.plantCode ?? 'ALL', filter?.keyword ?? ''],
    queryFn: () => fetchDiseases(filter),
  });
}

export function useDiseaseDetailQuery(codeOrId: string | null) {
  return useQuery({
    queryKey: ['pdds', 'disease-detail', codeOrId],
    queryFn: () => fetchDiseaseDetail(codeOrId as string),
    enabled: Boolean(codeOrId),
  });
}
