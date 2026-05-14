import { useState, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip,
  Legend, ResponsiveContainer, Cell,
} from 'recharts';
import { PriceIndexVariationDto, PriceDispersionDto, PriceStabilityDto } from '@/types';
import { getPriceIndexVariation, getPriceDispersion, getPriceStability } from '@/features/dashboard/services/analyticsService';
import { DataTable } from '@/components/ui/DataTable';
import { formatCurrency } from '@/utils/formatting';

const COLORS = ['#6366f1', '#f59e0b', '#10b981', '#ef4444', '#3b82f6', '#8b5cf6'];

export const PriceVariationChart = ({ data }: { data?: PriceIndexVariationDto[] }) => {
  const [monthsFilter, setMonthsFilter] = useState(6);
  const [lastValid, setLastValid] = useState<PriceIndexVariationDto[]>(data ?? []);
  const { data: fresh, isLoading, error } = useQuery({
    queryKey: ['analytics-price-variation', monthsFilter],
    queryFn: () => getPriceIndexVariation(monthsFilter, 50),
    initialData: data,
  });

  useEffect(() => {
    if (fresh && fresh.length > 0) setLastValid(fresh);
  }, [fresh]);

  const showData = (!error && fresh && fresh.length > 0) ? fresh : lastValid;

  if (isLoading && !showData.length) return <div className="centered-message">Calculando variaciones...</div>;
  if (error && !showData.length) return <div className="centered-message error-message">Error al cargar variaciones de precio.</div>;

  const byMonth: Record<string, Record<string, number[]>> = {};
  const suppliers = new Set<string>();
  for (const row of showData) {
    if (row.variationPct === null) continue;
    if (!byMonth[row.month]) byMonth[row.month] = {};
    if (!byMonth[row.month][row.supplierName]) byMonth[row.month][row.supplierName] = [];
    byMonth[row.month][row.supplierName].push(row.variationPct);
    suppliers.add(row.supplierName);
  }

  const chartData = Object.entries(byMonth)
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([month, supplierMap]) => {
      const entry: Record<string, number | string> = { month };
      for (const [supplier, values] of Object.entries(supplierMap)) {
        entry[supplier] = parseFloat((values.reduce((a, b) => a + b, 0) / values.length).toFixed(2));
      }
      return entry;
    });

  const supplierList = Array.from(suppliers);
  const outlierCount = showData.filter((r) => r.isOutlier).length;

  return (
    <div className="dashboard-card">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '1rem' }}>
        <h3 className="card-title" style={{ margin: 0 }}>Variación Mensual de Índice de Precios</h3>
        <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
          {outlierCount > 0 && (
            <span style={{ fontSize: '0.75rem', background: '#fef3c7', color: '#92400e', padding: '2px 8px', borderRadius: '9999px' }}>
              {outlierCount} outliers filtrados
            </span>
          )}
          <select
            value={monthsFilter}
            onChange={(e) => setMonthsFilter(Number(e.target.value))}
            style={{ fontSize: '0.8rem', padding: '4px 8px', borderRadius: '6px', border: '1px solid #e5e7eb' }}
          >
            <option value={3}>3 meses</option>
            <option value={6}>6 meses</option>
            <option value={12}>12 meses</option>
          </select>
        </div>
      </div>
      {chartData.length === 0 ? (
        <div className="centered-message">Sin datos de historial de precios en el periodo seleccionado.</div>
      ) : (
        <div style={{ width: '100%', minHeight: 280 }}>
          <ResponsiveContainer width="100%" height={280}>
            <BarChart data={chartData} margin={{ top: 5, right: 20, left: 0, bottom: 5 }}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="month" tick={{ fontSize: 12 }} />
              <YAxis tickFormatter={(v) => `${v}%`} tick={{ fontSize: 12 }} />
              <Tooltip formatter={(v: number | undefined) => [`${(v ?? 0).toFixed(2)}%`, 'Variación avg']} />
              <Legend />
              {supplierList.map((name, i) => (
                <Bar key={name} dataKey={name} fill={COLORS[i % COLORS.length]} radius={[3, 3, 0, 0]}>
                  {chartData.map((entry, idx) => {
                    const val = entry[name] as number;
                    const isHigh = val > 20;
                    return <Cell key={idx} fill={isHigh ? '#ef4444' : COLORS[i % COLORS.length]} />;
                  })}
                </Bar>
              ))}
            </BarChart>
          </ResponsiveContainer>
        </div>
      )}
      <p style={{ fontSize: '0.72rem', color: '#9ca3af', marginTop: '0.5rem' }}>
        Barras en rojo: variacion &gt;20%. Outliers (&gt;50%) excluidos automaticamente.
        Precios normalizados a EUR via tabla currency_rates.
      </p>
    </div>
  );
};

