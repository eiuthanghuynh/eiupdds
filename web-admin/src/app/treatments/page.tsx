'use client';

import React, { useState } from 'react';
import {
  BookOpen,
  Plus,
  Leaf,
  FlaskConical,
  ShieldCheck,
  CheckCircle2,
  Clock,
  Sparkles,
  Search,
  Filter,
  Check,
  X,
  AlertCircle,
} from 'lucide-react';
import { TreatmentDto, TreatmentType } from '@/types';

interface DiseaseItem {
  id: string;
  name: string;
  code: string;
  plant: string;
}

const mockDiseases: DiseaseItem[] = [
  { id: 'd1', name: 'Bệnh mốc sương khoai tây', code: 'POTATO_LATE_BLIGHT', plant: 'Khoai tây' },
  { id: 'd2', name: 'Bệnh đốm vòng khoai tây', code: 'POTATO_EARLY_BLIGHT', plant: 'Khoai tây' },
  { id: 'd3', name: 'Bệnh đạo ôn lúa', code: 'RICE_BLAST', plant: 'Lúa nước' },
  { id: 'd4', name: 'Bệnh đốm lá cà chua', code: 'TOMATO_LEAF_SPOT', plant: 'Cà chua' },
];

const initialTreatments: (TreatmentDto & { diseaseId: string })[] = [
  {
    id: 'tr-001',
    diseaseId: 'd1',
    treatmentType: 'CHEMICAL',
    titleVi: 'Phun thuốc đặc trị nấm hoại sinh Metalaxyl + Mancozeb',
    contentVi:
      'Sử dụng các thuốc gốc Metalaxyl-M (80g/kg) kết hợp Mancozeb (640g/kg) như Ridomil Gold 68WG. Phun ướt đều tán lá cả hai mặt, ưu tiên mặt dưới lá nơi bào tử tập trung.',
    activeIngredients: 'Metalaxyl-M 40g/kg + Mancozeb 640g/kg',
    dosageInstructions: '50g pha cho bình 16 lít nước, phun định kỳ cách nhau 5-7 ngày trong thời tiết sương ẩm.',
    safetyIntervalDays: 7,
  },
  {
    id: 'tr-002',
    diseaseId: 'd1',
    treatmentType: 'BIOLOGICAL',
    titleVi: 'Chế phẩm nấm đối kháng Trichoderma viride',
    contentVi:
      'Phun sương dung dịch vi sinh nấm Trichoderma kết hợp vi khuẩn đối kháng Bacillus subtilis để tiêu diệt màng sợi nấm tự nhiên, an toàn cho môi trường.',
    activeIngredients: 'Bào tử sống Trichoderma viride (10^9 CFU/g)',
    dosageInstructions: '30g cho bình 16 lít, phun vào lúc trời râm mát hoặc sau mưa rào.',
    safetyIntervalDays: 0,
  },
  {
    id: 'tr-003',
    diseaseId: 'd1',
    treatmentType: 'CULTURAL',
    titleVi: 'Cắt tỉa bộ phận nhiễm bệnh & cải tạo lưu thông gió',
    contentVi:
      'Khẩn trương cắt tỉa các nhánh lá bị thâm đen nặng gom vào bao tiêu hủy hoặc chôn rải vôi bột; dừng bón phân đạm (N) và không tưới phun mưa vào buổi chiều tối.',
    activeIngredients: 'Không sử dụng hóa chất',
    dosageInstructions: 'Thực hiện ngay trong vòng 24 giờ sau khi phát hiện bệnh.',
    safetyIntervalDays: 0,
  },
  {
    id: 'tr-004',
    diseaseId: 'd3',
    treatmentType: 'CHEMICAL',
    titleVi: 'Đặc trị đạo ôn lá bằng hoạt chất Tricyclazole',
    contentVi:
      'Phun thuốc đặc trị đạo ôn như Beam 75WP hoặc Flash 75WP khi phát hiện chớm có vết bệnh hình mắt én trên lá lúa.',
    activeIngredients: 'Tricyclazole 750g/kg',
    dosageInstructions: '15-20g cho bình 16 lít nước, phun 2 bình cho 1 sào (500m²).',
    safetyIntervalDays: 14,
  },
];

