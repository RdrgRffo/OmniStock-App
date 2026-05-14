import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { TrendingUp, AlertTriangle, BarChart2, Package, ShieldAlert, Activity, LineChart } from 'lucide-react';
import { getDashboardSummary } from '@/features/dashboard/services/dashboardService';
import TradingView from '@/features/dashboard/components/TradingView';
import '@/features/dashboard/pages/DashboardPage.css';
import { ANALYTICS_TABS, AnalyticsTabId } from '../constants/tabs';
import { SectionHeader, TableCard } from '../components/AnalyticsSectionLayout';
import { TopMoversTable, ZombiesTable } from '../components/OpportunitiesTables';
import { PriceVariationChart, PriceDispersionSection, PriceStabilitySection } from '../components/PriceSections';
import { StockoutRateSection, CostCoverageSection, StockVolatilitySection } from '../components/StockSections';
import { ConditionMixSection, MoqDistributionSection, CatalogGrowthSection } from '../components/CatalogSections';

const AnalyticsPage = () => {
  const [activeTab, setActiveTab] = useState<AnalyticsTabId>('oportunidades');

  const { data: summary, isLoading, error } = useQuery({
    queryKey: ['dashboardSummary'],
    queryFn: getDashboardSummary,
  });

  const isDataReady = !isLoading && !error && summary;

  const tabStyle = (id: AnalyticsTabId): React.CSSProperties => ({
    display: 'flex',
    alignItems: 'center',
    padding: '0.5rem 1.25rem',
    borderRadius: '8px',
    border: 'none',
    cursor: 'pointer',
    fontWeight: activeTab === id ? 600 : 400,
    backgroundColor: activeTab === id ? 'var(--color-primary, #6366f1)' : 'transparent',
    color: activeTab === id ? 'white' : 'var(--text-secondary, #6b7280)',
    fontSize: '0.875rem',
    transition: 'all 0.15s ease',
    whiteSpace: 'nowrap',
    boxShadow: activeTab === id ? '0 1px 4px rgba(99,102,241,0.35)' : 'none',
  });

  return (
    <div className="dashboard-container">
      <header className="dashboard-header">
        <h1 className="dashboard-title">Analytics &amp; KPIs</h1>
        <p style={{ margin: 0, color: '#6b7280', fontSize: '0.9rem' }}>
          Análisis de precios, disponibilidad y datos para modelos de predicción de compras
        </p>
      </header>

      <nav
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          flexWrap: 'wrap',
          gap: '0',
          padding: '0.5rem 1rem',
          backgroundColor: 'var(--bg-secondary)',
          borderRadius: '12px',
          border: '1px solid var(--bg-tertiary)',
          marginBottom: '2rem',
        }}
      >
        {ANALYTICS_TABS.map((tab, index) => (
          <div key={tab.id} style={{ display: 'flex', alignItems: 'center' }}>
            <button type="button" style={tabStyle(tab.id)} onClick={() => setActiveTab(tab.id)}>
              <span style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                {tab.icon}
                {tab.label}
              </span>
            </button>
            {index < ANALYTICS_TABS.length - 1 && (
              <span
                style={{
                  width: '1px',
                  height: '1.25rem',
                  backgroundColor: 'var(--bg-tertiary)',
                  margin: '0 0.125rem',
                  flexShrink: 0,
                }}
              />
            )}
          </div>
        ))}
      </nav>

      <main style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>
        {activeTab === 'oportunidades' && (
          <>
            {isDataReady && (
              <section>
                <SectionHeader icon={<TrendingUp size={20} />} title="Análisis de Margen y Oportunidades de Compra" />
                <TradingView />
              </section>
            )}
            {!isDataReady && isLoading && (
              <div className="centered-message">Cargando datos de oportunidades...</div>
            )}

            {isDataReady && (
              <section>
                <SectionHeader icon={<AlertTriangle size={20} />} title="Alertas de Precio en Tiempo Real" />
                <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
                  <TableCard title="Alertas: Subidas de Precio">
                    <TopMoversTable data={summary.topPriceIncreases} type="up" />
                  </TableCard>
                  <TableCard title="Oportunidades: Bajadas de Precio">
                    <TopMoversTable data={summary.topPriceDecreases} type="down" />
                  </TableCard>
                  {summary.staleProducts?.length > 0 && (
                    <TableCard
                      title='Productos "Zombies"'
                      subTitle="Productos sin actualización en +30 días. Revisa la conexión con el proveedor."
                    >
                      <ZombiesTable products={summary.staleProducts} />
                    </TableCard>
                  )}
                </div>
              </section>
            )}
            {!isDataReady && isLoading && (
              <div className="centered-message">Cargando datos de oportunidades...</div>
            )}
          </>
        )}

        {activeTab === 'precios' && (
          <>
            <section>
              <SectionHeader
                icon={<TrendingUp size={20} />}
                title="Price Index Variation por Proveedor"
                subtitle="Variación mensual de precios promedio (LAG). Precios normalizados a EUR."
              />
              <PriceVariationChart />
            </section>
            <section>
              <SectionHeader
                icon={<BarChart2 size={20} />}
                title="Price Dispersion Index"
                subtitle="SKUs con mayor diferencia de precio entre proveedores. Alto % = oportunidad de negociación."
              />
              <PriceDispersionSection />
            </section>
            <section>
              <SectionHeader
                icon={<LineChart size={20} />}
                title="Price Stability Score"
                subtitle="SKUs ordenados por coeficiente de variación del precio en los últimos 90 días."
              />
              <PriceStabilitySection />
            </section>
          </>
        )}

        {activeTab === 'stock' && (
          <>
            <section>
              <SectionHeader
                icon={<AlertTriangle size={20} />}
                title="Stockout Rate por Proveedor"
                subtitle="Tasa de SKUs con stock = 0. Un stockout es pérdida de oportunidad de venta."
              />
              <StockoutRateSection />
            </section>
            <section>
              <SectionHeader
                icon={<AlertTriangle size={20} />}
                title="Stock Volatility Index"
                subtitle="SKUs con fluctuaciones de stock anómalas en ventanas de 24 horas. Detecta inconsistencias de API."
              />
              <StockVolatilitySection />
            </section>
            <section>
              <SectionHeader
                icon={<ShieldAlert size={20} />}
                title="Cost Coverage Ratio"
                subtitle="SKUs en riesgo por dependencia de proveedor único o sin stock disponible."
              />
              <CostCoverageSection />
            </section>
          </>
        )}

        {activeTab === 'catalogo' && (
          <>
            <section>
              <SectionHeader
                icon={<Activity size={20} />}
                title="Condition Mix"
                subtitle="Distribución de condición de productos (NEW/REFURBISHED/BOX_DAMAGED/USED) por proveedor."
              />
              <ConditionMixSection />
            </section>
            <section>
              <SectionHeader
                icon={<Package size={20} />}
                title="MOQ Distribution"
                subtitle="Cantidad mínima de pedido promedio por proveedor. MOQ alto = barrera de entrada."
              />
              <MoqDistributionSection />
            </section>
            <section>
              <SectionHeader
                icon={<TrendingUp size={20} />}
                title="Catalog Growth Trend"
                subtitle="Evolución del número de productos nuevos incorporados al catálogo, semana a semana."
              />
              <CatalogGrowthSection />
            </section>
          </>
        )}
      </main>
    </div>
  );
};

export default AnalyticsPage;