export const PriceDispersionSection = () => {
  const { data, isLoading, error } = useQuery({
    queryKey: ['analytics-price-dispersion'],
    queryFn: () => getPriceDispersion(100),
    staleTime: 5 * 60 * 1000,
  });
  if (isLoading) return <div className="centered-message">Calculando dispersión de precios...</div>;
  if (error || !data) return <div className="centered-message error-message">Error al cargar Price Dispersion.</div>;
  return (
    <div className="dashboard-card" style={{ overflowX: 'auto' }}>
      <h3 className="card-title">Price Dispersion Index — SKUs con mayor diferencia de precio entre proveedores</h3>
      <DataTable
        columns={[
          { header: 'MPN', sortKey: 'mpn', cell: (r: PriceDispersionDto) => <span className="font-mono text-xs">{r.mpn}</span> },
          { header: 'Categoría', sortKey: 'category', cell: (r: PriceDispersionDto) => r.category ?? '—' },
          { header: 'Proveedores', sortKey: 'supplierCount', cell: (r: PriceDispersionDto) => r.supplierCount, align: 'center' },
          { header: 'Min €', sortKey: 'minPrice', cell: (r: PriceDispersionDto) => formatCurrency(r.minPrice), align: 'right' },
          { header: 'Max €', sortKey: 'maxPrice', cell: (r: PriceDispersionDto) => formatCurrency(r.maxPrice), align: 'right' },
          { header: 'Avg €', sortKey: 'avgPrice', cell: (r: PriceDispersionDto) => formatCurrency(r.avgPrice), align: 'right' },
          {
            header: 'Dispersión',
            sortKey: 'dispersionPct',
            align: 'right',
            cell: (r: PriceDispersionDto) => (
              <span style={{ fontWeight: 700, color: (r.dispersionPct ?? 0) > 30 ? '#dc2626' : (r.dispersionPct ?? 0) > 15 ? '#f59e0b' : '#10b981' }}>
                {(r.dispersionPct ?? 0).toFixed(1)}%
              </span>
            ),
          },
        ]}
        data={data}
        emptyMessage="No hay SKUs con 2+ proveedores todavía."
        keyExtractor={(r: PriceDispersionDto) => r.productId}
        defaultSortKey="dispersionPct"
        defaultSortDirection="desc"
      />
      <p style={{ fontSize: '0.72rem', color: '#9ca3af', marginTop: '0.5rem' }}>
        Solo SKUs con 2+ proveedores. Alto % = oportunidad de negociación.
      </p>
    </div>
  );
};

const stabilityBadgeStyle = (label: string) => {
  if (label === 'VOLATIL') return { bg: '#fef2f2', color: '#b91c1c' };
  if (label === 'MODERADO') return { bg: '#fefce8', color: '#92400e' };
  return { bg: '#f0fdf4', color: '#166534' };
};

export const PriceStabilitySection = () => {
  const { data, isLoading, error } = useQuery({
    queryKey: ['analytics-price-stability'],
    queryFn: () => getPriceStability(90, 100),
    staleTime: 5 * 60 * 1000,
  });
  if (isLoading) return <div className="centered-message">Calculando estabilidad de precios...</div>;
  if (error || !data) return <div className="centered-message error-message">Error al cargar Price Stability.</div>;
  return (
    <div className="dashboard-card" style={{ overflowX: 'auto' }}>
      <h3 className="card-title">Price Stability Score — SKUs por coeficiente de variación (90 días)</h3>
      {data.length === 0 ? (
        <p style={{ color: '#6b7280', fontSize: '0.875rem' }}>
          Insuficiente historial de precios. Necesita al menos 2 registros por SKU/proveedor.
        </p>
      ) : (
        <DataTable
          columns={[
            { header: 'MPN', sortKey: 'mpn', cell: (r: PriceStabilityDto) => <span className="font-mono text-xs">{r.mpn}</span> },
            { header: 'Proveedor', sortKey: 'supplierName', cell: (r: PriceStabilityDto) => r.supplierName },
            { header: 'Precio Avg', sortKey: 'avgPrice', cell: (r: PriceStabilityDto) => formatCurrency(r.avgPrice), align: 'right' },
            { header: 'Stddev', sortKey: 'stddevPrice', cell: (r: PriceStabilityDto) => formatCurrency(r.stddevPrice), align: 'right' },
            { header: 'CV %', sortKey: 'cvPct', cell: (r: PriceStabilityDto) => `${(r.cvPct ?? 0).toFixed(1)}%`, align: 'center' },
            { header: 'Puntos', sortKey: 'pricePoints', cell: (r: PriceStabilityDto) => r.pricePoints, align: 'center' },
            {
              header: 'Estabilidad',
              sortKey: 'stabilityLabel',
              align: 'center',
              cell: (r: PriceStabilityDto) => {
                const s = stabilityBadgeStyle(r.stabilityLabel);
                return (
                  <span style={{ background: s.bg, color: s.color, fontWeight: 700, fontSize: '0.7rem', padding: '2px 8px', borderRadius: '9999px' }}>
                    {r.stabilityLabel}
                  </span>
                );
              },
            },
          ]}
          data={data}
          emptyMessage="Sin historial suficiente."
          keyExtractor={(r: PriceStabilityDto) => `${r.productId}-${r.supplierId}`}
          defaultSortKey="cvPct"
          defaultSortDirection="desc"
        />
      )}
      <p style={{ fontSize: '0.72rem', color: '#9ca3af', marginTop: '0.5rem' }}>
        CV: Coeficiente de variación (stddev / avg). ESTABLE &lt;5% · MODERADO 5-15% · VOLÁTIL &gt;15%.
      </p>
    </div>
  );
};
