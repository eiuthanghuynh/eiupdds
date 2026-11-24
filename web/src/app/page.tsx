import React from 'react';
import { SeedhouseHeader } from '@/components/layout/SeedhouseHeader';
import { SeedhouseFooter } from '@/components/layout/SeedhouseFooter';
import { DiagnosticWorkbench } from '@/components/diagnosis/DiagnosticWorkbench';
import { HowItWorksBand } from '@/components/catalog/HowItWorksBand';
import { CropDiseaseAlmanac } from '@/components/catalog/CropDiseaseAlmanac';
import { SEEDHOUSE_COLORS } from '@/lib/theme/seedhouseTheme';

export default function HomePage(): React.JSX.Element {
  return (
    <div className="min-h-screen flex flex-col">
      <SeedhouseHeader />
      <main className="flex-1">
        <DiagnosticWorkbench />
        <HowItWorksBand />
        <CropDiseaseAlmanac />
      </main>
      <SeedhouseFooter topSurfaceFill={SEEDHOUSE_COLORS.parchment} />
    </div>
  );
}
