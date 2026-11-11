'use client';

import React, { useEffect, useState } from 'react';
import {
  Bug,
  Plus,
  Search,
  ExternalLink,
  ShieldAlert,
  Sparkles,
  Info,
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
];

export default function DiseasesPage() {
  const [diseases, setDiseases] = useState<DiseaseSummaryDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [severityFilter, setSeverityFilter] = useState<string>('ALL');

  useEffect(() => {
    async function fetchDiseases() {
      try {
        const data = await ApiService.getDiseases();
        setDiseases(data.length > 0 ? data : fallbackDiseases);
      } catch {
        setDiseases(fallbackDiseases);
      } finally {
        setLoading(false);
      }
    }
    fetchDiseases();
  }, []);

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
      d.commonNameEn.toLowerCase().includes(searchTerm.toLowerCase()) ||
      d.scientificName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      d.diseaseCode.toLowerCase().includes(searchTerm.toLowerCase());

    const matchSeverity = severityFilter === 'ALL' || d.severityLevel === severityFilter;

    return matchKeyword && matchSeverity;
  });

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">
            Quản lý danh mục bệnh cây trồng
          </h1>
          <p className="mt-1 text-sm text-slate-500">
            Hồ sơ phân loại mầm bệnh thực vật, triệu chứng nông học và đồng bộ nhãn mô hình AI.
          </p>
        </div>

        <button
          type="button"
          className="flex items-center gap-2 rounded-xl bg-emerald-600 px-4 py-2.5 text-xs font-semibold text-white shadow-sm transition hover:bg-emerald-700"
        >
          <Plus className="h-4 w-4" />
          Thêm mầm bệnh mới
        </button>
      </div>

      {/* Filter and Search Bar */}
      <div className="flex flex-wrap items-center justify-between gap-4 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
        <div className="relative w-96">
          <Search className="absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Tìm theo tên bệnh, tên khoa học hoặc mã Roboflow..."
            className="h-10 w-full rounded-xl border border-slate-200 bg-slate-50 pl-10 pr-4 text-xs text-slate-800 placeholder-slate-400 transition focus:border-emerald-500 focus:bg-white focus:outline-none focus:ring-2 focus:ring-emerald-500/20"
          />
        </div>

        <div className="flex items-center gap-2">
          <span className="text-xs font-semibold text-slate-500">Mức độ nguy hại:</span>
          {['ALL', 'CRITICAL', 'HIGH', 'MODERATE', 'LOW'].map((sev) => (
            <button
              key={sev}
              type="button"
              onClick={() => setSeverityFilter(sev)}
              className={`rounded-lg px-3 py-1.5 text-xs font-semibold transition ${
                severityFilter === sev
                  ? 'bg-emerald-600 text-white'
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
                      <div className="text-[11px] text-slate-400">{item.commonNameEn}</div>
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
                      <button
                        type="button"
                        className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-100 hover:text-slate-600"
                        title="Xem phác đồ điều trị"
                      >
                        <ExternalLink className="h-4 w-4" />
                      </button>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
