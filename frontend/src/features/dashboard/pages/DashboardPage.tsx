import { useQuery } from '@tanstack/react-query';
import { getDashboardSummary } from '../services/dashboardService';
import { getSupplierHealth } from '../services/supplierHealthService';
import { getUserBudgets } from 'features/budget/services/budgetService';
import { BudgetResponseDto, ChartDataDto, SyncHistoryDto, ProviderStatusDto, SupplierHealthDto } from 'src/types';
import {
  AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
  BarChart, Bar, PieChart, Pie, Cell, Legend
} from 'recharts';
import toast from 'react-hot-toast';
import api from 'services/api';
import { useAuth } from 'features/auth/context/AuthContext';
import { GRAFANA_DASHBOARD_URL, PROMETHEUS_DASHBOARD_URL } from 'src/constants/links';
import { Building2, Download, ExternalLink, FileText, Gauge, LayoutDashboard, Package, ShieldCheck, TriangleAlert } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import './DashboardPage.css';

// --- Componentes Reutilizables ---

const KpiCard = ({
  title,
  value,
  unit = '',
  icon,
  iconClass = 'icon-blue',
}: {
  title: string;
  value: string | number;
  unit?: string;
  icon?: React.ReactNode;
  iconClass?: string;
}) => (
  <div className="dashboard-card kpi-card" style={{ display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
    {icon && <div className={`kpi-icon ${iconClass}`}>{icon}</div>}
    <p className="kpi-info-title">{title}</p>
    <p className="kpi-info-value">{value}{unit && <span className="text-sm font-normal">{unit}</span>}</p>
  </div>
);

const SemaphoreCard = ({ label, value, colorClass }: { label: string; value: number; colorClass: string; }) => (
  <div className={`semaphore-card ${colorClass}`}>
    <div>
      <div className="semaphore-value">{value}</div>
      <div className="semaphore-label">{label}</div>
    </div>
  </div>
);

const ChartCard = ({ title, children, className }: { title: string, children: React.ReactNode, className?: string }) => (
  <div className={`dashboard-card ${className || ''}`} style={{ minHeight: '300px' }}>
    <h3 className="card-title">{title}</h3>
    {children}
  </div>
);

// --- Componentes Hijos ---

const SystemStatus = ({ failedProviders }: { failedProviders?: ProviderStatusDto[] }) => {
  if (!failedProviders) return null;
  const hasFailed = failedProviders.length > 0;

  if (hasFailed) {
    return (
      <div className="system-status-alert error">
        <p className="status-alert-title">
          <TriangleAlert size={16} aria-hidden="true" />
          Atención: Se han detectado problemas de conexión con las siguientes tiendas:
        </p>
        <ul>
          {failedProviders.map(provider => (
            <li key={provider.id}>
              <strong>{provider.name}</strong>: {provider.lastError}
            </li>
          ))}
        </ul>
      </div>
    );
  }

  return (
    <div className="system-status-alert success">
      <p>Consultas realizadas con éxito a todas las tiendas</p>
    </div>
  );
};

const SyncAreaChart = ({ data }: { data: SyncHistoryDto[] }) => (
  <div style={{ minHeight: '250px', height: '250px', width: '100%', position: 'relative' }}>
    <ResponsiveContainer width="100%" height="100%">
      <AreaChart data={data} margin={{ top: 10, right: 30, left: 0, bottom: 0 }}>
        <CartesianGrid strokeDasharray="3 3" />
        <XAxis dataKey="date" />
        <YAxis />
        <Tooltip />
        <Legend />
        <Area
          type="monotone"
          dataKey="successCount"
          stackId="1"
          stroke="#374151"
          fill="#374151"
          fillOpacity={0.6}
          name="Éxitos"
        />
        <Area
          type="monotone"
          dataKey="errorCount"
          stackId="1"
          stroke="#9ca3af"
          fill="#9ca3af"
          fillOpacity={0.6}
          name="Errores"
        />
      </AreaChart>
    </ResponsiveContainer>
  </div>
);

const BrandPieChart = ({ data }: { data: ChartDataDto[] }) => {
  const COLORS = ['#1f2937', '#374151', '#4b5563', '#6b7280', '#9ca3af'];
  return (
    <div style={{ height: '300px', width: '100%' }}>
      <ResponsiveContainer width="100%" height="100%">
        <PieChart>
          <Pie data={data} dataKey="value" nameKey="label" cx="50%" cy="50%" innerRadius={60} outerRadius={80} fill="#8884d8" paddingAngle={5}>
            {data.map((_, index) => <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />)}
          </Pie>
          <Tooltip />
          <Legend />
        </PieChart>
      </ResponsiveContainer>
    </div>
  );
};

const StockBarChart = ({ data }: { data: ChartDataDto[] }) => (
  <div style={{ height: '300px', width: '100%' }}>
    <ResponsiveContainer width="100%" height="100%">
      <BarChart data={data} layout="vertical" margin={{ top: 5, right: 20, left: 40, bottom: 5 }}>
        <CartesianGrid strokeDasharray="3 3" />
        <XAxis type="number" />
        <YAxis dataKey="label" type="category" width={80} />
        <Tooltip />
        <Legend />
        <Bar dataKey="value" fill="#4b5563" name="Stock" />
      </BarChart>
    </ResponsiveContainer>
  </div>
);

// --- Sección de Salud de Proveedores ---

const SlaGradeBadge = ({ grade }: { grade: string }) => {
  const colors: Record<string, string> = { A: '#1f2937', B: '#374151', C: '#6b7280', D: '#9ca3af' };
  const bg = colors[grade] || '#6b7280';
  return (
    <span style={{
      display: 'inline-block', padding: '2px 8px', borderRadius: '9999px', fontSize: '0.75rem',
      fontWeight: 700, color: '#fff', backgroundColor: bg,
    }}>
      {grade}
    </span>
  );
};

const SupplierHealthSection = ({ data }: { data: SupplierHealthDto[] }) => (
  <div className="dashboard-card" style={{ overflowX: 'auto' }}>
    <h3 className="card-title" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
      <ShieldCheck size={18} />
      Salud de Proveedores (Supplier Health KPIs)
    </h3>
    {data.length === 0 ? (
      <p style={{ color: '#6b7280', fontSize: '0.875rem' }}>
        No hay datos de sincronización aún. Los KPIs se poblarán tras la primera sincronización.
      </p>
    ) : (
      <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.875rem' }}>
        <thead>
          <tr style={{ borderBottom: '2px solid #e5e7eb' }}>
            <th style={{ textAlign: 'left', padding: '8px' }}>Proveedor</th>
            <th style={{ textAlign: 'center', padding: '8px' }}>SLA Score</th>
            <th style={{ textAlign: 'center', padding: '8px' }}>Grade</th>
            <th style={{ textAlign: 'center', padding: '8px' }}>Éxito Sync %</th>
            <th style={{ textAlign: 'center', padding: '8px' }}>Latencia Avg</th>
            <th style={{ textAlign: 'center', padding: '8px' }}>Items/Sync</th>
            <th style={{ textAlign: 'center', padding: '8px' }}>Errores API %</th>
            <th style={{ textAlign: 'center', padding: '8px' }}>Stale Rate %</th>
          </tr>
        </thead>
        <tbody>
          {data.map((row) => (
            <tr key={row.supplierId} style={{ borderBottom: '1px solid #f3f4f6' }}>
              <td style={{ padding: '8px', fontWeight: 500 }}>{row.supplierName}</td>
              <td style={{ textAlign: 'center', padding: '8px', fontWeight: 700 }}>
                {row.slaScore.toFixed(1)}
              </td>
              <td style={{ textAlign: 'center', padding: '8px' }}>
                <SlaGradeBadge grade={row.slaGrade} />
              </td>
              <td style={{ textAlign: 'center', padding: '8px' }}>{row.syncSuccessRate.toFixed(1)}%</td>
              <td style={{ textAlign: 'center', padding: '8px' }}>
                {row.avgLatencyMs > 0 ? `${row.avgLatencyMs} ms` : '—'}
              </td>
              <td style={{ textAlign: 'center', padding: '8px' }}>
                {row.avgItemsPerSync > 0 ? row.avgItemsPerSync.toFixed(0) : '—'}
              </td>
              <td style={{ textAlign: 'center', padding: '8px' }}>{row.apiErrorRate.toFixed(1)}%</td>
              <td style={{ textAlign: 'center', padding: '8px' }}>{row.staleRate.toFixed(1)}%</td>
            </tr>
          ))}
        </tbody>
      </table>
    )}
  </div>
);

// --- Widget de Resumen de Presupuestos ---

const BudgetSummaryWidget = ({ budgets }: { budgets: BudgetResponseDto[] }) => {
  const navigate = useNavigate();
  const totalBudgets = budgets.length;
  const draftCount = budgets.filter(b => b.status === 'DRAFT').length;
  const finalizedCount = budgets.filter(b => b.status === 'FINALIZED').length;
  const exportedCount = budgets.filter(b => b.status === 'EXPORTED').length;
  const totalAmount = budgets.reduce((sum, b) => sum + (b.totalAmount || 0), 0);

  // Estadísticas mensuales (últimos 3 meses)
  const toMonthKey = (iso: string) => {
    try {
      const d = new Date(iso);
      return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
    } catch {
      return '';
    }
  };

  const now = new Date();
  const monthKeys: string[] = [];
  for (let i = 2; i >= 0; i--) {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
    monthKeys.push(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`);
  }

  const totalsByMonth: Record<string, number> = {};
  const countsByMonth: Record<string, number> = {};
  budgets.forEach((b) => {
    const k = toMonthKey(b.createdAt);
    if (!k) return;
    totalsByMonth[k] = (totalsByMonth[k] || 0) + (b.totalAmount || 0);
    countsByMonth[k] = (countsByMonth[k] || 0) + 1;
  });

  const monthlyData = monthKeys.map((k) => ({ month: k, total: totalsByMonth[k] || 0, count: countsByMonth[k] || 0 }));
  const thisMonth = monthlyData[2];
  const prevMonth = monthlyData[1];
  const changePct = prevMonth && prevMonth.total > 0 ? ((thisMonth.total - prevMonth.total) / prevMonth.total) * 100 : (thisMonth.total > 0 ? 100 : 0);

  return (
    <div className="dashboard-card">
      <h3 className="card-title" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
        <FileText size={18} />
        Resumen de Presupuestos
      </h3>
      <div className="dashboard-grid grid-cols-4" style={{ marginBottom: '1rem' }}>
        <div className="kpi-card" style={{ flexDirection: 'column', alignItems: 'flex-start', gap: '0.25rem' }}>
          <p className="kpi-info-title">Totales</p>
          <p className="kpi-info-value">{totalBudgets}</p>
        </div>
        <div className="kpi-card" style={{ flexDirection: 'column', alignItems: 'flex-start', gap: '0.25rem' }}>
          <p className="kpi-info-title">Borrador</p>
          <p className="kpi-info-value" style={{ color: 'var(--text-secondary)' }}>{draftCount}</p>
        </div>
        <div className="kpi-card" style={{ flexDirection: 'column', alignItems: 'flex-start', gap: '0.25rem' }}>
          <p className="kpi-info-title">Finalizados</p>
          <p className="kpi-info-value" style={{ color: 'var(--text-primary)' }}>{finalizedCount}</p>
        </div>
        <div className="kpi-card" style={{ flexDirection: 'column', alignItems: 'flex-start', gap: '0.25rem' }}>
          <p className="kpi-info-title">Exportados</p>
          <p className="kpi-info-value" style={{ color: 'var(--text-muted)' }}>{exportedCount}</p>
        </div>
      </div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)' }}>
          Importe total: <strong style={{ color: 'var(--text-primary)' }}>{new Intl.NumberFormat('es-ES', { style: 'currency', currency: 'EUR' }).format(totalAmount)}</strong>
        </p>
        <button className="btn btn-secondary btn-sm" onClick={() => navigate('/presupuestos')}>
          <FileText size={14} />
          Ver todos
        </button>
      </div>
      <div style={{ marginTop: '0.75rem', display: 'flex', gap: '1rem', alignItems: 'center' }}>
        <div style={{ flex: '1 1 200px' }}>
          <p className="kpi-info-title">Presupuestado este mes</p>
          <p className="kpi-info-value">{new Intl.NumberFormat('es-ES', { style: 'currency', currency: 'EUR' }).format(thisMonth.total)}</p>
          <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>{thisMonth.count} presupuestos</p>
        </div>
        <div style={{ flex: '0 0 160px' }}>
          <p className="kpi-info-title">Variación vs mes anterior</p>
          <p className="kpi-info-value" style={{ color: changePct >= 0 ? 'var(--text-primary)' : 'var(--text-muted)' }}>{changePct.toFixed(1)}%</p>
        </div>
      </div>
    </div>
  );
};

// --- Componente Principal de la Página Dashboard ---

const DashboardPage = () => {
  const { isAdmin, roles } = useAuth();
  const isCliente = roles.includes('ROLE_CLIENTE');

  const { data: summary, isLoading, error } = useQuery({
    queryKey: ['dashboardSummary'],
    queryFn: getDashboardSummary,
  });

  const { data: supplierHealth } = useQuery({
    queryKey: ['supplierHealth'],
    queryFn: getSupplierHealth,
    enabled: isAdmin,
    staleTime: 5 * 60 * 1000,
  });

  const { data: budgets } = useQuery({
    queryKey: ['userBudgets'],
    queryFn: getUserBudgets,
    staleTime: 2 * 60 * 1000,
  });

  const handleExport = async () => {
    toast.loading('Exportando datos...');
    try {
      const response = await api.get('/dashboard/export/csv', {
        responseType: 'blob'
      });
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `dashboard_data_${new Date().toISOString().split('T')[0]}.zip`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      toast.dismiss();
      toast.success('Datos exportados con éxito.');
    } catch (error) {
      toast.dismiss();
      toast.error('Error al exportar los datos.');
      console.error("Error exportando datos", error);
    }
  };

  const isDataReady = !isLoading && !error && summary;

  return (
    <div className="dashboard-container">
      <header className="dashboard-header">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', width: '100%' }}>
          <h1 className="dashboard-title dashboard-title-row">
            <LayoutDashboard size={24} aria-hidden="true" />
            Dashboard de Sistema
          </h1>
          <div className="header-actions">
            <button onClick={handleExport} className="btn btn-secondary">
              <Download size={16} aria-hidden="true" />
              Exportar Datos (ZIP)
            </button>
            {GRAFANA_DASHBOARD_URL && (
              <a
                href={GRAFANA_DASHBOARD_URL}
                target="_blank"
                rel="noopener noreferrer"
                className="btn btn-secondary"
              >
                <ExternalLink size={16} aria-hidden="true" />
                Ver dashboard en Grafana
              </a>
            )}
            {PROMETHEUS_DASHBOARD_URL && (
              <a
                href={PROMETHEUS_DASHBOARD_URL}
                target="_blank"
                rel="noopener noreferrer"
                className="btn btn-secondary"
              >
                <ExternalLink size={16} aria-hidden="true" />
                Ver estado del sistema
              </a>
            )}
          </div>
        </div>
      </header>

      <main style={{ display: "flex", flexDirection: "column", gap: "1.5rem", marginTop: '2rem' }}>
        {isDataReady ? (
          <>
            {(isAdmin || isCliente) && <SystemStatus failedProviders={summary.failedProviders} />}

            <div className={`dashboard-grid ${isAdmin ? 'grid-cols-3' : 'grid-cols-2'}`}>
              <KpiCard title="Total Productos" value={summary.totalProducts} icon={<Package size={20} />} iconClass="icon-blue" />
              <KpiCard title="Total Proveedores" value={summary.totalSuppliers} icon={<Building2 size={20} />} iconClass="icon-purple" />
              {isAdmin && <KpiCard title="Latencia API" value={summary.avgLatencyOverall.toFixed(0)} unit="ms" icon={<Gauge size={20} />} iconClass="icon-green" />}
            </div>

            <div className="dashboard-grid grid-cols-2">
              <SemaphoreCard label="Disponibles" value={summary.productsAvailable} colorClass="bg-green" />
              <SemaphoreCard label="Sin Stock" value={summary.productsOutOfStock} colorClass="bg-red" />
            </div>

            {budgets && budgets.length > 0 && <BudgetSummaryWidget budgets={budgets} />}

            {isAdmin && (
              <div className="dashboard-grid grid-cols-3">
                <ChartCard title="Salud de Sincronización con APIs externas (7d)" className="col-span-2">
                  <SyncAreaChart data={summary.syncHistory} />
                </ChartCard>
                <ChartCard title="Top Marcas">
                  <BrandPieChart data={summary.productsByBrand} />
                </ChartCard>
              </div>
            )}
            {!isAdmin && isCliente && (
              <ChartCard title="Top Marcas">
                <BrandPieChart data={summary.productsByBrand} />
              </ChartCard>
            )}

            <ChartCard title="Volumen de Stock por Proveedor">
              <StockBarChart data={summary.stockBySupplier} />
            </ChartCard>

            {isAdmin && supplierHealth && (
              <SupplierHealthSection data={supplierHealth} />
            )}
          </>
        ) : (
          <div className="centered-message">Cargando datos del panel...</div>
        )}
      </main>
    </div>
  );
};

export default DashboardPage;
