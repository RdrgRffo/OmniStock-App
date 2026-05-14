import type { ReactNode } from 'react';
import { Layers, DollarSign, BoxesIcon, BookOpen } from 'lucide-react';

export type AnalyticsTabId = 'oportunidades' | 'precios' | 'stock' | 'catalogo';

export const ANALYTICS_TABS: { id: AnalyticsTabId; label: string; icon: ReactNode }[] = [
  { id: 'oportunidades', label: 'Oportunidades', icon: <Layers size={16} /> },
  { id: 'precios', label: 'Precios', icon: <DollarSign size={16} /> },
  { id: 'stock', label: 'Stock', icon: <BoxesIcon size={16} /> },
  { id: 'catalogo', label: 'Catálogo', icon: <BookOpen size={16} /> },
];