export default function TreatmentsPage() {
  const [selectedDiseaseId, setSelectedDiseaseId] = useState<string>('d1');
  const [typeFilter, setTypeFilter] = useState<string>('ALL');
  const [treatments, setTreatments] = useState(initialTreatments);
  const [showAddModal, setShowAddModal] = useState(false);

  // New Treatment Form State
  const [formType, setFormType] = useState<TreatmentType>('CHEMICAL');
  const [formTitle, setFormTitle] = useState('');
  const [formContent, setFormContent] = useState('');
  const [formIngredients, setFormIngredients] = useState('');
  const [formDosage, setFormDosage] = useState('');
  const [formInterval, setFormInterval] = useState('7');

  const selectedDisease = mockDiseases.find((d) => d.id === selectedDiseaseId) || mockDiseases[0];

  const filteredTreatments = treatments.filter((t) => {
    const matchDisease = t.diseaseId === selectedDiseaseId;
    const matchType = typeFilter === 'ALL' || t.treatmentType === typeFilter;
    return matchDisease && matchType;
  });

  const getTreatmentBadge = (type: TreatmentType) => {
    switch (type) {
      case 'BIOLOGICAL':
        return {
          label: 'Biện pháp sinh học',
          icon: Leaf,
          color: 'bg-emerald-50 text-emerald-700 border-emerald-200',
        };
      case 'CHEMICAL':
        return {
          label: 'Biện pháp hóa học',
          icon: FlaskConical,
          color: 'bg-red-50 text-red-700 border-red-200',
        };
      case 'PREVENTIVE':
        return {
          label: 'Biện pháp phòng ngừa',
          icon: ShieldCheck,
          color: 'bg-blue-50 text-blue-700 border-blue-200',
        };
      case 'CULTURAL':
      default:
        return {
          label: 'Biện pháp canh tác',
          icon: CheckCircle2,
          color: 'bg-amber-50 text-amber-700 border-amber-200',
        };
    }
  };

  const handleAddTreatment = (e: React.FormEvent) => {
    e.preventDefault();
    if (!formTitle || !formContent) return;

    const newTreatment: TreatmentDto & { diseaseId: string } = {
      id: `tr-${Date.now()}`,
      diseaseId: selectedDiseaseId,
      treatmentType: formType,
      titleVi: formTitle,
      contentVi: formContent,
      activeIngredients: formIngredients || null,
      dosageInstructions: formDosage || null,
      safetyIntervalDays: formInterval ? parseInt(formInterval, 10) : null,
    };

    setTreatments([newTreatment, ...treatments]);
    setShowAddModal(false);
    setFormTitle('');
    setFormContent('');
    setFormIngredients('');
    setFormDosage('');
  };

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">
            Quản lý phác đồ điều trị nông nghiệp
          </h1>
          <p className="mt-1 text-sm text-slate-500">
            Thiết lập các biện pháp sinh học, hóa học và kỹ thuật canh tác hiển thị xuống ứng dụng di động cho nông dân.
          </p>
        </div>

        <button
          type="button"
          onClick={() => setShowAddModal(true)}
          className="flex items-center gap-2 rounded-xl bg-emerald-600 px-4 py-2.5 text-xs font-semibold text-white shadow-sm transition hover:bg-emerald-700"
        >
          <Plus className="h-4 w-4" />
          Thêm phác đồ mới
        </button>
      </div>

      {/* Disease Selector Cards */}
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
        {mockDiseases.map((d) => {
          const isSelected = d.id === selectedDiseaseId;
          const count = treatments.filter((t) => t.diseaseId === d.id).length;

          return (
            <button
              key={d.id}
              type="button"
              onClick={() => setSelectedDiseaseId(d.id)}
              className={`flex flex-col text-left rounded-2xl border p-4 transition ${
                isSelected
                  ? 'border-emerald-600 bg-emerald-50/70 shadow-sm ring-2 ring-emerald-600/20'
                  : 'border-slate-200 bg-white hover:border-slate-300'
              }`}
            >
              <div className="flex items-center justify-between">
                <span className="text-[11px] font-bold uppercase tracking-wider text-emerald-700">
                  {d.plant}
                </span>
                <span className="rounded-full bg-slate-100 px-2 py-0.5 text-[11px] font-bold text-slate-600">
                  {count} phác đồ
                </span>
              </div>
              <h4 className="mt-2 text-sm font-bold text-slate-900 leading-snug">
                {d.name}
              </h4>
              <span className="mt-1 text-[11px] font-mono text-slate-400">
                {d.code}
              </span>
            </button>
          );
        })}
      </div>

      {/* Treatment Type Filter Bar */}
      <div className="flex flex-wrap items-center justify-between gap-4 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
        <div className="flex items-center gap-2">
          <Filter className="h-4 w-4 text-slate-400" />
          <span className="text-xs font-semibold text-slate-600">Phân loại biện pháp:</span>
          {[
            { id: 'ALL', label: 'Tất cả' },
            { id: 'CHEMICAL', label: 'Hóa học' },
            { id: 'BIOLOGICAL', label: 'Sinh học' },
            { id: 'CULTURAL', label: 'Canh tác' },
            { id: 'PREVENTIVE', label: 'Phòng ngừa' },
          ].map((type) => (
            <button
              key={type.id}
              type="button"
              onClick={() => setTypeFilter(type.id)}
              className={`rounded-lg px-3 py-1.5 text-xs font-semibold transition ${
                typeFilter === type.id
                  ? 'bg-slate-900 text-white shadow-sm'
                  : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
              }`}
            >
              {type.label}
            </button>
          ))}
        </div>

        <span className="text-xs text-slate-400 font-medium">
          Hiển thị {filteredTreatments.length} phác đồ cho {selectedDisease.name}
        </span>
      </div>

      {/* Treatments List */}
      <div className="space-y-4">
        {filteredTreatments.length === 0 ? (
          <div className="rounded-2xl border border-dashed border-slate-300 bg-white py-16 text-center shadow-sm">
            <BookOpen className="mx-auto h-10 w-10 text-slate-300" />
            <h3 className="mt-3 text-sm font-bold text-slate-700">
              Chưa có phác đồ điều trị cho danh mục này
            </h3>
            <p className="mt-1 text-xs text-slate-500">
              Bấm nút &quot;Thêm phác đồ mới&quot; phía trên để thiết lập quy chuẩn kỹ thuật cho bệnh này.
            </p>
          </div>
        ) : (
          filteredTreatments.map((tr) => {
            const badge = getTreatmentBadge(tr.treatmentType);
            const Icon = badge.icon;

            return (
              <div
                key={tr.id}
                className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm transition hover:shadow-md"
              >
                <div className="flex items-center justify-between border-b border-slate-100 pb-3.5">
                  <div className="flex items-center gap-2.5">
                    <span
                      className={`flex items-center gap-1.5 rounded-lg border px-2.5 py-1 text-xs font-bold ${badge.color}`}
                    >
                      <Icon className="h-3.5 w-3.5" />
                      {badge.label}
                    </span>
                    <h3 className="text-base font-bold text-slate-900">{tr.titleVi}</h3>
                  </div>

                  {tr.safetyIntervalDays !== null && tr.safetyIntervalDays !== undefined && (
                    <div className="flex items-center gap-1.5 text-xs font-semibold text-slate-600 bg-slate-50 border border-slate-200 rounded-lg px-2.5 py-1">
                      <Clock className="h-3.5 w-3.5 text-amber-500" />
                      Cách ly an toàn: {tr.safetyIntervalDays} ngày
                    </div>
                  )}
                </div>

                <p className="mt-3.5 text-sm leading-relaxed text-slate-600">
                  {tr.contentVi}
                </p>

                {/* Sub info boxes */}
                <div className="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-2 rounded-xl bg-slate-50/75 p-3.5 border border-slate-100 text-xs">
                  {tr.activeIngredients && (
                    <div>
                      <span className="font-semibold text-slate-700">Hoạt chất chính: </span>
                      <span className="text-slate-600">{tr.activeIngredients}</span>
                    </div>
                  )}
                  {tr.dosageInstructions && (
                    <div>
                      <span className="font-semibold text-slate-700">Liều lượng khuyến cáo: </span>
                      <span className="text-slate-600">{tr.dosageInstructions}</span>
                    </div>
                  )}
                </div>
              </div>
            );
          })
        )}
      </div>

      {/* Add Treatment Modal */}
      {showAddModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 backdrop-blur-xs p-4">
          <div className="w-full max-w-xl rounded-2xl bg-white p-6 shadow-xl border border-slate-200">
            <div className="flex items-center justify-between border-b border-slate-100 pb-3.5">
              <div>
                <h3 className="text-base font-bold text-slate-900">
                  Thêm phác đồ điều trị mới
                </h3>
                <p className="text-xs text-slate-500">
                  Gắn liền với: <span className="font-bold text-emerald-600">{selectedDisease.name}</span>
                </p>
              </div>
              <button
                type="button"
                onClick={() => setShowAddModal(false)}
                className="rounded-lg p-1 text-slate-400 hover:bg-slate-100 hover:text-slate-600"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            <form onSubmit={handleAddTreatment} className="mt-4 space-y-4 text-xs">
              <div>
                <label className="block font-semibold text-slate-700 mb-1">
                  Phân loại biện pháp
                </label>
                <select
                  value={formType}
                  onChange={(e) => setFormType(e.target.value as TreatmentType)}
                  className="w-full h-10 rounded-xl border border-slate-200 bg-slate-50 px-3 text-xs text-slate-800 focus:bg-white focus:outline-none focus:border-emerald-500"
                >
                  <option value="CHEMICAL">Biện pháp hóa học (Thuốc BVTV)</option>
                  <option value="BIOLOGICAL">Biện pháp sinh học (Chế phẩm vi sinh)</option>
                  <option value="CULTURAL">Biện pháp canh tác (Kỹ thuật nông học)</option>
                  <option value="PREVENTIVE">Biện pháp phòng ngừa (Khử trùng, giống)</option>
                </select>
              </div>

              <div>
                <label className="block font-semibold text-slate-700 mb-1">
                  Tiêu đề phác đồ điều trị *
                </label>
                <input
                  type="text"
                  required
                  value={formTitle}
                  onChange={(e) => setFormTitle(e.target.value)}
                  placeholder="Ví dụ: Phun phòng ngừa nấm hoại sinh Metalaxyl..."
                  className="w-full h-10 rounded-xl border border-slate-200 bg-slate-50 px-3 text-xs text-slate-800 focus:bg-white focus:outline-none focus:border-emerald-500"
                />
              </div>

              <div>
                <label className="block font-semibold text-slate-700 mb-1">
                  Nội dung hướng dẫn chi tiết *
                </label>
                <textarea
                  required
                  rows={3}
                  value={formContent}
                  onChange={(e) => setFormContent(e.target.value)}
                  placeholder="Mô tả cụ thể cách phun, thời điểm và vùng tán lá cần xử lý..."
                  className="w-full rounded-xl border border-slate-200 bg-slate-50 p-3 text-xs text-slate-800 focus:bg-white focus:outline-none focus:border-emerald-500"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-semibold text-slate-700 mb-1">
                    Hoạt chất chính (nếu có)
                  </label>
                  <input
                    type="text"
                    value={formIngredients}
                    onChange={(e) => setFormIngredients(e.target.value)}
                    placeholder="Ví dụ: Mancozeb 640g/kg..."
                    className="w-full h-10 rounded-xl border border-slate-200 bg-slate-50 px-3 text-xs text-slate-800 focus:bg-white focus:outline-none focus:border-emerald-500"
                  />
                </div>

                <div>
                  <label className="block font-semibold text-slate-700 mb-1">
                    Thời gian cách ly (ngày)
                  </label>
                  <input
                    type="number"
                    min="0"
                    value={formInterval}
                    onChange={(e) => setFormInterval(e.target.value)}
                    className="w-full h-10 rounded-xl border border-slate-200 bg-slate-50 px-3 text-xs text-slate-800 focus:bg-white focus:outline-none focus:border-emerald-500"
                  />
                </div>
              </div>

              <div>
                <label className="block font-semibold text-slate-700 mb-1">
                  Liều lượng khuyến cáo
                </label>
                <input
                  type="text"
                  value={formDosage}
                  onChange={(e) => setFormDosage(e.target.value)}
                  placeholder="Ví dụ: 50g cho bình 16 lít nước, phun ướt đều..."
                  className="w-full h-10 rounded-xl border border-slate-200 bg-slate-50 px-3 text-xs text-slate-800 focus:bg-white focus:outline-none focus:border-emerald-500"
                />
              </div>

              <div className="flex justify-end gap-2.5 pt-3 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setShowAddModal(false)}
                  className="rounded-xl px-4 py-2 font-semibold text-slate-600 hover:bg-slate-100"
                >
                  Hủy bỏ
                </button>
                <button
                  type="submit"
                  className="rounded-xl bg-emerald-600 px-4 py-2 font-semibold text-white shadow-sm hover:bg-emerald-700"
                >
                  Lưu phác đồ điều trị
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
