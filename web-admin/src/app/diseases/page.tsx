'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import {
  Bug,
  Plus,
  Search,
  ExternalLink,
  Edit2,
  Trash2,
  X,
  AlertTriangle,
  CheckCircle2,
  Loader2,
} from 'lucide-react';
import { ApiService } from '@/services/api';
import { DiseaseSummaryDto, SeverityLevel } from '@/types';

const fallbackDiseases: DiseaseSummaryDto[] = [
  {
    id: 'd1111111-1111-1111-1111-111111111101',
    diseaseCode: 'POTATO_LATE_BLIGHT',
    commonNameVi: 'Bệnh mốc sương khoai tây',
    commonNameEn: 'Potato Late Blight',
    scientificName: 'Phytophthora infestans',
    severityLevel: 'HIGH',
    symptomsVi: 'Xuất hiện các đốm nâu sẫm úng nước ở đầu hoặc mép lá, khi độ ẩm cao mặt dưới xuất hiện lớp mốc trắng bao phủ.',
  },
  {
    id: 'd1111111-1111-1111-1111-111111111102',
    diseaseCode: 'POTATO_EARLY_BLIGHT',
    commonNameVi: 'Bệnh đốm vòng khoai tây',
    commonNameEn: 'Potato Early Blight',
    scientificName: 'Alternaria solani',
    severityLevel: 'MODERATE',
    symptomsVi: 'Các vết đốm nâu hình tròn đồng tâm như bia bắn, mép lá có viền vàng úa.',
  },
  {
    id: 'd1111111-1111-1111-1111-111111111103',
    diseaseCode: 'RICE_BLAST',
    commonNameVi: 'Bệnh đạo ôn lúa',
    commonNameEn: 'Rice Blast',
    scientificName: 'Magnaporthe oryzae',
    severityLevel: 'CRITICAL',
    symptomsVi: 'Đốm hình mắt én ở phiến lá, tâm xám trắng viền nâu đậm, có thể gây cháy khô toàn bộ chỏm lá.',
  },
  {
    id: 'd1111111-1111-1111-1111-111111111104',
    diseaseCode: 'TOMATO_LEAF_SPOT',
    commonNameVi: 'Bệnh đốm lá cà chua',
    commonNameEn: 'Tomato Septoria Leaf Spot',
    scientificName: 'Septoria lycopersici',
    severityLevel: 'LOW',
    symptomsVi: 'Vết đốm nhỏ hình tròn xám tro rải rác trên bề mặt lá già, làm lá vàng úa dần.',
  },
  {
    id: 'd1111111-1111-1111-1111-111111111105',
    diseaseCode: 'CORN_COMMON_RUST',
    commonNameVi: 'Bệnh rỉ sắt ngô (bắp)',
    commonNameEn: 'Common Rust of Corn',
    scientificName: 'Puccinia sorghi',
    severityLevel: 'MODERATE',
    symptomsVi: 'Các ổ nấm nổi sần sùi màu nâu đỏ hoặc vàng đồng ở cả hai mặt phiến lá ngô.',
  },
];

interface DiseaseFormData {
  diseaseCode: string;
  commonNameVi: string;
  commonNameEn: string;
  scientificName: string;
  severityLevel: SeverityLevel;
  symptomsVi: string;
}

const initialFormData: DiseaseFormData = {
  diseaseCode: '',
  commonNameVi: '',
  commonNameEn: '',
  scientificName: '',
  severityLevel: 'MODERATE',
  symptomsVi: '',
};

