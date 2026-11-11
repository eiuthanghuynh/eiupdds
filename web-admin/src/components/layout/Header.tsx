'use client';

import React, { useEffect, useState } from 'react';
import { Bell, Search, Activity, User } from 'lucide-react';
import { ApiService } from '@/services/api';

export function Header() {
  const [isBackendOnline, setIsBackendOnline] = useState<boolean | null>(null);

  useEffect(() => {
    ApiService.checkHealth().then((status) => setIsBackendOnline(status));
  }, []);

  return (
    <header className="sticky top-0 z-40 flex h-16 w-full items-center justify-between border-b border-slate-200 bg-white/95 px-8 backdrop-blur-sm">
      {/* Search Input */}
      <div className="relative w-80">
        <Search className="absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
        <input
          type="text"
          placeholder="Tìm kiếm bệnh cây, hoạt chất, triệu chứng..."
          className="h-10 w-full rounded-xl border border-slate-200 bg-slate-50 pl-10 pr-4 text-xs text-slate-800 placeholder-slate-400 transition focus:border-emerald-500 focus:bg-white focus:outline-none focus:ring-2 focus:ring-emerald-500/20"
        />
      </div>

      {/* Right Controls */}
      <div className="flex items-center gap-4">
        {/* Backend Status Badge */}
        <div className="flex items-center gap-2 rounded-full border border-slate-200 bg-slate-50 px-3 py-1.5 text-xs">
          <Activity
            className={`h-3.5 w-3.5 ${
              isBackendOnline
                ? 'text-emerald-500 animate-pulse'
                : isBackendOnline === false
                ? 'text-amber-500'
                : 'text-slate-400'
            }`}
          />
          <span className="font-medium text-slate-600">
            {isBackendOnline ? 'Spring Boot 8080 (Online)' : 'API Backend'}
          </span>
        </div>

        {/* Notification Bell */}
        <button
          type="button"
          aria-label="Thông báo hệ thống"
          className="relative rounded-lg p-2 text-slate-500 hover:bg-slate-100 hover:text-slate-700"
        >
          <Bell className="h-4.5 w-4.5" />
          <span className="absolute right-1.5 top-1.5 h-2 w-2 rounded-full bg-emerald-600" />
        </button>

        {/* User Profile */}
        <div className="flex items-center gap-3 border-l border-slate-200 pl-4">
          <div className="flex h-9 w-9 items-center justify-center rounded-full bg-slate-900 text-white font-bold text-xs shadow-sm">
            <User className="h-4 w-4" />
          </div>
          <div>
            <span className="block text-xs font-bold text-slate-900 leading-tight">
              Bùi Tường Huy
            </span>
            <span className="block text-[11px] font-medium text-slate-500">
              Chuyên viên Frontend & Admin
            </span>
          </div>
        </div>
      </div>
    </header>
  );
}
