import { useQuery } from '@tanstack/react-query';
import { CostCoverageDto, StockVolatilityDto } from '@/types';
import { getStockoutRates, getCostCoverage, getStockVolatility } from '@/features/dashboard/services/analyticsService';
import { DataTable } from '@/components/ui/DataTable';

const stockoutColor = (rate: number | null | undefined) => {
  const r = rate ?? 0;
  if (r >= 50) return { bg: '#fef2f2', border: '#fca5a5', text: '#b91c1c', label: 'Critico' };
  if (r >= 25) return { bg: '#fff7ed', border: '#fdba74', text: '#c2410c', label: 'Alto' };
  if (r >= 10) return { bg: '#fefce8', border: '#fde047', text: '#92400e', label: 'Moderado' };
  return { bg: '#f0fdf4', border: '#86efac', text: '#166534', label: 'Bajo' };
};

export const StockoutRateSection = () => {
  const { data, isLoading, error } = useQuery({
    queryKey: ['analytics-stockout-rates'],
    queryFn: getStockoutRates,
  });

  if (isLoading) return <div className="centered-message">Calculando tasas de stockout...</div>;
  if (error || !data) return <div className="centered-message error-message">Error al cargar Stockout Rates.</div>;

  return (
    <div className="dashboard-card">
      <h3 className="card-title">Stockout Rate por Proveedor</h3>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))', gap: '1rem' }}>
        {data.map((dto) => {
          const colors = stockoutColor(dto.stockoutRate);
          return (
            <div
              key={dto.supplierId}
              style={{
                background: colors.bg,
                border: `1px solid ${colors.border}`,
                borderRadius: '10px',
                padding: '1rem',
              }}
            >
              <div style={{ fontWeight: 700, fontSize: '0.9rem', color: '#1f2937', marginBottom: '0.25rem' }}>
                {dto.supplierName}
              </div>
              <div style={{ fontSize: '1.8rem', fontWeight: 800, color: colors.text, lineHeight: 1 }}>
                {(dto.stockoutRate ?? 0).toFixed(1)}%
              </div>
              <div style={{ fontSize: '0.72rem', color: '#6b7280', marginTop: '0.25rem' }}>
                {dto.outOfStockSkus} / {dto.totalSkus} SKUs sin stock
              </div>
              <div
                style={{
                  marginTop: '0.5rem',
                  display: 'inline-block',
                  fontSize: '0.7rem',
                  fontWeight: 700,
                  color: colors.text,
                  background: 'white',
                  border: `1px solid ${colors.border}`,
                  borderRadius: '9999px',
                  padding: '1px 8px',
                }}
              >
                {colors.label}
              </div>
            </div>
          );
        })}
      </div>
      <p style={{ fontSize: '0.72rem', color: '#9ca3af', marginTop: '1rem' }}>
        Verde &lt;10% · Amarillo 10-25% · Naranja 25-50% · Rojo ≥50%
      </p>
    </div>
  );
};

const coverageBadge = (status: string) => {
  if (status === 'SIN_STOCK') return { bg: '#fef2f2', color: '#b91c1c', label: 'Sin Stock' };
  if (status === 'RIESGO_PROVEEDOR_UNICO') return { bg: '#fff7ed', color: '#c2410c', label: 'Proveedor Único' };
  return { bg: '#f0fdf4', color: '#166534', label: 'Cubierto' };
};

