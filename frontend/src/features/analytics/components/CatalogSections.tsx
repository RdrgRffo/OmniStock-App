import { useQuery } from '@tanstack/react-query';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip,
  Legend, ResponsiveContainer, AreaChart, Area,
} from 'recharts';
import { ConditionMixDto } from '@/types';
import { getMoqDistribution, getConditionMix, getCatalogGrowth } from '@/features/dashboard/services/analyticsService';

const CONDITION_COLORS = { NEW: '#10b981', REFURBISHED: '#3b82f6', BOX_DAMAGED: '#f59e0b', USED: '#ef4444' };

export const MoqDistributionSection = () => {
  const { data, isLoading, error } = useQuery({
    queryKey: ['analytics-moq'],
    queryFn: getMoqDistribution,
    staleTime: 5 * 60 * 1000,
  });
  if (isLoading) return <div className="centered-message">Calculando MOQ...</div>;
  if (error || !data) return <div className="centered-message error-message">Error al cargar MOQ Distribution.</div>;
  return (
    <div className="dashboard-card">
      <h3 className="card-title">MOQ Distribution — Cantidad mínima de pedido por proveedor</h3>
      {data.length === 0 ? (
        <div className="centered-message">Sin datos de MOQ.</div>
      ) : (
        <ResponsiveContainer width="100%" height={220}>
          <BarChart data={data} margin={{ top: 5, right: 20, left: 0, bottom: 5 }}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="supplierName" tick={{ fontSize: 11 }} />
            <YAxis tick={{ fontSize: 12 }} />
            <Tooltip formatter={(v: number | undefined) => [(v ?? 0).toFixed(1), 'Avg MOQ']} />
            <Legend />
            <Bar dataKey="avgMoq" fill="#6366f1" name="MOQ Promedio" radius={[4, 4, 0, 0]} />
          </BarChart>
        </ResponsiveContainer>
      )}
      <p style={{ fontSize: '0.72rem', color: '#9ca3af', marginTop: '0.5rem' }}>
        MOQ alto = barrera de compra. Considera negociar pedidos mínimos con proveedores de MOQ &gt; 10.
      </p>
    </div>
  );
};

export const ConditionMixSection = () => {
  const { data, isLoading, error } = useQuery({
    queryKey: ['analytics-condition-mix'],
    queryFn: getConditionMix,
    staleTime: 5 * 60 * 1000,
  });
  if (isLoading) return <div className="centered-message">Calculando Condition Mix...</div>;
  if (error || !data) return <div className="centered-message error-message">Error al cargar Condition Mix.</div>;
  const chartData = data.map((r: ConditionMixDto) => ({
    name: r.supplierName,
    NEW: r.newPct,
    REFURBISHED: r.refurbishedPct,
    BOX_DAMAGED: r.boxDamagedPct,
    USED: r.usedPct,
  }));
  return (
    <div className="dashboard-card">
      <h3 className="card-title">Condition Mix — Distribución de condición de productos por proveedor</h3>
      {chartData.length === 0 ? (
        <div className="centered-message">Sin datos de condición.</div>
      ) : (
        <ResponsiveContainer width="100%" height={240}>
          <BarChart data={chartData} margin={{ top: 5, right: 20, left: 0, bottom: 5 }}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="name" tick={{ fontSize: 11 }} />
            <YAxis tickFormatter={(v) => `${v}%`} tick={{ fontSize: 12 }} domain={[0, 100]} />
            <Tooltip formatter={(v: number | undefined) => [`${(v ?? 0).toFixed(1)}%`]} />
            <Legend />
            {Object.entries(CONDITION_COLORS).map(([key, color]) => (
              <Bar key={key} dataKey={key} stackId="a" fill={color} name={key} radius={key === 'USED' ? [4, 4, 0, 0] : [0, 0, 0, 0]} />
            ))}
          </BarChart>
        </ResponsiveContainer>
      )}
    </div>
  );
};

export const CatalogGrowthSection = () => {
  const { data, isLoading, error } = useQuery({
    queryKey: ['analytics-catalog-growth'],
    queryFn: () => getCatalogGrowth(12),
    staleTime: 5 * 60 * 1000,
  });
  if (isLoading) return <div className="centered-message">Calculando crecimiento del catálogo...</div>;
  if (error || !data) return <div className="centered-message error-message">Error al cargar Catalog Growth.</div>;
  return (
    <div className="dashboard-card">
      <h3 className="card-title">Catalog Growth Trend — Productos nuevos por semana (últimas 12 semanas)</h3>
      {data.length === 0 ? (
        <p style={{ color: '#6b7280', fontSize: '0.875rem' }}>Sin datos de crecimiento del catálogo aún.</p>
      ) : (
        <ResponsiveContainer width="100%" height={220}>
          <AreaChart data={data} margin={{ top: 5, right: 20, left: 0, bottom: 5 }}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="weekLabel" tick={{ fontSize: 11 }} />
            <YAxis tick={{ fontSize: 12 }} />
            <Tooltip formatter={(v: number | undefined) => [v ?? 0, 'Productos nuevos']} />
            <Area type="monotone" dataKey="newProducts" stroke="#6366f1" fill="#e0e7ff" name="Productos nuevos" />
          </AreaChart>
        </ResponsiveContainer>
      )}
    </div>
  );
};
