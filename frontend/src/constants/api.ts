/**
 * Rutas y configuración base de la API.
 * Centraliza paths para evitar magic strings en servicios y componentes.
 */

/** Prefijo base de la API (relativo en Docker/Nginx; override con VITE_API_BASE en dev directo). */
export const API_BASE = import.meta.env.VITE_API_BASE ?? '/api/v1';

/** Segmentos de rutas por recurso. */
export const API_ROUTES = {
  auth: `${API_BASE}/auth`,
  login: `${API_BASE}/auth/login`,
  register: `${API_BASE}/auth/register`,
  validate: `${API_BASE}/auth/validate`,
  refresh: `${API_BASE}/auth/refresh`,

  products: `${API_BASE}/productos`,
  productSearch: `${API_BASE}/productos/search`,

  suppliers: `${API_BASE}/proveedores`,
  supplierList: `${API_BASE}/proveedores/list`,
  supplierMappings: (id: number) => `${API_BASE}/proveedores/${id}/mappings`,

  dashboard: `${API_BASE}/dashboard`,
  dashboardSummary: `${API_BASE}/dashboard/summary`,
  dashboardExport: `${API_BASE}/dashboard/export/csv`,

  priceHistory: `${API_BASE}/historial-precios`,
  priceHistoryByProduct: (id: number) => `${API_BASE}/historial-precios/producto/${id}`,
  priceHistoryByProductAndSupplier: (productId: number, supplierId: number) =>
    `${API_BASE}/historial-precios/producto/${productId}/proveedor/${supplierId}`,
  priceHistoryAnalytics: (productId: number, supplierId: number) =>
    `${API_BASE}/historial-precios/producto/${productId}/proveedor/${supplierId}/analytics`,
  priceHistoryRegister: `${API_BASE}/historial-precios/registrar`,

  adminUsers: `${API_BASE}/admin/usuarios`,
  adminUserById: (id: number) => `${API_BASE}/admin/usuarios/${id}`,
  userProfile: `${API_BASE}/usuarios/perfil`,

  mappings: `${API_BASE}/mappings`,
  mappingById: (id: number) => `${API_BASE}/mappings/${id}`,

  analytics: `${API_BASE}/analytics`,
  analyticsPriceVariation: `${API_BASE}/analytics/price-variation`,
  analyticsStockoutRates: `${API_BASE}/analytics/stockout-rates`,
  analyticsPriceDispersion: `${API_BASE}/analytics/price-dispersion`,
  analyticsMoq: `${API_BASE}/analytics/moq`,
  analyticsConditionMix: `${API_BASE}/analytics/condition-mix`,
  analyticsCostCoverage: `${API_BASE}/analytics/cost-coverage`,
  analyticsStockVolatility: `${API_BASE}/analytics/stock-volatility`,
  analyticsCatalogGrowth: `${API_BASE}/analytics/catalog-growth`,
  analyticsPriceStability: `${API_BASE}/analytics/price-stability`,
  dashboardSupplierHealth: `${API_BASE}/dashboard/supplier-health`,

  budget: `${API_BASE}/budget`,
  budgetSimulate: `${API_BASE}/budget/simulate`,
  budgetById: (id: number) => `${API_BASE}/budget/${id}`,
  budgetStatus: (id: number) => `${API_BASE}/budget/${id}/status`,
  budgetExport: (id: number) => `${API_BASE}/budget/${id}/export`,
} as const;
