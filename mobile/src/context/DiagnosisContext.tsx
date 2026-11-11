import React, { createContext, useContext, useState, ReactNode } from 'react';
import { DiagnosisService } from '@/services/diagnosisService';
import { DiagnosisAnalyzeResponse } from '@/types/diagnosis';

interface DiagnosisContextType {
  currentDiagnosis: DiagnosisAnalyzeResponse | null;
  selectedPhotoUri: string | null;
  isAnalyzing: boolean;
  error: string | null;
  useMockData: boolean;
  setUseMockData: (val: boolean) => void;
  analyzePhoto: (
    photoUri: string,
    options?: { plantCode?: string; latitude?: number; longitude?: number }
  ) => Promise<DiagnosisAnalyzeResponse>;
  clearDiagnosis: () => void;
  setCurrentDiagnosis: (data: DiagnosisAnalyzeResponse | null) => void;
  setSelectedPhotoUri: (uri: string | null) => void;
}

const DiagnosisContext = createContext<DiagnosisContextType | undefined>(undefined);

export function DiagnosisProvider({ children }: { children: ReactNode }) {
  const [currentDiagnosis, setCurrentDiagnosis] = useState<DiagnosisAnalyzeResponse | null>(null);
  const [selectedPhotoUri, setSelectedPhotoUri] = useState<string | null>(null);
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [useMockData, setUseMockData] = useState<boolean>(false); // Set to false to always use live Backend API

  const analyzePhoto = async (
    photoUri: string,
    options: { plantCode?: string; latitude?: number; longitude?: number } = {}
  ): Promise<DiagnosisAnalyzeResponse> => {
    try {
      setIsAnalyzing(true);
      setError(null);
      setSelectedPhotoUri(photoUri);

      const result = await DiagnosisService.analyzeImage(
        {
          imageUri: photoUri,
          plantCode: options.plantCode,
          latitude: options.latitude,
          longitude: options.longitude,
          deviceInfo: 'Expo Dev Client',
        },
        {
          forceMock: useMockData,
          fallbackToMockOnFailure: false, // Strict live backend data only
        }
      );

      setCurrentDiagnosis(result);
      return result;
    } catch (err: any) {
      const errMsg = err?.message || 'Có lỗi xảy ra trong quá trình chẩn đoán hình ảnh';
      setError(errMsg);
      throw err;
    } finally {
      setIsAnalyzing(false);
    }
  };

  const clearDiagnosis = () => {
    setCurrentDiagnosis(null);
    setSelectedPhotoUri(null);
    setError(null);
  };

  return (
    <DiagnosisContext.Provider
      value={{
        currentDiagnosis,
        selectedPhotoUri,
        isAnalyzing,
        error,
        useMockData,
        setUseMockData,
        analyzePhoto,
        clearDiagnosis,
        setCurrentDiagnosis,
        setSelectedPhotoUri,
      }}>
      {children}
    </DiagnosisContext.Provider>
  );
}

export function useDiagnosis(): DiagnosisContextType {
  const context = useContext(DiagnosisContext);
  if (!context) {
    throw new Error('useDiagnosis must be used within a DiagnosisProvider');
  }
  return context;
}
