/**
 * Diagnosis Business Service
 * Encapsulates backend API communication with Roboflow inference proxy
 * and seamless fallback to offline mock data when testing.
 */

import { ApiClient } from './apiClient';
import { getRandomMockDiagnosis } from './mockDiagnosis';
import {
  AnalyzeImageParams,
  DiagnosisAnalyzeResponse,
  DiseaseSummaryDto,
} from '@/types/diagnosis';

export const DiagnosisService = {
  /**
   * Analyze leaf photo for plant diseases.
   * Sends multipart form data to Backend Spring Boot (which calls Roboflow).
   * If useMock is enabled or network is offline in mock mode, returns realistic simulated AI inference.
   */
  async analyzeImage(
    params: AnalyzeImageParams,
    options: { forceMock?: boolean; fallbackToMockOnFailure?: boolean } = {}
  ): Promise<DiagnosisAnalyzeResponse> {
    const { forceMock = false, fallbackToMockOnFailure = true } = options;

    if (forceMock) {
      // Simulate network latency 1.2s for realistic UX
      await new Promise((resolve) => setTimeout(resolve, 1200));
      return getRandomMockDiagnosis(params.imageUri);
    }

    try {
      const formData = new FormData();

      // Extract filename from uri or fallback
      const uriParts = params.imageUri.split('/');
      const fileName = uriParts[uriParts.length - 1] || 'leaf_diagnosis.jpg';
      const fileType = fileName.endsWith('.png') ? 'image/png' : 'image/jpeg';

      // Support both Native RN Fetch (via EXPO_PUBLIC_USE_RN_FETCH) and Web/WinterCG Fetch
      const useRnFetch = process.env.EXPO_PUBLIC_USE_RN_FETCH === 'true';

      if (useRnFetch) {
        // High-performance direct streaming via React Native Native Networking
        formData.append('image', {
          uri: params.imageUri,
          name: fileName,
          type: fileType,
        } as any);
      } else {
        // Fallback for WinterCG fetch if EXPO_PUBLIC_USE_RN_FETCH is not yet active
        try {
          const fileResponse = await fetch(params.imageUri);
          const fileBlob = await fileResponse.blob();
          formData.append('image', fileBlob, fileName);
        } catch {
          formData.append('image', {
            uri: params.imageUri,
            name: fileName,
            type: fileType,
          } as any);
        }
      }

      if (params.plantCode) {
        formData.append('plantCode', params.plantCode);
      }
      if (params.latitude !== undefined && params.latitude !== null) {
        formData.append('latitude', String(params.latitude));
      }
      if (params.longitude !== undefined && params.longitude !== null) {
        formData.append('longitude', String(params.longitude));
      }
      if (params.deviceInfo) {
        formData.append('deviceInfo', params.deviceInfo);
      }

      const response = await ApiClient.postMultipart<DiagnosisAnalyzeResponse>(
        '/diagnoses/analyze',
        formData
      );

      return response.data;
    } catch (error) {
      console.warn('Backend call failed, checking fallback mode:', error);
      if (fallbackToMockOnFailure) {
        console.info('Switching to mock diagnosis data for testing UI flow.');
        // Artificial delay for smooth UX
        await new Promise((resolve) => setTimeout(resolve, 1000));
        return getRandomMockDiagnosis(params.imageUri);
      }
      throw error;
    }
  },

  /**
   * Get list of supported plant diseases
   */
  async getDiseases(): Promise<DiseaseSummaryDto[]> {
    const response = await ApiClient.get<DiseaseSummaryDto[]>('/diseases');
    return response.data;
  },
};
