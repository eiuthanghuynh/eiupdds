'use client';

import React from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import {
  LayoutDashboard,
  Bug,
  BookOpen,
  MapPin,
  Settings,
  Leaf,
  ShieldAlert,
} from 'lucide-react';

const navigationItems = [
  { name: 'Tổng quan', href: '/', icon: LayoutDashboard },
  { name: 'Danh mục bệnh cây', href: '/diseases', icon: Bug },
  { name: 'Phác đồ điều trị', href: '/treatments', icon: BookOpen },
  { name: 'Bản đồ ca bệnh', href: '/diagnoses', icon: MapPin },
  { name: 'Cài đặt hệ thống', href: '/settings', icon: Settings },
];

export function Sidebar() {
  const pathname = usePathname();

  return (
    <aside className="fixed inset-y-0 left-0 z-50 flex w-64 flex-col border-r border-slate-200 bg-white">
      {/* Brand Header */}
      <div className="flex h-16 items-center gap-3 border-b border-slate-200 px-6">
        <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-emerald-600 text-white shadow-md shadow-emerald-600/20">
          <Leaf className="h-5 w-5" />
        </div>
        <div>
          <span className="text-lg font-bold tracking-tight text-slate-900">
            EIU PDDS
          </span>
          <span className="block text-[11px] font-semibold text-emerald-600">
            Admin Portal
          </span>
        </div>
      </div>

      {/* Nav List */}
      <nav className="flex-1 space-y-1.5 px-3 py-4">
        {navigationItems.map((item) => {
          const isActive = pathname === item.href;
          const Icon = item.icon;

          return (
            <Link
              key={item.href}
              href={item.href}
              className={`flex items-center gap-3 rounded-lg px-3.5 py-2.5 text-sm font-medium transition-colors ${
                isActive
                  ? 'bg-emerald-50 text-emerald-700 font-semibold'
                  : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900'
              }`}
            >
              <Icon
                className={`h-4.5 w-4.5 ${
                  isActive ? 'text-emerald-600' : 'text-slate-400'
                }`}
              />
              {item.name}
            </Link>
          );
        })}
      </nav>

      {/* Footer Info Box */}
      <div className="border-t border-slate-200 p-4">
        <div className="rounded-xl border border-emerald-100 bg-emerald-50/60 p-3.5">
          <div className="flex items-center gap-2 text-emerald-800">
            <ShieldAlert className="h-4 w-4 text-emerald-600" />
            <span className="text-xs font-semibold">Chẩn đoán thông minh</span>
          </div>
          <p className="mt-1 text-[11px] leading-relaxed text-slate-600">
            Mô hình Roboflow YOLOv8x kết hợp xử lý song song với Java 21 Loom.
          </p>
        </div>
      </div>
    </aside>
  );
}
