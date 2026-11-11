import { DiseaseSummaryDto, PageResponse, DiagnosisSummaryDto } from '@/types';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api/v1';

export const ApiService = {
  async getDiseases(): Promise<DiseaseSummaryDto[]> {
    try {
      const res = await fetch(`${API_BASE_URL}/diseases`, { cache: 'no-store' });
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const json = await res.json();
      return json.data || [];
    } catch (err) {
      console.warn('API error fetching diseases:', err);
      return [];
    }
  },

  async getDiagnoses(page = 0, size = 10): Promise<PageResponse<DiagnosisSummaryDto>> {
    try {
      const res = await fetch(`${API_BASE_URL}/diagnoses?page=${page}&size=${size}`, { cache: 'no-store' });
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const json = await res.json();
      return json.data || { content: [], page: 0, size, totalElements: 0, totalPages: 0, last: true };
    } catch (err) {
      console.warn('API error fetching diagnoses:', err);
      return { content: [], page: 0, size, totalElements: 0, totalPages: 0, last: true };
    }
  },

  async checkHealth(): Promise<boolean> {
    try {
      const res = await fetch(`${API_BASE_URL}/diseases`, { method: 'GET' });
      return res.ok;
    } catch {
      return false;
    }
  },

  async createDisease(data: Partial<DiseaseSummaryDto>): Promise<DiseaseSummaryDto> {
    const res = await fetch(`${API_BASE_URL}/diseases`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    if (!res.ok) {
      const errJson = await res.json().catch(() => null);
      throw new Error(errJson?.message || `Lỗi tạo mầm bệnh (HTTP ${res.status})`);
    }
    const json = await res.json();
    return json.data;
  },

  async updateDisease(id: string, data: Partial<DiseaseSummaryDto>): Promise<DiseaseSummaryDto> {
    const res = await fetch(`${API_BASE_URL}/diseases/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    if (!res.ok) {
      const errJson = await res.json().catch(() => null);
      throw new Error(errJson?.message || `Lỗi cập nhật mầm bệnh (HTTP ${res.status})`);
    }
    const json = await res.json();
    return json.data;
  },

  async deleteDisease(id: string): Promise<void> {
    const res = await fetch(`${API_BASE_URL}/diseases/${id}`, {
      method: 'DELETE',
    });
    if (!res.ok) {
      const errJson = await res.json().catch(() => null);
      throw new Error(errJson?.message || `Lỗi xóa mầm bệnh (HTTP ${res.status})`);
    }
  },
};