export default function DiseasesPage() {
  const [diseases, setDiseases] = useState<DiseaseSummaryDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [severityFilter, setSeverityFilter] = useState<string>('ALL');

  // Modal State
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [formData, setFormData] = useState<DiseaseFormData>(initialFormData);
  const [submitting, setSubmitting] = useState(false);
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; message: string } | null>(null);

  useEffect(() => {
    let ignore = false;
    async function load() {
      try {
        const data = await ApiService.getDiseases();
        if (!ignore) {
          setDiseases(data && data.length > 0 ? data : fallbackDiseases);
        }
      } catch {
        if (!ignore) {
          setDiseases(fallbackDiseases);
        }
      } finally {
        if (!ignore) {
          setLoading(false);
        }
      }
    }
    load();
    return () => {
      ignore = true;
    };
  }, []);

  const refreshDiseases = async () => {
    try {
      setLoading(true);
      const data = await ApiService.getDiseases();
      setDiseases(data && data.length > 0 ? data : fallbackDiseases);
    } catch {
      // keep current
    } finally {
      setLoading(false);
    }
  };

  const showNotification = (type: 'success' | 'error', message: string) => {
    setFeedback({ type, message });
    setTimeout(() => {
      setFeedback(null);
    }, 4000);
  };

  const handleOpenAddModal = () => {
    setEditingId(null);
    setFormData(initialFormData);
    setIsModalOpen(true);
  };

  const handleOpenEditModal = (item: DiseaseSummaryDto) => {
    setEditingId(item.id);
    setFormData({
      diseaseCode: item.diseaseCode,
      commonNameVi: item.commonNameVi,
      commonNameEn: item.commonNameEn || '',
      scientificName: item.scientificName,
      severityLevel: item.severityLevel,
      symptomsVi: item.symptomsVi || '',
    });
    setIsModalOpen(true);
  };

  const handleCloseModal = () => {
    setIsModalOpen(false);
    setEditingId(null);
    setFormData(initialFormData);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.diseaseCode.trim() || !formData.commonNameVi.trim()) {
      showNotification('error', 'Vui lòng nhập đầy đủ mã phân loại và tên bệnh tiếng Việt.');
      return;
    }

    try {
      setSubmitting(true);
      if (editingId) {
        // Update
        try {
          const updated = await ApiService.updateDisease(editingId, formData);
          setDiseases((prev) => prev.map((d) => (d.id === editingId ? { ...d, ...updated } : d)));
          await refreshDiseases();
        } catch {
          // Local fallback if backend is offline
          setDiseases((prev) =>
            prev.map((d) => (d.id === editingId ? { ...d, ...formData } : d))
          );
        }
        showNotification('success', `Đã cập nhật mầm bệnh "${formData.commonNameVi}" thành công.`);
      } else {
        // Create new
        const newRecord: DiseaseSummaryDto = {
          id: `disease-${Date.now()}`,
          ...formData,
        };
        try {
          const created = await ApiService.createDisease(formData);
          setDiseases((prev) => [created, ...prev]);
        } catch {
          // Local fallback
          setDiseases((prev) => [newRecord, ...prev]);
        }
        showNotification('success', `Đã thêm mầm bệnh mới "${formData.commonNameVi}".`);
      }
      handleCloseModal();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Không thể lưu mầm bệnh lúc này.';
      showNotification('error', msg);
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (id: string, name: string) => {
    if (!confirm(`Bạn có chắc chắn muốn xóa mầm bệnh "${name}" khỏi danh mục?`)) {
      return;
    }

    try {
      try {
        await ApiService.deleteDisease(id);
      } catch {
        // Local fallback
      }
      setDiseases((prev) => prev.filter((d) => d.id !== id));
      showNotification('success', `Đã xóa mầm bệnh "${name}" thành công.`);
    } catch {
      showNotification('error', 'Không thể xóa mầm bệnh lúc này.');
    }
  };

  const getSeverityBadge = (level?: SeverityLevel) => {
    switch (level) {
      case 'CRITICAL':
        return { label: 'Rất nguy cấp', color: 'bg-red-50 text-red-700 border-red-200' };
      case 'HIGH':
        return { label: 'Nghiêm trọng', color: 'bg-amber-50 text-amber-700 border-amber-200' };
      case 'MODERATE':
        return { label: 'Trung bình', color: 'bg-yellow-50 text-yellow-700 border-yellow-200' };
      case 'LOW':
      default:
        return { label: 'Nhẹ', color: 'bg-emerald-50 text-emerald-700 border-emerald-200' };
    }
  };

  const filtered = diseases.filter((d) => {
    const matchKeyword =
      d.commonNameVi.toLowerCase().includes(searchTerm.toLowerCase()) ||
      d.commonNameEn?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      d.scientificName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      d.diseaseCode.toLowerCase().includes(searchTerm.toLowerCase());

    const matchSeverity = severityFilter === 'ALL' || d.severityLevel === severityFilter;

    return matchKeyword && matchSeverity;
  });

  return (
    <div className="space-y-6">
      {/* Toast Notification */}
      {feedback && (
        <div
          className={`flex items-center gap-3 rounded-xl border p-4 shadow-sm transition-all ${
            feedback.type === 'success'
              ? 'border-emerald-200 bg-emerald-50 text-emerald-800'
              : 'border-red-200 bg-red-50 text-red-800'
          }`}
        >
          {feedback.type === 'success' ? (
            <CheckCircle2 className="h-5 w-5 text-emerald-600 shrink-0" />
          ) : (
            <AlertTriangle className="h-5 w-5 text-red-600 shrink-0" />
          )}
          <p className="text-sm font-medium">{feedback.message}</p>
        </div>
      )}

      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">
            Quản lý danh mục bệnh cây trồng
          </h1>
          <p className="mt-1 text-sm text-slate-500">
            Hồ sơ phân loại mầm bệnh thực vật, triệu chứng nông học và đồng bộ nhãn mô hình AI Roboflow.
          </p>
        </div>

        <button
          type="button"
          onClick={handleOpenAddModal}
          className="flex items-center justify-center gap-2 rounded-xl bg-emerald-600 px-4 py-2.5 text-xs font-semibold text-white shadow-sm transition hover:bg-emerald-700 active:scale-95"
        >
          <Plus className="h-4 w-4" />
          Thêm mầm bệnh mới
        </button>
      </div>

      {/* Filter and Search Bar */}
      <div className="flex flex-wrap items-center justify-between gap-4 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
        <div className="relative w-full sm:w-96">
          <Search className="absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Tìm theo tên bệnh, tên khoa học hoặc mã Roboflow..."
            className="h-10 w-full rounded-xl border border-slate-200 bg-slate-50 pl-10 pr-4 text-xs text-slate-800 placeholder-slate-400 transition focus:border-emerald-500 focus:bg-white focus:outline-none focus:ring-2 focus:ring-emerald-500/20"
          />
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <span className="text-xs font-semibold text-slate-500">Mức độ nguy hại:</span>
          {['ALL', 'CRITICAL', 'HIGH', 'MODERATE', 'LOW'].map((sev) => (
            <button
              key={sev}
              type="button"
              onClick={() => setSeverityFilter(sev)}
              className={`rounded-lg px-3 py-1.5 text-xs font-semibold transition ${
                severityFilter === sev
                  ? 'bg-emerald-600 text-white shadow-xs'
                  : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
              }`}
            >
              {sev === 'ALL'
                ? 'Tất cả'
                : sev === 'CRITICAL'
                ? 'Rất nguy cấp'
                : sev === 'HIGH'
                ? 'Nghiêm trọng'
                : sev === 'MODERATE'
                ? 'Trung bình'
                : 'Nhẹ'}
            </button>
          ))}
        </div>
      </div>

      {/* Disease Table */}
      <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-slate-200 bg-slate-50/75 text-xs font-bold text-slate-600">
                <th className="px-6 py-4">Tên bệnh hại</th>
                <th className="px-6 py-4">Tên khoa học</th>
                <th className="px-6 py-4">Mã phân loại AI</th>
                <th className="px-6 py-4">Mức độ nguy hại</th>
                <th className="px-6 py-4">Triệu chứng lâm sàng</th>
                <th className="px-6 py-4 text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 text-xs text-slate-700">
              {loading ? (
                <tr>
                  <td colSpan={6} className="py-12 text-center text-slate-400">
                    <Loader2 className="mx-auto h-6 w-6 animate-spin text-emerald-600 mb-2" />
                    Đang tải danh mục bệnh...
                  </td>
                </tr>
              ) : filtered.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-12 text-center text-slate-400">
                    Không tìm thấy mầm bệnh nào phù hợp với bộ lọc.
                  </td>
                </tr>
              ) : (
                filtered.map((item) => {
                  const badge = getSeverityBadge(item.severityLevel);
                  return (
                    <tr key={item.id} className="transition hover:bg-slate-50/60">
                      <td className="px-6 py-4">
                        <div className="font-bold text-slate-900">{item.commonNameVi}</div>
                        <div className="text-[11px] text-slate-400">{item.commonNameEn || '—'}</div>
                      </td>
                      <td className="px-6 py-4 italic text-slate-600">
                        {item.scientificName}
                      </td>
                      <td className="px-6 py-4">
                        <span className="rounded-md bg-slate-100 px-2 py-1 font-mono text-[11px] font-semibold text-slate-700 border border-slate-200">
                          {item.diseaseCode}
                        </span>
                      </td>
                      <td className="px-6 py-4">
                        <span
                          className={`inline-block rounded-md px-2.5 py-1 text-[11px] font-bold border ${badge.color}`}
                        >
                          {badge.label}
                        </span>
                      </td>
                      <td className="px-6 py-4 max-w-xs text-slate-500 line-clamp-2">
                        {item.symptomsVi || 'Chưa cập nhật mô tả triệu chứng.'}
                      </td>
                      <td className="px-6 py-4 text-right">
                        <div className="flex items-center justify-end gap-1.5">
                          <Link
                            href={`/treatments?diseaseCode=${item.diseaseCode}`}
                            className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-100 hover:text-emerald-600 transition"
                            title="Xem phác đồ điều trị"
                          >
                            <ExternalLink className="h-4 w-4" />
                          </Link>
                          <button
                            type="button"
                            onClick={() => handleOpenEditModal(item)}
                            className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-100 hover:text-blue-600 transition"
                            title="Chỉnh sửa thông tin"
                          >
                            <Edit2 className="h-4 w-4" />
                          </button>
                          <button
                            type="button"
                            onClick={() => handleDelete(item.id, item.commonNameVi)}
                            className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-100 hover:text-red-600 transition"
                            title="Xóa mầm bệnh"
                          >
                            <Trash2 className="h-4 w-4" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Modal: Add / Edit Disease */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-xs">
          <div className="relative w-full max-w-lg rounded-2xl border border-slate-200 bg-white p-6 shadow-xl animate-in fade-in zoom-in-95 duration-150">
            {/* Modal Header */}
            <div className="flex items-center justify-between border-b border-slate-100 pb-4">
              <div className="flex items-center gap-2.5">
                <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-emerald-100 text-emerald-700">
                  <Bug className="h-5 w-5" />
                </div>
                <div>
                  <h3 className="text-base font-bold text-slate-900">
                    {editingId ? 'Chỉnh sửa mầm bệnh' : 'Thêm mầm bệnh mới'}
                  </h3>
                  <p className="text-xs text-slate-500">
                    {editingId
                      ? 'Cập nhật phân loại và triệu chứng nhận diện'
                      : 'Đăng ký mã nhận diện AI và phác đồ mới'}
                  </p>
                </div>
              </div>
              <button
                type="button"
                onClick={handleCloseModal}
                className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-100 hover:text-slate-600"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            {/* Modal Form */}
            <form onSubmit={handleSubmit} className="mt-4 space-y-4 text-xs">
              <div>
                <label className="block font-semibold text-slate-700 mb-1">
                  Mã phân loại AI (diseaseCode) <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  required
                  disabled={!!editingId}
                  value={formData.diseaseCode}
                  onChange={(e) =>
                    setFormData({ ...formData, diseaseCode: e.target.value.toUpperCase() })
                  }
                  placeholder="Ví dụ: POTATO_LATE_BLIGHT, RICE_BLAST"
                  className="h-9 w-full rounded-lg border border-slate-200 bg-slate-50 px-3 font-mono text-xs text-slate-800 placeholder-slate-400 focus:border-emerald-500 focus:bg-white focus:outline-none focus:ring-1 focus:ring-emerald-500"
                />
                <span className="text-[10px] text-slate-400 mt-0.5 block">
                  Mã này phải khớp chính xác với lớp nhãn (class) trong mô hình Roboflow YOLO.
                </span>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block font-semibold text-slate-700 mb-1">
                    Tên tiếng Việt <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="text"
                    required
                    value={formData.commonNameVi}
                    onChange={(e) => setFormData({ ...formData, commonNameVi: e.target.value })}
                    placeholder="Bệnh mốc sương..."
                    className="h-9 w-full rounded-lg border border-slate-200 px-3 text-xs text-slate-800 placeholder-slate-400 focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
                  />
                </div>
                <div>
                  <label className="block font-semibold text-slate-700 mb-1">Tên tiếng Anh</label>
                  <input
                    type="text"
                    value={formData.commonNameEn}
                    onChange={(e) => setFormData({ ...formData, commonNameEn: e.target.value })}
                    placeholder="Potato Late Blight..."
                    className="h-9 w-full rounded-lg border border-slate-200 px-3 text-xs text-slate-800 placeholder-slate-400 focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block font-semibold text-slate-700 mb-1">
                    Tên khoa học (Scientific Name) <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="text"
                    required
                    value={formData.scientificName}
                    onChange={(e) => setFormData({ ...formData, scientificName: e.target.value })}
                    placeholder="Phytophthora infestans..."
                    className="h-9 w-full rounded-lg border border-slate-200 px-3 italic text-xs text-slate-800 placeholder-slate-400 focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
                  />
                </div>
                <div>
                  <label className="block font-semibold text-slate-700 mb-1">
                    Mức độ nguy hại
                  </label>
                  <select
                    value={formData.severityLevel}
                    onChange={(e) =>
                      setFormData({ ...formData, severityLevel: e.target.value as SeverityLevel })
                    }
                    className="h-9 w-full rounded-lg border border-slate-200 bg-white px-2.5 text-xs text-slate-800 focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
                  >
                    <option value="LOW">Nhẹ (LOW)</option>
                    <option value="MODERATE">Trung bình (MODERATE)</option>
                    <option value="HIGH">Nghiêm trọng (HIGH)</option>
                    <option value="CRITICAL">Rất nguy cấp (CRITICAL)</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block font-semibold text-slate-700 mb-1">
                  Mô tả triệu chứng lâm sàng
                </label>
                <textarea
                  rows={3}
                  value={formData.symptomsVi}
                  onChange={(e) => setFormData({ ...formData, symptomsVi: e.target.value })}
                  placeholder="Mô tả màu sắc đốm lá, viền vết bệnh, biểu hiện mặt dưới lá..."
                  className="w-full rounded-lg border border-slate-200 p-2.5 text-xs text-slate-800 placeholder-slate-400 focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
                />
              </div>

              {/* Modal Footer */}
              <div className="flex items-center justify-end gap-2.5 border-t border-slate-100 pt-4">
                <button
                  type="button"
                  onClick={handleCloseModal}
                  className="rounded-lg border border-slate-200 px-4 py-2 font-semibold text-slate-600 hover:bg-slate-50 transition"
                >
                  Hủy bỏ
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  className="flex items-center gap-1.5 rounded-lg bg-emerald-600 px-4 py-2 font-semibold text-white hover:bg-emerald-700 transition disabled:opacity-50"
                >
                  {submitting && <Loader2 className="h-3.5 w-3.5 animate-spin" />}
                  {editingId ? 'Lưu thay đổi' : 'Thêm mầm bệnh'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