export const CostCoverageSection = () => {
  const { data, isLoading, error } = useQuery({
    queryKey: ['analytics-cost-coverage'],
    queryFn: () => getCostCoverage(200),
    staleTime: 5 * 60 * 1000,
  });
  if (isLoading) return <div className="centered-message">Calculando cobertura de proveedores...</div>;
  if (error || !data) return <div className="centered-message error-message">Error al cargar Cost Coverage.</div>;
  const risky = data.filter((r: CostCoverageDto) => r.coverageStatus !== 'CUBIERTO');
  return (
    <div className="dashboard-card" style={{ overflowX: 'auto' }}>
      <h3 className="card-title">
        Cost Coverage Ratio — SKUs en riesgo
        {risky.length > 0 && (
          <span style={{ marginLeft: '0.75rem', fontSize: '0.75rem', background: '#fee2e2', color: '#b91c1c', padding: '2px 8px', borderRadius: '9999px' }}>
            {risky.length} en riesgo
          </span>
        )}
      </h3>
      <DataTable
        columns={[
          { header: 'MPN', sortKey: 'mpn', cell: (r: CostCoverageDto) => <span className="font-mono text-xs">{r.mpn}</span> },
          { header: 'Categoría', sortKey: 'category', cell: (r: CostCoverageDto) => r.category ?? '—' },
          { header: 'Proveedores', sortKey: 'totalSuppliers', cell: (r: CostCoverageDto) => r.totalSuppliers, align: 'center' },
          { header: 'Con Stock', sortKey: 'suppliersWithStock', cell: (r: CostCoverageDto) => r.suppliersWithStock, align: 'center' },
          {
            header: 'Estado',
            sortKey: 'coverageStatus',
            align: 'center',
            cell: (r: CostCoverageDto) => {
              const b = coverageBadge(r.coverageStatus);
              return (
                <span style={{ background: b.bg, color: b.color, fontWeight: 700, fontSize: '0.7rem', padding: '2px 8px', borderRadius: '9999px' }}>
                  {b.label}
                </span>
              );
            },
          },
        ]}
        data={risky.length > 0 ? risky : data.slice(0, 50)}
        emptyMessage="Todos los SKUs tienen cobertura adecuada."
        keyExtractor={(r: CostCoverageDto) => r.productId}
        defaultSortKey="suppliersWithStock"
        defaultSortDirection="asc"
      />
      <p style={{ fontSize: '0.72rem', color: '#9ca3af', marginTop: '0.5rem' }}>
        Mostrando SKUs en riesgo (sin stock o proveedor único). {data.length} SKUs totales analizados.
      </p>
    </div>
  );
};

const volatilityBadge = (type: string) => {
  if (type === 'INCONSISTENCIA_API') return { bg: '#fef2f2', color: '#b91c1c', label: 'Inconsistencia API' };
  if (type === 'VENTA_PROBABLE') return { bg: '#eff6ff', color: '#1d4ed8', label: 'Venta Probable' };
  return { bg: '#f9fafb', color: '#6b7280', label: 'Fluctuación Normal' };
};

export const StockVolatilitySection = () => {
  const { data, isLoading, error } = useQuery({
    queryKey: ['analytics-stock-volatility'],
    queryFn: () => getStockVolatility(30, 100),
    staleTime: 5 * 60 * 1000,
  });
  if (isLoading) return <div className="centered-message">Analizando volatilidad de stock...</div>;
  if (error || !data) return <div className="centered-message error-message">Error al cargar Stock Volatility.</div>;
  return (
    <div className="dashboard-card" style={{ overflowX: 'auto' }}>
      <h3 className="card-title">Stock Volatility Index — SKUs con alta volatilidad (últimos 30 días)</h3>
      {data.length === 0 ? (
        <p style={{ color: '#6b7280', fontSize: '0.875rem' }}>
          Sin volatilidad detectada. Los datos se acumulan en cada sincronización automática.
        </p>
      ) : (
        <DataTable
          columns={[
            { header: 'MPN', sortKey: 'mpn', cell: (r: StockVolatilityDto) => <span className="font-mono text-xs">{r.mpn}</span> },
            { header: 'Proveedor', sortKey: 'supplierName', cell: (r: StockVolatilityDto) => r.supplierName },
            { header: 'Cambios/24h', sortKey: 'changesIn24h', cell: (r: StockVolatilityDto) => r.changesIn24h, align: 'center' },
            { header: 'Stock Max', sortKey: 'maxStock', cell: (r: StockVolatilityDto) => r.maxStock, align: 'center' },
            { header: 'Stock Min', sortKey: 'minStock', cell: (r: StockVolatilityDto) => r.minStock, align: 'center' },
            {
              header: 'Tipo',
              sortKey: 'volatilityType',
              align: 'center',
              cell: (r: StockVolatilityDto) => {
                const b = volatilityBadge(r.volatilityType);
                return (
                  <span style={{ background: b.bg, color: b.color, fontWeight: 700, fontSize: '0.7rem', padding: '2px 8px', borderRadius: '9999px' }}>
                    {b.label}
                  </span>
                );
              },
            },
          ]}
          data={data}
          emptyMessage="Sin volatilidad detectada."
          keyExtractor={(r: StockVolatilityDto) => `${r.productId}-${r.supplierId}`}
          defaultSortKey="changesIn24h"
          defaultSortDirection="desc"
        />
      )}
      <p style={{ fontSize: '0.72rem', color: '#9ca3af', marginTop: '0.5rem' }}>
        Inconsistencia API: stock 100→0→≥50 en &lt;24h. Venta Probable: bajada sin recuperación.
      </p>
    </div>
  );
};
